package com.luneruniverse.minecraft.mod.nbteditor.multiversion.shaders;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.vertex.VertexFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public abstract class MVShader {

	public static class Builder {

		private final String shaderName;
		private final String layerName;
		private final MVVertexFormat vertexFormat;
		private final MVDrawMode drawMode;
		private final int expectedBufferSize;
		private final boolean affectsOutline;

		private final List<Object> snippets;
		private boolean translucentBlendFunc;

		public Builder(String shaderName, String layerName, MVVertexFormat vertexFormat, MVDrawMode drawMode,
				int expectedBufferSize, boolean affectsOutline) {
			this.shaderName = shaderName;
			this.layerName = layerName;
			this.vertexFormat = vertexFormat;
			this.drawMode = drawMode;
			this.expectedBufferSize = expectedBufferSize;
			this.affectsOutline = affectsOutline;
			this.snippets = new ArrayList<>();
			this.translucentBlendFunc = false;
		}

		public Builder withSnippet(Supplier<Object> snippet) {
			snippets.add(snippet.get());
			return this;
		}

		public Builder withTranslucentBlendFunc(boolean translucentBlendFunc) {
			this.translucentBlendFunc = translucentBlendFunc;
			return this;
		}

		public String getShaderName() { return shaderName; }
		public String getLayerName() { return layerName; }
		public MVVertexFormat getVertexFormat() { return vertexFormat; }
		public MVDrawMode getDrawMode() { return drawMode; }
		public List<Object> getSnippets() { return snippets; }
		public boolean isTranslucentBlendFunc() { return translucentBlendFunc; }

		public MVShader build() {
			return new MVShader3(this);
		}

	}

	public abstract RenderPipeline getPipeline();
	public abstract VertexFormat getFormat();

}
