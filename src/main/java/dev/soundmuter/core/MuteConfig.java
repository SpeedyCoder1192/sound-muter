package dev.soundmuter.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dev.soundmuter.SoundMuter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;

public class MuteConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final int VERSION = 1;

	private final Path file;
	// only sounds that aren't at 100%
	private final Map<Identifier, Float> volumes = new HashMap<>();

	public MuteConfig(Path file) {
		this.file = file;
	}

	public float getVolume(Identifier id) {
		return volumes.getOrDefault(id, 1f);
	}

	public boolean isMuted(Identifier id) {
		return getVolume(id) <= 0f;
	}

	public void setVolume(Identifier id, float volume) {
		if (volume >= 1f) {
			volumes.remove(id);
		} else {
			volumes.put(id, Math.max(volume, 0f));
		}
	}

	public Set<Identifier> getChanged() {
		return volumes.keySet();
	}

	public void clear() {
		volumes.clear();
	}

	public void load() {
		if (!Files.exists(file)) {
			return;
		}

		try (Reader reader = Files.newBufferedReader(file)) {
			JsonObject root = GsonHelper.parse(reader);
			// "version" is ignored for now, 1 is the only format so far
			JsonObject sounds = GsonHelper.getAsJsonObject(root, "sounds", new JsonObject());
			for (var entry : sounds.entrySet()) {
				Identifier id = Identifier.tryParse(entry.getKey());
				if (id == null) {
					SoundMuter.LOGGER.warn("Skipping invalid sound id '{}' in {}", entry.getKey(), file);
					continue;
				}
				setVolume(id, GsonHelper.convertToFloat(entry.getValue(), entry.getKey()));
			}
		} catch (IOException | JsonParseException e) {
			SoundMuter.LOGGER.error("Failed to load {}", file, e);
		}
	}

	public void save() {
		Map<String, Float> sorted = new TreeMap<>();
		volumes.forEach((id, volume) -> sorted.put(id.toString(), volume));

		JsonObject sounds = new JsonObject();
		sorted.forEach(sounds::addProperty);
		JsonObject root = new JsonObject();
		root.addProperty("version", VERSION);
		root.add("sounds", sounds);

		try {
			Files.createDirectories(file.getParent());
			Files.writeString(file, GSON.toJson(root));
		} catch (IOException e) {
			SoundMuter.LOGGER.error("Failed to save {}", file, e);
		}
	}
}
