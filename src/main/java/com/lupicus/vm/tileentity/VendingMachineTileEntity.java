package com.lupicus.vm.tileentity;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import com.lupicus.vm.block.ModBlocks;
import com.lupicus.vm.block.VendingMachine;
import com.lupicus.vm.config.MyConfig;
import com.lupicus.vm.config.MyConfig.CostData;
import com.lupicus.vm.sound.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.GameMasterBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueInput.TypedInputList;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueOutput.TypedOutputList;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

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
	// shared data
	private static int initId = -1;
	private static Item[] inputItems;
	private static Map<Item, Set<ItemStack>> groupMultiItems = new HashMap<>();
	private static Map<Item, Set<ItemStack>> allMultiItems = new HashMap<>();

	public VendingMachineTileEntity(BlockPos pos, BlockState state) {
		super(ModTileEntities.VENDING_MACHINE, pos, state);
		enabled = VendingMachine.isEnabled(state);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void loadAdditional(ValueInput input)
	{
		super.loadAdditional(input);
		if (!enabled)
			return;
		stockTime = input.getLongOr("stockTime", 0L);
		fixed = input.getBooleanOr("fixed", false);
		String offersKey = "Recipes";
		CompoundTag nbt = new CompoundTag();
		Optional<TypedInputList<CompoundTag>> inListOpt = input.list(offersKey, CompoundTag.CODEC);
		if (inListOpt.isPresent())
		{
			ListTag listTag = new ListTag();
			for (CompoundTag cTag : inListOpt.get())
				listTag.add(cTag);
			nbt.put(offersKey, listTag);
		}
		offers = readOffers(nbt, input.lookup());
		if (offers.isEmpty())
			offers = null;
		customName = parseCustomNameSafe(input, "CustomName");
	}

	@Override
	protected void saveAdditional(ValueOutput output)
	{
		super.saveAdditional(output);
		if (!enabled)
			return;
		output.putLong("stockTime", stockTime);
		output.putBoolean("fixed", fixed);
		if (offers != null)
		{
			String offersKey = "Recipes";
			CompoundTag nbt = getNbtOffers(level.registryAccess());
			Optional<ListTag> inListOpt = nbt.getList(offersKey);
			if (inListOpt.isPresent())
			{
				TypedOutputList<CompoundTag> outList = output.list(offersKey, CompoundTag.CODEC);
				for (Tag tag : inListOpt.get())
					if (tag instanceof CompoundTag cTag)
						outList.add(cTag);
			}
		}
		output.storeNullable("CustomName", ComponentSerialization.CODEC, customName);
	}

	public void readMined(CompoundTag compound)
	{
		if (compound.contains("mined"))
		{
			stockTime = level.getDayTime();
			stockTime -= Math.abs(stockTime % DAY);
			fixed = compound.getBooleanOr("fixed", false);
			offers = readOffers(compound, level.registryAccess());
			if (offers.isEmpty())
				offers = null;
		}
	}

	public void writeMined(CompoundTag compound)
	{
		compound.putBoolean("mined", true);
		compound.putBoolean("fixed", fixed);
		if (offers != null)
			compound.merge(getNbtOffers(level.registryAccess()));
	}

	private CompoundTag getNbtOffers(Provider hp)
	{
		return (CompoundTag) MerchantOffers.CODEC.encodeStart(hp.createSerializationContext(NbtOps.INSTANCE), offers).getOrThrow();
	}

	private MerchantOffers readOffers(CompoundTag compound, Provider hp)
	{
		Optional<MerchantOffers> opt = MerchantOffers.CODEC
				.parse(hp.createSerializationContext(NbtOps.INSTANCE), compound)
				.resultOrPartial();
		if (opt.isPresent())
			return opt.get();
		return new MerchantOffers();
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
			initData();
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


	@Override
	public boolean stillValid(Player player) {
		return customer == player && Container.stillValidBlockEntity(this, player);
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
		return customName != null ? customName : Component.translatable(ModBlocks.VENDING_MACHINE.getDescriptionId());
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
		Item[] values = inputItems;
		NonNullList<ItemStack> items = NonNullList.create();

		for (int i = 0; i < ITEM_COUNT; )
		{
			tryCount++;
			int j = this.level.random.nextInt(values.length);
			Item item = values[j];
			ItemStack stack;
			// handle Enchanted books and etc.
			items.clear();
			fillItems(groupMultiItems, item, items);
			if (!items.isEmpty())
				stack = items.get(this.level.random.nextInt(items.size()));
			else
				stack = new ItemStack(item);
			Rarity rarity = MyConfig.itemRarityMap.get(item);
			if (rarity == null)
				rarity = item.components().getOrDefault(DataComponents.RARITY, Rarity.COMMON);
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
			if (!stack.isEmpty())
			{
				ItemStack payment = cost.costB;
				ItemCost value = null;
				if (!payment.isEmpty())
					value = new ItemCost(payment.getItem(), payment.getCount());
				payment = cost.costA;
				MerchantOffer offer = new MerchantOffer(new ItemCost(payment.getItem(), payment.getCount()), Optional.ofNullable(value), stack, maxUses, 0, 0.0F);
				offers.add(offer);
			}
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
			fillItems(allMultiItems, item, items);
			if (!items.isEmpty())
				stack = items.get(this.level.random.nextInt(items.size()));
			else
				stack = new ItemStack(item);
			if (MyConfig.fixedData[i] != null)
				stack.applyComponents(MyConfig.fixedData[i]);
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
					rarity = item.components().getOrDefault(DataComponents.RARITY, Rarity.COMMON);
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
			if (!stack.isEmpty())
			{
				ItemStack payment = cost.costB;
				ItemCost value = null;
				if (!payment.isEmpty())
					value = new ItemCost(payment.getItem(), payment.getCount());
				payment = cost.costA;
				MerchantOffer offer = new MerchantOffer(new ItemCost(payment.getItem(), payment.getCount()), Optional.ofNullable(value), stack, maxUses, 0, 0.0F);
				offers.add(offer);
			}
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

	private void initData()
	{
		if (initId == MyConfig.loadId)
			return;
		initId = MyConfig.loadId;
		if (!MyConfig.disableGroups)
		{
			try {
				CreativeModeTabs.tryRebuildTabContents(level.enabledFeatures(), true, level.registryAccess());
			}
			catch (Throwable t) {
				MyConfig.setDisableGroups(t);
			}
		}
		Set<Item> work = new HashSet<>();
		if (MyConfig.disableGroups)
		{
			fillMultiItems();
			buildItemList(work);
		}
		else
		{
			findMultiItems();
			Set<Item> tempItems = new HashSet<>();
			buildGroupList(tempItems);
			if (MyConfig.includeAllItems)
				work = tempItems;
			else
			{
				work.addAll(MyConfig.includeItemSet);
				work.retainAll(tempItems);
			}
		}
		if (!MyConfig.includeModSet.contains("*") || !MyConfig.excludeModSet.isEmpty())
			filterMods(work);
		addItemList(work, MyConfig.addItemSet);
		filterCost(work);
		if (work.isEmpty())
			work.add(Items.AIR);
		inputItems = work.toArray(new Item[0]);
	}

	public static void clearData()
	{
		if (initId >= 0)
		{
			if (MyConfig.disableGroups)
				MyConfig.disableGroups = false;
			else
				CreativeModeTabs.tryRebuildTabContents(FeatureFlagSet.of(), false, ServerLifecycleHooks.getCurrentServer().registryAccess());
		}
		initId = -1;
		inputItems = null;
		groupMultiItems.clear();
		allMultiItems.clear();
	}

	private void buildGroupList(Collection<Item> set)
	{
		HashSet<String> includeSet = MyConfig.includeGroupSet;
		HashSet<String> excludeSet = MyConfig.excludeGroupSet;
		if (includeSet.contains("*"))
		{
			for (Entry<CreativeModeTab, String> e : MyConfig.groupName.entrySet())
			{
				if (excludeSet.contains(e.getValue()))
					continue;
				addItems(set, e.getKey().getSearchTabDisplayItems());
			}
			if (!excludeSet.contains("!"))
				addNoGroupItems(set);
		}
		else
		{
			Set<String> workSet = new HashSet<>(includeSet);
			workSet.removeAll(excludeSet);
			for (Entry<CreativeModeTab, String> e : MyConfig.groupName.entrySet())
			{
				if (workSet.contains(e.getValue()))
					addItems(set, e.getKey().getSearchTabDisplayItems());
			}
			if (workSet.contains("!"))
				addNoGroupItems(set);
		}
	}

	private void addItems(Collection<Item> set, Collection<ItemStack> stacks)
	{
		for (ItemStack stack : stacks)
			set.add(stack.getItem());
	}

	private void addNoGroupItems(Collection<Item> set)
	{
		Set<Item> groupItems = new HashSet<>();
		for (CreativeModeTab g : MyConfig.groupName.keySet())
			addItems(groupItems, g.getSearchTabDisplayItems());

		for (Item item : ForgeRegistries.ITEMS.getValues())
		{
			if (!groupItems.contains(item) && item.isEnabled(level.enabledFeatures()))
				set.add(item);
		}
	}

	private void findMultiItems()
	{
		Set<ItemStack> work = new HashSet<>();
		HashSet<String> includeSet = MyConfig.includeGroupSet;
		HashSet<String> excludeSet = MyConfig.excludeGroupSet;
		boolean addAll = includeSet.contains("*");
		groupMultiItems.clear();
		allMultiItems.clear();
		for (Entry<CreativeModeTab, String> e : MyConfig.groupName.entrySet())
		{
			Map<Item, Set<ItemStack>> multiItems = groupMultiItems;
			String name = e.getValue();
			if (!(addAll || includeSet.contains(name)) ||
				excludeSet.contains(name))
				multiItems = allMultiItems;
			CreativeModeTab g = e.getKey();
			boolean dups = false;
			Item lastItem = null;
			ItemStack lastStack = null;
			for (ItemStack s : g.getSearchTabDisplayItems())
			{
				Item item = s.getItem();
				if (item == lastItem)
				{
					if (!dups)
					{
						dups = true;
						work.add(lastStack);
					}
					work.add(s);
				}
				else
				{
					if (dups)
					{
						processItem(multiItems, lastItem, work);
						work.clear();
						dups = false;
					}
					lastItem = item;
				}
				lastStack = s;
			}
			if (dups)
			{
				processItem(multiItems, lastItem, work);
				work.clear();
			}
		}
		groupMultiItems.remove(Items.PAINTING);
		allMultiItems.remove(Items.PAINTING);
		for (Entry<Item, Set<ItemStack>> e : groupMultiItems.entrySet())
			processItem(allMultiItems, e.getKey(), e.getValue());
	}

	private void processItem(Map<Item, Set<ItemStack>> multiItems, Item item, Set<ItemStack> work)
	{
		Set<ItemStack> work2 = multiItems.get(item);
		if (work2 != null)
		{
			Set<ItemStack> temp = ItemStackLinkedSet.createTypeAndComponentsSet();
			temp.addAll(work2);
			temp.addAll(work);
			if (temp.size() != work2.size())
			{
				work2.clear();
				work2.addAll(temp);
			}
		}
		else
			multiItems.put(item, new HashSet<>(work));
	}

	private void buildItemList(Collection<Item> set)
	{
		Collection<Item> items = MyConfig.includeAllItems ? ForgeRegistries.ITEMS.getValues() : MyConfig.includeItemSet;
		FeatureFlagSet featureFlagSet = level.enabledFeatures();
		for (Item item : items)
		{
			if (item.isEnabled(featureFlagSet))
				set.add(item);
		}
	}

	private void addItemList(Collection<Item> set, Collection<Item> items)
	{
		FeatureFlagSet featureFlagSet = level.enabledFeatures();
		for (Item item : items)
		{
			if (item.isEnabled(featureFlagSet))
				set.add(item);
		}
	}

	private void fillMultiItems()
	{
		groupMultiItems.clear();
		allMultiItems.clear();
		MultiItems.generate(groupMultiItems, level.registryAccess(), level.enabledFeatures());
		allMultiItems.putAll(groupMultiItems);
	}

	private void fillItems(Map<Item, Set<ItemStack>> multiItems, Item item, NonNullList<ItemStack> items)
	{
		Set<ItemStack> v = multiItems.get(item);
		if (v != null)
			items.addAll(v);
	}

	private void filterMods(Collection<Item> set)
	{
		HashSet<String> includeSet = MyConfig.includeModSet;
		HashSet<String> excludeSet = MyConfig.excludeModSet;
		boolean addAll = includeSet.contains("*");
		set.removeIf(item ->
		{
			String name = ForgeRegistries.ITEMS.getKey(item).getNamespace();
			if (!(addAll || includeSet.contains(name)) ||
				excludeSet.contains(name))
				return true;
			return false;
		});
	}

	private void filterCost(Collection<Item> set)
	{
		HashMap<Item, Rarity> map = MyConfig.itemRarityMap;
		HashMap<Item, CostData> map2 = MyConfig.itemCostMap;
		if (MyConfig.commonCost == 0)
		{
			set.removeIf(item ->
			{
				Rarity rarity = map.get(item);
				if (rarity == null)
					rarity = item.components().getOrDefault(DataComponents.RARITY, Rarity.COMMON);
				return (rarity == Rarity.COMMON) && !map2.containsKey(item);
			});
		}
		if (MyConfig.uncommonCost == 0)
		{
			set.removeIf(item ->
			{
				Rarity rarity = map.get(item);
				if (rarity == null)
					rarity = item.components().getOrDefault(DataComponents.RARITY, Rarity.COMMON);
				return (rarity == Rarity.UNCOMMON) && !map2.containsKey(item);
			});
		}
		if (MyConfig.rareCost == 0)
		{
			set.removeIf(item ->
			{
				Rarity rarity = map.get(item);
				if (rarity == null)
					rarity = item.components().getOrDefault(DataComponents.RARITY, Rarity.COMMON);
				return (rarity == Rarity.RARE) && !map2.containsKey(item);
			});
		}
		if (MyConfig.epicCost == 0)
		{
			set.removeIf(item ->
			{
				Rarity rarity = map.get(item);
				if (rarity == null)
					rarity = item.components().getOrDefault(DataComponents.RARITY, Rarity.COMMON);
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

		String modName = ForgeRegistries.ITEMS.getKey(item).getNamespace();
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
