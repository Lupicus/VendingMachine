package com.lupicus.vm.tileentity;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import net.minecraft.core.Holder;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.InstrumentTags;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.item.InstrumentItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.SuspiciousEffectHolder;

public class MultiItems
{
	public static void generate(Map<Item, Set<ItemStack>> multiItems, RegistryAccess regs, FeatureFlagSet fs)
	{
		Set<ItemStack> work = ItemStackLinkedSet.createTypeAndComponentsSet();
		Optional<Registry<Enchantment>> oreg = regs.lookup(Registries.ENCHANTMENT);
		if (oreg.isPresent())
		{
			Iterator<Reference<Enchantment>> iter = oreg.get().listElements().iterator();
			Reference<Enchantment> ench;
			Enchantment enchantment;
			while (iter.hasNext())
			{
				ench = iter.next();
				enchantment = ench.value();
				for (int i = enchantment.getMinLevel(); i <= enchantment.getMaxLevel(); ++i)
				{
					work.add(EnchantmentHelper.createBook(new EnchantmentInstance(ench, i)));
				}
			}
			multiItems.put(Items.ENCHANTED_BOOK, work);
		}

		addPotionEffects(multiItems, Items.TIPPED_ARROW, fs);
		addPotionEffects(multiItems, Items.POTION, fs);
		addPotionEffects(multiItems, Items.SPLASH_POTION, fs);
		addPotionEffects(multiItems, Items.LINGERING_POTION, fs);

		List<SuspiciousEffectHolder> list = SuspiciousEffectHolder.getAllEffectHolders();
		Set<ItemStack> set = ItemStackLinkedSet.createTypeAndComponentsSet();
		for (SuspiciousEffectHolder suspiciouseffectholder : list)
		{
			ItemStack itemstack = new ItemStack(Items.SUSPICIOUS_STEW);
			itemstack.set(DataComponents.SUSPICIOUS_STEW_EFFECTS, suspiciouseffectholder.getSuspiciousEffects());
			set.add(itemstack);
		}
		multiItems.put(Items.SUSPICIOUS_STEW, set);

		Optional<Registry<Instrument>> oreg2 = regs.lookup(Registries.INSTRUMENT);
		if (oreg2.isPresent())
		{
			work = new HashSet<>();
			for (Holder<Instrument> holder : oreg2.get().get(InstrumentTags.GOAT_HORNS).get())
				work.add(InstrumentItem.create(Items.GOAT_HORN, holder));
			multiItems.put(Items.GOAT_HORN, work);
		}

		work = new HashSet<>();
		for (byte b0 : FireworkRocketItem.CRAFTABLE_DURATIONS)
		{
			ItemStack itemstack = new ItemStack(Items.FIREWORK_ROCKET);
			itemstack.set(DataComponents.FIREWORKS, new Fireworks(b0, List.of()));
			work.add(itemstack);
		}
		multiItems.put(Items.FIREWORK_ROCKET, work);

		work = new HashSet<>();
		for (int i = 15; i >= 0; --i)
			work.add(LightBlock.setLightOnStack(new ItemStack(Items.LIGHT), i));
		multiItems.put(Items.LIGHT, work);
	}

	private static void addPotionEffects(Map<Item, Set<ItemStack>> multiItems, Item item, FeatureFlagSet fs)
	{
		Set<ItemStack> work = new HashSet<>();
		for (Holder<Potion> ph : BuiltInRegistries.POTION.asHolderIdMap())
		{
			if (ph.get().isEnabled(fs))
			{
				work.add(PotionContents.createItemStack(item, ph));
			}
		}
		multiItems.put(item, work);
	}
}
