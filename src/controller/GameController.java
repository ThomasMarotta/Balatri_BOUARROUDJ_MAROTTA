package controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.EnumMap;

import domain.*;
import model.Deck;
import model.GameState;

public class GameController {

    // ── Niveaux de Planet Cards ────────────────────────────────────────────────
    private final Map<HandRank, Integer> planetLevels = new EnumMap<>(HandRank.class);

    // ── Configuration de la partie ─────────────────────────────────────────────
    private static final int HAND_SIZE       = 8;   // cartes en main
    private static final int CARDS_TO_PLAY   = 5;   // cartes jouées par main
    private static final int HANDS_PER_ROUND = 4;   // mains jouables par round
    private static final int DISCARDS_PER_ROUND = 3; // défausses par round

    // ── État courant ───────────────────────────────────────────────────────────
    private Deck        deck;
    private List<Card>  hand;
    private int         gameScore;
    private int         handsLeft;
    private int         discardsLeft;
    private int         roundNumber;
    private Blind       currentBlind;
    private boolean     gameOver;
    private boolean     roundWon;

    // Blindes prédéfinies (score cible croissant)
    private static final List<Blind> BLINDS = List.of(
        new Blind("Small Blind",  300),
        new Blind("Big Blind",    800),
        new Blind("The Hook",    2000),
        new Blind("The Wall",    5000),
        new Blind("The Ox",     11000),
        new Blind("The Boss",   20000)
    );

    public GameController() {
        this.roundNumber  = 0;
        this.gameScore    = 0;
        this.gameOver     = false;
        // Initialise tous les niveaux à 1
        for (HandRank hr : HandRank.values()) {
            planetLevels.put(hr, 1);
        }
        startNextRound();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Démarrage d'un nouveau round
    // ══════════════════════════════════════════════════════════════════════════
    private void startNextRound() {
        if (roundNumber >= BLINDS.size()) {
            gameOver = true;
            return;
        }
        currentBlind = BLINDS.get(roundNumber);
        roundNumber++;
        deck         = new Deck();
        hand         = new ArrayList<>();
        for (int i = 0; i < HAND_SIZE; i++) {
            hand.add(deck.drawOne());
        }
        handsLeft    = HANDS_PER_ROUND;
        discardsLeft = DISCARDS_PER_ROUND;
        gameScore    = 0;          // score repart à 0 pour chaque blind
        roundWon     = false;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Jouer une main (indices dans la main courante, 0-based)
    // ══════════════════════════════════════════════════════════════════════════
    public PlayResult playHand(List<Integer> indices) {
        if (gameOver)  throw new IllegalStateException("La partie est terminée.");
        if (handsLeft  <= 0) throw new IllegalStateException("Plus de mains disponibles ce round.");
        if (indices.size() != CARDS_TO_PLAY)
            throw new IllegalArgumentException("Vous devez jouer exactement " + CARDS_TO_PLAY + " cartes.");
        validateIndices(indices);

        List<Card> played = extractCards(indices);
        Hand pokerHand    = new Hand(played);
        EvaluatedHand ev  = RankEvaluator.evaluate(pokerHand);

        int score   = computeScore(ev);
        gameScore  += score;
        handsLeft--;

        // Repioche jusqu'à HAND_SIZE si possible
        refillHand();

        roundWon = gameScore >= currentBlind.targetScore();

        return new PlayResult(ev, score, gameScore, handsLeft, discardsLeft,
                              currentBlind.targetScore(), roundWon, gameOver);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Défausser des cartes (indices 0-based dans la main)
    // ══════════════════════════════════════════════════════════════════════════
    public DiscardResult discard(List<Integer> indices) {
        if (gameOver)      throw new IllegalStateException("La partie est terminée.");
        if (discardsLeft <= 0) throw new IllegalStateException("Plus de défausses disponibles ce round.");
        if (indices.isEmpty() || indices.size() > HAND_SIZE)
            throw new IllegalArgumentException("Nombre de cartes à défausser invalide.");
        validateIndices(indices);

        extractCards(indices);   // retire les cartes de la main
        discardsLeft--;
        refillHand();

        return new DiscardResult(List.copyOf(hand), discardsLeft);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Appliquer une Planet Card (monte le niveau d'un HandRank)
    // ══════════════════════════════════════════════════════════════════════════
    public void applyPlanet(Planet planet) {
        int lvl = planetLevels.get(planet.target);
        planetLevels.put(planet.target, lvl + 1);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Passer au round suivant (si roundWon)
    // ══════════════════════════════════════════════════════════════════════════
    public boolean nextRound() {
        if (!roundWon) return false;
        startNextRound();
        return !gameOver;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Calcul du score  (chips + rank.chips) × (mult + rank.mult)  avec niveaux
    // ══════════════════════════════════════════════════════════════════════════
    private int computeScore(EvaluatedHand ev) {
        HandRank rank  = ev.handRank();
        int level      = planetLevels.get(rank);

        // Bonus de niveau : chaque level au-delà de 1 ajoute bonusChips / bonusMult
        Planet planet = getPlanetForRank(rank);
        int extraChips = (level - 1) * planet.bonusChips;
        int extraMult  = (level - 1) * planet.bonusMult;

        int chips = rank.getBaseChips() + extraChips;
        int mult  = rank.getBaseMult()  + extraMult;

        // +chip value des cartes actives
        for (Card c : ev.activesCards()) {
            chips += c.rank().value();
        }

        return chips * mult;
    }

    private Planet getPlanetForRank(HandRank hr) {
        for (Planet p : Planet.values()) {
            if (p.target == hr) return p;
        }
        throw new IllegalArgumentException("No planet for " + hr);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Helpers internes
    // ══════════════════════════════════════════════════════════════════════════
    private void validateIndices(List<Integer> indices) {
        for (int i : indices) {
            if (i < 0 || i >= hand.size())
                throw new IllegalArgumentException("Index invalide : " + i);
        }
    }

    /** Retire les cartes aux indices donnés (triés en décroissant pour préserver les positions). */
    private List<Card> extractCards(List<Integer> indices) {
        List<Integer> sorted = indices.stream().distinct().sorted((a, b) -> b - a).toList();
        List<Card> extracted = new ArrayList<>();
        for (int i : sorted) {
            extracted.add(0, hand.remove(i));
        }
        return extracted;
    }

    private void refillHand() {
        while (hand.size() < HAND_SIZE && deck.remaining() > 0) {
            hand.add(deck.drawOne());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Accesseurs
    // ══════════════════════════════════════════════════════════════════════════
    public List<Card>     getHand()         { return List.copyOf(hand); }
    public int            getGameScore()    { return gameScore; }
    public int            getHandsLeft()    { return handsLeft; }
    public int            getDiscardsLeft() { return discardsLeft; }
    public Blind          getCurrentBlind() { return currentBlind; }
    public int            getRoundNumber()  { return roundNumber; }
    public boolean        isGameOver()      { return gameOver; }
    public boolean        isRoundWon()      { return roundWon; }
    public Map<HandRank, Integer> getPlanetLevels() { return Map.copyOf(planetLevels); }

    public GameState getGameState() {
        return new GameState(gameScore, currentBlind);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Records résultats
    // ══════════════════════════════════════════════════════════════════════════
    public record PlayResult(
        EvaluatedHand evaluatedHand,
        int           handScore,
        int           totalScore,
        int           handsLeft,
        int           discardsLeft,
        int           targetScore,
        boolean       roundWon,
        boolean       gameOver
    ) {}

    public record DiscardResult(
        List<Card> newHand,
        int        discardsLeft
    ) {}
}