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
    JACK(10),
    QUEEN(10),
    KING(10),
    ACE(11); 

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
	
	public int order() {
		return this.ordinal();
	}
	
}