package com.luneruniverse.minecraft.mod.nbteditor.mixin.toggled;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.ChestSpecialRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// MC-69683: 26.x 的 ChestSpecialRenderer.submit 忽略了 hasFoil 参数，导致箱子/末影箱等
// 方块实体物品（GUI 物品栏、手持、物品展示框）不显示附魔光效。
// 仿照 TridentSpecialRenderer 的做法：hasFoil 时用 entityGlint 渲染类型再提交一次模型。
@Mixin(ChestSpecialRenderer.class)
public class ChestSpecialRendererMixin {

	@Shadow
	@Final
	private ChestModel model;
	@Shadow
	@Final
	private float openness;

	@Inject(method = "submit", at = @At("TAIL"))
	private void submitGlint(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
			int overlayCoords, boolean hasFoil, int outlineColor, CallbackInfo info) {
		if (hasFoil) {
			submitNodeCollector.order(1).submitModel(this.model, this.openness, poseStack,
					RenderTypes.entityGlint(), lightCoords, overlayCoords, outlineColor, null);
		}
	}

}