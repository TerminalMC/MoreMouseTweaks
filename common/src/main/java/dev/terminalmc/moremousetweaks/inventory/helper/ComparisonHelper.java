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

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import static dev.terminalmc.moremousetweaks.config.Config.options;

public class ComparisonHelper {

    /**
     * @return {@code true} if the two stacks are considered equal according to mod configuration.
     */
    public static boolean itemMatches(ItemStack a, ItemStack b) {
        if (ItemStack.isSameItemSameComponents(a, b))
            return true;

        if (ItemStack.isSameItem(a, b)) {
            return options().alwaysMatchByType
                    || options().typeMatchItemCache.contains(a.getItem());
        }

        return false;
    }

    /**
     * @return {@code true} if the player's inventory contains at least the specified amount of the
     * specified item, ignoring NBT data.
     */
    public static boolean playerHas(ItemStack stack, int count) {
        for (Slot slot : Minecraft.getInstance().player.containerMenu.slots) {
            if (ItemStack.isSameItem(stack, slot.getItem())) {
                count -= slot.getItem().getCount();
                if (count <= 0) {
                    return true;
                }
            }
        }
        return false;
    }
}
