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

package dev.terminalmc.moremousetweaks.mixin.scroll.inject;

import dev.terminalmc.moremousetweaks.inventory.util.ScrollAction;
import dev.terminalmc.moremousetweaks.util.inject.IRecipeBookWidget;
import dev.terminalmc.moremousetweaks.util.inject.IScrollableRecipeBook;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Recipe book scrolling helper for inventory screens.
 */
@Mixin(AbstractRecipeBookScreen.class)
public abstract class AbstractRecipeBookScreenMixin extends AbstractContainerScreen<RecipeBookMenu>
        implements IScrollableRecipeBook {

    @Shadow
    @Final
    private RecipeBookComponent<?> recipeBookComponent;

    public AbstractRecipeBookScreenMixin(
            InventoryMenu container,
            Inventory playerInventory,
            Component text
    ) {
        super(container, playerInventory, text);
    }

    @Override
    public ScrollAction mmt$onMouseScrollRecipeBook(
            double mouseX,
            double mouseY,
            double scrollAmount
    ) {
        return ((IRecipeBookWidget) recipeBookComponent).mmt$scrollRecipeBook(
                mouseX,
                mouseY,
                scrollAmount
        );
    }
}
