package com.luneruniverse.minecraft.mod.nbteditor.multiversion;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;

import java.lang.invoke.MethodType;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MVRegistry<T> implements Iterable<T> {

	private static final Cache<String, Reflection.MethodInvoker> methodCache = CacheBuilder.newBuilder().build();
	@SuppressWarnings("unchecked")
	private static <R> R call(Object registry, String method, Supplier<MethodType> type, Object... args) {
		try {
			return (R) methodCache.get(method, () -> Reflection.getMethod(Registry.class, method, type.get())).invoke(registry, args);
		} catch (ExecutionException | UncheckedExecutionException e) {
			throw new RuntimeException("Error invoking method", e);
		}
	}

	private static MVRegistry<Enchantment> ENCHANTMENT;
	public static MVRegistry<Enchantment> getEnchantmentRegistry() {
		Registry<Enchantment> registry = DynamicRegistryManagerHolder.getManager().lookupOrThrow(Registries.ENCHANTMENT);
		if (ENCHANTMENT == null || ENCHANTMENT.getInternalValue() != registry)
			ENCHANTMENT = new MVRegistry<>(registry);
		return ENCHANTMENT;
	}

	private static MVRegistry<DataComponentType<?>> COMPONENTS;
	public static MVRegistry<DataComponentType<?>> getComponentsRegistry() {
		if (COMPONENTS == null)
			COMPONENTS = new MVRegistry<>(BuiltInRegistries.DATA_COMPONENT_TYPE);
		return COMPONENTS;
	}

	public static <V, T extends V> T register(MVRegistry<V> registry, Identifier id, T entry) {
		return call(null, "register", () -> MethodType.methodType(Object.class, Registry.class, Identifier.class, Object.class), registry.value, id, entry);
	}

	public static final MVRegistry<Item> ITEM = new MVRegistry<>(BuiltInRegistries.ITEM);
	public static final MVRegistry<Block> BLOCK = new MVRegistry<>(BuiltInRegistries.BLOCK);
	public static final MVRegistry<EntityType<?>> ENTITY_TYPE = new MVRegistry<>(BuiltInRegistries.ENTITY_TYPE);
	public static final MVRegistry<Attribute> ATTRIBUTE = new MVRegistry<>(BuiltInRegistries.ATTRIBUTE);
	public static final MVRegistry<Potion> POTION = new MVRegistry<>(BuiltInRegistries.POTION);
	public static final MVRegistry<MobEffect> STATUS_EFFECT = new MVRegistry<>(BuiltInRegistries.MOB_EFFECT);

	private final Object value;

	private MVRegistry(Object value) {
		this.value = value;
	}

	@SuppressWarnings("unchecked")
	public Registry<T> getInternalValue() {
		return (Registry<T>) value;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Iterator<T> iterator() {
		return ((Iterable<T>) value).iterator();
	}

	public Optional<T> getOrEmpty(Identifier id) {
		return call(value, "getOptional", () -> MethodType.methodType(Optional.class, Identifier.class), id);
	}

	public Identifier getId(T entry) {
		return call(value, "getKey", () -> MethodType.methodType(Identifier.class, Object.class), entry);
	}

	public T get(Identifier id) {
		return call(value, "getValue", () -> MethodType.methodType(Object.class, Identifier.class), id);
	}

	public Set<Identifier> getIds() {
		return call(value, "keySet", () -> MethodType.methodType(Set.class));
	}

	public Set<Map.Entry<Identifier, T>> getEntrySet() {
		Set<Map.Entry<Object, T>> output = call(value, "entrySet", () -> MethodType.methodType(Set.class));
		return output.stream().map(entry -> Map.entry(getRegistryKeyValue(entry.getKey()), entry.getValue()))
			.collect(Collectors.toUnmodifiableSet());
	}
	private static Identifier getRegistryKeyValue(Object key) {
		return ((ResourceKey<?>) key).identifier();
	}

	public boolean containsId(Identifier id) {
		return call(value, "containsKey", () -> MethodType.methodType(boolean.class, Identifier.class), id);
	}

}