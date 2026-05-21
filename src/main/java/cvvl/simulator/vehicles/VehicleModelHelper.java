package cvvl.simulator.vehicles;

import godot.api.BoxShape3D;
import godot.api.CollisionObject3D;
import godot.api.CollisionShape3D;
import godot.api.MeshInstance3D;
import godot.api.Node;
import godot.api.Node3D;
import godot.api.RigidBody3D;
import godot.api.StaticBody3D;
import godot.core.AABB;
import godot.core.Transform3D;
import godot.core.Vector3;

/**
 * Przygotowuje zaimportowany GLB: usuwa kolizje z modelu, skaluje do rozsądnego rozmiaru
 * i dopasowuje prostokątny collider pod wizual.
 */
public final class VehicleModelHelper {
	public static final int VEHICLE_COLLISION_LAYER = 2;
	private static final float TARGET_LENGTH_Z = 4.2f;
	private static final float COLLISION_PADDING = 0.12f;
	private VehicleModelHelper() {
	}

	public static Node3D attachVisualModel(Vehicle vehicle, Node instance) {
		Node3D holder = new Node3D();
		holder.setName("BodyModel");
		vehicle.addChild(holder);

		if (instance instanceof StaticBody3D || instance instanceof RigidBody3D) {
			transferChildren(instance, holder);
			instance.queueFree();
		} else if (instance instanceof Node3D node3d) {
			holder.addChild(node3d);
		} else {
			holder.addChild(instance);
		}

		stripImportedPhysics(holder);
		scaleToTargetLength(holder);
		sitOnGround(holder);
		fitCollisionToMeshes(vehicle, holder);
		return holder;
	}

	public static void configureVehiclePhysics(Vehicle vehicle) {
		vehicle.setCollisionLayer(VEHICLE_COLLISION_LAYER);
		vehicle.setCollisionMask(0);
	}

	private static void transferChildren(Node from, Node3D to) {
		while (from.getChildCount() > 0) {
			Node child = from.getChild(0);
			from.removeChild(child);
			to.addChild(child);
		}
	}

	private static void stripImportedPhysics(Node root) {
		if (root instanceof CollisionShape3D shape) {
			shape.setDisabled(true);
		} else if (root instanceof CollisionObject3D body) {
			body.setCollisionLayer(0);
			body.setCollisionMask(0);
		}
		for (int i = 0; i < root.getChildCount(); i++) {
			stripImportedPhysics(root.getChild(i));
		}
	}

	private static void scaleToTargetLength(Node3D modelRoot) {
		AABB bounds = computeLocalBounds(modelRoot, modelRoot);
		if (bounds == null) {
			return;
		}
		float lengthZ = (float) bounds.getSize().getZ();
		if (lengthZ < 0.05f) {
			return;
		}
		float factor = TARGET_LENGTH_Z / lengthZ;
		if (factor < 0.55f || factor > 1.8f) {
			Vector3 scale = modelRoot.getScale();
			modelRoot.setScale(new Vector3(
					(float) scale.getX() * factor,
					(float) scale.getY() * factor,
					(float) scale.getZ() * factor
			));
		}
	}

	private static void sitOnGround(Node3D modelRoot) {
		AABB bounds = computeLocalBounds(modelRoot, modelRoot);
		if (bounds == null) {
			return;
		}
		float minY = (float) bounds.getPosition().getY();
		if (Math.abs(minY) < 0.02f) {
			return;
		}
		Vector3 pos = modelRoot.getPosition();
		modelRoot.setPosition(new Vector3((float) pos.getX(), (float) pos.getY() - minY, (float) pos.getZ()));
	}

	private static void fitCollisionToMeshes(Vehicle vehicle, Node3D modelRoot) {
		AABB bounds = computeLocalBounds(vehicle, modelRoot);
		if (bounds == null) {
			return;
		}

		CollisionShape3D collision = (CollisionShape3D) vehicle.getNodeOrNull("Collision");
		if (collision == null) {
			return;
		}

		Vector3 size = bounds.getSize().plus(new Vector3(COLLISION_PADDING, COLLISION_PADDING, COLLISION_PADDING));
		Vector3 center = bounds.getPosition().plus(bounds.getSize().times(0.5f));

		BoxShape3D shape = new BoxShape3D();
		shape.setSize(size);
		collision.setShape(shape);
		collision.setPosition(center);
	}

	private static AABB computeLocalBounds(Node3D space, Node root) {
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
