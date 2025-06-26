package com.lupicus.vm;

import org.jetbrains.annotations.NotNull;

import com.lupicus.vm.block.ModBlocks;
import com.lupicus.vm.config.MyConfig;
import com.lupicus.vm.item.ModItems;
import com.lupicus.vm.sound.ModSounds;
import com.lupicus.vm.tileentity.ModTileEntities;
import com.lupicus.vm.tileentity.VendingMachineTileEntity;
import com.lupicus.vm.world.ModVillage;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

@Mod(Main.MODID)
public class Main
{
	public static final String MODID = "vm";

	public Main(FMLJavaModLoadingContext context)
	{
		context.registerConfig(ModConfig.Type.COMMON, MyConfig.COMMON_SPEC);
	}

	@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
	public static class ModEvents
	{
	    @SubscribeEvent
	    public static void onRegister(final RegisterEvent event)
	    {
	    	@NotNull
			ResourceKey<? extends Registry<?>> key = event.getRegistryKey();
	    	if (key.equals(ForgeRegistries.Keys.BLOCKS))
	    		ModBlocks.register(event.getForgeRegistry());
	    	else if (key.equals(ForgeRegistries.Keys.ITEMS))
	    		ModItems.register(event.getForgeRegistry());
	    	else if (key.equals(ForgeRegistries.Keys.BLOCK_ENTITY_TYPES))
	    		ModTileEntities.register(event.getForgeRegistry());
	    	else if (key.equals(ForgeRegistries.Keys.SOUND_EVENTS))
	    		ModSounds.register(event.getForgeRegistry());
	    }

	    @SubscribeEvent
	    public static void onCreativeTab(BuildCreativeModeTabContentsEvent event)
	    {
	    	ModItems.setupTabs(event);
	    }
	}

	@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
	public static class ForgeEvents
	{
		@SubscribeEvent
		public static void onStart(ServerAboutToStartEvent event)
		{
			if (MyConfig.villages)
				ModVillage.updatePools(event.getServer());
			MyConfig.updateTags();
		}

		@SubscribeEvent
		public static void onStop(ServerStoppedEvent event)
		{
			if (!event.getServer().isDedicatedServer())
				VendingMachineTileEntity.clearData();
		}

		@SubscribeEvent
		public static void onTags(final OnDatapackSyncEvent event)
		{
			if (event.getPlayer() == null)
				MyConfig.updateTags();
		}
	}
}
