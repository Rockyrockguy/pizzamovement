package com.example.pizzamovement.server;

import com.example.pizzamovement.config.PizzaConfig;
import com.example.pizzamovement.movement.CollisionHelper;
import com.example.pizzamovement.movement.MovementState;
import com.example.pizzamovement.movement.SpeedController;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side referee. In vanilla 1.16.5 the client moves the player and the server only checks the result,
 * so the server cannot simulate this movement itself. What it does instead:
 *  - accepts only (state, stage) from the client, never a velocity;
 *  - caps the claimed stage by its own measure of how long the player has really been sprinting;
 *  - checks that a wall / ceiling really exists when WALL_RUNNING / CEILING_RUNNING is claimed;
 *  - sets back players who move faster than the claimed stage allows;
 *  - owns fall damage (resets fallDistance during validated wall/ceiling running) and the particles.
 */
public class ServerMovementTracker {
    private static class Entry {
        MovementState state = MovementState.NORMAL;
        int claimedStage;
        int charge;
        int stateTicks;
        int bad;
        long lastPacket;
        boolean hasPos;
        double lx, ly, lz;
        int prevAllowedStage;
    }

    private static final Map<UUID, Entry> MAP = new HashMap<UUID, Entry>();

    public static void onClientState(ServerPlayerEntity p, byte stateByte, byte stageByte) {
        Entry e = MAP.get(p.getUUID());
        if (e == null) {
            e = new Entry();
            MAP.put(p.getUUID(), e);
        }
        e.claimedStage = Math.max(0, Math.min(4, (int) stageByte));
        e.lastPacket = p.level.getGameTime();
        setState(p, e, MovementState.byOrdinal(stateByte));
    }

    private static void setState(ServerPlayerEntity p, Entry e, MovementState ns) {
        MovementState old = e.state;
        if (old == ns) return;
        boolean wasSurface = old == MovementState.WALL_RUNNING || old == MovementState.CEILING_RUNNING;
        if (wasSurface) {
            p.fallDistance = -PizzaConfig.SERVER.exitFallGrace.get().floatValue();   // forgive part of the fall
        }
        e.state = ns;
        e.stateTicks = 0;
        e.bad = 0;
        if (ns == MovementState.WALL_RUNNING || ns == MovementState.CEILING_RUNNING || ns == MovementState.WALL_JUMPING) {
            puff(p, ParticleTypes.CLOUD, ns == MovementState.WALL_JUMPING ? 14 : 8);
        }
    }

    private static void puff(ServerPlayerEntity p, net.minecraft.particles.IParticleData type, int count) {
        if (!PizzaConfig.SERVER.enableParticles.get() || !(p.level instanceof ServerWorld)) return;
        ((ServerWorld) p.level).sendParticles(type, p.getX(), p.getY() + 0.2, p.getZ(), count, 0.3, 0.1, 0.3, 0.02);
    }

    @SubscribeEvent
    public void onTick(TickEvent.PlayerTickEvent ev) {
        if (ev.phase != TickEvent.Phase.END || ev.player.level.isClientSide) return;
        if (!(ev.player instanceof ServerPlayerEntity)) return;
        ServerPlayerEntity p = (ServerPlayerEntity) ev.player;
        Entry e = MAP.get(p.getUUID());
        if (e == null) return;
        PizzaConfig.Server cfg = PizzaConfig.SERVER;
        long now = p.level.getGameTime();

        if (e.state != MovementState.NORMAL && now - e.lastPacket > 40) setState(p, e, MovementState.NORMAL);
        if (e.state == MovementState.NORMAL && e.claimedStage == 0) {
            e.hasPos = false;
            e.charge = 0;
            return;
        }
        e.stateTicks++;

        // the server's own measure of how long this player has been running
        boolean running = p.isSprinting() || e.state != MovementState.NORMAL;
        e.charge = running ? e.charge + 1 : Math.max(0, e.charge - cfg.chargeDecayPerTick.get());
        int serverStage = SpeedController.stageForTicks(e.charge);
        int allowedStage = Math.min(e.claimedStage, serverStage + 1);   // +1 tolerates latency

        // geometry must back up the claimed state
        boolean valid = true;
        if (e.state == MovementState.WALL_RUNNING) valid = CollisionHelper.anyWall(p);
        else if (e.state == MovementState.CEILING_RUNNING) valid = CollisionHelper.hasCeiling(p, 0.5);
        if (!valid) {
            if (++e.bad > 6) setState(p, e, MovementState.NORMAL);
        } else {
            e.bad = 0;
        }
        boolean water = p.isInWater() || p.isInLava();
        if (water && ((e.state == MovementState.WALL_RUNNING && !cfg.allowWallRunInWater.get())
                || (e.state == MovementState.CEILING_RUNNING && !cfg.allowCeilingRunInWater.get()))) {
            setState(p, e, MovementState.NORMAL);
        }
        if (e.state == MovementState.WALL_RUNNING && e.stateTicks > cfg.wallRunMaxTicks.get() + 50) {
            setState(p, e, MovementState.NORMAL);
        }
        if (e.state == MovementState.WALL_JUMPING && e.stateTicks > 30) setState(p, e, MovementState.NORMAL);

        if ((e.state == MovementState.WALL_RUNNING || e.state == MovementState.CEILING_RUNNING) && e.bad <= 6) {
            p.fallDistance = 0.0F;           // fall damage is decided here, on the server
        }

        // speed cap: only for players currently claiming mod movement
        if (!p.isPassenger() && !p.isFallFlying()) {
            double moved = Math.hypot(p.getX() - e.lx, p.getZ() - e.lz);
            double cap = Math.min(SpeedController.stageSpeed(allowedStage) * 1.4 + 0.2, cfg.maxSpeed.get() * 1.5 + 0.2);
            if (e.hasPos && moved > cap && moved < 4.0) {            // >= 4 blocks in one tick = teleport, ignore
                p.connection.teleport(e.lx, e.ly, e.lz, p.yRot, p.xRot);
                return;                                              // keep the last good position
            }
        }
        e.lx = p.getX();
        e.ly = p.getY();
        e.lz = p.getZ();
        e.hasPos = true;

        if (allowedStage == 4 && e.prevAllowedStage < 4) puff(p, ParticleTypes.CRIT, 12);
        e.prevAllowedStage = allowedStage;
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent ev) {
        PlayerEntity p = ev.getPlayer();
        MAP.remove(p.getUUID());
    }
}
