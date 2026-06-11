package cvvl.simulator;

import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.annotation.RegisterProperty;
import godot.annotation.RegisterSignal;
import cvvl.simulator.data.SavedVehicleData;
import cvvl.simulator.systems.DifficultyLevel;
import godot.api.Node;
import godot.core.Signal0;
import godot.core.StringNames;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RegisterClass
public class GameState extends Node {
	public static GameState instance;

	@RegisterSignal
	public final Signal0 stateChanged = new Signal0(this, StringNames.toGodotName("stateChanged"));

	@RegisterProperty
	public int money = 500;

	@RegisterProperty
	public int reputation = 50;

	@RegisterProperty
	public int ticketsIssued = 0;

	@RegisterProperty
	public int day = 1;

	@RegisterProperty
	public int hour = 8;

	@RegisterProperty
	public int minute = 0;

	@RegisterProperty
	public float mouseSensitivity = 0.003f;

	@RegisterProperty
	public boolean invertMouse = false;

	@RegisterProperty
	public boolean vsync = true;

	@RegisterProperty
	public int difficulty = 1;

	@RegisterProperty
	public int carsInspectedToday = 0;

	@RegisterProperty
	public boolean difficultyLocked = false;

	@RegisterProperty
	public int activeSaveSlot = -1;

	@RegisterProperty
	public String pendingCancelPlate = "";

	@RegisterProperty
	public int pendingCancelMoneyDelta = 0;

	@RegisterProperty
	public int pendingCancelReputationDelta = 0;

	@RegisterProperty
	public boolean pendingCancelTicketIncrement = false;

	@RegisterProperty
	public long pendingCancelDeadlineMs = 0L;

	@RegisterProperty
	public String currentVehiclePlate = "—";

	@RegisterProperty
	public String returnScenePath = ScenePaths.MAIN_MENU;

	@RegisterProperty
	public boolean reopenPauseAfterReturn = false;

	@RegisterProperty
	public int targetFps = 60;

	/** "load" lub "save" — tryb menu zapisów */
	@RegisterProperty
	public String saveMenuMode = "load";

	@RegisterProperty
	public boolean persistPlayerTransform = false;

	@RegisterProperty
	public float playerPosX = 0f;

	@RegisterProperty
	public float playerPosY = 2f;

	@RegisterProperty
	public float playerPosZ = 4f;

	@RegisterProperty
	public float playerRotY = 0f;

	@RegisterProperty
	public float playerPitch = 0f;

	private boolean loadedFromSaveThisSession = false;

	private final Set<String> finedVehiclePlates = new HashSet<>();
	private final List<SavedVehicleData> persistedVehicles = new ArrayList<>();

	@RegisterFunction
	public void prepareReturnTo(String scenePath, boolean reopenPause) {
		returnScenePath = scenePath;
		reopenPauseAfterReturn = reopenPause;
	}

	@RegisterFunction
	@Override
	public void _ready() {
		instance = this;
		notifyStateChanged();
	}

	@RegisterFunction
	public void advanceTime(float deltaMinutes) {
		int previousDay = day;
		minute += (int) deltaMinutes;
		while (minute >= 60) {
			minute -= 60;
			hour++;
		}
		if (hour >= 24) {
			hour = 0;
			day++;
		}
		if (day > previousDay) {
			onNewDay();
		}
		notifyStateChanged();
	}

	public DifficultyLevel getDifficultyLevel() {
		return DifficultyLevel.fromId(difficulty);
	}

	@RegisterFunction
	public String formatDifficulty() {
		return getDifficultyLevel().getLabel();
	}

	@RegisterFunction
	public float getGameTimeTickSeconds() {
		return getDifficultyLevel().gameTimeTickSeconds();
	}

	@RegisterFunction
	public int getMinutesPerTick() {
		return getDifficultyLevel().minutesPerTick();
	}

	@RegisterFunction
	public void resetNewGame(int difficultyLevel) {
		money = 500;
		reputation = 50;
		ticketsIssued = 0;
		day = 1;
		hour = 8;
		minute = 0;
		difficulty = difficultyLevel;
		carsInspectedToday = 0;
		difficultyLocked = false;
		activeSaveSlot = -1;
		currentVehiclePlate = "—";
		reopenPauseAfterReturn = false;
		returnScenePath = ScenePaths.MAIN_MENU;
		clearFinedVehicles();
		clearPersistedVehicles();
		clearPlayerTransform();
		loadedFromSaveThisSession = false;
		clearPendingFineCancel();
		notifyStateChanged();
	}

	@RegisterFunction
	public boolean shouldRestorePlayerFromSave() {
		return loadedFromSaveThisSession && persistPlayerTransform;
	}

	public void captureVehicleSnapshots(List<SavedVehicleData> snapshots) {
		persistedVehicles.clear();
		if (snapshots != null) {
			persistedVehicles.addAll(snapshots);
		}
	}

	public List<SavedVehicleData> getPersistedVehicles() {
		return new ArrayList<>(persistedVehicles);
	}

	public boolean hasPersistedVehicles() {
		return !persistedVehicles.isEmpty();
	}

	public void clearPersistedVehicles() {
		persistedVehicles.clear();
	}

	public void setPersistedVehicles(List<SavedVehicleData> snapshots) {
		persistedVehicles.clear();
		if (snapshots != null) {
			persistedVehicles.addAll(snapshots);
		}
	}

	public void restoreFinedPlatesFromVehicles(List<SavedVehicleData> vehicles) {
		clearFinedVehicles();
		if (vehicles == null) {
			return;
		}
		for (SavedVehicleData vehicle : vehicles) {
			if (vehicle.fineIssued) {
				markVehicleFined(vehicle.plate);
			}
		}
	}

	@RegisterFunction
	public void markLoadedFromSave(int slot) {
		difficultyLocked = true;
		activeSaveSlot = slot;
		loadedFromSaveThisSession = true;
	}

	@RegisterFunction
	public void markSavedToSlot(int slot) {
		activeSaveSlot = slot;
		difficultyLocked = true;
	}

	@RegisterFunction
	public void incrementCarsInspected() {
		carsInspectedToday++;
		notifyStateChanged();
	}

	@RegisterFunction
	public String formatDailyGoal() {
		DifficultyLevel level = getDifficultyLevel();
		if (!level.showsDailyGoal()) {
			return "";
		}
		int goal = level.dailyInspectGoal();
		return String.format("CEL: Sprawdź %d aut (%d/%d)", goal, carsInspectedToday, goal);
	}

	@RegisterFunction
	public boolean canCancelFine() {
		if (!getDifficultyLevel().allowsFineCancel()) {
			return false;
		}
		return !pendingCancelPlate.isEmpty() && System.currentTimeMillis() < pendingCancelDeadlineMs;
	}

	@RegisterFunction
	public float pendingCancelSecondsLeft() {
		if (!canCancelFine()) {
			return 0f;
		}
		return Math.max(0f, (pendingCancelDeadlineMs - System.currentTimeMillis()) / 1000f);
	}

	@RegisterFunction
	public void beginFineCancelWindow(String plate, int moneyDelta, int reputationDelta, boolean ticketIncrement) {
		if (!getDifficultyLevel().allowsFineCancel()) {
			return;
		}
		pendingCancelPlate = plate == null ? "" : plate.trim();
		pendingCancelMoneyDelta = moneyDelta;
		pendingCancelReputationDelta = reputationDelta;
		pendingCancelTicketIncrement = ticketIncrement;
		long windowMs = (long) (getDifficultyLevel().fineCancelSeconds() * 1000f);
		pendingCancelDeadlineMs = System.currentTimeMillis() + windowMs;
		notifyStateChanged();
	}

	@RegisterFunction
	public boolean cancelLastFine() {
		if (!canCancelFine()) {
			return false;
		}
		addMoney(-pendingCancelMoneyDelta);
		changeReputation(-pendingCancelReputationDelta);
		if (pendingCancelTicketIncrement && ticketsIssued > 0) {
			ticketsIssued--;
		}
		unmarkVehicleFined(pendingCancelPlate);
		clearPendingFineCancel();
		notifyStateChanged();
		return true;
	}

	@RegisterFunction
	public void clearPendingFineCancel() {
		pendingCancelPlate = "";
		pendingCancelMoneyDelta = 0;
		pendingCancelReputationDelta = 0;
		pendingCancelTicketIncrement = false;
		pendingCancelDeadlineMs = 0L;
	}

	private void onNewDay() {
		carsInspectedToday = 0;
	}

	@RegisterFunction
	public void addMoney(int amount) {
		money += amount;
		notifyStateChanged();
	}

	@RegisterFunction
	public void changeReputation(int amount) {
		reputation = Math.max(0, Math.min(100, reputation + amount));
		notifyStateChanged();
	}

	@RegisterFunction
	public void incrementTickets() {
		ticketsIssued++;
		notifyStateChanged();
	}

	@RegisterFunction
	public void setCurrentVehiclePlate(String plate) {
		currentVehiclePlate = plate == null || plate.isEmpty() ? "—" : plate;
		notifyStateChanged();
	}

	@RegisterFunction
	public String formatClock() {
		return String.format("%02d:%02d", hour, minute);
	}

	@RegisterFunction
	public String formatDay() {
		return "Dzień " + day;
	}

	@RegisterFunction
	public void notifyStateChanged() {
		stateChanged.emit();
	}

	@RegisterFunction
	public void clearPlayerTransform() {
		persistPlayerTransform = false;
	}

	@RegisterFunction
	public void capturePlayerTransform(float x, float y, float z, float rotY, float pitch) {
		playerPosX = x;
		playerPosY = y;
		playerPosZ = z;
		playerRotY = rotY;
		playerPitch = pitch;
		persistPlayerTransform = true;
	}

	@RegisterFunction
	public boolean isVehicleFined(String plate) {
		String key = normalizePlate(plate);
		return !key.isEmpty() && finedVehiclePlates.contains(key);
	}

	@RegisterFunction
	public void markVehicleFined(String plate) {
		String key = normalizePlate(plate);
		if (!key.isEmpty()) {
			finedVehiclePlates.add(key);
		}
	}

	@RegisterFunction
	public void unmarkVehicleFined(String plate) {
		finedVehiclePlates.remove(normalizePlate(plate));
	}

	private static String normalizePlate(String plate) {
		if (plate == null) {
			return "";
		}
		return plate.trim().replaceAll("\\s+", " ");
	}

	@RegisterFunction
	public void clearFinedVehicles() {
		finedVehiclePlates.clear();
	}
}
