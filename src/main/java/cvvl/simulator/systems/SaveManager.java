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
        config.save(slotPath(slot));
    }

    private String slotPath(int slot) {
        return SAVE_DIR + "/slot_" + slot + ".cfg";
    }
}
