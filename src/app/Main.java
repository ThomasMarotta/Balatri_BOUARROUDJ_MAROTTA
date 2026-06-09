package app;

import controller.GameController;
import view.ZenView;

public class Main {
	public static void main(String[] args) {
		var game = new GameController();
		game.game(new ZenView());
	}
}