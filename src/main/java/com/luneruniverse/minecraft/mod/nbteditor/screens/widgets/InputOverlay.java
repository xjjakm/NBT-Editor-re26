package com.luneruniverse.minecraft.mod.nbteditor.screens.widgets;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.*;
import com.luneruniverse.minecraft.mod.nbteditor.screens.OverlayScreen;
import com.luneruniverse.minecraft.mod.nbteditor.screens.OverlaySupportingScreen;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;

import java.util.function.Consumer;

public class InputOverlay<T> extends GroupWidget implements InitializableOverlay<Screen> {
	
	public static interface Input<T> extends MVDrawable, MVElement {
		public void init(int x, int y);
		public int getWidth();
		public int getHeight();
		public T getValue();
		public boolean isValid();
	}
	
	public static <T> void show(Component title, Input<T> input, Consumer<T> valueConsumer) {
		OverlayScreen.setOverlayOrScreen(
				new InputOverlay<>(title, input, valueConsumer, () -> OverlaySupportingScreen.setOverlayStatic(null)), true);
	}
	
	private final Component title;
	private final Input<T> input;
	private final Consumer<T> valueConsumer;
	private final Runnable close;
	private int x;
	private int y;
	private Button ok;
	
	public InputOverlay(Component title, Input<T> input, Consumer<T> valueConsumer, Runnable close) {
		this.title = title;
		this.input = input;
		this.valueConsumer = valueConsumer;
		this.close = close;
	}
	
	@Override
	public void init(Screen parent, int width, int height) {
		clearWidgets();
		
		x = (width - input.getWidth()) / 2;
		y = (height - input.getHeight() - 24) / 2;
		input.init(x, y);

		
		ok = addWidget(MVMisc.newButton(x, y + input.getHeight() + 4,
				(input.getWidth() - 4) / 2, 20, TextInst.translatable("nbteditor.ok"), _ -> {
			close.run();
			valueConsumer.accept(input.getValue());
		}));
		addWidget(MVMisc.newButton(width / 2 + 2, y + input.getHeight() + 4,
				(input.getWidth() - 4) / 2, 20, TextInst.translatable("nbteditor.cancel"), _ -> close.run()));
		
		ok.active = input.isValid();

		addWidget(input);
		setFocused(input);
	}
	
	@Override
	public void extractRenderState(Matrix3x2fStack matrices, int mouseX, int mouseY, float delta) {
		ok.active = input.isValid();
		
		matrices.pushMatrix();
		matrices.translate(0.0f, 0.0f);
		MainUtil.client.gui.screen().extractBackground(MVDrawableHelper.getDrawContext(matrices), mouseX, mouseY, delta);
		if (title != null) {
			MVDrawableHelper.drawCenteredTextWithShadow(matrices, MainUtil.client.font, title,
					x + input.getWidth() / 2, y - 4 - MainUtil.client.font.lineHeight, -1);
		}
		super.extractRenderState(matrices, mouseX, mouseY, delta);
		MainUtil.renderLogo(matrices);
		matrices.popMatrix();
	}
	
	@Override
	public boolean keyPressed(KeyEvent keyInput) {
		int keyCode = keyInput.key();
		if (keyCode == InputConstants.KEY_ESCAPE) {
			close.run();
			return true;
		}
		if (keyCode == InputConstants.KEY_RETURN && ok.active) {
			close.run();
			valueConsumer.accept(input.getValue());
			return true;
		}
		
		return super.keyPressed(keyInput);
	}
	
}
