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

import dev.terminalmc.moremousetweaks.inventory.helper.InteractionHelper;
import dev.terminalmc.moremousetweaks.network.InteractionManager;
import dev.terminalmc.moremousetweaks.util.InputUtil;
import dev.terminalmc.moremousetweaks.util.KeyUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yalter.mousetweaks.MouseButton;

import java.util.Collection;
import java.util.List;
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
    protected RecipeBookMenu menu;

    /**
     * Quick-crafting via RMB click.
     */
    @Inject(
            method = "mouseClicked",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/recipebook/RecipeBookComponent;isOffsetNextToMainGUI()Z",
                    shift = Shift.AFTER
            )
    )
    public void mouseClicked(
            MouseButtonEvent event,
            boolean isDoubleClick,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!options().useQuickCrafting || event.button() != MouseButton.RIGHT.getValue())
            return;

        int resultSlotId = mmt$getResultSlotIndex(menu);
        if (event.hasShiftDown() && InputUtil.isMatchingSlotsKeyDown()) {
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
    public void keyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!isVisible() || minecraft.player.isSpectator())
            return;
        if (!options().useQuickCrafting || !minecraft.options.keyDrop.matches(event))
            return;
        // Drop interaction doesn't work if the cursor is carrying an item
        if (!minecraft.player.containerMenu.getCarried().isEmpty())
            return;

        // Click at the cursor position to select the recipe
        ignoreTextInput = false;
        boolean clickSuccess = recipeBookPage.mouseClicked(
                new MouseButtonEvent(
                        InputUtil.getMouseX(),
                        InputUtil.getMouseY(),
                        new MouseButtonInfo(MouseButton.LEFT.getValue(), 0)
                ),
                (width - IMAGE_WIDTH) / 2 - xOffset,
                (height - IMAGE_HEIGHT) / 2,
                IMAGE_WIDTH,
                IMAGE_HEIGHT,
                false
        );
        if (!clickSuccess)
            return;

        RecipeDisplayId recipe = recipeBookPage.getLastClickedRecipe();
        RecipeCollection collection = recipeBookPage.getLastClickedRecipeCollection();
        if (recipe == null || collection == null)
            return;

        int resultSlotId = mmt$getResultSlotIndex(menu);

        // Select the recipe
        InteractionManager.pushPacketEvent(
                new ServerboundPlaceRecipePacket(
                        menu.containerId,
                        recipe,
                        event.hasShiftDown()
                ),
                InteractionManager.TICK_WAITER
        );

        if (event.hasShiftDown() && InputUtil.isMatchingSlotsKeyDown()) {
            mmt$bulkQuickCraft(resultSlotId, true);
        } else {
            if (event.hasShiftDown()) {
                mmt$dropAll(resultSlotId);
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
        RecipeDisplayId recipe = recipeBookPage.getLastClickedRecipe();
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
                        return true;
                    }
            );
            // Shift-click or drop the result slot
            if (drop) {
                mmt$dropAll(resultSlotId);
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

        if (KeyUtil.hasShiftDown()) {
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
    private void mmt$dropAll(int resSlot) {
        int maxOps = mmt$getBiggestCraftingStackSize();
        for (int i = 0; i < maxOps; i++) {
            InteractionHelper.drop(menu.containerId, resSlot);
        }
    }

    @Unique
    private int mmt$getResultSlotIndex(RecipeBookMenu menu) {
        return switch (menu) {
            case AbstractCraftingMenu m -> m.getResultSlot().index;
            case AbstractFurnaceMenu m -> m.getResultSlot().index;
            default -> 0;
        };
    }

    @Unique
    public Collection<Slot> mmt$getInputSlots() {
        if (menu instanceof AbstractCraftingMenu m) {
            return m.getInputGridSlots();
        } else {
            return List.of();
        }
    }

    @Unique
    private int mmt$getBiggestCraftingStackSize() {
        int max = 0;
        for (Slot slot : mmt$getInputSlots()) {
            max = Math.max(max, slot.getItem().getCount());
        }
        return max;
    }
}
