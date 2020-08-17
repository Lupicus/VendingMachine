package com.lupicus.vm.world;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Function;

import com.lupicus.vm.Main;
import com.mojang.datafixers.util.Pair;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.WorldGenRegistries;
import net.minecraft.world.gen.feature.jigsaw.JigsawPattern;
import net.minecraft.world.gen.feature.jigsaw.JigsawPattern.PlacementBehaviour;
import net.minecraft.world.gen.feature.jigsaw.JigsawPiece;
import net.minecraft.world.gen.feature.jigsaw.LegacySingleJigsawPiece;
import net.minecraft.world.gen.feature.structure.VillagesPools;
import net.minecraft.world.gen.feature.template.ProcessorLists;
import net.minecraftforge.coremod.api.ASMAPI;

public class ModVillage
{
	@SuppressWarnings({ "unchecked" })
	public static void updatePools()
	{
		String[] biomeList = {"plains", "snowy", "savanna", "desert", "taiga"};
		VillagesPools.func_244194_a();

		try {
			String name = ASMAPI.mapField("field_214953_e"); // jigsawPieces
			Field field = JigsawPattern.class.getDeclaredField(name);
			field.setAccessible(true);
			String name2 = ASMAPI.mapField("field_214952_d"); // rawTemplates
			Field field2 = JigsawPattern.class.getDeclaredField(name2);
			field2.setAccessible(true);

			for (String biomeName : biomeList)
			{
				String baseName = "village/" + biomeName + "/houses";
		        JigsawPattern pattern = WorldGenRegistries.field_243656_h.getOrDefault(new ResourceLocation("minecraft:" + baseName));
		        if (pattern == null)
		        	continue;

				Function<PlacementBehaviour, LegacySingleJigsawPiece> funpiece = JigsawPiece.func_242851_a(Main.MODID + ":" + baseName + "/" + biomeName + "_vending_machine_1", ProcessorLists.field_244107_g);
		        JigsawPiece piece = funpiece.apply(PlacementBehaviour.RIGID);

				List<JigsawPiece> list = (List<JigsawPiece>) field.get(pattern);
				list.add(piece);
				List<Pair<JigsawPiece, Integer>> list2 = (List<Pair<JigsawPiece, Integer>>) field2.get(pattern);
				list2.add(Pair.of(piece, 1));
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
