package com.example.pizzamovement.movement;

import com.example.pizzamovement.config.PizzaConfig;
import com.example.pizzamovement.network.MovementStatePacket;
import com.example.pizzamovement.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.MovementInput;
import net.minecraft.util.math.vector.Vector3d;

/**
 * CLIENT-SIDE state machine for the local player. Only referenced from client code (ClientEvents).
 * Runs inside InputUpdateEvent, i.e. right before vanilla movement is applied each tick.
 * While a mod state is active the vanilla input impulses are reduced to ~0, so vanilla acceleration does not
 * add on top of our momentum; horizontal velocity is then written directly.
 * The server never accepts a velocity from the client: only (state, stage), which it re-validates.
 */
public class MovementController {
    public static final MovementController INSTANCE = new MovementController();

    public MovementState state = MovementState.NORMAL;
    public GravityDirection gravity = GravityDirection.DOWN;
    public final SpeedController speed = new SpeedController();
    public float fovScale = 1.0F;

    private final WallRunController wall = new WallRunController();
    private final CeilingRunController ceiling = new CeilingRunController();
    private int wallCooldown, jumpTicks, sinceSend;
    private boolean prevJump, havePos;
    private double lastX, lastZ;
    private MovementState lastSentState = null;
    private int lastSentStage = -1;

    public int stage() {
        return speed.stage();
    }

    public void hardReset() {
        state = MovementState.NORMAL;
        gravity = GravityDirection.DOWN;
        speed.reset();
        wallCooldown = jumpTicks = sinceSend = 0;
        prevJump = havePos = false;
        lastSentState = null;
        lastSentStage = -1;
        fovScale = 1.0F;
    }

    public void onInput(ClientPlayerEntity p, MovementInput in) {
        PizzaConfig.Server cfg = PizzaConfig.SERVER;
        boolean jumpPressed = in.jumping && !prevJump;
        prevJump = in.jumping;

        if (!p.isAlive() || p.isPassenger() || p.isSpectator() || p.abilities.flying
                || p.isFallFlying() || p.isSwimming() || p.onClimbable()) {
            state = MovementState.NORMAL;
            gravity = GravityDirection.DOWN;
            speed.reset();
            remember(p);
            updateFov(0);
            sendIfChanged();
            return;
        }
        if (wallCooldown > 0) wallCooldown--;

        // --- input intent in world space (same formula vanilla uses in moveRelative) ---
        double yaw = Math.toRadians(p.yRot);
        double sin = Math.sin(yaw), cos = Math.cos(yaw);
        double ix = in.leftImpulse * cos - in.forwardImpulse * sin;
        double iz = in.forwardImpulse * cos + in.leftImpulse * sin;
        double il = Math.hypot(ix, iz);
        boolean moving = il > 0.3;
        if (il > 1.0) {
            ix /= il;
            iz /= il;
        }
        boolean sneak = in.shiftKeyDown;
        boolean inFluid = p.isInWater() || p.isInLava();
        boolean special = state == MovementState.WALL_RUNNING || state == MovementState.CEILING_RUNNING
                || state == MovementState.WALL_JUMPING;
        boolean sprintHeld = p.isSprinting() || Minecraft.getInstance().options.keySprint.isDown();
        boolean runIntent = moving && !sneak && !inFluid && (sprintHeld || special);

        speed.tickCharge(runIntent);
        int stage = speed.stage();

        boolean controlled;
        if (state == MovementState.WALL_RUNNING) {
            controlled = doWall(p, in, ix, iz, jumpPressed, stage);
        } else if (state == MovementState.CEILING_RUNNING) {
            controlled = doCeiling(p, in, ix, iz, jumpPressed, stage);
        } else {
            controlled = doFree(p, in, ix, iz, moving, sneak, inFluid, stage, jumpPressed);
        }

        if (controlled) {          // keep sprint alive but remove vanilla input acceleration
            in.forwardImpulse = 1.0E-4F;
            in.leftImpulse = 0.0F;
        }
        gravity = state == MovementState.CEILING_RUNNING ? GravityDirection.UP : GravityDirection.DOWN;
        remember(p);
        updateFov(speed.stage());
        sendIfChanged();
    }

    // ------------------------------------------------------------------ ground / air / wall-jump

    private boolean doFree(ClientPlayerEntity p, MovementInput in, double ix, double iz, boolean moving,
                           boolean sneak, boolean inFluid, int stage, boolean jumpPressed) {
        PizzaConfig.Server cfg = PizzaConfig.SERVER;

        if (tryEnterWall(p, ix, iz, moving, sneak, inFluid, stage)) {
            return doWall(p, in, ix, iz, jumpPressed, stage);
        }

        if (state == MovementState.WALL_JUMPING) {
            if (--jumpTicks <= 0 || p.isOnGround()) {
                state = MovementState.NORMAL;
            } else {
                double dx = moving ? ix : speed.velX;     // no input: keep flying the way we were
                double dz = moving ? iz : speed.velZ;
                double target = Math.max(speed.speed(), SpeedController.stageSpeed(stage));
                speed.step(dx, dz, target, cfg.airTurnRate.get() * 0.5);
                applyHorizontal(p);
                return true;
            }
        }

        if (stage == 0 || inFluid) {                     // vanilla movement
            state = MovementState.NORMAL;
            if (havePos) speed.setVelocity(p.getX() - lastX, p.getZ() - lastZ);
            return false;
        }

        state = stage >= 4 ? MovementState.RUNNING : MovementState.ACCELERATING;

        // head-on wall hit: lose part of the speed instead of all of it
        if (havePos && p.horizontalCollision && speed.lastCommanded > 0.15) {
            double moved = Math.hypot(p.getX() - lastX, p.getZ() - lastZ);
            if (moved < speed.lastCommanded * 0.5) speed.impact();
        }
        double turn = p.isOnGround() ? cfg.turnRate.get() : cfg.airTurnRate.get();
        speed.step(moving ? ix : 0.0, moving ? iz : 0.0, SpeedController.stageSpeed(stage), turn);
        applyHorizontal(p);
        return true;
    }

    private boolean tryEnterWall(PlayerEntity p, double ix, double iz, boolean moving, boolean sneak,
                                 boolean inFluid, int stage) {
        PizzaConfig.Server cfg = PizzaConfig.SERVER;
        if (wallCooldown > 0 || !moving || stage < Math.max(1, cfg.wallRunMinStage.get())) return false;
        if (cfg.cancelOnSneak.get() && sneak) return false;
        if (inFluid && !cfg.allowWallRunInWater.get()) return false;
        Direction w = CollisionHelper.findWall(p, ix, iz);
        if (w == null) return false;
        double toward = ix * w.getStepX() + iz * w.getStepZ();
        boolean runUp = p.isOnGround() && toward > 0.6 && p.horizontalCollision;   // sprinted into a wall
        boolean air = !p.isOnGround() && p.getDeltaMovement().y > -0.65;            // airborne next to a wall
        if (!runUp && !air) return false;
        wall.begin(w, stage);
        state = MovementState.WALL_RUNNING;
        return true;
    }

    // ------------------------------------------------------------------ wall

    private boolean doWall(PlayerEntity p, MovementInput in, double ix, double iz, boolean jumpPressed, int stage) {
        switch (wall.tick(p, in, ix, iz, jumpPressed, speed, stage)) {
            case CONTINUE:
                return true;
            case JUMP:
                wallJump(p);
                return true;
            case CEILING:
                startCeiling(p, stage);
                return true;
            case VAULT:
                Vector3d n = CollisionHelper.normal(wall.dir);
                speed.setVelocity(-n.x * 0.22, -n.z * 0.22);
                p.setDeltaMovement(-n.x * 0.22, 0.32, -n.z * 0.22);   // hop onto the ledge
                state = MovementState.NORMAL;
                wallCooldown = 10;
                return false;
            default:
                Vector3d dm = p.getDeltaMovement();
                speed.setVelocity(dm.x, dm.z);
                state = MovementState.NORMAL;
                wallCooldown = 8;
                return false;
        }
    }

    private void wallJump(PlayerEntity p) {
        PizzaConfig.Server cfg = PizzaConfig.SERVER;
        Vector3d n = CollisionHelper.normal(wall.dir);
        double force = cfg.wallJumpForce.get();
        speed.setVelocity(speed.velX + n.x * force, speed.velZ + n.z * force);   // impulse away from the wall
        p.setDeltaMovement(speed.velX, cfg.wallJumpVerticalForce.get(), speed.velZ);
        p.fallDistance = 0.0F;
        state = MovementState.WALL_JUMPING;
        jumpTicks = 14;
        wallCooldown = cfg.wallJumpCooldownTicks.get();   // cannot re-grab immediately
    }

    private void startCeiling(PlayerEntity p, int stage) {
        PizzaConfig.Server cfg = PizzaConfig.SERVER;
        Vector3d head = CollisionHelper.normal(wall.dir);     // run away from the wall, along the ceiling
        // convert the climbing speed into ceiling speed (momentum is preserved)
        double v = Math.max(Math.abs(p.getDeltaMovement().y), SpeedController.stageSpeed(stage) * cfg.ceilingRunSpeed.get() * 0.8);
        v = Math.max(v, 0.25);
        speed.setVelocity(head.x * v, head.z * v);
        ceiling.begin(head);
        p.setDeltaMovement(speed.velX, 0.3, speed.velZ);
        state = MovementState.CEILING_RUNNING;
    }

    // ------------------------------------------------------------------ ceiling

    private boolean doCeiling(PlayerEntity p, MovementInput in, double ix, double iz, boolean jumpPressed, int stage) {
        switch (ceiling.tick(p, in, ix, iz, jumpPressed, speed, stage)) {
            case CONTINUE:
                return true;
            case DROP:
                p.setDeltaMovement(speed.velX, -0.42, speed.velZ);   // jump = opposite to gravity (down)
                state = MovementState.NORMAL;
                wallCooldown = 10;
                return false;
            default:
                state = MovementState.NORMAL;
                wallCooldown = 10;
                return false;
        }
    }

    // ------------------------------------------------------------------ helpers

    private void applyHorizontal(PlayerEntity p) {
        Vector3d dm = p.getDeltaMovement();
        p.setDeltaMovement(speed.velX, dm.y, speed.velZ);
    }

    private void remember(PlayerEntity p) {
        lastX = p.getX();
        lastZ = p.getZ();
        havePos = true;
    }

    private void updateFov(int stage) {
        float target = 1.0F;
        if (PizzaConfig.CLIENT.enableFov.get()) target += (float) (PizzaConfig.CLIENT.fovPerStage.get() * stage);
        fovScale += (target - fovScale) * 0.15F;
    }

    private void sendIfChanged() {
        int st = speed.stage();
        boolean changed = state != lastSentState || st != lastSentStage;
        if (changed || (state != MovementState.NORMAL && ++sinceSend >= 5)) {
            NetworkHandler.sendToServer(new MovementStatePacket((byte) state.ordinal(), (byte) st));
            lastSentState = state;
            lastSentStage = st;
            sinceSend = 0;
        }
    }
}
