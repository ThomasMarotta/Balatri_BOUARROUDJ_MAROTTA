package domain;

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
}