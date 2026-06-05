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

	// --- Constantes graphiques ---
	private static final int CARD_W = 80;
	private static final int CARD_H = 110;
	private static final int CARD_SPACING = 95;
	private static final int HAND_Y = 550;
	private static final int HAND_START_X = 100;

	private static final int PLAY_BTN_X = 900;
	private static final int PLAY_BTN_Y = 560;
	private static final int DISCARD_BTN_X = 900;
	private static final int DISCARD_BTN_Y = 630;
	private static final int BTN_W = 200;
	private static final int BTN_H = 55;

	private static final int SORT_RANK_X = 900;
	private static final int SORT_RANK_Y = 710;
	private static final int SORT_COLOR_X = 1010;
	private static final int SORT_COLOR_Y = 710;
	private static final int SORT_W = 100;
	private static final int SORT_H = 35;

	private static final Color BG_COLOR = new Color(34, 85, 34);
	private static final Color CARD_COLOR = Color.WHITE;
	private static final Color SELECTED_COLOR = new Color(255, 230, 50);
	private static final Color TEXT_COLOR = Color.WHITE;
	private static final Color RED_COLOR = new Color(200, 30, 30);
	private static final Color GREEN_BTN = new Color(50, 150, 50);
	private static final Color GREEN_BTN_DIM = new Color(50, 150, 50, 90);
	private static final Color ORANGE_BTN = new Color(200, 100, 30);
	private static final Color ORANGE_BTN_DIM = new Color(200, 100, 30, 90);
	private static final Color INFO_BG = new Color(0, 0, 0, 160);

	private static final Font FONT_TITLE = new Font("Arial", Font.BOLD, 28);
	private static final Font FONT_NORMAL = new Font("Arial", Font.PLAIN, 18);
	private static final Font FONT_BTN = new Font("Arial", Font.BOLD, 20);
	private static final Font FONT_CARD = new Font("Arial", Font.BOLD, 16);
	private static final Font FONT_SMALL = new Font("Arial", Font.PLAIN, 14);

	// --- État interne ---
	private ApplicationContext context;
	private List<Card> currentHand = List.of();
	private GameState currentState = null;
	private final List<Integer> selectedIndices = new ArrayList<>();

	private String messageLine1 = "";
	private String messageLine2 = "";

	// File unique : on transmet les indices après clic sur un des deux boutons.
	private final BlockingQueue<List<Integer>> selectionQueue = new ArrayBlockingQueue<>(1);
	// Mémorise quel bouton a été cliqué pour le retourner via displayPlayerAction.
	private boolean lastAction = true;

	private enum Mode { DISPLAY_ONLY, PLAYING }

	private Mode mode = Mode.DISPLAY_ONLY;
	private int exactCount = 5;
	private boolean shouldClose = false;

	public ZenView() {
		Thread zenThread = new Thread(() -> Application.run(BG_COLOR, ctx -> {
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

	// -----------------------------------------------------------------------
	// Événements
	// -----------------------------------------------------------------------
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
			int cy = selectedIndices.contains(i) ? HAND_Y - 20 : HAND_Y;
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

		// 2. Bouton Jouer
		if (inRect(px, py, PLAY_BTN_X, PLAY_BTN_Y, BTN_W, BTN_H)) {
			tryPlay();
			return;
		}

		// 3. Bouton Défausser
		if (canDiscardNow() && inRect(px, py, DISCARD_BTN_X, DISCARD_BTN_Y, BTN_W, BTN_H)) {
			tryDiscard();
			return;
		}

		// 4. Tri (la sélection devient invalide → on la vide)
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

	// -----------------------------------------------------------------------
	// Rendu
	// -----------------------------------------------------------------------
	private void render(ApplicationContext ctx, int w, int h) {
		context.renderFrame(g -> {
			g.setColor(BG_COLOR);
			g.fillRect(0, 0, w, h);

			g.setFont(FONT_TITLE);
			g.setColor(TEXT_COLOR);
			g.drawString("BALATRI", w / 2 - 60, 45);

			if (currentState != null) drawInfoPanel(g);

			drawHand(g);
			drawActionButtons(g);
			drawSortButtons(g);
			drawSelectionInfo(g);
			drawMessages(g, w, h);
		});
	}

	private void drawInfoPanel(java.awt.Graphics2D g) {
		g.setColor(INFO_BG);
		g.fillRoundRect(20, 60, 400, 120, 15, 15);

		g.setFont(FONT_NORMAL);
		g.setColor(TEXT_COLOR);
		g.drawString("Blind : " + currentState.currentBlind().name(), 35, 90);
		g.drawString("Score : " + currentState.gameScore() + " / " + currentState.currentBlind().targetScore(), 35, 115);
		g.drawString("Mains restantes : " + currentState.handsLeft(), 35, 140);
		g.drawString("Défausses restantes : " + currentState.discardsLeft(), 35, 165);
	}

	private void drawHand(java.awt.Graphics2D g) {
		for (int i = 0; i < currentHand.size(); i++) {
			Card card = currentHand.get(i);
			int cx = HAND_START_X + i * CARD_SPACING;
			int cy = selectedIndices.contains(i) ? HAND_Y - 20 : HAND_Y;

			g.setColor(new Color(0, 0, 0, 100));
			g.fillRoundRect(cx + 4, cy + 4, CARD_W, CARD_H, 10, 10);

			g.setColor(selectedIndices.contains(i) ? SELECTED_COLOR : CARD_COLOR);
			g.fillRoundRect(cx, cy, CARD_W, CARD_H, 10, 10);

			g.setColor(Color.DARK_GRAY);
			g.drawRoundRect(cx, cy, CARD_W, CARD_H, 10, 10);

			boolean isRed = card.suit().name().equals("HEARTS") || card.suit().name().equals("DIAMONDS");
			g.setColor(isRed ? RED_COLOR : Color.BLACK);
			g.setFont(FONT_CARD);

			g.drawString(shortRank(card.rank().name()), cx + 8, cy + 22);
			g.drawString(suitSymbol(card.suit().name()), cx + 8, cy + 40);

			g.setFont(FONT_SMALL);
			g.setColor(Color.GRAY);
			g.drawString("[" + i + "]", cx + CARD_W / 2 - 10, cy + CARD_H - 6);
		}
	}

	private void drawActionButtons(java.awt.Graphics2D g) {
		if (mode != Mode.PLAYING) return;

		boolean ready = selectedIndices.size() == exactCount;

		g.setColor(ready ? GREEN_BTN : GREEN_BTN_DIM);
		g.fillRoundRect(PLAY_BTN_X, PLAY_BTN_Y, BTN_W, BTN_H, 12, 12);
		g.setColor(TEXT_COLOR);
		g.setFont(FONT_BTN);
		g.drawString("(P) Jouer", PLAY_BTN_X + 50, PLAY_BTN_Y + 35);

		if (canDiscardNow()) {
			g.setColor(ready ? ORANGE_BTN : ORANGE_BTN_DIM);
			g.fillRoundRect(DISCARD_BTN_X, DISCARD_BTN_Y, BTN_W, BTN_H, 12, 12);
			g.setColor(TEXT_COLOR);
			g.drawString("(D) Défausser", DISCARD_BTN_X + 25, DISCARD_BTN_Y + 35);
		}
	}

	private void drawSortButtons(java.awt.Graphics2D g) {
		if (mode != Mode.PLAYING) return;

		g.setColor(new Color(80, 80, 80));
		g.fillRoundRect(SORT_RANK_X, SORT_RANK_Y, SORT_W, SORT_H, 8, 8);
		g.setColor(TEXT_COLOR);
		g.setFont(FONT_SMALL);
		g.drawString("Tri rang", SORT_RANK_X + 22, SORT_RANK_Y + 23);

		g.setColor(new Color(80, 80, 80));
		g.fillRoundRect(SORT_COLOR_X, SORT_COLOR_Y, SORT_W, SORT_H, 8, 8);
		g.setColor(TEXT_COLOR);
		g.drawString("Tri couleur", SORT_COLOR_X + 14, SORT_COLOR_Y + 23);
	}

	private void drawSelectionInfo(java.awt.Graphics2D g) {
		if (mode != Mode.PLAYING) return;
		g.setFont(FONT_NORMAL);
		g.setColor(TEXT_COLOR);
		g.drawString("Sélection : " + selectedIndices.size() + " / " + exactCount, PLAY_BTN_X, PLAY_BTN_Y - 20);
	}

	private void drawMessages(java.awt.Graphics2D g, int w, int h) {
		if (!messageLine1.isEmpty()) {
			g.setFont(FONT_NORMAL);
			g.setColor(SELECTED_COLOR);
			g.drawString(messageLine1, w / 2 - 200, h - 100);
		}
		if (!messageLine2.isEmpty()) {
			g.setFont(FONT_NORMAL);
			g.setColor(TEXT_COLOR);
			g.drawString(messageLine2, w / 2 - 200, h - 75);
		}
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------
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

	// -----------------------------------------------------------------------
	// View
	// -----------------------------------------------------------------------
	@Override
	public void displayBanner() {
		// Le titre est déjà permanent dans render()
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
		// Le choix a déjà été fait au moment du clic sur Jouer ou Défausser.
		return lastAction;
	}

	@Override
	public void displayHandResult(int scoreObtained, String handName) {
		messageLine1 = "Main jouée : " + handName;
		messageLine2 = "Points marqués : +" + scoreObtained + " chips !";
		sleep(2000);
	}

	@Override
	public void displayBlindBeaten() {
		messageLine1 = "✔ BLIND BATTU !";
		messageLine2 = "";
		sleep(2000);
	}

	@Override
	public void displayPlanetReward(Planet planet, int newLevel) {
		Objects.requireNonNull(planet);
		messageLine1 = "Récompense : Planète " + planet.name();
		messageLine2 = planet.target() + " passe au NIVEAU " + newLevel + " !";
		sleep(2500);
	}

	@Override
	public void displayGameOver(boolean isVictory) {
		messageLine1 = isVictory ? "🏆 FÉLICITATIONS, VOUS AVEZ GAGNÉ !" : "GAME OVER";
		messageLine2 = isVictory ? "Tous les blinds ont été battus !" : "Plus assez de mains pour atteindre la cible.";
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