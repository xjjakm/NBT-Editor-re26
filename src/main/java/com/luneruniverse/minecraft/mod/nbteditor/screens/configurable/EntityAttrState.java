package com.luneruniverse.minecraft.mod.nbteditor.screens.configurable;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.TextInst;
import net.minecraft.network.chat.Component;

/**
 * 实体属性 / 刷怪蛋实体数据编辑器共用的三态枚举：保持 NBT 原值 / 开 / 关。
 */
public enum EntityAttrState {
	KEEP("nbteditor.entity_attributes.state.keep"),
	ON("nbteditor.entity_attributes.state.on"),
	OFF("nbteditor.entity_attributes.state.off");
	
	private final Component name;
	EntityAttrState(String key) {
		this.name = TextInst.translatable(key);
	}
	@Override
	public String toString() {
		return name.getString();
	}
}
