package cvvl.simulator.data;

import cvvl.simulator.GameState;
import cvvl.simulator.systems.DifficultyLevel;

import java.util.List;

public class SaveSlotData {
    public boolean empty = true;
    public long savedAtEpochMs = 0L;
    public int money = 500;
    public int reputation = 50;
    public int ticketsIssued = 0;
    public int day = 1;
    public int hour = 8;
    public int minute = 0;
    public int difficulty = 1;
    public int carsInspectedToday = 0;
    public String vehiclesPayload = "";
    public boolean persistPlayerTransform = false;
    public float playerPosX = 0f;
    public float playerPosY = 2f;
    public float playerPosZ = 4f;
    public float playerRotY = 0f;
    public float playerPitch = 0f;

    public void captureFrom(GameState state) {
        empty = false;
        savedAtEpochMs = System.currentTimeMillis();
        money = state.money;
        reputation = state.reputation;
        ticketsIssued = state.ticketsIssued;
        day = state.day;
        hour = state.hour;
        minute = state.minute;
        difficulty = state.difficulty;
        carsInspectedToday = state.carsInspectedToday;
        vehiclesPayload = SavedVehicleCodec.encode(state.getPersistedVehicles());
        persistPlayerTransform = state.persistPlayerTransform;
        playerPosX = state.playerPosX;
        playerPosY = state.playerPosY;
        playerPosZ = state.playerPosZ;
        playerRotY = state.playerRotY;
        playerPitch = state.playerPitch;
    }

    public void applyTo(GameState state) {
        state.money = money;
        state.reputation = reputation;
        state.ticketsIssued = ticketsIssued;
        state.day = day;
        state.hour = hour;
        state.minute = minute;
        state.difficulty = difficulty;
        state.carsInspectedToday = carsInspectedToday;
        List<SavedVehicleData> vehicles = SavedVehicleCodec.decode(vehiclesPayload);
        state.setPersistedVehicles(vehicles);
        state.restoreFinedPlatesFromVehicles(vehicles);
        if (persistPlayerTransform) {
            state.capturePlayerTransform(playerPosX, playerPosY, playerPosZ, playerRotY, playerPitch);
        } else {
            state.clearPlayerTransform();
        }
    }

    public String formatSavedAt() {
        if (empty || savedAtEpochMs <= 0L) {
            return "Brak zapisu";
        }
        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm");
        return fmt.format(new java.util.Date(savedAtEpochMs));
    }

    public String formatDifficultyLabel() {
        return DifficultyLevel.fromId(difficulty).getLabel();
    }

    public boolean matchesDifficulty(int otherDifficulty) {
        return difficulty == otherDifficulty;
    }

    public String formatSummary() {
        if (empty) {
            return "Pusty slot";
        }
        return String.format(
                "Poziom: %s%nDzień %d | %02d:%02d%n%d zł | %d mandatów",
                formatDifficultyLabel(),
                day,
                hour,
                minute,
                money,
                ticketsIssued
        );
    }
}
