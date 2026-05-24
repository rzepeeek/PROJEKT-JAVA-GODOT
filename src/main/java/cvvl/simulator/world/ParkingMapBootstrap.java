package cvvl.simulator.world;

import cvvl.simulator.vehicles.VehicleSpawner;
import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.BoxShape3D;
import godot.api.CollisionShape3D;
import godot.api.MeshInstance3D;
import godot.api.Node;
import godot.api.Node3D;
import godot.api.StaticBody3D;
import godot.core.AABB;
import godot.core.Transform3D;
import godot.core.Vector3;

/**
 * Po wczytaniu GLB ustawia mapę na podłożu (Y=0), centruje w XZ i dodaje płaską kolizję podłoża.
 */
@RegisterClass
public class ParkingMapBootstrap extends Node3D {
	private static final int GROUND_LAYER = 1;

	@RegisterFunction
	@Override
	public void _ready() {
		Node model = getNodeOrNull("MapModel");
		if (model == null) {
			return;
		}

		AABB bounds = computeBounds(this, model);
		if (bounds == null) {
			return;
		}

		Vector3 min = bounds.getPosition();
		Vector3 size = bounds.getSize();
		Vector3 center = min.plus(size.times(0.5f));

		Vector3 pos = getPosition();
		setPosition(new Vector3(
				(float) pos.getX() - (float) center.getX(),
				(float) pos.getY() - (float) min.getY(),
				(float) pos.getZ() - (float) center.getZ()
		));

		ensureMapFloor((float) size.getX(), (float) size.getZ());
		spawnVehiclesOnMap();
	}

	private void spawnVehiclesOnMap() {
		for (int i = 0; i < getChildCount(); i++) {
			Node child = getChild(i);
			if (child instanceof VehicleSpawner spawner) {
				spawner.spawnAllVehicles();
				return;
			}
		}
	}

	private void ensureMapFloor(float mapWidth, float mapDepth) {
		if (getNodeOrNull("MapFloor") != null) {
			return;
		}

		float width = Math.max(24f, mapWidth + 6f);
		float depth = Math.max(24f, mapDepth + 6f);

		StaticBody3D floor = new StaticBody3D();
		floor.setName("MapFloor");
		floor.setCollisionLayer(GROUND_LAYER);
		floor.setCollisionMask(0);

		CollisionShape3D collision = new CollisionShape3D();
		BoxShape3D shape = new BoxShape3D();
		shape.setSize(new Vector3(width, 0.35f, depth));
		collision.setShape(shape);
		collision.setPosition(new Vector3(0f, -0.18f, 0f));
		floor.addChild(collision);
		addChild(floor);
	}

	private static AABB computeBounds(Node3D space, Node root) {
		return accumulateBounds(null, root, space);
	}

	private static AABB accumulateBounds(AABB merged, Node node, Node3D space) {
		if (node instanceof MeshInstance3D mesh && mesh.getMesh() != null) {
			AABB local = mesh.getAabb();
			if ((float) local.getSize().length() > 0.001f) {
				Transform3D toSpace = space.getGlobalTransform()
						.affineInverse()
						.times(mesh.getGlobalTransform());
				merged = mergeTransformedAabb(merged, local, toSpace);
			}
		}
		for (int i = 0; i < node.getChildCount(); i++) {
			merged = accumulateBounds(merged, node.getChild(i), space);
		}
		return merged;
	}

	private static AABB mergeTransformedAabb(AABB merged, AABB local, Transform3D transform) {
		AABB transformed = transform.times(local);
		if (merged == null) {
			return transformed;
		}
		merged = merged.expand(transformed.getPosition());
		merged = merged.expand(transformed.getPosition().plus(transformed.getSize()));
		return merged;
	}
}
