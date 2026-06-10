package view;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

import domain.Card;
import domain.Hand;
import domain.Planet;
import model.GameState;

public final class ConsoleView implements View {
	private List<Card> currentHand = new ArrayList<>();
	private final Scanner scanner;

	public ConsoleView() {
		this.scanner = new Scanner(System.in);
	}

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
		this.currentHand = hand;
		IO.println("---------------------------------------------");
		IO.println("Current Blind : " + state.currentBlind().name());
		IO.println("Target        : " + state.gameScore() + " / " + state.currentBlind().targetScore() + " chips");
		IO.println("Hands left    : " + state.handsLeft());
		IO.println("Discards left : " + state.discardsLeft());
		IO.println("---------------------------------------------");

		IO.println("Your hand:");
		for (int i = 0; i < hand.size(); i++) {
			IO.println("  [" + i + "] " + hand.get(i).rank() + " of " + hand.get(i).suit());
		}
		IO.println("---------------------------------------------");
	}

	@Override
	public List<Integer> promptCardSelection(int exactCount, int maxIndex) {
		if (exactCount < 0)
			throw new IllegalArgumentException("exact count must be positive!");
		if (maxIndex < 0)
			throw new IllegalArgumentException("max index must be positive!");
		while (true) {
			IO.println("\nEnter 1 to " + exactCount + " card indices (e.g., 0 2 3)");
			IO.println("Commands: r = sort by rank, s = sort by suit, q = quit");
			IO.print("> ");
			String input = scanner.nextLine().trim();

			if (input.equals("q")) {
				quitRequested = true;
				return List.of();
			}
			if (input.equals("r")) {
				currentHand = new ArrayList<>(Hand.sortByRank(currentHand));
				IO.println("Sorted by rank:");
				for (int i = 0; i < currentHand.size(); i++) {
					IO.println("  [" + i + "] " + currentHand.get(i).rank() + " of " + currentHand.get(i).suit());
				}
				continue;
			}
			if (input.equals("s")) {
				currentHand = new ArrayList<>(Hand.sortByColor(currentHand));
				IO.println("Sorted by suit:");
				for (int i = 0; i < currentHand.size(); i++) {
					IO.println("  [" + i + "] " + currentHand.get(i).rank() + " of " + currentHand.get(i).suit());
				}
				continue;
			}

			try {
				List<Integer> indices = new ArrayList<>();
				String[] tokens = input.split("\\s+");
				for (String token : tokens) {
					int index = Integer.parseInt(token);
					if (index < 0 || index >= currentHand.size())
						throw new IllegalArgumentException("Index " + index + " does not exist.");
					if (indices.contains(index))
						throw new IllegalArgumentException("Cannot select the same card twice (" + index + ").");
					indices.add(index);
				}
				if (indices.isEmpty() || indices.size() > exactCount)
					throw new IllegalArgumentException("Select between 1 and " + exactCount + " cards.");
				return indices;
			} catch (NumberFormatException e) {
				IO.println("Error: Please enter numbers only.");
			} catch (IllegalArgumentException e) {
				IO.println("Error: " + e.getMessage());
			}
		}
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
		IO.println("The hand [" + planet.target + "] levels up to LEVEL " + newLevel + "!\n");
	}

	@Override
	public boolean displayGameOver(boolean isVictory) {
		Objects.requireNonNull(isVictory);
		IO.println("\n=============================================");
		if (isVictory) {
			IO.println("CONGRATULATIONS, YOU WON!");
			IO.println("You have defeated the Boss Blind!");
		} else {
			IO.println("GAME OVER");
			IO.println("You have no hands left to reach the target.");
		}
		IO.println("=============================================\n");
		return isVictory;
	}

	@Override
	public boolean displayPlayerAction(boolean canDiscard) {
		Objects.requireNonNull(canDiscard);
		if (!canDiscard) {
			return true;
		} else {
			while (true) {
				IO.println("Enter p for play your hand or d for discard your hand");
				IO.print("> ");
				var input = scanner.nextLine();
				IO.println(input);

				if (input.equals("p")) {
					return true;
				} else if (input.equals("d")) {
					return false;
				} else {
					IO.println("You must enter p for play your hand or d for discard your hand");
				}
			}
		}
	}

	private boolean quitRequested = false;

	@Override
	public boolean wasQuitRequested() {
		return quitRequested;
	}

	@Override
	public void displayError(String message) {
		IO.println("Error: " + message);
	}
}