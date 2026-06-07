package cvvl.simulator.data;

import java.util.ArrayList;
import java.util.List;

public final class SavedVehicleCodec {
	private static final String RECORD_SEPARATOR = "\n";

	private SavedVehicleCodec() {
	}

	public static String encode(List<SavedVehicleData> vehicles) {
		if (vehicles == null || vehicles.isEmpty()) {
			return "";
		}
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < vehicles.size(); i++) {
			if (i > 0) {
				builder.append(RECORD_SEPARATOR);
			}
			builder.append(vehicles.get(i).serialize());
		}
		return builder.toString();
	}

	public static List<SavedVehicleData> decode(String payload) {
		List<SavedVehicleData> vehicles = new ArrayList<>();
		if (payload == null || payload.isEmpty()) {
			return vehicles;
		}
		for (String line : payload.split(RECORD_SEPARATOR, -1)) {
			if (line.isEmpty()) {
				continue;
			}
			vehicles.add(SavedVehicleData.deserialize(line));
		}
		return vehicles;
	}
}
