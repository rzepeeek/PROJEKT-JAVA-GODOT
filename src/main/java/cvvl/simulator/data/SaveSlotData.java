package cvvl.simulator.data;

import cvvl.simulator.GameState;

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
    }

    public void applyTo(GameState state) {
        state.money = money;
        state.reputation = reputation;
        state.ticketsIssued = ticketsIssued;
        state.day = day;
        state.hour = hour;
        state.minute = minute;
        state.difficulty = difficulty;
    }

    public String formatSavedAt() {
        if (empty || savedAtEpochMs <= 0L) {
            return "Brak zapisu";
        }
        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm");
        return fmt.format(new java.util.Date(savedAtEpochMs));
    }

    public String formatSummary() {
        if (empty) {
            return "Pusty slot";
        }
        return String.format("Dzień %d | %02d:%02d | %d zł | %d mandatów", day, hour, minute, money, ticketsIssued);
    }
}
