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
import domain.Planet;
import model.GameState;

public class ZenView implements View {

    // --- Constantes graphiques ---
    private static final int WIDTH        = 1200;
    private static final int HEIGHT       = 800;
    private static final int CARD_W       = 80;
    private static final int CARD_H       = 110;
    private static final int CARD_SPACING = 95;
    private static final int HAND_Y       = 550;
    private static final int HAND_START_X = 100;

    private static final Color BG_COLOR       = new Color(34, 85, 34);
    private static final Color CARD_COLOR     = Color.WHITE;
    private static final Color SELECTED_COLOR = new Color(255, 230, 50);
    private static final Color TEXT_COLOR     = Color.WHITE;
    private static final Color RED_COLOR      = new Color(200, 30, 30);
    private static final Color INFO_BG        = new Color(0, 0, 0, 160);

    private static final Font FONT_TITLE  = new Font("Arial", Font.BOLD, 28);
    private static final Font FONT_NORMAL = new Font("Arial", Font.PLAIN, 18);
    private static final Font FONT_CARD   = new Font("Arial", Font.BOLD, 16);
    private static final Font FONT_SMALL  = new Font("Arial", Font.PLAIN, 14);

    // --- État interne ---
    private ApplicationContext context;
    private List<Card>         currentHand  = List.of();
    private GameState          currentState = null;
    private final List<Integer> selectedIndices = new ArrayList<>();

    // Messages à afficher temporairement
    private String messageLine1 = "";
    private String messageLine2 = "";

    // File pour transmettre les indices sélectionnés au controller
    private final BlockingQueue<List<Integer>> selectionQueue = new ArrayBlockingQueue<>(1);
    // File pour transmettre le choix jouer/défausser
    private final BlockingQueue<Boolean> actionQueue = new ArrayBlockingQueue<>(1);

    // Mode courant : WAITING_ACTION, SELECTING_CARDS, DISPLAY_ONLY
    private enum Mode { DISPLAY_ONLY, WAITING_ACTION, SELECTING_CARDS }
    private Mode   mode          = Mode.DISPLAY_ONLY;
    private int    exactCount    = 5;
    private boolean canDiscard   = false;

    // -----------------------------------------------------------------------
    // Lancement de la fenêtre Zen dans un thread séparé
    // -----------------------------------------------------------------------
    public ZenView() {
        Thread zenThread = new Thread(() ->
            Application.run(BG_COLOR, ctx -> {
                this.context = ctx;
                var screen = ctx.getScreenInfo();
                runLoop(ctx, screen.width(), screen.height());
            })
        );
        zenThread.setDaemon(true);
        zenThread.start();

        // Attendre que le contexte Zen soit prêt
        while (context == null) {
            Thread.yield();
        }
    }

    // -----------------------------------------------------------------------
    // Boucle principale Zen
    // -----------------------------------------------------------------------
    private void runLoop(ApplicationContext ctx, int w, int h) {
        for (;;) {
            var event = ctx.pollOrWaitEvent(16); // ~60fps

            if (event != null) {
                switch (event) {
                    case KeyboardEvent ke -> handleKeyboard(ke);
                    case PointerEvent  pe -> handlePointer(pe);
                    default -> {}
                }
            }

            render(ctx, w, h);
        }
    }

    // -----------------------------------------------------------------------
    // Gestion des événements
    // -----------------------------------------------------------------------
    private void handleKeyboard(KeyboardEvent ke) {
        if (ke.action() != KeyboardEvent.Action.KEY_PRESSED) return;

        switch (mode) {
            case WAITING_ACTION -> {
                if (ke.key() == KeyboardEvent.Key.P) {
                    mode = Mode.DISPLAY_ONLY;
                    actionQueue.offer(true);   // jouer
                } else if (ke.key() == KeyboardEvent.Key.D && canDiscard) {
                    mode = Mode.DISPLAY_ONLY;
                    actionQueue.offer(false);  // défausser
                }
            }
            case SELECTING_CARDS -> {
                // Valider la sélection avec Entrée
                if (ke.key() == KeyboardEvent.Key.A) {
                    if (selectedIndices.size() == exactCount) {
                        mode = Mode.DISPLAY_ONLY;
                        selectionQueue.offer(new ArrayList<>(selectedIndices));
                        selectedIndices.clear();
                    } else {
                        messageLine1 = "Sélectionnez exactement " + exactCount + " cartes !";
                        messageLine2 = "";
                    }
                }
            }
            default -> {}
        }
    }

    private void handlePointer(PointerEvent pe) {
        if (pe.action() != PointerEvent.Action.POINTER_UP) return;

        float px = pe.location().x();
        float py = pe.location().y();

        if (mode == Mode.SELECTING_CARDS) {
            // Vérifier si on clique sur une carte
            for (int i = 0; i < currentHand.size(); i++) {
                int cx = HAND_START_X + i * CARD_SPACING;
                int cy = HAND_Y;
                if (px >= cx && px <= cx + CARD_W && py >= cy && py <= cy + CARD_H) {
                    if (selectedIndices.contains(i)) {
                        selectedIndices.remove(Integer.valueOf(i));
                    } else if (selectedIndices.size() < exactCount) {
                        selectedIndices.add(i);
                    }
                    return;
                }
            }
        }

        if (mode == Mode.WAITING_ACTION) {
            // Bouton "Jouer"
            if (px >= 400 && px <= 560 && py >= 700 && py <= 740) {
                mode = Mode.DISPLAY_ONLY;
                actionQueue.offer(true);
            }
            // Bouton "Défausser"
            if (canDiscard && px >= 600 && px <= 760 && py >= 700 && py <= 740) {
                mode = Mode.DISPLAY_ONLY;
                actionQueue.offer(false);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Rendu graphique
    // -----------------------------------------------------------------------
    private void render(ApplicationContext ctx, int w, int h) {
        context.renderFrame(g -> {
            // Fond
            g.setColor(BG_COLOR);
            g.fillRect(0, 0, w, h);

            // Titre
            g.setFont(FONT_TITLE);
            g.setColor(TEXT_COLOR);
            g.drawString("BALATRI", w / 2 - 60, 45);

            // Panneau infos blind
            if (currentState != null) {
                drawInfoPanel(g, w);
            }

            // Cartes en main
            drawHand(g);

            // Messages
            drawMessages(g, w, h);

            // Instructions selon le mode
            drawInstructions(g, w, h);
        });
    }

    private void drawInfoPanel(java.awt.Graphics2D g, int w) {
        g.setColor(INFO_BG);
        g.fillRoundRect(20, 60, 400, 120, 15, 15);

        g.setFont(FONT_NORMAL);
        g.setColor(TEXT_COLOR);
        g.drawString("Blind : " + currentState.currentBlind().name(), 35, 90);
        g.drawString("Score : " + currentState.gameScore()
                + " / " + currentState.currentBlind().targetScore(), 35, 115);
        g.drawString("Mains restantes : " + currentState.handsLeft(), 35, 140);
        g.drawString("Défausses restantes : " + currentState.discardsLeft(), 35, 165);
    }

    private void drawHand(java.awt.Graphics2D g) {
        for (int i = 0; i < currentHand.size(); i++) {
            Card card = currentHand.get(i);
            int  cx   = HAND_START_X + i * CARD_SPACING;
            int  cy   = selectedIndices.contains(i) ? HAND_Y - 20 : HAND_Y;

            // Ombre
            g.setColor(new Color(0, 0, 0, 100));
            g.fillRoundRect(cx + 4, cy + 4, CARD_W, CARD_H, 10, 10);

            // Corps de la carte
            g.setColor(selectedIndices.contains(i) ? SELECTED_COLOR : CARD_COLOR);
            g.fillRoundRect(cx, cy, CARD_W, CARD_H, 10, 10);

            // Bordure
            g.setColor(Color.DARK_GRAY);
            g.drawRoundRect(cx, cy, CARD_W, CARD_H, 10, 10);

            // Texte rang
            boolean isRed = card.suit().name().equals("HEARTS")
                         || card.suit().name().equals("DIAMONDS");
            g.setColor(isRed ? RED_COLOR : Color.BLACK);
            g.setFont(FONT_CARD);

            String rankStr = shortRank(card.rank().name());
            String suitStr = suitSymbol(card.suit().name());

            g.drawString(rankStr, cx + 8, cy + 22);
            g.drawString(suitStr, cx + 8, cy + 40);

            // Indice en bas
            g.setFont(FONT_SMALL);
            g.setColor(Color.GRAY);
            g.drawString("[" + i + "]", cx + CARD_W / 2 - 10, cy + CARD_H - 6);
        }
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

    private void drawInstructions(java.awt.Graphics2D g, int w, int h) {
        switch (mode) {
            case SELECTING_CARDS -> {
                g.setFont(FONT_NORMAL);
                g.setColor(TEXT_COLOR);
                g.drawString("Cliquez sur " + exactCount + " cartes puis appuyez sur ENTRÉE",
                        w / 2 - 220, h - 40);
                g.drawString("(" + selectedIndices.size() + "/" + exactCount + " sélectionnées)",
                        w / 2 - 80, h - 15);
            }
            case WAITING_ACTION -> {
                // Bouton Jouer
                g.setColor(new Color(50, 150, 50));
                g.fillRoundRect(400, 700, 160, 40, 10, 10);
                g.setColor(TEXT_COLOR);
                g.setFont(FONT_NORMAL);
                g.drawString("(P) Jouer", 430, 726);

                if (canDiscard) {
                    // Bouton Défausser
                    g.setColor(new Color(150, 80, 30));
                    g.fillRoundRect(600, 700, 160, 40, 10, 10);
                    g.setColor(TEXT_COLOR);
                    g.drawString("(D) Défausser", 615, 726);
                }
            }
            default -> {}
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------
    private String shortRank(String rankName) {
        return switch (rankName) {
            case "ACE"   -> "A";
            case "KING"  -> "K";
            case "QUEEN" -> "Q";
            case "JACK"  -> "J";
            case "TEN"   -> "10";
            case "NINE"  -> "9";
            case "EIGHT" -> "8";
            case "SEVEN" -> "7";
            case "SIX"   -> "6";
            case "FIVE"  -> "5";
            case "FOUR"  -> "4";
            case "THREE" -> "3";
            case "TWO"   -> "2";
            default      -> rankName;
        };
    }

    private String suitSymbol(String suitName) {
        return switch (suitName) {
            case "HEARTS"   -> "♥";
            case "DIAMONDS" -> "♦";
            case "CLUBS"    -> "♣";
            case "SPADES"   -> "♠";
            default         -> suitName;
        };
    }

    // -----------------------------------------------------------------------
    // Implémentation de View
    // -----------------------------------------------------------------------
    @Override
    public void displayBanner() {
        // Le titre est déjà affiché en permanence dans render()
    }

    @Override
    public void displayState(GameState state, List<Card> hand) {
        Objects.requireNonNull(state);
        Objects.requireNonNull(hand);
        this.currentState = state;
        this.currentHand  = hand;
        this.selectedIndices.clear();
    }

    @Override
    public List<Integer> promptCardSelection(int exactCount, int maxIndex) {
        this.exactCount = exactCount;
        this.mode       = Mode.SELECTING_CARDS;
        this.selectedIndices.clear();
        this.messageLine1 = "";
        this.messageLine2 = "";

        try {
            return selectionQueue.take(); // bloque jusqu'à validation
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return List.of();
        }
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
        messageLine2 = isVictory ? "Tous les blinds ont été battus !" 
                                 : "Plus assez de mains pour atteindre la cible.";
        sleep(4000);
        if (context != null) context.dispose();
    }

    @Override
    public boolean displayPlayerAction(boolean canDiscard) {
        this.canDiscard   = canDiscard;
        this.mode         = Mode.WAITING_ACTION;
        this.messageLine1 = "";
        this.messageLine2 = "";

        try {
            return actionQueue.take(); // bloque jusqu'au choix
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return true;
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}