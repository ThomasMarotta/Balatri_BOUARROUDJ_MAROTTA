package view;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import com.github.forax.zen.Application;
import com.github.forax.zen.ApplicationContext;
import com.github.forax.zen.PointerEvent;
import domain.Card;
import domain.Hand;
import domain.HandRank;
import domain.Planet;
import model.GameState;

public class ZenView {
	private static final int SIDE_X = 25;
	private static final int SIDE_W = 280;

	private static final int BLIND_PANEL_Y = 80;
	private static final int BLIND_PANEL_H = 170;

	private static final int SCORE_PANEL_Y = 265;
	private static final int SCORE_PANEL_H = 130;

	private static final int INFO_PANEL_Y = 410;
	private static final int INFO_PANEL_H = 130;

	private static final int CARD_W = 85;
	private static final int CARD_H = 120;
	private static final int CARD_SPACING = 95;
	private static final int HAND_Y = 600;
	private static final int CARD_LIFT = 25;

	private static final int PLAY_BTN_Y = 600;
	private static final int DISCARD_BTN_Y = 670;
	private static final int QUIT_GAME_BTN_Y = 745;
	private static final int BTN_W = 175;
	private static final int BTN_H = 55;

	private static final int SORT_W = 80;
	private static final int SORT_H = 32;

	private static final int GO_BTN_GAP = 30;
	private static final int GO_BTN_OFFSET_Y = 70;

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
	private static final Color QUIT_GAME_BTN = new Color(70, 70, 90);

	private static final Color HANDS_BLUE = new Color(70, 140, 200);
	private static final Color DISCARDS_RED = new Color(220, 75, 90);

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

	private enum Mode {
		DISPLAY_ONLY, PLAYING, GAME_OVER
	}

	private ApplicationContext context;
	private List<Card> currentHand = List.of();
	private GameState currentState;
	private final List<Integer> selectedIndices = new ArrayList<>();

	private String messageLine1 = "";
	private String messageLine2 = "";

	private final BlockingQueue<List<Integer>> selectionQueue = new ArrayBlockingQueue<>(1);
	private final BlockingQueue<Boolean> gameOverChoiceQueue = new ArrayBlockingQueue<>(1);
	private boolean lastAction = true;
	private volatile boolean quitRequested = false;

	private Mode mode = Mode.DISPLAY_ONLY;
	private int exactCount = 5;
	private boolean shouldClose = false;
	private int screenW;
	private int screenH;

	public ZenView() {
		var zenThread = new Thread(() -> Application.run(BG_BOTTOM, ctx -> {
			this.context = ctx;
			var screen = ctx.getScreenInfo();
			runLoop(ctx, screen.width(), screen.height());
		}));
		zenThread.setDaemon(true);
		zenThread.start();

		while (context == null) {
			Thread.yield();
		}

		for (var window : java.awt.Window.getWindows()) {
			window.addWindowListener(new java.awt.event.WindowAdapter() {
				@Override
				public void windowClosing(java.awt.event.WindowEvent e) {
					System.exit(0);
				}
			});
		}
	}

	private void runLoop(ApplicationContext ctx, int w, int h) {
		this.screenW = w;
		this.screenH = h;
		while (true) {
			if (shouldClose) {
				ctx.dispose();
				return;
			}
			var event = ctx.pollOrWaitEvent(16);
			if (event != null) {
				switch (event) {
				case PointerEvent pe -> handlePointer(pe);
				default -> {
				}
				}
			}
			render(ctx, w, h);
		}
	}

	/** X de départ de la main, centrée sur l'écran selon le nombre de cartes. */
	private int handStartX() {
		var n = currentHand.size();
		if (n == 0)
			return screenW / 2;
		return (screenW - ((n - 1) * CARD_SPACING + CARD_W)) / 2;
	}

	/** X des boutons d'action, calés juste à droite de la main. */
	private int actionBtnX() {
		var n = currentHand.size();
		var handWidth = n == 0 ? CARD_W : (n - 1) * CARD_SPACING + CARD_W;
		return handStartX() + handWidth + 40;
	}

	private void handlePointer(PointerEvent pe) {
		if (pe.action() != PointerEvent.Action.POINTER_UP) {
			return;
		}
		if (mode == Mode.GAME_OVER) {
			handleGameOverClick(pe);
			return;
		}
		if (mode != Mode.PLAYING) {
			return;
		}
		var px = pe.location().x();
		var py = pe.location().y();
		var startX = handStartX();

		for (var i = 0; i < currentHand.size(); i++) {
			var cy = selectedIndices.contains(i) ? HAND_Y - CARD_LIFT : HAND_Y;
			if (inRect(px, py, startX + i * CARD_SPACING, cy, CARD_W, CARD_H)) {
				toggleSelection(i);
				return;
			}
		}

		var btnX = actionBtnX();
		if (inRect(px, py, btnX, PLAY_BTN_Y, BTN_W, BTN_H)) {
			tryPlay();
			return;
		}
		if (canDiscardNow() && inRect(px, py, btnX, DISCARD_BTN_Y, BTN_W, BTN_H)) {
			tryDiscard();
			return;
		}
		if (inRect(px, py, btnX, QUIT_GAME_BTN_Y, BTN_W, BTN_H)) {
			tryQuitGame();
			return;
		}

		var sortRankX = screenW / 2 - SORT_W - 10;
		var sortSuitX = screenW / 2 + 10;
		if (inRect(px, py, sortRankX, 730, SORT_W, SORT_H)) {
			currentHand = Hand.sortByRank(currentHand);
			selectedIndices.clear();
			return;
		}
		if (inRect(px, py, sortSuitX, 730, SORT_W, SORT_H)) {
			currentHand = Hand.sortByColor(currentHand);
			selectedIndices.clear();
		}
	}

	private void handleGameOverClick(PointerEvent pe) {
		var px = pe.location().x();
		var py = pe.location().y();
		var startX = screenW / 2 - (BTN_W * 2 + GO_BTN_GAP) / 2;
		var btnY = screenH - (GO_BTN_OFFSET_Y * 2);
		if (inRect(px, py, startX, btnY, BTN_W, BTN_H)) {
			gameOverChoiceQueue.offer(Boolean.TRUE);
			return;
		}
		if (inRect(px, py, startX + BTN_W + GO_BTN_GAP, btnY, BTN_W, BTN_H)) {
			gameOverChoiceQueue.offer(Boolean.FALSE);
		}
	}

	private void toggleSelection(int i) {
		if (selectedIndices.contains(i)) {
			selectedIndices.remove(Integer.valueOf(i));
		} else if (selectedIndices.size() < exactCount) {
			selectedIndices.add(i);
		}
		messageLine1 = "";
	}

	private boolean inRect(float px, float py, int x, int y, int w, int h) {
		return px >= x && px <= x + w && py >= y && py <= y + h;
	}

	private boolean canDiscardNow() {
		return currentState != null && currentState.canDiscard();
	}

	private void tryPlay() {
		if (selectedIndices.size() != exactCount) {
			messageLine1 = "Select exactly " + exactCount + " cards!";
			messageLine2 = "";
			return;
		}
		lastAction = true;
		submitSelection();
	}

	private void tryDiscard() {
		if (selectedIndices.size() != exactCount) {
			messageLine1 = "Select exactly " + exactCount + " cards!";
			messageLine2 = "";
			return;
		}
		lastAction = false;
		submitSelection();
	}

	private void tryQuitGame() {
		quitRequested = true;
		mode = Mode.DISPLAY_ONLY;
		shouldClose = true;
		selectionQueue.offer(List.of());
	}

	private void submitSelection() {
		var indices = new ArrayList<>(selectedIndices);
		selectedIndices.clear();
		mode = Mode.DISPLAY_ONLY;
		selectionQueue.offer(indices);
	}

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
			drawSortButtons(g, w);
			drawMessages(g, w, h);
			drawGameOverButtons(g, w, h);
		});
	}

	private void drawBackground(Graphics2D g, int w, int h) {
		g.setPaint(new GradientPaint(0, 0, BG_TOP, 0, h, BG_BOTTOM));
		g.fillRect(0, 0, w, h);
	}

	private void drawTitle(Graphics2D g, int w) {
		g.setFont(FONT_TITLE);
		g.setColor(TEXT_COLOR);
		var fm = g.getFontMetrics();
		var title = "BALATRI";
		g.drawString(title, w / 2 - fm.stringWidth(title) / 2, 50);
	}

	private void drawBlindPanel(Graphics2D g) {
		drawPanel(g, SIDE_X, BLIND_PANEL_Y, SIDE_W, BLIND_PANEL_H);

		g.setFont(FONT_PANEL_LABEL);
		g.setColor(TEXT_DIM);
		g.drawString("CURRENT BLIND", SIDE_X + 15, BLIND_PANEL_Y + 25);

		g.setColor(blindColor(currentState.currentBlind().name()));
		g.fillOval(SIDE_X + 15, BLIND_PANEL_Y + 45, 30, 30);
		g.setColor(PANEL_BORDER);
		g.drawOval(SIDE_X + 15, BLIND_PANEL_Y + 45, 30, 30);

		g.setFont(FONT_BLIND_NAME);
		g.setColor(TEXT_COLOR);
		g.drawString(currentState.currentBlind().name(), SIDE_X + 55, BLIND_PANEL_Y + 67);

		g.setFont(FONT_PANEL_LABEL);
		g.setColor(TEXT_DIM);
		g.drawString("Score at least", SIDE_X + 15, BLIND_PANEL_Y + 105);

		g.setFont(FONT_TARGET);
		g.setColor(MULT_RED);
		g.drawString(String.valueOf(currentState.currentBlind().targetScore()), SIDE_X + 15, BLIND_PANEL_Y + 138);

		g.setFont(FONT_NORMAL);
		g.setColor(TEXT_DIM);
		g.drawString("chips", SIDE_X + 90, BLIND_PANEL_Y + 138);
	}

	private void drawScorePanel(Graphics2D g) {
		drawPanel(g, SIDE_X, SCORE_PANEL_Y, SIDE_W, SCORE_PANEL_H);

		g.setFont(FONT_PANEL_LABEL);
		g.setColor(TEXT_DIM);
		g.drawString("ROUND SCORE", SIDE_X + 15, SCORE_PANEL_Y + 25);

		var boxX = SIDE_X + 15;
		var boxY = SCORE_PANEL_Y + 40;
		var boxW = SIDE_W - 30;
		var boxH = 70;
		g.setColor(SCORE_BG);
		g.fillRoundRect(boxX, boxY, boxW, boxH, 10, 10);
		g.setColor(PANEL_BORDER);
		g.drawRoundRect(boxX, boxY, boxW, boxH, 10, 10);

		g.setFont(FONT_BIG_SCORE);
		g.setColor(CHIPS_BLUE);
		var score = String.valueOf(currentState.gameScore());
		var fm = g.getFontMetrics();
		g.drawString(score, boxX + boxW / 2 - fm.stringWidth(score) / 2, boxY + 52);
	}

	private void drawInfoPanel(Graphics2D g) {
		drawPanel(g, SIDE_X, INFO_PANEL_Y, SIDE_W, INFO_PANEL_H);

		var half = SIDE_W / 2;

		g.setFont(FONT_PANEL_LABEL);
		g.setColor(TEXT_DIM);
		g.drawString("HANDS", SIDE_X + 25, INFO_PANEL_Y + 30);
		g.setFont(FONT_COUNTER);
		g.setColor(HANDS_BLUE);
		g.drawString(String.valueOf(currentState.handsLeft()), SIDE_X + 35, INFO_PANEL_Y + 95);

		g.setFont(FONT_PANEL_LABEL);
		g.setColor(TEXT_DIM);
		g.drawString("DISCARDS", SIDE_X + half + 15, INFO_PANEL_Y + 30);
		g.setFont(FONT_COUNTER);
		g.setColor(DISCARDS_RED);
		g.drawString(String.valueOf(currentState.discardsLeft()), SIDE_X + half + 35, INFO_PANEL_Y + 95);
	}

	private void drawPanel(Graphics2D g, int x, int y, int w, int h) {
		g.setColor(PANEL_BG);
		g.fillRoundRect(x, y, w, h, 14, 14);
		g.setColor(PANEL_BORDER);
		g.drawRoundRect(x, y, w, h, 14, 14);
	}

	private void drawHand(Graphics2D g) {
		var startX = handStartX();
		for (var i = 0; i < currentHand.size(); i++) {
			var selected = selectedIndices.contains(i);
			var cx = startX + i * CARD_SPACING;
			var cy = selected ? HAND_Y - CARD_LIFT : HAND_Y;
			drawCard(g, currentHand.get(i), cx, cy, selected);
		}
	}

	private void drawCard(Graphics2D g, Card card, int cx, int cy, boolean selected) {
		if (selected) {
			g.setColor(SELECTED_GLOW);
			g.fillRoundRect(cx - 4, cy - 4, CARD_W + 8, CARD_H + 8, 14, 14);
		}

		g.setColor(new Color(0, 0, 0, 110));
		g.fillRoundRect(cx + 5, cy + 6, CARD_W, CARD_H, 12, 12);

		g.setColor(CARD_COLOR);
		g.fillRoundRect(cx, cy, CARD_W, CARD_H, 12, 12);
		g.setColor(CARD_BORDER);
		g.drawRoundRect(cx, cy, CARD_W, CARD_H, 12, 12);

		g.setColor(isRed(card) ? RED_SUIT : BLACK_SUIT);
		var rank = rankLabel(card);
		var suit = suitSymbol(card);

		g.setFont(FONT_CARD_SMALL);
		g.drawString(rank, cx + 8, cy + 20);
		g.drawString(suit, cx + 8, cy + 38);

		g.setFont(FONT_CARD_BIG);
		var fm = g.getFontMetrics();
		g.drawString(suit, cx + CARD_W / 2 - fm.stringWidth(suit) / 2, cy + CARD_H / 2 + 12);

		g.setFont(FONT_CARD_SMALL);
		fm = g.getFontMetrics();
		g.drawString(rank, cx + CARD_W - 8 - fm.stringWidth(rank), cy + CARD_H - 22);
		g.drawString(suit, cx + CARD_W - 8 - fm.stringWidth(suit), cy + CARD_H - 6);
	}

	private void drawActionButtons(Graphics2D g) {
		if (mode != Mode.PLAYING) {
			return;
		}
		var ready = selectedIndices.size() == exactCount;
		var btnX = actionBtnX();
		drawActionButton(g, btnX, PLAY_BTN_Y, ready ? PLAY_BTN : PLAY_BTN_DIM, "Play Hand");
		if (canDiscardNow()) {
			drawActionButton(g, btnX, DISCARD_BTN_Y, ready ? DISCARD_BTN : DISCARD_BTN_DIM, "Discard");
		}
		drawActionButton(g, btnX, QUIT_GAME_BTN_Y, QUIT_GAME_BTN, "Quit");
	}

	private void drawActionButton(Graphics2D g, int x, int y, Color bg, String label) {
		g.setColor(new Color(0, 0, 0, 90));
		g.fillRoundRect(x + 3, y + 4, BTN_W, BTN_H, 14, 14);

		g.setColor(bg);
		g.fillRoundRect(x, y, BTN_W, BTN_H, 14, 14);
		g.setColor(new Color(255, 255, 255, 40));
		g.drawRoundRect(x, y, BTN_W, BTN_H, 14, 14);

		g.setColor(TEXT_COLOR);
		g.setFont(FONT_BTN);
		var fm = g.getFontMetrics();
		var labelY = y + BTN_H / 2 + 7;
		g.drawString(label, x + BTN_W / 2 - fm.stringWidth(label) / 2, labelY);

		g.setFont(FONT_SMALL);
		fm = g.getFontMetrics();

	}

	private void drawSortButtons(Graphics2D g, int w) {
		if (mode != Mode.PLAYING) {
			return;
		}
		var sortRankX = w / 2 - SORT_W - 10;
		var sortSuitX = w / 2 + 10;
		g.setFont(FONT_SMALL);
		g.setColor(TEXT_DIM);
		var fm = g.getFontMetrics();
		var sortLabel = "Sort by:";
		g.drawString(sortLabel, sortRankX - fm.stringWidth(sortLabel) - 10, 751);
		drawSortButton(g, sortRankX, 730, "Rank");
		drawSortButton(g, sortSuitX, 730, "Suit");
	}

	private void drawSortButton(Graphics2D g, int x, int y, String label) {
		g.setColor(new Color(60, 80, 95));
		g.fillRoundRect(x, y, SORT_W, SORT_H, 8, 8);
		g.setColor(PANEL_BORDER);
		g.drawRoundRect(x, y, SORT_W, SORT_H, 8, 8);

		g.setColor(TEXT_COLOR);
		g.setFont(FONT_SMALL);
		var fm = g.getFontMetrics();
		g.drawString(label, x + SORT_W / 2 - fm.stringWidth(label) / 2, y + 21);
	}

	private void drawMessages(Graphics2D g, int w, int h) {
		if (messageLine1.isEmpty() && messageLine2.isEmpty()) {
			return;
		}
		var boxW = 600;
		var boxH = 100;
		var boxX = w / 2 - boxW / 2;
		var boxY = h / 2 - boxH / 2;

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

	private void drawGameOverButtons(Graphics2D g, int w, int h) {
		if (mode != Mode.GAME_OVER) {
			return;
		}
		var startX = w / 2 - (BTN_W * 2 + GO_BTN_GAP) / 2;
		var btnY = h - (GO_BTN_OFFSET_Y * 2);
		drawActionButton(g, startX, btnY, HANDS_BLUE, "Replay");
		drawActionButton(g, startX + BTN_W + GO_BTN_GAP, btnY, PLAY_BTN, "Quit");
	}

	private Color blindColor(String blindName) {
		return switch (blindName) {
		case "Small Blind" -> new Color(100, 180, 220);
		case "Big Blind" -> new Color(240, 180, 60);
		case "The Hook" -> new Color(180, 90, 180);
		case "The Wall" -> new Color(120, 200, 100);
		case "The Forax" -> new Color(220, 60, 80);
		default -> new Color(150, 150, 150);
		};
	}

	private boolean isRed(Card card) {
		return switch (card.suit()) {
		case HEARTS, DIAMONDS -> true;
		case CLUBS, SPADES -> false;
		};
	}

	private String suitSymbol(Card card) {
		return switch (card.suit()) {
		case HEARTS -> "♥";
		case DIAMONDS -> "♦";
		case CLUBS -> "♣";
		case SPADES -> "♠";
		};
	}

	private String rankLabel(Card card) {
		return switch (card.rank()) {
		case ACE -> "A";
		case KING -> "K";
		case QUEEN -> "Q";
		case JACK -> "J";
		case TEN -> "10";
		case NINE -> "9";
		case EIGHT -> "8";
		case SEVEN -> "7";
		case SIX -> "6";
		case FIVE -> "5";
		case FOUR -> "4";
		case THREE -> "3";
		case TWO -> "2";
		};
	}

	public void displayState(GameState state, List<Card> hand) {
		this.currentState = Objects.requireNonNull(state);
		this.currentHand = Objects.requireNonNull(hand);
		this.selectedIndices.clear();
	}

	public List<Integer> promptCardSelection(int exactCount, int maxIndex) {
		this.exactCount = exactCount;
		this.mode = Mode.PLAYING;
		this.selectedIndices.clear();
		this.messageLine1 = "";
		this.messageLine2 = "";
		this.quitRequested = false;
		try {
			return selectionQueue.take();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return List.of();
		}
	}

	public boolean wasQuitRequested() {
		return quitRequested;
	}

	public boolean displayPlayerAction(boolean canDiscard) {
		return lastAction;
	}

	public void displayHandResult(int scoreObtained, String handName) {
		messageLine1 = handName;
		messageLine2 = "+" + scoreObtained + " chips!";
		sleep(2000);
	}

	public void displayBlindBeaten() {
		messageLine1 = "BLIND DEFEATED!";
		messageLine2 = "";
		sleep(2000);
	}

	public String planetName(HandRank handRank) {
		return switch (handRank) {
		case HIGH_CARD -> "HIGH CARD";
		case PAIR -> "PAIR";
		case TWO_PAIR -> "TWO PAIR";
		case THREE_OF_A_KIND -> "THREE OF A KIND";
		case STRAIGHT -> "STRAIGHT";
		case FLUSH -> "FLUSH";
		case FULL_HOUSE -> "FULL HOUSE";
		case FOUR_OF_A_KIND -> "FOUR OF A KIND";
		case STRAIGHT_FLUSH -> "STRAIGHT FLUSH";
		default -> throw new IllegalArgumentException("Unexpected value: " + handRank);

		};
	}

	public void displayPlanetReward(Planet planet, int newLevel) {
		Objects.requireNonNull(planet);
		messageLine1 = "Planet " + planet.name();
		messageLine2 = planetName(planet.target()) + " level up to " + newLevel;
		sleep(2500);
	}

	public boolean displayGameOver(boolean isVictory) {
		messageLine1 = isVictory ? "YOU WIN!" : "GAME OVER";
		messageLine2 = isVictory ? "All blinds defeated!" : "Out of hands.";
		gameOverChoiceQueue.clear();
		mode = Mode.GAME_OVER;
		try {
			var replay = gameOverChoiceQueue.take();
			if (replay) {
				mode = Mode.DISPLAY_ONLY;
				messageLine1 = "";
				messageLine2 = "";
			} else {
				shouldClose = true;
			}
			return replay;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			shouldClose = true;
			return false;
		}
	}

	private void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}