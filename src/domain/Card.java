package domain;

import java.util.Objects;

public record Card(Rank suit, Rank rank) {
	public Card{
		Objects.requireNonNull(suit);
		Objects.requireNonNull(rank);
	}
}
