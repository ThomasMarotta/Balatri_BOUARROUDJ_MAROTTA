package domain;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record Hand(List<Card> cards) {
	public Hand(List<Card> cards){
		Objects.requireNonNull(cards, "List of cards cannot be null");
		if(cards.size() != 5) {
			throw new IllegalArgumentException("A hand must contain exactly five cards");
		}
		this.cards = Collections.unmodifiableList(cards);
	}
}
