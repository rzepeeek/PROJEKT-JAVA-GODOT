package cvvl.simulator.ui;

import cvvl.simulator.GameState;
import cvvl.simulator.systems.DifficultyLevel;
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
	private Label difficultyLabel;
	private Label dailyGoalLabel;
	private Label moneyLabel;
	private Label reputationLabel;
	private Label ticketsLabel;
	private Label vehicleLabel;
	private Timer gameTimeTimer;

	@RegisterFunction
	@Override
	public void _ready() {
		setModulate(new godot.core.Color(1, 1, 1, 1));
		styleSelf();
		clockLabel = (Label) getNode("Margin/VBox/Clock");
		dayLabel = (Label) getNode("Margin/VBox/Day");
		difficultyLabel = (Label) getNode("Margin/VBox/Difficulty");
		dailyGoalLabel = (Label) getNode("Margin/VBox/DailyGoal");
		moneyLabel = (Label) getNode("Margin/VBox/Money");
		reputationLabel = (Label) getNode("Margin/VBox/Reputation");
		ticketsLabel = (Label) getNode("Margin/VBox/Tickets");
		vehicleLabel = (Label) getNode("Margin/VBox/Vehicle");

		gameTimeTimer = (Timer) getNode("GameTimeTimer");
		gameTimeTimer.connect(
				"timeout",
				new MethodCallable0<Void>(this, StringNames.toGodotName("onGameTimeTick"), new Object[0])
		);

		applyDifficultyTimer();
		refresh();

		if (GameState.instance != null) {
			GameState.instance.stateChanged.connect(
					new MethodCallable0<Void>(this, StringNames.toGodotName("refresh"), new Object[0]),
					Object.ConnectFlags.DEFAULT
			);
		}
	}

	@RegisterFunction
	public void onGameTimeTick() {
		if (GameState.instance != null) {
			GameState.instance.advanceTime(GameState.instance.getMinutesPerTick());
		}
	}

	@RegisterFunction
	public void refresh() {
		if (GameState.instance == null) {
			return;
		}
		applyDifficultyTimer();
		GameState state = GameState.instance;
		clockLabel.setText("GODZ. " + state.formatClock());
		dayLabel.setText(state.formatDay());
		difficultyLabel.setText("POZIOM: " + state.formatDifficulty());

		DifficultyLevel level = state.getDifficultyLevel();
		if (level.showsDailyGoal()) {
			dailyGoalLabel.setVisible(true);
			dailyGoalLabel.setText(state.formatDailyGoal());
		} else {
			dailyGoalLabel.setVisible(false);
		}

		moneyLabel.setText("SALDO: " + state.money + " zł");
		reputationLabel.setText("REPUTACJA: " + state.reputation + "%");
		ticketsLabel.setText("MANDATY: " + state.ticketsIssued);
		vehicleLabel.setText("POJAZD: " + state.currentVehiclePlate);
	}

	private void applyDifficultyTimer() {
		if (gameTimeTimer == null || GameState.instance == null) {
			return;
		}
		gameTimeTimer.setWaitTime(GameState.instance.getGameTimeTickSeconds());
	}

	private void styleSelf() {
		DispatchUi.stylePanel(this);
		for (String path : new String[]{
				"Margin/VBox/Clock",
				"Margin/VBox/Day",
				"Margin/VBox/Difficulty",
				"Margin/VBox/DailyGoal",
				"Margin/VBox/Money",
				"Margin/VBox/Reputation",
				"Margin/VBox/Tickets",
				"Margin/VBox/Vehicle"
		}) {
			DispatchUi.styleTerminalLabel((Label) getNode(path), path.contains("Clock") || path.contains("Day"));
		}
	}
}
