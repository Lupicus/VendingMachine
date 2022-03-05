package com.lupicus.vm.datafix;

import java.lang.reflect.Field;

import org.slf4j.Logger;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.DataFixerBuilder;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.logging.LogUtils;

import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class ModFixers
{
	private static Logger LOGGER = LogUtils.getLogger();

	@SuppressWarnings("unchecked")
	public static void apply(DataFixerBuilder builder)
	{
		Int2ObjectSortedMap<Schema> schemas = null;

		try {
			Field fld = builder.getClass().getDeclaredField("schemas");
			fld.setAccessible(true);
			schemas = (Int2ObjectSortedMap<Schema>) fld.get(builder);
		}
		catch (Exception e) {
		}
		if (schemas == null || schemas.size() == 0)
		{
			LOGGER.error("failed to install mod datafixers");
			return;
		}

		int version = 2730; // 1.17.1
		int key = DataFixUtils.makeKey(version);
		Schema s = schemas.get(key);
		if (s == null)
			s = builder.addSchema(version, NamespacedSchema::new);
		builder.addFixer(new RotationFix(s, false));
	}
}
