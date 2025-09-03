/*
 * Copyright 2022 Siphalor
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

package dev.terminalmc.moremousetweaks.mixin.scroll;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.terminalmc.moremousetweaks.inventory.util.ScrollAction;
import dev.terminalmc.moremousetweaks.util.inject.IScrollableRecipeBook;
import dev.terminalmc.moremousetweaks.util.inject.ISpecialScrollableScreen;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Previously used a WrapMethod around {@link yalter.mousetweaks.Main#onMouseScrolled} but that
 * doesn't fire on creative inventory scroll on NeoForge due to how they implement the event.
 */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @WrapOperation(
            method = "onScroll",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/Screen;mouseScrolled(DDDD)Z"
            )
    )
    private boolean wrapMouseScroll(
            Screen screen,
            double mouseX,
            double mouseY,
            double deltaX,
            double deltaY,
            Operation<Boolean> original
    ) {
        ScrollAction result;

        // Creative inventory tab scrolling
        if (screen instanceof ISpecialScrollableScreen) {
            result = ((ISpecialScrollableScreen) screen).mmt$onMouseScrolledSpecial(
                    mouseX,
                    mouseY,
                    -deltaY
            );
            if (result.cancelsCustomActions())
                return result.cancelsAllActions();
        }

        // Recipe book tab and page scrolling
        else if (screen instanceof IScrollableRecipeBook) {
            result = ((IScrollableRecipeBook) screen).mmt$onMouseScrollRecipeBook(
                    mouseX,
                    mouseY,
                    -deltaY
            );
            if (result.cancelsCustomActions())
                return result.cancelsAllActions();
        }

        return original.call(screen, mouseX, mouseY, deltaX, deltaY);
    }
}
