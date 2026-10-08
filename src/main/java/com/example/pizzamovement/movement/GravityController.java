package com.example.pizzamovement.movement;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.vector.Vector3d;

/**
 * Custom gravity. DOWN leaves vanilla gravity alone (Y -= 0.08 each tick inside LivingEntity.travel).
 * UP adds a push stronger than that vanilla pull every tick, so the net force points up and the
 * player is pressed into the ceiling; vanilla collision then keeps them hanging flush against it.
 */
public final class GravityController {
    private static final double UP_ACCEL = 0.20;
    private static final double UP_MAX = 0.60;

    private GravityController() {}

    public static void apply(PlayerEntity p, GravityDirection dir) {
        if (dir == GravityDirection.DOWN) return;
        Vector3d dm = p.getDeltaMovement();
        p.setDeltaMovement(dm.x, Math.min(dm.y + UP_ACCEL, UP_MAX), dm.z);
        p.fallDistance = 0.0F;
    }
}
