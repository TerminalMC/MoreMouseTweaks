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

import dev.terminalmc.moremousetweaks.inventory.ScrollAction;
import dev.terminalmc.moremousetweaks.util.inject.IRecipeBookResults;
import dev.terminalmc.moremousetweaks.util.inject.IRecipeBookWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

/**
 * Recipe book scrolling.
 */
@Mixin(RecipeBookComponent.class)
public abstract class MixinRecipeBookComponent implements IRecipeBookWidget {
    @Shadow
    @Final private RecipeBookPage recipeBookPage;
    @Shadow
    private int width;
    @Shadow
    private int xOffset;
    @Shadow
    @Final private List<RecipeBookTabButton> tabButtons;
    @Shadow
    private RecipeBookTabButton selectedTab;
    @Shadow
    protected abstract void updateCollections(boolean resetPageNumber);
    @Shadow
    private int height;
    @Shadow
    public abstract boolean isVisible();
    @Shadow
    protected Minecraft minecraft;

    /**
     * Recipe book page and tab scrolling.
     */
    @Override
    public ScrollAction mmt$scrollRecipeBook(double mouseX, double mouseY, double scrollAmount) {
        if (!this.isVisible())
            return ScrollAction.PASS;
        int top = (this.height - 166) / 2;
        if (mouseY < top || mouseY >= top + 166)
            return ScrollAction.PASS;
        int left = (this.width - 147) / 2 - this.xOffset;
        // Page scrolling
        if (mouseX >= left && mouseX < left + 147) {
            // Ugly approach since assigning the casted value causes a runtime mixin error
            int maxPage = ((IRecipeBookResults)recipeBookPage).mmt$getPageCount() - 1;
            ((IRecipeBookResults)recipeBookPage).mmt$setCurrentPage(Mth.clamp(
                    (int)(((IRecipeBookResults)recipeBookPage).mmt$getCurrentPage()
                            + Math.round(scrollAmount)), 0, Math.max(maxPage, 0)));
            ((IRecipeBookResults)recipeBookPage).mmt$refreshResultButtons();
            return ScrollAction.SUCCESS;
        }
        // Tab scrolling
        else if (mouseX >= left - 30 && mouseX < left) {
            int index = tabButtons.indexOf(selectedTab);
            int newIndex = Mth.clamp(
                    index + (int)(Math.round(scrollAmount)), 0, tabButtons.size() - 1);
            if (newIndex != index) {
                selectedTab.setStateTriggered(false);
                selectedTab = tabButtons.get(newIndex);
                selectedTab.setStateTriggered(true);
                updateCollections(true);
            }
            return ScrollAction.SUCCESS;
        }
        return ScrollAction.PASS;
    }
}
