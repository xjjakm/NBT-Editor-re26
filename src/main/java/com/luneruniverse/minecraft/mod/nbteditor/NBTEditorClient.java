package com.luneruniverse.minecraft.mod.nbteditor;

import com.luneruniverse.minecraft.mod.nbteditor.addons.NBTEditorAPI;
import com.luneruniverse.minecraft.mod.nbteditor.addons.NBTEditorAddon;
import com.luneruniverse.minecraft.mod.nbteditor.async.HeadRefreshThread;
import com.luneruniverse.minecraft.mod.nbteditor.clientchest.*;
import com.luneruniverse.minecraft.mod.nbteditor.commands.CommandHandler;
import com.luneruniverse.minecraft.mod.nbteditor.containers.ContainerIOs;
import com.luneruniverse.minecraft.mod.nbteditor.misc.MixinLink;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVEnchantments;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVMisc;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.Reflection;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.TextInst;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.networking.MVClientNetworking;
import com.luneruniverse.minecraft.mod.nbteditor.packets.OpenEnderChestC2SPacket;
import com.luneruniverse.minecraft.mod.nbteditor.screens.ConfigScreen;
import com.luneruniverse.minecraft.mod.nbteditor.screens.containers.ClientChestScreen;
import com.luneruniverse.minecraft.mod.nbteditor.screens.containers.CursorManager;
import com.luneruniverse.minecraft.mod.nbteditor.server.NBTEditorServer;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import tsp.headdb.ported.HeadAPI;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class NBTEditorClient implements ClientModInitializer {
	
	public static final File SETTINGS_FOLDER = new File("nbteditor");
	public static CursorManager CURSOR_MANAGER;
	public static ClientChest CLIENT_CHEST;
	public static NBTEditorServerConn SERVER_CONN;
	
	private static final Map<String, NBTEditorAddon> addons = new HashMap<>();
	public static NBTEditorAddon getAddon(String modId) {
		return addons.get(modId);
	}
	public static Map<String, NBTEditorAddon> getAddons() {
		return Collections.unmodifiableMap(addons);
	}
	
	@Override
	public void onInitializeClient() {
		NBTEditorServer.IS_DEDICATED = false;
		
		if (!SETTINGS_FOLDER.exists())
			SETTINGS_FOLDER.mkdir();

		MVMisc.onRegistriesLoad(this::onRegistriesLoad);
		ExtraDataFixes.init();
	}
	
	private void onRegistriesLoad() {
		new Thread(() -> {


			while (MainUtil.client.level == null) {
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
				}
			}

			ItemStack clientChestIcon = new ItemStack(Items.ENDER_CHEST);
			clientChestIcon.set(DataComponents.CUSTOM_NAME, TextInst.translatable("itemGroup.nbteditor.client_chest"));

			ItemStack inventoryIcon = new ItemStack(Items.CHEST);
			inventoryIcon.set(DataComponents.CUSTOM_NAME, TextInst.translatable("itemGroup.nbteditor.inventory"));

			// 26.x：用 enchantment_glint_override 数据组件强制显示附魔光效；旧版本回退到忠诚附魔
			boolean glintOverrideApplied = false;
			try {
				DataComponentType<Boolean> glintOverride = (DataComponentType<Boolean>) Reflection
						.getField(DataComponents.class, "ENCHANTMENT_GLINT_OVERRIDE", "Lnet/minecraft/core/component/DataComponentType;")
						.get(null);
				if (glintOverride != null) {
					clientChestIcon.set(glintOverride, true);
					glintOverrideApplied = true;
				}
			} catch (Exception e) {
				NBTEditor.LOGGER.warn("Failed to apply enchantment glint override for the client chest tab", e);
			}
			if (!glintOverrideApplied)
				MVEnchantments.addEnchantment(clientChestIcon, MVEnchantments.LOYALTY, 1);
			NBTEditor.LOGGER.warn("[tab-debug] client chest tab: overrideApplied={}, hasFoil={}, components={}",
					glintOverrideApplied, clientChestIcon.hasFoil(), clientChestIcon.getComponents());
			NBTEditorAPI.registerInventoryTab(clientChestIcon,
					ClientChestScreen::show,
					screen -> screen instanceof CreativeModeInventoryScreen || (screen instanceof InventoryScreen && SERVER_CONN.isEditingExpanded()));
			NBTEditorAPI.registerInventoryTab(inventoryIcon,
					CURSOR_MANAGER::showRoot,
					screen -> screen instanceof ClientChestScreen);
			NBTEditorAPI.registerInventoryTab(new ItemStack(Items.ENDER_CHEST),
					() -> {
						// 只在客户端物品栏界面（纯客户端菜单）需要手动关闭；普通背包/创造界面直接发包，
						// 由服务端打开末影箱菜单自然切换界面（与 /open echest 指令行为一致）
						if (MainUtil.client.gui.screen() instanceof ClientChestScreen)
							CURSOR_MANAGER.closeRoot();
						MVClientNetworking.send(new OpenEnderChestC2SPacket());
					},
					screen -> (screen instanceof CreativeModeInventoryScreen || screen instanceof InventoryScreen || screen instanceof ClientChestScreen)
							&& SERVER_CONN.isEditingExpanded());

		}).start();

		CommandHandler.registerCommands();
		try {
			HeadAPI.loadFavorites();
		} catch (IOException e) {
			NBTEditor.LOGGER.error("Error while loading HeadDB favorites", e);
		}
		try {
			HeadAPI.loadCustomHeads();
		} catch (IOException e) {
			NBTEditor.LOGGER.error("Error while loading HeadDB custom heads", e);
		}
		ContainerIOs.loadClass();
		new HeadRefreshThread().start();
		ConfigScreen.loadSettings();
		CURSOR_MANAGER = new CursorManager();

		CLIENT_CHEST = new ClientChest(ConfigScreen.isLargeClientChest() ? new LargeClientChestPageCache(5) : new SmallClientChestPageCache(100));
		MVClientNetworking.PlayNetworkStateEvents.Start.EVENT.register(networkHandler -> {
			ClientChestHelper.loadDefaultPages(PageLoadLevel.DYNAMIC_ITEMS);
			ClientChestHelper.loadDefaultPages(PageLoadLevel.NORMAL_ITEMS);
		});
		//MVClientNetworking.PlayNetworkStateEvents.Stop.EVENT.register(() -> ClientChestHelper.unloadAllPages(PageLoadLevel.NORMAL_ITEMS));


		SERVER_CONN = new NBTEditorServerConn();

		for (EntrypointContainer<NBTEditorAddon> container : FabricLoader.getInstance()
				.getEntrypointContainers("nbteditor", NBTEditorAddon.class)) {
			addons.put(container.getProvider().getMetadata().getId(), container.getEntrypoint());
		}
		addons.forEach((id, addon) -> addon.onInit());

	}
	
}
