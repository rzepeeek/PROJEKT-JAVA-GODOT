package cvvl.simulator.ui;

import cvvl.simulator.GameState;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.Label;
import godot.api.Object;
import godot.api.PanelContainer;
import godot.api.Timer;
import godot.core.MethodCallable0;
import godot.core.StringNames;

@RegisterClass
public class HudController extends PanelContainer {
	private Label clockLabel;
	private Label dayLabel;
	private Label moneyLabel;
	private Label reputationLabel;
	private Label ticketsLabel;
	private Label vehicleLabel;

	@RegisterFunction
	@Override
	public void _ready() {
		setModulate(new godot.core.Color(1, 1, 1, 1));
		styleSelf();
		clockLabel = (Label) getNode("Margin/VBox/Clock");
		dayLabel = (Label) getNode("Margin/VBox/Day");
		moneyLabel = (Label) getNode("Margin/VBox/Money");
		reputationLabel = (Label) getNode("Margin/VBox/Reputation");
		ticketsLabel = (Label) getNode("Margin/VBox/Tickets");
		vehicleLabel = (Label) getNode("Margin/VBox/Vehicle");

		refresh();

		if (GameState.instance != null) {
			GameState.instance.stateChanged.connect(
					new MethodCallable0<Void>(this, StringNames.toGodotName("refresh"), new Object[0]),
					Object.ConnectFlags.DEFAULT
			);
		}

		Timer timer = (Timer) getNode("GameTimeTimer");
		timer.connect("timeout", new MethodCallable0<Void>(this, StringNames.toGodotName("onGameTimeTick"), new Object[0]));
	}

	@RegisterFunction
	public void onGameTimeTick() {
		if (GameState.instance != null) {
			GameState.instance.advanceTime(5);
		}
	}

	@RegisterFunction
	public void refresh() {
		if (GameState.instance == null) {
			return;
		}
		GameState state = GameState.instance;
		clockLabel.setText("GODZ. " + state.formatClock());
		dayLabel.setText(state.formatDay());
		moneyLabel.setText("SALDO: " + state.money + " zł");
		reputationLabel.setText("REPUTACJA: " + state.reputation + "%");
		ticketsLabel.setText("MANDATY: " + state.ticketsIssued);
		vehicleLabel.setText("POJAZD: " + state.currentVehiclePlate);
	}

	private void styleSelf() {
		DispatchUi.stylePanel(this);
		for (String path : new String[]{
				"Margin/VBox/Clock",
				"Margin/VBox/Day",
				"Margin/VBox/Money",
				"Margin/VBox/Reputation",
				"Margin/VBox/Tickets",
                "Margin/VBox/Vehicle"
		}) {
			DispatchUi.styleTerminalLabel((Label) getNode(path), path.contains("Clock") || path.contains("Day"));
		}
	}
}
