package app;

import com.github.forax.zen.Application;
import view.BalatriView;

import java.awt.Color;

public class Main {

    public static void main(String[] args) {
        Application.run(new Color(28, 70, 50), ctx -> new BalatriView().run(ctx));
    }
}
