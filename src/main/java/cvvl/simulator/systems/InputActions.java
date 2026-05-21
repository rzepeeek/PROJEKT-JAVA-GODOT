package cvvl.simulator.systems;

public final class InputActions {
    public static final String MOVE_FORWARD = "move_forward";
    public static final String MOVE_BACK = "move_back";
    public static final String MOVE_LEFT = "move_left";
    public static final String MOVE_RIGHT = "move_right";
    public static final String INTERACT = "interact";
    public static final String PAUSE = "pause";
    public static final String SPRINT = "sprint";
    public static final String JUMP = "jump";

    public static final String[] BINDABLE = {
            MOVE_FORWARD, MOVE_BACK, MOVE_LEFT, MOVE_RIGHT,
            INTERACT, PAUSE, SPRINT, JUMP
    };

    public static final String[] BIND_LABELS = {
            "Ruch do przodu", "Ruch do tyłu", "Ruch w lewo", "Ruch w prawo",
            "Interakcja (pojazd)", "Menu / pauza", "Sprint", "Skok"
    };

    private InputActions() {}
}
