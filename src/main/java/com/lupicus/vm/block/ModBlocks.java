package com.lupicus.vm.block;

import net.minecraft.block.AbstractBlock.Properties;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.registries.IForgeRegistry;

public class ModBlocks
{
	public static final Block VENDING_MACHINE = new VendingMachine(Properties.create(Material.IRON).notSolid().func_235861_h_().hardnessAndResistance(3.5F).sound(SoundType.METAL).func_235828_a_(VendingMachine::isNormalCube).func_235838_a_(VendingMachine::lightValue).harvestTool(ToolType.PICKAXE).harvestLevel(3)).setRegistryName("vending_machine");

	public static void register(IForgeRegistry<Block> forgeRegistry)
	{
		forgeRegistry.register(VENDING_MACHINE);
	}

	@OnlyIn(Dist.CLIENT)
	public static void setRenderLayer()
	{
		RenderTypeLookup.setRenderLayer(VENDING_MACHINE, RenderType.getCutout());
	}

	@OnlyIn(Dist.CLIENT)
	public static void register(BlockColors blockColors)
	{
	}
}
