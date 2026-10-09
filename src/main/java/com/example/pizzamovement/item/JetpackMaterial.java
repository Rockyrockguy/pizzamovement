package com.example.pizzamovement.item;

import com.example.pizzamovement.PizzaMovementMod;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.IArmorMaterial;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;

/** Armor material of the jetpack. "Durability" is the fuel tank; blaze powder refuels it on an anvil. */
public class JetpackMaterial implements IArmorMaterial {
    public static final JetpackMaterial INSTANCE = new JetpackMaterial();

    @Override public int getDurabilityForSlot(EquipmentSlotType slot) { return 240; }
    @Override public int getDefenseForSlot(EquipmentSlotType slot) { return slot == EquipmentSlotType.CHEST ? 5 : 0; }
    @Override public int getEnchantmentValue() { return 9; }
    @Override public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_IRON; }
    @Override public Ingredient getRepairIngredient() { return Ingredient.of(Items.BLAZE_POWDER); }
    @Override public String getName() { return PizzaMovementMod.MODID + ":jetpack"; }   // -> textures/models/armor/jetpack_layer_1.png
    @Override public float getToughness() { return 0.0F; }
    @Override public float getKnockbackResistance() { return 0.0F; }
}
