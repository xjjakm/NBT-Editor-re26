package com.luneruniverse.minecraft.mod.nbteditor.screens.factories;

import com.luneruniverse.minecraft.mod.nbteditor.localnbt.LocalNBT;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.TextInst;
import com.luneruniverse.minecraft.mod.nbteditor.nbtreferences.EntityReference;
import com.luneruniverse.minecraft.mod.nbteditor.nbtreferences.NBTReference;
import com.luneruniverse.minecraft.mod.nbteditor.screens.LocalEditorScreen;
import com.luneruniverse.minecraft.mod.nbteditor.screens.configurable.ConfigCategory;
import com.luneruniverse.minecraft.mod.nbteditor.screens.configurable.ConfigItem;
import com.luneruniverse.minecraft.mod.nbteditor.screens.configurable.ConfigPanel;
import com.luneruniverse.minecraft.mod.nbteditor.screens.configurable.ConfigValueDropdown;
import com.luneruniverse.minecraft.mod.nbteditor.screens.configurable.ConfigValueNumber;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.AgeableMob;
import org.joml.Matrix3x2fStack;

public class EntityAttributesScreen<L extends LocalNBT> extends LocalEditorScreen<L> {
	
	/**
	 * 三态：保持（不改动该 NBT 键）/ 开 / 关
	 */
	public enum EntityAttrState {
		KEEP("nbteditor.entity_attributes.state.keep"),
		ON("nbteditor.entity_attributes.state.on"),
		OFF("nbteditor.entity_attributes.state.off");
		
		private final Component name;
		private EntityAttrState(String key) {
			this.name = TextInst.translatable(key);
		}
		@Override
		public String toString() {
			return name.getString();
		}
	}
	
	private static final String MAX_HEALTH_ID = "minecraft:max_health";
	/** 幼体过渡期开始的 Age 值（Age < 0 即幼年，参见 AgeableMob.getBabyStartAge） */
	private static final int BABY_AGE = -24000;
	
	private final ConfigCategory config;
	private ConfigPanel panel;
	
	private final ConfigValueNumber<Double> healthCurrent;
	private final ConfigValueNumber<Double> healthMax;
	private final ConfigValueDropdown<EntityAttrState> noAi;
	private final ConfigValueDropdown<EntityAttrState> invulnerable;
	private final ConfigValueDropdown<EntityAttrState> noGravity;
	private final ConfigValueDropdown<EntityAttrState> silent;
	private final ConfigValueDropdown<EntityAttrState> baby;
	
	private final CompoundTag nbt;
	
	public EntityAttributesScreen(NBTReference<L> ref) {
		super(TextInst.translatable("nbteditor.entity_attributes"), ref);
		
		EntityReference entityRef = (EntityReference) ref;
		this.nbt = localNBT.getOrCreateNBT();
		boolean canBeBaby = AgeableMob.class.isAssignableFrom(entityRef.getEntityType().getBaseClass());
		
		this.config = new ConfigCategory();
		
		// ---------- 生命值：当前 / 最大 ----------
		double curHealth = nbt.getFloat("Health").orElse(20.0f);
		double maxHealth = getMaxHealthFromNbt(nbt);
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
		
		// ---------- 基础开关 ----------
		noAi = addToggle("no_ai", "nbteditor.entity_attributes.no_ai", nbt.get("NoAI") != null);
		invulnerable = addToggle("invulnerable", "nbteditor.entity_attributes.invulnerable", nbt.get("Invulnerable") != null);
		noGravity = addToggle("no_gravity", "nbteditor.entity_attributes.no_gravity", nbt.get("NoGravity") != null);
		silent = addToggle("silent", "nbteditor.entity_attributes.silent", nbt.get("Silent") != null);
		
		// ---------- 幼体（仅 AgeableMob 可成长生物） ----------
		if (canBeBaby) {
			baby = addToggle("baby", "nbteditor.entity_attributes.baby", nbt.getInt("Age").orElse(0) < 0);
		} else {
			baby = null;
		}
	}
	
	private ConfigValueDropdown<EntityAttrState> addToggle(String path, String langKey, boolean currentOn) {
		ConfigValueDropdown<EntityAttrState> state = ConfigValueDropdown.forEnum(EntityAttrState.KEEP, EntityAttrState.KEEP, EntityAttrState.class);
		state.addValueListener(_ -> writeNbt());
		config.setConfigurable(path, new ConfigItem<>(titleWithCurrent(langKey, currentOn), state));
		return state;
	}
	
	private static Component titleWithCurrent(String langKey, boolean current) {
		return TextInst.translatable(langKey).copy().append(
				TextInst.translatable(current
						? "nbteditor.entity_attributes.current_on"
						: "nbteditor.entity_attributes.current_off"));
	}
	
	/**
	 * 从 26.2 的 codec 结构 attributes 列表中读取 max_health 的 base；找不到返回 -1
	 */
	private static double getMaxHealthFromNbt(CompoundTag nbt) {
		ListTag attributes = nbt.getListOrEmpty("attributes");
		for (int i = 0; i < attributes.size(); i++) {
			Tag element = attributes.get(i);
			if (element instanceof CompoundTag compound && compound.getString("id").filter(MAX_HEALTH_ID::equals).isPresent())
				return compound.getDouble("base").orElse(0.0);
		}
		return -1.0;
	}
	
	/**
	 * 将当前 UI 状态写回实体 NBT（与服务端 entity.load 的读取顺序一致：先 attributes 后 Health）
	 */
	private void writeNbt() {
		nbt.putFloat("Health", healthCurrent.getValidValue().floatValue());
		setMaxHealthInNbt(nbt, healthMax.getValidValue());
		
		applyBooleanTag("NoAI", noAi);
		applyBooleanTag("Invulnerable", invulnerable);
		applyBooleanTag("NoGravity", noGravity);
		applyBooleanTag("Silent", silent);
		
		if (baby != null) {
			switch (baby.getValidValue()) {
				case KEEP -> {}
				case ON -> nbt.putInt("Age", BABY_AGE);
				case OFF -> {
					nbt.putInt("Age", 0);
					nbt.remove("AgeLocked");
				}
			}
		}
		
		checkSave();
	}
	
	private void setMaxHealthInNbt(CompoundTag nbt, double value) {
		ListTag attributes = nbt.getListOrEmpty("attributes");
		for (int i = 0; i < attributes.size(); i++) {
			Tag element = attributes.get(i);
			if (element instanceof CompoundTag compound && compound.getString("id").filter(MAX_HEALTH_ID::equals).isPresent()) {
				compound.putDouble("base", value);
				nbt.put("attributes", attributes);
				return;
			}
		}
		// 目标属性不存在时新建 codec 结构条目 {id, base, modifiers: []}
		CompoundTag entry = new CompoundTag();
		entry.putString("id", MAX_HEALTH_ID);
		entry.putDouble("base", value);
		entry.put("modifiers", new ListTag());
		attributes.add(entry);
		nbt.put("attributes", attributes);
	}
	
	private void applyBooleanTag(String key, ConfigValueDropdown<EntityAttrState> state) {
		switch (state.getValidValue()) {
			case KEEP -> {}
			case ON -> nbt.putBoolean(key, true);
			case OFF -> nbt.remove(key);
		}
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