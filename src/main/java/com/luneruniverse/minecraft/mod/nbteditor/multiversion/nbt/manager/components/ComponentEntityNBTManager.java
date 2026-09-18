package com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.manager.components;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.Attempt;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.manager.NBTManager;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComponentEntityNBTManager implements NBTManager<Entity> {
	
	private static final Logger LOGGER = LoggerFactory.getLogger("NBTEditor");
	
	// Registry element codecs (e.g. ItemCost.CODEC via Item.CODEC in MerchantOffers)
	// require a registry context; plain NbtOps.INSTANCE silently drops them on encode.
	// Prefer the entity's own level registry (server-side data pack registries).
	private static HolderLookup.Provider getLookup(Entity subject) {
		if (subject.level() != null)
			return subject.level().registryAccess();
		return (MainUtil.client.getConnection() == null ? VanillaRegistries.createWorldLookup() : MainUtil.client.getConnection().registryAccess());
	}
	
	@Override
	public Attempt<CompoundTag> trySerialize(Entity subject) {
		TagValueOutput view = new TagValueOutput(ProblemReporter.DISCARDING, getLookup(subject).createSerializationContext(NbtOps.INSTANCE), new CompoundTag());
		view.putString("id", EntityType.getKey(subject.getType()).toString());
		subject.saveWithoutId(view);
		return new Attempt<>(view.buildResult());
	}
	
	@Override
	public boolean hasNbt(Entity subject) {
		return true;
	}
	@Override
	public CompoundTag getNbt(Entity subject) {
		TagValueOutput v = new TagValueOutput(ProblemReporter.DISCARDING, getLookup(subject).createSerializationContext(NbtOps.INSTANCE), new CompoundTag());
		subject.saveWithoutId(v);
		return v.buildResult();
	}
	@Override
	public CompoundTag getOrCreateNbt(Entity subject) {
		return getNbt(subject);
	}
	@Override
	public void setNbt(Entity subject, CompoundTag nbt) {
		ProblemReporter.Collector collector = new ProblemReporter.Collector();
		subject.load(TagValueInput.create(collector, getLookup(subject), nbt));
		if (!collector.isEmpty())
			LOGGER.info("[entity-load] {} load problems:\n{}", EntityType.getKey(subject.getType()), collector.getReport());
	}
	
}
