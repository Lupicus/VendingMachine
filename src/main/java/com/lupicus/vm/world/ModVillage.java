package com.lupicus.vm.world;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Function;

import com.lupicus.vm.Main;
import com.mojang.datafixers.util.Pair;

import net.minecraft.data.BuiltinRegistries;
import net.minecraft.data.worldgen.ProcessorLists;
import net.minecraft.data.worldgen.VillagePools;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.structures.LegacySinglePoolElement;
import net.minecraft.world.level.levelgen.feature.structures.StructurePoolElement;
import net.minecraft.world.level.levelgen.feature.structures.StructureTemplatePool;
import net.minecraft.world.level.levelgen.feature.structures.StructureTemplatePool.Projection;
import net.minecraftforge.coremod.api.ASMAPI;

public class ModVillage
{
	@SuppressWarnings({ "unchecked" })
	public static void updatePools()
	{
		String[] biomeList = {"plains", "snowy", "savanna", "desert", "taiga"};
		VillagePools.bootstrap();

		try {
			String name = ASMAPI.mapField("f_69250_"); // templates
			Field field = StructureTemplatePool.class.getDeclaredField(name);
			field.setAccessible(true);
			String name2 = ASMAPI.mapField("f_69249_"); // rawTemplates
			Field field2 = StructureTemplatePool.class.getDeclaredField(name2);
			field2.setAccessible(true);

			for (String biomeName : biomeList)
			{
				String baseName = "village/" + biomeName + "/houses";
				StructureTemplatePool pattern = BuiltinRegistries.TEMPLATE_POOL.get(new ResourceLocation("minecraft:" + baseName));
		        if (pattern == null)
		        	continue;

				Function<Projection, LegacySinglePoolElement> funpiece = StructurePoolElement.legacy(Main.MODID + ":" + baseName + "/" + biomeName + "_vending_machine_1", ProcessorLists.MOSSIFY_10_PERCENT);
				StructurePoolElement piece = funpiece.apply(Projection.RIGID);

				List<StructurePoolElement> list = (List<StructurePoolElement>) field.get(pattern);
				list.add(piece);
				List<Pair<StructurePoolElement, Integer>> list2 = (List<Pair<StructurePoolElement, Integer>>) field2.get(pattern);
				list2.add(Pair.of(piece, 1));
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
