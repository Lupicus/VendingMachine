package com.lupicus.vm.world;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.lupicus.vm.Main;

import net.minecraft.core.Holder.Reference;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.ProcessorLists;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.structure.pools.LegacySinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool.Projection;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

public class ModVillage
{
	@SuppressWarnings("unchecked")
	public static void updatePools(MinecraftServer server)
	{
		String[] biomeList = {"plains", "snowy", "savanna", "desert", "taiga"};

		RegistryAccess regs = server.registryAccess();
		Optional<Registry<StructureTemplatePool>> opt = regs.lookup(Registries.TEMPLATE_POOL);
		if (opt.isEmpty())
			return;

		Registry<StructureTemplatePool> reg = opt.get();

		try {
			Field field = StructureTemplatePool.class.getDeclaredField("templates");
			field.setAccessible(true);

			Optional<Reference<StructureProcessorList>> opt3 = Optional.empty();
			Optional<Registry<StructureProcessorList>> opt2 = regs.lookup(Registries.PROCESSOR_LIST);
			if (opt2.isPresent())
				opt3 = opt2.get().get(ProcessorLists.MOSSIFY_10_PERCENT);

			for (String biomeName : biomeList)
			{
				String baseName = "village/" + biomeName + "/houses";
				StructureTemplatePool pattern = reg.getValue(ResourceLocation.parse("minecraft:" + baseName));
				if (pattern == null)
					continue;

				String pieceName = Main.MODID + ":" + baseName + "/" + biomeName + "_vending_machine_1";
				Function<Projection, LegacySinglePoolElement> funpiece;
				if (opt3.isPresent())
					funpiece = StructurePoolElement.legacy(pieceName, opt3.get());
				else
					funpiece = StructurePoolElement.legacy(pieceName);
				StructurePoolElement piece = funpiece.apply(Projection.RIGID);

				List<StructurePoolElement> list = (List<StructurePoolElement>) field.get(pattern);
				list.add(piece);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
