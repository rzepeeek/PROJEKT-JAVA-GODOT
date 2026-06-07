package cvvl.simulator.data;

import cvvl.simulator.vehicles.TicketType;
import cvvl.simulator.vehicles.Vehicle;

public class SavedVehicleData {
	public String parkingSpotName = "";
	public String modelFile = "";
	public boolean placeholder = false;
	public float placeholderR = 0.5f;
	public float placeholderG = 0.5f;
	public float placeholderB = 0.5f;
	public String vehicleId = "";
	public String plate = "";
	public float parkingMinutes = 0f;
	public float timeLimitMinutes = 120f;
	public String requiredSpotType = "standard";
	public String actualSpotType = "standard";
	public int ticketTypeOrdinal = TicketType.VALID.ordinal();
	public boolean fineIssued = false;
	public int parkedAtHour = 8;
	public int parkedAtMinute = 0;

	public static SavedVehicleData fromVehicle(Vehicle vehicle) {
		SavedVehicleData data = new SavedVehicleData();
		data.parkingSpotName = vehicle.parkingSpotName;
		data.modelFile = vehicle.modelFile == null ? "" : vehicle.modelFile;
		data.placeholder = vehicle.usesPlaceholderVisual;
		data.placeholderR = vehicle.placeholderColorR;
		data.placeholderG = vehicle.placeholderColorG;
		data.placeholderB = vehicle.placeholderColorB;
		data.vehicleId = vehicle.vehicleId;
		data.plate = vehicle.plate;
		data.parkingMinutes = vehicle.parkingMinutes;
		data.timeLimitMinutes = vehicle.timeLimitMinutes;
		data.requiredSpotType = vehicle.requiredSpotType;
		data.actualSpotType = vehicle.actualSpotType;
		data.ticketTypeOrdinal = vehicle.ticketType.ordinal();
		data.fineIssued = vehicle.fineIssued;
		data.parkedAtHour = vehicle.parkedAtHour;
		data.parkedAtMinute = vehicle.parkedAtMinute;
		return data;
	}

	public void applyTo(Vehicle vehicle) {
		vehicle.vehicleId = vehicleId;
		vehicle.plate = plate;
		vehicle.parkingSpotName = parkingSpotName;
		vehicle.requiredSpotType = requiredSpotType;
		vehicle.actualSpotType = actualSpotType;
		vehicle.timeLimitMinutes = timeLimitMinutes;
		vehicle.parkingMinutes = parkingMinutes;
		vehicle.ticketType = ticketTypeFromOrdinal(ticketTypeOrdinal);
		vehicle.fineIssued = fineIssued;
		vehicle.parkedAtHour = parkedAtHour;
		vehicle.parkedAtMinute = parkedAtMinute;
		vehicle.modelFile = modelFile == null ? "" : modelFile;
		vehicle.usesPlaceholderVisual = placeholder;
		vehicle.placeholderColorR = placeholderR;
		vehicle.placeholderColorG = placeholderG;
		vehicle.placeholderColorB = placeholderB;
	}

	private static TicketType ticketTypeFromOrdinal(int ordinal) {
		TicketType[] values = TicketType.values();
		if (ordinal < 0 || ordinal >= values.length) {
			return TicketType.VALID;
		}
		return values[ordinal];
	}

	public String serialize() {
		return String.join("|",
				escape(parkingSpotName),
				escape(modelFile),
				placeholder ? "1" : "0",
				format(placeholderR),
				format(placeholderG),
				format(placeholderB),
				escape(vehicleId),
				escape(plate),
				format(parkingMinutes),
				format(timeLimitMinutes),
				escape(requiredSpotType),
				escape(actualSpotType),
				Integer.toString(ticketTypeOrdinal),
				fineIssued ? "1" : "0",
				Integer.toString(parkedAtHour),
				Integer.toString(parkedAtMinute)
		);
	}

	public static SavedVehicleData deserialize(String line) {
		SavedVehicleData data = new SavedVehicleData();
		if (line == null || line.isEmpty()) {
			return data;
		}
		String[] parts = line.split("\\|", -1);
		if (parts.length < 16) {
			return data;
		}
		data.parkingSpotName = unescape(parts[0]);
		data.modelFile = unescape(parts[1]);
		data.placeholder = "1".equals(parts[2]);
		data.placeholderR = parseFloat(parts[3], 0.5f);
		data.placeholderG = parseFloat(parts[4], 0.5f);
		data.placeholderB = parseFloat(parts[5], 0.5f);
		data.vehicleId = unescape(parts[6]);
		data.plate = unescape(parts[7]);
		data.parkingMinutes = parseFloat(parts[8], 0f);
		data.timeLimitMinutes = parseFloat(parts[9], 120f);
		data.requiredSpotType = unescape(parts[10]);
		data.actualSpotType = unescape(parts[11]);
		data.ticketTypeOrdinal = parseInt(parts[12], TicketType.VALID.ordinal());
		data.fineIssued = "1".equals(parts[13]);
		data.parkedAtHour = parseInt(parts[14], 8);
		data.parkedAtMinute = parseInt(parts[15], 0);
		return data;
	}

	private static String escape(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("\\", "\\\\").replace("|", "\\p");
	}

	private static String unescape(String value) {
		if (value == null) {
			return "";
		}
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (c == '\\' && i + 1 < value.length()) {
				char next = value.charAt(i + 1);
				if (next == '\\') {
					out.append('\\');
					i++;
				} else if (next == 'p') {
					out.append('|');
					i++;
				} else {
					out.append(c);
				}
			} else {
				out.append(c);
			}
		}
		return out.toString();
	}

	private static String format(float value) {
		return String.format(java.util.Locale.ROOT, "%.4f", value);
	}

	private static float parseFloat(String value, float fallback) {
		try {
			return Float.parseFloat(value);
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	private static int parseInt(String value, int fallback) {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return fallback;
		}
	}
}
