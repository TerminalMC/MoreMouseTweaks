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

package dev.terminalmc.moremousetweaks.util;

import dev.terminalmc.moremousetweaks.compat.itemlocks.ItemLocksWrapper;
import dev.terminalmc.moremousetweaks.config.Config;
import dev.terminalmc.moremousetweaks.util.inject.ISlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static dev.terminalmc.moremousetweaks.config.Config.options;

public class InventoryUtil {
    public static final int INVALID_SCOPE = Integer.MAX_VALUE;

    public static boolean isHotbarSlot(Slot slot) {
        return ((ISlot) slot).mmt$getIndexInInv() < 9;
    }

    public static List<Slot> collectSlots(Slot originSlot, List<Slot> slots) {
        ArrayList<Slot> slotsInScope = new ArrayList<>();
        
        if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen)) {
            for (Slot slot : slots) {
                if (!ItemLocksWrapper.isLocked(slot)) {
                    slotsInScope.add(slot);
                }
            }
            return slotsInScope;
        }
        
        int originScope = getScope(screen, originSlot, false);
        if (originScope == InventoryUtil.INVALID_SCOPE) {
            return new ArrayList<>();
        } 
        
        for (Slot slot : slots) {
            if (originScope == getScope(screen, slot, true)) {
                if (!ItemLocksWrapper.isLocked(slot)) {
                    slotsInScope.add(slot);
                }
            }
        }
        return slotsInScope;
    }

    public static int getScope(AbstractContainerScreen<?> screen, Slot slot, boolean preferSmallerScopes) {
        if (!slot.mayPlace(ItemStack.EMPTY)) {
            return INVALID_SCOPE;
        }
        if (screen instanceof AbstractContainerScreen<?>) {
            if (slot.container instanceof Inventory) {
                if (isHotbarSlot(slot)) {
                    if (options().hotbarMode == Config.HotbarMode.SPLIT
                            || options().hotbarMode == Config.HotbarMode.NONE && preferSmallerScopes) {
                        return -1;
                    }
                } else if (((ISlot) slot).mmt$getIndexInInv() >= 40) {
                    if (options().extraSlotMode == Config.ExtraSlotMode.NONE) {
                        return -2;
                    } else if (options().extraSlotMode == Config.ExtraSlotMode.HOTBAR 
                            && (options().hotbarMode == Config.HotbarMode.SPLIT 
                            || options().hotbarMode == Config.HotbarMode.NONE && preferSmallerScopes)) {
                        return -1;
                    }
                }
                return 0;
            } else {
                return 2;
            }
        } else {
            if (slot.container instanceof Inventory) {
                if (isHotbarSlot(slot)) {
                    if (options().hotbarMode == Config.HotbarMode.SPLIT
                            || options().hotbarMode == Config.HotbarMode.NONE && preferSmallerScopes) {
                        return -1;
                    }
                }
                return 0;
            }
            return 1;
        }
    }
}
