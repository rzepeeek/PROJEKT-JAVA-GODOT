package cvvl.simulator.vehicles;

import godot.annotation.RegisterClass;
import godot.annotation.RegisterFunction;
import godot.annotation.RegisterProperty;
import godot.api.StaticBody3D;
import godot.core.StringNames;

@RegisterClass
public class Vehicle extends StaticBody3D {
	@RegisterProperty
	public String vehicleId = "";

	@RegisterProperty
	public String plate = "";

	@RegisterProperty
	public float parkingMinutes = 0f;

	@RegisterProperty
	public float timeLimitMinutes = 120f;

	@RegisterProperty
	public String requiredSpotType = "standard";

	@RegisterProperty
	public String actualSpotType = "standard";

	@RegisterProperty
	public TicketType ticketType = TicketType.VALID;

	@RegisterProperty
	public String parkingSpotName = "";

	@RegisterProperty
	public boolean fineIssued = false;

	@RegisterFunction
	public ParkingViolation getActualViolation() {
		if (ticketType == TicketType.NONE || ticketType == TicketType.NO_TICKET) {
			return ParkingViolation.NO_TICKET;
		}
		if (!requiredSpotType.equals(actualSpotType)) {
			return ParkingViolation.WRONG_SPOT;
		}
		if (parkingMinutes > timeLimitMinutes) {
			return ParkingViolation.EXPIRED_TIME;
		}
		return ParkingViolation.VALID;
	}

	@RegisterFunction
	public String getViolationLabel() {
		return switch (getActualViolation()) {
			case NO_TICKET -> "Brak biletu";
			case EXPIRED_TIME -> "Przekroczony czas";
			case WRONG_SPOT -> "Złe miejsce";
			case VALID -> "Prawidłowe parkowanie";
		};
	}

	@RegisterFunction
	public String getTicketTypeLabel() {
		return switch (ticketType) {
			case NONE -> "Brak";
			case VALID -> "Ważny";
			case EXPIRED -> "Wygasły";
			case WRONG_SPOT -> "Niewłaściwy";
			case NO_TICKET -> "Brak biletu";
		};
	}

	@RegisterFunction
	public String getStatusSummary() {
		if (fineIssued) {
			return "Miejsce: " + parkingSpotName + " | Mandat wystawiony";
		}
		return "Miejsce: " + parkingSpotName + " | Bilet: " + getTicketTypeLabel();
	}

	@RegisterFunction
	public void refitCollision() {
		VehicleModelHelper.refitInspectableCollision(this);
	}

	@RegisterFunction
	@Override
	public void _ready() {
		callDeferred(StringNames.toGodotName("refitCollision"));
	}

	@RegisterFunction
	public void syncFineStatusFromGameState() {
		if (cvvl.simulator.GameState.instance != null
				&& cvvl.simulator.GameState.instance.isVehicleFined(plate)) {
			fineIssued = true;
		}
	}
}
