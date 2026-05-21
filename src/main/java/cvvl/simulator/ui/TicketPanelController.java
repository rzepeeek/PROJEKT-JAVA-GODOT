package cvvl.simulator.ui;

import cvvl.simulator.GameState;
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
	private Control fineSubmenu;
	private Control actionsRow;
	private Label fineBlockedLabel;
	private Vehicle activeVehicle;

	@RegisterFunction
	@Override
	public void _ready() {
		DispatchUi.stylePanel(this);

		vehicleIdLabel = (Label) getNode("Margin/VBox/Sections/Pojazd/Value");
		plateLabel = (Label) getNode("Margin/VBox/Sections/Status/Value");
		statusLabel = (Label) getNode("Margin/VBox/Sections/Limit/StatusValue");
		limitLabel = (Label) getNode("Margin/VBox/Sections/Limit/LimitValue");
		fineSubmenu = (Control) getNode("Margin/VBox/FineSubmenu");
		actionsRow = (Control) getNode("Margin/VBox/Actions");
		fineBlockedLabel = (Label) getNodeOrNull("Margin/VBox/FineBlocked");

		connectBtn("Margin/VBox/Actions/BtnFine", "onFineClicked");
		connectBtn("Margin/VBox/Actions/BtnWarning", "onWarningClicked");
		connectBtn("Margin/VBox/Actions/BtnIgnore", "onIgnoreClicked");
		connectBtn("Margin/VBox/Actions/BtnClose", "onCloseClicked");
		connectBtn("Margin/VBox/FineSubmenu/BtnNoTicket", "onFineNoTicket");
		connectBtn("Margin/VBox/FineSubmenu/BtnExpired", "onFineExpired");
		connectBtn("Margin/VBox/FineSubmenu/BtnWrongSpot", "onFineWrongSpot");

		setModulate(new godot.core.Color(1, 1, 1, 1));
		if (fineBlockedLabel != null) {
			fineBlockedLabel.setVisible(false);
		}
		hidePanel();
	}

	@RegisterFunction
	public void openForVehicle(Vehicle vehicle) {
		activeVehicle = vehicle;
		vehicleIdLabel.setText(vehicle.vehicleId);
		plateLabel.setText(vehicle.plate);
		statusLabel.setText(vehicle.getStatusSummary());
		limitLabel.setText(String.format("%.0f / %.0f min", vehicle.parkingMinutes, vehicle.timeLimitMinutes));
		if (fineSubmenu != null) {
			fineSubmenu.setVisible(false);
		}
		updateFineControls(vehicle);
		setModulate(new godot.core.Color(1, 1, 1, 1));
		setVisible(true);
		DispatchUi.slideIn(this, 40f, 0.3f);

		if (GameState.instance != null) {
			GameState.instance.setCurrentVehiclePlate(vehicle.plate);
		}
	}

	@RegisterFunction
	public void hidePanel() {
		setVisible(false);
		setModulate(new godot.core.Color(1, 1, 1, 1));
		if (fineSubmenu != null) {
			fineSubmenu.setVisible(false);
		}
		activeVehicle = null;
		if (GameState.instance != null) {
			GameState.instance.setCurrentVehiclePlate("—");
		}
		releaseGameplayMouse();
	}

	@RegisterFunction
	public void onFineClicked() {
		if (isAlreadyFined(activeVehicle)) {
			return;
		}
		if (fineSubmenu != null) {
			fineSubmenu.setVisible(true);
		}
	}

	@RegisterFunction
	public void onWarningClicked() {
		if (isAlreadyFined(activeVehicle)) {
			return;
		}
		resolveAction(false, 5, -2);
		hidePanel();
	}

	@RegisterFunction
	public void onIgnoreClicked() {
		if (isAlreadyFined(activeVehicle)) {
			return;
		}
		if (activeVehicle != null && activeVehicle.getActualViolation() != ParkingViolation.VALID) {
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

	private void applyFine(ParkingViolation chosen) {
		if (isAlreadyFined(activeVehicle)) {
			return;
		}
		ParkingViolation actual = activeVehicle.getActualViolation();
		boolean correct = actual == chosen && chosen != ParkingViolation.VALID;

		if (correct) {
			resolveAction(true, 25, 8);
		} else if (actual == ParkingViolation.VALID) {
			resolveAction(false, -15, -10);
		} else {
			resolveAction(false, -10, -6);
		}
		markVehicleAsFined(activeVehicle);
		hidePanel();
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
		if (GameState.instance == null) {
			return;
		}
		if (success) {
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
