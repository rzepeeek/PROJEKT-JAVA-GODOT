package cvvl.simulator.ui;

import cvvl.simulator.GameState;
import cvvl.simulator.player.PlayerInteraction;
import cvvl.simulator.systems.DifficultyLevel;
import cvvl.simulator.vehicles.ParkingViolation;
import cvvl.simulator.vehicles.Vehicle;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.Button;
import godot.api.Control;
import godot.api.Label;
import godot.api.Node;
import godot.api.PanelContainer;
import godot.core.MethodCallable0;
import godot.core.StringNames;

@RegisterClass
public class TicketPanelController extends PanelContainer {
	private Label vehicleIdLabel;
	private Label plateLabel;
	private Label statusLabel;
	private Label limitLabel;
	private Label violationHintLabel;
	private Label parkedAtLabel;
	private Control fineSubmenu;
	private Control actionsRow;
	private Control cancelFineRow;
	private Label fineBlockedLabel;
	private Vehicle activeVehicle;
	private PlayerInteraction playerInteraction;
	private boolean awaitingCancel;

	@RegisterFunction
	@Override
	public void _ready() {
		DispatchUi.stylePanel(this);

		vehicleIdLabel = (Label) getNode("Margin/VBox/Sections/Pojazd/Value");
		plateLabel = (Label) getNode("Margin/VBox/Sections/Status/Value");
		statusLabel = (Label) getNode("Margin/VBox/Sections/Limit/StatusValue");
		limitLabel = (Label) getNode("Margin/VBox/Sections/Limit/LimitValue");
		violationHintLabel = (Label) getNodeOrNull("Margin/VBox/ViolationHint");
		parkedAtLabel = (Label) getNodeOrNull("Margin/VBox/Sections/Limit/ParkedAtValue");
		fineSubmenu = (Control) getNode("Margin/VBox/FineSubmenu");
		actionsRow = (Control) getNode("Margin/VBox/Actions");
		cancelFineRow = (Control) getNodeOrNull("Margin/VBox/CancelFineRow");
		fineBlockedLabel = (Label) getNodeOrNull("Margin/VBox/FineBlocked");

		connectBtn("Margin/VBox/Actions/BtnFine", "onFineClicked");
		connectBtn("Margin/VBox/Actions/BtnWarning", "onWarningClicked");
		connectBtn("Margin/VBox/Actions/BtnIgnore", "onIgnoreClicked");
		connectBtn("Margin/VBox/Actions/BtnClose", "onCloseClicked");
		connectBtn("Margin/VBox/FineSubmenu/BtnNoTicket", "onFineNoTicket");
		connectBtn("Margin/VBox/FineSubmenu/BtnExpired", "onFineExpired");
		connectBtn("Margin/VBox/FineSubmenu/BtnWrongSpot", "onFineWrongSpot");
		if (getNodeOrNull("Margin/VBox/CancelFineRow/BtnCancelFine") != null) {
			connectBtn("Margin/VBox/CancelFineRow/BtnCancelFine", "onCancelFineClicked");
		}

		setModulate(new godot.core.Color(1, 1, 1, 1));
		if (fineBlockedLabel != null) {
			fineBlockedLabel.setVisible(false);
		}
		if (cancelFineRow != null) {
			cancelFineRow.setVisible(false);
		}
		hidePanel();
		resolvePlayerInteraction();
	}

	@RegisterFunction
	@Override
	public void _process(double delta) {
		if (awaitingCancel && GameState.instance != null) {
			updateCancelFineRow();
			if (!GameState.instance.canCancelFine()) {
				awaitingCancel = false;
				if (cancelFineRow != null) {
					cancelFineRow.setVisible(false);
				}
				hidePanel();
			}
			return;
		}

		if (!isVisible() || activeVehicle == null) {
			return;
		}
		if (playerInteraction == null) {
			resolvePlayerInteraction();
		}
		if (playerInteraction == null || !playerInteraction.isVehicleWithinInspectDistance(activeVehicle)) {
			hidePanel();
		}
	}

	@RegisterFunction
	@Override
	public void _unhandledInput(godot.api.InputEvent event) {
		if (!isVisible() || !event.isActionPressed("cancel_fine")) {
			return;
		}
		if (tryCancelFine()) {
			DispatchUi.markInputHandled(this);
		}
	}

	@RegisterFunction
	public void openForVehicle(Vehicle vehicle) {
		if (vehicle == null) {
			return;
		}
		awaitingCancel = false;
		if (cancelFineRow != null) {
			cancelFineRow.setVisible(false);
		}

		activeVehicle = vehicle;
		DifficultyLevel level = currentDifficulty();

		vehicleIdLabel.setText(vehicle.vehicleId);
		plateLabel.setText(vehicle.plate);
		if (level == DifficultyLevel.HARD) {
			statusLabel.setText("Miejsce: " + vehicle.parkingSpotName);
		} else {
			statusLabel.setText(vehicle.getStatusSummary());
		}
		updateParkingInfo(vehicle, level);
		updateViolationHint(vehicle, level);

		if (fineSubmenu != null) {
			fineSubmenu.setVisible(false);
		}
		updateFineControls(vehicle);
		setVisible(true);
		setModulate(new godot.core.Color(1, 1, 1, 1));
		setProcessMode(godot.api.Node.ProcessMode.ALWAYS);

		if (GameState.instance != null) {
			GameState.instance.setCurrentVehiclePlate(vehicle.plate);
			GameState.instance.incrementCarsInspected();
		}
	}

	@RegisterFunction
	public void hidePanel() {
		setVisible(false);
		setModulate(new godot.core.Color(1, 1, 1, 1));
		setProcessMode(godot.api.Node.ProcessMode.INHERIT);
		if (fineSubmenu != null) {
			fineSubmenu.setVisible(false);
		}
		if (cancelFineRow != null) {
			cancelFineRow.setVisible(false);
		}
		awaitingCancel = false;
		activeVehicle = null;
		if (GameState.instance != null) {
			GameState.instance.setCurrentVehiclePlate("—");
		}
		releaseGameplayMouse();
	}

	@RegisterFunction
	public void onFineClicked() {
		if (activeVehicle == null || isAlreadyFined(activeVehicle)) {
			return;
		}
		DifficultyLevel level = currentDifficulty();
		if (level == DifficultyLevel.EASY) {
			ParkingViolation actual = activeVehicle.getActualViolation();
			if (actual != ParkingViolation.VALID) {
				applyFine(actual);
			}
			return;
		}
		if (fineSubmenu != null) {
			fineSubmenu.setVisible(true);
		}
	}

	@RegisterFunction
	public void onWarningClicked() {
		if (activeVehicle == null || isAlreadyFined(activeVehicle)) {
			return;
		}
		DifficultyLevel level = currentDifficulty();
		resolveAction(false, 5, level.warningReputation());
		hidePanel();
	}

	@RegisterFunction
	public void onIgnoreClicked() {
		if (activeVehicle == null || isAlreadyFined(activeVehicle)) {
			return;
		}
		if (activeVehicle.getActualViolation() != ParkingViolation.VALID) {
			resolveAction(false, 0, -5);
		} else {
			resolveAction(true, 0, 2);
		}
		hidePanel();
	}

	@RegisterFunction
	public void onCloseClicked() {
		hidePanel();
	}

	@RegisterFunction
	public void onCancelFineClicked() {
		tryCancelFine();
	}

	@RegisterFunction
	public boolean cancelLastFineFromInput() {
		return tryCancelFine();
	}

	@RegisterFunction
	public void onFineNoTicket() {
		applyFine(ParkingViolation.NO_TICKET);
	}

	@RegisterFunction
	public void onFineExpired() {
		applyFine(ParkingViolation.EXPIRED_TIME);
	}

	@RegisterFunction
	public void onFineWrongSpot() {
		applyFine(ParkingViolation.WRONG_SPOT);
	}

	private boolean tryCancelFine() {
		if (GameState.instance == null || !GameState.instance.canCancelFine()) {
			return false;
		}
		String plate = GameState.instance.pendingCancelPlate;
		if (!GameState.instance.cancelLastFine()) {
			return false;
		}
		if (activeVehicle != null && activeVehicle.plate.equals(plate)) {
			activeVehicle.fineIssued = false;
		}
		awaitingCancel = false;
		if (cancelFineRow != null) {
			cancelFineRow.setVisible(false);
		}
		hidePanel();
		return true;
	}

	private void applyFine(ParkingViolation chosen) {
		if (activeVehicle == null || isAlreadyFined(activeVehicle)) {
			return;
		}
		DifficultyLevel level = currentDifficulty();
		ParkingViolation actual = activeVehicle.getActualViolation();
		boolean correct = actual == chosen && chosen != ParkingViolation.VALID;

		int moneyDelta;
		int reputationDelta;
		boolean ticketIncrement = false;
		if (correct) {
			moneyDelta = 25;
			reputationDelta = 8;
			ticketIncrement = true;
		} else if (actual == ParkingViolation.VALID) {
			moneyDelta = -15;
			reputationDelta = level.wrongFineReputation();
		} else {
			moneyDelta = -10;
			reputationDelta = level.wrongFineReputation();
		}

		resolveAction(correct, moneyDelta, reputationDelta, ticketIncrement);
		markVehicleAsFined(activeVehicle);

		if (level.allowsFineCancel()) {
			GameState.instance.beginFineCancelWindow(
					activeVehicle.plate,
					moneyDelta,
					reputationDelta,
					ticketIncrement
			);
			awaitingCancel = true;
			if (actionsRow != null) {
				actionsRow.setVisible(false);
			}
			if (fineSubmenu != null) {
				fineSubmenu.setVisible(false);
			}
			if (cancelFineRow != null) {
				cancelFineRow.setVisible(true);
			}
			updateCancelFineRow();
		} else {
			hidePanel();
		}
	}

	private void updateCancelFineRow() {
		if (cancelFineRow == null || GameState.instance == null) {
			return;
		}
		Button btn = (Button) getNodeOrNull("Margin/VBox/CancelFineRow/BtnCancelFine");
		if (btn != null) {
			float seconds = GameState.instance.pendingCancelSecondsLeft();
			btn.setText(String.format("[X] Anuluj mandat (%.0fs)", seconds));
		}
	}

	private void updateParkingInfo(Vehicle vehicle, DifficultyLevel level) {
		if (parkedAtLabel != null) {
			parkedAtLabel.setVisible(level.showsParkedAtAndLimit());
			if (level.showsParkedAtAndLimit()) {
				parkedAtLabel.setText("Zaparkowano: " + vehicle.formatParkedAt());
			}
		}

		if (level.hidesParkingDuration()) {
			limitLabel.setText("Limit biletu: " + (int) vehicle.timeLimitMinutes + " min");
			return;
		}
		if (level.showsExactParkingMinutes()) {
			limitLabel.setText(String.format(
					"Postój: %.1f min (limit %.0f min)",
					vehicle.parkingMinutes,
					vehicle.timeLimitMinutes
			));
			return;
		}
		limitLabel.setText(String.format("%.0f / %.0f min", vehicle.parkingMinutes, vehicle.timeLimitMinutes));
	}

	private void updateViolationHint(Vehicle vehicle, DifficultyLevel level) {
		if (violationHintLabel == null) {
			return;
		}
		if (level.revealsViolationOnScan()) {
			ParkingViolation actual = vehicle.getActualViolation();
			if (actual == ParkingViolation.VALID) {
				violationHintLabel.setText("Skan: brak naruszenia — parkowanie prawidłowe");
			} else {
				violationHintLabel.setText("Skan: wykryte naruszenie — " + vehicle.getViolationLabel());
			}
			violationHintLabel.setVisible(true);
		} else {
			violationHintLabel.setVisible(false);
		}
	}

	private DifficultyLevel currentDifficulty() {
		if (GameState.instance == null) {
			return DifficultyLevel.NORMAL;
		}
		return GameState.instance.getDifficultyLevel();
	}

	private boolean isAlreadyFined(Vehicle vehicle) {
		if (vehicle == null) {
			return true;
		}
		vehicle.syncFineStatusFromGameState();
		return vehicle.fineIssued
				|| (GameState.instance != null && GameState.instance.isVehicleFined(vehicle.plate));
	}

	private void markVehicleAsFined(Vehicle vehicle) {
		if (vehicle == null) {
			return;
		}
		vehicle.fineIssued = true;
		if (GameState.instance != null) {
			GameState.instance.markVehicleFined(vehicle.plate);
		}
	}

	private void updateFineControls(Vehicle vehicle) {
		boolean blocked = isAlreadyFined(vehicle);
		if (blocked) {
			vehicle.fineIssued = true;
		}
		if (fineBlockedLabel != null) {
			fineBlockedLabel.setVisible(blocked);
		}
		if (actionsRow != null) {
			actionsRow.setVisible(!blocked);
		}
		if (fineSubmenu != null) {
			fineSubmenu.setVisible(false);
		}
		setFineButtonsEnabled(!blocked);
		Button fineBtn = (Button) getNodeOrNull("Margin/VBox/Actions/BtnFine");
		if (fineBtn != null && !blocked && currentDifficulty() == DifficultyLevel.EASY
				&& vehicle.getActualViolation() == ParkingViolation.VALID) {
			fineBtn.setDisabled(true);
		}
	}

	private void setFineButtonsEnabled(boolean enabled) {
		String[] paths = {
				"Margin/VBox/Actions/BtnFine",
				"Margin/VBox/Actions/BtnWarning",
				"Margin/VBox/Actions/BtnIgnore",
				"Margin/VBox/FineSubmenu/BtnNoTicket",
				"Margin/VBox/FineSubmenu/BtnExpired",
				"Margin/VBox/FineSubmenu/BtnWrongSpot"
		};
		for (String path : paths) {
			Button btn = (Button) getNodeOrNull(path);
			if (btn != null) {
				btn.setDisabled(!enabled);
			}
		}
	}

	private void resolveAction(boolean success, int moneyDelta, int reputationDelta) {
		resolveAction(success, moneyDelta, reputationDelta, success);
	}

	private void resolveAction(boolean success, int moneyDelta, int reputationDelta, boolean incrementTickets) {
		if (GameState.instance == null) {
			return;
		}
		if (incrementTickets) {
			GameState.instance.incrementTickets();
		}
		GameState.instance.addMoney(moneyDelta);
		GameState.instance.changeReputation(reputationDelta);
	}

	private void connectBtn(String path, String method) {
		Button button = (Button) getNode(path);
		DispatchUi.styleDispatchButton(button);
		button.connect("pressed", new MethodCallable0<Void>(this, StringNames.toGodotName(method), new Object[0]));
	}

	private void resolvePlayerInteraction() {
		Node ui = getParent();
		if (ui == null) {
			return;
		}
		Node game = ui.getParent();
		if (game == null) {
			return;
		}
		Node node = game.getNodeOrNull("Player/Interaction");
		if (node instanceof PlayerInteraction interaction) {
			playerInteraction = interaction;
		}
	}

	private void releaseGameplayMouse() {
		Node game = getParent().getParent();
		if (game == null) {
			return;
		}
		Node playerNode = game.getNodeOrNull("Player");
		if (playerNode instanceof cvvl.simulator.player.FpsPlayer player) {
			player.setMouseCaptured(true);
		}
	}
}
