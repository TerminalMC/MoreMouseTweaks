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

package dev.terminalmc.moremousetweaks;

import dev.terminalmc.moremousetweaks.config.Config;
import dev.terminalmc.moremousetweaks.network.InteractionManager;
import dev.terminalmc.moremousetweaks.util.ModLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.Nullable;

public class MoreMouseTweaks {

    public static final String MOD_ID = "moremousetweaks";
    public static final String MOD_NAME = "MoreMouseTweaks";
    public static final ModLogger LOG = new ModLogger(MOD_NAME);

    public static volatile int lastUpdatedSlot = -1;

    public static void init() {
        Config.getAndSave();
    }

    public static void afterConfigSaved(Config config) {
        @Nullable Minecraft mc = Minecraft.getInstance();
        Config.Options options = config.options;

        //noinspection ConstantValue
        if (mc != null) {
            setInteractionManagerTickRate(options);
            if (mc.getConnection() != null && mc.getConnection().isAcceptingMessages()) {
                // Update item tags
                updateItemTags(options);
            }
        }
    }

    public static void setInteractionManagerTickRate(Config.Options options) {
        if (Minecraft.getInstance().getSingleplayerServer() == null) {
            InteractionManager.setTickRate(options.interactionIntervalMp);
        } else {
            InteractionManager.setTickRate(options.interactionIntervalSp);
        }
    }

    public static void updateItemTags(Config.Options options) {
        options.typeMatchItemCache.clear();
        BuiltInRegistries.ITEM.getTags().forEach((pair) -> {
            if (options.typeMatchTags.contains(pair.getFirst().location().getPath())) {
                pair.getSecond().forEach((itemHolder) ->
                        options.typeMatchItemCache.add(itemHolder.value()));
            }
        });
    }
}
