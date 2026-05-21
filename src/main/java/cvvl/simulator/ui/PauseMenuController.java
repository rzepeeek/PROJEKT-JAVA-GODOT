package cvvl.simulator.ui;

import cvvl.simulator.GameState;
import cvvl.simulator.GameWorldController;
import cvvl.simulator.ScenePaths;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.Control;
import godot.api.Input;
import godot.api.Node;
import godot.api.Node.ProcessMode;
import godot.core.Color;
import godot.core.MethodCallable0;
import godot.core.StringNames;

@RegisterClass
public class PauseMenuController extends Control {
	private GameWorldController gameWorld;

	@RegisterFunction
	public void setup(GameWorldController controller) {
		gameWorld = controller;
	}

	@RegisterFunction
	@Override
	public void _ready() {
		setProcessMode(ProcessMode.ALWAYS);
		setVisible(false);
		setModulate(new Color(1, 1, 1, 1));

		connectBtn("Panel/VBox/BtnResume", "onResume");
		connectBtn("Panel/VBox/BtnOptions", "onOptions");
		connectBtn("Panel/VBox/BtnSave", "onSave");
		connectBtn("Panel/VBox/BtnMainMenu", "onMainMenu");

		styleButtons();
	}

	@RegisterFunction
	@Override
	public void _unhandledInput(godot.api.InputEvent event) {
		if (!isVisible()) {
			return;
		}
		if (event.isActionPressed("pause") || event.isActionPressed("ui_cancel")) {
			onResume();
			getViewport().setInputAsHandled();
		}
	}

	@RegisterFunction
	public void open() {
		setVisible(true);
		setModulate(new Color(1, 1, 1, 1));
		Input.setMouseMode(Input.MouseMode.VISIBLE);
		Control panel = (Control) getNode("Panel");
		panel.setModulate(new Color(1, 1, 1, 1));
		DispatchUi.stylePanel((godot.api.PanelContainer) panel);
	}

	@RegisterFunction
	public void close() {
		setVisible(false);
		if (gameWorld != null && !gameWorld.isPaused()) {
			Input.setMouseMode(Input.MouseMode.CAPTURED);
		}
	}

	@RegisterFunction
	public void onResume() {
		if (gameWorld != null) {
			gameWorld.resumeFromPause();
		} else {
			setVisible(false);
		}
	}

	@RegisterFunction
	public void onOptions() {
		navigateToSubmenu(ScenePaths.OPTIONS);
	}

	@RegisterFunction
	public void onSave() {
		navigateToSubmenu(ScenePaths.SAVE);
	}

	@RegisterFunction
	public void onMainMenu() {
		if (gameWorld != null) {
			gameWorld.resumeFromPause();
		}
		if (GameState.instance != null) {
			GameState.instance.reopenPauseAfterReturn = false;
		}
		getTree().changeSceneToFile(ScenePaths.MAIN_MENU);
	}

	private void navigateToSubmenu(String submenuPath) {
		if (gameWorld != null) {
			if (GameState.instance != null) {
				GameState.instance.prepareReturnTo(ScenePaths.GAME, true);
			}
			gameWorld.openInGameSubmenu(submenuPath);
			return;
		}
		if (GameState.instance != null) {
			GameState.instance.prepareReturnTo(ScenePaths.GAME, true);
			if (ScenePaths.SAVE.equals(submenuPath)) {
				GameState.instance.saveMenuMode = "save";
			}
		}
		getTree().changeSceneToFile(submenuPath);
	}

	private void styleButtons() {
		String[] paths = {
				"Panel/VBox/BtnResume",
				"Panel/VBox/BtnOptions",
				"Panel/VBox/BtnSave",
                "Panel/VBox/BtnMainMenu"
		};
		for (String path : paths) {
			DispatchUi.styleDispatchButton((godot.api.Button) getNode(path));
		}
	}

	private void connectBtn(String path, String method) {
		getNode(path).connect("pressed", new MethodCallable0<Void>(this, StringNames.toGodotName(method), new Object[0]));
	}
}
