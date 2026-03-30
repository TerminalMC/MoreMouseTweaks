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

package dev.terminalmc.moremousetweaks.gui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.terminalmc.moremousetweaks.config.Config;
import dev.terminalmc.moremousetweaks.config.Config.Options;
import dev.terminalmc.moremousetweaks.config.Config.Options.QcSingleCraftMode;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import java.util.Optional;

import static dev.terminalmc.moremousetweaks.config.Config.options;
import static dev.terminalmc.moremousetweaks.util.Localization.localized;

public class ClothScreenProvider {

    /**
     * Builds and returns a Cloth Config options screen.
     *
     * @param parent the current screen.
     * @return a new options {@link Screen}.
     * @throws NoClassDefFoundError if the Cloth Config API mod is not available.
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
                        localized("option", "interactionIntervalMp"),
                        options.interactionIntervalMp
                )
                .setTooltip(localized("option", "interactionInterval.tooltip"))
                .setErrorSupplier(val -> {
                    if (val < Config.Options.INTERACTION_INTERVAL_MIN)
                        return Optional.of(localized("error", "low"));
                    else if (val > Config.Options.INTERACTION_INTERVAL_MAX)
                        return Optional.of(localized("error", "high"));
                    else
                        return Optional.empty();
                })
                .setDefaultValue(Config.Options.interactionIntervalMpDefault)
                .setSaveConsumer(val -> options.interactionIntervalMp = val)
                .build());

        general.addEntry(eb.startIntField(
                        localized("option", "interactionIntervalSp"),
                        options.interactionIntervalSp
                )
                .setTooltip(localized("option", "interactionInterval.tooltip"))
                .setErrorSupplier(val -> {
                    if (val < Config.Options.INTERACTION_INTERVAL_MIN)
                        return Optional.of(localized("error", "low"));
                    else if (val > Config.Options.INTERACTION_INTERVAL_MAX)
                        return Optional.of(localized("error", "high"));
                    else
                        return Optional.empty();
                })
                .setDefaultValue(Config.Options.interactionIntervalSpDefault)
                .setSaveConsumer(val -> options.interactionIntervalSp = val)
                .build());

        general.addEntry(eb.startEnumSelector(
                        localized("option", "hotbarScope"),
                        Config.Options.HotbarScope.class,
                        options.hotbarScope
                )
                .setEnumNameProvider(val -> localized("hotbarScope", val.name()))
                .setTooltipSupplier(val -> Optional.of(new Component[]{
                        localized("hotbarScope", val + ".tooltip")
                }))
                .setDefaultValue(Config.Options.hotbarScopeDefault)
                .setSaveConsumer(val -> options.hotbarScope = val)
                .build());

        general.addEntry(eb.startEnumSelector(
                        localized("option", "extraSlotScope"),
                        Config.Options.ExtraSlotScope.class,
                        options.extraSlotScope
                )
                .setEnumNameProvider(val -> localized("extraSlotScope", val.name()))
                .setTooltipSupplier(val -> Optional.of(new Component[]{
                        localized("extraSlotScope", val + ".tooltip")
                }))
                .setDefaultValue(Config.Options.extraSlotScopeDefault)
                .setSaveConsumer(val -> options.extraSlotScope = val)
                .build());

        ConfigCategory matching = builder.getOrCreateCategory(localized("option", "matching"));

        matching.addEntry(eb.startBooleanToggle(
                        localized("option", "alwaysMatchByType"),
                        options.alwaysMatchByType
                )
                .setTooltip(localized("option", "alwaysMatchByType.tooltip"))
                .setDefaultValue(Config.Options.alwaysMatchByTypeDefault)
                .setSaveConsumer(val -> options.alwaysMatchByType = val)
                .build());

        matching.addEntry(eb.startStrList(
                        localized("option", "typeMatchTags"),
                        options.typeMatchTags
                )
                .setTooltip(localized("option", "typeMatchTags.tooltip.1").append("\n")
                        .append(localized("option", "typeMatchTags.tooltip.2"))
                        .append("\n")
                        .append(localized(
                                "option",
                                "typeMatchTags.tooltip.3",
                                Component.literal("https://minecraft.wiki/w/Item_tag")
                                        .withStyle(ChatFormatting.GOLD)
                        )))
                .setDefaultValue(Config.Options.typeMatchTagsDefault.get())
                .setSaveConsumer(val -> options.typeMatchTags =
                        val.stream().map(String::strip).filter((s) -> !s.isBlank()).toList())
                .setInsertInFront(true)
                .setExpanded(!options.alwaysMatchByType)
                .build());

        ConfigCategory scrolling = builder.getOrCreateCategory(localized("option", "scrolling"));

        scrolling.addEntry(eb.startBooleanToggle(
                        localized("option", "scrollCreativeTabs"),
                        options.scrollCreativeTabs
                )
                .setDefaultValue(Config.Options.scrollCreativeTabsDefault)
                .setSaveConsumer(val -> options.scrollCreativeTabs = val)
                .build());

        scrolling.addEntry(eb.startBooleanToggle(
                        localized("option", "scrollRecipeBookTabs"),
                        options.scrollRecipeBookTabs
                )
                .setDefaultValue(Options.scrollRecipeBookTabsDefault)
                .setSaveConsumer(val -> options.scrollRecipeBookTabs = val)
                .build());

        scrolling.addEntry(eb.startBooleanToggle(
                        localized("option", "scrollRecipeBookPages"),
                        options.scrollRecipeBookPages
                )
                .setDefaultValue(Options.scrollRecipeBookPagesDefault)
                .setSaveConsumer(val -> options.scrollRecipeBookPages = val)
                .build());

        ConfigCategory quickCrafting =
                builder.getOrCreateCategory(localized("option", "quickCrafting"));

        quickCrafting.addEntry(eb.startBooleanToggle(
                        localized("option", "useQuickCrafting"),
                        options.useQuickCrafting
                )
                .setTooltip(localized("option", "useQuickCrafting.tooltip"))
                .setDefaultValue(Config.Options.useQuickCraftingDefault)
                .setSaveConsumer(val -> options.useQuickCrafting = val)
                .build());

        quickCrafting.addEntry(eb.startEnumSelector(
                        localized("option", "qcSingleCraftMode"),
                        QcSingleCraftMode.class,
                        options.qcSingleCraftMode
                )
                .setEnumNameProvider(val -> localized("qcSingleCraftMode", val.name()))
                .setTooltipSupplier(val -> Optional.of(new Component[]{
                        localized("qcSingleCraftMode", val + ".tooltip")
                }))
                .setDefaultValue(Config.Options.qcSingleCraftModeDefault)
                .setSaveConsumer(val -> options.qcSingleCraftMode = val)
                .build());

        quickCrafting.addEntry(eb.startBooleanToggle(
                        localized("option", "useQuickTrading"),
                        options.useQuickTrading
                )
                .setTooltip(localized("option", "useQuickTrading.tooltip"))
                .setDefaultValue(Config.Options.useQuickTradingDefault)
                .setSaveConsumer(val -> options.useQuickTrading = val)
                .build());

        ConfigCategory keybinds = builder.getOrCreateCategory(localized("option", "keybinds"));

        keybinds.addEntry(eb.startKeyCodeField(
                        localized("option", "dropKey"),
                        InputConstants.getKey(new KeyEvent(options.dropKey, 0, 0))
                )
                .setTooltip(localized("option", "dropKey.tooltip"))
                .setDefaultValue(InputConstants.getKey(new KeyEvent(Options.dropKeyDefault, 0, 0)))
                .setKeySaveConsumer(val -> options.dropKey = val.getValue())
                .setAllowMouse(false)
                .build());

        keybinds.addEntry(eb.startKeyCodeField(
                        localized("option", "matchingSlotsKey"),
                        InputConstants.getKey(new KeyEvent(options.matchingSlotsKey, 0, 0))
                )
                .setTooltip(localized("option", "matchingSlotsKey.tooltip"))
                .setDefaultValue(InputConstants.getKey(
                        new KeyEvent(Options.matchingSlotsKeyDefault, 0, 0)))
                .setKeySaveConsumer(val -> options.matchingSlotsKey = val.getValue())
                .setAllowMouse(false)
                .build());

        return builder.build();
    }
}
