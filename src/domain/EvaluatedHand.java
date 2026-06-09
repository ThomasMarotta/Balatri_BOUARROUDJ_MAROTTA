package domain;

import java.util.List;
import java.util.Objects;

public record EvaluatedHand(Hand hand, HandRank handRank, List<Card> activesCards) {
	public EvaluatedHand {
		Objects.requireNonNull(hand);
		Objects.requireNonNull(handRank);
		Objects.requireNonNull(activesCards);
	}
}
