/*
 * Framework by TerminalMC
 *
 * To the extent possible under law, the person who associated CC0 with
 * Framework has waived all copyright and related or neighboring rights
 * to Framework.
 *
 * You should have received a copy of the CC0 legalcode along with this
 * work. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package dev.terminalmc.framework.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import dev.terminalmc.framework.Framework;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class Config {
    private static final Path DIR_PATH = Path.of("config");
    private static final String FILE_NAME = Framework.MOD_ID + ".json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Options

    public final Options options = new Options();

    public static class Options {
        
        // First category
        
        public static final boolean defaultBooleanOption = true;
        public boolean booleanOption = defaultBooleanOption;

        public static final int defaultIntOption = 7;
        public int intOption = defaultIntOption;

        public static final double defaultDoubleOption = 4.5;
        public double doubleOption = defaultDoubleOption;

        public static final String defaultLenientStringOption = "example";
        public String lenientStringOption = defaultLenientStringOption;

        public static final List<String> strictStringOptionValues = List.of("One", "Two", "Three");
        public static final String defaultStrictStringOption = strictStringOptionValues.getFirst();
        public String strictStringOption = defaultStrictStringOption;

        public static final TriState defaultEnumOption = TriState.Value1;
        public TriState enumOption = defaultEnumOption;

        // Second category
        
        public static final List<String> defaultStringListOption = List.of("One");
        public static final String defaultStringListOptionValue = "One";
        public List<String> stringListOption = defaultStringListOption;

        // Third Category
        
        public static final int defaultRgbOption = 16777215;
        public int rgbOption = defaultRgbOption;

        public static final int defaultArgbOption = -1;
        public int argbOption = defaultArgbOption;

        // Cloth Config only
        
        public static final int defaultKeyExample = InputConstants.KEY_J;
        public int keyOption = defaultKeyExample;

        // YACL only
        // Fourth category
        
        public static final String defaultItemOption = BuiltInRegistries.ITEM.getKey(Items.STONE).toString();
        public String itemOption = defaultItemOption;
        
        public static final List<CustomObject> defaultCustomObjectListExample = new ArrayList<>(List.of(
                new CustomObject("one", 1),
                new CustomObject("two", 2)
        ));
        public List<CustomObject> customObjectListExample = defaultCustomObjectListExample;
    }

    public enum TriState {
        Value1,
        Value2,
        Value3
    }
    
    public static class CustomObject {
        public static final String defaultName = "";
        public String name = defaultName;
        
        public static final int defaultSize = 0;
        public int size = defaultSize;

        public CustomObject() {
        }

        public CustomObject(String name, int size) {
            this.name = name;
            this.size = size;
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
        }
        if (config == null) {
            config = new Config();
        }
        return config;
    }

    private static @Nullable Config load(Path file, Gson gson) {
        try (FileReader reader = new FileReader(file.toFile())) {
            return gson.fromJson(reader, Config.class);
        } catch (Exception e) {
            // Catch Exception as errors in deserialization may not fall under
            // IOException or JsonParseException, but should not crash the game.
            Framework.LOG.error("Unable to load config.", e);
            return null;
        }
    }

    public static void save() {
        if (instance == null) return;
        instance.cleanup();
        try {
            if (!Files.isDirectory(DIR_PATH)) Files.createDirectories(DIR_PATH);
            Path file = DIR_PATH.resolve(FILE_NAME);
            Path tempFile = file.resolveSibling(file.getFileName() + ".tmp");

            try (FileWriter writer = new FileWriter(tempFile.toFile())) {
                writer.write(GSON.toJson(instance));
            } catch (IOException e) {
                throw new IOException(e);
            }
            Files.move(tempFile, file, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            Framework.onConfigSaved(instance);
        } catch (IOException e) {
            Framework.LOG.error("Unable to save config.", e);
        }
    }
}
