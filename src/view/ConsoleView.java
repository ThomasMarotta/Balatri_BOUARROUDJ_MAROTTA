package view;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import domain.Card;
import domain.Planet;
import model.GameState;


public class ConsoleView implements View {

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
        IO.println("---------------------------------------------");
        IO.println("Current Blind : " + state.currentBlind().name());
        IO.println("Target        : " + state.gameScore() + " / " + state.currentBlind().targetScore() + " chips");
        IO.println("Hands left    : " + state.handsLeft());
        IO.println("---------------------------------------------");
        
        IO.println("Your hand:");
        for (int i = 0; i < hand.size(); i++) {
            IO.println("  [" + i + "] " + hand.get(i).rank() + " of " + hand.get(i).suit());
        }
        IO.println("---------------------------------------------");
    }

    @Override
    public List<Integer> promptCardSelection(int exactCount, int maxIndex) {
        while (true) {
            IO.println("\nEnter the indices of " + exactCount + " cards to play, separated by spaces (e.g., 0 2 3 5 7):");
            IO.print("> ");
            String input = scanner.nextLine().trim();

            try {
                List<Integer> indices = new ArrayList<>();
                String[] tokens = input.split("\\s+");

                for (String token : tokens) {
                    int index = Integer.parseInt(token);
                    if (index < 0 || index >= maxIndex) {
                        throw new IllegalArgumentException("Index " + index + " does not exist in your hand.");
                    }
                    if (indices.contains(index)) {
                        throw new IllegalArgumentException("You cannot select the same card twice (" + index + ").");
                    }
                    indices.add(index);
                }

                if (indices.size() != exactCount) {
                    throw new IllegalArgumentException("You must select exactly " + exactCount + " cards (you selected " + indices.size() + ").");
                }

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
        IO.println("\n=> Played hand: " + handName);
        IO.println("=> Points scored: +" + scoreObtained + " chips!\n");
    }

    @Override
    public void displayBlindBeaten() {
        IO.println("BLIND BEATEN!\n");
    }

    @Override
    public void displayPlanetReward(Planet planet, int newLevel) {
        IO.println("Reward obtained: Planet " + planet.name());
        IO.println("The hand [" + planet.target + "] levels up to LEVEL " + newLevel + "!\n");
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
}