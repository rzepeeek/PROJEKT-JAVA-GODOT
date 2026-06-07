package cvvl.simulator.ui;

import cvvl.simulator.GameState;
import cvvl.simulator.ScenePaths;
import cvvl.simulator.systems.DifficultyLevel;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.Control;
import godot.api.Input;
import godot.api.InputEvent;
import godot.core.Color;
import godot.core.MethodCallable0;
import godot.core.StringNames;

@RegisterClass
public class DifficultySelectController extends Control {
	@RegisterFunction
	@Override
	public void _ready() {
		Input.setMouseMode(Input.MouseMode.VISIBLE);
		setModulate(new Color(1, 1, 1, 1));

		Control panel = (Control) getNode("MenuPanel");
		panel.setModulate(new Color(1, 1, 1, 1));

		connectBtn("MenuPanel/BtnEasy", "onEasy");
		connectBtn("MenuPanel/BtnNormal", "onNormal");
		connectBtn("MenuPanel/BtnHard", "onHard");
		connectBtn("MenuPanel/BtnBack", "onBack");
	}

	@RegisterFunction
	@Override
	public void _unhandledInput(InputEvent event) {
		if (event.isActionPressed("ui_cancel") || event.isActionPressed("pause")) {
			DispatchUi.markInputHandled(this);
			onBack();
		}
	}

	@RegisterFunction
	public void onEasy() {
		startGame(DifficultyLevel.EASY);
	}

	@RegisterFunction
	public void onNormal() {
		startGame(DifficultyLevel.NORMAL);
	}

	@RegisterFunction
	public void onHard() {
		startGame(DifficultyLevel.HARD);
	}

	@RegisterFunction
	public void onBack() {
		getTree().changeSceneToFile(
				GameState.instance != null ? GameState.instance.returnScenePath : ScenePaths.MAIN_MENU
		);
	}

	private void startGame(DifficultyLevel level) {
		if (GameState.instance != null) {
			GameState.instance.resetNewGame(level.getId());
		}
		getTree().changeSceneToFile(ScenePaths.GAME);
	}

	private void connectBtn(String nodePath, String method) {
		getNode(nodePath).connect(
				"pressed",
				new MethodCallable0<Void>(this, StringNames.toGodotName(method), new Object[0])
		);
	}
}
