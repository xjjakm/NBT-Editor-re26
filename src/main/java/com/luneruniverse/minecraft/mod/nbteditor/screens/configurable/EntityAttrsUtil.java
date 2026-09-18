package com.luneruniverse.minecraft.mod.nbteditor.screens.configurable;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.TextInst;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

/**
 * 实体属性 / 刷怪蛋实体数据编辑器共用的纯函数工具。
 * 负责读写 LivingEntity / Mob 的 attributes 列表、布尔标签、以及生成带当前状态的 UI 标题。
 */
public final class EntityAttrsUtil {
	
	/** AgeableMob 幼体 Age 值：Age < BABY_AGE 即幼年 */
	public static final int BABY_AGE = -24000;
	
	private EntityAttrsUtil() {}
	
	// ---------- attributes 列表（codec 结构：{id, base, modifiers: []}） ----------
	
	/** 从 attributes 列表里读取指定属性的 base；找不到返回 -1 */
	public static double getAttributeBase(CompoundTag nbt, String attributeId) {
		ListTag attributes = nbt.getListOrEmpty("attributes");
		for (int i = 0; i < attributes.size(); i++) {
			Tag element = attributes.get(i);
			if (element instanceof CompoundTag compound
					&& compound.getString("id").filter(attributeId::equals).isPresent())
				return compound.getDouble("base").orElse(0.0);
		}
		return -1;
	}
	
	/** 写 base 到 attributes 列表；条目不存在时新建 {id, base, modifiers: []} */
	public static void setAttributeBase(CompoundTag nbt, String attributeId, double value) {
		ListTag attributes = nbt.getListOrEmpty("attributes");
		for (int i = 0; i < attributes.size(); i++) {
			Tag element = attributes.get(i);
			if (element instanceof CompoundTag compound
					&& compound.getString("id").filter(attributeId::equals).isPresent()) {
				compound.putDouble("base", value);
				nbt.put("attributes", attributes);
				return;
			}
		}
		CompoundTag entry = new CompoundTag();
		entry.putString("id", attributeId);
		entry.putDouble("base", value);
		entry.put("modifiers", new ListTag());
		attributes.add(entry);
		nbt.put("attributes", attributes);
	}
	
	// ---------- 布尔标签 ----------
	
	/** 把 ConfigValueDropdown<EntityAttrState> 的值应用到 NBT。dropdown 为 null 时跳过 */
	public static void applyBooleanTag(CompoundTag nbt, String key, ConfigValueDropdown<EntityAttrState> dropdown) {
		if (dropdown == null)
			return;
		switch (dropdown.getValidValue()) {
			case KEEP -> {}
			case ON -> nbt.putBoolean(key, true);
			case OFF -> nbt.remove(key);
		}
	}
	
	// ---------- UI 辅助 ----------
	
	public static Component titleWithCurrent(String langKey, boolean current) {
		return TextInst.translatable(langKey).copy().append(
				TextInst.translatable(current
						? "nbteditor.entity_attributes.current_on"
						: "nbteditor.entity_attributes.current_off"));
	}
	
}
