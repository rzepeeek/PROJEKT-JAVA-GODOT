package cvvl.simulator.vehicles;

import godot.api.BoxShape3D;
import godot.api.CollisionObject3D;
import godot.api.CollisionShape3D;
import godot.api.MeshInstance3D;
import godot.api.Node;
import godot.api.Node3D;
import godot.api.PhysicsRayQueryParameters3D;
import godot.api.RigidBody3D;
import godot.api.StaticBody3D;
import godot.api.World3D;
import godot.core.AABB;
import godot.core.Dictionary;
import godot.core.RID;
import godot.core.Transform3D;
import godot.core.VariantArray;
import godot.core.Vector3;

/**
 * Przygotowuje zaimportowany GLB: usuwa kolizje z modelu, skaluje do rozsądnego rozmiaru
 * i dopasowuje prostokątny collider pod wizual.
 */
public final class VehicleModelHelper {
	public static final int VEHICLE_COLLISION_LAYER = 2;
	/** Warstwa podłoża + domyślna warstwa importowanych meshów mapy. */
	public static final int GROUND_COLLISION_MASK = 1;
	private static final float TARGET_LENGTH_Z = 4.2f;
	private static final float COLLISION_PADDING = 0.12f;
	private static final Vector3 FALLBACK_COLLISION_SIZE = new Vector3(1.8f, 1.4f, 4.2f);
	private static final float GROUND_RAY_TOP = 80f;
	private static final float GROUND_RAY_BOTTOM = -30f;
	private static final float MIN_FLOOR_NORMAL_Y = 0.55f;
	private static final float MAX_GROUND_ABOVE_MARKER = 1.0f;
	private static final float GROUND_SURFACE_OFFSET = 0.04f;
	private VehicleModelHelper() {
	}

	public static Node3D attachVisualModel(Vehicle vehicle, Node instance) {
		Node3D holder = new Node3D();
		holder.setName("BodyModel");
		vehicle.addChild(holder);

		if (instance instanceof StaticBody3D || instance instanceof RigidBody3D) {
			Node3D physicsRoot = (Node3D) instance;
			holder.setTransform(physicsRoot.getTransform());
			transferChildren(physicsRoot, holder);
			physicsRoot.queueFree();
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
		vehicle.setCollisionLayerValue(2, true);
		vehicle.setCollisionMask(0);
		vehicle.setCollisionLayer(VEHICLE_COLLISION_LAYER);
	}

	public static void ensureInspectableCollision(Vehicle vehicle) {
		CollisionShape3D collision = (CollisionShape3D) vehicle.getNodeOrNull("Collision");
		if (collision != null) {
			collision.setDisabled(false);
		}
	}

	/** Dopasowuje collider do wizualu — wywołaj po dodaniu pojazdu do sceny. */
	public static void refitInspectableCollision(Vehicle vehicle) {
		configureVehiclePhysics(vehicle);
		ensureCollisionShape(vehicle);

		Node3D visualRoot = findVisualRoot(vehicle);
		if (visualRoot != null) {
			fitCollisionToMeshes(vehicle, visualRoot);
		}

		CollisionShape3D collision = (CollisionShape3D) vehicle.getNodeOrNull("Collision");
		if (collision == null || collision.getShape() == null || !hasUsableCollisionShape(collision)) {
			applyFallbackCollision(vehicle);
		}
		ensureInspectableCollision(vehicle);
	}

	private static Node3D findVisualRoot(Vehicle vehicle) {
		Node bodyModel = vehicle.getNodeOrNull("BodyModel");
		if (bodyModel instanceof Node3D modelRoot) {
			return modelRoot;
		}
		Node body = vehicle.getNodeOrNull("Body");
		if (body instanceof Node3D placeholderRoot) {
			return placeholderRoot;
		}
		return null;
	}

	private static void ensureCollisionShape(Vehicle vehicle) {
		if (vehicle.getNodeOrNull("Collision") != null) {
			return;
		}
		CollisionShape3D collision = new CollisionShape3D();
		collision.setName("Collision");
		vehicle.addChild(collision);
	}

	private static boolean hasUsableCollisionShape(CollisionShape3D collision) {
		if (collision.getShape() instanceof BoxShape3D box) {
			return (float) box.getSize().length() > 0.2f;
		}
		return collision.getShape() != null;
	}

	private static void applyFallbackCollision(Vehicle vehicle) {
		CollisionShape3D collision = (CollisionShape3D) vehicle.getNodeOrNull("Collision");
		if (collision == null) {
			return;
		}
		Vector3 size = FALLBACK_COLLISION_SIZE;
		Vector3 center = new Vector3(0f, (float) size.getY() * 0.5f, 0f);
		BoxShape3D shape = new BoxShape3D();
		shape.setSize(size);
		collision.setShape(shape);
		collision.setPosition(center);
	}

	/** Ustawia pojazd na podłożu w X/Z markera; dolna krawędź modelu na asfalcie. */
	public static void placeOnGroundAt(Vehicle vehicle, Vector3 markerGlobal) {
		World3D world = vehicle.getWorld3d();
		if (world == null) {
			return;
		}

		float x = (float) markerGlobal.getX();
		float z = (float) markerGlobal.getZ();
		float markerY = (float) markerGlobal.getY();
		float groundY = findWalkableGroundY(world, x, z, markerY);
		snapVehicleBottomToWorldY(vehicle, groundY);
	}

	public static void placeOnGround(Vehicle vehicle) {
		placeOnGroundAt(vehicle, vehicle.getGlobalPosition());
	}

	private static float findWalkableGroundY(World3D world, float x, float z, float markerY) {
		Vector3 from = new Vector3(x, GROUND_RAY_TOP, z);
		Vector3 to = new Vector3(x, GROUND_RAY_BOTTOM, z);
		VariantArray<RID> exclude = new VariantArray<>(RID.class);

		for (int attempt = 0; attempt < 10; attempt++) {
			PhysicsRayQueryParameters3D params = PhysicsRayQueryParameters3D.create(from, to);
			params.setCollideWithAreas(false);
			params.setCollideWithBodies(true);
			params.setCollisionMask(GROUND_COLLISION_MASK);
			if (!exclude.isEmpty()) {
				params.setExclude(exclude);
			}

			Dictionary<Object, Object> result = world.getDirectSpaceState().intersectRay(params);
			if (result.isEmpty()) {
				break;
			}

			float hitY = readHitY(result, from, to);
			float normalY = readHitNormalY(result);
			boolean floorLike = normalY >= MIN_FLOOR_NORMAL_Y;
			boolean notTooHigh = hitY <= markerY + MAX_GROUND_ABOVE_MARKER;

			if (floorLike && notTooHigh) {
				return hitY;
			}

			RID rid = readHitRid(result);
			if (rid == null || !rid.isValid()) {
				break;
			}
			exclude.add(rid);
		}

		return markerY;
	}

	private static void snapVehicleBottomToWorldY(Vehicle vehicle, float groundY) {
		AABB bounds = computeGlobalMeshBounds(vehicle);
		if (bounds == null) {
			Vector3 pos = vehicle.getGlobalPosition();
			vehicle.setGlobalPosition(new Vector3((float) pos.getX(), groundY, (float) pos.getZ()));
			return;
		}

		float bottomY = (float) bounds.getPosition().getY();
		float lift = groundY - bottomY + GROUND_SURFACE_OFFSET;
		Vector3 pos = vehicle.getGlobalPosition();
		vehicle.setGlobalPosition(new Vector3(
				(float) pos.getX(),
				(float) pos.getY() + lift,
				(float) pos.getZ()
		));
	}

	private static AABB computeGlobalMeshBounds(Node3D root) {
		return accumulateGlobalMeshBounds(null, root);
	}

	private static AABB accumulateGlobalMeshBounds(AABB merged, Node node) {
		if (node instanceof MeshInstance3D mesh && mesh.getMesh() != null) {
			AABB local = mesh.getAabb();
			if ((float) local.getSize().length() > 0.001f) {
				Transform3D global = mesh.getGlobalTransform();
				merged = mergeGlobalAabb(merged, local, global);
			}
		}
		for (int i = 0; i < node.getChildCount(); i++) {
			merged = accumulateGlobalMeshBounds(merged, node.getChild(i));
		}
		return merged;
	}

	private static AABB mergeGlobalAabb(AABB merged, AABB local, Transform3D global) {
		AABB transformed = global.times(local);
		if (merged == null) {
			return transformed;
		}
		merged = merged.expand(transformed.getPosition());
		merged = merged.expand(transformed.getPosition().plus(transformed.getSize()));
		return merged;
	}

	private static float readHitNormalY(Dictionary<Object, Object> result) {
		Object normalObj = result.get("normal");
		if (normalObj instanceof Vector3 normal) {
			return (float) normal.getY();
		}
		return 1f;
	}

	private static RID readHitRid(Dictionary<Object, Object> result) {
		Object ridObj = result.get("rid");
		if (ridObj instanceof RID rid) {
			return rid;
		}
		return new RID();
	}

	private static float readHitY(Dictionary<Object, Object> result, Vector3 from, Vector3 to) {
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
			bounds = computeLocalBoundsFromGlobalMeshes(vehicle);
		}
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
		Transform3D rootToSpace = root == space
				? new Transform3D()
				: space.getGlobalTransform().affineInverse().times(((Node3D) root).getGlobalTransform());
		return accumulateBoundsLocal(null, root, rootToSpace);
	}

	private static AABB computeLocalBoundsFromGlobalMeshes(Vehicle vehicle) {
		AABB global = computeGlobalMeshBounds(vehicle);
		if (global == null) {
			return null;
		}
		Transform3D toLocal = vehicle.getGlobalTransform().affineInverse();
		Vector3 p = global.getPosition();
		Vector3 s = global.getSize();
		Vector3[] corners = {
				new Vector3((float) p.getX(), (float) p.getY(), (float) p.getZ()),
				new Vector3((float) (p.getX() + s.getX()), (float) p.getY(), (float) p.getZ()),
				new Vector3((float) p.getX(), (float) (p.getY() + s.getY()), (float) p.getZ()),
				new Vector3((float) p.getX(), (float) p.getY(), (float) (p.getZ() + s.getZ())),
				new Vector3((float) (p.getX() + s.getX()), (float) (p.getY() + s.getY()), (float) (p.getZ() + s.getZ()))
		};
		AABB local = null;
		for (Vector3 corner : corners) {
			Vector3 transformed = toLocal.times(corner);
			if (local == null) {
				local = new AABB(transformed, new Vector3());
			} else {
				local = local.expand(transformed);
			}
		}
		return local;
	}

	private static AABB accumulateBoundsLocal(AABB merged, Node node, Transform3D nodeToSpace) {
		if (node instanceof MeshInstance3D mesh && mesh.getMesh() != null) {
			AABB local = mesh.getAabb();
			if ((float) local.getSize().length() > 0.001f) {
				merged = mergeTransformedAabb(merged, local, nodeToSpace);
			}
		}
		for (int i = 0; i < node.getChildCount(); i++) {
			Node child = node.getChild(i);
			Transform3D childToSpace = nodeToSpace;
			if (child instanceof Node3D child3d) {
				childToSpace = nodeToSpace.times(child3d.getTransform());
			}
			merged = accumulateBoundsLocal(merged, child, childToSpace);
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
