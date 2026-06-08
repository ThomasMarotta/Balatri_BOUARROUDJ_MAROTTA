package view;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import domain.Card;
import domain.Planet;
import model.GameState;

public class ConsoleView implements View {

	private final Scanner scanner = new Scanner(System.in);

	@Override
	public void displayBanner() {
		IO.println("\n=============================================");
		IO.println("                  BALATRI                    ");
		IO.println("=============================================\n");
	}

	@Override
	public void displayState(GameState state, List<Card> hand) {
		Objects.requireNonNull(state);
		Objects.requireNonNull(hand);
		IO.println("---------------------------------------------");
		IO.println("Current Blind : " + state.currentBlind().name());
		IO.println("Target        : " + state.gameScore() + " / " + state.currentBlind().targetScore() + " chips");
		IO.println("Hands left    : " + state.handsLeft());
		IO.println("Discards left : " + state.discardsLeft());
		IO.println("---------------------------------------------");

		IO.println("Your hand:");
		for (var i = 0; i < hand.size(); i++) {
			var card = hand.get(i);
			IO.println("  [" + i + "] " + card.rank() + " of " + card.suit());
		}
		IO.println("---------------------------------------------");
	}

	@Override
	public List<Integer> promptCardSelection(int exactCount, int maxIndex) {
		if (exactCount < 0) {
			throw new IllegalArgumentException("exact count must be positive !");
		}
		if (maxIndex < 0) {
			throw new IllegalArgumentException("max index must be positive !");
		}
		for (;;) {
			IO.println("\nEnter the indices of " + exactCount + " cards, separated by spaces (e.g., 0 2 3 5 7):");
			IO.print("> ");
			var input = scanner.nextLine().trim();
			try {
				return parseSelection(input, exactCount, maxIndex);
			} catch (NumberFormatException e) {
				IO.println("Error: Please enter numbers only.");
			} catch (IllegalArgumentException e) {
				IO.println("Error: " + e.getMessage());
			}
		}
	}

	private List<Integer> parseSelection(String input, int exactCount, int maxIndex) {
		var indices = new ArrayList<Integer>();
		for (var token : input.split("\\s+")) {
			var index = Integer.parseInt(token);
			if (index < 0 || index >= maxIndex) {
				throw new IllegalArgumentException("Index " + index + " does not exist in your hand.");
			}
			if (indices.contains(index)) {
				throw new IllegalArgumentException("You cannot select the same card twice (" + index + ").");
			}
			indices.add(index);
		}
		if (indices.size() != exactCount) {
			throw new IllegalArgumentException(
					"You must select exactly " + exactCount + " cards (you selected " + indices.size() + ").");
		}
		return indices;
	}

	@Override
	public void displayHandResult(int scoreObtained, String handName) {
		if (scoreObtained < 0) {
			throw new IllegalArgumentException("Score must be positive");
		}
		Objects.requireNonNull(handName);
		IO.println("\n=> Played hand: " + handName);
		IO.println("=> Points scored: +" + scoreObtained + " chips!\n");
	}

	@Override
	public void displayBlindBeaten() {
		IO.println("BLIND BEATEN!\n");
	}

	@Override
	public void displayPlanetReward(Planet planet, int newLevel) {
		Objects.requireNonNull(planet);
		if (newLevel < 0) {
			throw new IllegalArgumentException("The new level must be positive");
		}
		IO.println("Reward obtained: Planet " + planet.name());
		IO.println("The hand [" + planet.target() + "] levels up to LEVEL " + newLevel + "!\n");
	}

	@Override
	public void displayGameOver(boolean isVictory) {
		IO.println("\n=============================================");
		if (isVictory) {
			IO.println("CONGRATULATIONS, YOU WON!");
			IO.println("You have defeated the Boss Blind!");
		} else {
			IO.println("GAME OVER");
			IO.println("You have no hands left to reach the target.");
		}
		IO.println("=============================================\n");
	}

	@Override
	public boolean displayPlayerAction(boolean canDiscard) {
		if (!canDiscard) {
			return true;
		}
		while (true) {
			IO.println("Enter p to play your hand or d to discard your hand");
			IO.print("> ");
			var input = scanner.nextLine();
			switch (input) {
			case "p" -> {
				return true;
			}
			case "d" -> {
				return false;
			}
			default -> IO.println("You must enter p to play your hand or d to discard your hand");
			}
		}
	}
}