package controller;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import domain.Card;
import domain.EvaluatedHand;
import domain.Hand;
import domain.Planet;
import domain.Rank;
import domain.RankEvaluator;
import model.BlindManager;
import model.Deck;
import model.GameState;
import model.PlanetManager;
import view.View;

public class GameController{
	private final Deck deck;
	private final PlanetManager planetManager;
	private final BlindManager blindManager;
	
    private static final int HAND_SIZE = 8;
    private static final int CARDS_TO_PLAY = 5;
    private static final int HANDS_PER_ROUND = 4;
    private static final int DISCARDS_PER_ROUND  = 4;
    
    public GameController() {
    	this.deck = new Deck();
    	this.planetManager = new PlanetManager();
    	this.blindManager = new BlindManager();
    }
    
  	private int evaluateScore(EvaluatedHand evaluatedHand) {
  		var currentLevel = planetManager.getLevel(evaluatedHand.handRank());
  		var activesCard = evaluatedHand.activesCards();
  		var planet = Planet.planetByRank(evaluatedHand.handRank());

  		var chips = evaluatedHand.handRank().baseChips() + ((currentLevel - 1) * planet.bonusChips());
  		var mult = evaluatedHand.handRank().baseMult() + ((currentLevel - 1) * planet.bonusMult());

  		var cardChips = activesCard.stream().map(Card::rank).mapToInt(Rank::chipValue).sum();

  		return (chips + cardChips) * mult;
  	}
    
    public void game(View gameView) {
    	gameView.displayBanner();
    	var gameState = new GameState(this.blindManager.getFirstBlind(), 0, HANDS_PER_ROUND, DISCARDS_PER_ROUND);
    	var currentHand = Stream.generate(deck::drawOne).limit(HAND_SIZE).collect(Collectors.toCollection(ArrayList::new));

    	while (true) {
    		Stream.generate(deck::drawOne).limit(HAND_SIZE - currentHand.size()).forEach(currentHand::add);
    		gameView.displayState(gameState, currentHand);

    		// 1. Le joueur sélectionne ses cartes (et clique sur Jouer/Défausser dans ZenView)
    		var selectedIndices = gameView.promptCardSelection(CARDS_TO_PLAY, currentHand.size());

    		// 2. On récupère le choix de l'action
    		if (!gameView.displayPlayerAction(gameState.canDiscard())) {
    			var cardsToDiscard = selectedIndices.stream().map(currentHand::get).toList();
    			cardsToDiscard.forEach(deck::addToDiscard);
    			gameState.decrementDiscards();
    			currentHand.removeAll(cardsToDiscard);
    			continue;
    		}

    		var hand = new Hand(selectedIndices.stream().map(currentHand::get).toList());
    		hand.cards().forEach(deck::addToDiscard);
    		currentHand.removeAll(hand.cards());
    		var evaluateHand = RankEvaluator.evaluate(hand);
    		int handScore = evaluateScore(evaluateHand);
    		gameView.displayHandResult(handScore, evaluateHand.handRank().name());

    		gameState.incrementScore(handScore);
    		gameState.decrementHands();

    		if (gameState.isBlindBeaten()) {
    			if (blindManager.isLastBlind(gameState.currentBlind())) {
    				gameView.displayGameOver(true);
    				break;
    			}

    			gameView.displayBlindBeaten();

    			var reward = this.planetManager.getRandomPlanet();
    			this.planetManager.applyPlanet(reward); // (un seul applyPlanet — il était dupliqué)
    			gameView.displayPlanetReward(reward, planetManager.getLevel(reward.target));

    			gameState.changeBlind(blindManager.getNextBlind(gameState.currentBlind()));
    			gameState.resetScore();
    			gameState.resetHandsLeft(HANDS_PER_ROUND);
    			gameState.resetDiscardsLeft(DISCARDS_PER_ROUND);
    		} else if (gameState.handsLeft() == 0) {
    			gameView.displayGameOver(false);
    			break;
    		}
			}
		}
}