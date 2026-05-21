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
public class SubmenuController extends Control {
    @RegisterFunction
    @Override
    public void _ready() {
        Input.setMouseMode(Input.MouseMode.VISIBLE);
        setModulate(new Color(1, 1, 1, 1));
        getNode("BtnBack").connect("pressed", new MethodCallable0<Void>(this, StringNames.toGodotName("goBack"), new Object[0]));
    }

    @RegisterFunction
    @Override
    public void _unhandledInput(godot.api.InputEvent event) {
        if (event.isActionPressed("pause") || event.isActionPressed("ui_cancel")) {
            goBack();
            getViewport().setInputAsHandled();
        }
    }

    @RegisterFunction
    public void goBack() {
        String target = ScenePaths.MAIN_MENU;
        if (GameState.instance != null && GameState.instance.returnScenePath != null
                && !GameState.instance.returnScenePath.isEmpty()) {
            target = GameState.instance.returnScenePath;
        }
        getTree().changeSceneToFile(target);
    }
}
