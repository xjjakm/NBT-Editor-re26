package com.luneruniverse.minecraft.mod.nbteditor.multiversion.mixin;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVElement;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * AbstractContainerEventHandler.setFocused(child) 只设置自身 focused 字段，
 * 不再向下传播 child.setFocused()。导致：
 * 1. MVTextFieldWidget (原生 EditBox) 的 IME 永远不会被激活，
 *    因为 IMBlocker 依赖 EditBox.setFocused() mixin 来管理 ImmAssociateContext。
 * 2. MVElement.setMultiFocused 没有触发，依赖它的 IMBlocker 组件锁输入法。
 * 
 * 本 mixin 在 setFocused RETURN 时：
 * - 对所有 MVElement 同步 setMultiFocused（供 MultiLineTextFieldWidget 等使用）
 * - 对所有 EditBox 调 setFocused()（供 IMBlocker 的原生 EditBox 处理使用）
 */
@Mixin(AbstractContainerEventHandler.class)
public class AbstractContainerEventHandlerMixin {
	@Inject(method = "setFocused", at = @At("RETURN"))
	private void setFocused(GuiEventListener element, CallbackInfo info) {
		var container = (AbstractContainerEventHandler) (Object) this;
		for (GuiEventListener child : container.children()) {
			boolean wantFocus = child == element;
			if (child instanceof MVElement)
				((MVElement) child).setMultiFocused(wantFocus);
			// EditBox (including MVTextFieldWidget) 依赖 setFocused() 触发 IMBlocker mixin
			if (child instanceof EditBox editBox)
				editBox.setFocused(wantFocus);
		}
	}
}
