package com.lupicus.vm.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lupicus.vm.Main;

import net.minecraft.entity.EntityType;
import net.minecraft.item.DyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.Rarity;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;

@Mod.EventBusSubscriber(modid = Main.MODID, bus=Mod.EventBusSubscriber.Bus.MOD)
public class MyConfig
{
	private static final Logger LOGGER = LogManager.getLogger();
	public static final Common COMMON;
	public static final ForgeConfigSpec COMMON_SPEC;
	static
	{
		final Pair<Common, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Common::new);
		COMMON_SPEC = specPair.getRight();
		COMMON = specPair.getLeft();
	}

	public static boolean restock;
	public static boolean fixed;
	public static boolean minable;
	public static boolean includeAllItems;
	public static HashSet<Item> excludeItemSet;
	public static HashSet<Item> includeItemSet;
	public static HashSet<String> excludeModSet;
	public static HashSet<String> includeModSet;
	public static HashSet<String> excludeGroupSet;
	public static HashSet<String> includeGroupSet;
	public static HashMap<Item, Rarity> itemRarityMap;
	public static int commonCost;
	public static int uncommonCost;
	public static int rareCost;
	public static int epicCost;
	public static int commonUses;
	public static int uncommonUses;
	public static int rareUses;
	public static int epicUses;
	public static Item commonItem;
	public static Item uncommonItem;
	public static Item rareItem;
	public static Item epicItem;
	public static boolean[] fixedExtended = new boolean[7];
	public static Item[] fixedItems = new Item[7];
	public static int[] fixedAmount = new int[7];
	public static int[] fixedUses = new int[7];
	public static ItemStack[] fixedPayment = new ItemStack[7];

	@SubscribeEvent
	public static void onModConfigEvent(final ModConfig.ModConfigEvent configEvent)
	{
		if (configEvent.getConfig().getSpec() == MyConfig.COMMON_SPEC)
		{
			bakeConfig();
		}
	}

	public static void bakeConfig()
	{
		restock = COMMON.restock.get();
		fixed = COMMON.fixed.get();
		minable = COMMON.minable.get();
		commonCost = COMMON.commonCost.get();
		uncommonCost = COMMON.uncommonCost.get();
		rareCost = COMMON.rareCost.get();
		epicCost = COMMON.epicCost.get();
		commonUses = COMMON.commonUses.get();
		uncommonUses = COMMON.uncommonUses.get();
		rareUses = COMMON.rareUses.get();
		epicUses = COMMON.epicUses.get();
		commonItem = getItem(COMMON.commonItem.get());
		uncommonItem = getItem(COMMON.uncommonItem.get());
		rareItem = getItem(COMMON.rareItem.get());
		epicItem = getItem(COMMON.epicItem.get());
		extractFixed(extract(COMMON.fixedItems.get()));
		includeModSet = stringSet(extract(COMMON.includeMods.get()));
		excludeModSet = stringSet(extract(COMMON.excludeMods.get()));
		String[] temp = extract(COMMON.includeItems.get());
		includeAllItems = hasAll(temp);
		if (includeAllItems)
			temp = new String[0];
		includeItemSet = itemSet(temp, "IncludeItems");
		excludeItemSet = itemSet(extract(COMMON.excludeItems.get()), "ExcludeItems");
		includeGroupSet = stringSet(extract(COMMON.includeGroups.get()));
		excludeGroupSet = stringSet(extract(COMMON.excludeGroups.get()));
		itemRarityMap = itemMap(extract(COMMON.itemRarity.get()));
		validateMods(includeModSet, "IncludeMods");
		validateMods(excludeModSet, "ExcludeMods");
		validateGroups(includeGroupSet, "IncludeGroups");
		validateGroups(excludeGroupSet, "ExcludeGroups");
	}

	private static boolean hasAll(String[] values)
	{
		for (String name : values)
		{
			if (name.equals("*"))
				return true;
		}
		return false;
	}

	private static Item getItem(String name)
	{
		Item ret = Items.EMERALD;
		try {
			ResourceLocation key = new ResourceLocation(name);
			if (ForgeRegistries.ITEMS.containsKey(key))
			{
				ret = ForgeRegistries.ITEMS.getValue(key);
			}
			else
				LOGGER.warn("Unknown item: " + name);
		}
		catch (Exception e)
		{
			LOGGER.warn("Bad item: " + name);
		}
		return ret;
	}

	private static void extractFixed(String[] values)
	{
		for (int i = 0; i < fixedItems.length; ++i)
		{
			fixedExtended[i] = false;
			fixedItems[i] = Items.AIR;
			if (i >= values.length)
				continue;
			try {
				String[] part = values[i].split(",");
				if (part.length == 5)
				{
					fixedExtended[i] = true;
					fixedUses[i] = commonUses;
					fixedPayment[i] = new ItemStack(commonItem, commonCost);
					fixedAmount[i] = 1;
					ResourceLocation key = new ResourceLocation(part[0]);
					if (ForgeRegistries.ITEMS.containsKey(key))
						fixedItems[i] = ForgeRegistries.ITEMS.getValue(key);
					else
						LOGGER.warn("Unknown item: " + key.toString());
					fixedAmount[i] = Integer.parseInt(part[1]);
					key = new ResourceLocation(part[2]);
					if (ForgeRegistries.ITEMS.containsKey(key))
					{
						Item payItem = ForgeRegistries.ITEMS.getValue(key);
						int cost = Integer.parseInt(part[3]);
						fixedPayment[i] = new ItemStack(payItem, cost);
					}
					else
						LOGGER.warn("Unknown item: " + key.toString());
					fixedUses[i] = Integer.parseInt(part[4]);
				}
				else
				{
					if (part.length != 1)
						LOGGER.warn("Bad number of subfields: ", values[i]);
					ResourceLocation key = new ResourceLocation(part[0]);
					if (ForgeRegistries.ITEMS.containsKey(key))
						fixedItems[i] = ForgeRegistries.ITEMS.getValue(key);
					else
						LOGGER.warn("Unknown item: " + key.toString());
				}
			}
			catch (Exception e) {
				LOGGER.warn("Bad entry: " + values[i]);
			}
		}
	}

	private static HashSet<String> stringSet(String[] values)
	{
		HashSet<String> set = new HashSet<>();
		for (int i = 0; i < values.length; ++i)
		{
			set.add(values[i].trim());
		}
		return set;
	}

	private static HashSet<Item> itemSet(String[] values, String configName)
	{
		HashSet<Item> ret = new HashSet<>();
		IForgeRegistry<Item> reg = ForgeRegistries.ITEMS;
		for (String name : values)
		{
			List<String> list = expandItem(name);
			for (String entry : list)
			{
				try {
					ResourceLocation key = new ResourceLocation(entry);
					if (reg.containsKey(key))
					{
						Item item = reg.getValue(key);
						ret.add(item);
					}
					else
						LOGGER.warn("Unknown entry in " + configName + ": " + entry);
				}
				catch (Exception e)
				{
					LOGGER.warn("Bad entry in " + configName + ": " + entry);
				}
			}
		}
		return ret;
	}

	private static HashMap<Item, Rarity> itemMap(String[] values)
	{
		HashMap<Item, Rarity> ret = new HashMap<>();
		IForgeRegistry<Item> reg = ForgeRegistries.ITEMS;
		for (String name : values)
		{
			Rarity rarity = Rarity.COMMON;
			int i = name.indexOf('=');
			if (i < 0)
			{
				LOGGER.warn("Bad entry in ItemRarity: " + name);
				continue;
			}
			String part1 = name.substring(0, i).trim();
			String part2 = name.substring(i + 1).trim();
			if (!part2.isEmpty())
			{
				try {
					int j = Integer.parseInt(part2);
					if (j == 1) rarity = Rarity.UNCOMMON;
					else if (j == 2) rarity = Rarity.RARE;
					else if (j >= 3) rarity = Rarity.EPIC;
				}
				catch (Exception e) {
					;
				}
			}
			if (part1.endsWith(":*"))
			{
				expandMod(reg, ret, part1.substring(0, part1.length() - 2), rarity);
				continue;
			}
			List<String> list = expandItem(part1);
			for (String entry : list)
			{
				try {
					ResourceLocation key = new ResourceLocation(entry);
					if (reg.containsKey(key))
					{
						Item item = reg.getValue(key);
						ret.put(item, rarity);
					}
					else
						LOGGER.warn("Unknown entry in ItemRarity: " + entry);
				}
				catch (Exception e)
				{
					LOGGER.warn("Bad entry in ItemRarity: " + entry);
				}
			}
		}
		return ret;
	}

	private static String[] extract(String value)
	{
		String[] ret = value.split(";");
		return ret;
	}

	private static void validateMods(HashSet<String> set, String configName)
	{
		ModList list = ModList.get();
		set.removeIf(name -> {
			if (name.equals("*"))
				return false;
			if (list.isLoaded(name))
				return false;
			LOGGER.warn("Unknown entry in " + configName + ": " + name);
			return true;
		});
	}

	private static void validateGroups(HashSet<String> set, String configName)
	{
		HashSet<String> groups = new HashSet<>();
		for (ItemGroup g : ItemGroup.GROUPS)
		{
			if (g == ItemGroup.HOTBAR || g == ItemGroup.SEARCH || g == ItemGroup.INVENTORY)
				continue;
			groups.add(g.getPath());
		}
		set.removeIf(name -> {
			if (name.equals("*"))
				return false;
			if (name.equals("!"))
				return false;
			if (groups.contains(name))
				return false;
			LOGGER.warn("Unknown entry in " + configName + ": " + name);
			return true;
		});
	}

	private static List<String> expandItem(String name)
	{
		List<String> ret = new ArrayList<>();
		ret.add(name);
		int i = name.indexOf(':');
		if (i >= 0 && name.indexOf('*') > 0)
		{
			String ns = name.substring(0, i + 1);
			String temp = name.substring(i + 1);
			if (temp.startsWith("toolset*"))
			{
				String type = temp.substring(8);
				ret.clear();
				ret.add(ns + type + "_sword");
				ret.add(ns + type + "_shovel");
				ret.add(ns + type + "_pickaxe");
				ret.add(ns + type + "_axe");
				ret.add(ns + type + "_hoe");
			}
			else if (temp.startsWith("armorset*"))
			{
				String type = temp.substring(9);
				ret.clear();
				ret.add(ns + type + "_helmet");
				ret.add(ns + type + "_chestplate");
				ret.add(ns + type + "_leggings");
				ret.add(ns + type + "_boots");
			}
			else if (temp.startsWith("colorset*"))
			{
				String type = temp.substring(9);
				ret.clear();
				for (DyeColor dye : DyeColor.values())
				{
					ret.add(ns + dye.toString() + "_" + type);
				}
			}
		}
		else if (name.startsWith("eggset*"))
		{
			String mode = name.substring(7);
			if (mode.equals("all"))
			{
				ret.clear();
				for (SpawnEggItem e : SpawnEggItem.getEggs())
				{
					ResourceLocation res = e.getRegistryName();
					ret.add(res.toString());
				}
			}
			else if (mode.equals("monster"))
			{
				ret.clear();
				for (SpawnEggItem e : SpawnEggItem.getEggs())
				{
					EntityType<?> type = e.getType(null);
					if (type.getClassification().getPeacefulCreature())
						continue;
					ResourceLocation res = e.getRegistryName();						
					ret.add(res.toString());
				}
			}
			else if (mode.equals("peaceful"))
			{
				ret.clear();
				for (SpawnEggItem e : SpawnEggItem.getEggs())
				{
					EntityType<?> type = e.getType(null);
					if (!type.getClassification().getPeacefulCreature())
						continue;
					ResourceLocation res = e.getRegistryName();						
					ret.add(res.toString());
				}
			}
		}
		return ret;
	}

	private static void expandMod(IForgeRegistry<Item> reg, HashMap<Item, Rarity> map, String name, Rarity newRarity)
	{
		if (!ModList.get().isLoaded(name))
		{
			LOGGER.warn("Unknown mod entry in ItemRarity: " + name);
			return;
		}

		for (Entry<ResourceLocation, Item> entry : reg.getEntries())
		{
			if (name.equals(entry.getKey().getNamespace()))
			{
				Item item = entry.getValue();
				Rarity rarity = map.get(item);
				if (rarity == null)
					rarity = item.getRarity(new ItemStack(item));
				if (rarity != newRarity)
					map.put(item, newRarity);
			}
		}
	}

	public static class Common
	{
		public final BooleanValue restock;
		public final BooleanValue fixed;
		public final BooleanValue minable;
		public final ConfigValue<String> commonItem;
		public final ConfigValue<String> uncommonItem;
		public final ConfigValue<String> rareItem;
		public final ConfigValue<String> epicItem;
		public final IntValue commonCost;
		public final IntValue uncommonCost;
		public final IntValue rareCost;
		public final IntValue epicCost;
		public final IntValue commonUses;
		public final IntValue uncommonUses;
		public final IntValue rareUses;
		public final IntValue epicUses;
		public final ConfigValue<String> fixedItems;
		public final ConfigValue<String> includeMods;
		public final ConfigValue<String> excludeMods;
		public final ConfigValue<String> includeItems;
		public final ConfigValue<String> excludeItems;
		public final ConfigValue<String> includeGroups;
		public final ConfigValue<String> excludeGroups;
		public final ConfigValue<String> itemRarity;

		public Common(ForgeConfigSpec.Builder builder)
		{
			String baseTrans = Main.MODID + ".config.";
			String sectionTrans;

			sectionTrans = baseTrans + "general.";
			restock = builder
					.comment("Restock")
					.translation(sectionTrans + "restock")
					.define("Restock", true);

			fixed = builder
					.comment("Use fixed items")
					.translation(sectionTrans + "use_fixed")
					.define("UseFixedItems", false);

			minable = builder
					.comment("Minable")
					.translation(sectionTrans + "minable")
					.define("Minable", false);

			fixedItems = builder
					.comment("Fixed items; item or item,amount,pay_item,cost,uses")
					.translation(sectionTrans + "fixed_items")
					.define("FixedItems", "minecraft:air;minecraft:air;minecraft:air;minecraft:air;minecraft:air;minecraft:air;minecraft:air");

			includeMods = builder
					.comment("Include Mods")
					.translation(sectionTrans + "include_mods")
					.define("IncludeMods", "*");

			excludeMods = builder
					.comment("Exclude Mods")
					.translation(sectionTrans + "exclude_mods")
					.define("ExcludeMods", "draconicevolution;avaritia;botania");

			includeItems = builder
					.comment("Include Items")
					.translation(sectionTrans + "include_items")
					.define("IncludeItems", "*");

			excludeItems = builder
					.comment("Exclude Items")
					.translation(sectionTrans + "exclude_items")
					.define("ExcludeItems", "minecraft:nether_star;minecraft:beacon;minecraft:bedrock;minecraft:shulker_box;minecraft:colorset*shulker_box;minecraft:elytra;minecraft:end_portal_frame;minecraft:armorset*netherite;minecraft:toolset*netherite;minecraft:netherite_block;minecraft:netherite_ingot;vm:vending_machine");

			includeGroups = builder
					.comment("Include Creative Tab Groups")
					.translation(sectionTrans + "include_groups")
					.define("IncludeGroups", "*");

			excludeGroups = builder
					.comment("Exclude Creative Tab Groups")
					.translation(sectionTrans + "exclude_groups")
					.define("ExcludeGroups", "!");

			itemRarity = builder
					.comment("Change item rarity value for pricing")
					.translation(sectionTrans + "item_rarity")
					.define("ItemRarity", "minecraft:emerald_block=1;minecraft:diamond_block=1;minecraft:armorset*diamond=1;minecraft:toolset*diamond=1;minecraft:anvil=2;minecraft:trident=3;minecraft:bell=2;minecraft:conduit=3;minecraft:nautilus_shell=1;eggset*peaceful=1;eggset*monster=2;minecraft:evoker_spawn_egg=3;minecraft:netherite_scrap=2;minecraft:ancient_debris=2");

			builder.push("RarityData");
			sectionTrans = baseTrans + ".rarity.";
			commonItem = builder
					.comment("Common Item")
					.translation(sectionTrans + "common_item")
					.define("CommonItem", "minecraft:emerald");
			uncommonItem = builder
					.comment("Uncommon Item")
					.translation(sectionTrans + "uncommon_item")
					.define("UncommonItem", "minecraft:emerald");
			rareItem = builder
					.comment("Rare Item")
					.translation(sectionTrans + "rare_item")
					.define("RareItem", "minecraft:emerald");
			epicItem = builder
					.comment("Epic Item")
					.translation(sectionTrans + "epic_item")
					.define("EpicItem", "minecraft:emerald");

			commonCost = builder
					.comment("Common Cost")
					.translation(sectionTrans + "common_cost")
					.defineInRange("CommonCost", 1, 0, 64);
			uncommonCost = builder
					.comment("Uncommon Cost")
					.translation(sectionTrans + "uncommon_cost")
					.defineInRange("UncommonCost", 16, 0, 64);
			rareCost = builder
					.comment("Rare Cost")
					.translation(sectionTrans + "rare_cost")
					.defineInRange("RareCost", 32, 0, 64);
			epicCost = builder
					.comment("Epic Cost")
					.translation(sectionTrans + "epic_cost")
					.defineInRange("EpicCost", 64, 0, 64);

			commonUses = builder
					.comment("Common Uses")
					.translation(sectionTrans + "common_uses")
					.defineInRange("CommonUses", 8, 0, 32);
			uncommonUses = builder
					.comment("Uncommon Uses")
					.translation(sectionTrans + "uncommon_uses")
					.defineInRange("UncommonUses", 4, 0, 32);
			rareUses = builder
					.comment("Rare Uses")
					.translation(sectionTrans + "rare_uses")
					.defineInRange("RareUses", 2, 0, 32);
			epicUses = builder
					.comment("Epic Uses")
					.translation(sectionTrans + "epic_uses")
					.defineInRange("EpicUses", 1, 0, 32);
			builder.pop();
		}
	}
}
