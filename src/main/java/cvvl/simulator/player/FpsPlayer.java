package cvvl.simulator.player;

import cvvl.simulator.systems.SettingsManager;
import cvvl.simulator.vehicles.VehicleModelHelper;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.annotation.RegisterProperty;
import godot.api.Camera3D;
import godot.api.CharacterBody3D;
import godot.api.Control;
import godot.api.Node;
import godot.api.Node3D;
import godot.api.Input;
import godot.api.InputEventMouseMotion;
import godot.api.PhysicsRayQueryParameters3D;
import godot.api.World3D;
import godot.core.Dictionary;
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

	@RegisterProperty
	public float eyeHeightBase = 1.65f;

	private static final float COYOTE_DURATION = 0.12f;
	private static final int GROUND_COLLISION_MASK = 1;

	private Camera3D camera;
	private float pitch = 0f;
	private float coyoteTimeLeft = 0f;
	private boolean gameplayEnabled = true;

	@RegisterFunction
	@Override
	public void _ready() {
		setCollisionMask(GROUND_COLLISION_MASK | VehicleModelHelper.VEHICLE_COLLISION_LAYER);
		setFloorSnapLength(0.25f);
		setFloorMaxAngle((float) Math.toRadians(50));
		applyStandingEyeHeight();
		camera = (Camera3D) getNode("Head/Camera3D");
		camera.makeCurrent();
		Input.setMouseMode(Input.MouseMode.CAPTURED);
	}

	private void applyStandingEyeHeight() {
		Node3D head = (Node3D) getNode("Head");
		head.setPosition(new Vector3(0, eyeHeightBase, 0));
	}

	/** Ustawia postać na wykrytym podłożu (warstwa 1). */
	@RegisterFunction
	public void snapToGround() {
		World3D world = getWorld3d();
		if (world == null) {
			return;
		}

		Vector3 pos = getGlobalPosition();
		Vector3 from = new Vector3((float) pos.getX(), (float) pos.getY() + 3f, (float) pos.getZ());
		Vector3 to = new Vector3((float) pos.getX(), (float) pos.getY() - 12f, (float) pos.getZ());

		PhysicsRayQueryParameters3D params = PhysicsRayQueryParameters3D.create(from, to);
		params.setCollideWithAreas(false);
		params.setCollideWithBodies(true);
		params.setCollisionMask(GROUND_COLLISION_MASK);

		Dictionary<Object, Object> result = world.getDirectSpaceState().intersectRay(params);
		if (result.isEmpty()) {
			return;
		}

		float groundY = readGroundY(result, from, to);
		setGlobalPosition(new Vector3((float) pos.getX(), groundY, (float) pos.getZ()));
		setVelocity(new Vector3(0, 0, 0));
		coyoteTimeLeft = COYOTE_DURATION;
	}

	private static float readGroundY(Dictionary<Object, Object> result, Vector3 from, Vector3 to) {
		Object positionObj = result.get("position");
		if (positionObj instanceof Vector3 hitPos) {
			return (float) hitPos.getY();
		}
		Object fractionObj = result.get("fraction");
		if (fractionObj instanceof Number fraction) {
			float t = (float) fraction.doubleValue();
			return (float) (from.getY() + (to.getY() - from.getY()) * t);
		}
		return (float) from.getY();
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
			return;
		}

		if (isTicketPanelOpen()) {
			return;
		}

		Node interactionNode = getNode("Interaction");
		if (interactionNode instanceof PlayerInteraction interaction
				&& interaction.handleInspectInput(event)) {
			getViewport().setInputAsHandled();
		}
	}

	private boolean isTicketPanelOpen() {
		Node game = getParent();
		if (game == null) {
			return false;
		}
		Node ticketPanel = game.getNodeOrNull("UI/TicketPanel");
		return ticketPanel instanceof Control panel && panel.isVisible();
	}

	@RegisterFunction
	@Override
	public void _physicsProcess(double delta) {
		if (!gameplayEnabled) {
			return;
		}
		Vector3 velocity = getVelocity();
		float dt = (float) delta;

		if (isOnFloor()) {
			coyoteTimeLeft = COYOTE_DURATION;
		} else {
			coyoteTimeLeft = Math.max(0f, coyoteTimeLeft - dt);
			velocity.setY(velocity.getY() - gravity * dt);
		}

		if (coyoteTimeLeft > 0f && Input.isActionJustPressed("jump")) {
			velocity.setY(jumpVelocity);
			coyoteTimeLeft = 0f;
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
