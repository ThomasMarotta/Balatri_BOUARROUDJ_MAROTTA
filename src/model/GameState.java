package model;

import java.util.Objects;

import domain.Blind;

public record GameState (int gameScore, Blind currentBlind) {
	
	public GameState{
		if(gameScore < 0) {
			throw new IllegalArgumentException("gameScore cannot be negative !");
		}
		Objects.requireNonNull(currentBlind);
	}
	
	public boolean isBlindBeaten() {
		return this.gameScore >= currentBlind.targetScore();
	}
}
