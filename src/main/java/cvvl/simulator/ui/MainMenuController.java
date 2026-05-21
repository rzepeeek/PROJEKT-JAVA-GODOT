package cvvl.simulator.ui;

import cvvl.simulator.GameState;
import cvvl.simulator.ScenePaths;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.Control;
import godot.api.Input;
import godot.core.Color;
import godot.core.MethodCallable0;
import godot.core.StringNames;

@RegisterClass
public class MainMenuController extends Control {
    @RegisterFunction
    @Override
    public void _ready() {
        Input.setMouseMode(Input.MouseMode.VISIBLE);
        setModulate(new Color(1, 1, 1, 1));

        Control menu = (Control) getNode("MenuPanel");
        menu.setModulate(new Color(1, 1, 1, 1));

        connectBtn("MenuPanel/BtnStart", "onStart");
        connectBtn("MenuPanel/BtnTutorial", "onTutorial");
        connectBtn("MenuPanel/BtnLoad", "onLoad");
        connectBtn("MenuPanel/BtnSettings", "onSettings");
        connectBtn("MenuPanel/BtnQuit", "onQuit");
    }

    @RegisterFunction
    public void onStart() {
        if (GameState.instance != null) {
            GameState.instance.reopenPauseAfterReturn = false;
            GameState.instance.clearPlayerTransform();
            GameState.instance.clearFinedVehicles();
            GameState.instance.returnScenePath = ScenePaths.MAIN_MENU;
        }
        changeScene(ScenePaths.GAME);
    }

    @RegisterFunction
    public void onTutorial() {
        onStart();
    }

    @RegisterFunction
    public void onLoad() {
        if (GameState.instance != null) {
            GameState.instance.saveMenuMode = "load";
        }
        openSubmenu(ScenePaths.SAVE);
    }

    @RegisterFunction
    public void onSettings() {
        openSubmenu(ScenePaths.OPTIONS);
    }

    private void openSubmenu(String path) {
        if (GameState.instance != null) {
            GameState.instance.prepareReturnTo(ScenePaths.MAIN_MENU, false);
        }
        changeScene(path);
    }

    @RegisterFunction
    public void onQuit() {
        getTree().quit();
    }

    private void changeScene(String path) {
        getTree().changeSceneToFile(path);
    }

    private void connectBtn(String nodePath, String method) {
        getNode(nodePath).connect("pressed", new MethodCallable0<Void>(this, StringNames.toGodotName(method), new Object[0]));
    }
}
