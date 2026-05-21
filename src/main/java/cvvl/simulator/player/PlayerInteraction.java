package cvvl.simulator.player;

import cvvl.simulator.vehicles.Vehicle;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.annotation.RegisterSignal;
import godot.api.Camera3D;
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
		Vector3 to = from.plus(forward.times(4.0));

		PhysicsRayQueryParameters3D params = PhysicsRayQueryParameters3D.create(from, to);
		params.setCollideWithAreas(true);
		params.setCollideWithBodies(true);

		Dictionary<Object, Object> result = world.getDirectSpaceState().intersectRay(params);
		if (result.isEmpty()) {
			return null;
		}

		Object collider = result.get("collider");
		if (!(collider instanceof Node node)) {
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
}
