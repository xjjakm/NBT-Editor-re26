package com.luneruniverse.minecraft.mod.nbteditor.screens.configurable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVElement;
import com.luneruniverse.minecraft.mod.nbteditor.util.OrderedMap;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

public abstract class ConfigGrouping<K, T extends ConfigGrouping<K, T>> implements ConfigPathNamed {
	
	protected interface Constructor<K, T extends ConfigGrouping<K, T>> {
		T newInstance(Component name);
	}
	
	protected final Component name;
	protected final OrderedMap<K, ConfigPath> paths;
	private final Constructor<K, T> cloneImpl;
	
	protected Component namePrefix;
	protected final List<ConfigValueListener<ConfigValue<?, ?>>> onChanged;

	/** 当前持有输入焦点的子路径；所有键盘/IME事件只路由给它。 */
	protected ConfigPath focusedChild;

	@Override
	public void clearFocusRecursive() {
		for (ConfigPath path : paths.values()) {
			path.clearFocusRecursive();
		}
		focusedChild = null;
	}

	protected ConfigGrouping(Component name, Constructor<K, T> cloneImpl) {
		this.name = name;
		this.paths = new OrderedMap<>();
		this.cloneImpl = cloneImpl;
		this.onChanged = new ArrayList<>();
	}
	
	public Component getName() {
		return name;
	}
	@Override
	public void setNamePrefix(Component prefix) {
		namePrefix = prefix;
	}
	@Override
	public Component getNamePrefix() {
		return namePrefix;
	}
	
	@SuppressWarnings("unchecked")
	public T setConfigurable(K key, ConfigPath path) {
		paths.put(key, path);
		path.addValueListener(source -> onChanged.forEach(listener -> listener.onValueChanged(source)));
		path.setParent(this);
		return (T) this;
	}
	public ConfigPath getConfigurable(K key) {
		return paths.get(key);
	}
	public Map<K, ConfigPath> getConfigurables() {
		return Collections.unmodifiableMap(paths);
	}
	@SuppressWarnings("unchecked")
	public T sort(Comparator<K> sorter) {
		paths.sort(sorter);
		return (T) this;
	}
	@SuppressWarnings("unchecked")
	public T setSorter(Comparator<K> sorter) {
		paths.setSorter(sorter);
		return (T) this;
	}
	
	@Override
	public boolean isValueValid() {
		return paths.values().stream().allMatch(ConfigPath::isValueValid);
	}
	@SuppressWarnings("unchecked")
	@Override
	public T addValueListener(ConfigValueListener<ConfigValue<?, ?>> listener) {
		onChanged.add(listener);
		return (T) this;
	}
	
	@Override
	public T clone(boolean defaults) {
		T output = cloneImpl.newInstance(name);
		paths.forEach((key, path) -> output.setConfigurable(key, path.clone(defaults)));
		output.onChanged.addAll(onChanged);
		return output;
	}
	
	
	// Make sure subclasses offset the mouse properly
	@Override
	public abstract boolean mouseClicked(MouseButtonEvent click, boolean b);
	@Override
	public abstract boolean mouseReleased(MouseButtonEvent click);
	@Override
	public abstract void mouseMoved(double mouseX, double mouseY);
	@Override
	public abstract boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY);
	@Override
	public abstract boolean mouseScrolled(double mouseX, double mouseY, double xAmount, double yAmount);

	/** 清除本层所有子元素的焦点（递归）；subclass 的 mouseClicked 应在分发事件前调用。 */
	protected void clearChildFocus() {
		for (ConfigPath path : paths.values()) {
			path.clearFocusRecursive();
		}
		focusedChild = null;
	}
	/** 设置新的 focusedChild；subclass 在子元素返回 true 后调用。 */
	protected void setChildFocused(ConfigPath child) {
		if (focusedChild != child && focusedChild instanceof MVElement mv)
			mv.setMultiFocused(false);
		focusedChild = child;
		if (child instanceof MVElement mv)
			mv.setMultiFocused(true);
	}

	@Override
	public boolean keyPressed(KeyEvent keyInput) {
		return focusedChild != null && focusedChild.keyPressed(keyInput);
	}
	@Override
	public boolean keyReleased(KeyEvent keyInput) {
		return focusedChild != null && focusedChild.keyReleased(keyInput);
	}
	@Override
	public boolean charTyped(CharacterEvent charInput) {
		return focusedChild != null && focusedChild.charTyped(charInput);
	}
	@Override
	public boolean preeditUpdated(@Nullable PreeditEvent event) {
		return focusedChild != null && focusedChild.preeditUpdated(event);
	}
	
	@Override
	public void tick() {
		for (ConfigPath path : new ArrayList<>(paths.values()))
			path.tick();
	}
}
