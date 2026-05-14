package model;

import java.util.HashSet;
import java.util.Set;

import domain.Card;
import domain.Rank;
import domain.Suit;

public class Deck {
	private Set<Card> cards;
	
	public Deck() {
		this.cards = new HashSet<Card>();
		initializeDeck();
	}
	
	private void initializeDeck() {
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                this.cards.add(new Card(suit, rank));
            }
        }
    }
}
