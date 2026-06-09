package view;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.GradientPaint;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
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

	private static final Color BG_DEFAULT = new Color(34, 50, 60);

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

	private static final Font FONT_TITLE = new Font("Arial", Font.BOLD, 38);
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

	// timer is used for the parrallax display
	private long startTime = System.currentTimeMillis();

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

	// initializes the zen6 window in a daemon thread
	public ZenView() {
		// start zen6 in a separate thread
		var zenThread = new Thread(() -> Application.run(BG_DEFAULT, ctx -> {
			this.context = ctx;
			var screen = ctx.getScreenInfo();
			runLoop(ctx, screen.width(), screen.height());
		}));
		zenThread.setDaemon(true);
		zenThread.start();

		while (context == null) {
			Thread.yield();
		}

		// attach close listener to all AWT windows to exit on close
		for (var window : java.awt.Window.getWindows()) {
			window.addWindowListener(new java.awt.event.WindowAdapter() {
				@Override
				public void windowClosing(java.awt.event.WindowEvent e) {
					System.exit(0);
				}
			});
		}
	}

	// main render loop
	private void runLoop(ApplicationContext ctx, int w, int h) {
		this.screenW = w;
		this.screenH = h;
		while (true) {
			if (shouldClose) {
				ctx.dispose();
				return;
			}
			// wait for an event up to 16ms (for 60fps)
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

	// returns the X starting position of the hand, centered on screen
	private int handStartX() {
		var n = currentHand.size();
		if (n == 0)
			return screenW / 2;
		return (screenW - ((n - 1) * CARD_SPACING + CARD_W)) / 2;
	}

	// returns the X position of the action buttons (play/discard/quit)
	private int actionBtnX() {
		var n = currentHand.size();
		var handWidth = n == 0 ? CARD_W : (n - 1) * CARD_SPACING + CARD_W;
		return handStartX() + handWidth + 40;
	}

	// handles all mouse click events
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

		// check if a card was clicked
		for (var i = 0; i < currentHand.size(); i++) {
			var cy = selectedIndices.contains(i) ? HAND_Y - CARD_LIFT : HAND_Y;
			if (inRect(px, py, startX + i * CARD_SPACING, cy, CARD_W, CARD_H)) {
				toggleSelection(i);
				return;
			}
		}

		// check play button
		var btnX = actionBtnX();
		if (inRect(px, py, btnX, PLAY_BTN_Y, BTN_W, BTN_H)) {
			tryPlay();
			return;
		}

		// check discard button (only if discards remaining)
		if (canDiscardNow() && inRect(px, py, btnX, DISCARD_BTN_Y, BTN_W, BTN_H)) {
			tryDiscard();
			return;
		}

		// check quit button
		if (inRect(px, py, btnX, QUIT_GAME_BTN_Y, BTN_W, BTN_H)) {
			tryQuitGame();
			return;
		}

		// check sort by rank button
		var sortRankX = screenW / 2 - SORT_W - 10;
		var sortSuitX = screenW / 2 + 10;
		if (inRect(px, py, sortRankX, 730, SORT_W, SORT_H)) {
			currentHand = Hand.sortByRank(currentHand);
			selectedIndices.clear();
			return;
		}

		// check sort by suit button
		if (inRect(px, py, sortSuitX, 730, SORT_W, SORT_H)) {
			currentHand = Hand.sortByColor(currentHand);
			selectedIndices.clear();
		}
	}

	// handles clicks on the game over screen
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

	// toggles card selection at index i
	private void toggleSelection(int i) {
		if (selectedIndices.contains(i)) {
			selectedIndices.remove(Integer.valueOf(i));
		} else if (selectedIndices.size() < exactCount) {
			selectedIndices.add(i);
		}
		messageLine1 = "";
	}

	// returns true if point (px, py) is inside the rectangle
	private boolean inRect(float px, float py, int x, int y, int w, int h) {
		return px >= x && px <= x + w && py >= y && py <= y + h;
	}

	// returns true if the player can still discard
	private boolean canDiscardNow() {
		return currentState != null && currentState.canDiscard();
	}

	// attempts to play the selected cards, shows error if not enough cards selected
	private void tryPlay() {
		if (selectedIndices.size() != exactCount) {
			messageLine1 = "Select exactly " + exactCount + " cards!";
			messageLine2 = "";
			return;
		}
		lastAction = true;
		submitSelection();
	}

	// discard the selected cards
	private void tryDiscard() {
		lastAction = false;
		submitSelection();
	}

	private void tryQuitGame() {
		quitRequested = true;
		mode = Mode.DISPLAY_ONLY;
		shouldClose = true;
		selectionQueue.offer(List.of());
	}

	// submits the current selection to the game thread and locks input
	private void submitSelection() {
		var indices = new ArrayList<>(selectedIndices);
		selectedIndices.clear();
		mode = Mode.DISPLAY_ONLY;
		selectionQueue.offer(indices);
	}

	// renders a complete frame: background, UI panels, cards, buttons, and overlays
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

	// returns the color with the given alpha (0-255)
	private Color withAlpha(Color c, int alpha) {
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
	}

	// draws a vertical gradient background
	private void drawBackground(Graphics2D g, int w, int h) {
		Color base = blindColor(currentState.currentBlind().name());
		Color top = base.darker().darker();
		Color bottom = new Color((int) (base.getRed() * 0.15f), (int) (base.getGreen() * 0.15f),
				(int) (base.getBlue() * 0.15f));

		g.setPaint(new GradientPaint(0, 0, top, 0, h, bottom));
		g.fillRect(0, 0, w, h);

		float t = (System.currentTimeMillis() - startTime) / 1000f;
		float cx = (float) Math.sin(t * 0.4) * 0.3f;
		float cy = (float) Math.cos(t * 0.3) * 0.3f;

		base = blindColor(currentState.currentBlind().name());
		drawParallaxLayer(g, w, h, cx, cy, 0.02f, withAlpha(base, 30), 200, 8);
		drawParallaxLayer(g, w, h, cx, cy, 0.05f, withAlpha(base, 50), 100, 12);
		drawParallaxLayer(g, w, h, cx, cy, 0.10f, withAlpha(base, 70), 50, 18);
	}

	// draws one parallax layer of circles of given size and color,
	private void drawParallaxLayer(Graphics2D g, int w, int h, float cx, float cy, float speed, Color color, int size,
			int count) {

		// antialiasing
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(color);

		// seed fixe = positions stables
		Random seed = new Random(size * 31L + count);
		// offset by cx/cy * speed to create depth effect
		float offsetX = cx * speed * w;
		float offsetY = cy * speed * h;

		for (int i = 0; i < count; i++) {
			int bx = seed.nextInt(w + size * 2) - size;
			int by = seed.nextInt(h + size * 2) - size;
			g.fillOval((int) (bx + offsetX), (int) (by + offsetY), size, size);
		}
	}

	private void drawTitle(Graphics2D g, int w) {
		g.setFont(FONT_TITLE);
		g.setColor(TEXT_COLOR);
		var fm = g.getFontMetrics();
		var title = "BALATRI";
		g.drawString(title, w / 2 - fm.stringWidth(title) / 2, 50);
	}

	// draws the left panel showing the current blind name, color and target score
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

	// draws the left panel showing the current round score
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

	// draws the left panel showing remaining hands and discards
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

	// draws a single card at (cx, cy), with a glow border if selected
	private void drawCard(Graphics2D g, Card card, int cx, int cy, boolean selected) {
		if (selected) {
			g.setColor(SELECTED_GLOW);
			g.fillRoundRect(cx - 4, cy - 4, CARD_W + 8, CARD_H + 8, 14, 14);
		}

		g.setColor(new Color(0, 0, 0, 110));
		g.fillRoundRect(cx + 5, cy + 6, CARD_W, CARD_H, 12, 12);

		g.setColor(CARD_COLOR);
		g.fillRoundRect(cx, cy, CARD_W, CARD_H, 12, 12);

		// border printed after the card print
		g.setColor(CARD_BORDER);
		g.drawRoundRect(cx, cy, CARD_W, CARD_H, 12, 12);

		g.setColor(isRed(card) ? RED_SUIT : BLACK_SUIT);
		var rank = rankLabel(card);
		var suit = suitSymbol(card);

		g.setFont(FONT_CARD_SMALL);
		g.drawString(rank, cx + 8, cy + 20);
		g.drawString(suit, cx + 8, cy + 38);
		g.setFont(FONT_CARD_BIG);

		// used to measure text width for centering the suit symbol
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
		var btnX = actionBtnX();
		var ready = selectedIndices.size() == exactCount;
		drawActionButton(g, btnX, PLAY_BTN_Y, ready ? PLAY_BTN : PLAY_BTN_DIM, "Play Hand");
		ready = selectedIndices.size() >= 1;
		if (canDiscardNow()) {
			drawActionButton(g, btnX, DISCARD_BTN_Y, ready ? DISCARD_BTN : DISCARD_BTN_DIM, "Discard");
		}else {
			ready = false;
			drawActionButton(g, btnX, DISCARD_BTN_Y, ready ? DISCARD_BTN : DISCARD_BTN_DIM, "Discard");
		}
		drawActionButton(g, btnX, QUIT_GAME_BTN_Y, QUIT_GAME_BTN, "Quit");
	}

	// draws a action button
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

	// draws the sort by rank and suit buttons
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

	// draws a small sort button with a centered label
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

	// draws a centered overlay box (two message lines)
	private void drawMessages(Graphics2D g, int w, int h) {
		if (messageLine1.isEmpty() && messageLine2.isEmpty()) {
			return;
		}
		var boxW = 600;
		var boxH = 100;
		var boxX = w / 2 - boxW / 2;
		var boxY = h / 2 - 150 - boxH / 2;

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

	// draws the replay and quit buttons (end game)
	private void drawGameOverButtons(Graphics2D g, int w, int h) {
		if (mode != Mode.GAME_OVER) {
			return;
		}
		var startX = w / 2 - (BTN_W * 2 + GO_BTN_GAP) / 2;
		var btnY = h - (GO_BTN_OFFSET_Y * 2);
		drawActionButton(g, startX, btnY, HANDS_BLUE, "Replay");
		drawActionButton(g, startX + BTN_W + GO_BTN_GAP, btnY, PLAY_BTN, "Quit");
	}

	// returns the display color associated with the given blind name
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

	// returns true if the card suit is red (hearts or diamonds)
	private boolean isRed(Card card) {
		return switch (card.suit()) {
		case HEARTS, DIAMONDS -> true;
		case CLUBS, SPADES -> false;
		};
	}

	// returns true if the card suit is red
	private String suitSymbol(Card card) {
		return switch (card.suit()) {
		case HEARTS -> "♥";
		case DIAMONDS -> "♦";
		case CLUBS -> "♣";
		case SPADES -> "♠";
		};
	}

	// return display label for the given card rank
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

	// updates the current game state and hand, clear previous selection
	public void displayState(GameState state, List<Card> hand) {
		this.currentState = Objects.requireNonNull(state);
		this.currentHand = Objects.requireNonNull(hand);
		this.selectedIndices.clear();
	}

	// switches to playing mode and blocks until the player submits a card selection
	public List<Integer> promptCardSelection(int exactCount, int maxIndex) {
		this.exactCount = exactCount;
		this.mode = Mode.PLAYING;
		this.selectedIndices.clear();
		this.messageLine1 = "";
		this.messageLine2 = "";
		this.quitRequested = false;

		// blocks until the player clicks play, discard or quit
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

	// returns the display name for the given hand rank
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

	public boolean displayPlayerAction(boolean canDiscard) {
		return lastAction;
	}

	// shows the hand name and chips gained for 2 seconds after a play
	public void displayHandResult(int scoreObtained, String handName) {
		messageLine1 = handName;
		messageLine2 = "+" + scoreObtained + " chips!";
		sleep(2000);
	}

	// shows a blind defeated message for 2 seconds
	public void displayBlindBeaten() {
		messageLine1 = "BLIND DEFEATED!";
		messageLine2 = "";
		sleep(2000);
	}

	// shows the planet reward and the hand rank level up for 3 seconds
	public void displayPlanetReward(Planet planet, int newLevel) {
		Objects.requireNonNull(planet);
		messageLine1 = "Planet " + planet.name();
		messageLine2 = planetName(planet.target()) + " level up to " + newLevel;
		sleep(2500);
	}

	// shows the game over or victory screen and blocks until the player chooses
	// replay or quit
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