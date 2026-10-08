package com.example.pizzamovement.movement;

import com.example.pizzamovement.config.PizzaConfig;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.MovementInput;
import net.minecraft.util.math.vector.Vector3d;

/** Physics of the WALL_RUNNING state (client side, runs before vanilla travel). */
public class WallRunController {
    public enum Result { CONTINUE, EXIT, VAULT, JUMP, CEILING }

    public Direction dir = Direction.NORTH;   // points from the player toward the wall
    public int ticks;
    private int budget;

    public void begin(Direction d, int stage) {
        dir = d;
        ticks = 0;
        budget = PizzaConfig.SERVER.wallRunMaxTicks.get() + stage * 8;
    }

    public Result tick(PlayerEntity p, MovementInput in, double ix, double iz, boolean jumpPressed,
                       SpeedController speed, int stage) {
        PizzaConfig.Server cfg = PizzaConfig.SERVER;
        ticks++;
        Vector3d dm = p.getDeltaMovement();

        if (jumpPressed) return Result.JUMP;
        if (cfg.cancelOnSneak.get() && in.shiftKeyDown) return Result.EXIT;
        if (!cfg.allowWallRunInWater.get() && (p.isInWater() || p.isInLava())) return Result.EXIT;
        if (!CollisionHelper.hasWall(p, dir)) return dm.y > 0.05 ? Result.VAULT : Result.EXIT;
        if (p.isOnGround() && ticks > 4) return Result.EXIT;
        if (ticks > budget) return Result.EXIT;
        if (ticks > 2 && dm.y >= 0.0 && CollisionHelper.hasCeiling(p, 0.25)) return Result.CEILING;

        Vector3d n = CollisionHelper.normal(dir);
        double target = SpeedController.stageSpeed(stage) * cfg.wallRunSpeed.get();
        boolean moving = Math.hypot(ix, iz) > 0.1;

        // movement along the wall = input with the "into the wall" part removed
        double dot = ix * n.x + iz * n.z;
        double tx = ix - dot * n.x;
        double tz = iz - dot * n.z;

        double vy;
        if (moving && target > 0.0) {
            double fade = 1.0 - 0.5 * ((double) ticks / Math.max(1, budget));
            vy = target * 0.85 * fade;
        } else {
            vy = -cfg.wallRunGravity.get();          // reduced gravity: gentle slide
        }
        double hx = tx * target * 0.8 - n.x * 0.05;  // small push into the wall keeps contact
        double hz = tz * target * 0.8 - n.z * 0.05;

        p.setDeltaMovement(hx, vy, hz);
        speed.setVelocity(tx * target * 0.8, tz * target * 0.8);
        p.fallDistance = 0.0F;
        return Result.CONTINUE;
    }
}
