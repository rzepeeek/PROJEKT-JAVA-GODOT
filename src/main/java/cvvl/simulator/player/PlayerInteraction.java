package cvvl.simulator.player;

import cvvl.simulator.GameWorldController;
import cvvl.simulator.vehicles.Vehicle;
import cvvl.simulator.vehicles.VehicleModelHelper;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.annotation.RegisterSignal;
import godot.api.Camera3D;
import godot.api.CollisionShape3D;
import godot.api.CollisionObject3D;
import godot.api.Input;
import godot.api.InputEventMouseButton;
import godot.api.Node;
import godot.api.PhysicsRayQueryParameters3D;
import godot.api.World3D;
import godot.core.Dictionary;
import godot.core.Signal1;
import godot.core.StringNames;
import godot.core.Vector3;

@RegisterClass
public class PlayerInteraction extends Node {
	/** Odległość od środka auta — mandat / inspekcja (m). */
	public static final float MAX_INSPECT_DISTANCE = 3.0f;
	private static final float MIN_LOOK_DOT = 0.78f;
	private static final float RAY_LENGTH = 8f;
	private static final int MOUSE_BUTTON_LEFT = 1;

	@RegisterSignal
	public final Signal1<Vehicle> vehicleTargeted =
			new Signal1<>(this, StringNames.toGodotName("vehicleTargeted"));

	private Camera3D camera;
	private GameWorldController gameWorld;
	private Vehicle currentTarget;

	@RegisterFunction
	@Override
	public void _ready() {
		camera = (Camera3D) getNode("../Head/Camera3D");
		Node game = getParent().getParent();
		if (game instanceof GameWorldController controller) {
			gameWorld = controller;
		}
	}

	@RegisterFunction
	@Override
	public void _process(double delta) {
		currentTarget = findInspectableVehicle();
	}

	@RegisterFunction
	public boolean handleInspectInput(godot.api.InputEvent event) {
		if (!isInspectTrigger(event)) {
			return false;
		}
		Vehicle vehicle = findInspectableVehicle();
		if (vehicle == null) {
			return false;
		}
		vehicle.syncFineStatusFromGameState();
		if (gameWorld != null) {
			gameWorld.openTicketForVehicle(vehicle);
		} else {
			vehicleTargeted.emit(vehicle);
		}
		return true;
	}

	@RegisterFunction
	public Vehicle getCurrentTarget() {
		return currentTarget;
	}

	/** Czy gracz jest blisko i patrzy na auto (otwarcie panelu). */
	@RegisterFunction
	public boolean canInspectVehicle(Vehicle vehicle) {
		if (vehicle == null || camera == null) {
			return false;
		}
		return isWithinInspectRange(vehicle);
	}

	/** Tylko odległość — używane gdy panel mandatu jest już otwarty. */
	@RegisterFunction
	public boolean isVehicleWithinInspectDistance(Vehicle vehicle) {
		if (vehicle == null || camera == null) {
			return false;
		}
		Vector3 from = camera.getGlobalPosition();
		Vector3 center = vehicleCenterWorld(vehicle);
		return (float) from.distanceTo(center) <= MAX_INSPECT_DISTANCE + 0.4f;
	}

	private boolean isInspectTrigger(godot.api.InputEvent event) {
		if (Input.isActionJustPressed("interact")) {
			return true;
		}
		if (event instanceof InputEventMouseButton mouse) {
			return mouse.isPressed() && mouse.getButtonIndex().getValue() == MOUSE_BUTTON_LEFT;
		}
		return false;
	}

	private Vehicle findInspectableVehicle() {
		Vehicle hit = raycastVehicle();
		if (hit == null) {
			return null;
		}
		return isWithinInspectRange(hit) ? hit : null;
	}

	private boolean isWithinInspectRange(Vehicle vehicle) {
		Vector3 from = camera.getGlobalPosition();
		Vector3 forward = cameraForward();
		Vector3 center = vehicleCenterWorld(vehicle);

		float dist = (float) from.distanceTo(center);
		if (dist > MAX_INSPECT_DISTANCE) {
			return false;
		}

		Vector3 toCenter = center.minus(from);
		if ((float) toCenter.lengthSquared() < 1e-4f) {
			return true;
		}
		Vector3 centerDir = toCenter.normalized();
		return (float) forward.dot(centerDir) >= MIN_LOOK_DOT;
	}

	private Vector3 cameraForward() {
		return camera.getGlobalTransform().getBasis().getColumn(2).unaryMinus().normalized();
	}

	private Vehicle raycastVehicle() {
		if (camera == null) {
			return null;
		}
		World3D world = getViewport().getWorld3d();
		if (world == null) {
			return null;
		}

		Vector3 from = camera.getGlobalPosition();
		Vector3 forward = cameraForward();
		Vector3 to = from.plus(forward.times(RAY_LENGTH));

		PhysicsRayQueryParameters3D params = PhysicsRayQueryParameters3D.create(from, to);
		params.setCollideWithAreas(false);
		params.setCollideWithBodies(true);
		params.setCollisionMask(VehicleModelHelper.VEHICLE_COLLISION_LAYER);

		Dictionary<Object, Object> result = world.getDirectSpaceState().intersectRay(params);
		if (result.isEmpty()) {
			return null;
		}

		Object collider = result.get("collider");
		return findVehicleFromCollider(collider);
	}

	private Vehicle findVehicleFromCollider(Object collider) {
		Node node = resolveColliderNode(collider);
		while (node != null) {
			if (node instanceof Vehicle vehicle) {
				return vehicle;
			}
			node = node.getParent();
		}
		return null;
	}

	private static Vector3 vehicleCenterWorld(Vehicle vehicle) {
		CollisionShape3D collision = (CollisionShape3D) vehicle.getNodeOrNull("Collision");
		if (collision != null) {
			return collision.getGlobalTransform().getOrigin();
		}
		return vehicle.getGlobalPosition().plus(new Vector3(0f, 0.9f, 0f));
	}

	private Node resolveColliderNode(Object collider) {
		if (collider instanceof CollisionShape3D shape) {
			Node parent = shape.getParent();
			return parent instanceof Node n ? n : null;
		}
		if (collider instanceof CollisionObject3D body) {
			return body;
		}
		if (collider instanceof Node node) {
			return node;
		}
		return null;
	}
}
