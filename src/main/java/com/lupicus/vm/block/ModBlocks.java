package com.lupicus.vm.block;

import java.util.function.Function;

import com.lupicus.vm.Main;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.IForgeRegistry;

public class ModBlocks
{
	public static final Block VENDING_MACHINE = create("vending_machine", VendingMachine::new, Properties.of().mapColor(MapColor.METAL).noOcclusion().requiresCorrectToolForDrops().strength(3.5F).sound(SoundType.METAL).isRedstoneConductor(VendingMachine::isNormalCube).lightLevel(VendingMachine::lightValue).pushReaction(PushReaction.BLOCK));

	public static void register(IForgeRegistry<Block> forgeRegistry)
	{
	}

	private static Block create(String name, Function<Properties, Block> func, Properties prop)
	{
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Main.MODID, name));
		return register(key, func, prop);
	}

	// same as Blocks#register
	private static Block register(ResourceKey<Block> key, Function<Properties, Block> func, Properties prop) {
		Block block = func.apply(prop.setId(key));
		return Registry.register(BuiltInRegistries.BLOCK, key, block);
	}

	@OnlyIn(Dist.CLIENT)
	@SuppressWarnings("deprecation")
	public static void setRenderLayer()
	{
		ItemBlockRenderTypes.setRenderLayer(VENDING_MACHINE, RenderType.cutout());
	}
}
