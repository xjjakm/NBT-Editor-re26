package com.luneruniverse.minecraft.mod.nbteditor.multiversion.shaders;

import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.vertex.VertexFormat;

import java.util.Map;

public class MVVertexFormat_of {
	
	// Avoid loading VertexFormat$Builder when MVVertexFormat is loaded by moving references to a different file
	public static Object of(Map<String, MVVertexFormatElement> elements) {
		VertexFormat.Builder vertexBuilder = VertexFormat.builder(0);
		elements.forEach((name, element) -> vertexBuilder.addAttribute(name, (GpuFormat) element.getInternalValue()));
		return vertexBuilder.build();
	}
	
}
