package com.lupicus.vm.block;

import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.IForgeRegistry;

public class ModBlocks
{
	public static final Block VENDING_MACHINE = new VendingMachine(Properties.of(Material.METAL).noOcclusion().requiresCorrectToolForDrops().strength(3.5F).sound(SoundType.METAL).isRedstoneConductor(VendingMachine::isNormalCube).lightLevel(VendingMachine::lightValue)).setRegistryName("vending_machine");

	public static void register(IForgeRegistry<Block> forgeRegistry)
	{
		forgeRegistry.register(VENDING_MACHINE);
	}

	@OnlyIn(Dist.CLIENT)
	public static void setRenderLayer()
	{
		ItemBlockRenderTypes.setRenderLayer(VENDING_MACHINE, RenderType.cutout());
	}

	@OnlyIn(Dist.CLIENT)
	public static void register(BlockColors blockColors)
	{
	}
}
