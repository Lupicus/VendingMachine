package com.lupicus.vm.tileentity;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import com.lupicus.vm.block.ModBlocks;
import com.lupicus.vm.config.MyConfig;
import com.lupicus.vm.sound.ModSounds;

import net.minecraft.entity.merchant.IMerchant;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MerchantOffer;
import net.minecraft.item.MerchantOffers;
import net.minecraft.item.OperatorOnlyItem;
import net.minecraft.item.Rarity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

public class VendingMachineTileEntity extends TileEntity implements IMerchant
{
	MerchantOffers offers = null;
	PlayerEntity customer = null;
	boolean fixed = MyConfig.fixed;
	long stockTime = 0;
	private static final long DAY = 24000;
	private static final int ITEM_COUNT = 7;
	private static final int RETRIES = 8;

	public VendingMachineTileEntity() {
		super(ModTileEntities.VENDING_MACHINE);
	}

	@Override
	public void read(CompoundNBT compound)
	{
		stockTime = compound.getLong("stockTime");
		fixed = compound.getBoolean("fixed");
		offers = new MerchantOffers(compound);
		if (offers.isEmpty())
			offers = null;
		super.read(compound);
	}

	@Override
	public CompoundNBT write(CompoundNBT compound)
	{
		compound.putLong("stockTime", stockTime);
		compound.putBoolean("fixed", fixed);
		if (offers != null)
			compound.merge(offers.write());
		return super.write(compound);
	}

	@Override
	public void setCustomer(PlayerEntity player) {
		customer = player;
	}

	@Override
	public PlayerEntity getCustomer() {
		return customer;
	}

	@Override
	public MerchantOffers getOffers()
	{
		if (MyConfig.restock)
		{
			long time = world.getDayTime();
			if (time < stockTime)
			{
				stockTime = time;
				stockTime -= Math.abs(stockTime % DAY);
			}
			if (time - stockTime >= DAY)
			{
				stockTime = time;
				stockTime -= Math.abs(stockTime % DAY);
				if (fixed)
					restock();
				else
					offers = null;
			}
		}
		if (offers == null)
		{
			if (fixed)
				configOffers();
			else
				fillOffers();
		}
		return offers;
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public void setClientSideOffers(MerchantOffers offers) {
		this.offers = offers;
	}

	@Override
	public void onTrade(MerchantOffer offer) {
		offer.increaseUses();
		world.playSound((PlayerEntity) null, pos, ModSounds.VENDING_MACHINE_TAKE_RESULT, SoundCategory.BLOCKS, 1.0F, 1.0F);
	}

	@Override
	public void verifySellingItem(ItemStack stack) {
	}

	@Override
	public World getWorld() {
		return world;
	}

	@Override
	public int getXp() {
		return 0;
	}

	@Override
	public void setXP(int xpIn) {
	}

	@Override
	public boolean func_213705_dZ() {
		return false;
	}

	@Override
	public SoundEvent getYesSound() {
		return SoundEvents.ENTITY_VILLAGER_YES;
	}

	public void openGui(PlayerEntity player) {
		setCustomer(player);
		TranslationTextComponent name = new TranslationTextComponent(ModBlocks.VENDING_MACHINE.getTranslationKey());
		this.openMerchantContainer(player, name, 5);
	}

	private void fillOffers()
	{
		int tryCount = 0;
		int maxUses;

		offers = new MerchantOffers();
		Collection<Item> set = new HashSet<>(ForgeRegistries.ITEMS.getValues());
		if (!MyConfig.includeGroupSet.contains("*") ||
			!(MyConfig.excludeGroupSet.size() == 1 && MyConfig.excludeGroupSet.contains("!")))
			filterGroups(set);
		if (!MyConfig.includeModSet.contains("*") || !MyConfig.excludeModSet.isEmpty())
			filterMods(set);
		filterRarity(set);
		if (set.isEmpty())
			set.add(Items.AIR);
		Item[] values = set.toArray(new Item[0]);
		NonNullList<ItemStack> items = NonNullList.create();

		for (int i = 0; i < ITEM_COUNT; )
		{
			tryCount++;
			int j = this.world.rand.nextInt(values.length);
			Item item = values[j];
			ItemStack stack;
			// handle Enchanted books and etc.
			items.clear();
			item.fillItemGroup(ItemGroup.SEARCH, items);
			if (!items.isEmpty())
				stack = items.get(this.world.rand.nextInt(items.size()));
			else
				stack = new ItemStack(item);
			Rarity rarity = MyConfig.itemRarityMap.get(item);
			if (rarity == null)
				rarity = item.getRarity(stack);
			ItemStack payment = itemPayment(rarity);
			if (payment.isEmpty() || invalidItem(item))
			{
				if (tryCount < RETRIES)
					continue;
				stack.setCount(0);
				payment = itemPayment(defRarity());
				maxUses = 1;
			}
			else
			{
				maxUses = itemUses(rarity);
			}
			MerchantOffer offer = new MerchantOffer(payment, stack, maxUses, 0, 0);
			offers.add(offer);
			tryCount = 0;
			++i;
		}
	}

	private void configOffers()
	{
		int maxUses;
		ItemStack payment;

		offers = new MerchantOffers();
		NonNullList<ItemStack> items = NonNullList.create();

		for (int i = 0; i < ITEM_COUNT; ++i)
		{
			Item item = MyConfig.fixedItems[i];
			ItemStack stack;
			// handle Enchanted books and etc.
			items.clear();
			item.fillItemGroup(ItemGroup.SEARCH, items);
			if (!items.isEmpty())
				stack = items.get(this.world.rand.nextInt(items.size()));
			else
				stack = new ItemStack(item);
			if (MyConfig.fixedExtended[i])
			{
				stack.grow(MyConfig.fixedAmount[i] - 1);
				maxUses = MyConfig.fixedUses[i];
				payment = MyConfig.fixedPayment[i];
			}
			else
			{
				Rarity rarity = MyConfig.itemRarityMap.get(item);
				if (rarity == null)
					rarity = item.getRarity(stack);
				payment = itemPayment(rarity);
				if (payment.isEmpty())
				{
					payment = itemPayment(Rarity.EPIC);
					maxUses = itemUses(Rarity.EPIC);
				}
				else
				{
					maxUses = itemUses(rarity);
				}
			}
			MerchantOffer offer = new MerchantOffer(payment, stack, maxUses, 0, 0);
			offers.add(offer);
		}
	}

	private void restock()
	{
		if (offers == null)
			return;
		for (MerchantOffer offer : offers)
			offer.resetUses();
	}

	private void filterGroups(Collection<Item> set)
	{
		HashSet<String> includeSet = MyConfig.includeGroupSet;
		HashSet<String> excludeSet = MyConfig.excludeGroupSet;
		boolean addAll = includeSet.contains("*");
		set.removeIf(item ->
		{
			ItemGroup group = item.getGroup();
			String name = (group == null) ? "!" : group.getPath();
			if (!(addAll || includeSet.contains(name)) ||
				excludeSet.contains(name))
				return true;
			return false;
		});
	}

	private void filterMods(Collection<Item> set)
	{
		HashSet<String> includeSet = MyConfig.includeModSet;
		HashSet<String> excludeSet = MyConfig.excludeModSet;
		boolean addAll = includeSet.contains("*");
		set.removeIf(item ->
		{
			String name = item.getRegistryName().getNamespace();
			if (!(addAll || includeSet.contains(name)) ||
				excludeSet.contains(name))
				return true;
			return false;
		});
	}

	private void filterRarity(Collection<Item> set)
	{
		ItemStack stack = new ItemStack(Items.AIR);
		HashMap<Item, Rarity> map = MyConfig.itemRarityMap;
		if (MyConfig.commonCost == 0)
		{
			set.removeIf(item ->
			{
				Rarity rarity = map.get(item);
				if (rarity == null)
					rarity = item.getRarity(stack);
				return (rarity == Rarity.COMMON);
			});
		}
		if (MyConfig.uncommonCost == 0)
		{
			set.removeIf(item ->
			{
				Rarity rarity = map.get(item);
				if (rarity == null)
					rarity = item.getRarity(stack);
				return (rarity == Rarity.UNCOMMON);
			});
		}
		if (MyConfig.rareCost == 0)
		{
			set.removeIf(item ->
			{
				Rarity rarity = map.get(item);
				if (rarity == null)
					rarity = item.getRarity(stack);
				return (rarity == Rarity.RARE);
			});
		}
		if (MyConfig.epicCost == 0)
		{
			set.removeIf(item ->
			{
				Rarity rarity = map.get(item);
				if (rarity == null)
					rarity = item.getRarity(stack);
				return (rarity == Rarity.EPIC);
			});
		}
	}

	private boolean invalidItem(Item item)
	{
		if (item instanceof OperatorOnlyItem)
			return true;

		ItemGroup group = item.getGroup();
		String groupName = (group == null) ? "!" : group.getPath();
		if (!(MyConfig.includeGroupSet.contains("*") ||
			  MyConfig.includeGroupSet.contains(groupName)))
			return true;

		if (MyConfig.excludeGroupSet.contains(groupName))
			return true;

		String modName = item.getRegistryName().getNamespace();
		if (!(MyConfig.includeModSet.contains("*") ||
			  MyConfig.includeModSet.contains(modName)))
			return true;

		if (MyConfig.excludeModSet.contains(modName) ||
			MyConfig.excludeItemSet.contains(item))
			return true;

		return false;
	}

	private Rarity defRarity()
	{
		if (MyConfig.commonCost > 0)
			return Rarity.COMMON;
		if (MyConfig.uncommonCost > 0)
			return Rarity.UNCOMMON;
		if (MyConfig.rareCost > 0)
			return Rarity.RARE;
		return Rarity.EPIC;
	}

	private ItemStack itemPayment(Rarity type)
	{
		ItemStack cost;
		switch (type)
		{
		case COMMON:
			cost = new ItemStack(MyConfig.commonItem, MyConfig.commonCost);
			break;
		case EPIC:
			cost = new ItemStack(MyConfig.epicItem, MyConfig.epicCost);
			break;
		case RARE:
			cost = new ItemStack(MyConfig.rareItem, MyConfig.rareCost);
			break;
		case UNCOMMON:
			cost = new ItemStack(MyConfig.uncommonItem, MyConfig.uncommonCost);
			break;
		default:
			cost = ItemStack.EMPTY;
			break;
		}
		return cost;
	}

	private int itemUses(Rarity type)
	{
		int maxUses;
		switch (type)
		{
		case COMMON:
			maxUses = MyConfig.commonUses;
			break;
		case EPIC:
			maxUses = MyConfig.epicUses;
			break;
		case RARE:
			maxUses = MyConfig.rareUses;
			break;
		case UNCOMMON:
			maxUses = MyConfig.uncommonUses;
			break;
		default:
			maxUses = 0;
			break;
		}
		return maxUses;
	}
}
