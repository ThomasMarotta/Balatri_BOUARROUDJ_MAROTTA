package view;

import java.awt.Rectangle;

/**
 * Regroupe tous les rectangles cliquables et leurs positions.
 * Recalculé à chaque changement de taille d'écran.
 */
public final class Layout {

    public static final int CARD_W = 110;
    public static final int CARD_H = 160;
    public static final int CARD_GAP = 18;
    public static final int CARD_LIFT = 30;

    public final int screenW;
    public final int screenH;

    public final Rectangle playBtn;
    public final Rectangle discardBtn;
    public final Rectangle planetBtn;
    public final Rectangle quitBtn;
    public final Rectangle nextBtn;

    public Layout(int screenW, int screenH) {
        this.screenW = screenW;
        this.screenH = screenH;

        int btnW = 200;
        int btnH = 60;
        int rightX = screenW - btnW - 40;

        this.playBtn    = new Rectangle(rightX, screenH - 260, btnW, btnH);
        this.discardBtn = new Rectangle(rightX, screenH - 180, btnW, btnH);
        this.planetBtn  = new Rectangle(screenW - 220, 30, 180, 50);
        this.quitBtn    = new Rectangle(40, 30, 120, 50);
        this.nextBtn    = new Rectangle(screenW / 2 - 120, screenH / 2 + 80, 240, 70);
    }

    /** Position X de la carte i parmi handSize cartes, centrées en bas. */
    public int cardX(int i, int handSize) {
        int totalW = handSize * CARD_W + (handSize - 1) * CARD_GAP;
        int startX = (screenW - totalW) / 2;
        return startX + i * (CARD_W + CARD_GAP);
    }

    public int cardY(boolean selected) {
        int base = screenH - CARD_H - 60;
        return selected ? base - CARD_LIFT : base;
    }

    /** Retourne l'indice de la carte cliquée, ou -1. */
    public int hitCard(int mx, int my, int handSize, java.util.Set<Integer> selected) {
        for (int i = 0; i < handSize; i++) {
            int x = cardX(i, handSize);
            int y = cardY(selected.contains(i));
            if (mx >= x && mx <= x + CARD_W && my >= y && my <= y + CARD_H) {
                return i;
            }
        }
        return -1;
    }

    /** Rectangle d'une carte de l'offre Planet (3 cartes centrées). */
    public Rectangle planetOfferRect(int i, int total) {
        int w = 260;
        int h = 320;
        int gap = 40;
        int totalW = total * w + (total - 1) * gap;
        int startX = (screenW - totalW) / 2;
        int y = (screenH - h) / 2;
        return new Rectangle(startX + i * (w + gap), y, w, h);
    }

    public static boolean inside(Rectangle r, int x, int y) {
        return r.contains(x, y);
    }
}
