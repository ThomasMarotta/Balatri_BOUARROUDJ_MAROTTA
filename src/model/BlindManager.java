package model;

import java.util.List;
import java.util.Objects;

import domain.Blind;

public class BlindManager {
	private static final List<Blind> BLINDS = List.of(
			new Blind("Small Blind", 100),
			new Blind("Big Blind", 250),
			new Blind("The Hook", 500),
			new Blind("The Wall", 1000),
			new Blind("The Forax", 2500));

	public Blind getFirstBlind() {
		return BLINDS.get(0);
	}

	public Blind getNextBlind(Blind currentBlind) {
		Objects.requireNonNull(currentBlind);
		var index = BLINDS.indexOf(currentBlind);

		if (index == BLINDS.size() - 1) {
			throw new IllegalStateException("Its already the last blind !");
		}

		return BLINDS.get(index + 1);
	}

	public boolean isLastBlind(Blind currentBlind) {
		Objects.requireNonNull(currentBlind);
		return BLINDS.indexOf(currentBlind) == BLINDS.size() - 1;
	}
}
