package com.luneruniverse.minecraft.mod.nbteditor.multiversion;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.lang.invoke.MethodType;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class RegistryCache {
	
	private static final Map<RegistryAccess, RegistryCache> caches = Collections.synchronizedMap(new WeakHashMap<>());
	public static RegistryCache get(RegistryAccess registryManager) {
		return caches.computeIfAbsent(registryManager, key -> new RegistryCache(registryManager, false));
	}
	
	private static final Supplier<Reflection.MethodInvoker> Registry_getEntry =
			Reflection.getOptionalMethod(Registry.class, "method_55841", MethodType.methodType(Optional.class, Identifier.class));
	private static final boolean IS_1_21_2_PLUS = true;
	/**
	 * @return May be null
	 */
	public static <T> Holder.Reference<T> convertManagerWithCache(Holder.Reference<T> ref) {
		RegistryCache cache = get(DynamicRegistryManagerHolder.getManager());
		
		@SuppressWarnings("unchecked")
		Registry<T> registry = (Registry<T>) cache.getRegistry(ref.key().registry()).orElse(null);
		if (registry == null)
			return null;
		
		if (IS_1_21_2_PLUS)
			return registry.get(ref.key().identifier()).orElse(null);
		else
			return ((Optional<Holder.Reference<T>>) Registry_getEntry.get().invoke(registry, ref.key().identifier())).orElse(null);
	}
	
	private static final Supplier<Reflection.MethodInvoker> Registry_getKey =
			Reflection.getOptionalMethod(Registry.class, "method_30517", MethodType.methodType(ResourceKey.class));
	private static final LoadingCache<Registry<?>, Boolean> staticRegistries = CacheBuilder.newBuilder().build(
			CacheLoader.from(registry -> BuiltInRegistries.REGISTRY.getValue(registry.key().identifier()) != null));
	public static boolean isRegistryStatic(Registry<?> registry) {
		return staticRegistries.getUnchecked(registry);
	}
	
	private final WeakReference<RegistryAccess> registryManagerRef;
	@SuppressWarnings("unused") // Holds a strong reference
	private final RegistryAccess registryManager;
	private final Map<Identifier, Optional<? extends Registry<?>>> cache;
	
	public RegistryCache(RegistryAccess registryManager, boolean stronglyRef) {
		this.registryManagerRef = new WeakReference<>(registryManager);
		this.registryManager = (stronglyRef ? registryManager : null);
		this.cache = new ConcurrentHashMap<>();
	}
	public RegistryCache(RegistryAccess registryManager) {
		this(registryManager, true);
	}
	
	private static final Supplier<Reflection.MethodInvoker> DynamicRegistryManager_getOptional =
			Reflection.getOptionalMethod(RegistryAccess.class, "method_33310", MethodType.methodType(Optional.class, ResourceKey.class));
	public Optional<? extends Registry<?>> getRegistry(Identifier registryKey) {
		return cache.computeIfAbsent(registryKey, id -> {
			RegistryAccess registryManager = registryManagerRef.get();
			if (registryManager == null)
				return Optional.empty();
		return registryManager.lookup(ResourceKey.createRegistryKey(id));
		});
	}
	
	@SuppressWarnings("unchecked")
	public <T> Optional<? extends Registry<T>> getRegistry(ResourceKey<Registry<T>> registryKey) {
		return (Optional<? extends Registry<T>>) getRegistry(registryKey.identifier());
	}
	
}
