package com.example.pizzamovement.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/** SERVER config = physics (synced to clients). CLIENT config = purely visual options. Speeds are blocks per tick. */
public class PizzaConfig {
    public static final ForgeConfigSpec SERVER_SPEC;
    public static final Server SERVER;
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;

    static {
        Pair<Server, ForgeConfigSpec> s = new ForgeConfigSpec.Builder().configure(Server::new);
        SERVER = s.getLeft();
        SERVER_SPEC = s.getRight();
        Pair<Client, ForgeConfigSpec> c = new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT = c.getLeft();
        CLIENT_SPEC = c.getRight();
    }

    public static class Server {
        public final ForgeConfigSpec.DoubleValue stage1Seconds, stage2Seconds, stage3Seconds, stage4Seconds;
        public final ForgeConfigSpec.DoubleValue stage1Speed, stage2Speed, stage3Speed, stage4Speed;
        public final ForgeConfigSpec.DoubleValue acceleration, deceleration, maxSpeed, turnRate, airTurnRate, impactSpeedKeep;
        public final ForgeConfigSpec.IntValue chargeDecayPerTick;
        public final ForgeConfigSpec.DoubleValue wallRunSpeed, wallRunGravity, wallJumpForce, wallJumpVerticalForce, ceilingRunSpeed, exitFallGrace;
        public final ForgeConfigSpec.IntValue wallRunMaxTicks, wallRunMinStage, wallJumpCooldownTicks;
        public final ForgeConfigSpec.BooleanValue cancelOnSneak, allowWallRunInWater, allowCeilingRunInWater, enableParticles;
        public final ForgeConfigSpec.DoubleValue jetpackThrust, jetpackMaxSpeed, jetpackBreakSpeedFraction, jetpackBreakMaxHardness;
        public final ForgeConfigSpec.BooleanValue jetpackBreakBlocks;

        Server(ForgeConfigSpec.Builder b) {
            b.push("speedStages");
            stage1Seconds = b.comment("Seconds of continuous running to reach stage 1").defineInRange("stage1Seconds", 0.5, 0.05, 60.0);
            stage2Seconds = b.defineInRange("stage2Seconds", 1.0, 0.05, 60.0);
            stage3Seconds = b.defineInRange("stage3Seconds", 1.5, 0.05, 60.0);
            stage4Seconds = b.defineInRange("stage4Seconds", 2.0, 0.05, 60.0);
            stage1Speed = b.comment("Horizontal speed in blocks per tick (vanilla sprint is about 0.28)").defineInRange("stage1Speed", 0.30, 0.05, 3.0);
            stage2Speed = b.defineInRange("stage2Speed", 0.38, 0.05, 3.0);
            stage3Speed = b.defineInRange("stage3Speed", 0.48, 0.05, 3.0);
            stage4Speed = b.defineInRange("stage4Speed", 0.60, 0.05, 3.0);
            chargeDecayPerTick = b.comment("Charge ticks lost per tick when you stop running").defineInRange("chargeDecayPerTick", 2, 1, 100);
            b.pop();

            b.push("momentum");
            acceleration = b.comment("Fraction of the gap to the target speed closed each tick").defineInRange("acceleration", 0.12, 0.005, 1.0);
            deceleration = b.defineInRange("deceleration", 0.06, 0.005, 1.0);
            maxSpeed = b.comment("Hard cap, never exceeded").defineInRange("maxSpeed", 0.75, 0.1, 3.0);
            turnRate = b.comment("How fast the heading follows your input on ground (speed is kept while turning)").defineInRange("turnRate", 0.22, 0.01, 1.0);
            airTurnRate = b.defineInRange("airTurnRate", 0.08, 0.01, 1.0);
            impactSpeedKeep = b.comment("Fraction of speed kept after hitting a wall head-on").defineInRange("impactSpeedKeep", 0.6, 0.0, 1.0);
            b.pop();

            b.push("wall");
            wallRunSpeed = b.comment("Multiplier of the current stage speed while wall-running").defineInRange("wallRunSpeed", 0.9, 0.1, 3.0);
            wallRunGravity = b.comment("Slide-down speed (blocks/tick) while wall-running without moving").defineInRange("wallRunGravity", 0.04, 0.0, 1.0);
            wallRunMaxTicks = b.defineInRange("wallRunMaxTicks", 50, 5, 1000);
            wallRunMinStage = b.defineInRange("wallRunMinStage", 1, 0, 4);
            wallJumpForce = b.defineInRange("wallJumpForce", 0.55, 0.0, 3.0);
            wallJumpVerticalForce = b.defineInRange("wallJumpVerticalForce", 0.55, 0.0, 3.0);
            wallJumpCooldownTicks = b.defineInRange("wallJumpCooldownTicks", 12, 0, 200);
            b.pop();

            b.push("ceiling");
            ceilingRunSpeed = b.comment("Multiplier of the current stage speed while ceiling-running").defineInRange("ceilingRunSpeed", 1.0, 0.1, 3.0);
            b.pop();

            b.push("rules");
            cancelOnSneak = b.define("cancelOnSneak", true);
            allowWallRunInWater = b.define("allowWallRunInWater", false);
            allowCeilingRunInWater = b.define("allowCeilingRunInWater", false);
            exitFallGrace = b.comment("Extra fall distance forgiven when you leave a wall or ceiling").defineInRange("exitFallGrace", 2.0, 0.0, 20.0);
            enableParticles = b.define("enableParticles", true);
            b.pop();

            b.push("jetpack");
            jetpackThrust = b.comment("Acceleration (blocks/tick^2) along the view direction while Jump is held in flight").defineInRange("jetpackThrust", 0.07, 0.0, 1.0);
            jetpackMaxSpeed = b.comment("Top speed in blocks per tick (2.0 = 40 blocks/s)").defineInRange("jetpackMaxSpeed", 2.0, 0.2, 4.0);
            jetpackBreakBlocks = b.comment("Break blocks in front at (almost) top speed with the engine on").define("jetpackBreakBlocks", true);
            jetpackBreakSpeedFraction = b.comment("Fraction of jetpackMaxSpeed needed to start breaking blocks").defineInRange("jetpackBreakSpeedFraction", 0.9, 0.1, 1.0);
            jetpackBreakMaxHardness = b.comment("Blocks harder than this are not broken (stone 1.5, iron ore 3, obsidian 50)").defineInRange("jetpackBreakMaxHardness", 5.0, 0.0, 100.0);
            b.pop();
        }
    }

    public static class Client {
        public final ForgeConfigSpec.BooleanValue enableFov, enableHud, enableJetpackCameraRoll;
        public final ForgeConfigSpec.DoubleValue fovPerStage, jetpackCameraRoll, jetpackCameraRollMax;

        Client(ForgeConfigSpec.Builder b) {
            enableFov = b.define("enableFov", true);
            fovPerStage = b.defineInRange("fovPerStage", 0.03, 0.0, 0.2);
            enableHud = b.define("enableHud", true);
            enableJetpackCameraRoll = b.define("enableJetpackCameraRoll", true);
            jetpackCameraRoll = b.comment("Roll degrees per degree of turn per tick; negative inverts the tilt").defineInRange("jetpackCameraRoll", 1.2, -10.0, 10.0);
            jetpackCameraRollMax = b.defineInRange("jetpackCameraRollMax", 25.0, 0.0, 90.0);
        }
    }
}
