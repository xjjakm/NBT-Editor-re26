package com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.manager.nbt;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.Attempt;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.DynamicRegistryManagerHolder;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.manager.DeserializableNBTManager;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;

public class NBTItemNBTManager implements DeserializableNBTManager<ItemStack> {
	
	/**
	 * ItemStack.CODEC + RegistryOps 序列化到 CompoundTag
	 * 结构: { id: "...", count: N, components: { ... } }
	 */
	private RegistryOps<net.minecraft.nbt.Tag> getOps() {
		return RegistryOps.create(NbtOps.INSTANCE, DynamicRegistryManagerHolder.getManager());
	}
	
	@Override
	public Attempt<CompoundTag> trySerialize(ItemStack subject) {
		if (subject.isEmpty())
			return new Attempt<>(new CompoundTag());
		try {
			return new Attempt<>(ItemStack.CODEC.encodeStart(getOps(), subject)
					.result()
					.filter(tag -> tag instanceof CompoundTag)
					.map(tag -> (CompoundTag) tag)
					.orElse(new CompoundTag()));
		} catch (Exception e) {
			return new Attempt<>(new CompoundTag(), e.getMessage());
		}
	}
	
	@Override
	public Attempt<ItemStack> tryDeserialize(CompoundTag nbt) {
		try {
			return new Attempt<>(ItemStack.CODEC.parse(getOps(), nbt)
					.result()
					.orElse(ItemStack.EMPTY));
		} catch (Exception e) {
			return new Attempt<>(ItemStack.EMPTY, e.getMessage());
		}
	}
	
	@Override
	public boolean hasNbt(ItemStack subject) {
		if (subject.isEmpty())
			return false;
		// 26.3+: 有非默认 components 或 count != 1 就算有 NBT
		if (subject.getCount() != 1)
			return true;
		return !subject.getComponentsPatch().isEmpty();
	}
	
	@Override
	public CompoundTag getNbt(ItemStack subject) {
		if (subject.isEmpty())
			return null;
		Attempt<CompoundTag> attempt = trySerialize(subject);
		return attempt.value().map(CompoundTag::copy).orElse(null);
	}
	
	@Override
	public CompoundTag getOrCreateNbt(ItemStack subject) {
		if (subject.isEmpty())
			return new CompoundTag();
		Attempt<CompoundTag> attempt = trySerialize(subject);
		return attempt.value().map(CompoundTag::copy).orElse(new CompoundTag());
	}
	
	@Override
	public void setNbt(ItemStack subject, CompoundTag nbt) {
		if (subject.isEmpty() || nbt == null)
			return;
		Attempt<ItemStack> attempt = tryDeserialize(nbt);
		ItemStack result = attempt.value().orElse(null);
		if (result == null || result.isEmpty())
			return;
		// 从 result 复制数据到 subject（替换 components + count）
		// 保留原 Item 类型不变，只更新 components 和 count
		subject.setCount(result.getCount());
		subject.applyComponents(result.getComponentsPatch());
	}
	
}
