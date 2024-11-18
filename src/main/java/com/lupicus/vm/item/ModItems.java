package com.lupicus.vm.item;

import java.util.function.BiFunction;

import com.lupicus.vm.block.ModBlocks;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.IForgeRegistry;

public class ModItems
{
	public static final Item VENDING_MACHINE = create(ModBlocks.VENDING_MACHINE, BlockItem::new, new Properties().rarity(Rarity.RARE));

	public static void register(IForgeRegistry<Item> forgeRegistry)
	{
	}

	private static Item create(Block block, BiFunction<Block, Properties, Item> func, Properties prop)
	{
		return Items.registerBlock(block, func, prop);
	}

	public static void setupTabs(BuildCreativeModeTabContentsEvent event)
	{
		if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS)
			event.accept(VENDING_MACHINE);
	}
}
