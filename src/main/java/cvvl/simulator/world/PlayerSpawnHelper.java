package cvvl.simulator.world;

import cvvl.simulator.player.FpsPlayer;
import godot.api.Marker3D;
import godot.api.Node;
import godot.core.Transform3D;
import godot.core.Vector3;

public final class PlayerSpawnHelper {
	public static final String MARKER_NAME = "spawn_player";
	public static final String DEFAULT_MARKER_PATH = "World/ParkingMap/spawn_player2";

	private PlayerSpawnHelper() {}

	public static Marker3D findSpawnMarker(Node sceneRoot) {
		if (sceneRoot == null) {
			return null;
		}
		Node byPath = sceneRoot.getNodeOrNull(DEFAULT_MARKER_PATH);
		if (byPath instanceof Marker3D marker) {
			return marker;
		}
		return findSpawnMarkerRecursive(sceneRoot);
	}

	private static Marker3D findSpawnMarkerRecursive(Node root) {
		if (hasName(root, MARKER_NAME) && root instanceof Marker3D marker) {
			return marker;
		}
		for (int i = 0; i < root.getChildCount(); i++) {
			Marker3D found = findSpawnMarkerRecursive(root.getChild(i));
			if (found != null) {
				return found;
			}
		}
		return null;
	}

	private static boolean hasName(Node node, String name) {
		Object nodeName = node.getName();
		return nodeName != null && name.equals(nodeName.toString());
	}

	public static void applyMarkerToPlayer(FpsPlayer player, Marker3D spawn) {
		if (player == null || spawn == null) {
			return;
		}
		Transform3D transform = spawn.getGlobalTransform();
		Vector3 pos = transform.getOrigin();
		Vector3 forward = transform.getBasis().getColumn(2).unaryMinus().normalized();
		float rotY = (float) Math.atan2(forward.getX(), forward.getZ());
		player.applyTransformState(
				(float) pos.getX(),
				(float) pos.getY(),
				(float) pos.getZ(),
				rotY,
				0f
		);
	}
}
