package com.luneruniverse.minecraft.mod.nbteditor;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.DynamicRegistryManagerHolder;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.Version;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.networking.MVNetworking;
import com.luneruniverse.minecraft.mod.nbteditor.packets.*;
import com.luneruniverse.minecraft.mod.nbteditor.server.NBTEditorServer;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NBTEditor implements ModInitializer {
	
	public static final Logger LOGGER = LogManager.getLogger("nbteditor");
	public static NBTEditorServer SERVER;
	public static boolean IS_SYSTEM_MAC = Util.getPlatform() == Util.OS.OSX;

	public static boolean hasControlDown() {
		if (IS_SYSTEM_MAC) {
			return InputConstants.isKeyDown(227) || InputConstants.isKeyDown(231);
		} else {
			return InputConstants.isKeyDown(224) || InputConstants.isKeyDown(228);
		}
	}

	public static boolean hasShiftDown() {
		return InputConstants.isKeyDown(225) || InputConstants.isKeyDown(229);
	}

	public static boolean hasAltDown() {
		return InputConstants.isKeyDown(226) || InputConstants.isKeyDown(230);
	}
	public static boolean isCut(int code) {
		return code == 88 && hasControlDown() && !hasShiftDown() && !hasAltDown();
	}

	public static boolean isPaste(int code) {
		return code == 86 && hasControlDown() && !hasShiftDown() && !hasAltDown();
	}

	public static boolean isCopy(int code) {
		return code == 67 && hasControlDown() && !hasShiftDown() && !hasAltDown();
	}

	public static boolean isSelectAll(int code) {
		return code == 65 && hasControlDown() && !hasShiftDown() && !hasAltDown();
	}

	@Override
	public void onInitialize() {
		MVNetworking.registerPacket(ContainerScreenS2CPacket.ID, ContainerScreenS2CPacket::new);
		MVNetworking.registerPacket(GetBlockC2SPacket.ID, GetBlockC2SPacket::new);
		MVNetworking.registerPacket(GetEntityC2SPacket.ID, GetEntityC2SPacket::new);
		MVNetworking.registerPacket(GetLecternBlockC2SPacket.ID, GetLecternBlockC2SPacket::new);
		MVNetworking.registerPacket(OpenEnderChestC2SPacket.ID, OpenEnderChestC2SPacket::new);
		MVNetworking.registerPacket(ProtocolVersionS2CPacket.ID, ProtocolVersionS2CPacket::new);
		MVNetworking.registerPacket(SetBlockC2SPacket.ID, SetBlockC2SPacket::new);
		MVNetworking.registerPacket(SetCursorC2SPacket.ID, SetCursorC2SPacket::new);
		MVNetworking.registerPacket(SetEntityC2SPacket.ID, SetEntityC2SPacket::new);
		MVNetworking.registerPacket(SetSlotC2SPacket.ID, SetSlotC2SPacket::new);
		MVNetworking.registerPacket(SummonEntityC2SPacket.ID, SummonEntityC2SPacket::new);
		MVNetworking.registerPacket(ViewBlockS2CPacket.ID, ViewBlockS2CPacket::read);
		MVNetworking.registerPacket(ViewEntityS2CPacket.ID, ViewEntityS2CPacket::new);
		
		SERVER = new NBTEditorServer();
		
		Version.newSwitch()
				.range("1.20.5", null, () -> ServerLifecycleEvents.SERVER_STARTING.register(DynamicRegistryManagerHolder::setServerManager))
				.range(null, "1.20.4", () -> {})
				.run();
	}
	
}
