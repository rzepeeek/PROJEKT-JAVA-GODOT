package cvvl.simulator.player;

import cvvl.simulator.vehicles.Vehicle;
import cvvl.simulator.vehicles.VehicleModelHelper;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.annotation.RegisterSignal;
import godot.api.Camera3D;
import godot.api.CollisionShape3D;
import godot.api.CollisionObject3D;
import godot.api.Input;
import godot.api.Node;
import godot.api.PhysicsRayQueryParameters3D;
import godot.api.World3D;
import godot.core.Dictionary;
import godot.core.Signal1;
import godot.core.StringNames;
import godot.core.Vector3;

@RegisterClass
public class PlayerInteraction extends Node {
	@RegisterSignal
	public final Signal1<Vehicle> vehicleTargeted =
			new Signal1<>(this, StringNames.toGodotName("vehicleTargeted"));

	private Camera3D camera;
	private Vehicle currentTarget;

	@RegisterFunction
	@Override
	public void _ready() {
		camera = (Camera3D) getNode("../Head/Camera3D");
	}

	@RegisterFunction
	@Override
	public void _process(double delta) {
		currentTarget = raycastVehicle();
	}

	@RegisterFunction
	@Override
	public void _unhandledInput(godot.api.InputEvent event) {
		if (event.isActionPressed("interact") && currentTarget != null) {
			currentTarget.syncFineStatusFromGameState();
			vehicleTargeted.emit(currentTarget);
		}
	}

	@RegisterFunction
	public Vehicle getCurrentTarget() {
		return currentTarget;
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
		Vector3 forward = camera.getGlobalTransform().getBasis().getColumn(2).unaryMinus();
		Vector3 to = from.plus(forward.times(8.0));

		PhysicsRayQueryParameters3D params = PhysicsRayQueryParameters3D.create(from, to);
		params.setCollideWithAreas(false);
		params.setCollideWithBodies(true);
		params.setCollisionMask(VehicleModelHelper.VEHICLE_COLLISION_LAYER);

		Dictionary<Object, Object> result = world.getDirectSpaceState().intersectRay(params);
		if (result.isEmpty()) {
			return null;
		}

		Object collider = result.get("collider");
		Node node = resolveColliderNode(collider);
		if (node == null) {
			return null;
		}

		while (node != null) {
			if (node instanceof Vehicle vehicle) {
				return vehicle;
			}
			node = node.getParent();
		}
		return null;
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
