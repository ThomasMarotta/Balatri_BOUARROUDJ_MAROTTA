package domain;

import java.util.Objects;

public record Blind(String name, int targetScore) {
	public Blind {
		Objects.requireNonNull(name);
		if (targetScore < 0) {
			throw new IllegalArgumentException("Target score must be positive !");
		}
	}
}
