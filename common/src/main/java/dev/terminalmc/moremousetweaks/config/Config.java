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
import dev.terminalmc.moremousetweaks.MoreMouseTweaks;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;

public class Config {
    private static final Path DIR_PATH = Path.of("config");
    private static final String FILE_NAME = MoreMouseTweaks.MOD_ID + ".json";
    private static final String BACKUP_FILE_NAME = MoreMouseTweaks.MOD_ID + ".unreadable.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Options

    public final Options options = new Options();

    public static Options options() {
        return Config.get().options;
    }

    public static class Options {
        public static final int interactionRateServerDefault = 10;
        public int interactionRateServer = interactionRateServerDefault;

        public static final int interactionRateClientDefault = 1;
        public int interactionRateClient = interactionRateClientDefault;

        public static final boolean scrollCreativeTabsDefault = true;
        public boolean scrollCreativeTabs = scrollCreativeTabsDefault;
        
        public static final boolean quickCraftingDefault = true;
        public boolean quickCrafting = quickCraftingDefault;
        
        public static final QcOverflowMode qcOverflowModeDefault = QcOverflowMode.INVENTORY;
        public QcOverflowMode qcOverflowMode = qcOverflowModeDefault;

        public static final boolean matchByTypeDefault = false;
        public boolean matchByType = matchByTypeDefault;

        public static final List<String> typeMatchTagsDefault = List.of(
                "enchantable/weapon",
                "enchantable/mining",
                "enchantable/armor"
        );
        public List<String> typeMatchTags = typeMatchTagsDefault;
        public transient final HashSet<Item> typeMatchItems = new HashSet<>();
        
        // TODO allow configuring modifiers?
        public Modifier allOfKindModifier = Modifier.CTRL;
        public Modifier wholeStackModifier = Modifier.SHIFT;
    }
    
    public enum QcOverflowMode {
        NONE,
        RESULT_SLOT,
        INVENTORY;

        public String lowerName() {
            return switch(this) {
                case NONE -> "none";
                case RESULT_SLOT -> "result";
                case INVENTORY -> "inventory";
            };
        }
    }

    public enum Modifier {
        CTRL(Screen::hasControlDown),
        SHIFT(Screen::hasShiftDown),
        ALT(Screen::hasAltDown);
        
        private final Supplier<Boolean> downSupplier;

        Modifier(Supplier<Boolean> downSupplier) {
            this.downSupplier = downSupplier;
        }

        public boolean isDown() {
            return downSupplier.get();
        }
    }

    // Instance management

    private static Config instance = null;

    public static Config get() {
        if (instance == null) {
            instance = Config.load();
        }
        return instance;
    }

    public static Config getAndSave() {
        get();
        save();
        return instance;
    }

    public static Config resetAndSave() {
        instance = new Config();
        save();
        return instance;
    }

    // Cleanup

    private void cleanup() {
        // Called before config is saved
    }

    // Load and save

    public static @NotNull Config load() {
        Path file = DIR_PATH.resolve(FILE_NAME);
        Config config = null;
        if (Files.exists(file)) {
            config = load(file, GSON);
            if (config == null) {
                backup();
                MoreMouseTweaks.LOG.warn("Resetting config");
            }
        }
        return config != null ? config : new Config();
    }

    private static @Nullable Config load(Path file, Gson gson) {
        try (InputStreamReader reader = new InputStreamReader(
                new FileInputStream(file.toFile()), StandardCharsets.UTF_8)) {
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
            if (!Files.isDirectory(DIR_PATH)) Files.createDirectories(DIR_PATH);
            Path file = DIR_PATH.resolve(FILE_NAME);
            Path backupFile = file.resolveSibling(BACKUP_FILE_NAME);
            Files.move(file, backupFile, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            MoreMouseTweaks.LOG.error("Unable to copy config file", e);
        }
    }

    public static void save() {
        if (instance == null) return;
        instance.cleanup();
        try {
            if (!Files.isDirectory(DIR_PATH)) Files.createDirectories(DIR_PATH);
            Path file = DIR_PATH.resolve(FILE_NAME);
            Path tempFile = file.resolveSibling(file.getFileName() + ".tmp");
            try (OutputStreamWriter writer = new OutputStreamWriter(
                    new FileOutputStream(tempFile.toFile()), StandardCharsets.UTF_8)) {
                writer.write(GSON.toJson(instance));
            } catch (IOException e) {
                throw new IOException(e);
            }
            Files.move(tempFile, file, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            MoreMouseTweaks.onConfigSaved(instance);
        } catch (IOException e) {
            MoreMouseTweaks.LOG.error("Unable to save config", e);
        }
    }
}
