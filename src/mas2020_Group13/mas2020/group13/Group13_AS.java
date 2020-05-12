package group13;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import genius.core.Bid;
import genius.core.boaframework.AcceptanceStrategy;
import genius.core.boaframework.Actions;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OfferingStrategy;
import genius.core.boaframework.OpponentModel;
import genius.core.uncertainty.UserModel;

/**
 * Acceptance strategy for agent NiceHardHeaded of group 13 of Multi-agents Systems project
 * 
 * @author Shaoya Ren
 *
 */
public class Group13_AS extends AcceptanceStrategy {
	
	// Duration of each phase
	private final double[] endPhases = {0.5, 0.45, 0.05};
	// Possible utility threshold from possible bids in each phase
	private double[] maxThresArray;
	// Opponent tyep, 1 stands for conceder and 2 stands for boulware
	private int opponentType;
	// Size of queue to calculated moves left
	private final int queueSize = 3;
	// Size of queue to store opponent utility
	private final int queueUtilitySize = 15;
	// Queue to store time spent on last 15 rounds
	private Queue<Double> queue;
	// Queue to store opponent utility
	private Queue<Double> queueUtility;
	// Threshold of our acceptance condition
	private double threshold;
	// Phase number, including 1, 2, 3
	private static int phase = 1;
	// Number of moves remaining in this negotiation
	private int movesLeft; 
	
	/**
	 * Empty constructor for the BOA framework.
	 */
	public Group13_AS() {
	}

	public Group13_AS(NegotiationSession negoSession, OfferingStrategy start) throws Exception {
		init(negoSession, start, null, null);
	}
	
	/**
	 * Initialize the acceptance class.
	 */
	@Override
	public void init(NegotiationSession negoSession, OfferingStrategy start, OpponentModel opponentModel,
			Map<String, Double> parameters) throws Exception {
		this.negotiationSession = negoSession;
		this.offeringStrategy = start;
		this.opponentModel = opponentModel;
	}

	/**
	 * Determine whether to accept opponent's bid. 
	 * Change acceptance condition in different phase.
	 * Phase 1 and 2 are rather strict, while phase 3 is not.
	 * In the last 3 rounds (estimated), accept if opponent current bid is better than preiouse worst bid.
	 * 
	 * @return Action of reject of accept
	 */
	@Override
	public Actions determineAcceptability() {
		// Initialize varibles
		Actions decision = Actions.Reject;
		double utilityReceived = 0, prevUtil = 1;
		double[] acceptMultiplier = {0.9, 1.0, 1.1};
		maxThresArray = new double[4];
		queue = new LinkedList<Double>();
		queueUtility = new LinkedList<Double>();
		UserModel userModel = negotiationSession.getUserModel();
		// Preferance uncertainty
		if (userModel != null) {
			List<Bid> bidOrder = userModel.getBidRanking().getBidOrder();
			Bid lastBid = negotiationSession.getOwnBidHistory().getLastBid();
			Bid receivedBid = negotiationSession.getOpponentBidHistory().getLastBid();
			if (lastBid == null || receivedBid == null) {
				return Actions.Reject;
			}
			if (negotiationSession.getTime() > 0.9) {
				if (bidOrder.contains(receivedBid)) {
					double ranking = (bidOrder.size() - bidOrder.indexOf(receivedBid)) / bidOrder.size();
					if (ranking <= 0.1) {
						return Actions.Accept;
					}
					if (bidOrder.contains(lastBid) && bidOrder.indexOf(receivedBid) < bidOrder.indexOf(lastBid)) {
						return Actions.Accept;
					}
				}
			}
			return Actions.Reject;
		}
		try {
			// Effectively this will ensure that the utility is 0 if our agent is first
			if (negotiationSession.getOpponentBidHistory().getHistory().size() > 0) {
				// Get the utility we received from the last opponent bid
				utilityReceived = negotiationSession.getUtilitySpace().getUtility(negotiationSession.getOpponentBidHistory().getLastBid());
			}
			// Predict opponent type
			opponentType = predictOpponentType(negotiationSession.getTime());
			// Track opponent worst best given
			prevUtil = negotiationSession.getUtilitySpace().getUtility(negotiationSession.getOpponentBidHistory().getWorstBidDetails().getBid());//the minimum utility got from opponent bid in history
			// Calculate current phase, threshold array, current threshold, and how many rounds left
			phase = calculateCurrentPhase(negotiationSession.getTime());
			calculateMaxToMinThresholds();
			threshold = calculateThreshold(negotiationSession.getTime());
			movesLeft = calculateMovesLeft();
		} catch (Exception e) {
			e.printStackTrace();
		}
		switch (phase) {
		case 1:
			// Accept only if we received more than 10/9 of the threshold
			if (acceptMultiplier[phase - 1] * utilityReceived >= threshold) {
				decision = Actions.Accept;
			}
			break;
		case 2:
			// Accept only if we received more than the threshold
			if (acceptMultiplier[phase - 1] * utilityReceived >= threshold) {
				decision = Actions.Accept;
			}
			break;
		case 3:
			// If there are more than 3 rounds left, accept only if we received more than 10/11 of the threshold
			if (movesLeft >= 3) {
				if (acceptMultiplier[phase - 1] * utilityReceived >= threshold) {
					decision = Actions.Accept;
				}
			} else {
					// Accept if we received more than worst bid we encounter 
					if(utilityReceived >= prevUtil)	{
						decision = Actions.Accept;
					}
			}
			break;
		}
		return decision;
	}

	/**
	 * Returns the current phase of the negotiation.
	 * 
	 * @return phase of the negotiation
	 */
	public int calculateCurrentPhase(double time) {
		phase = 1;
		if (time > endPhases[0] + endPhases[1]) {
			// Store time used in past rounds
			double lastTime = negotiationSession.getOwnBidHistory().getHistory().get(negotiationSession.getOwnBidHistory().getHistory().size() - 1).getTime();
			double lastLastTime = negotiationSession.getOwnBidHistory().getHistory().get(negotiationSession.getOwnBidHistory().getHistory().size() - 2).getTime();
			double gap = lastTime - lastLastTime;
			queue.add(gap);
			if (queue.size() > queueSize) {
				queue.poll();
			}
			phase = 3;
		} else if (time > endPhases[0]) {
			phase = 2;
		}
		return phase;
	}
	
	/**
	 * Calculate threshold range, maxThresArray[0] is the maximum and maxThresArray[3] is the minimum.
	 * This function is called when calculating the threshold for each phase
	 * 
	 * @throws Exception 
	 */
	public void calculateMaxToMinThresholds() throws Exception {
		maxThresArray[0] = negotiationSession.getUtilitySpace().getUtility(negotiationSession.getUtilitySpace().getMaxUtilityBid());
		maxThresArray[3] = negotiationSession.getUtilitySpace().getUtility(negotiationSession.getUtilitySpace().getMinUtilityBid());
		maxThresArray[1] = (maxThresArray[0] - maxThresArray[3]) * 7 / 8 + maxThresArray[3];
		maxThresArray[2] = (maxThresArray[0] - maxThresArray[3]) * 5 / 8 + maxThresArray[3];
	}   
	
	/**
	 * Calculate threshold for each phase.
	 * Phase 1 the threshold is fixed and high
	 * Phase 2 the threshold changes with opponent 
	 * Phase 3 the threshold decreases quickly to reach an agreement
	 * 
	 * @return current threshold 
	 */
	public double calculateThreshold(double time) {
		switch (phase) {
		case 1:
			threshold = maxThresArray[0]; 
			break;
		case 2:
			if (opponentType == 1) {
				threshold = maxThresArray[0] - (((time - endPhases[0]) / (endPhases[1])) * (maxThresArray[0] - maxThresArray[1]));
			} else {
				threshold = maxThresArray[0] - (((time - endPhases[0]) / (endPhases[1])) * (maxThresArray[0] - maxThresArray[2]));
			}
			break;
		case 3:
			threshold = maxThresArray[2] - (((time - endPhases[0] - endPhases[1]) / (endPhases[2])) * (maxThresArray[2] - maxThresArray[3]));
			break;
		}
		return threshold;
	}

	/**
	 * Calculate the remaining moves.
	 * left moves = left time / (time spent on last 15 moves / queue size)
	 * 
	 * @return number of moves left
	 */
	public int calculateMovesLeft() {
		// Avoid empty queue
		if (queue.isEmpty()) {
			movesLeft = 500;
		} else {
			// Sum up all time in queue and calculated the average time
			double total = 0;
			for (double i : queue) {
				total += i;
			}
			movesLeft = (int) Math.floor((1.0 - negotiationSession.getTime()) / (total / (double) queue.size()));
		}
		return movesLeft;
	}
	
	/**
	 * Calculate the type of opponent, 1 is conceder and 2 is boulware
	 * 
	 */
	public int predictOpponentType(double time) {
		// Default type is boulware
		opponentType = 2;
		// Sum and average of at most last 15 utilities of opponent bid 
		double utilTotal = 0, average = 0;
		// Utility of opponent last bid
		double opponentUtil = opponentModel.getBidEvaluation(negotiationSession.getOpponentBidHistory().getLastBid());
		
		// Update queue
		queueUtility.add(opponentUtil);	
		if(queueUtility.size() > queueUtilitySize) {
			queueUtility.poll();
		}
		
		// Sum the utility up
		for (double i : queueUtility) {
			utilTotal += i;
		}
		// Calculated average
		average = utilTotal / queueUtility.size();
		
		// If opponent bid gives itself less than 0.7 utility, then it possibly not greedy
		if(average < 0.7) {
			return 1;
		} else {
			// Calculate utilities of every 5 bid, and see how much can opponent receive from its own bid
			// If the average of last 5 bid utilities is smaller than 90% of the average utility of those 5 bids before them, then it probably not greedy 
			double temp = 0;
			Double[] queueUtilityArray = queueUtility.toArray(new Double[queueUtility.size()]);
			double[] averageUtility = new double[queueUtility.size() / 5];
			
			// Calculate average of opponent utility recently
			for (int i = 0, j = 0, k = 0; i < queueUtility.size() - 1 - queueUtility.size() % 5; i++, j++) {
				if (j == 5) {
					averageUtility[averageUtility.length - 1 - k] = temp / 5;
					k++;
					temp = 0;
					j = 0;
				} else {
					temp += queueUtilityArray[queueUtility.size() - 1 - i];
				}
			}
			
			// Check whether those utilities are droping rapidly
			for (int i = 0; i < queueUtility.size() / 5; i++) {
				if (averageUtility[i + 1] < 0.9 * averageUtility[i]) {
					return 1;
				}
			}
		}
		return 2;
	}

	@Override
	public String getName() {
		return "Group13_acceptance_strategy";
	}
}