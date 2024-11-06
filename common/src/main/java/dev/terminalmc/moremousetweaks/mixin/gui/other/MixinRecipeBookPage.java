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

import com.llamalad7.mixinextras.sugar.Local;
import dev.terminalmc.moremousetweaks.MoreMouseTweaks;
import dev.terminalmc.moremousetweaks.mixin.accessor.MainAccessor;
import dev.terminalmc.moremousetweaks.util.inject.IRecipeBookResults;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import yalter.mousetweaks.MouseButton;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static dev.terminalmc.moremousetweaks.config.Config.options;

/**
 * Quick-crafting helper for single recipes.
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
	private RecipeHolder<?> lastClickedRecipe;
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
                             Iterator<?> iterator, @Local RecipeButton recipeButton) {
		if (options().quickCrafting 
                && button == MouseButton.RIGHT.getValue() 
                && recipeButton.isOnlyOption()) {
            
            // Drop carried item if required
            ItemStack carried = minecraft.player.containerMenu.getCarried();
            ItemStack result = recipeButton.getRecipe().value().getResultItem(minecraft.level.registryAccess());
            if (!carried.isEmpty() && (!ItemStack.isSameItemSameComponents(carried, result) 
                    || carried.getCount() + result.getCount() > carried.getMaxStackSize())) {
                MoreMouseTweaks.LOG.warn("tf1");
                mmt$tryPlaceCarried(carried.copy());
            }
            
			lastClickedRecipe = recipeButton.getRecipe();
			lastClickedRecipeCollection = recipeButton.getCollection();
		}
	}


    @Unique
    public void mmt$tryPlaceCarried(ItemStack carried) {
        if (carried.isEmpty()) return;

        Inventory inv = minecraft.player.getInventory();
        int maxStack = inv.getMaxStackSize(carried);
        int remaining = carried.getCount();
        
        List<Integer> ignoredSlots = new ArrayList<>();
        while (remaining > 0) {
            int i = mmt$getSlotForStack(inv, carried, ignoredSlots);
            if (i == -1) {
                i = mmt$getFreeSlot(inv);
                MoreMouseTweaks.LOG.warn("finding free slot");
            }
            if (i == -1) {
                minecraft.gameMode.handleInventoryMouseClick(
                        minecraft.player.containerMenu.containerId, 
                        AbstractContainerMenu.SLOT_CLICKED_OUTSIDE, 
                        MouseButton.LEFT.getValue(), 
                        ClickType.THROW, 
                        minecraft.player);
                break;
            }

            ignoredSlots.add(i);
            remaining -= maxStack - inv.getItem(i).getCount();
            MoreMouseTweaks.LOG.warn("placing in slot {} with item {}", i, inv.getItem(i).getDisplayName().getString());
            MainAccessor.getHandler().clickSlot(
                    minecraft.player.containerMenu.getSlot(i), MouseButton.LEFT, false);
        }
    }
    
    @Unique
    public int mmt$getSlotForStack(Inventory inv, ItemStack stack, List<Integer> ignoredSlots) {
        if (!ignoredSlots.contains(inv.selected) 
                && this.mmt$hasSpace(inv, inv.getItem(inv.selected), stack)) {
            return inv.selected;
        } else if (!ignoredSlots.contains(Inventory.SLOT_OFFHAND) 
                && mmt$hasSpace(inv, inv.getItem(Inventory.SLOT_OFFHAND), stack)) {
            return Inventory.SLOT_OFFHAND;
        } else {
            for (int i = 0; i < inv.items.size(); i++) {
                if (!ignoredSlots.contains(i) && mmt$hasSpace(inv, inv.items.get(i), stack)) {
                    return i;
                }
            }
            return -1;
        }
    }

    @Unique
    private boolean mmt$hasSpace(Inventory inv, ItemStack destination, ItemStack origin) {
        return !destination.isEmpty()
                && ItemStack.isSameItemSameComponents(destination, origin)
                && destination.isStackable()
                && destination.getCount() < inv.getMaxStackSize(destination);
    }

    @Unique
    public int mmt$getFreeSlot(Inventory inv) {
        for(
                int i = 4; 
                i < inv.items.size() + 5; 
                ++i
        ) {
            if (inv.items.get(i).isEmpty()) return i;
        }
        return -1;
    }
}
