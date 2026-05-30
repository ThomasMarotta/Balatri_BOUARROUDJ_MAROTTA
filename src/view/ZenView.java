package view;

import java.util.List;

import domain.Card;
import domain.Planet;
import model.GameState;

public class ZenView implements View {

	@Override
	public void displayBanner() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void displayState(GameState state, List<Card> hand) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<Integer> promptCardSelection(int exactCount, int maxIndex) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void displayHandResult(int scoreObtained, String handRankName) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void displayBlindBeaten() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void displayPlanetReward(Planet planet, int newLevel) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void displayGameOver(boolean isVictory) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean displayPlayerAction(boolean canDiscard) {
		// TODO Auto-generated method stub
		return false;
	}

}
