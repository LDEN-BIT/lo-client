package dev.visualclient;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

import java.util.ArrayList;
import java.util.List;

/**
 * Module keys. They are NOT in Options > Controls: you set them in Lo Client settings > Modules.
 * Each one is stored in the Lo Client config, and starts unbound.
 */
public final class Modules {
    public record Module(String id, String label, KeyMapping key) {}

    public static final List<Module> ALL = new ArrayList<>();

    private Modules() {}

    public static void init(KeyMapping.Category category) {
        add(category, "hud", "HUD");
        add(category, "keystrokes", "Keystrokes");
        add(category, "freelook", "Freelook (hold)");
        add(category, "zoom", "Zoom (hold)");
        add(category, "fullbright", "Fullbright");
        add(category, "buddy", "Buddy");
        add(category, "waypoint", "Add waypoint");
        add(category, "editor", "HUD layout editor");
        add(category, "corner", "Next HUD corner");
        apply();
    }

    private static void add(KeyMapping.Category category, String id, String label) {
        // Created but never registered with the game's key list, so it stays out of Controls.
        ALL.add(new Module(id, label, new KeyMapping("key.loclient.module." + id,
                InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), category)));
    }

    public static KeyMapping key(String id) {
        for (Module m : ALL) if (m.id.equals(id)) return m.key;
        throw new IllegalArgumentException(id);
    }

    public static String name(String id) {
        return key(id).getTranslatedKeyMessage().getString();
    }

    /** Pushes the saved key codes into the key objects. */
    public static void apply() {
        Config c = Config.get();
        for (Module m : ALL) {
            Integer code = c.keys.get(m.id);
            m.key.setKey(code == null || code < 0
                    ? InputConstants.UNKNOWN
                    : InputConstants.Type.KEYSYM.getOrCreate(code));
        }
        KeyMapping.resetMapping();
    }

    public static void set(String id, int code) {
        Config c = Config.get();
        if (code < 0) c.keys.remove(id);
        else c.keys.put(id, code);
        Config.save();
        apply();
    }

    public static void clearAll() {
        Config.get().keys.clear();
        Config.save();
        apply();
    }
}
