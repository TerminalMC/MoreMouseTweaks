/*
 * Copyright 2022 Siphalor
 * Copyright 2026 TerminalMC
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
import dev.terminalmc.moremousetweaks.inventory.ClickHandler;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import yalter.mousetweaks.api.IMTModGuiContainer3Ex;
import yalter.mousetweaks.handlers.IMTModGuiContainer3ExHandler;

import java.util.List;

@Mixin(IMTModGuiContainer3ExHandler.class)
public abstract class IMTModGuiContainer3ExHandlerMixin {

    @Shadow
    public List<Slot> getSlots() {
        return null;
    }

    /**
     * @see GuiContainerHandlerMixin
     */
    @WrapOperation(
            method = "clickSlot",
            at = @At(
                    value = "INVOKE",
                    target = "Lyalter/mousetweaks/api/IMTModGuiContainer3Ex;MT_clickSlot(Lnet/minecraft/world/inventory/Slot;ILnet/minecraft/world/inventory/ContainerInput;)V"
            )
    )
    private void wrapSlotClicked(
            IMTModGuiContainer3Ex instance,
            Slot slot,
            int button,
            ContainerInput clickType,
            Operation<Void> original
    ) {
        if (!ClickHandler.handleSlotClick(
                slot,
                button,
                (s, b, c) -> original.call(instance, s, b, c),
                this::getSlots
        )) {
            original.call(instance, slot, button, clickType);
        }
    }
}
