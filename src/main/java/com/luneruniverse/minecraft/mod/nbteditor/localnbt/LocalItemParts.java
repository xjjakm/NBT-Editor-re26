package com.luneruniverse.minecraft.mod.nbteditor.localnbt;

import com.luneruniverse.minecraft.mod.nbteditor.NBTEditor;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.*;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.manager.NBTManagers;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix3x2fStack;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class LocalItemParts extends LocalItem {
	
	private Item item;
	private CompoundTag nbt;
	private int count;
	
	private ItemStack cachedItem;
	private CompoundTag cachedNbt;
	
	public LocalItemParts(ItemStack item) {
		this.item = item.getItem();
		CompoundTag componentsNbt = null;
		// Path 1: trySerialize with current registry manager
		Attempt<CompoundTag> nbtAttempt = NBTManagers.ITEM.trySerialize(item);
		if (nbtAttempt.isSuccessful()) {
			componentsNbt = nbtAttempt.value().map(nbt ->
					nbt.getCompoundOrEmpty("components")).orElse(null);
		}
		// Path 2: retry with default registry manager
		if (componentsNbt == null || componentsNbt.isEmpty()) {
			nbtAttempt = MVMisc.withDefaultRegistryManager(() -> NBTManagers.ITEM.trySerialize(item));
			if (nbtAttempt.isSuccessful()) {
				componentsNbt = nbtAttempt.value().map(nbt ->
						nbt.getCompoundOrEmpty("components")).orElse(null);
			}
		}
		// Path 3: use DataComponentPatch encoding directly with default registry
		if (componentsNbt == null || componentsNbt.isEmpty()) {
			try {
				CompoundTag directNbt = MVMisc.withDefaultRegistryManager(() -> NBTManagers.ITEM.getNbt(item));
				if (directNbt != null && !directNbt.isEmpty()) {
					componentsNbt = directNbt;
				}
			} catch (Exception e) {
				NBTEditor.LOGGER.warn("Could not serialize item components directly", e);
			}
		}
		this.nbt = componentsNbt;
		this.count = item.getCount();
		
		if (this.item == null)
			this.item = Items.AIR;
		
		this.cachedItem = MainUtil.copyAirable(item);
		this.cachedNbt = (this.nbt == null ? null : this.nbt.copy());
	}
	private LocalItemParts(LocalItemParts toCopy) {
		this.item = toCopy.item;
		this.nbt = (toCopy.nbt == null ? null : toCopy.nbt.copy());
		this.count = toCopy.count;
		this.cachedItem = MainUtil.copyAirable(toCopy.cachedItem);
		this.cachedNbt = (toCopy.cachedNbt == null ? null : toCopy.cachedNbt.copy());
	}
	
	@Override
	public LocalItemStack toStack() {
		return new LocalItemStack(getCachedItem());
	}
	@Override
	public LocalItemParts toParts() {
		return this;
	}
	
	private void setCachedItemCount() {
		Version.newSwitch()
				.range("1.21.0", null, () -> cachedItem.setCount(Math.min(count, cachedItem.getMaxStackSize())))
				.range(null, "1.20.6", () -> cachedItem.setCount(count))
				.run();
	}
	private ItemStack getCachedItem() {
		if (cachedItem.getItem() == item && Objects.equals(cachedNbt, nbt)) {
			setCachedItemCount();
			return cachedItem;
		}
		
		ItemStack oldCachedItem = cachedItem;
		cachedItem = new ItemStack(item, 1);
		cachedNbt = (nbt == null ? null : nbt.copy());
		try {
			NBTManagers.ITEM.setNbt(cachedItem,cachedNbt);
		} catch (Exception e) {
			NBTEditor.LOGGER.warn("Error while updating item cache", e);
			cachedItem = oldCachedItem;
		}
		setCachedItemCount();
		return cachedItem;
	}
	@Override
	public ItemStack getEditableItem() {
		throw new UnsupportedOperationException("LocalItemParts's items cannot be edited directly!");
	}
	@Override
	public ItemStack getReadableItem() {
		return getCachedItem();
	}
	
	@Override
	public boolean isEmpty() {
		return item == Items.AIR || count <= 0;
	}
	@Override
	public boolean isEmpty(Identifier id) {
		return MVRegistry.ITEM.get(id) == Items.AIR;
	}
	
	@Override
	public Component getName() {
		return MainUtil.getCustomItemNameSafely(getCachedItem());
	}
	@Override
	public void setName(Component name) {
		if (NBTManagers.COMPONENTS_EXIST) {
			if (name == null) {
				if (nbt != null) {
					nbt.remove("custom_name");
					nbt.remove("minecraft:custom_name");
				}
			} else {
				CompoundTag nbt = getOrCreateNBT();
				nbt.put(nbt.contains("minecraft:custom_name") || !nbt.contains("custom_name") ?
						"minecraft:custom_name" : "custom_name", TextInst.toMinecraft(name));
			}
		} else {
			CompoundTag nbt = getOrCreateNBT();
			CompoundTag display = nbt.getCompoundOrEmpty("display");
			if (name == null)
				display.remove("Name");
			else {
				display.putString("Name", TextInst.toJson(name));
				nbt.put("display", display);
			}
		}
	}
	@Override
	public String getDefaultName() {
		return MainUtil.getBaseItemNameSafely(getCachedItem()).getString();
	}
	
	@Override
	public Item getItemType() {
		return item;
	}
	@Override
	public Identifier getId() {
		return MVRegistry.ITEM.getId(item);
	}
	@Override
	public void setId(Identifier id) {
		item = MVRegistry.ITEM.get(id);
	}
	@Override
	public Set<Identifier> getIdOptions() {
		return MVRegistry.ITEM.getIds();
	}
	
	@Override
	public int getCount() {
		return count;
	}
	@Override
	public void setCount(int count) {
		this.count = count;
	}
	
	@Override
	public CompoundTag getNBT() {
		return nbt;
	}
	@Override
	public void setNBT(CompoundTag nbt) {
		this.nbt = nbt;
	}
	@Override
	public CompoundTag getOrCreateNBT() {
		if (nbt == null)
			nbt = new CompoundTag();
		return nbt;
	}
	
	@Override
	public void renderIcon(Matrix3x2fStack matrices, int x, int y, float tickDelta) {
		MVDrawableHelper.renderItem(matrices, 200.0F, true, getCachedItem(), x, y);
	}
	
	@Override
	public Optional<ItemStack> toItem(boolean cleanup) {
		return Optional.of(getCachedItem().copy());
	}
	@Override
	public CompoundTag serialize() {
		CompoundTag output = new CompoundTag();
		output.putString("id", getId().toString());
		output.put(NBTManagers.COMPONENTS_EXIST ? "components" : "tag", nbt);
		output.putInt("count", count);
		output.putString("type", "item");
		return output;
	}
	@Override
	public Component toHoverableText() {
		return getCachedItem().getDisplayName();
	}
	
	@Override
	public LocalItemParts copy() {
		return new LocalItemParts(this);
	}
	@Override
	public boolean equals(Object nbt) {
		if (nbt instanceof LocalItemParts item)
			return this.item == item.item && Objects.equals(this.nbt, item.nbt) && this.count == item.count;
		return super.equals(nbt);
	}
	
}
