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
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yalter.mousetweaks.MouseButton;

import java.util.Collection;
import java.util.List;

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
    protected abstract void updateCollections(boolean resetPageNumber, boolean flag);
    @Shadow
    private int height;
    @Shadow
    public abstract boolean isVisible();
    @Shadow
    private boolean ignoreTextInput;
    @Shadow
    protected Minecraft minecraft;
    @Shadow
    @Final protected RecipeBookMenu menu;
    @Shadow
    @Final public static int IMAGE_HEIGHT;
    @Shadow
    @Final public static int IMAGE_WIDTH;

    @Shadow
    protected abstract boolean isFiltering();

    @Shadow
    protected abstract boolean isCraftingSlot(Slot slot);

    /**
     * Quick-crafting via RMB click.
     */
    @Inject(
            method = "mouseClicked",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/recipebook/RecipeBookComponent;isOffsetNextToMainGUI()Z",
                    shift = At.Shift.AFTER
            )
    )
    public void mouseClicked(double mouseX, double mouseY, int mouseButton,
                             CallbackInfoReturnable<Boolean> cir) {
        if (options().quickCrafting && mouseButton == MouseButton.RIGHT.getValue()) {
            // Result is set by the call to RecipeBookPage#mouseClicked earlier
            // in the target method.
            ItemStack result = MoreMouseTweaks.resultStack;
            if (result == null || result.isEmpty()) return;

            // Quick-move if bulk crafting or overflowing to inventory, otherwise pickup
            ClickType clickType = ClickType.PICKUP;
            ItemStack carried = Minecraft.getInstance().player.containerMenu.getCarried();
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
            InteractionManager.pushClickEvent(menu.containerId, mmt$getResultSlotIndex(menu),
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
        RecipeDisplayId oldRecipeEntry = recipeBookPage.getLastClickedRecipe();
        if (this.recipeBookPage.mouseClicked(MoreMouseTweaks.getMouseX(), MoreMouseTweaks.getMouseY(),
                MouseButton.LEFT.getValue(), (this.width - IMAGE_WIDTH) / 2 - this.xOffset,
                (this.height - IMAGE_HEIGHT) / 2, IMAGE_WIDTH, IMAGE_HEIGHT)) {
            RecipeDisplayId recipeId = recipeBookPage.getLastClickedRecipe();
            RecipeCollection resultCollection = recipeBookPage.getLastClickedRecipeCollection();
            if (!resultCollection.isCraftable(recipeId)) {
                return;
            }
            int resSlot = mmt$getResultSlotIndex(menu);
            if (Screen.hasControlDown()) {
                if (
                        oldRecipeEntry != recipeId
                                || menu.slots.get(resSlot).getItem().isEmpty()
                                || mmt$canCraftMore(recipeId)
                ) {
                    InteractionManager.pushPacketEvent(
                            new ServerboundPlaceRecipePacket(menu.containerId, recipeId, true),
                            (triggerType) -> !mmt$isCraftingSlot(MoreMouseTweaks.lastUpdatedSlot));
                }
                InteractionManager.pushClickEvent(menu.containerId, resSlot,
                        MouseButton.RIGHT.getValue(), ClickType.THROW);
            }
            InteractionManager.pushCallbackEvent(() -> {
                minecraft.gameMode.handleInventoryMouseClick(menu.containerId,
                        mmt$getResultSlotIndex(menu), MouseButton.LEFT.getValue(),
                        ClickType.THROW, minecraft.player);
                updateCollections(false, isFiltering());
                return InteractionManager.TICK_WAITER;
            });
            cir.setReturnValue(true);
        }
    }

    @Unique
    private boolean mmt$canCraftMore(RecipeDisplayId recipeEntry) {
        return true;
//        return mmt$getBiggestCraftingStackSize() < stackedContents.getBiggestCraftableStack(
//                recipeId, recipeId.value().getResultItem(minecraft.level.registryAccess())
//                        .getMaxStackSize(), null);
    }

    @Unique
    private int mmt$getBiggestCraftingStackSize() {
        int cnt = 0;
        for (Slot slot : mmt$getInputSlots()) {
            cnt = Math.max(cnt, slot.getItem().getCount());
        }
        return cnt;
    }

    @Unique
    private boolean mmt$isCraftingSlot(int index) {
        if (index < 0 || index >= menu.slots.size()) return false;
        return isCraftingSlot(menu.getSlot(index));
    }

    @Unique
    private int mmt$getResultSlotIndex(RecipeBookMenu menu) {
        return switch(menu) {
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
}
