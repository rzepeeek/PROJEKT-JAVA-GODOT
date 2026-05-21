package cvvl.simulator.ui;

import cvvl.simulator.DispatchColors;
import godot.api.Button;
import godot.api.Control;
import godot.api.Label;
import godot.api.PanelContainer;
import godot.api.StyleBoxFlat;
import godot.api.Tween;
import godot.core.Color;
import godot.core.Vector2;

public final class DispatchUi {
    private DispatchUi() {}

    public static StyleBoxFlat panelBox(float cornerRadius) {
        StyleBoxFlat box = new StyleBoxFlat();
        box.setBgColor(DispatchColors.BG_PANEL);
        box.setBorderColor(DispatchColors.CYAN_DIM);
        box.setBorderWidthAll(1);
        box.setCornerRadiusAll((int) cornerRadius);
        box.setShadowColor(DispatchColors.ACCENT_GLOW);
        box.setShadowSize(8);
        box.setContentMarginAll(12);
        return box;
    }

    public static StyleBoxFlat buttonNormal() {
        StyleBoxFlat box = new StyleBoxFlat();
        box.setBgColor(new Color(0.05f, 0.08f, 0.14f, 0.75f));
        box.setBorderColor(DispatchColors.CYAN_DIM);
        box.setBorderWidthAll(1);
        box.setCornerRadiusAll(6);
        box.setContentMarginAll(10);
        return box;
    }

    public static StyleBoxFlat buttonHover() {
        StyleBoxFlat box = buttonNormal();
        box.setBorderColor(DispatchColors.CYAN);
        box.setBgColor(new Color(0.08f, 0.14f, 0.22f, 0.9f));
        box.setShadowColor(DispatchColors.ACCENT_GLOW);
        box.setShadowSize(12);
        return box;
    }

    public static void stylePanel(PanelContainer panel) {
        panel.addThemeStyleboxOverride("panel", panelBox(10));
    }

    public static void styleDispatchButton(Button button) {
        button.addThemeStyleboxOverride("normal", buttonNormal());
        button.addThemeStyleboxOverride("hover", buttonHover());
        button.addThemeStyleboxOverride("pressed", buttonHover());
        button.addThemeStyleboxOverride("focus", buttonNormal());
        button.addThemeColorOverride("font_color", DispatchColors.TEXT);
        button.addThemeColorOverride("font_hover_color", DispatchColors.CYAN);
        button.addThemeFontSizeOverride("font_size", 16);
    }

    public static void styleTerminalLabel(Label label, boolean header) {
        label.addThemeColorOverride("font_color", header ? DispatchColors.CYAN : DispatchColors.TEXT);
        label.addThemeFontSizeOverride("font_size", header ? 14 : 13);
    }

    public static void fadeIn(Control node, float duration) {
        node.setModulate(new Color(1, 1, 1, 0));
        Tween tween = node.createTween();
        tween.setEase(Tween.EaseType.OUT);
        tween.setTrans(Tween.TransitionType.CUBIC);
        tween.tweenProperty(node, "modulate:a", 1.0, duration);
    }

    public static void slideIn(Control node, float offsetX, float duration) {
        Vector2 start = node.getPosition();
        node.setPosition(new Vector2(start.getX() + offsetX, start.getY()));
        node.setModulate(new Color(1, 1, 1, 0));
        Tween tween = node.createTween();
        tween.setParallel(true);
        tween.setEase(Tween.EaseType.OUT);
        tween.setTrans(Tween.TransitionType.CUBIC);
        tween.tweenProperty(node, "position", start, duration);
        tween.tweenProperty(node, "modulate:a", 1.0, duration);
    }
}
