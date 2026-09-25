package com.luneruniverse.minecraft.mod.nbteditor.multiversion.mixin.toggled;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.ChestSpecialRenderer;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// MC-69683: ChestSpecialRenderer.submit 忽略了 hasFoil 参数，导致箱子/末影箱等
// 方块实体物品（GUI 物品栏、手持、物品展示框）不显示附魔光效。
// 仿照 TridentSpecialRenderer：hasFoil 时用 entitySolidGlint 替换主模型提交，
// 因为 entitySolidGlint 的 fragment shader 内部已经把 glint 颜色叠加到主纹理上。
//
// 关键：entitySolidGlint(texture) 的参数必须是 atlasLocation（sprite.atlasLocation()），
// 不是 sprite.texture()（那是 sprite namespace id）。Sampler0 绑定的是 atlas 贴图。
// 另外 Chest 用 Sprite atlas 打包了多种箱子纹理，必须用 sprites.get(sprite)
// 作为 uvMapping 来限定只采样对应 sprite 的区域，否则会采整个 atlas 变成紫块。
@Mixin(ChestSpecialRenderer.class)
public class ChestSpecialRendererMixin {

	@Shadow
	@Final
	private ChestModel model;
	@Shadow
	@Final
	private float openness;
	@Shadow
	@Final
	private SpriteId sprite;
	@Shadow
	@Final
	private SpriteGetter sprites;

	@Inject(method = "submit", at = @At("HEAD"), cancellable = true)
	private void submitGlint(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
			int overlayCoords, boolean hasFoil, int outlineColor, CallbackInfo info) {
		if (hasFoil) {
			submitNodeCollector.submitModel(this.model, this.openness, poseStack,
					RenderTypes.entitySolidGlint(this.sprite.atlasLocation()),
					lightCoords, overlayCoords, -1,
					this.sprites.get(this.sprite), outlineColor);
			info.cancel();
		}
	}

}
