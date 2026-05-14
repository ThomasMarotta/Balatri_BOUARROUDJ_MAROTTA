package domain;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RankEvaluator {
	public static EvaluatedHand evaluate(Hand hand) {
	    HandRank rank = HandRank.HIGH_CARD;
	    var rankGroup = groupByRank(hand.cards());

	    if (isStraightFlush(hand)) {
	        rank = HandRank.STRAIGHT_FLUSH;
	    } else if (isFourOfAKind(rankGroup)) {
	        rank = HandRank.FOUR_OF_A_KIND;
	    } else if (isFullHouse(rankGroup)) {
	        rank = HandRank.FULL_HOUSE;
	    } else if (isFlush(hand)) {
	        rank = HandRank.FLUSH;
	    } else if (isStraight(hand)) {
	        rank = HandRank.STRAIGHT;
	    } else if (isThreeOfAKind(rankGroup)) {
	        rank = HandRank.THREE_OF_A_KIND;
	    } else if (isTwoPairs(rankGroup)) {
	        rank = HandRank.TWO_PAIR;
	    } else if (isPair(rankGroup)) {
	        rank = HandRank.PAIR;
	    }

	    List<Card> activesCards = getActiveCards(hand, rank, rankGroup);

	    return new EvaluatedHand(hand, rank, activesCards);
	}
	
	private static Map<Rank, Long> groupByRank(List<Card> cards) {
	    return cards.stream().collect(Collectors.groupingBy(Card::rank, Collectors.counting()));
	}
	
	private static boolean isPair(Map<Rank, Long> rankGroup) {
		return rankGroup.values().stream().anyMatch(count -> count == 2L);
	}
	
	private static boolean isTwoPairs(Map<Rank, Long> rankGroup) {
		return rankGroup.values().stream().filter(count -> count == 2L).count() == 2L;
	}

	private static boolean isThreeOfAKind(Map<Rank, Long> rankGroup) {
		return  rankGroup.values().stream().anyMatch(count -> count == 3L);
	}
	
	private static boolean isStraight(Hand hand) {
		 var orders = hand.cards().stream().map(c -> c.rank().value()).sorted().toList();
		 
		// Low card A-2-3-4-5 -> {2,3,4,5,14}
		 if(orders.equals(List.of(2, 3, 4, 5, 14))){
			return true; 
		 }
		 
		 return IntStream.range(1, orders.size()).allMatch(i -> orders.get(i) == orders.get(i - 1) + 1);
	}
	
	private static boolean isFlush(Hand hand) {
		return hand.cards().stream().map(Card::suit).distinct().count() == 1;
	}
	
	private static boolean isFullHouse(Map<Rank, Long> rankGroup) {
		return rankGroup.values().containsAll(List.of(2L, 3L));
	}
	
	private static boolean isFourOfAKind(Map<Rank, Long> rankGroup) {
		return  rankGroup.values().containsAll(List.of(1L, 4L));
	}
	
	private static boolean isStraightFlush(Hand hand) {
		return isStraight(hand) && isFlush(hand);
	}
	
	private static List<Card> getActiveCards(Hand hand, HandRank rank, Map<Rank, Long> rankGroup) {
	    return switch (rank) {
	        case STRAIGHT_FLUSH, STRAIGHT, FLUSH, FULL_HOUSE, HIGH_CARD-> hand.cards();
	        case FOUR_OF_A_KIND -> getByCount(rankGroup, hand.cards(), 4);
	        case THREE_OF_A_KIND -> getByCount(rankGroup, hand.cards(), 3);
	        case TWO_PAIR -> hand.cards().stream().filter(c -> rankGroup.get(c.rank()) == 2L).toList();
	        case PAIR -> getByCount(rankGroup, hand.cards(), 2);
	    };
	}

	private static List<Card> getByCount(Map<Rank, Long> rankGroup,List<Card> cards, long count) {
	    return cards.stream().filter(c -> rankGroup.get(c.rank()) == count).toList();
	}
}
