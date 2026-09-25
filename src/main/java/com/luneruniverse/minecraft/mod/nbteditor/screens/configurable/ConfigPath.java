package com.luneruniverse.minecraft.mod.nbteditor.screens.configurable;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVElement;
import com.luneruniverse.minecraft.mod.nbteditor.screens.Tickable;

public interface ConfigPath extends Configurable<ConfigPath>, Tickable {
	public ConfigPath addValueListener(ConfigValueListener<ConfigValue<?, ?>> listener);
	
	/** 递归清除自身及所有子元素的 multiFocused 状态 */
	default void clearFocusRecursive() {
		if (this instanceof MVElement mv)
			mv.setMultiFocused(false);
	}
}
