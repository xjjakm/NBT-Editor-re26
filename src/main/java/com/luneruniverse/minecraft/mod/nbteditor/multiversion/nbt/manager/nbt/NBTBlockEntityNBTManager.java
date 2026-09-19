package com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.manager.nbt;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.Attempt;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.DynamicRegistryManagerHolder;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.manager.NBTManager;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;

public class NBTBlockEntityNBTManager implements NBTManager<BlockEntity> {
	
	@Override
	public Attempt<CompoundTag> trySerialize(BlockEntity subject) {
		try {
			return new Attempt<>(subject.saveWithFullMetadata(DynamicRegistryManagerHolder.getManager()));
		} catch (Exception e) {
			return new Attempt<>(new CompoundTag(), e.getMessage());
		}
	}
	
	@Override
	public boolean hasNbt(BlockEntity subject) {
		return true;
	}
	
	@Override
	public CompoundTag getNbt(BlockEntity subject) {
		try {
			// saveWithoutMetadata: 包含自定义数据 + components，不含 id/pos 等 metadata
			// 与旧版 createNbt（writeNbt 不含 identifying data）语义对齐
			return subject.saveWithoutMetadata(DynamicRegistryManagerHolder.getManager());
		} catch (Exception e) {
			return new CompoundTag();
		}
	}
	
	@Override
	public CompoundTag getOrCreateNbt(BlockEntity subject) {
		return getNbt(subject);
	}
	
	@Override
	public void setNbt(BlockEntity subject, CompoundTag nbt) {
		if (nbt == null)
			return;
		try {
			// 用 loadWithComponents 覆盖 BlockEntity 的 components + 自定义数据
			// 注意：不覆盖 pos/id 等 metadata（保持 BlockEntity 在世界中的身份）
			subject.loadWithComponents(TagValueInput.create(
					ProblemReporter.DISCARDING,
					DynamicRegistryManagerHolder.getManager(),
					nbt.copy()));
		} catch (Exception ignored) {
			// 反序列化失败时静默忽略，编辑器还能显示当前内容
		}
	}
	
}
