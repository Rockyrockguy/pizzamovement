package com.example.pizzamovement.client;

import com.example.pizzamovement.config.PizzaConfig;
import com.example.pizzamovement.item.JetpackItem;
import com.example.pizzamovement.network.JetpackThrustPacket;
import com.example.pizzamovement.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.event.TickEvent;

/** Client side of the jetpack: engine thrust along the camera direction and the banking camera roll. */
public final class JetpackClient {
    private static int sendTimer;
    private static float lastYaw;
    private static float roll;
    private static boolean haveYaw;

    private JetpackClient() {}

    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.START || !(e.player instanceof ClientPlayerEntity)) return;
        Minecraft mc = Minecraft.getInstance();
        ClientPlayerEntity p = (ClientPlayerEntity) e.player;
        if (p != mc.player) return;

        PizzaConfig.Client cc = PizzaConfig.CLIENT;
        ItemStack chest = p.getItemBySlot(EquipmentSlotType.CHEST);
        boolean flying = p.isFallFlying() && chest.getItem() instanceof JetpackItem && JetpackItem.isUsable(chest);

        // --- camera roll (bank into turns) ---
        float targetRoll = 0.0F;
        if (flying && cc.enableJetpackCameraRoll.get()) {
            float dYaw = haveYaw ? MathHelper.wrapDegrees(p.yRot - lastYaw) : 0.0F;
            float max = cc.jetpackCameraRollMax.get().floatValue();
            targetRoll = MathHelper.clamp(dYaw * cc.jetpackCameraRoll.get().floatValue(), -max, max);
        }
        lastYaw = p.yRot;
        haveYaw = true;
        roll += (targetRoll - roll) * 0.2F;

        // --- engine: hold Jump while gliding to accelerate toward where the camera looks ---
        if (!flying || !mc.options.keyJump.isDown()) {
            sendTimer = 0;
            return;
        }
        PizzaConfig.Server cfg = PizzaConfig.SERVER;
        Vector3d look = p.getLookAngle();
        Vector3d nd = p.getDeltaMovement().add(look.scale(cfg.jetpackThrust.get()));
        double max = cfg.jetpackMaxSpeed.get();
        double len = nd.length();
        if (len > max) nd = nd.scale(max / len);
        p.setDeltaMovement(nd);

        if (sendTimer-- <= 0) {                 // tell the server the engine is on (fuel, particles, block breaking)
            NetworkHandler.sendToServer(new JetpackThrustPacket());
            sendTimer = 3;
        }
    }

    public static void onCamera(EntityViewRenderEvent.CameraSetup e) {
        if (Math.abs(roll) > 0.01F) e.setRoll(e.getRoll() + roll);
    }

    public static void reset() {
        roll = 0.0F;
        haveYaw = false;
        sendTimer = 0;
    }
}
