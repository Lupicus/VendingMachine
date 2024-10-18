package com.lupicus.vm;

import com.lupicus.vm.block.ModBlocks;
import com.lupicus.vm.config.MyConfig;
import com.lupicus.vm.item.ModItems;
import com.lupicus.vm.sound.ModSounds;
import com.lupicus.vm.tileentity.ModTileEntities;
import com.lupicus.vm.world.ModVillage;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Main.MODID)
public class Main
{
	public static final String MODID = "vm";

	public Main()
	{
		FMLJavaModLoadingContext.get().getModEventBus().register(this);
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, MyConfig.COMMON_SPEC);
	}

	@SubscribeEvent
	public void setupCommon(final FMLCommonSetupEvent event)
	{
		if (MyConfig.villages)
			event.enqueueWork(() -> ModVillage.updatePools());
	}

	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
	public void setupClient(final FMLClientSetupEvent event)
	{
		ModBlocks.setRenderLayer();
	}

	@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
	public static class ModEvents
	{
		@SubscribeEvent
		public static void onItemsRegistry(final RegistryEvent.Register<Item> event)
		{
			ModItems.register(event.getRegistry());
		}

		@SubscribeEvent
		public static void onBlocksRegistry(final RegistryEvent.Register<Block> event)
		{
			ModBlocks.register(event.getRegistry());
		}

		@OnlyIn(Dist.CLIENT)
		@SubscribeEvent
		public static void onColorsRegistry(final ColorHandlerEvent.Item event)
		{
			ModItems.register(event.getItemColors());
		}

		@SubscribeEvent
		public static void onSoundRegistry(final RegistryEvent.Register<SoundEvent> event)
		{
			ModSounds.register(event.getRegistry());
		}

		@SubscribeEvent
		public static void onTileEntitiesRegistry(final RegistryEvent.Register<BlockEntityType<?>> event)
		{
			ModTileEntities.register(event.getRegistry());
		}
	}

	@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
	public static class ForgeEvents
	{
		@SubscribeEvent
		public static void onTags(final TagsUpdatedEvent event)
		{
			MyConfig.updateTags();
		}
	}
}
