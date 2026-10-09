package com.example.pizzamovement.item;

import com.example.pizzamovement.PizzaMovementMod;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, PizzaMovementMod.MODID);

    public static final RegistryObject<Item> JETPACK = ITEMS.register("jetpack",
            () -> new JetpackItem(new Item.Properties().tab(ItemGroup.TAB_TRANSPORTATION).stacksTo(1)));
}
