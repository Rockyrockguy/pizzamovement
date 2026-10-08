package com.example.pizzamovement.movement;

import com.example.pizzamovement.config.PizzaConfig;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.MovementInput;
import net.minecraft.util.math.vector.Vector3d;

/** Physics of the CEILING_RUNNING state: gravity points UP, the player hangs flush under a valid ceiling. */
public class CeilingRunController {
    public enum Result { CONTINUE, EXIT, DROP }

    private int transition;          // ticks of smooth heading change after leaving the wall
    private int lost;                // ticks without a ceiling above
    private int ticks;
    private double headX, headZ;

    public void begin(Vector3d heading) {
        headX = heading.x;
        headZ = heading.z;
        transition = 8;
        lost = 0;
        ticks = 0;
    }

    public Result tick(PlayerEntity p, MovementInput in, double ix, double iz, boolean jumpPressed,
                       SpeedController speed, int stage) {
        PizzaConfig.Server cfg = PizzaConfig.SERVER;
        ticks++;

        if (jumpPressed) return Result.DROP;                      // jump = push away from the ceiling (down)
        if (cfg.cancelOnSneak.get() && in.shiftKeyDown) return Result.EXIT;
        if (!cfg.allowCeilingRunInWater.get() && (p.isInWater() || p.isInLava())) return Result.EXIT;
        if (!CollisionHelper.hasCeiling(p, 0.3)) {
            if (++lost > 3) return Result.EXIT;
        } else {
            lost = 0;
        }
        if (transition == 0 && speed.speed() < 0.06) return Result.EXIT;   // ran out of speed: fall

        GravityController.apply(p, GravityDirection.UP);

        double target = SpeedController.stageSpeed(stage) * cfg.ceilingRunSpeed.get();
        double dx = ix, dz = iz, turn = cfg.turnRate.get();
        if (transition > 0) {                                      // smooth wall -> ceiling corner
            dx = headX;
            dz = headZ;
            turn = 0.35;
            transition--;
        }
        speed.step(dx, dz, target, turn);
        Vector3d dm = p.getDeltaMovement();
        p.setDeltaMovement(speed.velX, dm.y, speed.velZ);
        p.fallDistance = 0.0F;
        return Result.CONTINUE;
    }
}
