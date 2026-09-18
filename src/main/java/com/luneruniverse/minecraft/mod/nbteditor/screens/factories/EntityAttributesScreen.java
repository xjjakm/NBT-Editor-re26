package com.luneruniverse.minecraft.mod.nbteditor.screens.factories;

import com.luneruniverse.minecraft.mod.nbteditor.localnbt.LocalNBT;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.TextInst;
import com.luneruniverse.minecraft.mod.nbteditor.nbtreferences.EntityReference;
import com.luneruniverse.minecraft.mod.nbteditor.nbtreferences.NBTReference;
import com.luneruniverse.minecraft.mod.nbteditor.screens.LocalEditorScreen;
import com.luneruniverse.minecraft.mod.nbteditor.screens.configurable.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.AgeableMob;
import org.joml.Matrix3x2fStack;

public class EntityAttributesScreen<L extends LocalNBT> extends LocalEditorScreen<L> {
	
	// ---------- attributes 列表里的 attribute id ----------
	private static final String MAX_HEALTH_ID = "minecraft:max_health";
	private static final String MOVEMENT_SPEED_ID = "minecraft:movement_speed";
	private static final String ATTACK_DAMAGE_ID = "minecraft:attack_damage";
	private static final String FOLLOW_RANGE_ID = "minecraft:follow_range";
	
	private final ConfigCategory config;
	private ConfigPanel panel;
	
	// 生命值
	private final ConfigValueNumber<Double> healthCurrent;
	private final ConfigValueNumber<Double> healthMax;
	
	// A 类：attributes 数字属性
	private final ConfigValueNumber<Double> movementSpeed;
	private final ConfigValueNumber<Double> attackDamage;
	private final ConfigValueNumber<Double> followRange;
	
	// B 类：布尔开关
	private final ConfigValueDropdown<EntityAttrState> noAi;
	private final ConfigValueDropdown<EntityAttrState> invulnerable;
	private final ConfigValueDropdown<EntityAttrState> noGravity;
	private final ConfigValueDropdown<EntityAttrState> silent;
	private final ConfigValueDropdown<EntityAttrState> canBreatheUnderwater;
	private final ConfigValueDropdown<EntityAttrState> fireImmune;
	
	// C 类：LivingEntity 数值标签
	private final ConfigValueNumber<Double> fallDistance;
	private final ConfigValueNumber<Double> absorptionAmount;
	private final ConfigValueNumber<Integer> air;
	
	// 幼体
	private final ConfigValueDropdown<EntityAttrState> baby;
	
	private final CompoundTag nbt;
	
	public EntityAttributesScreen(NBTReference<L> ref) {
		super(TextInst.translatable("nbteditor.entity_attributes"), ref);
		
		EntityReference entityRef = (EntityReference) ref;
		this.nbt = localNBT.getOrCreateNBT();
		boolean canBeBaby = AgeableMob.class.isAssignableFrom(entityRef.getEntityType().getBaseClass());
		
		this.config = new ConfigCategory();
		
		// ---------- 生命值 ----------
		double curHealth = nbt.getFloat("Health").orElse(20.0f);
		double maxHealth = EntityAttrsUtil.getAttributeBase(nbt, MAX_HEALTH_ID);
		if (maxHealth <= 0)
			maxHealth = curHealth;
		ConfigCategory health = new ConfigCategory(TextInst.translatable("nbteditor.entity_attributes.health"));
		healthCurrent = ConfigValueNumber.forDouble(curHealth, curHealth, 0.0, 1000000000.0);
		healthCurrent.addValueListener(_ -> writeNbt());
		healthMax = ConfigValueNumber.forDouble(maxHealth, maxHealth, 1.0, 1000000000.0);
		healthMax.addValueListener(_ -> writeNbt());
		health.setConfigurable("current", new ConfigItem<>(TextInst.translatable("nbteditor.entity_attributes.health.current"), healthCurrent));
		health.setConfigurable("max", new ConfigItem<>(TextInst.translatable("nbteditor.entity_attributes.health.max"), healthMax));
		config.setConfigurable("health", health);
		
		// ---------- A 类：attributes 数字属性 ----------
		double curSpeed = EntityAttrsUtil.getAttributeBase(nbt, MOVEMENT_SPEED_ID);
		if (curSpeed < 0) curSpeed = 0.25; // 默认移速
		movementSpeed = ConfigValueNumber.forDouble(curSpeed, curSpeed, 0.0, 10000.0);
		movementSpeed.addValueListener(_ -> writeNbt());
		config.setConfigurable("movement_speed", new ConfigItem<>(
				TextInst.translatable("nbteditor.entity_attributes.movement_speed"), movementSpeed));
		
		double curAtk = EntityAttrsUtil.getAttributeBase(nbt, ATTACK_DAMAGE_ID);
		if (curAtk < 0) curAtk = 2.0;
		attackDamage = ConfigValueNumber.forDouble(curAtk, curAtk, 0.0, 100000.0);
		attackDamage.addValueListener(_ -> writeNbt());
		config.setConfigurable("attack_damage", new ConfigItem<>(
				TextInst.translatable("nbteditor.entity_attributes.attack_damage"), attackDamage));
		
		double curFollow = EntityAttrsUtil.getAttributeBase(nbt, FOLLOW_RANGE_ID);
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
		
		// ---------- C 类：LivingEntity 数值标签 ----------
		// FallDistance: -1 = 摔伤免疫（无伤害），>=0 = 正常摔伤缓冲距离
		double curFallDist = nbt.getFloat("FallDistance").orElse(0.0f);
		fallDistance = ConfigValueNumber.forDouble(curFallDist, curFallDist, -1.0, 1000.0);
		fallDistance.addValueListener(_ -> writeNbt());
		config.setConfigurable("fall_distance", new ConfigItem<>(
				TextInst.translatable("nbteditor.entity_attributes.fall_distance"), fallDistance));
		
		// AbsorptionAmount: 伤害吸收值
		double curAbsorb = nbt.getFloat("AbsorptionAmount").orElse(0.0f);
		absorptionAmount = ConfigValueNumber.forDouble(curAbsorb, curAbsorb, 0.0, 1000.0);
		absorptionAmount.addValueListener(_ -> writeNbt());
		config.setConfigurable("absorption_amount", new ConfigItem<>(
				TextInst.translatable("nbteditor.entity_attributes.absorption_amount"), absorptionAmount));
		
		// Air: 水下空气 tick，Dolphin 等 WaterMob 消耗；设 -1 永久无限
		int curAir = nbt.getInt("Air").orElse(300);
		air = ConfigValueNumber.forInt(curAir, curAir, -1, 10000);
		air.addValueListener(_ -> writeNbt());
		config.setConfigurable("air", new ConfigItem<>(
				TextInst.translatable("nbteditor.entity_attributes.air"), air));
		
		// ---------- 幼体 ----------
		if (canBeBaby) {
			boolean currentIsBaby = nbt.getInt("Age").orElse(0) < 0;
			ConfigValueDropdown<EntityAttrState> state = ConfigValueDropdown.forEnum(
					EntityAttrState.KEEP, EntityAttrState.KEEP, EntityAttrState.class);
			state.addValueListener(_ -> writeNbt());
			config.setConfigurable("baby", new ConfigItem<>(
					EntityAttrsUtil.titleWithCurrent("nbteditor.entity_attributes.baby", currentIsBaby), state));
			baby = state;
		} else {
			baby = null;
		}
	}
	
	private ConfigValueDropdown<EntityAttrState> addToggle(String path, String langKey, String nbtKey) {
		boolean currentOn = nbt.get(nbtKey) != null;
		ConfigValueDropdown<EntityAttrState> state = ConfigValueDropdown.forEnum(
				EntityAttrState.KEEP, EntityAttrState.KEEP, EntityAttrState.class);
		state.addValueListener(_ -> writeNbt());
		config.setConfigurable(path, new ConfigItem<>(
				EntityAttrsUtil.titleWithCurrent(langKey, currentOn), state));
		return state;
	}
	
	private void writeNbt() {
		// 生命值
		nbt.putFloat("Health", healthCurrent.getValidValue().floatValue());
		EntityAttrsUtil.setAttributeBase(nbt, MAX_HEALTH_ID, healthMax.getValidValue());
		
		// A 类：attributes 数字属性（直接覆盖，不保留旧值——用户改了就是新值）
		EntityAttrsUtil.setAttributeBase(nbt, MOVEMENT_SPEED_ID, movementSpeed.getValidValue());
		EntityAttrsUtil.setAttributeBase(nbt, ATTACK_DAMAGE_ID, attackDamage.getValidValue());
		EntityAttrsUtil.setAttributeBase(nbt, FOLLOW_RANGE_ID, followRange.getValidValue());
		
		// B 类：布尔
		EntityAttrsUtil.applyBooleanTag(nbt, "NoAI", noAi);
		EntityAttrsUtil.applyBooleanTag(nbt, "Invulnerable", invulnerable);
		EntityAttrsUtil.applyBooleanTag(nbt, "NoGravity", noGravity);
		EntityAttrsUtil.applyBooleanTag(nbt, "Silent", silent);
		EntityAttrsUtil.applyBooleanTag(nbt, "CanBreatheUnderwater", canBreatheUnderwater);
		EntityAttrsUtil.applyBooleanTag(nbt, "FireImmune", fireImmune);
		
		// C 类：LivingEntity 数值标签（总是写——用户看到什么值就写什么值）
		nbt.putFloat("FallDistance", fallDistance.getValidValue().floatValue());
		nbt.putFloat("AbsorptionAmount", absorptionAmount.getValidValue().floatValue());
		nbt.putInt("Air", air.getValidValue());
		
		// 幼体
		if (baby != null) {
			switch (baby.getValidValue()) {
				case KEEP -> {}
				case ON -> nbt.putInt("Age", EntityAttrsUtil.BABY_AGE);
				case OFF -> {
					nbt.putInt("Age", 0);
					nbt.remove("AgeLocked");
				}
			}
		}
		
		checkSave();
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
