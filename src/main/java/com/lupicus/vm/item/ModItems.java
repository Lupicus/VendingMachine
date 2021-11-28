package com.lupicus.vm.item;

import com.lupicus.vm.block.ModBlocks;

import net.minecraft.client.color.item.ItemColors;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.IForgeRegistry;

public class ModItems
{
	public static final Item VENDING_MACHINE = new BlockItem(ModBlocks.VENDING_MACHINE, new Properties().tab(CreativeModeTab.TAB_DECORATIONS).rarity(Rarity.RARE)).setRegistryName("vending_machine");

	public static void register(IForgeRegistry<Item> forgeRegistry)
	{
		forgeRegistry.register(VENDING_MACHINE);
	}

	@OnlyIn(Dist.CLIENT)
	public static void register(ItemColors itemColors)
	{
	}
}
