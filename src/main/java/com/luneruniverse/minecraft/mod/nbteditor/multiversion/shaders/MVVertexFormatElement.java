package com.luneruniverse.minecraft.mod.nbteditor.multiversion.shaders;

import com.mojang.renderpearl.api.GpuFormat;

public class MVVertexFormatElement {
	
	public static final MVVertexFormatElement POSITION = new MVVertexFormatElement(GpuFormat.RGB32_FLOAT);
	public static final MVVertexFormatElement UV0 = new MVVertexFormatElement(GpuFormat.RG32_FLOAT);
	public static final MVVertexFormatElement UV2 = new MVVertexFormatElement(GpuFormat.RG16_SINT);
	
	private final Object value;
	
	private MVVertexFormatElement(Object value) {
		this.value = value;
	}
	
	public Object getInternalValue() {
		return value;
	}
	
}
