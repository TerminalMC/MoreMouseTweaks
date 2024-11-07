/*
 * Copyright 2024 TerminalMC
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

package dev.terminalmc.moremousetweaks.gui.screen;

import dev.terminalmc.moremousetweaks.config.Config;
import me.shedaniel.clothconfig2.api.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Optional;

import static dev.terminalmc.moremousetweaks.config.Config.options;
import static dev.terminalmc.moremousetweaks.util.Localization.localized;

public class ClothScreenProvider {
    /**
     * Builds and returns a Cloth Config options screen.
     * @param parent the current screen.
     * @return a new options {@link Screen}.
     * @throws NoClassDefFoundError if the Cloth Config API mod is not
     * available.
     */
    static Screen getConfigScreen(Screen parent) {
        Config.Options options = options();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(localized("name"))
                .setSavingRunnable(Config::save);
        ConfigEntryBuilder eb = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(localized("option", "general"));

        general.addEntry(eb.startIntField(
                localized("option", "interactionRateServer"), 
                        options.interactionRateServer)
                .setTooltip(localized("option", "interactionRate.tooltip"))
                .setErrorSupplier(val -> {
                    if (val < 1) return Optional.of(
                            localized("option", "error.low"));
                    else if (val > 100) return Optional.of(
                            localized("option", "error.high"));
                    else return Optional.empty();
                })
                .setDefaultValue(Config.Options.interactionRateServerDefault)
                .setSaveConsumer(val -> options.interactionRateServer = val)
                .build());

        general.addEntry(eb.startIntField(
                localized("option", "interactionRateClient"), 
                        options.interactionRateClient)
                .setTooltip(localized("option", "interactionRate.tooltip"))
                .setErrorSupplier(val -> {
                    if (val < 1) return Optional.of(
                            localized("option", "error.low"));
                    else if (val > 100) return Optional.of(
                            localized("option", "error.high"));
                    else return Optional.empty();
                })
                .setDefaultValue(Config.Options.interactionRateClientDefault)
                .setSaveConsumer(val -> options.interactionRateClient = val)
                .build());

        general.addEntry(eb.startBooleanToggle(localized("option", "scrollCreativeTabs"),
                        options.scrollCreativeTabs)
                .setTooltip(localized("option", "scrollCreativeTabs.tooltip"))
                .setDefaultValue(Config.Options.scrollCreativeTabsDefault)
                .setSaveConsumer(val -> options.scrollCreativeTabs = val)
                .build());

        general.addEntry(eb.startBooleanToggle(localized("option", "quickCrafting"),
                        options.quickCrafting)
                .setTooltip(localized("option", "quickCrafting.tooltip"))
                .setDefaultValue(Config.Options.quickCraftingDefault)
                .setSaveConsumer(val -> options.quickCrafting = val)
                .build());

        general.addEntry(eb.startEnumSelector(localized("option", "qcOverflowMode"),
                        Config.QcOverflowMode.class, options.qcOverflowMode)
                .setEnumNameProvider(val -> localized("option", "qcOverflowMode." 
                        + ((Config.QcOverflowMode)val).lowerName()))
                .setTooltipSupplier(val -> Optional.of(new Component[]{
                        localized("option", "qcOverflowMode." + val.lowerName() + ".tooltip")
                }))
                .setDefaultValue(Config.Options.qcOverflowModeDefault)
                .setSaveConsumer(val -> options.qcOverflowMode = val)
                .build());

        return builder.build();
    }
}
