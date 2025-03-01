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

package dev.terminalmc.moremousetweaks.gui.screen;

import dev.terminalmc.moremousetweaks.config.Config;
import me.shedaniel.clothconfig2.api.*;
import net.minecraft.ChatFormatting;
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
                    if (val < Config.Options.interactionRateMin) return Optional.of(
                            localized("option", "error.low"));
                    else if (val > Config.Options.interactionRateMax) return Optional.of(
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
                    if (val < Config.Options.interactionRateMin) return Optional.of(
                            localized("option", "error.low"));
                    else if (val > Config.Options.interactionRateMax) return Optional.of(
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
                        Config.Options.QcOverflowMode.class, options.qcOverflowMode)
                .setEnumNameProvider(val -> localized("qcOverflowMode", val.name()))
                .setTooltipSupplier(val -> Optional.of(new Component[]{
                        localized("qcOverflowMode", val + ".tooltip")
                }))
                .setDefaultValue(Config.Options.qcOverflowModeDefault)
                .setSaveConsumer(val -> options.qcOverflowMode = val)
                .build());

        general.addEntry(eb.startEnumSelector(localized("option", "hotbarScope"),
                        Config.Options.HotbarScope.class, options.hotbarScope)
                .setEnumNameProvider(val -> localized("hotbarScope", val.name()))
                .setTooltipSupplier(val -> Optional.of(new Component[]{
                        localized("hotbarScope", val + ".tooltip")
                }))
                .setDefaultValue(Config.Options.hotbarScopeDefault)
                .setSaveConsumer(val -> options.hotbarScope = val)
                .build());

        general.addEntry(eb.startEnumSelector(localized("option", "extraSlotScope"),
                        Config.Options.ExtraSlotScope.class, options.extraSlotScope)
                .setEnumNameProvider(val -> localized("extraSlotScope", val.name()))
                .setTooltipSupplier(val -> Optional.of(new Component[]{
                        localized("extraSlotScope", val + ".tooltip")
                }))
                .setDefaultValue(Config.Options.extraSlotScopeDefault)
                .setSaveConsumer(val -> options.extraSlotScope = val)
                .build());

        general.addEntry(eb.startBooleanToggle(localized("option", "alwaysMatchByType"),
                        options.alwaysMatchByType)
                .setTooltip(localized("option", "alwaysMatchByType.tooltip"))
                .setDefaultValue(Config.Options.alwaysMatchByTypeDefault)
                .setSaveConsumer(val -> options.alwaysMatchByType = val)
                .build());

        general.addEntry(eb.startStrList(
                        localized("option", "typeMatchTags"), options.typeMatchTags)
                .setTooltip(localized("option", "typeMatchTags.tooltip",
                        Component.literal("https://minecraft.wiki/w/Tag#Item_tags")
                                .withStyle(ChatFormatting.GOLD)))
                .setDefaultValue(Config.Options.typeMatchTagsDefault)
                .setSaveConsumer(val -> options.typeMatchTags = val)
                .setInsertInFront(true)
                .setExpanded(true)
                .build());

        return builder.build();
    }
}
