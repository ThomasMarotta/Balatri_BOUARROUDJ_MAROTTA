package app;

import controller.GameController;
import controller.GameController.PlayResult;
import controller.GameController.DiscardResult;
import domain.*;

import java.util.*;

public class Main {

    private static final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        printBanner();
        GameController game = new GameController();

        // ── Boucle principale : on enchaîne les blindes ───────────────────────
        while (!game.isGameOver()) {
            playRound(game);

            if (game.isGameOver()) break;

            if (game.isRoundWon()) {
                System.out.println("\n╔══════════════════════════════════╗");
                System.out.println("║  🏆  BLIND BATTUE ! Bien joué !  ║");
                System.out.println("╚══════════════════════════════════╝");
                offerPlanetCard(game);
                System.out.println("\nAppuyez sur Entrée pour la prochaine blind...");
                sc.nextLine();
                game.nextRound();
            } else {
                System.out.println("\n╔══════════════════════════════════╗");
                System.out.println("║  💀  GAME OVER — blind ratée !   ║");
                System.out.println("╚══════════════════════════════════╝");
                break;
            }
        }

        if (game.isGameOver() && game.isRoundWon()) {
            System.out.println("\n🎉  Félicitations, vous avez complété toutes les blindes !");
        }
        System.out.println("\nFin de la partie. Merci d'avoir joué !");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Un round complet
    // ══════════════════════════════════════════════════════════════════════════
    private static void playRound(GameController game) {
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.printf("  ROUND %d  —  %s   (Objectif : %d pts)%n",
                game.getRoundNumber(), game.getCurrentBlind().name(),
                game.getCurrentBlind().targetScore());
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        while (!game.isRoundWon() && game.getHandsLeft() > 0) {
            printStatus(game);
            printHand(game.getHand());

            String action = askAction(game);

            switch (action) {
                case "j" -> handlePlay(game);
                case "d" -> handleDiscard(game);
                case "p" -> printPlanetLevels(game);
                case "q" -> {
                    System.out.println("Abandon de la partie.");
                    System.exit(0);
                }
            }
        }

        if (!game.isRoundWon()) {
            System.out.printf("%n  Score final : %d / %d — Blind non battue.%n",
                    game.getGameScore(), game.getCurrentBlind().targetScore());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Jouer une main
    // ══════════════════════════════════════════════════════════════════════════
    private static void handlePlay(GameController game) {
        System.out.println("Entrez 5 indices de cartes à jouer (ex: 0 1 2 3 4) :");
        List<Integer> indices = readIndices(5, game.getHand().size());
        if (indices == null) return;

        try {
            PlayResult result = game.playHand(indices);
            EvaluatedHand ev  = result.evaluatedHand();

            System.out.println("\n  ▶ Main jouée : " + formatCards(ev.hand().cards()));
            System.out.println("  ★ " + ev.handRank() + "  (cartes actives : " + formatCards(ev.activesCards()) + ")");
            System.out.printf ("  + %d pts   (total : %d / %d)%n",
                    result.handScore(), result.totalScore(), result.targetScore());

        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("  ⚠ Erreur : " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Défausser
    // ══════════════════════════════════════════════════════════════════════════
    private static void handleDiscard(GameController game) {
        System.out.println("Entrez les indices à défausser (1 à 8, ex: 0 3 5) :");
        List<Integer> indices = readIndices(0, game.getHand().size());
        if (indices == null || indices.isEmpty()) return;

        try {
            DiscardResult result = game.discard(indices);
            System.out.println("  Cartes défaussées. Nouvelle main :");
            printHand(result.newHand());
            System.out.println("  Défausses restantes : " + result.discardsLeft());
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("  ⚠ Erreur : " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Offrir une Planet Card entre deux rounds
    // ══════════════════════════════════════════════════════════════════════════
    private static void offerPlanetCard(GameController game) {
        Planet[] planets = Planet.values();
        // Tire 3 planètes aléatoires
        List<Planet> offer = new ArrayList<>(Arrays.asList(planets));
        Collections.shuffle(offer);
        offer = offer.subList(0, Math.min(3, offer.size()));

        System.out.println("\n  🪐 Choisissez une Planet Card à appliquer :");
        for (int i = 0; i < offer.size(); i++) {
            Planet p = offer.get(i);
            int currentLevel = game.getPlanetLevels().get(p.target);
            System.out.printf("  [%d] %-8s  →  %s  (niveau actuel : %d)   +%d chips / +%d mult%n",
                    i, p.name(), p.target, currentLevel, p.bonusChips, p.bonusMult);
        }
        System.out.println("  [s] Passer (skip)");

        String input = sc.nextLine().trim().toLowerCase();
        if (input.equals("s")) return;

        try {
            int idx = Integer.parseInt(input);
            if (idx >= 0 && idx < offer.size()) {
                Planet chosen = offer.get(idx);
                game.applyPlanet(chosen);
                System.out.println("  ✓ " + chosen.name() + " appliquée ! " + chosen.target + " monte au niveau "
                        + game.getPlanetLevels().get(chosen.target));
            } else {
                System.out.println("  Choix invalide, planet card ignorée.");
            }
        } catch (NumberFormatException e) {
            System.out.println("  Choix invalide, planet card ignorée.");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Affichage des niveaux Planet
    // ══════════════════════════════════════════════════════════════════════════
    private static void printPlanetLevels(GameController game) {
        System.out.println("\n  🪐 Niveaux des mains :");
        game.getPlanetLevels().forEach((rank, level) ->
            System.out.printf("     %-20s niveau %d%n", rank, level));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Helpers affichage
    // ══════════════════════════════════════════════════════════════════════════
    private static void printBanner() {
        System.out.println("""
                ╔══════════════════════════════════════╗
                ║      🃏  MINI-BALATRO  (Java)  🃏      ║
                ║   Battez toutes les blindes !        ║
                ╚══════════════════════════════════════╝
                """);
    }

    private static void printStatus(GameController game) {
        System.out.printf("%n  Score : %d / %d   |   Mains : %d   |   Défausses : %d%n",
                game.getGameScore(), game.getCurrentBlind().targetScore(),
                game.getHandsLeft(), game.getDiscardsLeft());
    }

    private static void printHand(List<domain.Card> cards) {
        System.out.println();
        for (int i = 0; i < cards.size(); i++) {
            domain.Card c = cards.get(i);
            System.out.printf("  [%d] %-5s de %-9s (%2d chips)%n",
                    i, c.rank(), c.suit().getSymbol(), c.rank().value());
        }
    }

    private static String formatCards(List<domain.Card> cards) {
        StringBuilder sb = new StringBuilder();
        for (domain.Card c : cards) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(c.rank()).append(" de ").append(c.suit().getSymbol());
        }
        return sb.toString();
    }

    private static String askAction(GameController game) {
        System.out.println();
        System.out.println("  [j] Jouer une main   [d] Défausser   [p] Niveaux Planet   [q] Quitter");
        System.out.print("  > ");
        String input = sc.nextLine().trim().toLowerCase();
        if (game.getDiscardsLeft() == 0 && input.equals("d")) {
            System.out.println("  ⚠ Plus de défausses disponibles.");
            return "";
        }
        return input;
    }

    private static List<Integer> readIndices(int min, int max) {
        System.out.print("  > ");
        String line = sc.nextLine().trim();
        try {
            List<Integer> result = new ArrayList<>();
            for (String token : line.split("\\s+")) {
                int idx = Integer.parseInt(token);
                if (idx < 0 || idx >= max) {
                    System.out.println("  ⚠ Index hors plage : " + idx);
                    return null;
                }
                result.add(idx);
            }
            if (min > 0 && result.size() != min) {
                System.out.println("  ⚠ Vous devez entrer exactement " + min + " indices.");
                return null;
            }
            return result;
        } catch (NumberFormatException e) {
            System.out.println("  ⚠ Format invalide. Entrez des nombres séparés par des espaces.");
            return null;
        }
    }
}