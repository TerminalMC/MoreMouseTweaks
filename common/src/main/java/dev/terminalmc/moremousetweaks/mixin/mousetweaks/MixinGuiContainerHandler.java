/*
 * Copyright 2024 TerminalMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.terminalmc.moremousetweaks.mixin.mousetweaks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import yalter.mousetweaks.MouseButton;
import yalter.mousetweaks.handlers.GuiContainerHandler;
import yalter.mousetweaks.mixin.AbstractContainerScreenAccessor;

import java.util.List;

@Mixin(value = GuiContainerHandler.class, remap = false)
public class MixinGuiContainerHandler {
    @Shadow
    public List<Slot> getSlots() { return null; }
    
    @WrapOperation(method = "clickSlot", at = @At(value = "INVOKE", target = "Lyalter/mousetweaks/mixin/AbstractContainerScreenAccessor;mousetweaks$invokeSlotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ClickType;)V"))
    private void wrapSlotClicked(AbstractContainerScreenAccessor instance, Slot slot, int index, int button, ClickType clickType, Operation<Void> original) {
        if (button == MouseButton.LEFT.getValue() && !Screen.hasShiftDown()) {
            if (Screen.hasControlDown()) {
                // Quick-move all matching items
                ItemStack stack = slot.getItem().copy();
                for (Slot slot2 : getSlots()) {
                    if (slot2 != null
                            && slot2.mayPickup(Minecraft.getInstance().player)
                            && slot2.hasItem()
                            && slot2.container == slot.container
                            && AbstractContainerMenu.canItemQuickReplace(slot2, stack, true)) {
                        original.call(instance, slot2, slot2.index, button, ClickType.QUICK_MOVE);
                    }
                }
                return;
            } else if (Screen.hasAltDown()) {
                button = MouseButton.RIGHT.getValue();
                clickType = ClickType.THROW;
            }
        }
        original.call(instance, slot, index, button, clickType);
    }
}
