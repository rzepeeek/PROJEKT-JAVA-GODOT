package cvvl.simulator.systems;

import cvvl.simulator.GameState;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.ConfigFile;
import godot.api.DisplayServer;
import godot.api.Engine;
import godot.api.InputEventKey;
import godot.api.InputMap;
import godot.core.Error;
import godot.core.Key;
import godot.core.StringNames;
import godot.core.VariantArray;

@RegisterClass
public class SettingsManager extends godot.api.Node {
    public static SettingsManager instance;

    public static final int[] FPS_OPTIONS = {30, 60, 120, 144, 240};

    private static final String SETTINGS_PATH = "user://settings.cfg";
    private static final String SECTION = "settings";
    private static final String KEY_FPS = "target_fps";
    private static final String KEY_SENS = "mouse_sensitivity";
    private static final String KEY_SENS_PERCENT = "mouse_sensitivity_percent";
    private static final String KEY_INVERT = "invert_mouse";
    private static final String KEY_BIND_PREFIX = "bind_";
    private int fpsIndex = 1;
    private int sensitivityPercent = 35;
    private float mouseSensitivity = ANCHOR_SENSITIVITY;
    private boolean invertMouse = false;

    @RegisterFunction
    @Override
    public void _ready() {
        instance = this;
        ensureInputActionsExist();
        loadSettings();
        applySettings();
    }

    @RegisterFunction
    public void ensureInputActionsExist() {
        for (String action : InputActions.BINDABLE) {
            if (!InputMap.hasAction(StringNames.asCachedStringName(action))) {
                InputMap.addAction(StringNames.asCachedStringName(action), 0.5f);
            }
        }
    }

    @RegisterFunction
    public void loadSettings() {
        ConfigFile config = new ConfigFile();
        if (config.load(SETTINGS_PATH) != Error.OK) {
            syncFpsIndexFromValue(60);
            syncToGameState(getCurrentFps());
            return;
        }

        int fps = readInt(config, KEY_FPS, 60);
        syncFpsIndexFromValue(fps);

        if (config.hasSectionKey(SECTION, KEY_SENS_PERCENT)) {
            sensitivityPercent = clampPercent(readInt(config, KEY_SENS_PERCENT, 25));
        } else {
            float legacySens = readFloat(config, KEY_SENS, 0.01f);
            sensitivityPercent = sensitivityFromLegacy(legacySens);
        }
        applyPercentToSensitivity();
        invertMouse = readBool(config, KEY_INVERT, false);
        syncToGameState(fps);

        for (String action : InputActions.BINDABLE) {
            long keyCode = ((Number) config.getValue(SECTION, KEY_BIND_PREFIX + action, -1L)).longValue();
            if (keyCode >= 0) {
                applyBinding(action, (int) keyCode);
            }
        }
    }

    @RegisterFunction
    public void saveSettings() {
        ConfigFile config = new ConfigFile();
        config.setValue(SECTION, KEY_FPS, getCurrentFps());
        config.setValue(SECTION, KEY_SENS_PERCENT, sensitivityPercent);
        config.setValue(SECTION, KEY_SENS, mouseSensitivity);
        config.setValue(SECTION, KEY_INVERT, invertMouse);
        for (String action : InputActions.BINDABLE) {
            int code = getBindingKeycode(action);
            if (code >= 0) {
                config.setValue(SECTION, KEY_BIND_PREFIX + action, code);
            }
        }
        config.save(SETTINGS_PATH);
    }

    @RegisterFunction
    public void applySettings() {
        int fps = getCurrentFps();
        Engine.setMaxFps(fps);
        DisplayServer.windowSetVsyncMode(DisplayServer.VSyncMode.VSYNC_DISABLED);
        syncToGameState(fps);
    }

    @RegisterFunction
    public float getMouseSensitivity() {
        return mouseSensitivity;
    }

    @RegisterFunction
    public int getMouseSensitivityPercent() {
        return sensitivityPercent;
    }

    @RegisterFunction
    public void setMouseSensitivityPercent(int percent) {
        sensitivityPercent = clampPercent(percent);
        applyPercentToSensitivity();
    }

    @RegisterFunction
    public void increaseSensitivity() {
        setMouseSensitivityPercent(sensitivityPercent + 5);
    }

    @RegisterFunction
    public void decreaseSensitivity() {
        setMouseSensitivityPercent(sensitivityPercent - 5);
    }

    @RegisterFunction
    public void setMouseSensitivity(float value) {
        sensitivityPercent = sensitivityFromLegacy(value);
        applyPercentToSensitivity();
    }

    private void applyPercentToSensitivity() {
        mouseSensitivity = percentToSensitivity(sensitivityPercent);
        if (GameState.instance != null) {
            GameState.instance.mouseSensitivity = mouseSensitivity;
        }
    }

    private static int clampPercent(int percent) {
        return Math.max(1, Math.min(100, percent));
    }

    /** Stara czułość przy ~2% suwaka — teraz przypisana do 35%. */
    private static final float ANCHOR_SENSITIVITY = 0.00279f;
    private static final int ANCHOR_PERCENT = 35;
    private static final float SENS_AT_MIN_PERCENT = 0.0004f;
    private static final float SENS_AT_MAX_PERCENT = 0.012f;

    private static float percentToSensitivity(int percent) {
        int p = clampPercent(percent);
        if (p <= ANCHOR_PERCENT) {
            float t = (p - 1f) / (ANCHOR_PERCENT - 1f);
            return SENS_AT_MIN_PERCENT + t * (ANCHOR_SENSITIVITY - SENS_AT_MIN_PERCENT);
        }
        float t = (p - ANCHOR_PERCENT) / (100f - ANCHOR_PERCENT);
        return ANCHOR_SENSITIVITY + t * (SENS_AT_MAX_PERCENT - ANCHOR_SENSITIVITY);
    }

    private static int sensitivityFromLegacy(float value) {
        if (value <= SENS_AT_MIN_PERCENT) {
            return 1;
        }
        if (value >= SENS_AT_MAX_PERCENT) {
            return 100;
        }
        if (value <= ANCHOR_SENSITIVITY) {
            float t = (value - SENS_AT_MIN_PERCENT) / (ANCHOR_SENSITIVITY - SENS_AT_MIN_PERCENT);
            return clampPercent(1 + Math.round(t * (ANCHOR_PERCENT - 1)));
        }
        float t = (value - ANCHOR_SENSITIVITY) / (SENS_AT_MAX_PERCENT - ANCHOR_SENSITIVITY);
        return clampPercent(ANCHOR_PERCENT + Math.round(t * (100 - ANCHOR_PERCENT)));
    }

    @RegisterFunction
    public boolean isInvertMouse() {
        return invertMouse;
    }

    private void syncToGameState(int fps) {
        if (GameState.instance == null) {
            return;
        }
        GameState.instance.mouseSensitivity = mouseSensitivity;
        GameState.instance.invertMouse = invertMouse;
        GameState.instance.targetFps = fps;
    }

    @RegisterFunction
    public int getCurrentFps() {
        return FPS_OPTIONS[Math.max(0, Math.min(fpsIndex, FPS_OPTIONS.length - 1))];
    }

    @RegisterFunction
    public int getFpsIndex() {
        return fpsIndex;
    }

    @RegisterFunction
    public void setFpsIndex(int index) {
        fpsIndex = Math.max(0, Math.min(index, FPS_OPTIONS.length - 1));
        applySettings();
    }

    @RegisterFunction
    public void increaseFps() {
        setFpsIndex(fpsIndex + 1);
    }

    @RegisterFunction
    public void decreaseFps() {
        setFpsIndex(fpsIndex - 1);
    }

    @RegisterFunction
    public void applyBinding(String action, int physicalKeycode) {
        godot.core.StringName actionName = StringNames.asCachedStringName(action);
        InputMap.actionEraseEvents(actionName);

        InputEventKey event = new InputEventKey();
        Key key = Key.Companion.from(physicalKeycode);
        event.setPhysicalKeycode(key);
        event.setKeycode(key);
        InputMap.actionAddEvent(actionName, event);
    }

    @RegisterFunction
    public String getBindingLabel(String action) {
        godot.core.StringName actionName = StringNames.asCachedStringName(action);
        VariantArray<godot.api.InputEvent> events = InputMap.actionGetEvents(actionName);
        if (events.isEmpty()) {
            return "—";
        }
        godot.api.InputEvent event = events.get(0);
        if (event instanceof InputEventKey keyEvent) {
            return keyEvent.asTextPhysicalKeycode();
        }
        return "?";
    }

    @RegisterFunction
    public int getBindingKeycode(String action) {
        godot.core.StringName actionName = StringNames.asCachedStringName(action);
        VariantArray<godot.api.InputEvent> events = InputMap.actionGetEvents(actionName);
        if (events.isEmpty()) {
            return -1;
        }
        godot.api.InputEvent event = events.get(0);
        if (event instanceof InputEventKey keyEvent) {
            return (int) keyEvent.getPhysicalKeycode().getValue();
        }
        return -1;
    }

    private void syncFpsIndexFromValue(int fps) {
        fpsIndex = 1;
        for (int i = 0; i < FPS_OPTIONS.length; i++) {
            if (FPS_OPTIONS[i] == fps) {
                fpsIndex = i;
                return;
            }
        }
    }

    private static int readInt(ConfigFile config, String key, int defaultValue) {
        Object value = config.getValue(SECTION, key, defaultValue);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof Boolean bool) {
            return bool ? 1 : 0;
        }
        return defaultValue;
    }

    private static float readFloat(ConfigFile config, String key, float defaultValue) {
        Object value = config.getValue(SECTION, key, defaultValue);
        if (value instanceof Number number) {
            return number.floatValue();
        }
        return defaultValue;
    }

    private static boolean readBool(ConfigFile config, String key, boolean defaultValue) {
        Object value = config.getValue(SECTION, key, defaultValue);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return defaultValue;
    }
}
