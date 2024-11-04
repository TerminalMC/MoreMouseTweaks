/*
 * Copyright 2020-2022 Siphalor
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

package dev.terminalmc.moremousetweaks.mixin.gui.other;

import dev.terminalmc.moremousetweaks.config.Config;
import dev.terminalmc.moremousetweaks.util.inject.IRecipeBookResults;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;

@Mixin(RecipeBookPage.class)
public abstract class MixinRecipeBookPage implements IRecipeBookResults {
	@Shadow
	private int currentPage;
	@Shadow
	private int totalPages;
	@Shadow
	protected abstract void updateButtonsForPage();
	@Shadow
	private RecipeHolder<?> lastClickedRecipe;
	@Shadow
	private RecipeCollection lastClickedRecipeCollection;
    
	@Override
	public void mouseWheelie_setCurrentPage(int page) {
		currentPage = page;
	}
    
	@Override
	public int mouseWheelie_getCurrentPage() {
		return currentPage;
	}

	@Override
	public int mouseWheelie_getPageCount() {
		return totalPages;
	}

	@Override
	public void mouseWheelie_refreshResultButtons() {
		updateButtonsForPage();
	}

	@Inject(method = "mouseClicked", at = @At(value = "JUMP", opcode = 154), locals = LocalCapture.CAPTURE_FAILSOFT)
	public void mouseClicked(double mouseX, double mouseY, int button, int areaLeft, int areaTop, int areaWidth, int areaHeight, CallbackInfoReturnable<Boolean> cir, Iterator<?> iterator, RecipeButton animatedResultButton) {
		if (Config.get().options.quickCrafting && button == 1 && animatedResultButton.isOnlyOption()) {
			lastClickedRecipe = animatedResultButton.getRecipe();
			lastClickedRecipeCollection = animatedResultButton.getCollection();
		}
	}
}
