package view;

import domain.Card;
import domain.Rank;
import domain.Suit;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Dessin d'une carte de poker. */
public final class CardRenderer {

    private static final Color BG       = new Color(250, 248, 240);
    private static final Color BORDER   = new Color(20, 20, 20);
    private static final Color SELECTED = new Color(255, 215, 0);
    private static final Color RED      = new Color(200, 30, 40);
    private static final Color BLACK    = new Color(20, 20, 20);

    private CardRenderer() {}

    public static void draw(Graphics2D g, Card card, int x, int y, int w, int h, boolean selected) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(BG);
        g.fillRoundRect(x, y, w, h, 18, 18);

        g.setStroke(new java.awt.BasicStroke(selected ? 4f : 2f));
        g.setColor(selected ? SELECTED : BORDER);
        g.drawRoundRect(x, y, w, h, 18, 18);

        Color suitColor = (card.suit() == Suit.HEARTS || card.suit() == Suit.DIAMONDS) ? RED : BLACK;
        g.setColor(suitColor);

        String rankStr = rankLabel(card.rank());
        String suitStr = suitSymbol(card.suit());

        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.drawString(rankStr, x + 10, y + 28);
        g.setFont(new Font("SansSerif", Font.PLAIN, 22));
        g.drawString(suitStr, x + 10, y + 54);

        g.setFont(new Font("SansSerif", Font.BOLD, 64));
        int sw = g.getFontMetrics().stringWidth(suitStr);
        g.drawString(suitStr, x + (w - sw) / 2, y + h / 2 + 22);

        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        int rw = g.getFontMetrics().stringWidth(rankStr);
        g.drawString(rankStr, x + w - rw - 10, y + h - 14);
    }

    private static String rankLabel(Rank r) {
        return switch (r) {
            case TWO -> "2";
            case THREE -> "3";
            case FOUR -> "4";
            case FIVE -> "5";
            case SIX -> "6";
            case SEVEN -> "7";
            case EIGHT -> "8";
            case NINE -> "9";
            case TEN -> "10";
            case JACK -> "J";
            case QUEEN -> "Q";
            case KING -> "K";
            case ACE -> "A";
        };
    }

    private static String suitSymbol(Suit s) {
        return switch (s) {
            case HEARTS -> "♥";
            case DIAMONDS -> "♦";
            case CLUBS -> "♣";
            case SPADES -> "♠";
        };
    }
}
