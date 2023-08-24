package com.lupicus.vm.item;

import com.lupicus.vm.block.ModBlocks;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.event.CreativeModeTabEvent;
import net.minecraftforge.registries.IForgeRegistry;

public class ModItems
{
	public static final Item VENDING_MACHINE = new BlockItem(ModBlocks.VENDING_MACHINE, new Properties().rarity(Rarity.RARE));

	public static void register(IForgeRegistry<Item> forgeRegistry)
	{
		forgeRegistry.register("vending_machine", VENDING_MACHINE);
	}

	public static void setupTabs(CreativeModeTabEvent.BuildContents event)
	{
		if (event.getTab() == CreativeModeTabs.FUNCTIONAL_BLOCKS)
			event.accept(VENDING_MACHINE);
	}
}
