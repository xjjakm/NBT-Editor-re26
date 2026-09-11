package com.luneruniverse.minecraft.mod.nbteditor.screens.factories;

import com.luneruniverse.minecraft.mod.nbteditor.localnbt.LocalItem;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.TextInst;
import com.luneruniverse.minecraft.mod.nbteditor.nbtreferences.NBTReference;
import com.luneruniverse.minecraft.mod.nbteditor.nbtreferences.itemreferences.ItemReference;
import com.luneruniverse.minecraft.mod.nbteditor.screens.NBTEditorScreen;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 交易物品的 NBT 编辑界面，保存按钮文案为「保存并返回」，
 * 保存后自动回到上一界面（村民编辑器）。
 */
public class ItemNBTScreen extends NBTEditorScreen<LocalItem> {
	
	private final Screen returnScreen;
	
	public ItemNBTScreen(NBTReference<LocalItem> ref, Screen returnScreen) {
		super(ref);
		this.returnScreen = returnScreen;
	}
	
	@Override
	protected Component getSaveBtnText() {
		return TextInst.translatable("nbteditor.save_and_return");
	}

	@Override
	protected boolean save() {
		boolean output = super.save();
		MainUtil.client.gui.setScreen(returnScreen);
		return output;
	}
	
	// 从高级编辑器（factory）进入的子编辑器也要保存后返回村民编辑器
	@Override
	protected FactoryLink<LocalItem> getFactoryLink() {
		FactoryLink<LocalItem> link = super.getFactoryLink();
		return new FactoryLink<>(link.langName(), ref -> {
			Screen screen = link.factory().apply(ItemReference.toItemStackRef(ref));
			if (screen instanceof LocalFactoryScreen<?> factoryScreen)
				factoryScreen.setReturnScreen(returnScreen);
			return screen;
		});
	}
	
}