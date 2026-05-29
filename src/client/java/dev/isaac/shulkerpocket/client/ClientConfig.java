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
 * Loaded from {@code config/shulker_pocket.json}. No config screen in v1 — edit the JSON.
 * Defaults are written out on first launch if the file is missing.
 */
public final class ClientConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean invertScroll = false;
    public int cooldownMs = 50;
    public boolean allowEmptyPosition = true;
    public boolean playSounds = true;
    public boolean respectVanillaSlotChange = true;

    public static ClientConfig load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("shulker_pocket.json");
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
