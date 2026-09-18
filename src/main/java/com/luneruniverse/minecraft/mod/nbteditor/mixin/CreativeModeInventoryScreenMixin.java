package com.luneruniverse.minecraft.mod.nbteditor.mixin;

import com.luneruniverse.minecraft.mod.nbteditor.commands.get.GetLostItemCommand;
import com.luneruniverse.minecraft.mod.nbteditor.misc.MixinLink;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CreativeModeInventoryScreen.class)
public class CreativeModeInventoryScreenMixin {
	@Inject(method = "slotClicked", at = @At(value = "HEAD"), cancellable = true)
	private void onMouseClick(Slot slot, int slotId, int button, ContainerInput actionType, CallbackInfo info) {
		MixinLink.onMouseClick((CreativeModeInventoryScreen) (Object) this, slot, slotId, button, actionType, info);
	}
	@Inject(method = "slotClicked", at = @At(value = "RETURN"))
	private void onMouseClickReturn(Slot slot, int slotId, int button, ContainerInput actionType, CallbackInfo info) {
		ItemStack cursor = ((CreativeModeInventoryScreen) (Object) this).getMenu().getCarried();
		if (!cursor.isEmpty())
			GetLostItemCommand.addToHistory(cursor);
	}
	
	@Inject(method = "keyPressed", at = @At(value = "HEAD"), cancellable = true)
	private void keyPressed(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
		MixinLink.keyPressed((CreativeModeInventoryScreen) (Object) this, input.key(), input.input(), input.modifiers(), cir);
	}
}
