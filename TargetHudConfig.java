package dev.targethud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class TargetHudConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("targethud.json");

	public static TargetHudConfig INSTANCE = new TargetHudConfig();

	public boolean enabled = true;
	public boolean showHead = true;
	public boolean showHpText = true;
	public boolean playersOnly = false;

	// позиция в долях экрана (0..1)
	public float xRatio = 0.56f;
	public float yRatio = 0.55f;

	// размер худа
	public float scale = 1.0f;

	// цвета в формате ARGB
	public int backgroundColor = 0xE6141418;
	public int barColor = 0xFF8A4DFF;
	public int textColor = 0xFFFFFFFF;

	public static void load() {
		try {
			if (Files.exists(PATH)) {
				TargetHudConfig loaded = GSON.fromJson(Files.readString(PATH), TargetHudConfig.class);
				INSTANCE = loaded != null ? loaded : new TargetHudConfig();
			} else {
				save();
			}
		} catch (Exception e) {
			INSTANCE = new TargetHudConfig();
		}
		INSTANCE.scale = Math.max(0.3f, Math.min(4.0f, INSTANCE.scale));
		INSTANCE.xRatio = Math.max(0f, Math.min(1f, INSTANCE.xRatio));
		INSTANCE.yRatio = Math.max(0f, Math.min(1f, INSTANCE.yRatio));
	}

	public static void save() {
		try {
			Files.writeString(PATH, GSON.toJson(INSTANCE));
		} catch (Exception ignored) {
		}
	}
}
