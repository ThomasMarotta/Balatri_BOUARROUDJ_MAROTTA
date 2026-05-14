package view;

import com.github.forax.zen.ApplicationContext;
import com.github.forax.zen.Event;
import com.github.forax.zen.KeyboardEvent;
import com.github.forax.zen.PointerEvent;
import com.github.forax.zen.ScreenInfo;

import controller.GameController;
import controller.GameController.DiscardResult;
import controller.GameController.PlayResult;
import domain.Card;
import domain.EvaluatedHand;
import domain.HandRank;
import domain.Planet;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Vue graphique zen6 du jeu.
 * Pilote la state machine : MENU -> PLAYING -> PLANET_PICK -> PLAYING -> ...
 *                                       \-> ROUND_LOST -> GAME_OVER
 *                                       \-> ALL_BLINDS_WON
 */
public final class BalatriView {

    private enum Screen {
        MENU, PLAYING, PLANET_PICK, ROUND_LOST, ALL_BLINDS_WON
    }

    private static final Color BG          = new Color(28, 70, 50);
    private static final Color PANEL       = new Color(0, 0, 0, 140);
    private static final Color TEXT        = new Color(245, 245, 245);
    private static final Color ACCENT      = new Color(255, 200, 80);
    private static final Color BTN         = new Color(60, 60, 80);
    private static final Color BTN_HOT     = new Color(90, 90, 130);
    private static final Color BTN_DISABLED = new Color(50, 50, 50);

    private final GameController game = new GameController();
    private final Set<Integer> selected = new HashSet<>();

    private Screen screen = Screen.MENU;
    private Layout layout;

    private EvaluatedHand lastEvaluated;
    private int lastScore;
    private String errorMsg;

    private List<Planet> planetOffer;
    private boolean showPlanetLevels;

    private int mouseX, mouseY;

    public void run(ApplicationContext ctx) {
        ScreenInfo si = ctx.getScreenInfo();
        layout = new Layout(si.width(), si.height());

        for (;;) {
            Event e = ctx.pollOrWaitEvent(20);
            if (e instanceof KeyboardEvent ke) {
                if (ke.action() == KeyboardEvent.Action.KEY_PRESSED
                        && ke.key() == KeyboardEvent.Key.ESCAPE) {
                    ctx.dispose();
                    return;
                }
            } else if (e instanceof PointerEvent pe) {
                mouseX = pe.location().x();
                mouseY = pe.location().y();
                if (pe.action() == PointerEvent.Action.POINTER_DOWN) {
                    if (handleClick(mouseX, mouseY)) {
                        ctx.dispose();
                        return;
                    }
                }
            }
            ctx.renderFrame(this::render);
        }
    }

    /** Retourne true si l'app doit se fermer. */
    private boolean handleClick(int x, int y) {
        if (Layout.inside(layout.quitBtn, x, y)) {
            return true;
        }
        return switch (screen) {
            case MENU -> { screen = Screen.PLAYING; yield false; }
            case PLAYING -> { handlePlayingClick(x, y); yield false; }
            case PLANET_PICK -> { handlePlanetClick(x, y); yield false; }
            case ROUND_LOST, ALL_BLINDS_WON -> true;
        };
    }

    private void handlePlayingClick(int x, int y) {
        if (Layout.inside(layout.planetBtn, x, y)) {
            showPlanetLevels = !showPlanetLevels;
            return;
        }
        if (Layout.inside(layout.playBtn, x, y) && selected.size() == 5) {
            tryPlay();
            return;
        }
        if (Layout.inside(layout.discardBtn, x, y)
                && !selected.isEmpty()
                && game.getDiscardsLeft() > 0) {
            tryDiscard();
            return;
        }
        int idx = layout.hitCard(x, y, game.getHand().size(), selected);
        if (idx >= 0) {
            if (selected.contains(idx)) {
                selected.remove(idx);
            } else if (selected.size() < 5) {
                selected.add(idx);
            }
        }
    }

    private void tryPlay() {
        try {
            List<Integer> picked = new ArrayList<>(selected);
            PlayResult r = game.playHand(picked);
            lastEvaluated = r.evaluatedHand();
            lastScore = r.handScore();
            errorMsg = null;
            selected.clear();
            if (r.roundWon()) {
                buildPlanetOffer();
                screen = Screen.PLANET_PICK;
            } else if (r.handsLeft() == 0) {
                screen = Screen.ROUND_LOST;
            }
        } catch (IllegalArgumentException | IllegalStateException ex) {
            errorMsg = ex.getMessage();
        }
    }

    private void tryDiscard() {
        try {
            List<Integer> picked = new ArrayList<>(selected);
            DiscardResult r = game.discard(picked);
            errorMsg = null;
            selected.clear();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            errorMsg = ex.getMessage();
        }
    }

    private void buildPlanetOffer() {
        List<Planet> all = new ArrayList<>(List.of(Planet.values()));
        Collections.shuffle(all);
        planetOffer = all.subList(0, Math.min(3, all.size()));
    }

    private void handlePlanetClick(int x, int y) {
        for (int i = 0; i < planetOffer.size(); i++) {
            if (layout.planetOfferRect(i, planetOffer.size()).contains(x, y)) {
                game.applyPlanet(planetOffer.get(i));
                advanceRound();
                return;
            }
        }
        Rectangle skip = new Rectangle(layout.screenW / 2 - 120,
                layout.screenH - 140, 240, 70);
        if (skip.contains(x, y)) {
            advanceRound();
        }
    }

    private void advanceRound() {
        boolean more = game.nextRound();
        if (!more) {
            screen = Screen.ALL_BLINDS_WON;
        } else {
            lastEvaluated = null;
            lastScore = 0;
            screen = Screen.PLAYING;
        }
    }

    private void render(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setColor(BG);
        g.fillRect(0, 0, layout.screenW, layout.screenH);

        switch (screen) {
            case MENU -> renderMenu(g);
            case PLAYING -> renderPlaying(g);
            case PLANET_PICK -> renderPlanetPick(g);
            case ROUND_LOST -> renderRoundLost(g);
            case ALL_BLINDS_WON -> renderVictory(g);
        }

        drawButton(g, layout.quitBtn, "Quitter", true);
    }

    private void renderMenu(Graphics2D g) {
        g.setColor(ACCENT);
        g.setFont(new Font("SansSerif", Font.BOLD, 96));
        String title = "BALATRI";
        int tw = g.getFontMetrics().stringWidth(title);
        g.drawString(title, (layout.screenW - tw) / 2, layout.screenH / 2 - 40);

        g.setColor(TEXT);
        g.setFont(new Font("SansSerif", Font.PLAIN, 28));
        String prompt = "Cliquez n'importe où pour commencer";
        int pw = g.getFontMetrics().stringWidth(prompt);
        g.drawString(prompt, (layout.screenW - pw) / 2, layout.screenH / 2 + 40);
    }

    private void renderPlaying(Graphics2D g) {
        renderHeader(g);
        renderHand(g);
        renderLastHand(g);
        renderButtons(g);
        if (errorMsg != null) {
            g.setColor(new Color(255, 100, 100));
            g.setFont(new Font("SansSerif", Font.BOLD, 20));
            g.drawString("⚠ " + errorMsg, 40, layout.screenH - 30);
        }
        if (showPlanetLevels) {
            renderPlanetLevelsOverlay(g);
        }
    }

    private void renderHeader(Graphics2D g) {
        g.setColor(PANEL);
        g.fillRoundRect(180, 20, layout.screenW - 420, 90, 12, 12);

        g.setColor(ACCENT);
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.drawString("Round " + game.getRoundNumber() + " — "
                + game.getCurrentBlind().name(), 210, 55);

        g.setColor(TEXT);
        g.setFont(new Font("SansSerif", Font.PLAIN, 20));
        int target = game.getCurrentBlind().targetScore();
        int score = game.getGameScore();
        g.drawString("Score : " + score + " / " + target, 210, 90);
        g.drawString("Mains : " + game.getHandsLeft()
                + "    Défausses : " + game.getDiscardsLeft(),
                500, 90);

        int barW = layout.screenW - 480;
        int barX = 210;
        int barY = 100;
        g.setColor(new Color(40, 40, 40));
        g.fillRect(barX, barY, barW, 6);
        int fill = (int) Math.min(barW, ((long) barW * score) / Math.max(1, target));
        g.setColor(ACCENT);
        g.fillRect(barX, barY, fill, 6);

        drawButton(g, layout.planetBtn, "Planètes", true);
    }

    private void renderHand(Graphics2D g) {
        List<Card> hand = game.getHand();
        for (int i = 0; i < hand.size(); i++) {
            boolean sel = selected.contains(i);
            int x = layout.cardX(i, hand.size());
            int y = layout.cardY(sel);
            CardRenderer.draw(g, hand.get(i), x, y, Layout.CARD_W, Layout.CARD_H, sel);
        }

        g.setColor(TEXT);
        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        g.drawString("Sélectionnées : " + selected.size() + " / 5",
                40, layout.screenH - 200);
    }

    private void renderLastHand(Graphics2D g) {
        if (lastEvaluated == null) return;
        int x = 40;
        int y = 180;
        g.setColor(PANEL);
        g.fillRoundRect(x, y, 300, 160, 12, 12);
        g.setColor(ACCENT);
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.drawString("Dernière main", x + 16, y + 30);
        g.setColor(TEXT);
        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        g.drawString(lastEvaluated.handRank().name(), x + 16, y + 60);
        g.drawString("Cartes actives : "
                + lastEvaluated.activesCards().size(), x + 16, y + 90);
        g.setColor(ACCENT);
        g.setFont(new Font("SansSerif", Font.BOLD, 26));
        g.drawString("+" + lastScore + " pts", x + 16, y + 130);
    }

    private void renderButtons(Graphics2D g) {
        boolean canPlay = selected.size() == 5 && game.getHandsLeft() > 0;
        boolean canDiscard = !selected.isEmpty() && game.getDiscardsLeft() > 0;
        drawButton(g, layout.playBtn,    "Jouer (" + selected.size() + "/5)", canPlay);
        drawButton(g, layout.discardBtn, "Défausser",                          canDiscard);
    }

    private void renderPlanetLevelsOverlay(Graphics2D g) {
        int w = 420;
        int h = 360;
        int x = (layout.screenW - w) / 2;
        int y = (layout.screenH - h) / 2;
        g.setColor(new Color(0, 0, 0, 220));
        g.fillRoundRect(x, y, w, h, 16, 16);
        g.setColor(ACCENT);
        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        g.drawString("Niveaux des mains", x + 20, y + 40);
        g.setColor(TEXT);
        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        int row = 0;
        for (var entry : game.getPlanetLevels().entrySet()) {
            HandRank hr = entry.getKey();
            int lvl = entry.getValue();
            g.drawString(hr.name(), x + 20, y + 80 + row * 28);
            g.drawString("niveau " + lvl, x + 280, y + 80 + row * 28);
            row++;
        }
    }

    private void renderPlanetPick(Graphics2D g) {
        g.setColor(ACCENT);
        g.setFont(new Font("SansSerif", Font.BOLD, 48));
        String title = "BLIND BATTUE — Choisissez une Planet Card";
        int tw = g.getFontMetrics().stringWidth(title);
        g.drawString(title, (layout.screenW - tw) / 2, 100);

        for (int i = 0; i < planetOffer.size(); i++) {
            Planet p = planetOffer.get(i);
            Rectangle r = layout.planetOfferRect(i, planetOffer.size());
            boolean hover = r.contains(mouseX, mouseY);
            g.setColor(hover ? new Color(70, 90, 130) : new Color(40, 50, 80));
            g.fillRoundRect(r.x, r.y, r.width, r.height, 16, 16);
            g.setColor(ACCENT);
            g.setStroke(new java.awt.BasicStroke(2f));
            g.drawRoundRect(r.x, r.y, r.width, r.height, 16, 16);

            g.setColor(ACCENT);
            g.setFont(new Font("SansSerif", Font.BOLD, 32));
            g.drawString(p.name(), r.x + 20, r.y + 50);
            g.setColor(TEXT);
            g.setFont(new Font("SansSerif", Font.PLAIN, 20));
            g.drawString("Cible : " + p.target.name(), r.x + 20, r.y + 100);
            int lvl = game.getPlanetLevels().get(p.target);
            g.drawString("Niveau actuel : " + lvl, r.x + 20, r.y + 130);
            g.drawString("+ " + p.bonusChips + " chips", r.x + 20, r.y + 180);
            g.drawString("+ " + p.bonusMult + " mult",  r.x + 20, r.y + 210);
        }

        Rectangle skip = new Rectangle(layout.screenW / 2 - 120,
                layout.screenH - 140, 240, 70);
        drawButton(g, skip, "Passer", true);
    }

    private void renderRoundLost(Graphics2D g) {
        g.setColor(new Color(200, 50, 50));
        g.setFont(new Font("SansSerif", Font.BOLD, 80));
        String s = "GAME OVER";
        int sw = g.getFontMetrics().stringWidth(s);
        g.drawString(s, (layout.screenW - sw) / 2, layout.screenH / 2 - 20);
        g.setColor(TEXT);
        g.setFont(new Font("SansSerif", Font.PLAIN, 28));
        String sub = "Blind ratée — cliquez pour quitter";
        int sbw = g.getFontMetrics().stringWidth(sub);
        g.drawString(sub, (layout.screenW - sbw) / 2, layout.screenH / 2 + 40);
    }

    private void renderVictory(Graphics2D g) {
        g.setColor(ACCENT);
        g.setFont(new Font("SansSerif", Font.BOLD, 80));
        String s = "VICTOIRE !";
        int sw = g.getFontMetrics().stringWidth(s);
        g.drawString(s, (layout.screenW - sw) / 2, layout.screenH / 2 - 20);
        g.setColor(TEXT);
        g.setFont(new Font("SansSerif", Font.PLAIN, 28));
        String sub = "Toutes les blindes battues — cliquez pour quitter";
        int sbw = g.getFontMetrics().stringWidth(sub);
        g.drawString(sub, (layout.screenW - sbw) / 2, layout.screenH / 2 + 40);
    }

    private void drawButton(Graphics2D g, Rectangle r, String label, boolean enabled) {
        boolean hover = enabled && r.contains(mouseX, mouseY);
        Color bg = !enabled ? BTN_DISABLED : (hover ? BTN_HOT : BTN);
        g.setColor(bg);
        g.fillRoundRect(r.x, r.y, r.width, r.height, 12, 12);
        g.setColor(enabled ? TEXT : new Color(140, 140, 140));
        g.setStroke(new java.awt.BasicStroke(2f));
        g.drawRoundRect(r.x, r.y, r.width, r.height, 12, 12);
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        int lw = g.getFontMetrics().stringWidth(label);
        int lh = g.getFontMetrics().getAscent();
        g.drawString(label, r.x + (r.width - lw) / 2,
                r.y + (r.height + lh) / 2 - 4);
    }
}
