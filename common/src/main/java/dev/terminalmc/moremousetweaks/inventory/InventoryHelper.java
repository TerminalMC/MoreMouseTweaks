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

package dev.terminalmc.moremousetweaks.inventory;

import dev.terminalmc.moremousetweaks.MoreMouseTweaks;
import dev.terminalmc.moremousetweaks.compat.itemlocks.ItemLocksWrapper;
import dev.terminalmc.moremousetweaks.network.InteractionManager;
import dev.terminalmc.moremousetweaks.util.inject.ISlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import yalter.mousetweaks.MouseButton;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static dev.terminalmc.moremousetweaks.config.Config.options;

public class InventoryHelper {

    /**
     * @return {@code true} if the index of the slot in its inventory is less
     * than 9.
     */
    public static boolean isHotbarSlot(Slot slot) {
        return ((ISlot) slot).mmt$getIndexInInv() < 9;
    }

    /**
     * @return {@code true} if the index of the slot in its inventory is less
     * than 9.
     */
    public static boolean isExtraSlot(Slot slot) {
        return ((ISlot) slot).mmt$getIndexInInv() >= 40;
    }

    /**
     * Collects all slots that are in the same scope as {@code originSlot} as
     * adjusted by configuration.
     */
    public static List<Slot> collectSlots(Slot originSlot, List<Slot> slots) {
        ArrayList<Slot> slotsInScope = new ArrayList<>();
        if (Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen) {
            ContainerScreenHelper<?> screenHelper = ContainerScreenHelper.of(screen);
            // Use screen to determine scope
            Scope originScope = screenHelper.getScope(originSlot);
            if (originScope != Scope.INVALID) {
                // Collect all unlocked slots in the same scope
                for (Slot slot : slots) {
                    if (originScope == screenHelper.getScope(slot)) {
                        if (!ItemLocksWrapper.isLocked(slot)) {
                            slotsInScope.add(slot);
                        }
                    }
                }
            }
        } else {
            // Collect all unlocked slots
            for (Slot slot : slots) {
                if (!ItemLocksWrapper.isLocked(slot)) {
                    slotsInScope.add(slot);
                }
            }
        }
        return slotsInScope;
    }

    /**
     * @see dev.terminalmc.moremousetweaks.mixin.mousetweaks.MixinGuiContainerHandler
     */
    @FunctionalInterface
    public interface ClickConsumer {
        void call(Slot slot, int button, ClickType clickType);
    }

    /**
     * @return {@code true} if the click was handled.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean handleSlotClick(Slot slot, int button, ClickType clickType,
                                          ClickConsumer clickConsumer,
                                          Supplier<List<Slot>> slotSupplier) {
        // Only operate on LMB
        if (button != MouseButton.LEFT.getValue()) return false;

        // Only operate if there's not already a special vanilla operation
        if (Screen.hasShiftDown()) return false;

        // Only operate on unlocked slots
        if (ItemLocksWrapper.isLocked(slot)) return false;

        if (Screen.hasControlDown()) {
            // Quick-move or drop all matching slots
            handleControlClick(slot, clickConsumer, slotSupplier);
            return true;
        }
        else if (Screen.hasAltDown()) {
            // Drop single slot
            clickConsumer.call(slot, MouseButton.RIGHT.getValue(), ClickType.THROW);
            return true;
        }
        return false;
    }

    private static void handleControlClick(Slot slot, ClickConsumer original,
                                           Supplier<List<Slot>> slotSupplier) {
        // Quick-move or throw all matching items
        int button = Screen.hasAltDown() ? MouseButton.RIGHT.getValue() : MouseButton.LEFT.getValue();
        ClickType clickType = Screen.hasAltDown() ? ClickType.THROW : ClickType.QUICK_MOVE;
        ItemStack stack = slot.getItem().copy();

        // Operate on the original slot immediately to avoid delayed conflict
        original.call(slot, button, clickType);

        // Use interaction manager for other slots
        for (Slot slot2 : InventoryHelper.collectSlots(slot, slotSupplier.get())) {
            // Replicate check used by vanilla shift-double-click in
            // AbstractContainerScreen#mouseReleased
            if (slot2 == slot) continue;
            //noinspection DataFlowIssue
            if (!slot2.mayPickup(Minecraft.getInstance().player)) continue;
            if (slot2.container != slot.container) continue;
            if (!slot2.hasItem()) continue;
            if (!itemMatches(stack, slot2.getItem())) continue;
            // Okay to proceed
            InteractionManager.pushCallbackEvent(() -> {
                original.call(slot2, button, clickType);
                return InteractionManager.TICK_WAITER;
            });
        }
    }

    private static boolean itemMatches(ItemStack stack1, ItemStack stack2) {
        return ItemStack.isSameItemSameComponents(stack1, stack2) || (
                ItemStack.isSameItem(stack1, stack2)
                        && (
                        options().alwaysMatchByType
                                || options().typeMatchItems.contains(stack1.getItem())
                )
        );
    }
}
