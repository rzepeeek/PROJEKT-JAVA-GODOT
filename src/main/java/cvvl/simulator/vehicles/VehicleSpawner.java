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
	/** Wrzuć model do folderu vehicles/ w katalogu projektu (patrz vehicles/README.md). */
	private static final String CUSTOM_VEHICLE_MODEL = "res://vehicles/car.glb";
	private static final Vector3 PLACEHOLDER_COLLISION_SIZE = new Vector3(1.8f, 1.2f, 3.6f);

	private static final String[] SPOT_NAMES = {"parking_01", "parking_02", "parking_03"};
	private static final String[] SPOT_TYPES = {"standard", "disabled", "delivery"};
	private static final String[] PLATE_PREFIX = {"KR", "WW", "GD", "PO", "WA"};

	private final RandomNumberGenerator rng = new RandomNumberGenerator();
	private final List<Vehicle> spawned = new ArrayList<>();

	@RegisterFunction
	@Override
	public void _ready() {
		rng.randomize();
		for (String spotName : SPOT_NAMES) {
			Node3D marker = (Node3D) getNodeOrNull(spotName);
			if (marker instanceof Marker3D parkingSpot) {
				spawnAt(parkingSpot, spotName);
			}
		}
	}

	@RegisterFunction
	public List<Vehicle> getSpawnedVehicles() {
		return spawned;
	}

	private void spawnAt(Marker3D spot, String spotName) {
		Vehicle vehicle = new Vehicle();
		vehicle.setName("Vehicle_" + spotName);
		vehicle.setGlobalTransform(spot.getGlobalTransform());

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

		if (!buildVisualFromCustomModel(vehicle)) {
			buildPlaceholderVisual(vehicle, randomCarColor());
		}
		vehicle.syncFineStatusFromGameState();
		addChild(vehicle);
		spawned.add(vehicle);
	}

	private boolean buildVisualFromCustomModel(Vehicle vehicle) {
		if (!ResourceLoader.exists(CUSTOM_VEHICLE_MODEL)) {
			return false;
		}
		godot.api.Resource resource = ResourceLoader.load(CUSTOM_VEHICLE_MODEL);
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
