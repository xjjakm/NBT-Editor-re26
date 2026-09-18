package com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.manager.components;

import com.luneruniverse.minecraft.mod.nbteditor.misc.MixinLink;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.Attempt;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.DynamicRegistryManagerHolder;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.manager.DeserializableNBTManager;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

public class ComponentItemNBTManager implements DeserializableNBTManager<ItemStack> {
	
	private static HolderLookup.Provider getLookup() {
		try {
			return DynamicRegistryManagerHolder.getManager();
		} catch (RuntimeException e) {
			return VanillaRegistries.createWorldLookup();
		}
	}
	
	@Override
	public Attempt<CompoundTag> trySerialize(ItemStack subject) {
		if (subject.isEmpty())
			return new Attempt<>(new CompoundTag());
		
		DataResult<Tag> result = ItemStack.CODEC.encodeStart(
				getLookup().createSerializationContext(NbtOps.INSTANCE), subject);
		return new Attempt<>(
				result.resultOrPartial().map(nbt -> (CompoundTag) nbt.copy()),
				result.error().map(DataResult.Error::message).orElse(null));
	}
	@Override
	public Attempt<ItemStack> tryDeserialize(CompoundTag nbt) {
		if (nbt.getString("id").filter(id -> id.equals("minecraft:air") || id.equals(":air") || id.equals("air")).isPresent())
			return new Attempt<>(ItemStack.EMPTY);
		if (nbt.getInt("count").filter(count -> count <= 0).isPresent())
			return new Attempt<>(ItemStack.EMPTY);
		
		DataResult<Pair<ItemStack, Tag>> result = ItemStack.OPTIONAL_CODEC.decode(
				getLookup().createSerializationContext(NbtOps.INSTANCE), nbt.copy());
		return new Attempt<>(
				result.resultOrPartial().map(Pair::getFirst).map(item -> {
					if (item.has(DataComponents.MAX_DAMAGE) && item.getOrDefault(DataComponents.MAX_STACK_SIZE, 1) > 1)
						item.remove(DataComponents.MAX_DAMAGE);
					return item;
				}),
				result.error().map(DataResult.Error::message).orElse(null));
	}
	
	@Override
	public boolean hasNbt(ItemStack subject) {
		return !subject.getComponentsPatch().isEmpty();
	}
	@Override
	public CompoundTag getNbt(ItemStack subject) {
		DataResult<Tag> result = DataComponentPatch.CODEC.encodeStart(
				getLookup().createSerializationContext(NbtOps.INSTANCE), subject.getComponentsPatch());
		return (CompoundTag) result.resultOrPartial().map(Tag::copy).orElse(null);
	}
	@Override
	public CompoundTag getOrCreateNbt(ItemStack subject) {
		CompoundTag nbt = getNbt(subject);
		return nbt != null ? nbt : new CompoundTag();
	}
	@Override
	public void setNbt(ItemStack subject, CompoundTag nbt) {
		DataComponentPatch components = DataComponentPatch.CODEC.decode(
				getLookup().createSerializationContext(NbtOps.INSTANCE), nbt.copy()).getPartialOrThrow().getFirst();
		DataComponentPatch.SplitResult split = components.split();
		Integer maxDamage = split.added().get(DataComponents.MAX_DAMAGE);
		Integer maxStackSize = split.added().get(DataComponents.MAX_STACK_SIZE);
		boolean stackSizeMissing = !split.removed().contains(DataComponents.MAX_STACK_SIZE) && maxStackSize == null;
		if (maxDamage != null &&
				(stackSizeMissing ?
						subject.getPrototype().get(DataComponents.MAX_STACK_SIZE) > 1 :
						maxStackSize != null && maxStackSize > 1)) {
			components = components.forget(component -> component == DataComponents.MAX_DAMAGE);
		}
		MixinLink.setChanges(subject, components);
	}
	
	@Override
	public String getNbtString(ItemStack subject) {
		DataComponentPatch components = subject.getComponentsPatch();
		DataComponentPatch.SplitResult split = components.split();
		StringBuilder builder = new StringBuilder("[");
		boolean first = true;
		for (TypedDataComponent<?> typed : split.added()) {
			if (!first) builder.append(",");
			first = false;
			builder.append(typed.type());
			builder.append("=");
			builder.append(encodeComponent(typed.type(), typed.value()).getPartialOrThrow());
		}
		for (DataComponentType<?> removed : split.removed()) {
			if (!first) builder.append(",");
			first = false;
			builder.append("!");
			builder.append(removed);
		}
		builder.append(']');
		return builder.toString();
	}
	@SuppressWarnings("unchecked")
	private <T> DataResult<Tag> encodeComponent(DataComponentType<T> component, Object value) {
		return component.codecOrThrow().encodeStart(
				getLookup().createSerializationContext(NbtOps.INSTANCE), (T) value);
	}
	
}
