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

import static dev.terminalmc.moremousetweaks.config.Config.options;

@Mixin(GuiContainerHandler.class)
public class MixinGuiContainerHandler {
    @Shadow
    public List<Slot> getSlots() { return null; }

    /**
     * Wraps an implementation of {@link yalter.mousetweaks.IGuiScreenHandler}
     * to allow CTRL+LMB clicking to quick-move all matching slots and ALT+LMB
     * clicking to drop the slot, in addition to the existing SHIFT+LMB to
     * quick-move the slot and raw LMB to pick up the slot.
     * See also {@link MixinIMTModGuiContainer3ExHandler}.
     */
    @WrapOperation(
            method = "clickSlot", 
            at = @At(
                    value = "INVOKE", 
                    target = "Lyalter/mousetweaks/mixin/AbstractContainerScreenAccessor;mousetweaks$invokeSlotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ClickType;)V"
            )
    )
    private void wrapSlotClicked(AbstractContainerScreenAccessor instance, Slot slot, int index, int button, ClickType clickType, Operation<Void> original) {
        // Only operate if LMB and not SHIFT+LMB
        if (button == MouseButton.LEFT.getValue() && !Screen.hasShiftDown()) {
            if (Screen.hasControlDown()) {
                boolean alt = Screen.hasAltDown();
                // Quick-move all matching items
                ItemStack stack = slot.getItem().copy();
                for (Slot slot2 : getSlots()) {
                    // Replicate check used by vanilla shift-double-click
                    if (slot2 == null) continue;
                    ItemStack stack2 = slot2.getItem();
                    if (
                            slot2.mayPickup(Minecraft.getInstance().player)
                            && slot2.hasItem()
                            && slot2.container == slot.container
//                            && AbstractContainerMenu.canItemQuickReplace(slot2, stack, true)
                            && (
                                    ItemStack.isSameItemSameComponents(stack2, stack) 
                                    || (
                                            ItemStack.isSameItem(stack2, stack)
                                            && (
                                                    options().matchByType
                                                    || options().typeMatchItems.contains(stack2.getItem())
                                            )
                                    )
                            )
                    ) {                        
                        original.call(instance, slot2, slot2.index, 
                                alt ? MouseButton.RIGHT.getValue() : button, 
                                alt ? ClickType.THROW : ClickType.QUICK_MOVE);
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
