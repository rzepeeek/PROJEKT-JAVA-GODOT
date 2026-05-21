package cvvl.simulator.player;

import cvvl.simulator.systems.SettingsManager;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.annotation.RegisterProperty;
import godot.api.Camera3D;
import godot.api.CharacterBody3D;
import godot.api.Input;
import godot.api.InputEventMouseMotion;
import godot.core.Vector2;
import godot.core.Vector3;

@RegisterClass
public class FpsPlayer extends CharacterBody3D {
	@RegisterProperty
	public float walkSpeed = 5.0f;

	@RegisterProperty
	public float sprintSpeed = 8.5f;

	@RegisterProperty
	public float jumpVelocity = 4.5f;

	@RegisterProperty
	public float gravity = 9.8f;

    private Camera3D camera;
    private float pitch = 0f;
    private boolean gameplayEnabled = true;

    @RegisterFunction
    @Override
    public void _ready() {
		camera = (Camera3D) getNode("Head/Camera3D");
		camera.makeCurrent();
		Input.setMouseMode(Input.MouseMode.CAPTURED);
	}

    @RegisterFunction
    public void setGameplayEnabled(boolean enabled) {
        gameplayEnabled = enabled;
        setProcess(enabled);
        setPhysicsProcess(enabled);
    }

    @RegisterFunction
    @Override
    public void _input(godot.api.InputEvent event) {
        if (!gameplayEnabled) {
            return;
        }
        if (event instanceof InputEventMouseMotion motion) {
			if (Input.getMouseMode() != Input.MouseMode.CAPTURED) {
				return;
			}
			float sens = SettingsManager.instance != null
					? SettingsManager.instance.getMouseSensitivity()
					: 0.003f;
			boolean invert = SettingsManager.instance != null && SettingsManager.instance.isInvertMouse();
			float yMult = invert ? 1f : -1f;

			rotateY((float) -motion.getRelative().getX() * sens);
			pitch += (float) motion.getRelative().getY() * sens * yMult;
			pitch = Math.max(-1.4f, Math.min(1.4f, pitch));
			camera.setRotation(new Vector3(pitch, 0, 0));
		}
	}

    @RegisterFunction
    @Override
    public void _physicsProcess(double delta) {
        if (!gameplayEnabled) {
            return;
        }
        Vector3 velocity = getVelocity();
		if (!isOnFloor()) {
			velocity.setY(velocity.getY() - gravity * (float) delta);
		} else if (Input.isActionJustPressed("jump")) {
			velocity.setY(jumpVelocity);
		}

		Vector2 inputDir = Input.getVector("move_left", "move_right", "move_forward", "move_back");
		Vector3 direction = getTransform().getBasis().times(new Vector3(inputDir.getX(), 0, inputDir.getY()));

		float speed = Input.isActionPressed("sprint") ? sprintSpeed : walkSpeed;
		if (inputDir.length() > 0.1f) {
			direction = direction.normalized();
			velocity.setX(direction.getX() * speed);
			velocity.setZ(direction.getZ() * speed);
		} else {
			velocity.setX(0);
			velocity.setZ(0);
		}

		setVelocity(velocity);
		moveAndSlide();
	}

	@RegisterFunction
	public void setMouseCaptured(boolean captured) {
		Input.setMouseMode(captured ? Input.MouseMode.CAPTURED : Input.MouseMode.VISIBLE);
	}

	@RegisterFunction
	public float getPitch() {
		return pitch;
	}

	@RegisterFunction
	public void setPitch(float value) {
		pitch = Math.max(-1.4f, Math.min(1.4f, value));
		if (camera != null) {
			camera.setRotation(new Vector3(pitch, 0, 0));
		}
	}

	@RegisterFunction
	public void applyTransformState(float x, float y, float z, float rotY, float pitchValue) {
		setGlobalPosition(new Vector3(x, y, z));
		setRotation(new Vector3(0, rotY, 0));
		setPitch(pitchValue);
		setVelocity(new Vector3(0, 0, 0));
	}
}
