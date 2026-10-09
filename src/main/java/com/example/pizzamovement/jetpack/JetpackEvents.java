package com.example.pizzamovement.jetpack;

import com.example.pizzamovement.config.PizzaConfig;
import com.example.pizzamovement.item.JetpackItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class JetpackEvents {
    /** While the engine is on and block-breaking is enabled, crashing into a wall does not hurt (the wall breaks instead). */
    @SubscribeEvent
    public void onAttack(LivingAttackEvent e) {
        if (e.getSource() != DamageSource.FLY_INTO_WALL) return;
        if (!(e.getEntityLiving() instanceof ServerPlayerEntity)) return;
        ServerPlayerEntity p = (ServerPlayerEntity) e.getEntityLiving();
        if (!PizzaConfig.SERVER.jetpackBreakBlocks.get()) return;
        ItemStack chest = p.getItemBySlot(EquipmentSlotType.CHEST);
        if (!(chest.getItem() instanceof JetpackItem)) return;
        JetpackState.Entry entry = JetpackState.entry(p.getUUID());
        if (JetpackState.isThrusting(entry, p.level.getGameTime())) e.setCanceled(true);
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        PlayerEntity p = e.getPlayer();
        JetpackState.clear(p.getUUID());
    }
}
