package com.luneruniverse.minecraft.mod.nbteditor.screens.widgets;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVDrawableHelper;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVElement;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVMisc;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.TextInst;
import com.luneruniverse.minecraft.mod.nbteditor.screens.OverlayScreen;
import com.luneruniverse.minecraft.mod.nbteditor.screens.OverlaySupportingScreen;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class SelectionOverlay<T> extends GroupWidget implements InitializableOverlay<OverlaySupportingScreen> {
	
	public static <T> void show(Component title, List<T> options, Consumer<T> onSelect) {
		show(title, options, value -> false, Object::toString, onSelect);
	}
	public static <T> void show(Component title, List<T> options, Predicate<T> importantValues, Consumer<T> onSelect) {
		show(title, options, importantValues, Object::toString, onSelect);
	}
	public static <T> void show(Component title, List<T> options, Predicate<T> importantValues, Function<T, String> display, Consumer<T> onSelect) {
		OverlayScreen.setOverlayOrScreen(
				new SelectionOverlay<>(title, options, importantValues, display, onSelect, () -> OverlaySupportingScreen.setOverlayStatic(null)), true);
	}
	
	private class EntryElement extends List2D.List2DValue {
		private final T option;
		public EntryElement(T option) {
			this.option = option;
		}
		@Override
		public void extractRenderState(Matrix3x2fStack matrices, int mouseX, int mouseY, float delta) {
			boolean hovered = isHovering(mouseX, mouseY);
			boolean selected = SelectionOverlay.this.selected == option;
			boolean important = SelectionOverlay.this.importantValues.test(option);
			int x = 0;
			int y = 0;
			int w = SelectionOverlay.this.list.getItemWidth();
			int h = SelectionOverlay.this.list.getItemHeight();
			int border = selected ? 0xFFFFAA00 : (hovered ? 0xFFFFFFFF : (important ? 0xFFFF55FF : 0xFF777777));
			int inner = selected ? (hovered ? 0xFF8A6A1A : 0xFF7A5A00) : (hovered ? 0xFF666666 : 0xFF333333);
			MVDrawableHelper.fill(matrices, x, y, x + w, y + h, border);
			MVDrawableHelper.fill(matrices, x + 1, y + 1, x + w - 1, y + h - 1, inner);
			MVDrawableHelper.drawTextWithShadow(matrices, MainUtil.client.font,
					Component.literal(SelectionOverlay.this.display.apply(option)),
					x + 3, y + (h - MainUtil.client.font.lineHeight) / 2, important ? 0xFFFF55FF : 0xFFFFFFFF);
		}
		@Override
		public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
			if (isHovering(click.x(), click.y())) {
				SelectionOverlay.this.selected = option;
				confirmBtn.active = true;
				return true;
			}
			return false;
		}
		private boolean isHovering(double mouseX, double mouseY) {
			return isInsideList() && mouseX >= 0 && mouseY >= 0
					&& mouseX <= SelectionOverlay.this.list.getItemWidth()
					&& mouseY <= SelectionOverlay.this.list.getItemHeight();
		}
	}
	
	private final Component title;
	private final Consumer<T> onSelect;
	private final Runnable close;
	private final Predicate<T> importantValues;
	private final Function<T, String> display;
	private final List<T> allOptions;
	private final List<T> filtered = new ArrayList<>();
	private EditBox searchBox;
	private List2D list;
	private Button confirmBtn;
	private T selected;
	private int x;
	private int y;
	private int width;
	private int height;
	
	private SelectionOverlay(Component title, List<T> options, Predicate<T> importantValues, Function<T, String> display, Consumer<T> onSelect, Runnable close) {
		this.title = title;
		this.allOptions = options;
		this.importantValues = importantValues;
		this.display = display;
		this.onSelect = onSelect;
		this.close = close;
	}
	
	@Override
	public void init(OverlaySupportingScreen parent, int screenWidth, int screenHeight) {
		clearWidgets();
		
		filtered.clear();
		filtered.addAll(allOptions);
		
		this.width = Math.min(320, screenWidth - 40);
		this.height = Math.min(420, screenHeight - 80);
		this.x = (screenWidth - width) / 2;
		this.y = (screenHeight - height) / 2;
		
		searchBox = new EditBox(MainUtil.client.font, x + 4, y + 4, width - 8, 20, TextInst.translatable("nbteditor.item_search"));
		searchBox.setMaxLength(128);
		searchBox.setResponder(this::filter);
		addWidget(searchBox);
		
		int listY = y + 4 + 20 + 4;
		int listHeight = height - (4 + 20 + 4) - 24 - 4;
		list = new List2D(x + 4, listY, width - 8, listHeight, 0, width - 8, 16, 2)
				.setFinalEventHandler(new MVElement() {});
		rebuild();
		addWidget(list);
		
		int gap = 4;
		int btnWidth = (width - 8 - gap) / 2;
		int btnX = x + 4;
		addWidget(MVMisc.newButton(btnX, y + height - 20, btnWidth, 20,
				TextInst.translatable("nbteditor.cancel"), _ -> close.run()));
		btnX += btnWidth + gap;
		confirmBtn = MVMisc.newButton(btnX, y + height - 20, btnWidth, 20,
				TextInst.translatable("nbteditor.confirm"), _ -> {
					if (selected != null) {
						close.run();
						onSelect.accept(selected);
					}
				});
		confirmBtn.active = false;
		addWidget(confirmBtn);
		
		searchBox.setFocused(true);
	}
	
	private void filter(String query) {
		query = query.toLowerCase().trim();
		filtered.clear();
		if (query.isEmpty()) {
			filtered.addAll(allOptions);
		} else {
			for (T option : allOptions) {
			if (display.apply(option).toLowerCase().contains(query))
				filtered.add(option);
		}
		}
		rebuild();
	}
	
	private void rebuild() {
		list.clearElements();
		for (T option : filtered)
			list.addElement(new EntryElement(option));
		list.setScroll(0);
	}
	
	@Override
	public void extractRenderState(Matrix3x2fStack matrices, int mouseX, int mouseY, float delta) {
		matrices.pushMatrix();
		matrices.translate(0.0f, 0.0f);
		MainUtil.client.gui.screen().extractBackground(MVDrawableHelper.getDrawContext(matrices), mouseX, mouseY, delta);
		MVDrawableHelper.drawCenteredTextWithShadow(matrices, MainUtil.client.font, title,
				x + width / 2, y - 4 - MainUtil.client.font.lineHeight, -1);
		super.extractRenderState(matrices, mouseX, mouseY, delta);
		MainUtil.renderLogo(matrices);
		matrices.popMatrix();
	}
	
	@Override
	public boolean keyPressed(KeyEvent keyInput) {
		if (keyInput.key() == InputConstants.KEY_ESCAPE) {
			close.run();
			return true;
		}
		if (searchBox.keyPressed(keyInput) || searchBox.canConsumeInput())
			return true;
		return super.keyPressed(keyInput);
	}
	
	@Override
	public boolean charTyped(CharacterEvent chr) {
		if (searchBox.charTyped(chr))
			return true;
		return super.charTyped(chr);
	}
	
}