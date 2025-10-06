/*
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

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;

import static dev.terminalmc.moremousetweaks.config.Config.options;

public class InputUtil {

    public static double getMouseX() {
        Minecraft mc = Minecraft.getInstance();
        return mc.mouseHandler.xpos()
                * (double) mc.getWindow().getGuiScaledWidth()
                / (double) mc.getWindow().getScreenWidth();
    }

    public static double getMouseY() {
        Minecraft mc = Minecraft.getInstance();
        return mc.mouseHandler.ypos()
                * (double) mc.getWindow().getGuiScaledHeight()
                / (double) mc.getWindow().getScreenHeight();
    }

    private static Window window() {
        return Minecraft.getInstance().getWindow();
    }

    public static boolean isAnyKeyDown() {
        return isAnyKeyDown(window());
    }

    public static boolean isAnyKeyDown(Window window) {
        return isDropKeyDown(window)
                || isMatchingSlotsKeyDown(window);
    }

    public static boolean isDropKeyDown() {
        return isDropKeyDown(window());
    }

    public static boolean isDropKeyDown(Window window) {
        return options().dropKey != -1 && InputConstants.isKeyDown(window, options().dropKey);
    }

    public static boolean isMatchingSlotsKeyDown() {
        return isMatchingSlotsKeyDown(window());
    }

    public static boolean isMatchingSlotsKeyDown(Window window) {
        return options().matchingSlotsKey != -1
                && InputConstants.isKeyDown(window, options().matchingSlotsKey);
    }
}
