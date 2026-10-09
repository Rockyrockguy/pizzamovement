package com.example.pizzamovement;

import com.example.pizzamovement.client.ClientEvents;
import com.example.pizzamovement.config.PizzaConfig;
import com.example.pizzamovement.item.ModItems;
import com.example.pizzamovement.jetpack.JetpackEvents;
import com.example.pizzamovement.network.NetworkHandler;
import com.example.pizzamovement.server.ServerMovementTracker;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(PizzaMovementMod.MODID)
public class PizzaMovementMod {
    public static final String MODID = "pizzamovement";

    public PizzaMovementMod() {
        // SERVER config is synced from the server to every client, so everyone simulates with the same numbers.
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, PizzaConfig.SERVER_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, PizzaConfig.CLIENT_SPEC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        ModItems.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.register(new ServerMovementTracker());
        MinecraftForge.EVENT_BUS.register(new JetpackEvents());
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ClientEvents::register);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        NetworkHandler.register();
    }
}
