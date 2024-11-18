package com.lupicus.vm.sound;

import com.lupicus.vm.Main;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.IForgeRegistry;

public class ModSounds
{
	public static final SoundEvent VENDING_MACHINE_TAKE_RESULT = create("block.vm.vending_machine.take_result");

	private static SoundEvent create(String key)
	{
		ResourceLocation res = ResourceLocation.fromNamespaceAndPath(Main.MODID, key);
		SoundEvent ret = SoundEvent.createVariableRangeEvent(res);
		return ret;
	}

	public static void register(IForgeRegistry<SoundEvent> registry)
	{
		registry.register(VENDING_MACHINE_TAKE_RESULT.location(), VENDING_MACHINE_TAKE_RESULT);
	}
}
