package com.luneruniverse.minecraft.mod.nbteditor.screens.factories;

import com.luneruniverse.minecraft.mod.nbteditor.commands.OpenCommand;
import com.luneruniverse.minecraft.mod.nbteditor.commands.factories.AttributesCommand;
import com.luneruniverse.minecraft.mod.nbteditor.commands.factories.BlockStatesCommand;
import com.luneruniverse.minecraft.mod.nbteditor.commands.factories.SignboardCommand;
import com.luneruniverse.minecraft.mod.nbteditor.localnbt.LocalNBT;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.IdentifierInst;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.TextInst;
import com.luneruniverse.minecraft.mod.nbteditor.nbtreferences.EntityReference;
import com.luneruniverse.minecraft.mod.nbteditor.nbtreferences.NBTReference;
import com.luneruniverse.minecraft.mod.nbteditor.nbtreferences.itemreferences.ItemReference;
import com.luneruniverse.minecraft.mod.nbteditor.screens.LocalEditorScreen;
import com.luneruniverse.minecraft.mod.nbteditor.screens.NBTEditorScreen;
import com.luneruniverse.minecraft.mod.nbteditor.screens.configurable.ConfigButton;
import com.luneruniverse.minecraft.mod.nbteditor.screens.configurable.ConfigCategory;
import com.luneruniverse.minecraft.mod.nbteditor.screens.configurable.ConfigPanel;
import com.luneruniverse.minecraft.mod.nbteditor.screens.containers.ContainerScreen;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class LocalFactoryScreen<L extends LocalNBT> extends LocalEditorScreen<L> {
	
	public static final Identifier FACTORY_ICON = IdentifierInst.of("nbteditor", "textures/factory.png");
	
	public record LocalFactoryReference(Component buttonText, Predicate<NBTReference<?>> supported, Consumer<NBTReference<?>> factory) {}
	public static final List<LocalFactoryReference> BASIC_FACTORIES = new ArrayList<>();
	private static void addFactory(String key, Predicate<NBTReference<?>> supported, Function<NBTReference<?>, Screen> screen) {
		BASIC_FACTORIES.add(new LocalFactoryReference(TextInst.translatable(key), supported,
				ref -> MainUtil.client.gui.setScreen(screen.apply(ref))));
	}
	private static <T extends NBTReference<?>> void addFactory(String key, Predicate<T> supported, Function<T, Screen> screen, Class<T> clazz) {
		addFactory(key, ref -> clazz.isInstance(ref) && supported.test(clazz.cast(ref)), ref -> screen.apply(clazz.cast(ref)));
	}
	private static void addFactory(String key, Function<NBTReference<?>, Screen> screen) {
		addFactory(key, ref -> true, screen);
	}
	private static <T extends NBTReference<?>> void addFactory(String key, Function<T, Screen> screen, Class<T> clazz) {
		addFactory(key, ref -> true, screen, clazz);
	}
	static {
		addFactory("nbteditor", NBTEditorScreen::new);
		BASIC_FACTORIES.add(new LocalFactoryReference(TextInst.translatable("nbteditor.container"),
				OpenCommand.CONTAINER_FILTER, ContainerScreen::show));
		addFactory("nbteditor.book", ref -> ref.getItem().getItem() == Items.WRITTEN_BOOK, BookScreen::new, ItemReference.class);
		addFactory("nbteditor.display", DisplayScreen::new);
		addFactory("nbteditor.signboard", SignboardCommand.SIGNBOARD_FILTER, SignboardScreen::new);
		addFactory("nbteditor.enchantments", EnchantmentsScreen::new, ItemReference.class);
		addFactory("nbteditor.attributes", AttributesCommand.ATTRIBUTES_FILTER, AttributesScreen::new);
		addFactory("nbteditor.block_states", BlockStatesCommand.BLOCK_FILTER, BlockStatesScreen::new);
		addFactory("nbteditor.villager", ref -> ref instanceof EntityReference && ((EntityReference) ref).getEntityType() == EntityTypes.VILLAGER,
				VillagerScreen::new, EntityReference.class);
		addFactory("nbteditor.entity_attributes", ref -> ref instanceof EntityReference,
				EntityAttributesScreen::new, EntityReference.class);
		addFactory("nbteditor.spawn_egg_attributes",
				ref -> ((ItemReference) ref).getItem().getItem() instanceof SpawnEggItem,
				SpawnEggAttributesScreen::new, ItemReference.class);
	}
	
	private final ConfigCategory config;
	private ConfigPanel panel;
	
	// 从该面板进入的子编辑器中「保存后要返回的屏幕」（非 null 时子编辑器保存按钮变为「保存并返回」）
	private Screen returnScreen;
	public void setReturnScreen(Screen returnScreen) {
		this.returnScreen = returnScreen;
	}
	
	public LocalFactoryScreen(NBTReference<L> ref) {
		super(TextInst.of("Factories"), ref);
		this.config = new ConfigCategory();
		for (LocalFactoryReference factory : BASIC_FACTORIES) {
			if (factory.supported().test(ref)) {
				this.config.setConfigurable(factory.buttonText().getString(), new ConfigButton(150, factory.buttonText(),
						_ -> openFactory(factory.factory(), ref)));
			}
		}
	}
	
	private void openFactory(Consumer<NBTReference<?>> factory, NBTReference<?> ref) {
		factory.accept(ref);
		if (returnScreen != null) {
			Screen current = MainUtil.client.gui.screen();
			if (current instanceof LocalEditorScreen<?> localScreen)
				localScreen.setSaveReturnTarget(returnScreen);
		}
	}
	
	@Override
	protected boolean isSaveRequried() {
		return false;
	}
	
	@Override
	protected FactoryLink<L> getFactoryLink() {
		return null;
	}
	
	@Override
	protected void initEditor() {
		ConfigPanel newPanel = addRenderableWidget(new ConfigPanel(16, 64, width - 32, height - 80, config));
		if (panel != null)
			newPanel.setScroll(panel.getScroll());
		panel = newPanel;
	}
	
}
