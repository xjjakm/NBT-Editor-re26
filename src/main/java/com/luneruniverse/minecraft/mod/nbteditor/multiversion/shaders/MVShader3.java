package com.luneruniverse.minecraft.mod.nbteditor.multiversion.shaders;

import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;

import java.util.ArrayList;
import java.util.List;

public class MVShader3 extends MVShader {
	
	public static final List<RenderPipeline> RENDER_PIPELINES = new ArrayList<>();
	
	private final RenderPipeline pipeline;
	private final VertexFormat format;
	private final PrimitiveTopology topology;
	
	public MVShader3(MVShader.Builder builder) {
		RenderPipeline.Builder pipelineBuilder = RenderPipeline.builder(builder.getSnippets().stream()
				.map(snippet -> (RenderPipeline.Snippet) snippet).toArray(RenderPipeline.Snippet[]::new))
				.withLocation("pipeline/" + builder.getLayerName())
				.withVertexShader("core/" + builder.getShaderName())
				.withFragmentShader("core/" + builder.getShaderName())
				.withVertexBinding(0, (VertexFormat) builder.getVertexFormat().getInternalValue())
				.withPrimitiveTopology((PrimitiveTopology) builder.getDrawMode().getInternalValue());
		
		if (builder.isTranslucentBlendFunc())
			pipelineBuilder.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT));
		
		pipeline = RenderPipelines.register(pipelineBuilder.build());
		format = (VertexFormat) builder.getVertexFormat().getInternalValue();
		topology = (PrimitiveTopology) builder.getDrawMode().getInternalValue();
		
		RENDER_PIPELINES.add(pipeline);
	}
	
	@Override
	public RenderPipeline getPipeline() {
		return pipeline;
	}
	
	@Override
	public VertexFormat getFormat() {
		return format;
	}
	
	public PrimitiveTopology getTopology() {
		return topology;
	}
	
}
