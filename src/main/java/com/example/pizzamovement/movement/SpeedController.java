package com.example.pizzamovement.movement;

import com.example.pizzamovement.config.PizzaConfig;
import net.minecraft.util.math.vector.Vector3d;

/** Owns the run charge (-> stage 0..4) and the horizontal velocity vector (momentum). Speeds are blocks/tick. */
public class SpeedController {
    public int chargeTicks;
    public double velX, velZ;
    public double lastCommanded;

    private static int ticks(double seconds) {
        return (int) Math.round(seconds * 20.0);
    }

    public static int stageForTicks(int t) {
        PizzaConfig.Server c = PizzaConfig.SERVER;
        if (t >= ticks(c.stage4Seconds.get())) return 4;
        if (t >= ticks(c.stage3Seconds.get())) return 3;
        if (t >= ticks(c.stage2Seconds.get())) return 2;
        if (t >= ticks(c.stage1Seconds.get())) return 1;
        return 0;
    }

    public static double stageSpeed(int stage) {
        PizzaConfig.Server c = PizzaConfig.SERVER;
        double v;
        switch (stage) {
            case 1: v = c.stage1Speed.get(); break;
            case 2: v = c.stage2Speed.get(); break;
            case 3: v = c.stage3Speed.get(); break;
            case 4: v = c.stage4Speed.get(); break;
            default: return 0.0;
        }
        return Math.min(v, c.maxSpeed.get());
    }

    public int stage() {
        return stageForTicks(chargeTicks);
    }

    public void tickCharge(boolean running) {
        if (running) chargeTicks++;
        else chargeTicks = Math.max(0, chargeTicks - PizzaConfig.SERVER.chargeDecayPerTick.get());
    }

    public double speed() {
        return Math.hypot(velX, velZ);
    }

    public void setVelocity(double x, double z) {
        double max = PizzaConfig.SERVER.maxSpeed.get();
        double l = Math.hypot(x, z);
        if (l > max) {
            x = x / l * max;
            z = z / l * max;
        }
        velX = x;
        velZ = z;
        lastCommanded = speed();
    }

    public void syncFrom(Vector3d dm) {
        velX = dm.x;
        velZ = dm.z;
        lastCommanded = speed();
    }

    public void reset() {
        chargeTicks = 0;
        velX = velZ = lastCommanded = 0.0;
    }

    /**
     * One momentum step. (dirX, dirZ) is the desired heading (any length; zero = coast).
     * The scalar speed approaches 'target' with ACCELERATION/DECELERATION; the heading rotates toward the
     * desired one by 'turn' per tick WITHOUT losing speed, which is what keeps momentum through turns.
     */
    public void step(double dirX, double dirZ, double target, double turn) {
        PizzaConfig.Server c = PizzaConfig.SERVER;
        double s = speed();
        double dl = Math.hypot(dirX, dirZ);

        if (dl < 1.0E-4) {                       // no input: coast down
            double ns = s * (1.0 - c.deceleration.get());
            if (ns < 0.02 || s < 1.0E-6) {
                velX = velZ = 0.0;
            } else {
                double k = ns / s;
                velX *= k;
                velZ *= k;
            }
            lastCommanded = speed();
            return;
        }
        dirX /= dl;
        dirZ /= dl;

        double max = c.maxSpeed.get();
        double goal = Math.min(target, max);
        double ns;
        if (goal > s) {
            ns = s + Math.max((goal - s) * c.acceleration.get(), 0.004);
            ns = Math.min(ns, goal);
        } else {
            ns = s + (goal - s) * c.deceleration.get();
        }
        ns = Math.min(ns, max);

        double ux, uz;
        if (s < 1.0E-3) {
            ux = dirX;
            uz = dirZ;
        } else {
            ux = velX / s * (1.0 - turn) + dirX * turn;
            uz = velZ / s * (1.0 - turn) + dirZ * turn;
            double ul = Math.hypot(ux, uz);
            if (ul < 1.0E-4) {
                ux = dirX;
                uz = dirZ;
            } else {
                ux /= ul;
                uz /= ul;
            }
        }
        velX = ux * ns;
        velZ = uz * ns;
        lastCommanded = ns;
    }

    /** Head-on wall hit: lose part of the speed and charge instead of stopping dead. */
    public void impact() {
        double keep = PizzaConfig.SERVER.impactSpeedKeep.get();
        velX *= keep;
        velZ *= keep;
        chargeTicks = (int) (chargeTicks * 0.75);
        lastCommanded = speed();
    }
}
