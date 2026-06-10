package controller;

import java.util.List;

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

public class GameController {
	private Deck deck;
	private PlanetManager planetManager;
	private BlindManager blindManager;

	private static final int HAND_SIZE = 8;
	private static final int CARDS_TO_PLAY = 5;
	private static final int HANDS_PER_ROUND = 4;
	private static final int DISCARDS_PER_ROUND = 4;

	public GameController() {
		setupNewRun();
	}

	private void setupNewRun() {
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
		boolean replay = true;
		while (replay) {
			setupNewRun();
			var victory = runOneGame(gameView);
			if (gameView.wasQuitRequested())
				return;
			replay = gameView.displayGameOver(victory);
		}
	}

	private boolean runOneGame(View gameView) {
		var gameState = new GameState(this.blindManager.getFirstBlind(), 0, HANDS_PER_ROUND, DISCARDS_PER_ROUND);
		var currentHand = deck.drawMany(HAND_SIZE);

		while (true) {
			var toAdd = HAND_SIZE - currentHand.size();
			for (int i = 0; i < toAdd; i++) {
				currentHand.add(deck.drawOne());
			}
			gameView.displayState(gameState, currentHand);

			List<Integer> selectedIndices;
			boolean isPlay;
			while (true) {
				selectedIndices = gameView.promptCardSelection(CARDS_TO_PLAY, currentHand.size());
				if (gameView.wasQuitRequested())
					return false;
				isPlay = gameView.displayPlayerAction(gameState.canDiscard());
				if (!isPlay || selectedIndices.size() == CARDS_TO_PLAY)
					break;
				gameView.displayError("Select exactly 5 cards to play!");
			}

			if (!isPlay) {
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
					return true;
				}
				gameView.displayBlindBeaten();
				var reward = this.planetManager.getRandomPlanet();
				this.planetManager.applyPlanet(reward);
				gameView.displayPlanetReward(reward, planetManager.getLevel(reward.target));
				gameState.changeBlind(blindManager.getNextBlind(gameState.currentBlind()));
				gameState.resetScore();
				gameState.resetHandsLeft(HANDS_PER_ROUND);
				gameState.resetDiscardsLeft(DISCARDS_PER_ROUND);
			} else if (gameState.handsLeft() == 0) {
				return false;
			}
		}
	}
}