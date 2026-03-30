/*
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

package dev.terminalmc.moremousetweaks.compat.itemlocks;

import dev.terminalmc.moremousetweaks.MoreMouseTweaks;
import dev.terminalmc.moremousetweaks.util.inject.ISlot;
import net.minecraft.world.inventory.Slot;

public class ItemLocksWrapper {

    private static boolean hasFailed = false;

    /**
     * Wraps {@link ItemLocksCompat#isLocked} to catch errors if the ItemLocks mod is not loaded or
     * the expected method is not available.
     *
     * @param slot the slot to check.
     * @return {@code true} if the slot is valid, locked, and the bypass is not active.
     */
    public static boolean isLocked(Slot slot) {
        if (hasFailed)
            return false;
        try {
            return ItemLocksCompat.isLocked(slot);
        } catch (NoClassDefFoundError | NoSuchMethodError ignored) {
            hasFailed = true;
            MoreMouseTweaks.LOG.info("ItemLocks not found: compat is now disabled");
            return false;
        } catch (Exception e) {
            hasFailed = true;
            MoreMouseTweaks.LOG.error(
                    "ItemLocks threw an unexpected error when checking slot {}: compat is now disabled",
                    ((ISlot) slot).mmt$getIdInContainer()
            );
            MoreMouseTweaks.LOG.error(e.getLocalizedMessage());
            return false;
        }
    }
}
