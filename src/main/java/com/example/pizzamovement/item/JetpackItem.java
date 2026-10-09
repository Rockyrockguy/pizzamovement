package com.example.pizzamovement.item;

import com.example.pizzamovement.jetpack.JetpackBreaker;
import com.example.pizzamovement.jetpack.JetpackState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;

/**
 * Worn in the chest slot like an elytra: vanilla starts and steers the flight (jump in the air, fly where you look).
 * The engine itself (holding Jump while flying) is applied on the client, see JetpackClient.
 * This class runs on both sides but only the server part below does anything: fuel, flame particles, block breaking.
 */
public class JetpackItem extends ArmorItem {
    public JetpackItem(Properties props) {
        super(JetpackMaterial.INSTANCE, EquipmentSlotType.CHEST, props);
    }

    /** Usable while at least one point of fuel is left. */
    public static boolean isUsable(ItemStack stack) {
        return stack.getDamageValue() < stack.getMaxDamage() - 1;
    }

    @Override
    public boolean canElytraFly(ItemStack stack, LivingEntity entity) {
        return isUsable(stack);
    }

    @Override
    public boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
        if (!isUsable(stack)) return false;
        if (entity.level.isClientSide || !(entity instanceof ServerPlayerEntity)) return true;

        ServerPlayerEntity p = (ServerPlayerEntity) entity;
        JetpackState.Entry e = JetpackState.entry(p.getUUID());
        boolean thrusting = JetpackState.isThrusting(e, p.level.getGameTime());

        if (thrusting) {
            if (flightTicks % 10 == 0) {
                stack.hurtAndBreak(1, p, pl -> pl.broadcastBreakEvent(EquipmentSlotType.CHEST));   // fuel use
            }
            if (p.level instanceof ServerWorld) {
                Vector3d back = p.position().add(0.0, p.getBbHeight() * 0.6, 0.0).subtract(p.getLookAngle().scale(0.6));
                ((ServerWorld) p.level).sendParticles(ParticleTypes.FLAME, back.x, back.y, back.z, 3, 0.1, 0.1, 0.1, 0.02);
                ((ServerWorld) p.level).sendParticles(ParticleTypes.SMOKE, back.x, back.y, back.z, 1, 0.1, 0.1, 0.1, 0.01);
            }
            JetpackBreaker.tick(p, stack, e);
        }
        e.remember(p.getX(), p.getY(), p.getZ());
        return true;
    }
}
