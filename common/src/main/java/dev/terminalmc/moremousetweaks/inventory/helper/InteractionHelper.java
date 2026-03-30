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

import dev.terminalmc.moremousetweaks.network.InteractionManager;
import net.minecraft.world.inventory.ContainerInput;
import yalter.mousetweaks.MouseButton;

public class InteractionHelper {

    public static void pickup(int containerId, int slotId) {
        InteractionManager.pushClickEvent(
                containerId,
                slotId,
                MouseButton.LEFT.getValue(),
                ContainerInput.PICKUP
        );
    }

    public static void quickMove(int containerId, int slotId) {
        InteractionManager.pushClickEvent(
                containerId,
                slotId,
                MouseButton.LEFT.getValue(),
                ContainerInput.QUICK_MOVE
        );
    }

    public static void drop(int containerId, int slotId) {
        InteractionManager.pushClickEvent(
                containerId,
                slotId,
                MouseButton.RIGHT.getValue(),
                ContainerInput.THROW
        );
    }
}
