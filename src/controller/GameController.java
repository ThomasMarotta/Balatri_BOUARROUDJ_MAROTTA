package controller;

import java.util.stream.Stream;

import domain.EvaluatedHand;
import domain.Hand;
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
    
    public GameController() {
    	this.deck = new Deck();
    	this.planetManager = new PlanetManager();
    	this.blindManager = new BlindManager();
    }
    
    public void game(View gameView) {
    	gameView.displayBanner();
    	var gameState = new GameState(this.blindManager.getFirstBlind(), 0, HANDS_PER_ROUND);
    	while(true) {
    		var currentHand = Stream.generate(deck::drawOne).limit(HAND_SIZE).toList();
    		gameView.displayState(gameState, currentHand);
    		
    		var selectedIndices = gameView.promptCardSelection(CARDS_TO_PLAY, currentHand.size());
    		
    		var hand = new Hand(selectedIndices.stream().map(currentHand::get).toList());
    		hand.cards().forEach(deck::addToDiscard);
    		
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
    			this.planetManager.applyPlanet(reward);
    			
                planetManager.applyPlanet(reward);
                gameView.displayPlanetReward(reward, planetManager.getLevel(reward.target));
                
                gameState.changeBlind(blindManager.getNextBlind(gameState.currentBlind()));
                gameState.resetScore();
                gameState.resetHandsLeft(HANDS_PER_ROUND);
    		}else if (gameState.handsLeft() == 0) {
                gameView.displayGameOver(false);
                break; 
            }
    		
    	}
    }
    
    private int evaluateScore(EvaluatedHand evaluateHand) {
    	var currentLevel = planetManager.getLevel(evaluateHand.handRank());
    	var chips = evaluateHand.handRank().getBaseChips() + ((currentLevel - 1) * evaluateHand.handRank().getBaseMult());
    	var mult = evaluateHand.handRank().getBaseMult() + ((currentLevel - 1) * evaluateHand.handRank().getBaseMult());
    	
    	
    	return mult * chips;
    }
}