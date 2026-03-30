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
import com.mojang.blaze3d.platform.Window;
import dev.terminalmc.moremousetweaks.util.InputUtil;
import dev.terminalmc.moremousetweaks.util.KeyUtil;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import yalter.mousetweaks.IGuiScreenHandler;
import yalter.mousetweaks.Main;
import yalter.mousetweaks.MouseButton;

@Mixin(Main.class)
public abstract class MainMixin {

    @Shadow
    private static IGuiScreenHandler handler;

    @Shadow
    private static Slot oldSelectedSlot;

    /**
     * Wraps the first {@link com.mojang.blaze3d.platform.InputConstants#isKeyDown} invocation in
     * {@link Main#onMouseDrag} to allow MouseTweaks' LMB drag functionality to work when
     * MoreMouseTweaks' keybinds are pressed.
     */
    @WrapOperation(
            method = "onMouseDrag",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/platform/InputConstants;isKeyDown(Lcom/mojang/blaze3d/platform/Window;I)Z",
                    ordinal = 0
            )
    )
    private static boolean wrapIsKeyDown(Window window, int key, Operation<Boolean> original) {
        return original.call(window, key) || InputUtil.isAnyKeyDown();
    }

    /**
     * Wraps the first {@link ItemStack#isEmpty} invocation in {@link Main#onMouseClicked} to allow
     * triggering modified click operations.
     */
    @WrapOperation(
            method = "onMouseClicked",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z",
                    ordinal = 0
            )
    )
    private static boolean wrapIsEmpty(ItemStack instance, Operation<Boolean> original) {
        if (original.call(instance)) {
            if (oldSelectedSlot != null && InputUtil.isAnyKeyDown()) {
                handler.clickSlot(oldSelectedSlot, MouseButton.LEFT, KeyUtil.hasShiftDown());
            }
            return true;
        }
        return false;
    }
}
