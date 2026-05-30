package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import domain.Card;
import domain.Rank;
import domain.Suit;

public class Deck {
    private final List<Card> drawPile;
    private final List<Card> discardPile;

    public Deck() {
        this.drawPile = new ArrayList<Card>();
        this.discardPile = new ArrayList<Card>();
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                this.drawPile.add(new Card(suit, rank));
            }
        }
        Collections.shuffle(this.drawPile);
    }

    /** Tire une seule carte du dessus du deck. */
    public Card drawOne() {
        if (drawPile.isEmpty()) {
        	this.recycleDiscard();
        }
        return drawPile.remove(0);
    }
    
    public void addToDiscard(Card cardToDiscard) {
    	Objects.requireNonNull(cardToDiscard);
    	this.discardPile.add(cardToDiscard);
    }
    
    public void recycleDiscard() {
    	this.drawPile.addAll(discardPile);
    	this.discardPile.clear();
    	Collections.shuffle(this.drawPile);
    	
    }

    public int remaining() {
        return drawPile.size();
    }
}