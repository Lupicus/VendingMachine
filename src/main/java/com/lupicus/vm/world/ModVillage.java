package com.lupicus.vm.world;

import java.lang.reflect.Field;
import java.util.List;

import com.google.common.collect.ImmutableList;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.gen.feature.jigsaw.JigsawManager;
import net.minecraft.world.gen.feature.jigsaw.JigsawPattern;
import net.minecraft.world.gen.feature.jigsaw.JigsawPiece;
import net.minecraft.world.gen.feature.jigsaw.SingleJigsawPiece;
import net.minecraft.world.gen.feature.structure.DesertVillagePools;
import net.minecraft.world.gen.feature.structure.PlainsVillagePools;
import net.minecraft.world.gen.feature.structure.SavannaVillagePools;
import net.minecraft.world.gen.feature.structure.SnowyVillagePools;
import net.minecraft.world.gen.feature.structure.TaigaVillagePools;
import net.minecraftforge.coremod.api.ASMAPI;

public class ModVillage
{
	@SuppressWarnings({ "unchecked" })
	public static void updatePools()
	{
		String[] biomeList = {"plains", "snowy", "savanna", "desert", "taiga"};
		PlainsVillagePools.init();
		SnowyVillagePools.init();
		SavannaVillagePools.init();
		DesertVillagePools.init();
		TaigaVillagePools.init();

		try {
			String name = ASMAPI.mapField("field_214953_e"); // jigsawPieces
			Field field = JigsawPattern.class.getDeclaredField(name);
			field.setAccessible(true);
			for (String biomeName : biomeList)
			{
				String baseName = "village/" + biomeName + "/houses";
				SingleJigsawPiece piece = new SingleJigsawPiece("vm:" + baseName + "/" + biomeName + "_vending_machine_1", ImmutableList.of(), JigsawPattern.PlacementBehaviour.RIGID);
		
				JigsawPattern pattern = JigsawManager.REGISTRY.get(new ResourceLocation("minecraft:" + baseName));
				List<JigsawPiece> list = (List<JigsawPiece>) field.get(pattern);
				list.add(piece);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
