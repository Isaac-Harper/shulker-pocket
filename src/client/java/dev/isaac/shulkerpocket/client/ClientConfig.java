package dev.isaac.shulkerpocket.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.isaac.shulkerpocket.ShulkerPocket;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loaded from {@code config/shulker_pocket.json}. Editable in-game via the Mod Menu config
 * screen ({@link ModMenuIntegration}) or by hand. Defaults are written out on first launch if
 * the file is missing.
 */
public final class ClientConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Slider bounds for {@link #cooldownMs}, in milliseconds. */
    public static final int COOLDOWN_MIN_MS = 50;
    public static final int COOLDOWN_MAX_MS = 1000;

    public boolean invertScroll = false;
    public int cooldownMs = 250;
    public boolean allowEmptyPosition = true;
    public boolean playSounds = true;
    /** false → hold sneak to activate; true → hold the bindable activation key instead. */
    public boolean useActivationKey = false;
    /** Show a discoverability hint line on shulker-box tooltips. */
    public boolean showTooltipHint = true;

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("shulker_pocket.json");
    }

    public static ClientConfig load() {
        Path path = configPath();
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                ClientConfig loaded = GSON.fromJson(reader, ClientConfig.class);
                if (loaded != null) return loaded;
            } catch (IOException e) {
                ShulkerPocket.LOGGER.warn("Failed to read config, using defaults", e);
            }
        }
        ClientConfig fresh = new ClientConfig();
        fresh.save(path);
        return fresh;
    }

    /** Persist to the standard config path (used by the in-game config screen). */
    public void save() {
        save(configPath());
    }

    public void save(Path path) {
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            ShulkerPocket.LOGGER.warn("Failed to write config", e);
        }
    }
}
