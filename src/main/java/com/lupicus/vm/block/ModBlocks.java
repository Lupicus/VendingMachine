package com.lupicus.vm.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.IForgeRegistry;

public class ModBlocks
{
	public static final Block VENDING_MACHINE = new VendingMachine(Properties.of().mapColor(MapColor.METAL).noOcclusion().requiresCorrectToolForDrops().strength(3.5F).sound(SoundType.METAL).isRedstoneConductor(VendingMachine::isNormalCube).lightLevel(VendingMachine::lightValue).pushReaction(PushReaction.BLOCK));

	public static void register(IForgeRegistry<Block> forgeRegistry)
	{
		forgeRegistry.register("vending_machine", VENDING_MACHINE);
	}
}
