package cvvl.simulator.vehicles;

import cvvl.simulator.GameState;
import cvvl.simulator.data.SavedVehicleData;
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
		"parking_01", "parking_02", "parking_03", "parking_04", "parking_05",
		"parking_06", "parking_07", "parking_08", "parking_09", "parking_10",
		"parking_11", "parking_12", "parking_13", "parking_14", "parking_15",
		"parking_16", "parking_17", "parking_18", "parking_19", "parking_20",
		"parking_21", "parking_22", "parking_23", "parking_24", "parking_25",
		"parking_26", "parking_27", "parking_28", "parking_29", "parking_30",
		"parking_31", "parking_32", "parking_33", "parking_34", "parking_35",
		"parking_36", "parking_37", "parking_38", "parking_39", "parking_40",
		"parking_41", "parking_42", "parking_43", "parking_44", "parking_45",
		"parking_46", "parking_47", "parking_48", "parking_49", "parking_50",
		"parking_51", "parking_52", "parking_53", "parking_54", "parking_55",
		"parking_56", "parking_57", "parking_58", "parking_59", "parking_60",
		"parking_61", "parking_62", "parking_63", "parking_64", "parking_65",
		"parking_66", "parking_67", "parking_68", "parking_69", "parking_70",
		"parking_71", "parking_72", "parking_73", "parking_74", "parking_75",
		"parking_76", "parking_77", "parking_78", "parking_79", "parking_80",
		"parking_81", "parking_82", "parking_83", "parking_84", "parking_85",
		"parking_86", "parking_87", "parking_88", "parking_89", "parking_90",
		"parking_91", "parking_92", "parking_93", "parking_94", "parking_95",
		"parking_96", "parking_97", "parking_98", "parking_99", "parking_100",
		"parking_101", "parking_102", "parking_103", "parking_104", "parking_105",
		"parking_106", "parking_107"
};
private static final String[] SPOT_MODEL_FILES = {
		"sedan1.glb",
		"sedan2.glb",
		"sedan3.glb",
		"bus1.glb",
		"bus2.glb",
		"bus3.glb",
		"suv1.glb"
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

	if (GameState.instance != null && GameState.instance.hasPersistedVehicles()) {
		spawnFromPersisted(GameState.instance.getPersistedVehicles());
		GameState.instance.clearPersistedVehicles();
		return;
	}

	for (int i = 0; i < SPOT_NAMES.length; i++) {
		if (rng.randf() > 0.55f) {
			continue;
		}

		String spotName = SPOT_NAMES[i];
		Marker3D marker = findParkingMarker(spotName);
		if (marker != null) {
			spawnAt(marker, spotName, modelFileForSpotIndex(), null);
		}
	}
}

	@RegisterFunction
	public List<SavedVehicleData> captureSpawnedVehicles() {
		List<SavedVehicleData> snapshots = new ArrayList<>();
		for (Vehicle vehicle : spawned) {
			vehicle.syncFineStatusFromGameState();
			snapshots.add(SavedVehicleData.fromVehicle(vehicle));
		}
		return snapshots;
	}

	@RegisterFunction
	public List<Vehicle> getSpawnedVehicles() {
		return spawned;
	}

	private void spawnFromPersisted(List<SavedVehicleData> snapshots) {
		for (SavedVehicleData snapshot : snapshots) {
			Marker3D marker = findParkingMarker(snapshot.parkingSpotName);
			if (marker != null) {
				spawnAt(marker, snapshot.parkingSpotName, snapshot.modelFile, snapshot);
			}
		}
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

	private String modelFileForSpotIndex() {
	return SPOT_MODEL_FILES[
			rng.randiRange(0, SPOT_MODEL_FILES.length - 1)
	];
	}

	private static String resolveModelPath(String modelFile) {
		String path = VEHICLES_DIR + modelFile;
		if (ResourceLoader.exists(path)) {
			return path;
		}
		String fallback = VEHICLES_DIR + DEFAULT_MODEL_FILE;
		return ResourceLoader.exists(fallback) ? fallback : path;
	}

	private void spawnAt(Marker3D spot, String spotName, String modelFile, SavedVehicleData snapshot) {
		Vehicle vehicle = new Vehicle();
		vehicle.setName("Vehicle_" + spotName);

		if (snapshot != null) {
			snapshot.applyTo(vehicle);
			vehicle.parkingSpotName = spotName;
		} else {
			vehicle.vehicleId = "V-" + rng.randiRange(1000, 9999);
			vehicle.plate = randomPlate();
			vehicle.parkingSpotName = spotName;
			vehicle.requiredSpotType = randomSpotType();
			vehicle.actualSpotType = randomSpotType();
			vehicle.timeLimitMinutes = rng.randiRange(30, 180);
			vehicle.parkingMinutes = rng.randfRange(5f, 240f);
			vehicle.ticketType = randomTicketType();
			assignParkedAt(vehicle);
		}

		String resolvedModel = snapshot != null && snapshot.placeholder
				? ""
				: resolveModelPath(modelFile == null ? "" : modelFile);
		vehicle.modelFile = modelFile == null ? "" : modelFile;

		VehicleModelHelper.configureVehiclePhysics(vehicle);
		addCollisionShape(vehicle, PLACEHOLDER_COLLISION_SIZE, groundedCollisionCenter(PLACEHOLDER_COLLISION_SIZE));

		boolean visualReady = false;
		if (snapshot != null && snapshot.placeholder) {
			buildPlaceholderVisual(vehicle, snapshotPlaceholderColor(snapshot));
			vehicle.usesPlaceholderVisual = true;
			visualReady = true;
		} else if (!resolvedModel.isEmpty() && buildVisualFromModel(vehicle, resolvedModel)) {
			vehicle.usesPlaceholderVisual = false;
			visualReady = true;
		}

		if (!visualReady) {
			Color color = randomCarColor();
			buildPlaceholderVisual(vehicle, color);
			vehicle.usesPlaceholderVisual = true;
			vehicle.placeholderColorR = (float) color.getR();
			vehicle.placeholderColorG = (float) color.getG();
			vehicle.placeholderColorB = (float) color.getB();
		}

		vehicle.syncFineStatusFromGameState();

		// Ten sam rodzic co markery — kopia lokalnego transformu parking_XX
		addChild(vehicle);
		vehicle.setTransform(spot.getTransform());

		Vector3 markerGlobal = spot.getGlobalPosition();
		VehicleModelHelper.placeOnGroundAt(vehicle, markerGlobal);
		VehicleModelHelper.refitInspectableCollision(vehicle);
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
		vehicle.usesPlaceholderVisual = true;
		vehicle.placeholderColorR = (float) color.getR();
		vehicle.placeholderColorG = (float) color.getG();
		vehicle.placeholderColorB = (float) color.getB();
	}

	private static Color snapshotPlaceholderColor(SavedVehicleData snapshot) {
		return new Color(snapshot.placeholderR, snapshot.placeholderG, snapshot.placeholderB, 1f);
	}

	private static void assignParkedAt(Vehicle vehicle) {
		if (GameState.instance == null) {
			return;
		}
		int now = GameState.instance.hour * 60 + GameState.instance.minute;
		int parked = now - (int) vehicle.parkingMinutes;
		while (parked < 0) {
			parked += 24 * 60;
		}
		vehicle.parkedAtHour = (parked / 60) % 24;
		vehicle.parkedAtMinute = parked % 60;
	}

	private static Vector3 groundedCollisionCenter(Vector3 size) {
		return new Vector3(0f, (float) size.getY() * 0.5f, 0f);
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
