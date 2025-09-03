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

package dev.terminalmc.moremousetweaks.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import dev.terminalmc.moremousetweaks.MoreMouseTweaks;
import dev.terminalmc.moremousetweaks.platform.Services;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;

public class Config {

    private static final Path CONFIG_DIR = Services.PLATFORM.getConfigDir();
    private static final String FILE_NAME = MoreMouseTweaks.MOD_ID + ".json";
    private static final String BACKUP_FILE_NAME = MoreMouseTweaks.MOD_ID + ".unreadable.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Options

    public final Options options = new Options();

    public static Options options() {
        return Config.get().options;
    }

    public static class Options {

        // General options

        public static final int INTERACTION_INTERVAL_MIN = 1;
        public static final int INTERACTION_INTERVAL_MAX = 100;
        public static Validator<Integer> interactionIntervalValidator = (val) ->
                Math.clamp(unbox(val), INTERACTION_INTERVAL_MIN, INTERACTION_INTERVAL_MAX);

        public static final int interactionIntervalMpDefault = 5;
        public int interactionIntervalMp = interactionIntervalMpDefault;

        public static final int interactionIntervalSpDefault = 1;
        public int interactionIntervalSp = interactionIntervalSpDefault;

        public enum HotbarScope {
            HOTBAR,
            INVENTORY,
            NONE
        }

        public static final HotbarScope hotbarScopeDefault = HotbarScope.HOTBAR;
        public HotbarScope hotbarScope = hotbarScopeDefault;
        public static Validator<HotbarScope> hotbarScopeValidator = (val) ->
                val != null && Arrays.stream(HotbarScope.values()).toList().contains(val)
                        ? val : hotbarScopeDefault;

        public enum ExtraSlotScope {
            EXTRA,
            HOTBAR,
            INVENTORY,
            NONE
        }

        public static final ExtraSlotScope extraSlotScopeDefault = ExtraSlotScope.EXTRA;
        public ExtraSlotScope extraSlotScope = extraSlotScopeDefault;
        public static Validator<ExtraSlotScope> extraSlotScopeValidator = (val) ->
                val != null && Arrays.stream(ExtraSlotScope.values()).toList().contains(val)
                        ? val : extraSlotScopeDefault;

        // Matching options

        public static final boolean alwaysMatchByTypeDefault = false;
        public boolean alwaysMatchByType = alwaysMatchByTypeDefault;

        public static final Supplier<List<String>> typeMatchTagsDefault = () -> List.of(
                "enchantable/weapon",
                "enchantable/mining",
                "enchantable/armor"
        );
        public List<String> typeMatchTags = typeMatchTagsDefault.get();
        public static Validator<List<String>> typeMatchTagsValidator = (val) -> val != null
                ? val : typeMatchTagsDefault.get();
        public transient final HashSet<Item> typeMatchItemCache = new HashSet<>();

        // Scrolling options

        public static final boolean scrollCreativeTabsDefault = true;
        public boolean scrollCreativeTabs = scrollCreativeTabsDefault;

        public static final boolean scrollRecipeBookTabsDefault = true;
        public boolean scrollRecipeBookTabs = scrollRecipeBookTabsDefault;

        public static final boolean scrollRecipeBookPagesDefault = true;
        public boolean scrollRecipeBookPages = scrollRecipeBookPagesDefault;

        // Quick crafting options

        public static final boolean useQuickCraftingDefault = true;
        public boolean useQuickCrafting = useQuickCraftingDefault;

        public enum QcSingleCraftMode {
            CURSOR,
            CURSOR_INVENTORY,
            INVENTORY
        }

        public static final QcSingleCraftMode qcSingleCraftModeDefault =
                QcSingleCraftMode.INVENTORY;
        public QcSingleCraftMode qcSingleCraftMode = qcSingleCraftModeDefault;
        public static Validator<QcSingleCraftMode> qcSingleCraftModeValidator = (val) ->
                val != null && Arrays.stream(QcSingleCraftMode.values()).toList().contains(val)
                        ? val : qcSingleCraftModeDefault;

        public static final boolean useQuickTradingDefault = true;
        public boolean useQuickTrading = useQuickTradingDefault;

        // Keybind options

        public static Validator<Integer> keyValidator = (val) ->
                Math.max(unbox(val), -1);

        public static final int dropKeyDefault = InputConstants.KEY_LALT;
        public int dropKey = dropKeyDefault;

        public static final int matchingSlotsKeyDefault = InputConstants.KEY_LCONTROL;
        public int matchingSlotsKey = matchingSlotsKeyDefault;
    }

    // Utils

    private static int unbox(@Nullable Integer val) {
        return val != null ? val : 0;
    }

    // Validation

    @FunctionalInterface
    public interface Validator<T> {

        @NotNull T validate(@Nullable T obj);
    }

    /**
     * Ensures that all config values are valid.
     */
    private void validate() {
        options.interactionIntervalMp =
                Options.interactionIntervalValidator.validate(options.interactionIntervalMp);
        options.interactionIntervalSp =
                Options.interactionIntervalValidator.validate(options.interactionIntervalSp);
        options.hotbarScope =
                Options.hotbarScopeValidator.validate(options.hotbarScope);
        options.extraSlotScope =
                Options.extraSlotScopeValidator.validate(options.extraSlotScope);
        options.typeMatchTags =
                Options.typeMatchTagsValidator.validate(options.typeMatchTags);
        options.qcSingleCraftMode =
                Options.qcSingleCraftModeValidator.validate(options.qcSingleCraftMode);
        options.dropKey =
                Options.keyValidator.validate(options.dropKey);
        options.matchingSlotsKey =
                Options.keyValidator.validate(options.matchingSlotsKey);
    }

    /**
     * Updates legacy config fields.
     */
    private void upgradeLegacy() {
    }

    // Instance management

    private static Config instance = null;

    public static Config get() {
        if (instance == null) {
            instance = Config.load();
        }
        return instance;
    }

    @SuppressWarnings("UnusedReturnValue")
    public static Config getAndSave() {
        get();
        save();
        return instance;
    }

    @SuppressWarnings("unused")
    public static Config resetAndSave() {
        instance = new Config();
        save();
        return instance;
    }

    // Load and save

    public static @NotNull Config load() {
        Path file = CONFIG_DIR.resolve(FILE_NAME);
        Config config = null;
        if (Files.exists(file)) {
            config = load(file, GSON);
            if (config == null) {
                backup();
                MoreMouseTweaks.LOG.warn("Resetting config");
            } else {
                config.upgradeLegacy();
            }
        }
        return config != null ? config : new Config();
    }

    @SuppressWarnings("SameParameterValue")
    private static @Nullable Config load(Path file, Gson gson) {
        try (
                InputStreamReader reader = new InputStreamReader(
                        new FileInputStream(file.toFile()),
                        StandardCharsets.UTF_8
                )
        ) {
            return gson.fromJson(reader, Config.class);
        } catch (Exception e) {
            // Catch Exception as errors in deserialization may not fall under
            // IOException or JsonParseException, but should not crash the game.
            MoreMouseTweaks.LOG.error("Unable to load config", e);
            return null;
        }
    }

    private static void backup() {
        try {
            MoreMouseTweaks.LOG.warn("Copying {} to {}", FILE_NAME, BACKUP_FILE_NAME);
            if (!Files.isDirectory(CONFIG_DIR))
                Files.createDirectories(CONFIG_DIR);
            Path file = CONFIG_DIR.resolve(FILE_NAME);
            Path backupFile = file.resolveSibling(BACKUP_FILE_NAME);
            Files.move(
                    file,
                    backupFile,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (IOException e) {
            MoreMouseTweaks.LOG.error("Unable to copy config file", e);
        }
    }

    public static void save() {
        if (instance == null)
            return;
        instance.validate();
        try {
            if (!Files.isDirectory(CONFIG_DIR))
                Files.createDirectories(CONFIG_DIR);
            Path file = CONFIG_DIR.resolve(FILE_NAME);
            Path tempFile = file.resolveSibling(file.getFileName() + ".tmp");
            try (
                    OutputStreamWriter writer = new OutputStreamWriter(
                            new FileOutputStream(tempFile.toFile()),
                            StandardCharsets.UTF_8
                    )
            ) {
                writer.write(GSON.toJson(instance));
            } catch (IOException e) {
                throw new IOException(e);
            }
            Files.move(
                    tempFile,
                    file,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
            MoreMouseTweaks.afterConfigSaved(instance);
        } catch (IOException e) {
            MoreMouseTweaks.LOG.error("Unable to save config", e);
        }
    }
}
