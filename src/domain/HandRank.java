package domain;

public enum HandRank {
    HIGH_CARD       (5,   1),
    PAIR            (10,  2),
    TWO_PAIR        (20,  2),
    THREE_OF_A_KIND (30,  3),
    STRAIGHT        (30,  4),
    FLUSH           (35,  4),
    FULL_HOUSE      (40,  4),
    FOUR_OF_A_KIND  (60,  7),
    STRAIGHT_FLUSH  (100, 8);

    private final int baseChips;
    private final int baseMult;

    HandRank(int baseChips, int baseMult) {
        this.baseChips = baseChips;
        this.baseMult  = baseMult;
    }

    public int getBaseChips() { return baseChips; }
    public int getBaseMult()  { return baseMult; }
}
