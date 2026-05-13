package domain;

import java.util.Objects;

public enum Suit {
    CLUBS("♣"),
    DIAMONDS("♦"),
    HEARTS("♥"),
    SPADES("♠");

    private final String symbol;

    Suit(String symbol) {
    	Objects.requireNonNull("Symbol cannot be null");
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}