package domain;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RankEvaluator {
	public static EvaluatedHand evaluate(Hand hand) {
	    var rankGroup = groupByRank(hand.cards());
	    var cards = hand.cards();


        var straightFlushCards = getStraightFlushCards(hand);
        if (!straightFlushCards.isEmpty()) {
            return new EvaluatedHand(hand, HandRank.STRAIGHT_FLUSH, straightFlushCards);
        }

        var fourOfAKindCards = getByCount(rankGroup, cards, 4);
        if (!fourOfAKindCards.isEmpty()) {
            return new EvaluatedHand(hand, HandRank.FOUR_OF_A_KIND, fourOfAKindCards);
        }

        var fullHouseCards = getFullHouseCards(rankGroup, cards);
        if (!fullHouseCards.isEmpty()) {
            return new EvaluatedHand(hand, HandRank.FULL_HOUSE, fullHouseCards);
        }

        var flushCards = getFlushCards(hand);
        if (!flushCards.isEmpty()) {
            return new EvaluatedHand(hand, HandRank.FLUSH, flushCards);
        }

        var straightCards = getStraightCards(hand);
        if (!straightCards.isEmpty()) {
            return new EvaluatedHand(hand, HandRank.STRAIGHT, straightCards);
        }

        var threeOfAKindCards = getByCount(rankGroup, cards, 3);
        if (!threeOfAKindCards.isEmpty()) {
            return new EvaluatedHand(hand, HandRank.THREE_OF_A_KIND, threeOfAKindCards);
        }
	    
        var twoPairCards = getTwoPairCards(rankGroup, cards);
        if (!twoPairCards.isEmpty()) {
            return new EvaluatedHand(hand, HandRank.TWO_PAIR, twoPairCards);
        }
        
	    var pairCards = getByCount(rankGroup, cards, 2);
	    if(!pairCards.isEmpty()) {
	    	return new EvaluatedHand(hand, HandRank.PAIR, pairCards);
	    }

	    return new EvaluatedHand(hand, HandRank.HIGH_CARD, getBestCardForHighCard(hand));
	}
	
	private static Map<Rank, Long> groupByRank(List<Card> cards) {
	    return cards.stream().collect(Collectors.groupingBy(Card::rank, Collectors.counting()));
	}
	
	
    private static List<Card> getTwoPairCards(Map<Rank, Long> rankGroup, List<Card> cards) {
        var isTwoPair = rankGroup.values().stream().filter(count -> count == 2L).count() == 2L;
        return isTwoPair ? cards.stream().filter(c -> rankGroup.get(c.rank()) == 2L).toList() : List.of();
    }
	
    private static List<Card> getStraightCards(Hand hand) {
        var cards = hand.cards();
        var orders = cards.stream().map(c -> c.rank().ordinal()).sorted().toList();
        
        // Low card A-2-3-4-5 -> {0,1,2,3,12}
        var isStraight = orders.equals(List.of(0, 1, 2, 3, 12))
                || IntStream.range(1, orders.size()).allMatch(i -> orders.get(i) == orders.get(i - 1) + 1);

        return isStraight ? cards : List.of();
    }
	

    private static List<Card> getFlushCards(Hand hand) {
        var cards = hand.cards();
        return cards.stream().map(Card::suit).distinct().count() == 1 ? cards : List.of();
    }

	
    private static List<Card> getFullHouseCards(Map<Rank, Long> rankGroup, List<Card> cards) {
        return rankGroup.values().containsAll(List.of(2L, 3L)) ? cards : List.of();
    }
	
    private static List<Card> getStraightFlushCards(Hand hand) {
        var straight = getStraightCards(hand);
        var flush = getFlushCards(hand);
        return (!straight.isEmpty() && !flush.isEmpty()) ? hand.cards() : List.of();
    }
    
    private static List<Card> getBestCardForHighCard(Hand hand) {
        return hand.cards().stream().max(Comparator.comparingInt(card -> card.rank().ordinal())).stream().toList();
    }            

	private static List<Card> getByCount(Map<Rank, Long> rankGroup,List<Card> cards, long count) {
	    return cards.stream().filter(c -> rankGroup.get(c.rank()) == count).toList();
	}
}
