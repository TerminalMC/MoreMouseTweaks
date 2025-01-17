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

package dev.terminalmc.moremousetweaks.mixin.gui.other;

import dev.terminalmc.moremousetweaks.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.OverlayRecipeComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yalter.mousetweaks.MouseButton;

import java.util.Iterator;
import java.util.List;

import static dev.terminalmc.moremousetweaks.config.Config.options;

/**
 * Quick-crafting helper for alternative recipes.
 * See {@link MixinRecipeBookPage} for regular-slot quick-crafting.
 * See {@link MixinRecipeBookComponent} for the rest of the quick-crafting code.
 */
@Mixin(OverlayRecipeComponent.class)
public class MixinOverlayRecipeComponent {
    @Shadow
    @Final private List<OverlayRecipeComponent.OverlayRecipeButton> recipeButtons;
    @Shadow
    private RecipeHolder<?> lastRecipeClicked;
    @Shadow
    private Minecraft minecraft;

    @Inject(
            method = "mouseClicked", 
            at = @At("HEAD"),
            cancellable = true
    )
    private void onMouseClicked(double mouseX, double mouseY, int button, 
                                CallbackInfoReturnable<Boolean> cir) {
        if (
                options().quickCrafting 
                && button == MouseButton.RIGHT.getValue()
        ) {
            Iterator<OverlayRecipeComponent.OverlayRecipeButton> iter = this.recipeButtons.iterator();
            OverlayRecipeComponent.OverlayRecipeButton overlayButton;
            do {
                if (!iter.hasNext()) {
                    cir.setReturnValue(false);
                    return; // Crash prevention
                }
                overlayButton = iter.next();
            } while(!overlayButton.mouseClicked(mouseX, mouseY, MouseButton.LEFT.getValue()));

            // Optionally prevent clicking past a full carried stack
            ItemStack carried = minecraft.player.containerMenu.getCarried();
            ItemStack result = overlayButton.recipe.value().getResultItem(
                    minecraft.level.registryAccess());
            if (
                    !options().qcOverflowMode.equals(Config.QcOverflowMode.NONE)
                    || carried.isEmpty()
                    || (
                            ItemStack.isSameItemSameComponents(carried, result) 
                            && carried.getCount() + result.getCount() <= carried.getMaxStackSize()
                    )
            ) {
                // Quick-craft
                this.lastRecipeClicked = overlayButton.recipe;
                cir.setReturnValue(true);
            }
        }
    }
}
