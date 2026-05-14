package domain;
	
public enum Rank {
    TWO(2),
    THREE(3),
    FOUR(4),
    FIVE(5),
    SIX(6),
    SEVEN(7),
    EIGHT(8),
    NINE(9),
    TEN(10),
    JACK(11),
    QUEEN(12),
    KING(13),
    ACE(14); 

	private final int chipValue;
	
	Rank(int chipValue) {
		if(chipValue <= 1) {
			throw new IllegalArgumentException("Value cannot be null or negative");
		}
		this.chipValue = chipValue;
	}
	
	public int value() {
		return this.chipValue;
	}
}