package cvvl.simulator.vehicles;

import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.api.BoxMesh;
import godot.api.CollisionShape3D;
import godot.api.MeshInstance3D;
import godot.api.Node;
import godot.api.Node3D;
import godot.api.PackedScene;
import godot.api.ResourceLoader;
import godot.api.StandardMaterial3D;
import godot.api.BoxShape3D;
import godot.api.Marker3D;
import godot.api.RandomNumberGenerator;
import godot.core.Color;
import godot.core.Vector3;

import java.util.ArrayList;
import java.util.List;

@RegisterClass
public class VehicleSpawner extends Node3D {
	private static final String VEHICLES_DIR = "res://vehicles/";
	private static final String DEFAULT_MODEL_FILE = "car.glb";
	private static final Vector3 PLACEHOLDER_COLLISION_SIZE = new Vector3(1.8f, 1.2f, 3.6f);

	private static final String[] SPOT_NAMES = {
			"parking_01", "parking_02", "parking_03",
			"parking_04", "parking_05", "parking_06",
			"parking_07", "parking_08", "parking_09"
	};
	private static final String[] SPOT_MODEL_FILES = {
			"sedan1.glb", "sedan2.glb", "sedan3.glb",
			"van2.glb", "van2.glb", "van3.glb",
			"suv1.glb", "suv2.glb", "suv3.glb"
	};
	private static final String[] SPOT_TYPES = {"standard", "disabled", "delivery"};
	private static final String[] PLATE_PREFIX = {"KR", "WW", "GD", "PO", "WA"};

	private final RandomNumberGenerator rng = new RandomNumberGenerator();
	private final List<Vehicle> spawned = new ArrayList<>();
	private boolean hasSpawned = false;

	@RegisterFunction
	@Override
	public void _ready() {
		rng.randomize();
	}

	@RegisterFunction
	public void spawnAllVehicles() {
		if (hasSpawned) {
			return;
		}
		hasSpawned = true;

		for (int i = 0; i < SPOT_NAMES.length; i++) {
			String spotName = SPOT_NAMES[i];
			Marker3D marker = findParkingMarker(spotName);
			if (marker != null) {
				spawnAt(marker, spotName, modelFileForSpotIndex(i));
			}
		}
	}

	@RegisterFunction
	public List<Vehicle> getSpawnedVehicles() {
		return spawned;
	}

	private Marker3D findParkingMarker(String spotName) {
		Node node = getNodeOrNull(spotName);
		if (node instanceof Marker3D marker) {
			return marker;
		}
		for (int i = 0; i < getChildCount(); i++) {
			Node child = getChild(i);
			if (spotName.equals(child.getName()) && child instanceof Marker3D marker) {
				return marker;
			}
		}
		return null;
	}

	private static String modelFileForSpotIndex(int index) {
		if (index >= 0 && index < SPOT_MODEL_FILES.length) {
			return SPOT_MODEL_FILES[index];
		}
		return DEFAULT_MODEL_FILE;
	}

	private static String resolveModelPath(String modelFile) {
		String path = VEHICLES_DIR + modelFile;
		if (ResourceLoader.exists(path)) {
			return path;
		}
		String fallback = VEHICLES_DIR + DEFAULT_MODEL_FILE;
		return ResourceLoader.exists(fallback) ? fallback : path;
	}

	private void spawnAt(Marker3D spot, String spotName, String modelFile) {
		Vehicle vehicle = new Vehicle();
		vehicle.setName("Vehicle_" + spotName);

		vehicle.vehicleId = "V-" + rng.randiRange(1000, 9999);
		vehicle.plate = randomPlate();
		vehicle.parkingSpotName = spotName;
		vehicle.requiredSpotType = randomSpotType();
		vehicle.actualSpotType = randomSpotType();
		vehicle.timeLimitMinutes = rng.randiRange(30, 180);
		vehicle.parkingMinutes = rng.randfRange(5f, 240f);
		vehicle.ticketType = randomTicketType();

		VehicleModelHelper.configureVehiclePhysics(vehicle);
		addCollisionShape(vehicle, PLACEHOLDER_COLLISION_SIZE, new Vector3(0, 0, 0));
		VehicleModelHelper.ensureInspectableCollision(vehicle);

		if (!buildVisualFromModel(vehicle, resolveModelPath(modelFile))) {
			buildPlaceholderVisual(vehicle, randomCarColor());
		}
		vehicle.syncFineStatusFromGameState();

		// Ten sam rodzic co markery — kopia lokalnego transformu parking_XX
		addChild(vehicle);
		vehicle.setTransform(spot.getTransform());

		Vector3 markerGlobal = spot.getGlobalPosition();
		VehicleModelHelper.placeOnGroundAt(vehicle, markerGlobal);
		spawned.add(vehicle);
	}

	private boolean buildVisualFromModel(Vehicle vehicle, String modelPath) {
		if (!ResourceLoader.exists(modelPath)) {
			return false;
		}
		godot.api.Resource resource = ResourceLoader.load(modelPath);
		if (!(resource instanceof PackedScene scene)) {
			return false;
		}
		Node instance = scene.instantiate();
		if (instance == null) {
			return false;
		}
		VehicleModelHelper.attachVisualModel(vehicle, instance);
		return true;
	}

	private void buildPlaceholderVisual(Vehicle vehicle, Color color) {
		MeshInstance3D body = new MeshInstance3D();
		body.setName("Body");
		BoxMesh mesh = new BoxMesh();
		mesh.setSize(PLACEHOLDER_COLLISION_SIZE);
		body.setMesh(mesh);

		StandardMaterial3D material = new StandardMaterial3D();
		material.setAlbedo(color);
		material.setMetallic(0.35f);
		material.setRoughness(0.45f);
		body.setMaterialOverride(material);
		vehicle.addChild(body);
	}

	private void addCollisionShape(Vehicle vehicle, Vector3 size, Vector3 center) {
		CollisionShape3D collision = new CollisionShape3D();
		collision.setName("Collision");
		BoxShape3D shape = new BoxShape3D();
		shape.setSize(size);
		collision.setShape(shape);
		collision.setPosition(center);
		vehicle.addChild(collision);
	}

	private String randomPlate() {
		return PLATE_PREFIX[rng.randiRange(0, PLATE_PREFIX.length - 1)]
				+ " "
				+ rng.randiRange(10000, 99999);
	}

	private String randomSpotType() {
		return SPOT_TYPES[rng.randiRange(0, SPOT_TYPES.length - 1)];
	}

	private TicketType randomTicketType() {
		return switch (rng.randiRange(0, 4)) {
			case 0 -> TicketType.NONE;
			case 1 -> TicketType.VALID;
			case 2 -> TicketType.EXPIRED;
			case 3 -> TicketType.WRONG_SPOT;
			default -> TicketType.NO_TICKET;
		};
	}

	private Color randomCarColor() {
		float hue = rng.randf();
		return new Color(
				0.35f + hue * 0.4f,
				0.4f + (1f - hue) * 0.35f,
				0.55f + hue * 0.25f,
				1f
		);
	}
}
