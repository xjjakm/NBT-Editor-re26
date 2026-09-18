package com.luneruniverse.minecraft.mod.nbteditor.screens.factories;

import com.luneruniverse.minecraft.mod.nbteditor.localnbt.LocalEntity;
import com.luneruniverse.minecraft.mod.nbteditor.localnbt.LocalItem;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.TextInst;
import com.luneruniverse.minecraft.mod.nbteditor.nbtreferences.NBTReference;
import com.luneruniverse.minecraft.mod.nbteditor.nbtreferences.itemreferences.ItemReference;
import com.luneruniverse.minecraft.mod.nbteditor.screens.LocalEditorScreen;
import com.luneruniverse.minecraft.mod.nbteditor.screens.configurable.*;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.ItemTagReferences;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.TypedEntityData;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 刷怪蛋专属高级编辑器：直接编辑 entity_data 组件里的实体 NBT 标签。
 * 适用于任何 SpawnEggItem，不依赖实体是否实际加载。
 */
public class SpawnEggAttributesScreen extends LocalEditorScreen<LocalItem> {
	
	// ---------- attributes 列表里的 attribute id ----------
	private static final String MAX_HEALTH_ID = "minecraft:max_health";
	private static final String MOVEMENT_SPEED_ID = "minecraft:movement_speed";
	private static final String ATTACK_DAMAGE_ID = "minecraft:attack_damage";
	private static final String FOLLOW_RANGE_ID = "minecraft:follow_range";
	
	private ConfigCategory config;
	private ConfigPanel panel;
	
	// 生命值
	private ConfigValueNumber<Double> healthCurrent;
	private ConfigValueNumber<Double> healthMax;
	
	// A 类：attributes 数字属性
	private ConfigValueNumber<Double> movementSpeed;
	private ConfigValueNumber<Double> attackDamage;
	private ConfigValueNumber<Double> followRange;
	
	// B 类：布尔开关（EntityAttrState 三态）
	private ConfigValueDropdown<EntityAttrState> noAi;
	private ConfigValueDropdown<EntityAttrState> invulnerable;
	private ConfigValueDropdown<EntityAttrState> noGravity;
	private ConfigValueDropdown<EntityAttrState> silent;
	private ConfigValueDropdown<EntityAttrState> canBreatheUnderwater;
	private ConfigValueDropdown<EntityAttrState> fireImmune;
	private ConfigValueDropdown<EntityAttrState> customNameVisible;
	private ConfigValueDropdown<EntityAttrState> glowing;
	
	// B 类 Mob 专属（null 表示当前 entityType 不是 Mob）
	private ConfigValueDropdown<EntityAttrState> persistenceRequired;
	private ConfigValueDropdown<EntityAttrState> canPickUpLoot;
	
	// C 类：数值型 LivingEntity 标签
	private ConfigValueNumber<Double> fallDistance;
	private ConfigValueNumber<Double> absorptionAmount;
	private ConfigValueNumber<Integer> air;
	
	// 幼体（null 表示当前 entityType 不是 AgeableMob）
	private ConfigValueDropdown<EntityAttrState> baby;
	
	private final ItemStack itemStack;
	private EntityType<?> entityType;
	private CompoundTag entityNbt;
	
	public SpawnEggAttributesScreen(ItemReference ref) {
		super(TextInst.translatable("nbteditor.spawn_egg_attributes"), ref);
		
		this.itemStack = localNBT.getEditableItem();
		this.entityType = SpawnEggItem.getType(itemStack);
		
		TypedEntityData<EntityType<?>> entityData = itemStack.get(DataComponents.ENTITY_DATA);
		this.entityNbt = (entityData != null) ? entityData.getUnsafe().copy() : new CompoundTag();
		
		rebuildConfig();
	}
	
	/**
	 * 从当前 itemStack / entityNbt 重建 ConfigCategory 和所有控件。
	 * 实体类型下拉改变时、保存后都会调。
	 */
	private void rebuildConfig() {
		boolean isMob = Mob.class.isAssignableFrom(entityType.getBaseClass());
		boolean canBeBaby = AgeableMob.class.isAssignableFrom(entityType.getBaseClass());
		
		this.config = new ConfigCategory();
		
		// ---------- 实体类型（可改生成什么生物） ----------
		List<EntityType<?>> allEntityTypes = new ArrayList<>();
		for (EntityType<?> t : BuiltInRegistries.ENTITY_TYPE)
			if (t != null) allEntityTypes.add(t);
		ConfigValueOverlay<EntityType<?>> entityTypeSelector = ConfigValueOverlay.forList(
				TextInst.translatable("nbteditor.spawn_egg_attributes.entity_type"),
				entityType, entityType, allEntityTypes,
				ignored -> false,
				t -> TextInst.translatable(t.getDescriptionId()).getString()
						+ " (" + EntityType.getKey(t) + ")");
		entityTypeSelector.addValueListener(source -> {
			this.entityType = source.getValidValue();
			// 只更新 NBT + 写 itemStack,不重建 UI;保存后 onAfterSave 会统一重建
			ItemTagReferences.ENTITY_DATA.set(itemStack, TypedEntityData.of(entityType, entityNbt));
			checkSave();
		});
		config.setConfigurable("entity_type", new ConfigItem<>(
				TextInst.translatable("nbteditor.spawn_egg_attributes.entity_type"),
				entityTypeSelector));
		
		// ---------- 生命值 ----------
		double curHealth = entityNbt.getFloat("Health").orElse(20.0f);
		double maxHealth = EntityAttrsUtil.getAttributeBase(entityNbt, MAX_HEALTH_ID);
		if (maxHealth <= 0)
			maxHealth = curHealth;
		healthCurrent = ConfigValueNumber.forDouble(curHealth, curHealth, 0.0, 1000000000.0);
		healthCurrent.addValueListener(_ -> writeNbt());
		healthMax = ConfigValueNumber.forDouble(maxHealth, maxHealth, 1.0, 1000000000.0);
		healthMax.addValueListener(_ -> writeNbt());
		ConfigCategory health = new ConfigCategory(TextInst.translatable("nbteditor.entity_attributes.health"));
		health.setConfigurable("current", new ConfigItem<>(TextInst.translatable("nbteditor.entity_attributes.health.current"), healthCurrent));
		health.setConfigurable("max", new ConfigItem<>(TextInst.translatable("nbteditor.entity_attributes.health.max"), healthMax));
		config.setConfigurable("health", health);
		
		// ---------- A 类：attributes 数字属性 ----------
		double curSpeed = EntityAttrsUtil.getAttributeBase(entityNbt, MOVEMENT_SPEED_ID);
		if (curSpeed < 0) curSpeed = 0.25;
		movementSpeed = ConfigValueNumber.forDouble(curSpeed, curSpeed, 0.0, 10000.0);
		movementSpeed.addValueListener(_ -> writeNbt());
		config.setConfigurable("movement_speed", new ConfigItem<>(
				TextInst.translatable("nbteditor.entity_attributes.movement_speed"), movementSpeed));
		
		double curAtk = EntityAttrsUtil.getAttributeBase(entityNbt, ATTACK_DAMAGE_ID);
		if (curAtk < 0) curAtk = 2.0;
		attackDamage = ConfigValueNumber.forDouble(curAtk, curAtk, 0.0, 100000.0);
		attackDamage.addValueListener(_ -> writeNbt());
		config.setConfigurable("attack_damage", new ConfigItem<>(
				TextInst.translatable("nbteditor.entity_attributes.attack_damage"), attackDamage));
		
		double curFollow = EntityAttrsUtil.getAttributeBase(entityNbt, FOLLOW_RANGE_ID);
		if (curFollow < 0) curFollow = 32.0;
		followRange = ConfigValueNumber.forDouble(curFollow, curFollow, 0.0, 100000.0);
		followRange.addValueListener(_ -> writeNbt());
		config.setConfigurable("follow_range", new ConfigItem<>(
				TextInst.translatable("nbteditor.entity_attributes.follow_range"), followRange));
		
		// ---------- B 类：布尔开关 ----------
		noAi = addToggle("no_ai", "nbteditor.entity_attributes.no_ai", "NoAI");
		invulnerable = addToggle("invulnerable", "nbteditor.entity_attributes.invulnerable", "Invulnerable");
		noGravity = addToggle("no_gravity", "nbteditor.entity_attributes.no_gravity", "NoGravity");
		silent = addToggle("silent", "nbteditor.entity_attributes.silent", "Silent");
		canBreatheUnderwater = addToggle("can_breathe_underwater",
				"nbteditor.entity_attributes.can_breathe_underwater", "CanBreatheUnderwater");
		fireImmune = addToggle("fire_immune", "nbteditor.entity_attributes.fire_immune", "FireImmune");
		customNameVisible = addToggle("custom_name_visible",
				"nbteditor.spawn_egg_attributes.custom_name_visible", "CustomNameVisible");
		glowing = addToggle("glowing", "nbteditor.spawn_egg_attributes.glowing", "Glowing");
		
		// ---------- B 类 Mob 专属 ----------
		if (isMob) {
			persistenceRequired = addToggle("persistence_required",
					"nbteditor.spawn_egg_attributes.persistence_required", "PersistenceRequired");
			canPickUpLoot = addToggle("can_pick_up_loot",
					"nbteditor.spawn_egg_attributes.can_pick_up_loot", "CanPickUpLoot");
		} else {
			persistenceRequired = null;
			canPickUpLoot = null;
		}
		
		// ---------- C 类：LivingEntity 数值标签 ----------
		double curFallDist = entityNbt.getFloat("FallDistance").orElse(0.0f);
		fallDistance = ConfigValueNumber.forDouble(curFallDist, curFallDist, -1.0, 1000.0);
		fallDistance.addValueListener(_ -> writeNbt());
		config.setConfigurable("fall_distance", new ConfigItem<>(
				TextInst.translatable("nbteditor.entity_attributes.fall_distance"), fallDistance));
		
		double curAbsorb = entityNbt.getFloat("AbsorptionAmount").orElse(0.0f);
		absorptionAmount = ConfigValueNumber.forDouble(curAbsorb, curAbsorb, 0.0, 1000.0);
		absorptionAmount.addValueListener(_ -> writeNbt());
		config.setConfigurable("absorption_amount", new ConfigItem<>(
				TextInst.translatable("nbteditor.entity_attributes.absorption_amount"), absorptionAmount));
		
		int curAir = entityNbt.getInt("Air").orElse(300);
		air = ConfigValueNumber.forInt(curAir, curAir, -1, 10000);
		air.addValueListener(_ -> writeNbt());
		config.setConfigurable("air", new ConfigItem<>(
				TextInst.translatable("nbteditor.entity_attributes.air"), air));
		
		// ---------- 幼体 ----------
		if (canBeBaby) {
			boolean currentIsBaby = entityNbt.getInt("Age").orElse(0) < 0;
			baby = ConfigValueDropdown.forEnum(
					EntityAttrState.KEEP, EntityAttrState.KEEP, EntityAttrState.class);
			baby.addValueListener(_ -> writeNbt());
			config.setConfigurable("baby", new ConfigItem<>(
					EntityAttrsUtil.titleWithCurrent("nbteditor.entity_attributes.baby", currentIsBaby), baby));
		} else {
			baby = null;
		}
		
		// ---------- 村民交易入口（仅 VILLAGER / ZOMBIE_VILLAGER） ----------
		if (entityType == EntityTypes.VILLAGER || entityType == EntityTypes.ZOMBIE_VILLAGER) {
			config.setConfigurable("villager_link",
					new ConfigButton(200, TextInst.translatable("nbteditor.spawn_egg_attributes.open_villager"),
							_ -> openVillagerEditor()));
		}
	}
	
	private ConfigValueDropdown<EntityAttrState> addToggle(String path, String langKey, String nbtKey) {
		boolean currentOn = entityNbt.get(nbtKey) != null;
		ConfigValueDropdown<EntityAttrState> state = ConfigValueDropdown.forEnum(
				EntityAttrState.KEEP, EntityAttrState.KEEP, EntityAttrState.class);
		state.addValueListener(_ -> writeNbt());
		config.setConfigurable(path, new ConfigItem<>(
				EntityAttrsUtil.titleWithCurrent(langKey, currentOn), state));
		return state;
	}
	
	private void writeNbt() {
		// 生命值
		entityNbt.putFloat("Health", healthCurrent.getValidValue().floatValue());
		EntityAttrsUtil.setAttributeBase(entityNbt, MAX_HEALTH_ID, healthMax.getValidValue());
		
		// A 类：attributes 数字属性
		EntityAttrsUtil.setAttributeBase(entityNbt, MOVEMENT_SPEED_ID, movementSpeed.getValidValue());
		EntityAttrsUtil.setAttributeBase(entityNbt, ATTACK_DAMAGE_ID, attackDamage.getValidValue());
		EntityAttrsUtil.setAttributeBase(entityNbt, FOLLOW_RANGE_ID, followRange.getValidValue());
		
		// B 类：布尔
		EntityAttrsUtil.applyBooleanTag(entityNbt, "NoAI", noAi);
		EntityAttrsUtil.applyBooleanTag(entityNbt, "Invulnerable", invulnerable);
		EntityAttrsUtil.applyBooleanTag(entityNbt, "NoGravity", noGravity);
		EntityAttrsUtil.applyBooleanTag(entityNbt, "Silent", silent);
		EntityAttrsUtil.applyBooleanTag(entityNbt, "CanBreatheUnderwater", canBreatheUnderwater);
		EntityAttrsUtil.applyBooleanTag(entityNbt, "FireImmune", fireImmune);
		EntityAttrsUtil.applyBooleanTag(entityNbt, "CustomNameVisible", customNameVisible);
		EntityAttrsUtil.applyBooleanTag(entityNbt, "Glowing", glowing);
		if (persistenceRequired != null)
			EntityAttrsUtil.applyBooleanTag(entityNbt, "PersistenceRequired", persistenceRequired);
		if (canPickUpLoot != null)
			EntityAttrsUtil.applyBooleanTag(entityNbt, "CanPickUpLoot", canPickUpLoot);
		
		// C 类：LivingEntity 数值标签
		entityNbt.putFloat("FallDistance", fallDistance.getValidValue().floatValue());
		entityNbt.putFloat("AbsorptionAmount", absorptionAmount.getValidValue().floatValue());
		entityNbt.putInt("Air", air.getValidValue());
		
		// 幼体
		if (baby != null) {
			switch (baby.getValidValue()) {
				case KEEP -> {}
				case ON -> entityNbt.putInt("Age", EntityAttrsUtil.BABY_AGE);
				case OFF -> {
					entityNbt.putInt("Age", 0);
					entityNbt.remove("AgeLocked");
				}
			}
		}
		
		// 写回 TypedEntityData 到物品组件
		ItemTagReferences.ENTITY_DATA.set(itemStack, TypedEntityData.of(entityType, entityNbt));
		
		checkSave();
	}
	
	/** 保存后重建：从最新 itemStack 读 entityType + entityNbt，重建 ConfigCategory 并替换 panel 内部引用 */
	@Override
	protected void onAfterSave() {
		TypedEntityData<EntityType<?>> entityData = itemStack.get(DataComponents.ENTITY_DATA);
		this.entityType = SpawnEggItem.getType(itemStack);
		this.entityNbt = (entityData != null) ? entityData.getUnsafe().copy() : new CompoundTag();
		
		rebuildConfig();
		if (panel != null) {
			int scroll = panel.getScroll();
			panel.setConfig(config);
			panel.setScroll(scroll);
		}
	}
	
	/**
	 * 把 VillagerScreen 作为子编辑器打开，保存时结果写回 itemStack 的 entity_data。
	 */
	private void openVillagerEditor() {
		NBTReference<LocalEntity> villagerRef = new NBTReference<>() {
			@Override
			public LocalEntity getLocalNBT() {
				return new LocalEntity(entityType, entityNbt.copy());
			}
			@Override
			public boolean exists() {
				return true;
			}
			@Override
			public Identifier getId() {
				return EntityType.getKey(entityType);
			}
			@Override
			public CompoundTag getNBT() {
				return entityNbt.copy();
			}
			@Override
			public void saveNBT(Identifier id, CompoundTag toSave, Runnable onFinished) {
				ItemTagReferences.ENTITY_DATA.set(itemStack, TypedEntityData.of(entityType, toSave));
				onFinished.run();
			}
		};
		VillagerScreen villagerScreen = new VillagerScreen(villagerRef, true);
		villagerScreen.setSaveReturnTarget(SpawnEggAttributesScreen.this);
		MainUtil.client.gui.setScreen(villagerScreen);
	}
	
	@Override
	protected void initEditor() {
		ConfigPanel newPanel = addRenderableWidget(new ConfigPanel(16, 64, width - 32, height - 80, config));
		if (panel != null)
			newPanel.setScroll(panel.getScroll());
		panel = newPanel;
	}
	
	@Override
	protected void renderEditor(Matrix3x2fStack matrices, int mouseX, int mouseY, float delta) {
	}
	
}
