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

import dev.terminalmc.moremousetweaks.compat.itemlocks.ItemLocksWrapper;
import dev.terminalmc.moremousetweaks.inventory.helper.ComparisonHelper;
import dev.terminalmc.moremousetweaks.inventory.helper.ScopeHelper;
import dev.terminalmc.moremousetweaks.mixin.mousetweaks.GuiContainerHandlerMixin;
import dev.terminalmc.moremousetweaks.mixin.mousetweaks.IMTModGuiContainer3ExHandlerMixin;
import dev.terminalmc.moremousetweaks.network.InteractionManager;
import dev.terminalmc.moremousetweaks.util.InputUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import yalter.mousetweaks.MouseButton;

import java.util.List;
import java.util.function.Supplier;

/**
 * Custom click operations.
 *
 * @see GuiContainerHandlerMixin
 * @see IMTModGuiContainer3ExHandlerMixin
 */
public class ClickHandler {

    /**
     * Allows a click wrapper mixin to pass the wrapped method.
     */
    @FunctionalInterface
    public interface ClickConsumer {

        void call(Slot slot, int button, ClickType clickType);
    }

    /**
     * @return {@code true} if the click was handled.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean handleSlotClick(
            Slot slot,
            int button,
            ClickConsumer clickConsumer,
            Supplier<List<Slot>> slotSupplier
    ) {
        // Only operate on LMB
        if (button != MouseButton.LEFT.getValue())
            return false;

        // Only operate if there's not already a special vanilla operation
        if (Screen.hasShiftDown())
            return false;

        // Only operate on unlocked slots
        if (ItemLocksWrapper.isLocked(slot))
            return false;

        if (InputUtil.isMatchingSlotsKeyDown()) {
            // Quick-move or drop all matching slots
            handleMatchingSlotsClick(slot, clickConsumer, slotSupplier);
            return true;
        } else if (InputUtil.isDropKeyDown()) {
            // Drop single slot
            clickConsumer.call(slot, MouseButton.RIGHT.getValue(), ClickType.THROW);
            return true;
        }
        return false;
    }

    private static void handleMatchingSlotsClick(
            Slot slot,
            ClickConsumer original,
            Supplier<List<Slot>> slotSupplier
    ) {
        // Quick-move or throw all matching items
        int button = InputUtil.isDropKeyDown()
                ? MouseButton.RIGHT.getValue()
                : MouseButton.LEFT.getValue();
        ClickType clickType =
                InputUtil.isDropKeyDown() ? ClickType.THROW : ClickType.QUICK_MOVE;
        ItemStack stack = slot.getItem().copy();

        // Operate on the original slot immediately to avoid delayed conflict
        original.call(slot, button, clickType);

        // Use interaction manager for other slots
        for (Slot slot2 : ScopeHelper.collectSlots(slot, slotSupplier.get())) {
            // Replicate check used by vanilla shift-double-click in
            // AbstractContainerScreen#mouseReleased
            if (slot2 == slot)
                continue;
            //noinspection DataFlowIssue
            if (!slot2.mayPickup(Minecraft.getInstance().player))
                continue;
            if (slot2.container != slot.container)
                continue;
            if (!slot2.hasItem())
                continue;
            if (!ComparisonHelper.itemMatches(stack, slot2.getItem()))
                continue;
            // Okay to proceed
            InteractionManager.pushCallbackEvent(() -> {
                original.call(slot2, button, clickType);
                return InteractionManager.TICK_WAITER;
            });
        }
    }
}
