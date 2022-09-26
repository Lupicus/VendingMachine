package com.lupicus.vm.item;

import com.lupicus.vm.block.ModBlocks;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.IForgeRegistry;

public class ModItems
{
	public static final Item VENDING_MACHINE = new BlockItem(ModBlocks.VENDING_MACHINE, new Properties().tab(CreativeModeTab.TAB_DECORATIONS).rarity(Rarity.RARE));

	public static void register(IForgeRegistry<Item> forgeRegistry)
	{
		forgeRegistry.register("vending_machine", VENDING_MACHINE);
	}
}
