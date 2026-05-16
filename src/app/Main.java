package app;

import controller.GameController;

import view.ConsoleView;


public class Main {
	public void main() {
		var game = new GameController();
		game.game(new ConsoleView());
	}
}