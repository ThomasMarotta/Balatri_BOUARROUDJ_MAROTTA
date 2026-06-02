package domain;

import java.util.Arrays;

public enum Planet {
    PLUTO   (HandRank.HIGH_CARD,       10, 1),
    MERCURY (HandRank.PAIR,            15, 1),
    URANUS  (HandRank.TWO_PAIR,        20, 1),
    VENUS   (HandRank.THREE_OF_A_KIND, 20, 2),
    SATURN  (HandRank.STRAIGHT,        30, 3),
    JUPITER (HandRank.FLUSH,           15, 2),
    EARTH   (HandRank.FULL_HOUSE,      25, 2),
    MARS    (HandRank.FOUR_OF_A_KIND,  30, 3),
    NEPTUNE (HandRank.STRAIGHT_FLUSH,  40, 4);

    public final HandRank target;
    public final int bonusChips;
    public final int bonusMult;

    Planet(HandRank target, int bonusChips, int bonusMult) {
        this.target     = target;
        this.bonusChips = bonusChips;
        this.bonusMult  = bonusMult;
    }
    public HandRank target() {
    	return target;
    }
    public int bonusChips() {
    	return bonusChips;
    }
    public int bonusMult() {
    	return bonusMult;
    }
    
    public static Planet planetByRank(HandRank handRank) {
        return Arrays.stream(Planet.values())
                     .filter(p -> p.target == handRank)
                     .findFirst()
                     .orElseThrow(() -> new IllegalArgumentException("Aucune planète pour : " + handRank));
    }
}