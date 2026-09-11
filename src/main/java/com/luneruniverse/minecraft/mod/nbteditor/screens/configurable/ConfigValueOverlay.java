package com.luneruniverse.minecraft.mod.nbteditor.screens.configurable;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVButtonWidget;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVDrawableHelper;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.TextInst;
import com.luneruniverse.minecraft.mod.nbteditor.screens.widgets.SelectionOverlay;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A config value shown as a button that opens a searchable selection overlay
 * (same UX as the item selection overlay) instead of an inline dropdown.
 */
public class ConfigValueOverlay<T> extends MVButtonWidget implements ConfigValue<T, ConfigValueOverlay<T>> {
	
	public static <T> ConfigValueOverlay<T> forList(Component title, T value, T defaultValue, List<T> allValues) {
		return forList(title, value, defaultValue, allValues, ignored -> false);
	}
	public static <T> ConfigValueOverlay<T> forList(Component title, T value, T defaultValue, List<T> allValues, Predicate<T> importantValues) {
		return forList(title, value, defaultValue, allValues, importantValues, Object::toString);
	}
	public static <T> ConfigValueOverlay<T> forList(Component title, T value, T defaultValue, List<T> allValues, Predicate<T> importantValues, Function<T, String> display) {
		return new ConfigValueOverlay<>(title, value, defaultValue, allValues, importantValues, display, new ArrayList<>());
	}
	
	protected Component title;
	protected T value;
	protected final T defaultValue;
	protected final List<T> allValues;
	protected final Predicate<T> importantValues;
	protected final Function<T, String> display;
	
	protected final List<ConfigValueListener<ConfigValueOverlay<T>>> onChanged;
	
	@SuppressWarnings("unchecked")
	private ConfigValueOverlay(Component title, T value, T defaultValue, List<T> allValues, Predicate<T> importantValues, Function<T, String> display, List<ConfigValueListener<ConfigValueOverlay<T>>> onChanged) {
		super(0, 0, getMaxWidth(allValues, display) + MainUtil.client.font.lineHeight * 2, 20, TextInst.of(display.apply(value)),
				btn -> ((ConfigValueOverlay<T>) btn).onClickButton());
		
		this.title = title;
		this.value = value;
		this.defaultValue = defaultValue;
		this.allValues = allValues;
		this.importantValues = importantValues;
		this.display = display;
		this.onChanged = onChanged;
	}
	private static <T> int getMaxWidth(List<T> allValues, Function<T, String> display) {
		return allValues.stream().map(display).mapToInt(MainUtil.client.font::width).max().orElse(0);
	}
	
	private void onClickButton() {
		this.playDownSound(Minecraft.getInstance().getSoundManager());
		SelectionOverlay.show(title, allValues, importantValues, display, this::setValue);
	}
	
	@Override
	public void renderButton(Matrix3x2fStack matrices, int mouseX, int mouseY, float delta) {
		super.renderButton(matrices, mouseX, mouseY, delta);
		if (isHoveredOrFocused() && value instanceof ConfigTooltipSupplier)
			((ConfigTooltipSupplier) value).getTooltip().render(matrices, mouseX, mouseY);
	}
	
	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		return super.mouseClicked(click, doubled);
	}
	
	@Override
	public T getDefaultValue() {
		return defaultValue;
	}
	
	@Override
	public void setValue(T value) {
		this.value = value;
		setMessage(TextInst.of(display.apply(value)));
		onChanged.forEach(listener -> listener.onValueChanged(this));
	}
	@Override
	public T getConfigValue() {
		return value;
	}
	@Override
	public boolean isValueValid() {
		return true;
	}
	@Override
	public ConfigValueOverlay<T> addValueListener(ConfigValueListener<ConfigValueOverlay<T>> listener) {
		onChanged.add(listener);
		return this;
	}
	
	@Override
	public int getSpacingWidth() {
		return this.width;
	}
	
	@Override
	public int getSpacingHeight() {
		return this.height;
	}
	
	@Override
	public int getRenderHeight() {
		return getSpacingHeight();
	}
	
	@Override
	public ConfigValueOverlay<T> clone(boolean defaults) {
		return new ConfigValueOverlay<>(title, defaults ? defaultValue : value, defaultValue, allValues, importantValues, display, onChanged);
	}
	
	@Override
	public boolean keyPressed(KeyEvent keyInput) {
		return false; // Stop space from triggering the button
	}
	
}