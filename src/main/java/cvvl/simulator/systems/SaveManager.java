package cvvl.simulator.systems;

import cvvl.simulator.GameState;
import cvvl.simulator.data.SaveSlotData;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.ConfigFile;
import godot.api.DirAccess;
import godot.api.FileAccess;
import godot.core.Error;

@RegisterClass
public class SaveManager extends godot.api.Node {
    public static SaveManager instance;

    public static final int SLOT_COUNT = 3;
    private static final String SAVE_DIR = "user://saves";
    private static final String SECTION = "save";

    @RegisterFunction
    @Override
    public void _ready() {
        instance = this;
        ensureSaveDirectory();
    }

    @RegisterFunction
    public void ensureSaveDirectory() {
        DirAccess.open("user://").makeDirRecursive("saves");
    }

    @RegisterFunction
    public boolean hasSlot(int slot) {
        return loadSlotData(slot).empty == false;
    }

    @RegisterFunction
    public SaveSlotData loadSlotData(int slot) {
        SaveSlotData data = new SaveSlotData();
        ConfigFile config = new ConfigFile();
        if (config.load(slotPath(slot)) != Error.OK) {
            data.empty = true;
            return data;
        }
        long savedAt = ((Number) config.getValue(SECTION, "saved_at", 0L)).longValue();
        if (savedAt <= 0L) {
            data.empty = true;
            return data;
        }
        data.empty = false;
        data.savedAtEpochMs = savedAt;
        data.money = ((Number) config.getValue(SECTION, "money", 500)).intValue();
        data.reputation = ((Number) config.getValue(SECTION, "reputation", 50)).intValue();
        data.ticketsIssued = ((Number) config.getValue(SECTION, "tickets", 0)).intValue();
        data.day = ((Number) config.getValue(SECTION, "day", 1)).intValue();
        data.hour = ((Number) config.getValue(SECTION, "hour", 8)).intValue();
        data.minute = ((Number) config.getValue(SECTION, "minute", 0)).intValue();
        data.difficulty = ((Number) config.getValue(SECTION, "difficulty", 1)).intValue();
        data.carsInspectedToday = ((Number) config.getValue(SECTION, "cars_inspected", 0)).intValue();
        Object vehiclesPayload = config.getValue(SECTION, "vehicles_payload", "");
        data.vehiclesPayload = vehiclesPayload == null ? "" : vehiclesPayload.toString();
        data.persistPlayerTransform = toBool(config.getValue(SECTION, "player_saved", false));
        data.playerPosX = toFloat(config.getValue(SECTION, "player_x", 0f));
        data.playerPosY = toFloat(config.getValue(SECTION, "player_y", 2f));
        data.playerPosZ = toFloat(config.getValue(SECTION, "player_z", 4f));
        data.playerRotY = toFloat(config.getValue(SECTION, "player_rot_y", 0f));
        data.playerPitch = toFloat(config.getValue(SECTION, "player_pitch", 0f));
        return data;
    }

    @RegisterFunction
    public void saveToSlot(int slot) {
        if (GameState.instance == null) {
            return;
        }
        SaveSlotData data = new SaveSlotData();
        data.captureFrom(GameState.instance);
        writeSlot(slot, data);
    }

    @RegisterFunction
    public boolean loadFromSlot(int slot) {
        SaveSlotData data = loadSlotData(slot);
        if (data.empty || GameState.instance == null) {
            return false;
        }
        data.applyTo(GameState.instance);
        GameState.instance.markLoadedFromSave(slot);
        GameState.instance.notifyStateChanged();
        return true;
    }

    @RegisterFunction
    public void deleteSlot(int slot) {
        String path = slotPath(slot);
        if (FileAccess.fileExists(path)) {
            DirAccess.open("user://").remove("saves/slot_" + slot + ".cfg");
        }
    }

    private void writeSlot(int slot, SaveSlotData data) {
        ConfigFile config = new ConfigFile();
        config.setValue(SECTION, "saved_at", data.savedAtEpochMs);
        config.setValue(SECTION, "money", data.money);
        config.setValue(SECTION, "reputation", data.reputation);
        config.setValue(SECTION, "tickets", data.ticketsIssued);
        config.setValue(SECTION, "day", data.day);
        config.setValue(SECTION, "hour", data.hour);
        config.setValue(SECTION, "minute", data.minute);
        config.setValue(SECTION, "difficulty", data.difficulty);
        config.setValue(SECTION, "cars_inspected", data.carsInspectedToday);
        config.setValue(SECTION, "vehicles_payload", data.vehiclesPayload == null ? "" : data.vehiclesPayload);
        config.setValue(SECTION, "player_saved", data.persistPlayerTransform);
        config.setValue(SECTION, "player_x", data.playerPosX);
        config.setValue(SECTION, "player_y", data.playerPosY);
        config.setValue(SECTION, "player_z", data.playerPosZ);
        config.setValue(SECTION, "player_rot_y", data.playerRotY);
        config.setValue(SECTION, "player_pitch", data.playerPitch);
        config.save(slotPath(slot));
    }

    private static boolean toBool(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return false;
    }

    private static float toFloat(Object value) {
        if (value instanceof Number number) {
            return number.floatValue();
        }
        return 0f;
    }

    private String slotPath(int slot) {
        return SAVE_DIR + "/slot_" + slot + ".cfg";
    }
}
