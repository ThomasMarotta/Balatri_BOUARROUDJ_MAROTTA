package view;

import java.util.List;
import domain.Card;
import domain.Planet;
import model.GameState;

public sealed interface View permits ZenView, ConsoleView {
	void displayBanner();

	void displayState(GameState state, List<Card> hand);

	boolean displayPlayerAction(boolean canDiscard);

	List<Integer> promptCardSelection(int exactCount, int maxIndex);

	void displayHandResult(int scoreObtained, String handRankName);

	void displayBlindBeaten();

	void displayPlanetReward(Planet planet, int newLevel);

	boolean displayGameOver(boolean isVictory);

	void displayError(String message);

	boolean wasQuitRequested();

}