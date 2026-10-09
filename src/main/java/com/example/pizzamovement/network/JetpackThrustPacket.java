package com.example.pizzamovement.network;

import com.example.pizzamovement.item.JetpackItem;
import com.example.pizzamovement.jetpack.JetpackState;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: "the jetpack engine is on right now". Carries no data; the server checks everything itself. */
public class JetpackThrustPacket {
    public static void encode(JetpackThrustPacket m, PacketBuffer buf) {}

    public static JetpackThrustPacket decode(PacketBuffer buf) {
        return new JetpackThrustPacket();
    }

    public static void handle(JetpackThrustPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayerEntity p = c.getSender();
            if (p == null || !p.isFallFlying()) return;
            if (!(p.getItemBySlot(EquipmentSlotType.CHEST).getItem() instanceof JetpackItem)) return;
            if (!JetpackItem.isUsable(p.getItemBySlot(EquipmentSlotType.CHEST))) return;
            JetpackState.entry(p.getUUID()).lastThrust = p.level.getGameTime();
        });
        c.setPacketHandled(true);
    }
}
