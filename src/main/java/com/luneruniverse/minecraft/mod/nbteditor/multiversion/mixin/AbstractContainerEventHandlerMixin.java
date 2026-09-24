package com.luneruniverse.minecraft.mod.nbteditor.multiversion.mixin;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVElement;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 当 AbstractContainerEventHandler.setFocused(child) 被调用时，
 * 同步所有 MVElement children 的 setMultiFocused 状态。
 * 
 * 这保证了：
 * - Screen/GroupWidget/Panel 等容器的 focused child 变化时，
 *   MVElement.setMultiFocused 会被正确调用
 * - MultiLineTextFieldWidget 的 IMBlocker 注册/注销能够跟随焦点变化
 * - 输入框视觉上的选中状态能够正确显示
 * 
 * 只处理 MVElement.setMultiFocused，不修改 EditBox.setFocused
 * （EditBox 的 IME 由 IMBlocker 自己的 TextFieldMixin 处理）。
 */
@Mixin(AbstractContainerEventHandler.class)
public class AbstractContainerEventHandlerMixin {
	@Inject(method = "setFocused", at = @At("RETURN"))
	private void setFocused(GuiEventListener element, CallbackInfo info) {
		for (GuiEventListener child : ((AbstractContainerEventHandler) (Object) this).children()) {
			if (child instanceof MVElement)
				((MVElement) child).setMultiFocused(child == element);
		}
	}
}
