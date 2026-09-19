package com.luneruniverse.minecraft.mod.nbteditor.mixin.toggled;

import com.luneruniverse.minecraft.mod.nbteditor.misc.BasicMixinPlugin;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

import java.util.List;

public class NBTEditorMixinPlugin extends BasicMixinPlugin {

	@Override
	public void addMixins(List<String> output) {
		// 26.3+ 全保留
		output.add("toggled.ServerGamePacketListenerImplMixin");
		output.add("toggled.ArmorSlotMixin");

		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER)
			return;

		output.add("toggled.GuiGraphicsExtractorMixin");
		output.add("toggled.GuiRenderStateMixin");
		output.add("toggled.ItemStackMixin");
		output.add("toggled.TooltipMixin");
		output.add("toggled.EnchantmentMixin");
		output.add("toggled.ItemModelResolverMixin");
		output.add("toggled.ItemRenderStateLayerRenderStateMixin");
		output.add("toggled.ClientPacketListenerMixin");
		output.add("toggled.SnbtGrammarMixin");
	}

}
