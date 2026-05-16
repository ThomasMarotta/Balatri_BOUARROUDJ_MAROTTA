package model;

import java.util.Objects;

import domain.Blind;

public record GameState ( Blind currentBlind, int gameScore, int handsLeft) {
	
	public GameState{
		Objects.requireNonNull(currentBlind);
		if(gameScore < 0) {
			throw new IllegalArgumentException("gameScore cannot be negative !");
		}
		if(handsLeft < 0) {
			throw new IllegalArgumentException("handsLeft cannot be negative !");
		}
	}
	
	public boolean isBlindBeaten() {
		return this.gameScore >= currentBlind.targetScore();
	}
}
