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
import dev.terminalmc.moremousetweaks.inventory.helper.InteractionHelper;
import dev.terminalmc.moremousetweaks.network.InteractionManager;
import dev.terminalmc.moremousetweaks.util.InputUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yalter.mousetweaks.MouseButton;

import java.util.concurrent.atomic.AtomicBoolean;

import static dev.terminalmc.moremousetweaks.config.Config.options;

/**
 * Quick-crafting.
 */
@Mixin(RecipeBookComponent.class)
public abstract class RecipeBookComponentMixin {

    @Shadow
    public static final int IMAGE_WIDTH = 147;

    @Shadow
    public static final int IMAGE_HEIGHT = 166;

    @Shadow
    @Final
    private RecipeBookPage recipeBookPage;

    @Shadow
    private int width;

    @Shadow
    private int xOffset;

    @Shadow
    private int height;

    @Shadow
    public abstract boolean isVisible();

    @Shadow
    private boolean ignoreTextInput;

    @Shadow
    protected Minecraft minecraft;

    @Shadow
    @Final
    private StackedContents stackedContents;

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
                    shift = Shift.AFTER
            )
    )
    public void mouseClicked(
            double mouseX,
            double mouseY,
            int mouseButton,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!options().useQuickCrafting || mouseButton != MouseButton.RIGHT.getValue())
            return;

        int resultSlotId = menu.getResultSlotIndex();
        if (Screen.hasShiftDown() && InputUtil.isMatchingSlotsKeyDown()) {
            mmt$bulkQuickCraft(resultSlotId, false);
        } else {
            mmt$quickCraft(resultSlotId);
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
    public void keyPressed(
            int keyCode,
            int scanCode,
            int modifiers,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!isVisible() || minecraft.player.isSpectator())
            return;
        if (!options().useQuickCrafting || !minecraft.options.keyDrop.matches(keyCode, scanCode))
            return;
        // Drop interaction doesn't work if the cursor is carrying an item
        if (!minecraft.player.containerMenu.getCarried().isEmpty())
            return;

        // Click at the cursor position to select the recipe
        ignoreTextInput = false;
        boolean clickSuccess = recipeBookPage.mouseClicked(
                InputUtil.getMouseX(),
                InputUtil.getMouseY(),
                MouseButton.LEFT.getValue(),
                (width - IMAGE_WIDTH) / 2 - xOffset,
                (height - IMAGE_HEIGHT) / 2,
                IMAGE_WIDTH,
                IMAGE_HEIGHT
        );
        if (!clickSuccess)
            return;

        RecipeHolder<?> recipe = recipeBookPage.getLastClickedRecipe();
        RecipeCollection collection = recipeBookPage.getLastClickedRecipeCollection();
        if (recipe == null || collection == null)
            return;

        int resultSlotId = menu.getResultSlotIndex();

        // Select the recipe
        InteractionManager.pushPacketEvent(
                new ServerboundPlaceRecipePacket(
                        menu.containerId,
                        recipe,
                        Screen.hasShiftDown()
                ),
                InteractionManager.TICK_WAITER
        );

        if (Screen.hasShiftDown() && InputUtil.isMatchingSlotsKeyDown()) {
            mmt$bulkQuickCraft(resultSlotId, true);
        } else {
            if (Screen.hasShiftDown()) {
                mmt$dropAll(recipe, resultSlotId);
            } else {
                InteractionHelper.drop(menu.containerId, resultSlotId);
            }
        }

        cir.setReturnValue(true);
    }

    /**
     * Exhaustively shift-select and shift-craft the recipe.
     * <p>
     * Figuring out how many crafts are needed, with the info we have at this point, is possible but
     * annoying. Instead, we queue the theoretical max number of interactions and abort early if the
     * recipe becomes unavailable.
     */
    @Unique
    private void mmt$bulkQuickCraft(int resultSlotId, boolean drop) {
        RecipeHolder<?> recipe = recipeBookPage.getLastClickedRecipe();
        RecipeCollection collection = recipeBookPage.getLastClickedRecipeCollection();
        if (recipe == null || collection == null)
            return;

        // Count the number of nonempty inventory slots
        int maxOps = 0;
        for (Slot slot : minecraft.player.containerMenu.slots) {
            if (slot.hasItem()) {
                maxOps++;
            }
        }
        // Normally the loop will finish naturally before the interaction manager aborts, but we
        // can't guarantee that so this allows aborting the loop as well
        AtomicBoolean abort = new AtomicBoolean(false);

        // Queue interactions
        for (int i = 0; i < maxOps; i++) {
            if (abort.get())
                break;
            // Shift-select the recipe
            InteractionManager.pushPacketEvent(
                    new ServerboundPlaceRecipePacket(
                            menu.containerId,
                            recipe,
                            true
                    ),
                    (type) -> {
                        // Check abort condition
                        if (!collection.isCraftable(recipe)) {
                            abort.set(true);
                            InteractionManager.clear();
                            return false;
                        }
                        return MoreMouseTweaks.lastUpdatedSlot >= menu.getSize();
                    }
            );
            // Shift-click or drop the result slot
            if (drop) {
                mmt$dropAll(recipe, resultSlotId);
            } else {
                InteractionHelper.quickMove(menu.containerId, resultSlotId);
            }
        }
    }

    /**
     * Pick up a single result instance or stack, or move it to the inventory.
     */
    @Unique
    private void mmt$quickCraft(int resultSlotId) {
        boolean pickupClick = false;
        boolean quickMoveClick = false;

        if (Screen.hasShiftDown()) {
            quickMoveClick = true;
        } else {
            switch (options().qcSingleCraftMode) {
                case CURSOR -> pickupClick = true;
                case CURSOR_INVENTORY -> {
                    pickupClick = true;
                    quickMoveClick = true;
                }
                case INVENTORY -> quickMoveClick = true;
            }
        }

        if (pickupClick) {
            InteractionHelper.pickup(menu.containerId, resultSlotId);
        }
        if (quickMoveClick) {
            InteractionHelper.quickMove(menu.containerId, resultSlotId);
        }
    }

    @Unique
    private void mmt$dropAll(RecipeHolder<?> recipe, int resSlot) {
        int maxOps = stackedContents.getBiggestCraftableStack(
                recipe,
                recipe.value().getResultItem(minecraft.level.registryAccess()).getMaxStackSize(),
                null
        );
        for (int i = 0; i < maxOps; i++) {
            InteractionHelper.drop(menu.containerId, resSlot);
        }
    }
}
