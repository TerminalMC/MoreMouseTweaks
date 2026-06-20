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

package dev.terminalmc.moremousetweaks.inventory.helper;

import dev.terminalmc.moremousetweaks.compat.itemlocks.ItemLocksCompat;
import dev.terminalmc.moremousetweaks.inventory.screen.ContainerScreenHelper;
import dev.terminalmc.moremousetweaks.inventory.util.Scope;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;

public class ScopeHelper {

    /**
     * Collects all slots that are in the same scope as {@code originSlot} as adjusted by
     * configuration.
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
                        if (!ItemLocksCompat.isLocked(slot)) {
                            slotsInScope.add(slot);
                        }
                    }
                }
            }
        } else {
            // Collect all unlocked slots
            for (Slot slot : slots) {
                if (!ItemLocksCompat.isLocked(slot)) {
                    slotsInScope.add(slot);
                }
            }
        }
        return slotsInScope;
    }
}
