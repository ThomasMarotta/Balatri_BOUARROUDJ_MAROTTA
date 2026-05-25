package model;

import java.util.Objects;

import domain.Blind;

public class GameState {
	private Blind currentBlind;
	private int gameScore;
	private int handsLeft;
	
	public GameState(Blind currentBlind, int gameScore, int handsLeft) {
		Objects.requireNonNull(currentBlind);
		
		if(gameScore < 0) {
			throw new IllegalArgumentException("gameScore cannot be negative !");
		}
		if(handsLeft < 0) {
			throw new IllegalArgumentException("handsLeft cannot be negative !");
		}
		
		this.currentBlind = currentBlind;
		this.gameScore = gameScore;
		this.handsLeft = handsLeft;
	}
	
	/* Getters */
	
	public Blind currentBlind() {
		return currentBlind;
	}
	
	public int gameScore() {
		return gameScore;
	}
	
	public int handsLeft() {
		return handsLeft;
	}
	
	/* Methods */
	
	public void incrementScore(int scoreToHad) {
		gameScore += scoreToHad;
	}
	
	public void resetScore() {
		gameScore = 0;
	}
	
	public void resetHandsLeft(int hands) {
		handsLeft = hands;
	}
	
	public void decrementHands() {
		handsLeft--;
	}
	
	public void changeBlind(Blind newBlind) {
		currentBlind = newBlind;
	}
	
	public boolean isBlindBeaten() {
		return gameScore >= currentBlind.targetScore();
	}
}