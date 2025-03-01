/*
 * Copyright 2025 TerminalMC
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
import dev.terminalmc.moremousetweaks.inventory.InventoryHelper;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import yalter.mousetweaks.handlers.GuiContainerHandler;
import yalter.mousetweaks.mixin.AbstractContainerScreenAccessor;

import java.util.List;

@Mixin(GuiContainerHandler.class)
public class MixinGuiContainerHandler {
    @Shadow
    public List<Slot> getSlots() { return null; }

    /**
     * Wraps an implementation of {@link yalter.mousetweaks.IGuiScreenHandler}
     * to allow CTRL+LMB clicking to quick-move all matching slots and ALT+LMB
     * clicking to drop the slot, in addition to the existing SHIFT+LMB to
     * quick-move the slot and plain LMB to pick up the slot.
     *
     * @see MixinIMTModGuiContainer3ExHandler
     */
    @WrapOperation(
            method = "clickSlot",
            at = @At(
                    value = "INVOKE",
                    target = "Lyalter/mousetweaks/mixin/AbstractContainerScreenAccessor;mousetweaks$invokeSlotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ClickType;)V"
            )
    )
    private void wrapSlotClicked(AbstractContainerScreenAccessor instance, Slot slot, int index,
                                 int button, ClickType clickType, Operation<Void> original) {
        if (!InventoryHelper.handleSlotClick(slot, button, clickType,
                (s, b, c) -> original.call(instance, s, s.index, b, c), this::getSlots)) {
            original.call(instance, slot, slot.index, button, clickType);
        }
    }
}
