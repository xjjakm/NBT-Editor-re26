package com.luneruniverse.minecraft.mod.nbteditor.screens.factories;

import com.luneruniverse.minecraft.mod.nbteditor.localnbt.LocalItem;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVRegistry;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.TextInst;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.Version;
import com.luneruniverse.minecraft.mod.nbteditor.nbtreferences.itemreferences.ItemReference;
import com.luneruniverse.minecraft.mod.nbteditor.screens.LocalEditorScreen;
import com.luneruniverse.minecraft.mod.nbteditor.screens.configurable.*;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.ItemTagReferences;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific.data.Enchants;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EnchantmentsScreen extends LocalEditorScreen<LocalItem> {
	
	@SuppressWarnings("unchecked")
	private static ConfigValueOverlay<String> getConfigEnchantment(ConfigCategory enchant) {
		return ((ConfigItem<ConfigValueOverlay<String>>) enchant.getConfigurable("enchantment")).getValue();
	}
	@SuppressWarnings("unchecked")
	private static ConfigValueNumber<Integer> getConfigLevel(ConfigCategory enchant) {
		return ((ConfigItem<ConfigValueNumber<Integer>>) enchant.getConfigurable("level")).getValue();
	}
	
	
	private final ConfigList config;
	private ConfigPanel panel;
	
	public EnchantmentsScreen(ItemReference ref) {
		super(TextInst.of("Enchantments"), ref);
		
		MVRegistry<Enchantment> registry = MVRegistry.getEnchantmentRegistry();
		Map<String, Holder<Enchantment>> allEnchantments = new LinkedHashMap<>();
		for (Holder<Enchantment> holder : registry.getInternalValue().asHolderIdMap()) {
			Identifier id = registry.getId(holder.value());
			if (id != null)
				allEnchantments.put(id.toString(), holder);
		}
		// 显示名称 → id 的映射，用于下拉框显示各种语言
		Map<String, String> displayNameToId = new LinkedHashMap<>();
		Map<String, String> idToDisplayName = new LinkedHashMap<>();
		for (Map.Entry<String, Holder<Enchantment>> e : allEnchantments.entrySet()) {
			String displayName = e.getValue().value().description().getString();
			displayNameToId.put(displayName, e.getKey());
			idToDisplayName.put(e.getKey(), displayName);
		}
		// 按显示名称排序
		final Map<String, String> sortedDisplayNameToId = displayNameToId.entrySet().stream()
				.sorted((a, b) -> a.getKey().compareToIgnoreCase(b.getKey()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
		
		ItemStack inputItem = ref.getItem();
		ConfigCategory entry = new ConfigCategory();
		List<String> orderedEnchants = sortedDisplayNameToId.entrySet().stream()
				.map(enchant -> Map.entry(enchant.getKey(), allEnchantments.get(enchant.getValue()).value().canEnchant(inputItem)))
				.sorted((a, b) -> {
					if (a.getValue()) {
						if (!b.getValue())
							return -1;
					} else if (b.getValue())
						return 1;
					return a.getKey().compareToIgnoreCase(b.getKey());
				})
				.map(Map.Entry::getKey).toList();
		String firstEnchant = orderedEnchants.get(0);
		entry.setConfigurable("enchantment", new ConfigItem<>(TextInst.translatable("nbteditor.enchantments.enchantment"),
				ConfigValueOverlay.forList(TextInst.translatable("nbteditor.enchantments.select"), firstEnchant, firstEnchant,
						orderedEnchants,
						name -> allEnchantments.get(sortedDisplayNameToId.getOrDefault(name, name)).value().canEnchant(inputItem),
						name -> name + " (" + sortedDisplayNameToId.getOrDefault(name, name) + ")")));
		entry.setConfigurable("level", new ConfigItem<>(TextInst.translatable("nbteditor.enchantments.level"),
				ConfigValueNumber.forInt(1, 1, 1,
						Version.<Integer>newSwitch()
								.range("1.17.1", null, 255)
								.range(null, "1.17", 32767)
								.get())));
		config = new ConfigList(TextInst.translatable("nbteditor.enchantments"), false, entry);

		ItemTagReferences.ENCHANTMENTS.get(localNBT.getEditableItem()).enchants().forEach(enchant -> {
			ConfigCategory enchantConfig = entry.clone(true);
			// Use the holder's key directly instead of searching by description
			Identifier id = enchant.enchantHolder().unwrapKey().map(key -> key.identifier()).orElse(null);
			if (id == null) {
				// Fallback: search by description match
				for (Holder<Enchantment> e : registry.getInternalValue().asHolderIdMap()) {
					if(e.value().description().getString().equals(enchant.enchant().description().getString())) {
						id = registry.getId(e.value());
					}
				}
			}
			// 显示名称而不是 id
			String displayName = id != null ? idToDisplayName.getOrDefault(id.toString(), id.toString()) : "";
			getConfigEnchantment(enchantConfig).setValue(displayName);
			getConfigLevel(enchantConfig).setValue(enchant.level());
			config.addConfigurable(enchantConfig);
		});
		
		config.addValueListener(source -> {
			List<Enchants.EnchantWithLevel> newEnchants = new ArrayList<>();
			for (ConfigPath path : config.getConfigurables().values()) {
				ConfigCategory enchant = (ConfigCategory) path;
				String displayName = getConfigEnchantment(enchant).getValidValue();
				String enchantId = sortedDisplayNameToId.getOrDefault(displayName, displayName);
				newEnchants.add(new Enchants.EnchantWithLevel(
						allEnchantments.get(enchantId),
						getConfigLevel(enchant).getValidValue()));
			}
			ItemTagReferences.ENCHANTMENTS.set(localNBT.getEditableItem(), new Enchants(newEnchants));
			checkSave();
		});
	}
	
	@Override
	protected void initEditor() {
		ConfigPanel newPanel = addRenderableWidget(new ConfigPanel(16, 64, width - 32, height - 80, config));
		if (panel != null)
			newPanel.setScroll(panel.getScroll());
		panel = newPanel;
	}
	
	@Override
	protected void renderEditor(Matrix3x2fStack matrices, int mouseX, int mouseY, float delta) {
		renderTip(matrices, "nbteditor.enchantments.tip");
	}
	
}
