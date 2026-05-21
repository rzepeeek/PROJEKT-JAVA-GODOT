package cvvl.simulator;

import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.annotation.RegisterProperty;
import godot.annotation.RegisterSignal;
import godot.api.Node;
import godot.core.Signal0;
import godot.core.StringNames;

import java.util.HashSet;
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

	private final Set<String> finedVehiclePlates = new HashSet<>();

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
		minute += (int) deltaMinutes;
		while (minute >= 60) {
			minute -= 60;
			hour++;
		}
		if (hour >= 24) {
			hour = 0;
			day++;
		}
		notifyStateChanged();
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
