package com.luneruniverse.minecraft.mod.nbteditor.screens.factories;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DynamicOps;

import com.luneruniverse.minecraft.mod.nbteditor.localnbt.LocalItem;
import com.luneruniverse.minecraft.mod.nbteditor.localnbt.LocalNBT;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.DynamicRegistryManagerHolder;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.IdentifierInst;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVRegistry;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.manager.NBTManagers;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.TextInst;
import com.luneruniverse.minecraft.mod.nbteditor.nbtreferences.NBTReference;
import com.luneruniverse.minecraft.mod.nbteditor.nbtreferences.EntityReference;
import com.luneruniverse.minecraft.mod.nbteditor.nbtreferences.itemreferences.ItemReference;
import com.luneruniverse.minecraft.mod.nbteditor.screens.LocalEditorScreen;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;
import com.luneruniverse.minecraft.mod.nbteditor.screens.configurable.ConfigBar;
import com.luneruniverse.minecraft.mod.nbteditor.screens.configurable.ConfigButton;
import com.luneruniverse.minecraft.mod.nbteditor.screens.configurable.ConfigCategory;
import com.luneruniverse.minecraft.mod.nbteditor.screens.configurable.ConfigHiddenData;
import com.luneruniverse.minecraft.mod.nbteditor.screens.configurable.ConfigItem;
import com.luneruniverse.minecraft.mod.nbteditor.screens.configurable.ConfigList;
import com.luneruniverse.minecraft.mod.nbteditor.screens.configurable.ConfigPanel;
import com.luneruniverse.minecraft.mod.nbteditor.screens.configurable.ConfigPath;
import com.luneruniverse.minecraft.mod.nbteditor.screens.configurable.ConfigValueDropdown;
import com.luneruniverse.minecraft.mod.nbteditor.screens.configurable.ConfigValueNumber;
import com.luneruniverse.minecraft.mod.nbteditor.screens.widgets.ItemSelectOverlay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

public class VillagerScreen<L extends LocalNBT> extends LocalEditorScreen<L> {
	
	// 职业显示名称 → 注册表 id
	private static final Map<String, String> PROFESSION_DISPLAY_TO_ID = new LinkedHashMap<>();
	private static final Map<String, String> ID_TO_PROFESSION_DISPLAY = new LinkedHashMap<>();
	private static final List<String> PROFESSION_DISPLAYS = new ArrayList<>();
	// 村民类型显示名称 → 注册表 id
	private static final Map<String, String> TYPE_DISPLAY_TO_ID = new LinkedHashMap<>();
	private static final Map<String, String> ID_TO_TYPE_DISPLAY = new LinkedHashMap<>();
	private static final List<String> TYPE_DISPLAYS = new ArrayList<>();
	static {
		for (Holder<VillagerProfession> holder : BuiltInRegistries.VILLAGER_PROFESSION.asHolderIdMap()) {
			VillagerProfession profession = holder.value();
			Identifier id = holder.unwrapKey().map(ResourceKey::identifier)
					.orElseGet(() -> BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession));
			if (id == null)
				continue;
			// 职业使用原版官方翻译 entity.minecraft.villager.<path>
			String display = profession.name().getString();
			PROFESSION_DISPLAY_TO_ID.put(display, id.toString());
			ID_TO_PROFESSION_DISPLAY.put(id.toString(), display);
		}
		PROFESSION_DISPLAY_TO_ID.entrySet().stream()
				.sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
				.forEach(entry -> PROFESSION_DISPLAYS.add(entry.getKey()));
		
		for (Holder<VillagerType> holder : BuiltInRegistries.VILLAGER_TYPE.asHolderIdMap()) {
			VillagerType type = holder.value();
			Identifier id = holder.unwrapKey().map(ResourceKey::identifier)
					.orElseGet(() -> BuiltInRegistries.VILLAGER_TYPE.getKey(type));
			if (id == null)
				continue;
			// 类型用标准语言文件的 biome.<namespace>.<path> 翻译
			String display = getTypeDisplayName(id);
			TYPE_DISPLAY_TO_ID.put(display, id.toString());
			ID_TO_TYPE_DISPLAY.put(id.toString(), display);
		}
		TYPE_DISPLAY_TO_ID.entrySet().stream()
				.sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
				.forEach(entry -> TYPE_DISPLAYS.add(entry.getKey()));
	}
	
	@SuppressWarnings("unchecked")
	private static ConfigHiddenData<ConfigCategory, SlotData> getSlot(ConfigCategory entry, String key) {
		return (ConfigHiddenData<ConfigCategory, SlotData>) entry.getConfigurable(key);
	}
	@SuppressWarnings("unchecked")
	private static ConfigValueNumber<Integer> getSlotCount(ConfigCategory entry, String key) {
		ConfigPath visible = getSlot(entry, key).getVisible();
		if (visible instanceof ConfigCategory cat && cat.getConfigurable("main") instanceof ConfigBar bar)
			return ((ConfigItem<ConfigValueNumber<Integer>>) bar.getConfigurable("count")).getValue();
		throw new IllegalStateException("Unexpected slot layout: " + key);
	}
	@SuppressWarnings("unchecked")
	private static ConfigValueNumber<Integer> getConfigNumber(ConfigCategory entry, String key) {
		return ((ConfigItem<ConfigValueNumber<Integer>>) entry.getConfigurable(key)).getValue();
	}
	
	private static String getTypeDisplayName(Identifier id) {
		String key = "biome." + id.getNamespace() + "." + id.getPath();
		String display = TextInst.translatable(key).getString();
		if (!display.equals(key))
			return display;
		// 原版村民类型 "snow" 没有对应的 biome.snow 翻译，映射到积雪的平原
		if (id.toString().equals("minecraft:snow"))
			return TextInst.translatable("biome.minecraft.snowy_plains").getString();
		return id.getPath();
	}
	
	private static String getItemId(Item item) {
		Identifier id = MVRegistry.ITEM.getId(item);
		return id == null ? "minecraft:air" : id.toString();
	}
	
	/**
	 * 交易条目的单个物品槽位（收购A/收购B/出售）数据
	 */
	private static class SlotData {
		public ItemSelectOverlay.ItemEntry item; // null 表示未选择
		public CompoundTag nbt; // null 表示无额外 NBT
		public SlotData(ItemSelectOverlay.ItemEntry item, CompoundTag nbt) {
			this.item = item;
			this.nbt = nbt;
		}
		public SlotData copy() {
			return new SlotData(item, nbt == null ? null : nbt.copy());
		}
	}
	
	/**
	 * 仅存在于内存中的物品引用：编辑后通过回调把结果写回交易条目
	 */
	private static class InMemoryItemReference implements ItemReference {
		private ItemStack item;
		private final Consumer<ItemStack> onSave;
		private final Runnable onShowParent;
		public InMemoryItemReference(ItemStack item, Consumer<ItemStack> onSave, Runnable onShowParent) {
			this.item = item;
			this.onSave = onSave;
			this.onShowParent = onShowParent;
		}
		@Override
		public boolean exists() {
			return true;
		}
		@Override
		public ItemStack getItem() {
			return item;
		}
		@Override
		public void saveItem(ItemStack toSave, Runnable onFinished) {
			this.item = toSave;
			onSave.accept(toSave);
			onFinished.run();
		}
		@Override
		public Identifier getId() {
			return MVRegistry.ITEM.getId(item.getItem());
		}
		@Override
		public boolean isLocked() {
			return false;
		}
		@Override
		public boolean isLockable() {
			return false;
		}
		@Override
		public int getBlockedSlot() {
			return -1;
		}
		@Override
		public void showParent() {
			onShowParent.run();
		}
	}
	
	private static ItemSelectOverlay.ItemEntry toItemEntry(String id) {
		if (id == null || id.isEmpty())
			return null;
		Item item;
		try {
			item = MVRegistry.ITEM.getOrEmpty(IdentifierInst.of(id)).orElse(Items.AIR);
		} catch (Exception e) {
			item = Items.AIR;
		}
		if (item == Items.AIR && !id.equals("minecraft:air"))
			return null;
		Identifier registryId = MVRegistry.ITEM.getId(item);
		if (registryId == null)
			return null;
		return new ItemSelectOverlay.ItemEntry(item, item.getName(new ItemStack(item)).getString(), registryId.toString());
	}
	
	private static Component getSlotDisplay(SlotData data) {
		if (data.item == null)
			return TextInst.translatable("nbteditor.villager.choose_item");
		return TextInst.literal(data.item.displayName() + " (" + data.item.itemId() + ")");
	}
	
	/**
	 * 通过 Configurable.PARENTS 弱引用链从按钮反查所属槽位的 SlotData
	 */
	private static SlotData getParentSlotData(ConfigPath path) {
		ConfigPath parent = path.getParent();
		while (parent != null) {
			if (parent instanceof ConfigHiddenData<?, ?> hidden && hidden.getData() instanceof SlotData data)
				return data;
			parent = parent.getParent();
		}
		return null;
	}
	
	private ItemStack buildStack(SlotData data, int count) {
		if (data.item == null)
			return ItemStack.EMPTY;
		ItemStack stack = new ItemStack(data.item.item());
		if (data.nbt != null && !data.nbt.isEmpty())
			NBTManagers.ITEM.setNbt(stack, data.nbt);
		stack.setCount(count);
		return stack;
	}
	
	private static DataComponentExactPredicate nbtToPredicate(CompoundTag nbt) {
		if (nbt == null || nbt.isEmpty())
			return DataComponentExactPredicate.EMPTY;
		return DataComponentExactPredicate.CODEC.decode(
				DynamicRegistryManagerHolder.getManager().createSerializationContext(NbtOps.INSTANCE), nbt.copy())
				.result().map(Pair::getFirst).orElse(DataComponentExactPredicate.EMPTY);
	}
	
	private static ItemCost toCost(ItemStack stack, CompoundTag nbt) {
		DataComponentExactPredicate pred = nbtToPredicate(nbt);
		if (pred.isEmpty())
			return new ItemCost(stack.getItem(), stack.getCount());
		return new ItemCost(BuiltInRegistries.ITEM.wrapAsHolder(stack.getItem()), stack.getCount(), pred);
	}
	
	private static CompoundTag getCostNbt(ItemCost cost) {
		CompoundTag nbt = NBTManagers.ITEM.getNbt(cost.itemStack());
		if (nbt != null)
			nbt.remove("count");
		return nbt;
	}
	
	private final ConfigCategory config;
	private final ConfigList offers;
	private ConfigPanel panel;
	private VillagerData villagerData;
	private int villagerXp;
	
	/**
	 * 创建一个交易配置项。null 的 ItemEntry 表示该槽位没有物品（buyB 可空）。
	 */
	private ConfigCategory createOfferEntry(String buyId, int buyCount, CompoundTag buyNbt,
			String buyBId, int buyBCount, CompoundTag buyBNbt,
			String sellId, int sellCount, CompoundTag sellNbt, int maxUses, int xp) {
		ConfigCategory entry = new ConfigCategory(TextInst.translatable("nbteditor.villager.offer"));
		entry.setConfigurable("buy", createItemSlot("nbteditor.villager.buy", buyId, buyCount, buyNbt, false));
		entry.setConfigurable("buyB", createItemSlot("nbteditor.villager.buy_b", buyBId, buyBCount, buyBNbt, true));
		entry.setConfigurable("sell", createItemSlot("nbteditor.villager.sell", sellId, sellCount, sellNbt, false));
		entry.setConfigurable("max_uses", new ConfigItem<>(TextInst.translatable("nbteditor.villager.max_uses"),
				ConfigValueNumber.forInt(maxUses, 12, 1, Integer.MAX_VALUE)));
		entry.setConfigurable("xp", new ConfigItem<>(TextInst.translatable("nbteditor.villager.offer_xp"),
				ConfigValueNumber.forInt(xp, 1, 0, Integer.MAX_VALUE)));
		return entry;
	}
	
	/**
	 * 创建一个物品槽位（垂直布局）：标签在顶部，下一行 物品名按钮 + NBT 按钮 + 数量
	 * @param allowClear 是否可在物品选择弹窗里清空该槽位（设为 null 表示无物品，如 buyB）
	 */
	private ConfigHiddenData<ConfigCategory, SlotData> createItemSlot(String labelKey, String itemId,
			int count, CompoundTag nbt, boolean allowClear) {
		SlotData slotData = new SlotData(toItemEntry(itemId), nbt == null ? null : nbt.copy());
		ConfigButton itemButton = new ConfigButton(140, getSlotDisplay(slotData), btn -> {
			ItemSelectOverlay.show(selected -> {
				SlotData target = getParentSlotData(btn);
				if (target == null)
					return;
				target.item = selected;
				target.nbt = null;
				btn.setMessage(getSlotDisplay(target));
				writeNbt();
			}, allowClear);
		});
		ConfigButton nbtButton = new ConfigButton(50, TextInst.translatable("nbteditor.villager.nbt"), btn -> {
			SlotData target = getParentSlotData(btn);
			if (target == null || target.item == null) {
				if (MainUtil.client.player != null)
					MainUtil.client.player.sendSystemMessage(TextInst.translatable("nbteditor.villager.select_item_first"));
				return;
			}
			openItemNbtEditor(btn, target);
		});
		ConfigCategory slot = new ConfigCategory(TextInst.translatable(labelKey));
		slot.setConfigurable("main", new ConfigBar(null)
				.setConfigurable("name", itemButton)
				.setConfigurable("nbt", nbtButton)
				.setConfigurable("count", new ConfigItem<>(TextInst.translatable("nbteditor.villager.count"),
						ConfigValueNumber.forInt(count, 1, 1, 64))));
		return new ConfigHiddenData<>(slot, slotData, (data, defaults) -> data.copy());
	}
	
	/**
	 * 打开该槽位物品的 NBT 编辑界面，保存后把结果写回槽位并返回本界面
	 */
	private void openItemNbtEditor(ConfigPath nbtButton, SlotData data) {
		ItemStack stack = new ItemStack(data.item.item());
		if (data.nbt != null && !data.nbt.isEmpty())
			NBTManagers.ITEM.setNbt(stack, data.nbt);
		InMemoryItemReference ref = new InMemoryItemReference(stack, edited -> {
			data.nbt = NBTManagers.ITEM.getNbt(edited);
			ItemSelectOverlay.ItemEntry newEntry = toItemEntry(getItemId(edited.getItem()));
			if (newEntry != null)
				data.item = newEntry;
			// 同步物品按钮文本（NBT 按钮与 name 按钮同属一个 ConfigBar）
			if (nbtButton.getParent() instanceof ConfigBar bar
					&& bar.getConfigurable("name") instanceof ConfigButton nameButton)
				nameButton.setMessage(getSlotDisplay(data));
			writeNbt();
		}, () -> minecraft.gui.setScreen(this));
		minecraft.gui.setScreen(new ItemNBTScreen(ref, this));
	}
	
	/**
	 * 从界面收集所有交易，重建 MerchantOffers
	 */
	private MerchantOffers buildOffers() {
		MerchantOffers result = new MerchantOffers();
		for (ConfigPath path : offers.getConfigurables().values()) {
			ConfigCategory entry = (ConfigCategory) path;
			SlotData buy = getSlot(entry, "buy").getData();
			if (buy.item == null)
				continue;
			ItemCost costA = toCost(buildStack(buy, getSlotCount(entry, "buy").getConfigValue()), buy.nbt);
			
			Optional<ItemCost> costB = Optional.empty();
			SlotData buyB = getSlot(entry, "buyB").getData();
			if (buyB.item != null)
				costB = Optional.of(toCost(buildStack(buyB, getSlotCount(entry, "buyB").getConfigValue()), buyB.nbt));
			
			SlotData sell = getSlot(entry, "sell").getData();
			ItemStack sellStack = buildStack(sell, getSlotCount(entry, "sell").getConfigValue());
			
			int maxUses = getConfigNumber(entry, "max_uses").getConfigValue();
			int xp = getConfigNumber(entry, "xp").getConfigValue();
			result.add(new MerchantOffer(costA, costB, sellStack, 0, maxUses, xp, 0.0f, 0));
		}
		return result;
	}
	
	/**
	 * 将当前 UI 状态通过原版 codec 写回实体 NBT（VillagerData / Offers / Xp）
	 */
	private void writeNbt() {
		CompoundTag nbt = localNBT.getOrCreateNBT();
		VillagerData.CODEC.encodeStart(NbtOps.INSTANCE, villagerData).result()
				.ifPresent(tag -> nbt.put("VillagerData", tag));
		DynamicOps<Tag> ops = DynamicRegistryManagerHolder.getManager().createSerializationContext(NbtOps.INSTANCE);
		MerchantOffers.CODEC.encodeStart(ops, buildOffers()).result()
				.ifPresent(tag -> nbt.put("Offers", tag));
		nbt.putInt("Xp", villagerXp);
		nbt.putBoolean("VillagerDataFinalized", true);
		checkSave();
	}
	
	private static final Logger LOGGER = LoggerFactory.getLogger("NBTEditor");

	@SuppressWarnings("unchecked")
	public VillagerScreen(NBTReference<L> ref) {
		this(ref, false);
	}
	
	@SuppressWarnings("unchecked")
	public VillagerScreen(NBTReference<L> ref, boolean skipServerFetch) {
		super(TextInst.translatable("nbteditor.villager"), ref);
		
		// 从 NBT 解码村民数据（避免依赖服务端已加载的实体实例）
		DynamicOps<Tag> ops = DynamicRegistryManagerHolder.getManager().createSerializationContext(NbtOps.INSTANCE);
		CompoundTag nbt = localNBT.getOrCreateNBT();
		this.villagerData = VillagerData.CODEC.decode(ops, nbt.getCompound("VillagerData").orElseGet(CompoundTag::new))
				.resultOrPartial(error -> LOGGER.error("[villager-debug] VillagerData decode failed: {}", error))
				.map(Pair::getFirst)
				.orElseGet(() -> new VillagerData(
						BuiltInRegistries.VILLAGER_TYPE.getOrThrow(VillagerData.DEFAULT_TYPE),
						BuiltInRegistries.VILLAGER_PROFESSION.getOrThrow(VillagerProfession.NONE),
						1));
		this.villagerXp = nbt.getInt("Xp").orElse(0);
		
		// ---------- 村民数据 ----------
		ConfigCategory data = new ConfigCategory(TextInst.translatable("nbteditor.villager.data"));
		
		String professionId = getIdOrFallback(getProfessionId(villagerData.profession()), ID_TO_PROFESSION_DISPLAY, "minecraft:none");
		String professionDisplay = ID_TO_PROFESSION_DISPLAY.getOrDefault(professionId, professionId);
		if (!PROFESSION_DISPLAYS.contains(professionDisplay))
			PROFESSION_DISPLAYS.add(professionDisplay);
		ConfigValueDropdown<String> profession = ConfigValueDropdown.forList(professionDisplay, professionDisplay, PROFESSION_DISPLAYS);
		profession.addValueListener(_ -> {
			String display = profession.getValidValue();
			String id = PROFESSION_DISPLAY_TO_ID.getOrDefault(display, display);
			Holder<VillagerProfession> holder = BuiltInRegistries.VILLAGER_PROFESSION.getOrThrow(
					ResourceKey.create(Registries.VILLAGER_PROFESSION, IdentifierInst.of(id)));
			villagerData = villagerData.withProfession(holder);
			writeNbt();
		});
		data.setConfigurable("profession", new ConfigItem<>(TextInst.translatable("nbteditor.villager.profession"), profession));
		
		String typeId = getIdOrFallback(getTypeId(villagerData.type()), ID_TO_TYPE_DISPLAY, "minecraft:plains");
		String typeDisplay = ID_TO_TYPE_DISPLAY.getOrDefault(typeId, typeId);
		if (!TYPE_DISPLAYS.contains(typeDisplay))
			TYPE_DISPLAYS.add(typeDisplay);
		ConfigValueDropdown<String> type = ConfigValueDropdown.forList(typeDisplay, typeDisplay, TYPE_DISPLAYS);
		type.addValueListener(_ -> {
			String display = type.getValidValue();
			String id = TYPE_DISPLAY_TO_ID.getOrDefault(display, display);
			Holder<VillagerType> holder = BuiltInRegistries.VILLAGER_TYPE.getOrThrow(
					ResourceKey.create(Registries.VILLAGER_TYPE, IdentifierInst.of(id)));
			villagerData = villagerData.withType(holder);
			writeNbt();
		});
		data.setConfigurable("type", new ConfigItem<>(TextInst.translatable("nbteditor.villager.type"), type));
		
		ConfigValueNumber<Integer> level = ConfigValueNumber.forInt(
				villagerData.level(), villagerData.level(),
				VillagerData.MIN_VILLAGER_LEVEL, VillagerData.MAX_VILLAGER_LEVEL);
		level.addValueListener(_ -> {
			villagerData = villagerData.withLevel(level.getValidValue());
			writeNbt();
		});
		data.setConfigurable("level", new ConfigItem<>(TextInst.translatable("nbteditor.villager.level"), level));
		
		ConfigValueNumber<Integer> xp = ConfigValueNumber.forInt(villagerXp, villagerXp, 0, Integer.MAX_VALUE);
		xp.addValueListener(_ -> {
			villagerXp = xp.getValidValue();
			writeNbt();
		});
		data.setConfigurable("xp", new ConfigItem<>(TextInst.translatable("nbteditor.villager.xp"), xp));
		
		// ---------- 交易列表 ----------
		this.offers = new ConfigList(TextInst.translatable("nbteditor.villager.offers"), true,
				createOfferEntry("minecraft:emerald", 1, null, "", 1, null, "minecraft:emerald", 1, null, 12, 1));
		MerchantOffers initialOffers = new MerchantOffers();
		Tag offersTag = nbt.get("Offers");
		LOGGER.info("[villager-debug] Opening VillagerScreen, offersTag={}",
				offersTag == null ? "null" : offersTag.getClass().getSimpleName());
		if (offersTag != null) {
			Optional<MerchantOffers> decoded = MerchantOffers.CODEC.decode(ops, offersTag)
					.resultOrPartial(error -> LOGGER.error("[villager-debug] MerchantOffers decode failed: {}", error))
					.map(Pair::getFirst);
			if (decoded.isPresent()) {
				initialOffers = decoded.get();
			} else {
				// 整体解码失败时逐条容错，单个坏交易不影响其余恢复
				LOGGER.error("[villager-debug] Trying to decode offers one by one...");
				if (offersTag instanceof ListTag list) {
					for (Tag t : list) {
						MerchantOffer.CODEC.decode(ops, t)
								.resultOrPartial(error -> LOGGER.error("[villager-debug] Offer decode failed: {}", error))
								.map(Pair::getFirst)
								.ifPresent(initialOffers::add);
					}
				}
			}
		}
		for (MerchantOffer offer : initialOffers) {
			ItemCost costA = offer.getItemCostA();
			Optional<ItemCost> costB = offer.getItemCostB();
			ItemStack result = offer.getResult();
			offers.addConfigurable(createOfferEntry(
					getItemId(costA.item().value()), costA.count(), getCostNbt(costA),
					costB.map(cost -> getItemId(cost.item().value())).orElse(""), costB.map(ItemCost::count).orElse(1),
					costB.map(VillagerScreen::getCostNbt).orElse(null),
					getItemId(result.getItem()), result.getCount(), NBTManagers.ITEM.getNbt(result),
					offer.getMaxUses(), offer.getXp()));
		}
		offers.addValueListener(_ -> writeNbt());
		
		this.config = new ConfigCategory();
		config.setConfigurable("data", data);
		config.setConfigurable("offers", offers);
		
		// 26.2 客户端实体不保存交易数据（getOffers 在客户端抛异常），
		// 本地快照若无 Offers，则从服务端拉取真实实体 NBT（含 Offers）并用它重建界面
		if (!skipServerFetch && ref instanceof EntityReference entityRef && nbt.get("Offers") == null) {
			LOGGER.info("[villager-debug] No offers in local NBT, fetching from server...");
			EntityReference.getEntity(entityRef.getWorld(), entityRef.getUUID()).thenAccept(optional -> {
				if (optional.isEmpty())
					return;
				EntityReference serverRef = optional.get();
				if (serverRef.getNBT().get("Offers") == null)
					return;
				LOGGER.info("[villager-debug] Loaded offers from server, reopening VillagerScreen");
				MainUtil.client.execute(() -> MainUtil.client.gui.setScreen(new VillagerScreen<>(serverRef, true)));
			});
		}
	}
	
	private static String getIdOrFallback(String requestedId, Map<String, String> idToDisplay, String fallbackId) {
		return idToDisplay.containsKey(requestedId) ? requestedId : fallbackId;
	}
	
	private static String getProfessionId(Holder<VillagerProfession> holder) {
		return holder.unwrapKey().map(key -> key.identifier().toString())
				.orElseGet(() -> {
					Identifier id = BuiltInRegistries.VILLAGER_PROFESSION.getKey(holder.value());
					return id == null ? "minecraft:none" : id.toString();
				});
	}
	private static String getTypeId(Holder<VillagerType> holder) {
		return holder.unwrapKey().map(key -> key.identifier().toString())
				.orElseGet(() -> {
					Identifier id = BuiltInRegistries.VILLAGER_TYPE.getKey(holder.value());
					return id == null ? "minecraft:plains" : id.toString();
				});
	}
	
	@Override
	protected void initEditor() {
		ConfigPanel newPanel = addRenderableWidget(new ConfigPanel(16, 64, width - 32, height - 80, config));
		if (panel != null)
			newPanel.setScroll(panel.getScroll());
		panel = newPanel;
	}
	
}