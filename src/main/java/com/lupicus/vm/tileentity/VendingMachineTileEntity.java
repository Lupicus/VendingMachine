package com.lupicus.vm.tileentity;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import com.lupicus.vm.block.ModBlocks;
import com.lupicus.vm.block.VendingMachine;
import com.lupicus.vm.config.MyConfig;
import com.lupicus.vm.config.MyConfig.CostData;
import com.lupicus.vm.sound.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.GameMasterBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

public class VendingMachineTileEntity extends BlockEntity implements Merchant, Nameable
{
	final boolean enabled;
	MerchantOffers offers = null;
	Player customer = null;
	boolean fixed = MyConfig.fixed;
	long stockTime = 0;
	Component customName = null;
	private static final long DAY = 24000;
	private static final int ITEM_COUNT = 7;
	private static final int RETRIES = 8;

	public VendingMachineTileEntity(BlockPos pos, BlockState state) {
		super(ModTileEntities.VENDING_MACHINE, pos, state);
		enabled = VendingMachine.isEnabled(state);
	}

	@Override
	public void load(CompoundTag compound)
	{
		super.load(compound);
		if (!enabled)
			return;
		stockTime = compound.getLong("stockTime");
		fixed = compound.getBoolean("fixed");
		offers = new MerchantOffers(compound);
		if (offers.isEmpty())
			offers = null;
		if (compound.contains("CustomName", 8))
			customName = Component.Serializer.fromJson(compound.getString("CustomName"));
	}

	@Override
	protected void saveAdditional(CompoundTag compound)
	{
		super.saveAdditional(compound);
		if (!enabled)
			return;
		compound.putLong("stockTime", stockTime);
		compound.putBoolean("fixed", fixed);
		if (offers != null)
			compound.merge(offers.createTag());
		if (customName != null)
			compound.putString("CustomName", Component.Serializer.toJson(customName));
	}

	public void readMined(CompoundTag compound)
	{
		if (compound.contains("mined"))
		{
			stockTime = level.getDayTime();
			stockTime -= Math.abs(stockTime % DAY);
			fixed = compound.getBoolean("fixed");
			offers = new MerchantOffers(compound);
			if (offers.isEmpty())
				offers = null;
		}
	}

	public void writeMined(CompoundTag compound)
	{
		compound.putBoolean("mined", true);
		compound.putBoolean("fixed", fixed);
		if (offers != null)
			compound.merge(offers.createTag());
	}

	@Override
	public void setTradingPlayer(Player player) {
		customer = player;
	}

	@Override
	public Player getTradingPlayer() {
		return customer;
	}

	@Override
	public MerchantOffers getOffers()
	{
		if (MyConfig.restock)
		{
			long time = level.getDayTime();
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
			setChanged();
		}
		return offers;
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public void overrideOffers(MerchantOffers offers) {
		this.offers = offers;
	}

	@Override
	public void notifyTrade(MerchantOffer offer) {
		offer.increaseUses();
		level.playSound((Player) null, worldPosition, ModSounds.VENDING_MACHINE_TAKE_RESULT, SoundSource.BLOCKS, 1.0F, 1.0F);
		setChanged();
	}

	@Override
	public void notifyTradeUpdated(ItemStack stack) {
	}

	@Override
	public int getVillagerXp() {
		return 0;
	}

	@Override
	public void overrideXp(int xpIn) {
	}

	@Override
	public boolean showProgressBar() {
		return false;
	}

	@Override
	public SoundEvent getNotifyTradeSound() {
		return SoundEvents.VILLAGER_YES;
	}

	@Override
	public boolean isClientSide() {
		return level.isClientSide;
	}

	public void setCustomName(Component name) {
		customName = name;
	}

	@Override
	public Component getCustomName() {
		return customName;
	}

	@Override
	public Component getName() {
		return customName != null ? customName : new TranslatableComponent(ModBlocks.VENDING_MACHINE.getDescriptionId());
	}

	public void openGui(Player player) {
		setTradingPlayer(player);
		this.openTradingScreen(player, getName(), 5);
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
			int j = this.level.random.nextInt(values.length);
			Item item = values[j];
			ItemStack stack;
			// handle Enchanted books and etc.
			items.clear();
			item.fillItemCategory(CreativeModeTab.TAB_SEARCH, items);
			if (!items.isEmpty())
				stack = items.get(this.level.random.nextInt(items.size()));
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
			item.fillItemCategory(CreativeModeTab.TAB_SEARCH, items);
			if (!items.isEmpty())
				stack = items.get(this.level.random.nextInt(items.size()));
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
		setChanged();
	}

	private void filterGroups(Collection<Item> set)
	{
		HashSet<String> includeSet = MyConfig.includeGroupSet;
		HashSet<String> excludeSet = MyConfig.excludeGroupSet;
		boolean addAll = includeSet.contains("*");
		set.removeIf(item ->
		{
			CreativeModeTab group = item.getItemCategory();
			String name = (group == null) ? "!" : group.getRecipeFolderName();
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

		if (item instanceof GameMasterBlockItem)
			return true;

		CreativeModeTab group = item.getItemCategory();
		String groupName = (group == null) ? "!" : group.getRecipeFolderName();
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
