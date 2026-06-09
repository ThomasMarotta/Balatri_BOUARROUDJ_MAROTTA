package model;

import java.util.Objects;

import domain.Blind;

public class GameState {
	private Blind currentBlind;
	private int gameScore;
	private int handsLeft;
	private int discardsLeft;

	public GameState(Blind currentBlind, int gameScore, int handsLeft, int discardsLeft) {
		Objects.requireNonNull(currentBlind);

		if (gameScore < 0) {
			throw new IllegalArgumentException("gameScore cannot be negative !");
		}
		if (handsLeft < 0) {
			throw new IllegalArgumentException("handsLeft cannot be negative !");
		}
		if (discardsLeft < 0) {
			throw new IllegalArgumentException("discardsLeft cannot be negative !");
		}

		this.currentBlind = currentBlind;
		this.gameScore = gameScore;
		this.handsLeft = handsLeft;
		this.discardsLeft = discardsLeft;
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

	public int discardsLeft() {
		return discardsLeft;
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

	public void resetDiscardsLeft(int discards) {
		discardsLeft = discards;
	}

	public void decrementHands() {
		handsLeft--;
	}

	public void decrementDiscards() {
		discardsLeft--;
	}

	public boolean canDiscard() {
		if (discardsLeft > 0) {
			return true;
		}
		return false;
	}

	public void changeBlind(Blind newBlind) {
		currentBlind = newBlind;
	}

	public boolean isBlindBeaten() {
		return gameScore >= currentBlind.targetScore();
	}
}