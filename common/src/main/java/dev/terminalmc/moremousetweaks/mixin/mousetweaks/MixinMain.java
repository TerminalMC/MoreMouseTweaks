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

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.terminalmc.moremousetweaks.util.inject.ISpecialScrollableScreen;
import dev.terminalmc.moremousetweaks.util.ScrollAction;
import dev.terminalmc.moremousetweaks.util.inject.IScrollableRecipeBook;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yalter.mousetweaks.IGuiScreenHandler;
import yalter.mousetweaks.Main;
import yalter.mousetweaks.MouseButton;

@Mixin(value = Main.class, remap = false)
public class MixinMain {
    @Shadow
    private static IGuiScreenHandler handler;
    
    @Shadow
    private static Slot oldSelectedSlot;
    
    @WrapMethod(method = "onMouseScrolled")
    private static boolean wrapOnMouseScrolled(
            Screen screen, double x, double y, double scrollDelta, Operation<Boolean> original) {
        
        ScrollAction result;
        
        if (screen instanceof ISpecialScrollableScreen) {
            result = ((ISpecialScrollableScreen)screen).mouseWheelie_onMouseScrolledSpecial(x, y, -scrollDelta);
            if (result.cancelsCustomActions()) {
                return result.cancelsAllActions();
            }
        }
        
        if (screen instanceof IScrollableRecipeBook) {
            result = ((IScrollableRecipeBook)screen).mouseWheelie_onMouseScrollRecipeBook(x, y, -scrollDelta);
            if (result.cancelsCustomActions()) {
                return result.cancelsAllActions();
            }
        }
        
        original.call(screen, x, y, scrollDelta);
        return false;
    }
    
    @WrapOperation(method = "onMouseDrag", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/InputConstants;isKeyDown(JI)Z"))
    private static boolean wrapIsKeyDown(long window, int key, Operation<Boolean> original) {
        return Screen.hasShiftDown() || Screen.hasControlDown() || Screen.hasAltDown();
    }
    
    @Inject(method = "onMouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z", ordinal = 0))
    private static void injectOnMouseClicked(Screen screen, double x, double y, MouseButton button, CallbackInfoReturnable<Boolean> cir, @Local ItemStack stackOnMouse) {
        if (stackOnMouse.isEmpty()) {
            if (Screen.hasControlDown() || Screen.hasAltDown()) {
                handler.clickSlot(oldSelectedSlot, MouseButton.LEFT, false);
            }
        }
    }
}
