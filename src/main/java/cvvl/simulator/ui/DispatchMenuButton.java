package cvvl.simulator.ui;

import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.Button;
import godot.api.Tween;
import godot.core.MethodCallable0;
import godot.core.StringNames;
import godot.core.Vector2;

@RegisterClass
public class DispatchMenuButton extends Button {
	@RegisterFunction
	@Override
	public void _ready() {
		DispatchUi.styleDispatchButton(this);
		setScale(new Vector2(1, 1));
		connect("mouse_entered", new MethodCallable0<Void>(this, StringNames.toGodotName("onHoverIn"), new Object[0]));
		connect("mouse_exited", new MethodCallable0<Void>(this, StringNames.toGodotName("onHoverOut"), new Object[0]));
	}

	@RegisterFunction
	public void onHoverIn() {
		animateScale(1.04f, 0.15f);
	}

	@RegisterFunction
	public void onHoverOut() {
		animateScale(1f, 0.15f);
	}

	private void animateScale(float target, float duration) {
		Tween tween = createTween();
		tween.setEase(Tween.EaseType.OUT);
		tween.setTrans(Tween.TransitionType.CUBIC);
		tween.tweenProperty(this, "scale", new Vector2(target, target), duration);
	}
}
