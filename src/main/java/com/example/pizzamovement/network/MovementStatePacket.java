package com.example.pizzamovement.network;

import com.example.pizzamovement.server.ServerMovementTracker;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: only the claimed state and speed stage. Never a velocity or a position. */
public class MovementStatePacket {
    public final byte state;
    public final byte stage;

    public MovementStatePacket(byte state, byte stage) {
        this.state = state;
        this.stage = stage;
    }

    public static void encode(MovementStatePacket m, PacketBuffer buf) {
        buf.writeByte(m.state);
        buf.writeByte(m.stage);
    }

    public static MovementStatePacket decode(PacketBuffer buf) {
        return new MovementStatePacket(buf.readByte(), buf.readByte());
    }

    public static void handle(MovementStatePacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayerEntity sender = c.getSender();
            if (sender != null) ServerMovementTracker.onClientState(sender, m.state, m.stage);
        });
        c.setPacketHandled(true);
    }
}
