package view;

import java.util.List;

import domain.Card;
import domain.Planet;
import model.GameState;

public interface View {
	void displayBanner();

	void displayState(GameState state, List<Card> hand);

	boolean displayPlayerAction(boolean canDiscard);

	List<Integer> promptCardSelection(int exactCount, int maxIndex);

	void displayHandResult(int scoreObtained, String handRankName);

	void displayBlindBeaten();

	void displayPlanetReward(Planet planet, int newLevel);

	void displayGameOver(boolean isVictory);
}