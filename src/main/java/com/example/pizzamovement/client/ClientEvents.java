package com.example.pizzamovement.client;

import com.example.pizzamovement.config.PizzaConfig;
import com.example.pizzamovement.movement.MovementController;
import com.example.pizzamovement.movement.MovementState;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Client-only hooks. Loaded only through DistExecutor, so a dedicated server never touches this class. */
public class ClientEvents {
    public static void register() {
        MinecraftForge.EVENT_BUS.register(new ClientEvents());
    }

    @SubscribeEvent
    public void onInput(InputUpdateEvent e) {
        if (e.getPlayer() instanceof ClientPlayerEntity) {
            MovementController.INSTANCE.onInput((ClientPlayerEntity) e.getPlayer(), e.getMovementInput());
        }
    }

    @SubscribeEvent
    public void onFov(FOVUpdateEvent e) {
        if (!PizzaConfig.CLIENT.enableFov.get()) return;
        if (e.getEntity() != Minecraft.getInstance().player) return;
        e.setNewfov(e.getNewfov() * MovementController.INSTANCE.fovScale);
    }

    @SubscribeEvent
    public void onLogout(ClientPlayerNetworkEvent.LoggedOutEvent e) {
        MovementController.INSTANCE.hardReset();
    }

    @SubscribeEvent
    public void onHud(RenderGameOverlayEvent.Post e) {
        if (e.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        if (!PizzaConfig.CLIENT.enableHud.get()) return;
        MovementController c = MovementController.INSTANCE;
        int stage = c.stage();
        if (stage == 0 && c.state == MovementState.NORMAL) return;   // only while accelerating

        Minecraft mc = Minecraft.getInstance();
        FontRenderer font = mc.font;
        MatrixStack ms = e.getMatrixStack();
        StringBuilder bar = new StringBuilder("RUN: [");
        for (int i = 1; i <= 4; i++) bar.append(i <= stage ? '#' : '-');
        bar.append(']');
        int x = 8;
        int y = mc.getWindow().getGuiScaledHeight() - 52;
        int color = stage >= 4 ? 0xFFAA00 : 0xFFFFFF;
        font.drawShadow(ms, "SPEED STAGE: " + stage, x, y, color);
        font.drawShadow(ms, bar.toString(), x, y + 10, color);
        if (c.state == MovementState.WALL_RUNNING || c.state == MovementState.CEILING_RUNNING) {
            font.drawShadow(ms, c.state.name().replace('_', ' '), x, y + 20, 0x55FFFF);
        }
    }
}
