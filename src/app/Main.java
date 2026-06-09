package app;

import controller.GameController;
import view.ZenView;

public class Main {
	static void main() {
		var game = new GameController();
		game.game(new ZenView());
	}
}