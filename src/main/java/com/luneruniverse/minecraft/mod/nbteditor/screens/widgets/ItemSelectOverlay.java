package com.luneruniverse.minecraft.mod.nbteditor.screens.widgets;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.*;
import com.luneruniverse.minecraft.mod.nbteditor.screens.OverlayScreen;
import com.luneruniverse.minecraft.mod.nbteditor.screens.OverlaySupportingScreen;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

public class ItemSelectOverlay extends GroupWidget implements InitializableOverlay<OverlaySupportingScreen> {
	
	public record ItemEntry(Item item, String displayName, String itemId) {
		public ItemStack toStack() {
			return new ItemStack(item);
		}
	}
	
	public static void show(Consumer<ItemEntry> onSelect) {
		show(onSelect, false);
	}
	public static void show(Consumer<ItemEntry> onSelect, boolean allowClear) {
		OverlayScreen.setOverlayOrScreen(
				new ItemSelectOverlay(onSelect, allowClear, () -> OverlaySupportingScreen.setOverlayStatic(null)), true);
	}
	
	private class EntryElement extends List2D.List2DValue {
		private final ItemEntry entry;
		public EntryElement(ItemEntry entry) {
			this.entry = entry;
		}
		@Override
		public void extractRenderState(Matrix3x2fStack matrices, int mouseX, int mouseY, float delta) {
			boolean hovered = isHovering(mouseX, mouseY);
			boolean selected = ItemSelectOverlay.this.selectedEntry == entry;
			int x = 0;
			int y = 0;
			int w = ItemSelectOverlay.this.itemsList.getItemWidth();
			int h = ItemSelectOverlay.this.itemsList.getItemHeight();
			// 边框：选中金色加亮，悬浮白色，正常灰色
			int border = selected ? 0xFFFFAA00 : (hovered ? 0xFFFFFFFF : 0xFF777777);
			int inner = selected ? (hovered ? 0xFF8A6A1A : 0xFF7A5A00) : (hovered ? 0xFF666666 : 0xFF333333);
			MVDrawableHelper.fill(matrices, x, y, x + w, y + h, border);
			MVDrawableHelper.fill(matrices, x + 1, y + 1, x + w - 1, y + h - 1, inner);
			MVDrawableHelper.drawTextWithShadow(matrices, MainUtil.client.font,
					Component.literal(entry.displayName() + " (" + entry.itemId() + ")"),
					x + 3, y + (h - MainUtil.client.font.lineHeight) / 2, 0xFFFFFFFF);
		}
		@Override
		public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
			if (isHovering(click.x(), click.y())) {
				ItemSelectOverlay.this.selectedEntry = entry;
				confirmBtn.active = true;
				return true;
			}
			return false;
		}
		private boolean isHovering(double mouseX, double mouseY) {
			return isInsideList() && mouseX >= 0 && mouseY >= 0
					&& mouseX <= ItemSelectOverlay.this.itemsList.getItemWidth()
					&& mouseY <= ItemSelectOverlay.this.itemsList.getItemHeight();
		}
	}
	
	private final Consumer<ItemEntry> onSelect;
	private final boolean allowClear;
	private final Runnable close;
	private EditBox searchBox;
	private List2D itemsList;
	private Button confirmBtn;
	private ItemEntry selectedEntry;
	private final List<ItemEntry> allItems = new ArrayList<>();
	private final List<ItemEntry> filteredItems = new ArrayList<>();
	private int x;
	private int y;
	private int width;
	private int height;
	
	public ItemSelectOverlay(Consumer<ItemEntry> onSelect, boolean allowClear, Runnable close) {
		this.onSelect = onSelect;
		this.allowClear = allowClear;
		this.close = close;
	}
	
	@Override
	public void init(OverlaySupportingScreen parent, int screenWidth, int screenHeight) {
		clearWidgets();
		
		// 预缓存所有物品（按翻译名排序）
		if (allItems.isEmpty()) {
			for (Holder<Item> holder : BuiltInRegistries.ITEM.asHolderIdMap()) {
				Item item = holder.value();
				Identifier id = holder.unwrapKey().map(ResourceKey::identifier)
						.orElseGet(() -> BuiltInRegistries.ITEM.getKey(item));
				if (id == null)
					continue;
				allItems.add(new ItemEntry(item, item.getName(new ItemStack(item)).getString(), id.toString()));
			}
			allItems.sort(Comparator.comparing(entry -> entry.displayName().toLowerCase()));
			filteredItems.addAll(allItems);
		}
		
		this.width = Math.min(320, screenWidth - 40);
		this.height = Math.min(420, screenHeight - 80);
		this.x = (screenWidth - width) / 2;
		this.y = (screenHeight - height) / 2;
		
		// 搜索框（按翻译名或 id 搜索）
		searchBox = new EditBox(MainUtil.client.font, x + 4, y + 4, width - 8, 20, TextInst.translatable("nbteditor.item_search"));
		searchBox.setMaxLength(128);
		searchBox.setResponder(this::filterItems);
		addWidget(searchBox);
		
		// 物品列表
		int listY = y + 4 + 20 + 4;
		int listHeight = height - (4 + 20 + 4) - 24 - 4;
		itemsList = new List2D(x + 4, listY, width - 8, listHeight, 0, width - 8, 16, 2)
				.setFinalEventHandler(new MVElement() {});
		rebuildItems();
		addWidget(itemsList);
		
		// 确认/取消（可选：清空）按钮，从左到右等分排列
		int btnX = x + 4;
		int gap = 4;
		int btnCount = allowClear ? 3 : 2;
		int btnWidth = (width - 8 - gap * (btnCount - 1)) / btnCount;
		if (allowClear) {
			addWidget(MVMisc.newButton(btnX, y + height - 20, btnWidth, 20,
					TextInst.translatable("nbteditor.clear"), _ -> {
						close.run();
						onSelect.accept(null);
					}));
			btnX += btnWidth + gap;
		}
		addWidget(MVMisc.newButton(btnX, y + height - 20, btnWidth, 20,
				TextInst.translatable("nbteditor.cancel"), _ -> close.run()));
		btnX += btnWidth + gap;
		confirmBtn = MVMisc.newButton(btnX, y + height - 20, btnWidth, 20,
				TextInst.translatable("nbteditor.confirm"), _ -> {
					if (selectedEntry != null) {
						close.run();
						onSelect.accept(selectedEntry);
					}
				});
		confirmBtn.active = false;
		addWidget(confirmBtn);
		
		searchBox.setFocused(true);
	}
	
	private void filterItems(String query) {
		query = query.toLowerCase().trim();
		filteredItems.clear();
		if (query.isEmpty()) {
			filteredItems.addAll(allItems);
		} else {
			for (ItemEntry entry : allItems) {
				if (entry.displayName().toLowerCase().contains(query) || entry.itemId().contains(query))
					filteredItems.add(entry);
			}
		}
		rebuildItems();
	}
	
	private void rebuildItems() {
		itemsList.clearElements();
		for (ItemEntry entry : filteredItems)
			itemsList.addElement(new EntryElement(entry));
		itemsList.setScroll(0);
	}
	
	@Override
	public void extractRenderState(Matrix3x2fStack matrices, int mouseX, int mouseY, float delta) {
		matrices.pushMatrix();
		matrices.translate(0.0f, 0.0f);
		MainUtil.client.gui.screen().extractBackground(MVDrawableHelper.getDrawContext(matrices), mouseX, mouseY, delta);
		MVDrawableHelper.drawCenteredTextWithShadow(matrices, MainUtil.client.font,
				TextInst.translatable("nbteditor.select_item"),
				x + width / 2, y - 4 - MainUtil.client.font.lineHeight, -1);
		super.extractRenderState(matrices, mouseX, mouseY, delta);
		MainUtil.renderLogo(matrices);
		matrices.popMatrix();
	}
	
	@Override
	public boolean keyPressed(KeyEvent keyInput) {
		if (keyInput.key() == GLFW.GLFW_KEY_ESCAPE) {
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