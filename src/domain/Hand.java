package domain;

import java.util.List;
import java.util.Objects;

public record Hand(List<Card> cards) {
	public Hand{
		Objects.requireNonNull(cards, "List of cards cannot be null");
		if(cards.size() != 5) {
			throw new IllegalArgumentException("A hand must must be exist with five value");
		}
	}
	
	//Todo
}
