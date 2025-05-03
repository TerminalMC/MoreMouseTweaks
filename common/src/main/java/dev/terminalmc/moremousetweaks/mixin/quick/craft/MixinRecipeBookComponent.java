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

package dev.terminalmc.moremousetweaks.mixin.quick.craft;

import dev.terminalmc.moremousetweaks.MoreMouseTweaks;
import dev.terminalmc.moremousetweaks.config.Config;
import dev.terminalmc.moremousetweaks.network.InteractionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yalter.mousetweaks.MouseButton;

import static dev.terminalmc.moremousetweaks.config.Config.options;

/**
 * Quick-crafting helper for recipe book.
 */
@Mixin(RecipeBookComponent.class)
public abstract class MixinRecipeBookComponent {
    @Shadow
    @Final private RecipeBookPage recipeBookPage;
    @Shadow
    private int width;
    @Shadow
    private int xOffset;
    @Shadow
    protected abstract void updateCollections(boolean resetPageNumber);
    @Shadow
    private int height;
    @Shadow
    public abstract boolean isVisible();
    @Shadow
    private boolean ignoreTextInput;
    @Shadow
    protected Minecraft minecraft;
    @Shadow
    @Final private StackedContents stackedContents;
    @Shadow
    protected RecipeBookMenu<?, ?> menu;

    /**
     * Quick-crafting via RMB click.
     */
    @Inject(
            method = "mouseClicked",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;handlePlaceRecipe(ILnet/minecraft/world/item/crafting/RecipeHolder;Z)V",
                    shift = At.Shift.AFTER
            )
    )
    public void mouseClicked(double mouseX, double mouseY, int mouseButton,
                             CallbackInfoReturnable<Boolean> cir) {
        if (options().quickCrafting & mouseButton == MouseButton.RIGHT.getValue()) {
            int resSlot = menu.getResultSlotIndex();
            RecipeHolder<?> recipe = recipeBookPage.getLastClickedRecipe();
            if (mmt$canCraftMore(recipe)) {
                InteractionManager.clear();
                InteractionManager.setWaiter((triggerType) ->
                        MoreMouseTweaks.lastUpdatedSlot >= menu.getSize());
            }

            // Quick-move if bulk crafting or overflowing to inventory, otherwise pickup
            ClickType clickType = ClickType.PICKUP;
            ItemStack carried = minecraft.player.containerMenu.getCarried();
            ItemStack result = recipe.value().getResultItem(minecraft.level.registryAccess());
            if (
                    Screen.hasShiftDown()
                            || (
                            options().qcOverflowMode.equals(Config.Options.QcOverflowMode.INVENTORY)
                                    && !carried.isEmpty()
                                    && (
                                    !ItemStack.isSameItemSameComponents(carried, result)
                                            || carried.getCount() + result.getCount() > carried.getMaxStackSize()
                            )
                    )
            ) {
                clickType = ClickType.QUICK_MOVE;
            }
            InteractionManager.pushClickEvent(menu.containerId, resSlot,
                    MouseButton.LEFT.getValue(), clickType);
        }
    }

    /**
     * Quick-crafting via drop key press.
     */
    @Inject(
            method = "keyPressed",
            at = @At("HEAD"),
            cancellable = true
    )
    public void keyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (!options().quickCrafting || !isVisible() || minecraft.player.isSpectator()) return;
        if (!Minecraft.getInstance().options.keyDrop.matches(keyCode, scanCode)) return;

        ignoreTextInput = false;
        RecipeHolder<?> oldRecipeEntry = recipeBookPage.getLastClickedRecipe();
        if (this.recipeBookPage.mouseClicked(MoreMouseTweaks.getMouseX(), MoreMouseTweaks.getMouseY(),
                MouseButton.LEFT.getValue(), (this.width - 147) / 2 - this.xOffset,
                (this.height - 166) / 2, 147, 166)) {
            RecipeHolder<?> recipeEntry = recipeBookPage.getLastClickedRecipe();
            RecipeCollection resultCollection = recipeBookPage.getLastClickedRecipeCollection();
            if (!resultCollection.isCraftable(recipeEntry)) {
                return;
            }
            int resSlot = menu.getResultSlotIndex();
            if (MoreMouseTweaks.isMatchingSlotsKeyDown()) {
                if (
                        oldRecipeEntry != recipeEntry
                                || menu.slots.get(resSlot).getItem().isEmpty()
                                || mmt$canCraftMore(recipeEntry)
                ) {
                    InteractionManager.pushPacketEvent(
                            new ServerboundPlaceRecipePacket(menu.containerId, recipeEntry, true),
                            (triggerType) -> MoreMouseTweaks.lastUpdatedSlot >= menu.getSize());
                }
                int cnt = stackedContents.getBiggestCraftableStack(recipeEntry, recipeEntry.value()
                        .getResultItem(minecraft.level.registryAccess()).getMaxStackSize(), null);
                for (int i = 1; i < cnt; i++) {
                    InteractionManager.pushClickEvent(menu.containerId, resSlot,
                            MouseButton.RIGHT.getValue(), ClickType.THROW);
                }
            } else {
                if (oldRecipeEntry != recipeEntry || menu.slots.get(resSlot).getItem().isEmpty()) {
                    InteractionManager.pushPacketEvent(
                            new ServerboundPlaceRecipePacket(menu.containerId, recipeEntry, false),
                            (triggerType) -> MoreMouseTweaks.lastUpdatedSlot >= menu.getSize());
                }
            }
            InteractionManager.pushCallbackEvent(() -> {
                minecraft.gameMode.handleInventoryMouseClick(menu.containerId,
                        menu.getResultSlotIndex(), MouseButton.LEFT.getValue(),
                        ClickType.THROW, minecraft.player);
                updateCollections(false);
                return InteractionManager.TICK_WAITER;
            });
            cir.setReturnValue(true);
        }
    }

    @Unique
    private boolean mmt$canCraftMore(RecipeHolder<?> recipeEntry) {
        return mmt$getBiggestCraftingStackSize() < stackedContents.getBiggestCraftableStack(
                recipeEntry, recipeEntry.value().getResultItem(minecraft.level.registryAccess())
                        .getMaxStackSize(), null);
    }

    @Unique
    private int mmt$getBiggestCraftingStackSize() {
        int resSlot = menu.getResultSlotIndex();
        int cnt = 0;
        for (int i = 0; i < menu.getSize(); i++) {
            if (i == resSlot) continue;
            cnt = Math.max(cnt, menu.slots.get(i).getItem().getCount());
        }
        return cnt;
    }
}
