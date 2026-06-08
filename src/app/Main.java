package app;

import controller.GameController;
import view.ZenView;

public class Main {
	public void main() {
		var game = new GameController();
		game.game(new ZenView());
	}
}