package com.luneruniverse.minecraft.mod.nbteditor.screens.widgets;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

public record HSVColorAreaRenderState(
		RenderPipeline pipeline,
		TextureSetup textureSetup,
		Matrix3x2fc pose,
		int x0,
		int y0,
		int x1,
		int y1,
		int hueValue,
		@Nullable ScreenRectangle scissorArea,
		@Nullable ScreenRectangle bounds
) implements GuiElementRenderState {

	public HSVColorAreaRenderState(
			final RenderPipeline pipeline,
			final TextureSetup textureSetup,
			final Matrix3x2fc pose,
			final int x0,
			final int y0,
			final int x1,
			final int y1,
			final int hueValue,
			final @Nullable ScreenRectangle scissorArea
	) {
		this(pipeline, textureSetup, pose, x0, y0, x1, y1, hueValue, scissorArea, getBounds(x0, y0, x1, y1, pose, scissorArea));
	}

	@Override
	public void buildVertices(final VertexConsumer vertexConsumer) {
		vertexConsumer.addVertexWith2DPose(this.pose, this.x0, this.y0).setUv(0.0F, 0.0F).setUv2(this.hueValue, 0);
		vertexConsumer.addVertexWith2DPose(this.pose, this.x0, this.y1).setUv(0.0F, 1.0F).setUv2(this.hueValue, 0);
		vertexConsumer.addVertexWith2DPose(this.pose, this.x1, this.y1).setUv(1.0F, 1.0F).setUv2(this.hueValue, 0);
		vertexConsumer.addVertexWith2DPose(this.pose, this.x1, this.y0).setUv(1.0F, 0.0F).setUv2(this.hueValue, 0);
	}

	private static @Nullable ScreenRectangle getBounds(
			final int x0, final int y0, final int x1, final int y1, final Matrix3x2fc pose, final @Nullable ScreenRectangle scissorArea
	) {
		ScreenRectangle bounds = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose);
		return scissorArea != null ? scissorArea.intersection(bounds) : bounds;
	}
}
