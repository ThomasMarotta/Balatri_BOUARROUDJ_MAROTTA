package domain;

import java.util.Objects;

public enum Suit {
	CLUBS("Clubs"),
	DIAMONDS("Diamonds"),
	HEARTS("Hearts"),
	SPADES("Spades");

	private final String symbol;

	Suit(String symbol) {
		Objects.requireNonNull("Symbol cannot be null");
		this.symbol = symbol;
	}

	public String getSymbol() {
		return symbol;
	}
}