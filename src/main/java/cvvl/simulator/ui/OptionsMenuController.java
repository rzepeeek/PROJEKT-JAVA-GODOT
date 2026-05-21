package cvvl.simulator.ui;

import cvvl.simulator.GameWorldController;
import cvvl.simulator.ScenePaths;
import cvvl.simulator.systems.InputActions;
import cvvl.simulator.systems.SettingsManager;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.Button;
import godot.api.Control;
import godot.api.HSlider;
import godot.api.Input;
import godot.api.InputEventKey;
import godot.api.Label;
import godot.core.Color;
import godot.core.Key;
import godot.core.MethodCallable0;
import godot.core.MethodCallable1;
import godot.core.StringNames;

@RegisterClass
public class OptionsMenuController extends Control {
    private static final String[] BIND_NODE_PATHS = {
            "Panel/Margin/Scroll/VBox/KeybindList/Row0/BindBtn",
            "Panel/Margin/Scroll/VBox/KeybindList/Row1/BindBtn",
            "Panel/Margin/Scroll/VBox/KeybindList/Row2/BindBtn",
            "Panel/Margin/Scroll/VBox/KeybindList/Row3/BindBtn",
            "Panel/Margin/Scroll/VBox/KeybindList/Row4/BindBtn",
            "Panel/Margin/Scroll/VBox/KeybindList/Row5/BindBtn",
            "Panel/Margin/Scroll/VBox/KeybindList/Row6/BindBtn",
            "Panel/Margin/Scroll/VBox/KeybindList/Row7/BindBtn"
    };

    private Label fpsValueLabel;
    private HSlider sensitivitySlider;
    private Label sensitivityValueLabel;
    private Label rebindHintLabel;

    private String waitingAction = null;
    private Button waitingButton = null;
    private int lastAppliedSensitivityPercent = -1;

    @RegisterFunction
    @Override
    public void _ready() {
        if (findGameWorldController() == null) {
            Input.setMouseMode(Input.MouseMode.VISIBLE);
        }
        setModulate(new Color(1, 1, 1, 1));

        fpsValueLabel = (Label) getNode("Panel/Margin/Scroll/VBox/FpsRow/FpsValue");
        sensitivitySlider = (HSlider) getNode("Panel/Margin/Scroll/VBox/SensRow/HBox/SensitivitySlider");
        sensitivityValueLabel = (Label) getNode("Panel/Margin/Scroll/VBox/SensRow/HBox/SensValue");
        rebindHintLabel = (Label) getNode("Panel/Margin/Scroll/VBox/RebindHint");

        stylePanel((godot.api.PanelContainer) getNode("Panel"));
        DispatchUi.styleDispatchButton((Button) getNode("Panel/Margin/Scroll/VBox/FpsRow/BtnFpsDown"));
        DispatchUi.styleDispatchButton((Button) getNode("Panel/Margin/Scroll/VBox/FpsRow/BtnFpsUp"));
        DispatchUi.styleDispatchButton((Button) getNode("Panel/Margin/Scroll/VBox/SensRow/HBox/BtnSensDown"));
        DispatchUi.styleDispatchButton((Button) getNode("Panel/Margin/Scroll/VBox/SensRow/HBox/BtnSensUp"));
        DispatchUi.styleDispatchButton((Button) getNode("BtnBack"));

        for (int i = 0; i < BIND_NODE_PATHS.length; i++) {
            Button btn = (Button) getNode(BIND_NODE_PATHS[i]);
            DispatchUi.styleDispatchButton(btn);
            final int index = i;
            btn.connect("pressed", new MethodCallable0<Void>(this, bindMethodName(index), new Object[0]));
        }

        connectBtn("Panel/Margin/Scroll/VBox/FpsRow/BtnFpsDown", "onFpsDown");
        connectBtn("Panel/Margin/Scroll/VBox/FpsRow/BtnFpsUp", "onFpsUp");
        connectBtn("Panel/Margin/Scroll/VBox/SensRow/HBox/BtnSensDown", "onSensDown");
        connectBtn("Panel/Margin/Scroll/VBox/SensRow/HBox/BtnSensUp", "onSensUp");
        connectBtn("BtnBack", "goBack");

        sensitivitySlider.connect("value_changed",
                new MethodCallable1<Void, Double>(this, StringNames.toGodotName("onSensitivitySliderValue"), new Object[0])
        );

        refreshUi();
    }

    @RegisterFunction
    @Override
    public void _process(double delta) {
        if (!isVisible() || SettingsManager.instance == null || sensitivitySlider == null) {
            return;
        }
        int sliderPercent = clampPercent((int) Math.round(sensitivitySlider.getValue()));
        if (sliderPercent != lastAppliedSensitivityPercent) {
            applySensitivityPercent(sliderPercent);
        }
    }

    @RegisterFunction
    public void onOverlayOpened() {
        Input.setMouseMode(Input.MouseMode.VISIBLE);
        refreshUi();
    }

    @RegisterFunction
    public void onBind0() { startRebind(0); }
    @RegisterFunction
    public void onBind1() { startRebind(1); }
    @RegisterFunction
    public void onBind2() { startRebind(2); }
    @RegisterFunction
    public void onBind3() { startRebind(3); }
    @RegisterFunction
    public void onBind4() { startRebind(4); }
    @RegisterFunction
    public void onBind5() { startRebind(5); }
    @RegisterFunction
    public void onBind6() { startRebind(6); }
    @RegisterFunction
    public void onBind7() { startRebind(7); }

    @RegisterFunction
    @Override
    public void _unhandledInput(godot.api.InputEvent event) {
        if (!isVisible()) {
            return;
        }
        if (waitingAction == null) {
            if (event.isActionPressed("pause") || event.isActionPressed("ui_cancel")) {
                saveAndGoBack();
                getViewport().setInputAsHandled();
            }
            return;
        }

        if (event instanceof InputEventKey keyEvent && keyEvent.isPressed() && !keyEvent.isEcho()) {
            if (keyEvent.getKeycode() == Key.ESCAPE) {
                cancelRebind();
                getViewport().setInputAsHandled();
                return;
            }
            int code = (int) keyEvent.getPhysicalKeycode().getValue();
            if (SettingsManager.instance != null) {
                SettingsManager.instance.applyBinding(waitingAction, code);
            }
            if (waitingButton != null && SettingsManager.instance != null) {
                waitingButton.setText(SettingsManager.instance.getBindingLabel(waitingAction));
            }
            cancelRebind();
            getViewport().setInputAsHandled();
        }
    }

    @RegisterFunction
    public void onFpsDown() {
        if (SettingsManager.instance != null) {
            SettingsManager.instance.decreaseFps();
            refreshFpsLabel();
        }
    }

    @RegisterFunction
    public void onFpsUp() {
        if (SettingsManager.instance != null) {
            SettingsManager.instance.increaseFps();
            refreshFpsLabel();
        }
    }

    @RegisterFunction
    public void onSensitivitySliderValue(double value) {
        applySensitivityPercent(clampPercent((int) Math.round(value)));
    }

    @RegisterFunction
    public void onSensDown() {
        if (SettingsManager.instance != null) {
            SettingsManager.instance.decreaseSensitivity();
            syncSensitivityUiFromSettings();
        }
    }

    @RegisterFunction
    public void onSensUp() {
        if (SettingsManager.instance != null) {
            SettingsManager.instance.increaseSensitivity();
            syncSensitivityUiFromSettings();
        }
    }

    @RegisterFunction
    public void goBack() {
        saveAndGoBack();
    }

    private void startRebind(int index) {
        waitingAction = InputActions.BINDABLE[index];
        waitingButton = (Button) getNode(BIND_NODE_PATHS[index]);
        rebindHintLabel.setText("Naciśnij klawisz: " + InputActions.BIND_LABELS[index] + "  (ESC = anuluj)");
    }

    private void cancelRebind() {
        waitingAction = null;
        waitingButton = null;
        rebindHintLabel.setText("Kliknij pole klawisza, aby zmienić przypisanie.");
    }

    private void refreshUi() {
        syncSensitivityUiFromSettings();
        refreshFpsLabel();
        refreshBindLabels();
    }

    private void syncSensitivityUiFromSettings() {
        if (SettingsManager.instance == null || sensitivitySlider == null) {
            return;
        }
        int percent = SettingsManager.instance.getMouseSensitivityPercent();
        lastAppliedSensitivityPercent = percent;
        sensitivitySlider.setValue(percent);
        refreshSensitivityLabel(percent);
    }

    private void applySensitivityPercent(int percent) {
        if (SettingsManager.instance == null) {
            return;
        }
        int clamped = clampPercent(percent);
        lastAppliedSensitivityPercent = clamped;
        SettingsManager.instance.setMouseSensitivityPercent(clamped);
        refreshSensitivityLabel(clamped);
    }

    private void refreshFpsLabel() {
        if (SettingsManager.instance != null) {
            fpsValueLabel.setText(SettingsManager.instance.getCurrentFps() + " FPS");
        }
    }

    private void refreshSensitivityLabel(int percent) {
        sensitivityValueLabel.setText(clampPercent(percent) + "%");
    }

    private static int clampPercent(int percent) {
        return Math.max(1, Math.min(100, percent));
    }

    private void refreshBindLabels() {
        if (SettingsManager.instance == null) {
            return;
        }
        for (int i = 0; i < BIND_NODE_PATHS.length; i++) {
            Button btn = (Button) getNode(BIND_NODE_PATHS[i]);
            btn.setText(SettingsManager.instance.getBindingLabel(InputActions.BINDABLE[i]));
        }
    }

    private void saveAndGoBack() {
        if (SettingsManager.instance != null) {
            SettingsManager.instance.saveSettings();
        }
        if (tryCloseInGameOverlay()) {
            return;
        }
        String target = ScenePaths.MAIN_MENU;
        if (cvvl.simulator.GameState.instance != null && cvvl.simulator.GameState.instance.returnScenePath != null) {
            target = cvvl.simulator.GameState.instance.returnScenePath;
        }
        getTree().changeSceneToFile(target);
    }

    private boolean tryCloseInGameOverlay() {
        GameWorldController gameWorld = findGameWorldController();
        if (gameWorld == null) {
            return false;
        }
        gameWorld.closeInGameSubmenu(true);
        return true;
    }

    private GameWorldController findGameWorldController() {
        godot.api.Node node = this;
        while (node != null) {
            if (node instanceof GameWorldController controller) {
                return controller;
            }
            node = node.getParent();
        }
        return null;
    }

    private godot.core.StringName bindMethodName(int index) {
        return StringNames.toGodotName("onBind" + index);
    }

    private void stylePanel(godot.api.PanelContainer panel) {
        DispatchUi.stylePanel(panel);
    }

    private void connectBtn(String path, String method) {
        getNode(path).connect("pressed", new MethodCallable0<Void>(this, StringNames.toGodotName(method), new Object[0]));
    }
}
