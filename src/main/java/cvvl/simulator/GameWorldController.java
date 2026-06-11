package cvvl.simulator;

import cvvl.simulator.player.FpsPlayer;
import cvvl.simulator.systems.SettingsManager;
import cvvl.simulator.player.PlayerInteraction;
import cvvl.simulator.world.PlayerSpawnHelper;
import cvvl.simulator.ui.DispatchUi;
import cvvl.simulator.ui.OptionsMenuController;
import cvvl.simulator.ui.PauseMenuController;
import cvvl.simulator.ui.SaveMenuController;
import cvvl.simulator.ui.TicketPanelController;
import cvvl.simulator.vehicles.Vehicle;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.Control;
import godot.api.Input;
import godot.api.Node;
import godot.api.Node.ProcessMode;
import godot.api.Object;
import godot.core.Vector3;
import godot.core.MethodCallable1;
import godot.core.StringNames;

@RegisterClass
public class GameWorldController extends Node {
	private FpsPlayer player;
	private PlayerInteraction interaction;
	private PauseMenuController pauseMenu;
	private TicketPanelController ticketPanel;
	private Control optionsOverlay;
	private Control saveOverlay;
	private boolean paused;

	@RegisterFunction
	@Override
	public void _ready() {
		player = (FpsPlayer) getNode("Player");
		interaction = (PlayerInteraction) player.getNode("Interaction");
		pauseMenu = (PauseMenuController) getNode("UI/PauseMenu");
		ticketPanel = (TicketPanelController) getNode("UI/TicketPanel");
		optionsOverlay = (Control) getNode("UI/OptionsOverlay");
		saveOverlay = (Control) getNode("UI/SaveOverlay");
		setOverlayActive(optionsOverlay, false);
		setOverlayActive(saveOverlay, false);
		pauseMenu.setup(this);

		interaction.vehicleTargeted.connect(
				new MethodCallable1<Void, Vehicle>(this, StringNames.toGodotName("onVehicleTargeted"), new Object[0]),
				Object.ConnectFlags.DEFAULT
		);

		if (SettingsManager.instance != null) {
			SettingsManager.instance.applySettings();
		}

		player.setMouseCaptured(true);
		finishPlayerSpawn();
		callDeferred(StringNames.toGodotName("finishPlayerSpawn"));

		if (GameState.instance != null && GameState.instance.reopenPauseAfterReturn) {
			GameState.instance.reopenPauseAfterReturn = false;
			openPauseMenu();
		}
	}

	@RegisterFunction
	@Override
	public void _unhandledInput(godot.api.InputEvent event) {
		if (event.isActionPressed("cancel_fine")
				&& GameState.instance != null
				&& GameState.instance.canCancelFine()) {
			ticketPanel.cancelLastFineFromInput();
			DispatchUi.markInputHandled(this);
			return;
		}
		if (!event.isActionPressed("pause")) {
			return;
		}
		if (isOverlayOpen()) {
			return;
		}
		if (ticketPanel.isVisible()) {
			DispatchUi.markInputHandled(this);
			ticketPanel.hidePanel();
			return;
		}
		DispatchUi.markInputHandled(this);
		togglePause();
	}

	@RegisterFunction
	public void togglePause() {
		if (paused) {
			resumeFromPause();
		} else {
			openPauseMenu();
		}
	}

	@RegisterFunction
	public void openPauseMenu() {
		paused = true;
		setGameplayPaused(true);
		ticketPanel.hidePanel();
		pauseMenu.open();
	}

	@RegisterFunction
	public void resumeFromPause() {
		paused = false;
		setOverlayActive(optionsOverlay, false);
		setOverlayActive(saveOverlay, false);
		setGameplayPaused(false);
		pauseMenu.close();
		player.setMouseCaptured(true);
	}

	@RegisterFunction
	public void openInGameSubmenu(String submenuPath) {
		paused = true;
		setGameplayPaused(true);
		pauseMenu.close();
		setOverlayActive(optionsOverlay, false);
		setOverlayActive(saveOverlay, false);

		if (ScenePaths.OPTIONS.equals(submenuPath)) {
			setOverlayActive(optionsOverlay, true);
			if (optionsOverlay instanceof OptionsMenuController options) {
				options.onOverlayOpened();
			}
		} else if (ScenePaths.SAVE.equals(submenuPath)) {
			if (GameState.instance != null) {
				GameState.instance.saveMenuMode = "save";
			}
			setOverlayActive(saveOverlay, true);
			if (saveOverlay instanceof SaveMenuController saveMenu) {
				saveMenu.onOverlayOpened();
			}
		}
	}

	@RegisterFunction
	public void closeInGameSubmenu(boolean reopenPause) {
		setOverlayActive(optionsOverlay, false);
		setOverlayActive(saveOverlay, false);
		if (reopenPause) {
			openPauseMenu();
		} else {
			resumeFromPause();
		}
	}

	private boolean isOverlayOpen() {
		return optionsOverlay.isVisible() || saveOverlay.isVisible();
	}

	private void setOverlayActive(Control overlay, boolean active) {
		overlay.setVisible(active);
		overlay.setProcessMode(active ? ProcessMode.ALWAYS : ProcessMode.DISABLED);
	}

	@RegisterFunction
	public void captureWorldForSave() {
		capturePlayerTransform();
		Node spawnerNode = getNodeOrNull("World/ParkingMap/VehicleSpawner");
		if (spawnerNode instanceof cvvl.simulator.vehicles.VehicleSpawner spawner
				&& GameState.instance != null) {
			GameState.instance.captureVehicleSnapshots(spawner.captureSpawnedVehicles());
		}
	}

	@RegisterFunction
	public void capturePlayerTransform() {
		if (GameState.instance == null || player == null) {
			return;
		}
		Vector3 pos = player.getGlobalPosition();
		GameState.instance.capturePlayerTransform(
				(float) pos.getX(),
				(float) pos.getY(),
				(float) pos.getZ(),
				(float) player.getRotation().getY(),
				player.getPitch()
		);
	}

	private void restorePlayerTransform() {
		if (GameState.instance == null || player == null || !GameState.instance.persistPlayerTransform) {
			return;
		}
		player.applyTransformState(
				GameState.instance.playerPosX,
				GameState.instance.playerPosY,
				GameState.instance.playerPosZ,
				GameState.instance.playerRotY,
				GameState.instance.playerPitch
		);
	}

	private void applyPlayerSpawnMarker() {
		PlayerSpawnHelper.applyMarkerToPlayer(player, PlayerSpawnHelper.findSpawnMarker(this));
	}

	@RegisterFunction
	public boolean isPaused() {
		return paused;
	}

	@RegisterFunction
	public void finishPlayerSpawn() {
		if (GameState.instance != null && GameState.instance.shouldRestorePlayerFromSave()) {
			restorePlayerTransform();
		} else {
			applyPlayerSpawnMarker();
		}
		if (player != null) {
			player.snapToGround();
		}
	}

	@RegisterFunction
	public void onVehicleTargeted(Vehicle vehicle) {
		openTicketForVehicle(vehicle);
	}

	@RegisterFunction
	public void openTicketForVehicle(Vehicle vehicle) {
		if (paused || vehicle == null) {
			return;
		}
		ticketPanel.openForVehicle(vehicle);
		player.setMouseCaptured(false);
		Input.setMouseMode(Input.MouseMode.VISIBLE);
	}

	private void setGameplayPaused(boolean pause) {
		player.setGameplayEnabled(!pause);
		interaction.setProcess(!pause);
	}
}
