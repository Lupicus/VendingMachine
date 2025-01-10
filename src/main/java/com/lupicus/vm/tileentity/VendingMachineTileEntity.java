package com.lupicus.vm.tileentity;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import com.lupicus.vm.block.ModBlocks;
import com.lupicus.vm.config.MyConfig;
import com.lupicus.vm.config.MyConfig.CostData;
import com.lupicus.vm.sound.ModSounds;

import net.minecraft.block.BlockState;
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
import net.minecraft.util.INameable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

public class VendingMachineTileEntity extends TileEntity implements IMerchant, INameable
{
	MerchantOffers offers = null;
	PlayerEntity customer = null;
	boolean fixed = MyConfig.fixed;
	long stockTime = 0;
	ITextComponent customName = null;
	private static final long DAY = 24000;
	private static final int ITEM_COUNT = 7;
	private static final int RETRIES = 8;

	public VendingMachineTileEntity() {
		super(ModTileEntities.VENDING_MACHINE);
	}

	@Override
	public void func_230337_a_(BlockState state, CompoundNBT compound) // read
	{
		stockTime = compound.getLong("stockTime");
		fixed = compound.getBoolean("fixed");
		offers = new MerchantOffers(compound);
		if (offers.isEmpty())
			offers = null;
		if (compound.contains("CustomName", 8))
			customName = ITextComponent.Serializer.func_240643_a_(compound.getString("CustomName"));
		super.func_230337_a_(state, compound);
	}

	@Override
	public CompoundNBT write(CompoundNBT compound)
	{
		compound.putLong("stockTime", stockTime);
		compound.putBoolean("fixed", fixed);
		if (offers != null)
			compound.merge(offers.write());
		if (customName != null)
			compound.putString("CustomName", ITextComponent.Serializer.toJson(customName));
		return super.write(compound);
	}

	public void readMined(CompoundNBT compound)
	{
		if (compound.contains("mined"))
		{
			stockTime = world.getDayTime();
			stockTime -= Math.abs(stockTime % DAY);
			fixed = compound.getBoolean("fixed");
			offers = new MerchantOffers(compound);
			if (offers.isEmpty())
				offers = null;
		}
	}

	public void writeMined(CompoundNBT compound)
	{
		compound.putBoolean("mined", true);
		compound.putBoolean("fixed", fixed);
		if (offers != null)
			compound.merge(offers.write());
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
			markDirty();
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
		markDirty();
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

	public void setCustomName(ITextComponent name) {
		customName = name;
	}

	@Override
	public ITextComponent getCustomName() {
		return customName;
	}

	@Override
	public ITextComponent getName() {
		return customName != null ? customName : new TranslationTextComponent(ModBlocks.VENDING_MACHINE.getTranslationKey());
	}

	public void openGui(PlayerEntity player) {
		setCustomer(player);
		this.openMerchantContainer(player, getName(), 5);
	}

	private void fillOffers()
	{
		int tryCount = 0;
		int maxUses;
		CostData cost;

		offers = new MerchantOffers();
		Collection<Item> set;
		set = (MyConfig.includeAllItems) ? ForgeRegistries.ITEMS.getValues() : MyConfig.includeItemSet;
		set = new HashSet<>(set);
		if (!MyConfig.includeGroupSet.contains("*") ||
			!(MyConfig.excludeGroupSet.isEmpty() ||
			  (MyConfig.excludeGroupSet.size() == 1 && MyConfig.excludeGroupSet.contains("!"))))
			filterGroups(set);
		if (!MyConfig.includeModSet.contains("*") || !MyConfig.excludeModSet.isEmpty())
			filterMods(set);
		set.addAll(MyConfig.addItemSet);
		filterCost(set);
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
			cost = MyConfig.itemCostMap.get(item);
			if (cost != null)
			{
				maxUses = (cost.maxUses > 0) ? cost.maxUses : itemUses(rarity);
			}
			else
			{
				cost = itemPayment(rarity);
				maxUses = cost.maxUses;
			}
			if (cost.costA.isEmpty() || invalidItem(item))
			{
				if (tryCount < RETRIES)
					continue;
				stack.setCount(0);
				cost = itemPayment(defRarity());
				maxUses = 1;
			}
			MerchantOffer offer = new MerchantOffer(cost.costA, cost.costB, stack, maxUses, 0, 0);
			offers.add(offer);
			tryCount = 0;
			++i;
		}
	}

	private void configOffers()
	{
		int maxUses;
		CostData cost;

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
			if (MyConfig.fixedTags[i] != null)
				stack.getOrCreateTag().merge(MyConfig.fixedTags[i]);
			if (MyConfig.fixedExtended[i])
			{
				stack.grow(MyConfig.fixedAmount[i] - 1);
				cost = new CostData();
				cost.costA = MyConfig.fixedPayment[i];
				maxUses = MyConfig.fixedUses[i];
			}
			else
			{
				Rarity rarity = MyConfig.itemRarityMap.get(item);
				if (rarity == null)
					rarity = item.getRarity(stack);
				cost = MyConfig.itemCostMap.get(item);
				if (cost != null)
				{
					maxUses = (cost.maxUses > 0) ? cost.maxUses : itemUses(rarity);
				}
				else
				{
					cost = itemPayment(rarity);
					maxUses = cost.maxUses;
				}
				if (cost.costA.isEmpty())
				{
					cost = itemPayment(Rarity.EPIC);
					maxUses = cost.maxUses;
				}
			}
			MerchantOffer offer = new MerchantOffer(cost.costA, cost.costB, stack, maxUses, 0, 0);
			offers.add(offer);
		}
	}

	private void restock()
	{
		if (offers == null)
			return;
		for (MerchantOffer offer : offers)
			offer.resetUses();
		markDirty();
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

	private void filterCost(Collection<Item> set)
	{
		ItemStack stack = new ItemStack(Items.AIR);
		HashMap<Item, Rarity> map = MyConfig.itemRarityMap;
		HashMap<Item, CostData> map2 = MyConfig.itemCostMap;
		if (MyConfig.commonCost == 0)
		{
			set.removeIf(item ->
			{
				Rarity rarity = map.get(item);
				if (rarity == null)
					rarity = item.getRarity(stack);
				return (rarity == Rarity.COMMON) && !map2.containsKey(item);
			});
		}
		if (MyConfig.uncommonCost == 0)
		{
			set.removeIf(item ->
			{
				Rarity rarity = map.get(item);
				if (rarity == null)
					rarity = item.getRarity(stack);
				return (rarity == Rarity.UNCOMMON) && !map2.containsKey(item);
			});
		}
		if (MyConfig.rareCost == 0)
		{
			set.removeIf(item ->
			{
				Rarity rarity = map.get(item);
				if (rarity == null)
					rarity = item.getRarity(stack);
				return (rarity == Rarity.RARE) && !map2.containsKey(item);
			});
		}
		if (MyConfig.epicCost == 0)
		{
			set.removeIf(item ->
			{
				Rarity rarity = map.get(item);
				if (rarity == null)
					rarity = item.getRarity(stack);
				return (rarity == Rarity.EPIC) && !map2.containsKey(item);
			});
		}
	}

	private boolean invalidItem(Item item)
	{
		if (MyConfig.addItemSet.contains(item))
			return false;

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

		if (MyConfig.excludeModSet.contains(modName))
			return true;

		if (!(MyConfig.includeAllItems ||
			  MyConfig.includeItemSet.contains(item)))
			return true;

		if (MyConfig.excludeItemSet.contains(item))
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

	private CostData itemPayment(Rarity type)
	{
		CostData cost = MyConfig.rarityCostMap.get(type);
		if (cost == null)
		{
			cost = new CostData();
			cost.costA = ItemStack.EMPTY;
		}
		return cost;
	}

	private int itemUses(Rarity type)
	{
		CostData cost = MyConfig.rarityCostMap.get(type);
		return (cost != null) ? cost.maxUses : 0;
	}
}
