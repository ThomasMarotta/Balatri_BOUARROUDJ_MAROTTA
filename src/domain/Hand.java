package domain;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public record Hand(List<Card> cards) {
	public Hand(List<Card> cards) {
		Objects.requireNonNull(cards, "List of cards cannot be null");
		if (cards.size() != 5) {
			throw new IllegalArgumentException("A hand must contain exactly five cards");
		}
		this.cards = Collections.unmodifiableList(cards);
	}

	/*
	 * sort by color and by rank public static List<Card> sortByColor(List<Card>
	 * cards) { Objects.requireNonNull(cards); Comparator<Card> parCouleur =
	 * Comparator .comparing(Card::suit) .thenComparing(Card::rank);
	 * cards.sort(parCouleur); return cards; }
	 */

	public static List<Card> sortByRank(List<Card> cards) {
		Objects.requireNonNull(cards);
		Comparator<Card> parCouleur = Comparator.comparing(Card::rank);
		cards.sort(parCouleur);
		return cards;
	}

	public static List<Card> sortByColor(List<Card> cards) {
		Objects.requireNonNull(cards);
		Comparator<Card> parCouleur = Comparator.comparing(Card::suit);
		cards.sort(parCouleur);
		return cards;
	}

}
