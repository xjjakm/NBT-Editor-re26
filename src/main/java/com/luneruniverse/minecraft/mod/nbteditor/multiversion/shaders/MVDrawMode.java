package com.luneruniverse.minecraft.mod.nbteditor.multiversion.shaders;

import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;

public enum MVDrawMode {
	LINES,
	LINE_STRIP,
	DEBUG_LINES,
	DEBUG_LINE_STRIP,
	TRIANGLES,
	TRIANGLE_STRIP,
	TRIANGLE_FAN,
	QUADS;
	
	private final Object value;
	
	MVDrawMode() {
		// 26.2+: VertexFormat.Mode was replaced by PrimitiveTopology
		// PrimitiveTopology has no LINE_STRIP (ordinal 1); fall back to DEBUG_LINE_STRIP
		value = (ordinal() == 1) ? PrimitiveTopology.DEBUG_LINE_STRIP : PrimitiveTopology.valueOf(name());
	}
	
	public Object getInternalValue() {
		return value;
	}
	
}
