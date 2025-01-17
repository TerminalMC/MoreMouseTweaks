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

package dev.terminalmc.moremousetweaks;

import dev.terminalmc.moremousetweaks.config.Config;
import dev.terminalmc.moremousetweaks.network.InteractionManager;
import dev.terminalmc.moremousetweaks.util.ModLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class MoreMouseTweaks {
    public static final String MOD_ID = "moremousetweaks";
    public static final String MOD_NAME = "MoreMouseTweaks";
    public static final ModLogger LOG = new ModLogger(MOD_NAME);

    public static int lastUpdatedSlot = -1;
    public static @Nullable ItemStack resultStack = null;

    public static void init() {
        Config.getAndSave();
    }

    public static void onEndTick(Minecraft mc) {
        
    }

    public static void onConfigSaved(Config config) {
        if (Minecraft.getInstance().getSingleplayerServer() == null) {
            InteractionManager.setTickRate(config.options.interactionRateServer);
        } else {
            InteractionManager.setTickRate(config.options.interactionRateClient);
        }
    }
    
    public static void updateItemTags(Config config) {
        config.options.typeMatchItems.clear();
        BuiltInRegistries.ITEM.getTags().forEach((named) -> {
            if (config.options.typeMatchTags.contains(named.key().location().getPath())) {
                named.forEach((itemHolder) -> 
                        config.options.typeMatchItems.add(itemHolder.value()));
            }
        });
    }

    public static double getMouseX() {
        Minecraft mc = Minecraft.getInstance();
        return mc.mouseHandler.xpos() * (double) mc.getWindow().getGuiScaledWidth() / (double) mc.getWindow().getScreenWidth();
    }

    public static double getMouseY() {
        Minecraft mc = Minecraft.getInstance();
        return mc.mouseHandler.ypos() * (double) mc.getWindow().getGuiScaledHeight() / (double) mc.getWindow().getScreenHeight();
    }
}
