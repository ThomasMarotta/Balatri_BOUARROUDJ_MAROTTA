package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import domain.Card;
import domain.Rank;
import domain.Suit;

public class Deck {
    private final List<Card> cards;

    public Deck() {
        this.cards = new ArrayList<>();
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                this.cards.add(new Card(suit, rank));
            }
        }
        Collections.shuffle(this.cards);
    }

    /** Tire une seule carte du dessus du deck. */
    public Card drawOne() {
        if (cards.isEmpty()) throw new IllegalStateException("Deck vide !");
        return cards.remove(0);
    }

    public int remaining() {
        return cards.size();
    }
}