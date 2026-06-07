package cvvl.simulator.ui;

import cvvl.simulator.GameState;
import cvvl.simulator.GameWorldController;
import cvvl.simulator.ScenePaths;
import cvvl.simulator.data.SaveSlotData;
import cvvl.simulator.systems.SaveManager;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.Button;
import godot.api.Control;
import godot.api.Input;
import godot.api.Label;
import godot.core.Color;
import godot.core.MethodCallable0;
import godot.core.StringNames;

@RegisterClass
public class SaveMenuController extends Control {
    private Label titleLabel;
    private Label statusLabel;
    private final Label[] slotSummaryLabels = new Label[3];
    private final Label[] slotDateLabels = new Label[3];

    @RegisterFunction
    @Override
    public void _ready() {
        if (findGameWorldController() == null) {
            Input.setMouseMode(Input.MouseMode.VISIBLE);
        }
        setModulate(new Color(1, 1, 1, 1));

        titleLabel = (Label) getNode("Panel/VBox/Title");
        statusLabel = (Label) getNode("Panel/VBox/Status");
        for (int i = 0; i < 3; i++) {
            slotSummaryLabels[i] = (Label) getNode("Panel/VBox/Slots/Slot" + i + "/Summary");
            slotDateLabels[i] = (Label) getNode("Panel/VBox/Slots/Slot" + i + "/Date");
            Button actionBtn = (Button) getNode("Panel/VBox/Slots/Slot" + i + "/ActionBtn");
            DispatchUi.styleDispatchButton(actionBtn);
            final int slot = i;
            actionBtn.connect("pressed", new MethodCallable0<Void>(this, actionMethodName(slot), new Object[0]));
        }

        DispatchUi.stylePanel((godot.api.PanelContainer) getNode("Panel"));
        DispatchUi.styleDispatchButton((Button) getNode("BtnBack"));
        getNode("BtnBack").connect("pressed", new MethodCallable0<Void>(this, StringNames.toGodotName("goBack"), new Object[0]));

        refreshSlots();
        updateTitle();
    }

    @RegisterFunction
    public void onOverlayOpened() {
        Input.setMouseMode(Input.MouseMode.VISIBLE);
        refreshSlots();
        updateTitle();
    }

    @RegisterFunction
    public void onSlot0() { handleSlot(0); }
    @RegisterFunction
    public void onSlot1() { handleSlot(1); }
    @RegisterFunction
    public void onSlot2() { handleSlot(2); }

    @RegisterFunction
    @Override
    public void _unhandledInput(godot.api.InputEvent event) {
        if (!isVisible()) {
            return;
        }
        if (event.isActionPressed("pause") || event.isActionPressed("ui_cancel")) {
            DispatchUi.markInputHandled(this);
            goBack();
        }
    }

    @RegisterFunction
    public void goBack() {
        if (tryCloseInGameOverlay()) {
            return;
        }
        String target = ScenePaths.MAIN_MENU;
        if (GameState.instance != null && GameState.instance.returnScenePath != null) {
            target = GameState.instance.returnScenePath;
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

    private void handleSlot(int slot) {
        if (SaveManager.instance == null) {
            return;
        }
        boolean saveMode = GameState.instance != null && "save".equals(GameState.instance.saveMenuMode);
        SaveSlotData data = SaveManager.instance.loadSlotData(slot);

        if (saveMode) {
            GameWorldController gameWorld = findGameWorldController();
            if (gameWorld != null) {
                gameWorld.captureWorldForSave();
            }
            if (!data.empty && GameState.instance != null
                    && !data.matchesDifficulty(GameState.instance.difficulty)) {
                statusLabel.setText(String.format(
                        "Slot %d jest zapisany na poziomie %s. Bieżąca gra: %s — nie można nadpisać.",
                        slot + 1,
                        data.formatDifficultyLabel(),
                        GameState.instance.formatDifficulty()
                ));
                return;
            }
            SaveManager.instance.saveToSlot(slot);
            if (GameState.instance != null) {
                GameState.instance.markSavedToSlot(slot);
            }
            statusLabel.setText(String.format(
                    "Zapisano w slocie %d (poziom: %s).",
                    slot + 1,
                    GameState.instance != null ? GameState.instance.formatDifficulty() : "—"
            ));
            refreshSlots();
            return;
        }

        if (data.empty) {
            statusLabel.setText("Slot " + (slot + 1) + " jest pusty.");
            return;
        }

        if (SaveManager.instance.loadFromSlot(slot)) {
            if (GameState.instance != null) {
                GameState.instance.reopenPauseAfterReturn = false;
            }
            statusLabel.setText("Wczytano zapis — poziom: " + data.formatDifficultyLabel() + ".");
            getTree().changeSceneToFile(ScenePaths.GAME);
        }
    }

    private void refreshSlots() {
        if (SaveManager.instance == null) {
            return;
        }
        boolean saveMode = GameState.instance != null && "save".equals(GameState.instance.saveMenuMode);
        for (int i = 0; i < 3; i++) {
            SaveSlotData data = SaveManager.instance.loadSlotData(i);
            slotSummaryLabels[i].setText(data.formatSummary());
            slotDateLabels[i].setText("Ostatni zapis: " + data.formatSavedAt());
            Button btn = (Button) getNode("Panel/VBox/Slots/Slot" + i + "/ActionBtn");
            boolean difficultyBlocked = saveMode && !data.empty && GameState.instance != null
                    && !data.matchesDifficulty(GameState.instance.difficulty);
            if (saveMode) {
                btn.setText(difficultyBlocked ? "Inny poziom" : "Zapisz (nadpisz)");
            } else {
                btn.setText(data.empty ? "Pusty slot" : "Wczytaj");
            }
            btn.setDisabled(saveMode && difficultyBlocked);
        }
    }

    private void updateTitle() {
        boolean saveMode = GameState.instance != null && "save".equals(GameState.instance.saveMenuMode);
        titleLabel.setText(saveMode ? "ZAPISZ GRĘ" : "WCZYTAJ GRĘ");
        if (saveMode && GameState.instance != null) {
            statusLabel.setText(String.format(
                    "Bieżący poziom: %s. Możesz nadpisać tylko slot z tym samym poziomem trudności.",
                    GameState.instance.formatDifficulty()
            ));
        } else {
            statusLabel.setText("Wybierz slot do wczytania. Poziom trudności zostanie przywrócony z zapisu.");
        }
    }

    private godot.core.StringName actionMethodName(int slot) {
        return StringNames.toGodotName("onSlot" + slot);
    }
}
