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

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import static dev.terminalmc.moremousetweaks.config.Config.options;
import static dev.terminalmc.moremousetweaks.inventory.InventoryHelper.isExtraSlot;
import static dev.terminalmc.moremousetweaks.inventory.InventoryHelper.isHotbarSlot;

/**
 * Provides slot scope information and interaction methods for 
 * {@link AbstractContainerScreen}s.
 */
public class ContainerScreenHelper<T extends AbstractContainerScreen<?>> {
    protected final T screen;

    protected ContainerScreenHelper(T screen) {
        this.screen = screen;
    }

    /**
     * Creates a {@link ContainerScreenHelper} for the specified 
     * {@link AbstractContainerScreen}.
     */
    @SuppressWarnings("unchecked")
    public static <T extends AbstractContainerScreen<?>> ContainerScreenHelper<T> of(T screen) {
        if (screen instanceof CreativeModeInventoryScreen) {
            // Creative inventory screen helper
            return (ContainerScreenHelper<T>) new CreativeContainerScreenHelper<>(
                    (CreativeModeInventoryScreen) screen);
        }
        // Normal inventory screen helper
        return new ContainerScreenHelper<>(screen);
    }

    /**
     * Gets the scope of the specified {@link Slot}.
     *
     * <p>Scope is a way of grouping slots together based on their location in
     * the inventory.</p>
     *
     * @param slot the slot for which to get the scope.
     * @return the scope of the slot, or {@link Scope#INVALID} if the slot is
     * not accessible.
     */
    public Scope getScope(Slot slot) {
        // If the slot is not accessible, consider the scope invalid
        if (!slot.mayPlace(ItemStack.EMPTY)) {
            return Scope.INVALID;
        }

        // Player inventory only screen
        if (screen instanceof AbstractContainerScreen<?>) {
            // Player inventory
            if (slot.container instanceof Inventory) {
                boolean mergeWithHotbar = false;

                // Extra inventory slots e.g. offhand
                if (isExtraSlot(slot)) {
                    switch (options().extraSlotScope) {
                        case HOTBAR -> mergeWithHotbar = true;
                        case EXTRA -> {
                            return Scope.PLAYER_INV_EXTRA;
                        }
                        case NONE -> {
                            return Scope.INVALID;
                        }
                    }
                }

                // Hotbar
                if (mergeWithHotbar || isHotbarSlot(slot)) {
                    switch (options().hotbarScope) {
                        case HOTBAR -> {
                            return Scope.PLAYER_INV_HOTBAR;
                        }
                        case NONE -> {
                            return Scope.INVALID;
                        }
                    }
                }

                return Scope.PLAYER_INV;
            }

            // Out of inventory e.g. 2x2 crafting grid
            else {
                return Scope.PLAYER_OTHER;
            }
        }

        // Container screen, probably with player inventory attached
        else {
            // Player inventory
            if (slot.container instanceof Inventory) {
                // Hotbar
                if (isHotbarSlot(slot)) {
                    switch (options().hotbarScope) {
                        case HOTBAR -> {
                            return Scope.PLAYER_INV_HOTBAR;
                        }
                        case NONE -> {
                            return Scope.INVALID;
                        }
                    }
                }

                return Scope.PLAYER_INV;
            }

            // Container
            else {
                return Scope.CONTAINER_INV;
            }
        }
    }
}
