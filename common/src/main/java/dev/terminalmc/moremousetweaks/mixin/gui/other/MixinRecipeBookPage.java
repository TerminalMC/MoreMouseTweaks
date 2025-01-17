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

package dev.terminalmc.moremousetweaks.mixin.gui.other;

import com.llamalad7.mixinextras.sugar.Local;
import dev.terminalmc.moremousetweaks.MoreMouseTweaks;
import dev.terminalmc.moremousetweaks.config.Config;
import dev.terminalmc.moremousetweaks.util.inject.IRecipeBookResults;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import yalter.mousetweaks.MouseButton;

import java.util.Iterator;

import static dev.terminalmc.moremousetweaks.config.Config.options;

/**
 * Quick-crafting helper for single recipes.
 * See {@link MixinOverlayRecipeComponent} for alternative-slot quick-crafting.
 * See {@link MixinRecipeBookComponent} for the rest of the quick-crafting code.
 */
@Mixin(RecipeBookPage.class)
public abstract class MixinRecipeBookPage implements IRecipeBookResults {
    @Shadow
    private int currentPage;
    @Shadow
    private int totalPages;
    @Shadow
    protected abstract void updateButtonsForPage();
    @Shadow
    private RecipeDisplayId lastClickedRecipe;
    @Shadow
    private RecipeCollection lastClickedRecipeCollection;
    @Shadow
    private Minecraft minecraft;

    @Override
    public void mmt$setCurrentPage(int page) {
        currentPage = page;
    }
    
    @Override
    public int mmt$getCurrentPage() {
        return currentPage;
    }

    @Override
    public int mmt$getPageCount() {
        return totalPages;
    }

    @Override
    public void mmt$refreshResultButtons() {
        updateButtonsForPage();
    }
    
    @Inject(
            method = "mouseClicked", 
            at = @At(
                    value = "JUMP", 
                    opcode = 154
            ), 
            locals = LocalCapture.CAPTURE_FAILSOFT
    )
    public void mouseClicked(double mouseX, double mouseY, int button, int areaLeft, int areaTop,
                             int areaWidth, int areaHeight, CallbackInfoReturnable<Boolean> cir,
                             @Local Iterator<?> iterator, @Local RecipeButton recipeButton) {
        if (
                options().quickCrafting 
                && button == MouseButton.RIGHT.getValue() 
                && recipeButton.isOnlyOption()
        ) {
            // Optionally prevent clicking past a full carried stack
            ItemStack carried = minecraft.player.containerMenu.getCarried();
            ItemStack result = recipeButton.getDisplayStack();
            if (
                    !options().qcOverflowMode.equals(Config.QcOverflowMode.NONE)
                    || carried.isEmpty()
                    || (
                            ItemStack.isSameItemSameComponents(carried, result)
                            && carried.getCount() + result.getCount() <= carried.getMaxStackSize()
                    )
            ) {
                // Quick-craft
                lastClickedRecipe = recipeButton.getCurrentRecipe();
                lastClickedRecipeCollection = recipeButton.getCollection();
                // Notify of result stack
                MoreMouseTweaks.resultStack = result;
            }
        }
    }
}
