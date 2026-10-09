package com.example.pizzamovement.network;

import com.example.pizzamovement.PizzaMovementMod;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.Optional;

public class NetworkHandler {
    private static final String PROTOCOL = "1";
    public static SimpleChannel CHANNEL;

    public static void register() {
        CHANNEL = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(PizzaMovementMod.MODID, "main"))
                .networkProtocolVersion(() -> PROTOCOL)
                .clientAcceptedVersions(PROTOCOL::equals)
                .serverAcceptedVersions(PROTOCOL::equals)
                .simpleChannel();
        CHANNEL.registerMessage(0, MovementStatePacket.class, MovementStatePacket::encode,
                MovementStatePacket::decode, MovementStatePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(1, JetpackThrustPacket.class, JetpackThrustPacket::encode,
                JetpackThrustPacket::decode, JetpackThrustPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static void sendToServer(Object msg) {
        CHANNEL.sendToServer(msg);
    }
}
