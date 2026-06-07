package cvvl.simulator.systems;

public enum DifficultyLevel {
	EASY(0, "Łatwy"),
	NORMAL(1, "Normalny"),
	HARD(2, "Trudny");

	private final int id;
	private final String label;

	DifficultyLevel(int id, String label) {
		this.id = id;
		this.label = label;
	}

	public int getId() {
		return id;
	}

	public String getLabel() {
		return label;
	}

	public static DifficultyLevel fromId(int id) {
		return switch (id) {
			case 0 -> EASY;
			case 2 -> HARD;
			default -> NORMAL;
		};
	}

	public boolean showsExactParkingMinutes() {
		return this == EASY;
	}

	public boolean revealsViolationOnScan() {
		return this == EASY;
	}

	public boolean allowsFineCancel() {
		return this == EASY;
	}

	public float fineCancelSeconds() {
		return 5f;
	}

	public boolean playerChoosesFineReason() {
		return this == NORMAL || this == HARD;
	}

	public int wrongFineReputation() {
		return switch (this) {
			case EASY, NORMAL -> -5;
			case HARD -> -10;
		};
	}

	public int warningReputation() {
		return switch (this) {
			case EASY, NORMAL -> -1;
			case HARD -> -5;
		};
	}

	public float gameTimeTickSeconds() {
		return this == EASY ? 11.125f : 8f;
	}

	public int minutesPerTick() {
		return 5;
	}

	public boolean hidesParkingDuration() {
		return this == HARD;
	}

	public boolean showsParkedAtAndLimit() {
		return this == HARD;
	}

	public boolean showsDailyGoal() {
		return this == HARD;
	}

	public int dailyInspectGoal() {
		return 10;
	}
}
