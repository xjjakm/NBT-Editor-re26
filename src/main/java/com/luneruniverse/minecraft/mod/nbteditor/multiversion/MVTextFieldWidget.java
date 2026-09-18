package com.luneruniverse.minecraft.mod.nbteditor.multiversion;

import com.luneruniverse.minecraft.mod.nbteditor.mixin.EditBoxMixin;
import com.luneruniverse.minecraft.mod.nbteditor.screens.Tickable;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
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
		// IME activation is handled by super.setFocused() called from setFocused().
		// - When IMBlocker is absent: super.setFocused() -> EditBox.setFocused()
		//   -> Minecraft.onTextInputFocusChange() activates OS-level IME.
		// - When IMBlocker is present: it cancels vanilla onTextInputFocusChange
		//   via TextInputManagerMixin, but its own @Inject on EditBox.setFocused
		//   TAIL fires and manages IME via ImmAssociateContext.
		// No additional IME activation needed here.
	}

	@Override
	public void tick() {

	}
}
