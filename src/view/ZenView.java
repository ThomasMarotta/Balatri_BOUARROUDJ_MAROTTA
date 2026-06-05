package view;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.github.forax.zen.Application;
import com.github.forax.zen.ApplicationContext;
import com.github.forax.zen.KeyboardEvent;
import com.github.forax.zen.PointerEvent;

import domain.Card;
import domain.Hand;
import domain.Planet;
import model.GameState;

public class ZenView implements View {

	// =====================================================================
	// LAYOUT
	// =====================================================================
	// Sidebar gauche (panneaux empilés style Balatro)
	private static final int SIDE_X = 25;
	private static final int SIDE_W = 280;

	private static final int BLIND_PANEL_Y = 80;
	private static final int BLIND_PANEL_H = 170;

	private static final int SCORE_PANEL_Y = 265;
	private static final int SCORE_PANEL_H = 130;

	private static final int INFO_PANEL_Y = 410;
	private static final int INFO_PANEL_H = 130;

	// Main du joueur (au centre/bas)
	private static final int CARD_W = 85;
	private static final int CARD_H = 120;
	private static final int CARD_SPACING = 95;
	private static final int HAND_Y = 600;
	private static final int HAND_START_X = 360;
	private static final int CARD_LIFT = 25;

	// Boutons d'action à droite de la main (empilés verticalement)
	private static final int PLAY_BTN_X = 1130;
	private static final int PLAY_BTN_Y = 600;
	private static final int DISCARD_BTN_X = 1130;
	private static final int DISCARD_BTN_Y = 670;
	private static final int BTN_W = 175;
	private static final int BTN_H = 55;

	// Boutons de tri (sous la main, à droite)
	private static final int SORT_LABEL_X = 970;
	private static final int SORT_LABEL_Y = 745;
	private static final int SORT_RANK_X = 1090;
	private static final int SORT_RANK_Y = 730;
	private static final int SORT_COLOR_X = 1180;
	private static final int SORT_COLOR_Y = 730;
	private static final int SORT_W = 80;
	private static final int SORT_H = 32;

	// =====================================================================
	// COULEURS (palette Balatro)
	// =====================================================================
	private static final Color BG_TOP = new Color(58, 80, 90);
	private static final Color BG_BOTTOM = new Color(34, 50, 60);

	private static final Color PANEL_BG = new Color(20, 30, 38);
	private static final Color PANEL_BORDER = new Color(80, 110, 130);

	private static final Color SCORE_BG = new Color(12, 20, 28);
	private static final Color CHIPS_BLUE = new Color(80, 170, 220);
	private static final Color MULT_RED = new Color(225, 80, 90);

	private static final Color CARD_COLOR = new Color(245, 240, 230);
	private static final Color CARD_BORDER = new Color(20, 30, 38);
	private static final Color SELECTED_GLOW = new Color(255, 215, 80);

	private static final Color TEXT_COLOR = new Color(245, 240, 230);
	private static final Color TEXT_DIM = new Color(170, 180, 190);
	private static final Color RED_SUIT = new Color(205, 50, 60);
	private static final Color BLACK_SUIT = new Color(25, 30, 38);

	private static final Color PLAY_BTN = new Color(225, 70, 90);
	private static final Color PLAY_BTN_DIM = new Color(120, 50, 60);
	private static final Color DISCARD_BTN = new Color(240, 165, 50);
	private static final Color DISCARD_BTN_DIM = new Color(130, 95, 40);

	private static final Color HANDS_BLUE = new Color(70, 140, 200);
	private static final Color DISCARDS_RED = new Color(220, 75, 90);

	// =====================================================================
	// FONTS
	// =====================================================================
	private static final Font FONT_TITLE = new Font("Arial", Font.BOLD, 32);
	private static final Font FONT_PANEL_LABEL = new Font("Arial", Font.BOLD, 14);
	private static final Font FONT_BLIND_NAME = new Font("Arial", Font.BOLD, 22);
	private static final Font FONT_BIG_SCORE = new Font("Arial", Font.BOLD, 42);
	private static final Font FONT_TARGET = new Font("Arial", Font.BOLD, 22);
	private static final Font FONT_COUNTER = new Font("Arial", Font.BOLD, 28);
	private static final Font FONT_BTN = new Font("Arial", Font.BOLD, 20);
	private static final Font FONT_CARD_BIG = new Font("Arial", Font.BOLD, 20);
	private static final Font FONT_CARD_SMALL = new Font("Arial", Font.BOLD, 16);
	private static final Font FONT_NORMAL = new Font("Arial", Font.PLAIN, 16);
	private static final Font FONT_SMALL = new Font("Arial", Font.PLAIN, 13);

	// =====================================================================
	// ÉTAT
	// =====================================================================
	private ApplicationContext context;
	private List<Card> currentHand = List.of();
	private GameState currentState = null;
	private final List<Integer> selectedIndices = new ArrayList<>();

	private String messageLine1 = "";
	private String messageLine2 = "";

	private final BlockingQueue<List<Integer>> selectionQueue = new ArrayBlockingQueue<>(1);
	private boolean lastAction = true;

	private enum Mode { DISPLAY_ONLY, PLAYING }

	private Mode mode = Mode.DISPLAY_ONLY;
	private int exactCount = 5;
	private boolean shouldClose = false;

	public ZenView() {
		Thread zenThread = new Thread(() -> Application.run(BG_BOTTOM, ctx -> {
			this.context = ctx;
			var screen = ctx.getScreenInfo();
			runLoop(ctx, screen.width(), screen.height());
		}));
		zenThread.setDaemon(true);
		zenThread.start();

		while (context == null) {
			Thread.yield();
		}
	}

	private void runLoop(ApplicationContext ctx, int w, int h) {
		for (;;) {
			if (shouldClose) {
				ctx.dispose();
				return;
			}
			var event = ctx.pollOrWaitEvent(16);

			if (event != null) {
				switch (event) {
				case KeyboardEvent ke -> handleKeyboard(ke);
				case PointerEvent pe -> handlePointer(pe);
				default -> {}
				}
			}

			render(ctx, w, h);
		}
	}

	// =====================================================================
	// ÉVÉNEMENTS
	// =====================================================================
	private void handleKeyboard(KeyboardEvent ke) {
		if (ke.action() != KeyboardEvent.Action.KEY_PRESSED) return;
		if (mode != Mode.PLAYING) return;

		if (ke.key() == KeyboardEvent.Key.P) {
			tryPlay();
		} else if (ke.key() == KeyboardEvent.Key.D && canDiscardNow()) {
			tryDiscard();
		}
	}

	private void handlePointer(PointerEvent pe) {
		if (pe.action() != PointerEvent.Action.POINTER_UP) return;
		if (mode != Mode.PLAYING) return;

		float px = pe.location().x();
		float py = pe.location().y();

		// 1. Toggle sélection d'une carte
		for (int i = 0; i < currentHand.size(); i++) {
			int cx = HAND_START_X + i * CARD_SPACING;
			int cy = selectedIndices.contains(i) ? HAND_Y - CARD_LIFT : HAND_Y;
			if (inRect(px, py, cx, cy, CARD_W, CARD_H)) {
				if (selectedIndices.contains(i)) {
					selectedIndices.remove(Integer.valueOf(i));
				} else if (selectedIndices.size() < exactCount) {
					selectedIndices.add(i);
				}
				messageLine1 = "";
				return;
			}
		}

		// 2. Play
		if (inRect(px, py, PLAY_BTN_X, PLAY_BTN_Y, BTN_W, BTN_H)) {
			tryPlay();
			return;
		}

		// 3. Discard
		if (canDiscardNow() && inRect(px, py, DISCARD_BTN_X, DISCARD_BTN_Y, BTN_W, BTN_H)) {
			tryDiscard();
			return;
		}

		// 4. Tri
		if (inRect(px, py, SORT_RANK_X, SORT_RANK_Y, SORT_W, SORT_H)) {
			currentHand = Hand.sortByRank(currentHand);
			selectedIndices.clear();
			return;
		}
		if (inRect(px, py, SORT_COLOR_X, SORT_COLOR_Y, SORT_W, SORT_H)) {
			currentHand = Hand.sortByColor(currentHand);
			selectedIndices.clear();
			return;
		}
	}

	private boolean inRect(float px, float py, int x, int y, int w, int h) {
		return px >= x && px <= x + w && py >= y && py <= y + h;
	}

	private boolean canDiscardNow() {
		return currentState != null && currentState.canDiscard();
	}

	private void tryPlay() {
		if (selectedIndices.size() != exactCount) {
			messageLine1 = "Sélectionne exactement " + exactCount + " cartes !";
			messageLine2 = "";
			return;
		}
		lastAction = true;
		submitSelection();
	}

	private void tryDiscard() {
		if (selectedIndices.size() != exactCount) {
			messageLine1 = "Sélectionne exactement " + exactCount + " cartes !";
			messageLine2 = "";
			return;
		}
		lastAction = false;
		submitSelection();
	}

	private void submitSelection() {
		var indices = new ArrayList<>(selectedIndices);
		selectedIndices.clear();
		mode = Mode.DISPLAY_ONLY;
		selectionQueue.offer(indices);
	}

	// =====================================================================
	// RENDU
	// =====================================================================
	private void render(ApplicationContext ctx, int w, int h) {
		context.renderFrame(g -> {
			drawBackground(g, w, h);
			drawTitle(g, w);

			if (currentState != null) {
				drawBlindPanel(g);
				drawScorePanel(g);
				drawInfoPanel(g);
			}

			drawHand(g);
			drawActionButtons(g);
			drawSortButtons(g);
			drawMessages(g, w, h);
		});
	}

	private void drawBackground(java.awt.Graphics2D g, int w, int h) {
		// Dégradé vertical façon table de jeu
		var gp = new java.awt.GradientPaint(0, 0, BG_TOP, 0, h, BG_BOTTOM);
		g.setPaint(gp);
		g.fillRect(0, 0, w, h);
	}

	private void drawTitle(java.awt.Graphics2D g, int w) {
		g.setFont(FONT_TITLE);
		g.setColor(TEXT_COLOR);
		var fm = g.getFontMetrics();
		String title = "BALATRI";
		g.drawString(title, w / 2 - fm.stringWidth(title) / 2, 50);
	}

	// ---- Panneau Blind ----
	private void drawBlindPanel(java.awt.Graphics2D g) {
		drawPanel(g, SIDE_X, BLIND_PANEL_Y, SIDE_W, BLIND_PANEL_H);

		g.setFont(FONT_PANEL_LABEL);
		g.setColor(TEXT_DIM);
		g.drawString("BLIND ACTUEL", SIDE_X + 15, BLIND_PANEL_Y + 25);

		// Pastille de couleur (façon "gem" de Balatro)
		var gem = blindColor(currentState.currentBlind().name());
		g.setColor(gem);
		g.fillOval(SIDE_X + 15, BLIND_PANEL_Y + 45, 30, 30);
		g.setColor(PANEL_BORDER);
		g.drawOval(SIDE_X + 15, BLIND_PANEL_Y + 45, 30, 30);

		g.setFont(FONT_BLIND_NAME);
		g.setColor(TEXT_COLOR);
		g.drawString(currentState.currentBlind().name(), SIDE_X + 55, BLIND_PANEL_Y + 67);

		g.setFont(FONT_PANEL_LABEL);
		g.setColor(TEXT_DIM);
		g.drawString("Atteindre au moins", SIDE_X + 15, BLIND_PANEL_Y + 105);

		g.setFont(FONT_TARGET);
		g.setColor(MULT_RED);
		g.drawString(String.valueOf(currentState.currentBlind().targetScore()), SIDE_X + 15, BLIND_PANEL_Y + 138);

		g.setFont(FONT_NORMAL);
		g.setColor(TEXT_DIM);
		g.drawString("chips", SIDE_X + 90, BLIND_PANEL_Y + 138);
	}

	// ---- Panneau Round Score ----
	private void drawScorePanel(java.awt.Graphics2D g) {
		drawPanel(g, SIDE_X, SCORE_PANEL_Y, SIDE_W, SCORE_PANEL_H);

		g.setFont(FONT_PANEL_LABEL);
		g.setColor(TEXT_DIM);
		g.drawString("SCORE DE LA MANCHE", SIDE_X + 15, SCORE_PANEL_Y + 25);

		// Boîte noire pour le score (style Balatro)
		int boxX = SIDE_X + 15;
		int boxY = SCORE_PANEL_Y + 40;
		int boxW = SIDE_W - 30;
		int boxH = 70;
		g.setColor(SCORE_BG);
		g.fillRoundRect(boxX, boxY, boxW, boxH, 10, 10);
		g.setColor(PANEL_BORDER);
		g.drawRoundRect(boxX, boxY, boxW, boxH, 10, 10);

		g.setFont(FONT_BIG_SCORE);
		g.setColor(CHIPS_BLUE);
		String score = String.valueOf(currentState.gameScore());
		var fm = g.getFontMetrics();
		g.drawString(score, boxX + boxW / 2 - fm.stringWidth(score) / 2, boxY + 52);
	}

	// ---- Panneau Hands & Discards ----
	private void drawInfoPanel(java.awt.Graphics2D g) {
		drawPanel(g, SIDE_X, INFO_PANEL_Y, SIDE_W, INFO_PANEL_H);

		int half = SIDE_W / 2;

		// Mains
		g.setFont(FONT_PANEL_LABEL);
		g.setColor(TEXT_DIM);
		g.drawString("MAINS", SIDE_X + 25, INFO_PANEL_Y + 30);
		g.setFont(FONT_COUNTER);
		g.setColor(HANDS_BLUE);
		g.drawString(String.valueOf(currentState.handsLeft()), SIDE_X + 35, INFO_PANEL_Y + 95);

		// Défausses
		g.setFont(FONT_PANEL_LABEL);
		g.setColor(TEXT_DIM);
		g.drawString("DÉFAUSSES", SIDE_X + half + 15, INFO_PANEL_Y + 30);
		g.setFont(FONT_COUNTER);
		g.setColor(DISCARDS_RED);
		g.drawString(String.valueOf(currentState.discardsLeft()), SIDE_X + half + 35, INFO_PANEL_Y + 95);
	}

	private void drawPanel(java.awt.Graphics2D g, int x, int y, int w, int h) {
		g.setColor(PANEL_BG);
		g.fillRoundRect(x, y, w, h, 14, 14);
		g.setColor(PANEL_BORDER);
		g.drawRoundRect(x, y, w, h, 14, 14);
	}

	// ---- Main du joueur ----
	private void drawHand(java.awt.Graphics2D g) {
		for (int i = 0; i < currentHand.size(); i++) {
			Card card = currentHand.get(i);
			int cx = HAND_START_X + i * CARD_SPACING;
			boolean selected = selectedIndices.contains(i);
			int cy = selected ? HAND_Y - CARD_LIFT : HAND_Y;

			// Glow jaune si sélectionnée
			if (selected) {
				g.setColor(SELECTED_GLOW);
				g.fillRoundRect(cx - 4, cy - 4, CARD_W + 8, CARD_H + 8, 14, 14);
			}

			// Ombre portée
			g.setColor(new Color(0, 0, 0, 110));
			g.fillRoundRect(cx + 5, cy + 6, CARD_W, CARD_H, 12, 12);

			// Corps
			g.setColor(CARD_COLOR);
			g.fillRoundRect(cx, cy, CARD_W, CARD_H, 12, 12);

			// Bordure
			g.setColor(CARD_BORDER);
			g.drawRoundRect(cx, cy, CARD_W, CARD_H, 12, 12);

			boolean isRed = card.suit().name().equals("HEARTS") || card.suit().name().equals("DIAMONDS");
			g.setColor(isRed ? RED_SUIT : BLACK_SUIT);

			String rankStr = shortRank(card.rank().name());
			String suitStr = suitSymbol(card.suit().name());

			// Coin haut-gauche
			g.setFont(FONT_CARD_SMALL);
			g.drawString(rankStr, cx + 8, cy + 20);
			g.drawString(suitStr, cx + 8, cy + 38);

			// Symbole central
			g.setFont(FONT_CARD_BIG);
			var fm = g.getFontMetrics();
			g.drawString(suitStr, cx + CARD_W / 2 - fm.stringWidth(suitStr) / 2, cy + CARD_H / 2 + 12);

			// Coin bas-droite (inversé)
			g.setFont(FONT_CARD_SMALL);
			fm = g.getFontMetrics();
			g.drawString(rankStr, cx + CARD_W - 8 - fm.stringWidth(rankStr), cy + CARD_H - 22);
			g.drawString(suitStr, cx + CARD_W - 8 - fm.stringWidth(suitStr), cy + CARD_H - 6);
		}
	}

	// ---- Boutons Play / Discard ----
	private void drawActionButtons(java.awt.Graphics2D g) {
		if (mode != Mode.PLAYING) return;

		boolean ready = selectedIndices.size() == exactCount;

		// Play
		drawActionButton(g, PLAY_BTN_X, PLAY_BTN_Y, ready ? PLAY_BTN : PLAY_BTN_DIM, "Jouer la main", "(P)");

		// Discard
		if (canDiscardNow()) {
			drawActionButton(g, DISCARD_BTN_X, DISCARD_BTN_Y, ready ? DISCARD_BTN : DISCARD_BTN_DIM, "Défausser", "(D)");
		}
	}

	private void drawActionButton(java.awt.Graphics2D g, int x, int y, Color bg, String label, String shortcut) {
		// Ombre
		g.setColor(new Color(0, 0, 0, 90));
		g.fillRoundRect(x + 3, y + 4, BTN_W, BTN_H, 14, 14);

		g.setColor(bg);
		g.fillRoundRect(x, y, BTN_W, BTN_H, 14, 14);
		g.setColor(new Color(255, 255, 255, 40));
		g.drawRoundRect(x, y, BTN_W, BTN_H, 14, 14);

		g.setColor(TEXT_COLOR);
		g.setFont(FONT_BTN);
		var fm = g.getFontMetrics();
		g.drawString(label, x + BTN_W / 2 - fm.stringWidth(label) / 2, y + 28);

		g.setFont(FONT_SMALL);
		fm = g.getFontMetrics();
		g.drawString(shortcut, x + BTN_W / 2 - fm.stringWidth(shortcut) / 2, y + 46);
	}

	// ---- Boutons de tri ----
	private void drawSortButtons(java.awt.Graphics2D g) {
		if (mode != Mode.PLAYING) return;

		g.setFont(FONT_SMALL);
		g.setColor(TEXT_DIM);
		g.drawString("Trier par :", SORT_LABEL_X, SORT_LABEL_Y);

		drawSortButton(g, SORT_RANK_X, SORT_RANK_Y, "Rang");
		drawSortButton(g, SORT_COLOR_X, SORT_COLOR_Y, "Couleur");
	}

	private void drawSortButton(java.awt.Graphics2D g, int x, int y, String label) {
		g.setColor(new Color(60, 80, 95));
		g.fillRoundRect(x, y, SORT_W, SORT_H, 8, 8);
		g.setColor(PANEL_BORDER);
		g.drawRoundRect(x, y, SORT_W, SORT_H, 8, 8);

		g.setColor(TEXT_COLOR);
		g.setFont(FONT_SMALL);
		var fm = g.getFontMetrics();
		g.drawString(label, x + SORT_W / 2 - fm.stringWidth(label) / 2, y + 21);
	}

	// ---- Messages temporaires ----
	private void drawMessages(java.awt.Graphics2D g, int w, int h) {
		if (messageLine1.isEmpty() && messageLine2.isEmpty()) return;

		// Bandeau sombre semi-transparent au centre
		int boxW = 600;
		int boxH = 100;
		int boxX = w / 2 - boxW / 2;
		int boxY = h / 2 - boxH / 2;

		g.setColor(new Color(0, 0, 0, 200));
		g.fillRoundRect(boxX, boxY, boxW, boxH, 16, 16);
		g.setColor(PANEL_BORDER);
		g.drawRoundRect(boxX, boxY, boxW, boxH, 16, 16);

		g.setFont(FONT_BTN);
		var fm = g.getFontMetrics();
		if (!messageLine1.isEmpty()) {
			g.setColor(SELECTED_GLOW);
			g.drawString(messageLine1, w / 2 - fm.stringWidth(messageLine1) / 2, boxY + 40);
		}
		if (!messageLine2.isEmpty()) {
			g.setColor(TEXT_COLOR);
			g.drawString(messageLine2, w / 2 - fm.stringWidth(messageLine2) / 2, boxY + 75);
		}
	}

	// =====================================================================
	// HELPERS
	// =====================================================================
	private Color blindColor(String blindName) {
		// Une teinte différente par blind, comme les "gems" Balatro
		return switch (blindName) {
		case "Small Blind" -> new Color(100, 180, 220);
		case "Big Blind" -> new Color(240, 180, 60);
		case "The Hook" -> new Color(180, 90, 180);
		case "The Wall" -> new Color(120, 200, 100);
		case "The Forax" -> new Color(220, 60, 80);
		default -> new Color(150, 150, 150);
		};
	}

	private String shortRank(String rankName) {
		return switch (rankName) {
		case "ACE" -> "A";
		case "KING" -> "K";
		case "QUEEN" -> "Q";
		case "JACK" -> "J";
		case "TEN" -> "10";
		case "NINE" -> "9";
		case "EIGHT" -> "8";
		case "SEVEN" -> "7";
		case "SIX" -> "6";
		case "FIVE" -> "5";
		case "FOUR" -> "4";
		case "THREE" -> "3";
		case "TWO" -> "2";
		default -> rankName;
		};
	}

	private String suitSymbol(String suitName) {
		return switch (suitName) {
		case "HEARTS" -> "♥";
		case "DIAMONDS" -> "♦";
		case "CLUBS" -> "♣";
		case "SPADES" -> "♠";
		default -> suitName;
		};
	}

	// =====================================================================
	// VIEW
	// =====================================================================
	@Override
	public void displayBanner() {
		// Permanent dans render()
	}

	@Override
	public void displayState(GameState state, List<Card> hand) {
		Objects.requireNonNull(state);
		Objects.requireNonNull(hand);
		this.currentState = state;
		this.currentHand = hand;
		this.selectedIndices.clear();
	}

	@Override
	public List<Integer> promptCardSelection(int exactCount, int maxIndex) {
		this.exactCount = exactCount;
		this.mode = Mode.PLAYING;
		this.selectedIndices.clear();
		this.messageLine1 = "";
		this.messageLine2 = "";

		try {
			return selectionQueue.take();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return List.of();
		}
	}

	@Override
	public boolean displayPlayerAction(boolean canDiscard) {
		return lastAction;
	}

	@Override
	public void displayHandResult(int scoreObtained, String handName) {
		messageLine1 = handName;
		messageLine2 = "+" + scoreObtained + " chips !";
		sleep(2000);
	}

	@Override
	public void displayBlindBeaten() {
		messageLine1 = "BLIND BATTU !";
		messageLine2 = "";
		sleep(2000);
	}

	@Override
	public void displayPlanetReward(Planet planet, int newLevel) {
		Objects.requireNonNull(planet);
		messageLine1 = "Planète " + planet.name();
		messageLine2 = planet.target() + " → niveau " + newLevel;
		sleep(2500);
	}

	@Override
	public void displayGameOver(boolean isVictory) {
		messageLine1 = isVictory ? "VICTOIRE !" : "GAME OVER";
		messageLine2 = isVictory ? "Tous les blinds ont été battus !" : "Plus de mains à jouer.";
		sleep(4000);
		shouldClose = true;
	}

	private void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}