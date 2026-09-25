package com.luneruniverse.minecraft.mod.nbteditor.multiversion;

import com.luneruniverse.minecraft.mod.nbteditor.mixin.EditBoxMixin;
import com.luneruniverse.minecraft.mod.nbteditor.screens.Tickable;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.joml.Matrix3x2fStack;

public class MVTextFieldWidget extends EditBox implements Tickable, MVElement {
	
	/**
	 * The selection highlight doesn't move when {@link PoseStack#translate(double, double, double)} is called <br />
	 * Via {@link EditBoxMixin}, the vertex calls are redirected to take this matrix into account
	 * As of 1.19.4, this is fixed
	 */

	protected MVTooltip tooltip;
	
	public MVTextFieldWidget(int x, int y, int width, int height, EditBox copyFrom) {
		super(MainUtil.client.font, x, y, width, height, copyFrom, TextInst.of(""));
	}
	public MVTextFieldWidget(int x, int y, int width, int height) {
		super(MainUtil.client.font, x, y, width, height, TextInst.of(""));
	}
	
	public MVTextFieldWidget tooltip(MVTooltip tooltip) {
		this.tooltip = tooltip;
		setTooltip(tooltip == null ? null : tooltip.toNewTooltip());
		return this;
	}
	
	
	public void extractRenderState(Matrix3x2fStack matrices, int mouseX, int mouseY, float delta) {
		// EditBox 渲染选中时只检查 highlightPos != cursorPos，完全不看 isFocused()
		// (Minecraft 26.3 EditBox.java L447)。失焦后如果用户拖动过鼠标产生选中，
		// highlightPos 就停留在远处，cursorPos 在末尾，结果就是失焦输入框也显示
		// 蓝色选中背景。这里在渲染前强制对齐 highlightPos=cursorPos，确保失焦时
		// EditBox 自己的判断条件 highlightPos != cursorPos 不成立，不画选中。
		if (!isMultiFocused()) {
			setHighlightPos(getCursorPosition());
		}
		super.extractWidgetRenderState(MVDrawableHelper.getDrawContext(matrices),mouseX,mouseY,delta);
	}
	public final void method_25394(Matrix3x2fStack matrices, int mouseX, int mouseY, float delta) {
		extractRenderState(matrices, mouseX, mouseY, delta);
	}
	@Override
	public final void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		extractRenderState(MVDrawableHelper.getMatrices(context), mouseX, mouseY, delta);
	}
	
	public void method_25352(Matrix3x2fStack matrices, int mouseX, int mouseY) { // renderTooltip
		if (tooltip != null)
			tooltip.render(matrices, mouseX, mouseY);
	}
	
	@Override
	@Deprecated
	public void setFocused(boolean focused) {
		// Update multi-focus state first so that isFocused() returns the correct
		// value when IMBlocker's TextFieldMixin calls canConsumeInput() (which
		// checks isFocused()) at the TAIL of super.setFocused().
		setMultiFocused(focused);
		// Call super.setFocused() to trigger both vanilla IME handling and
		// IMBlocker's focus management mixin (@Inject on EditBox.setFocused TAIL).
		// Without this, IMBlocker's mixin never fires because this override
		// bypasses EditBox.setFocused entirely.
		super.setFocused(focused);
	}
	@Override
	@Deprecated
	public boolean isFocused() {
		// Invoke super.isFocused() purely for SIDE EFFECTS.
		// IMBlocker's AbstractWidgetMixin injects into AbstractWidget.isFocused()
		// at TAIL to update lastRenderTime when FocusManager.isGameRendering is true.
		// Without this call, lastRenderTime stays 0 forever, isRenderable becomes false,
		// locateRealFocus() strips our focus, and IME gets locked to English.
		// The return value from super is ignored; we use our own multi-focus state.
		super.isFocused();
		return isMultiFocused();
	}

	@Override
	public void onMultiFocusedSet(boolean focused, boolean prevFocused) {
		// 失焦时清选中。虽然 render 守卫也会处理，但这里先对齐一次，
		// 避免中间状态（失焦后 render 前那一帧）还残留 highlightPos != cursorPos。
		if (!focused) {
			setHighlightPos(getCursorPosition());
		}
	}

	@Override
	public boolean charTyped(final CharacterEvent event) {
		// 26.3 的 EditBox.charTyped 用 event.isAllowedChatCharacter() 过滤字符，
		// 这会把中文等非聊天字符挡掉。NBTEditor 需要支持 Unicode 输入（物品显示名、
		// Sign、Book 等），所以绕过这个过滤——直接允许所有可打印字符。
		if (!this.canConsumeInput())
			return false;
		this.insertText(event.codepointAsString());
		return true;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		boolean result = super.mouseClicked(event, doubleClick);
		var mc = MainUtil.client;
		if (result && mc.gui != null && mc.gui.screen() instanceof AbstractContainerEventHandler root) {
			AbstractContainerEventHandler target = findDirectParent(root, this);
			if (target != null)
				target.setFocused(this);
		}
		return result;
	}

	/** 从 root 开始向下找直接包含 target 的 container（最近一层） */
	private static AbstractContainerEventHandler findDirectParent(AbstractContainerEventHandler root, GuiEventListener target) {
		AbstractContainerEventHandler deepest = null;
		if (root.children().contains(target))
			deepest = root;
		for (GuiEventListener child : root.children()) {
			if (child instanceof AbstractContainerEventHandler inner) {
				AbstractContainerEventHandler found = findDirectParent(inner, target);
				if (found != null)
					deepest = found;
			}
		}
		return deepest;
	}

	@Override
	public void tick() {

	}
}
