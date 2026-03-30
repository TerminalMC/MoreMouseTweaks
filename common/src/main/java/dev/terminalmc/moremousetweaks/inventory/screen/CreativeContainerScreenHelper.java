/*
 * Copyright 2022 Siphalor
 * Copyright 2026 TerminalMC
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

package dev.terminalmc.moremousetweaks.inventory.screen;

import dev.terminalmc.moremousetweaks.inventory.util.Scope;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import static dev.terminalmc.moremousetweaks.config.Config.options;

/**
 * A {@link ContainerScreenHelper} for an instance of {@link CreativeModeInventoryScreen}.
 */
public class CreativeContainerScreenHelper<T extends CreativeModeInventoryScreen>
        extends ContainerScreenHelper<T> {

    public CreativeContainerScreenHelper(T screen) {
        super(screen);
    }

    @Override
    public Scope getScope(Slot slot) {
        // If full inventory is visible
        if (screen.isInventoryOpen()) {
            return super.getScope(slot);
        }
        // If only hotbar is visible
        if (slot.container instanceof Inventory) {
            if (isHotbarSlot(slot)) {
                return switch (options().hotbarScope) {
                    case HOTBAR, INVENTORY -> Scope.PLAYER_INV_HOTBAR;
                    case NONE -> Scope.INVALID;
                };
            }
        }
        return Scope.INVALID;
    }
}
