package group13;

import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

import genius.core.Bid;
import genius.core.BidHistory;
import genius.core.bidding.BidDetails;
import genius.core.bidding.BidDetailsStrictSorterUtility;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.NoModel;
import genius.core.boaframework.OMStrategy;
import genius.core.boaframework.OfferingStrategy;
import genius.core.boaframework.OpponentModel;
import genius.core.boaframework.OutcomeSpace;
import negotiator.boaframework.opponentmodel.DefaultModel;
import genius.core.misc.Range;
import genius.core.uncertainty.UserModel;

/**
 * Bidding strategy for agent NiceHardHead of group 13 of Multi-Agent Systems project
 * 
 * @author Jiayaun Hu
 */
public class Group13_BS extends OfferingStrategy{

	private Bid bestBid, worstBid;
	private double maxUtil, minUtil, alpha; 
	private final double TIME_MAX = 1.0;
	private final double BETA = 0.01;
	private OutcomeSpace outcome;
	
	/**
	 * Empty constructor for BOA framework
	 */
	public Group13_BS() {
	}

	/**
	 * Init required for BOA framework.
	 * Try to get the best and worst bid and their utility in this domain.
	 */
	@Override
	public void init(NegotiationSession negotiationSession, 
			OpponentModel opponentModel, 
			OMStrategy omStrategy, 
			Map<String, Double> parameters) throws Exception {
		if (opponentModel instanceof DefaultModel) {
			opponentModel = new NoModel();
		}
		this.negotiationSession = negotiationSession;
		this.opponentModel = opponentModel;
		this.omStrategy = omStrategy;
		this.outcome = new OutcomeSpace(negotiationSession.getUtilitySpace());
		try {
			this.bestBid = negotiationSession.getUtilitySpace().getMaxUtilityBid();
			this.worstBid = negotiationSession.getUtilitySpace().getMinUtilityBid();
			this.maxUtil = negotiationSession.getUtilitySpace().getUtility(bestBid);
			this.minUtil = negotiationSession.getUtilitySpace().getUtility(worstBid);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Return the best bid in current domain at the first round. 
	 */
	@Override
	public BidDetails determineOpeningBid() {
		return negotiationSession.getMaxBidinDomain();
	}

	/**
	 * Determine next bid to be offered to opponent by calling getMyBid method.
	 * Otherwise return the best bid in current domain. 
	 * 
	 * @return bid to be offered to opponent.
	 */
	@SuppressWarnings("finally")
	@Override
	public BidDetails determineNextBid() {
		// Preferance uncertainty
		UserModel userModel = negotiationSession.getUserModel();
		if (userModel != null) {
			// List<Bid> bidOrder = userModel.getBidRanking().getBidOrder();
			return new BidDetails(userModel.getBidRanking().getMaximalBid(), userModel.getBidRanking().getHighUtility());
		}
		BidDetails bid = negotiationSession.getMaxBidinDomain();
		BidDetails opponentLastBid = negotiationSession.getOpponentBidHistory().getLastBidDetails();
		if (opponentLastBid == null) {
			return bid;
		}
		try {
			bid = getMyBid(new Range(this.minUtil, this.maxUtil));
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			return bid;
		}
	}

	/**
	 * Get bid details within given range using combination of time-dependent and behaviour-dependent tactic.
	 * Compute the target utility and keep searching possible bids near target. 
	 * All possible bid in one search will be add to a priority queue which is sorted by their utility. 
	 * Once find one or more possible bid, poll the bid with maximum utility from the queue. 
	 * 
	 * @param range: record the lower and upper bound of possible bids.
	 * @return next bid to be offered
	 */
	private BidDetails getMyBid(Range range) {
		// Calculate alpha and utility of two tactic
		this.alpha = calculateAlpha(negotiationSession.getTime(), BETA);
		double timeDependentTargetUtility = getTimeDependentTargetUtility(range);
		double behaviourDependentTargetUtility = getBehaviourDependentTargetUtility(range);
		
		// Calculate target utility using linear combination of two utility
		double timeWeight = 0.2, behaviourWeight = 0.8;
		double target = timeWeight * timeDependentTargetUtility + behaviourWeight * behaviourDependentTargetUtility;
		
		try {
			// Initialize range around the target and priority queue for candidates
			Range targetRange = new Range(target - 0.01, target + 0.01);
			Queue<BidDetails> bidCandidate = new PriorityQueue<>(new BidDetailsStrictSorterUtility());
			
			// Repeat this loop until we find such bid
			while (bidCandidate.peek() == null) {
				// Keep searching possible bid and if their utility is larger than 0.5, add them to the queue
				for (BidDetails bid : this.outcome.getBidsinRange(targetRange)) {
					if (this.opponentModel.getBidEvaluation(bid.getBid()) > 0.5) {
						bidCandidate.add(bid);
					}
				}
				
				// Expand the range in case we can not find bid within this range
				targetRange.setLowerbound(Math.max(targetRange.getLowerbound() - 0.01, 0.5));
				targetRange.setUpperbound(Math.min(targetRange.getUpperbound() + 0.03, 1.0));
			}
			
			// Return the head of the queue, repersenting the bid with highest utility in all possible bids
			return bidCandidate.poll();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// Return the best bid if some mistakes happen
		BidDetails nextBid = this.outcome.getBidNearUtility(target);
		return nextBid;
	}
	
	/**
	 * Agent will concede more rapidly as time passes. 
	 * Alpha is parameter depend on time and beta.
	 * Beta determines whether agent perfer to conceder or not.
	 * 
	 * @return time dependent utility value
	 */
	private double getTimeDependentTargetUtility(Range range) {
		return range.getLowerbound() + (1 - this.alpha) * (range.getUpperbound() - range.getLowerbound());
	}
	
	/**
	 * Calculate current alpha value using current time and beta.
	 * 
	 * @param time: current time
	 * @param beta: whether agent would like to concede or not
	 * @return alpha value
	 */
	public double calculateAlpha(double time, double beta) {
		return Math.pow((time / TIME_MAX), (1 / beta));
	}
	
	/**
	 * Agent will imitate opponent's behaviour using combination of Relative Tit-For-Tat.
	 * Namely, agent will compute the variation rate of opponent's last bid and bid of three round ago.
	 * Then agent take this ratio times the utility of our bid from the round before last round. 
	 * Finally this value will be our behaviour dependent utility.
	 * 
	 * @return behaviour dependent utility value
	 */
	private double getBehaviourDependentTargetUtility(Range range) {
		BidHistory opponentBids = negotiationSession.getOpponentBidHistory();
		int opponentRound = opponentBids.getHistory().size();
		double P = this.maxUtil;
		
		// If model is not updated, return same value as time dependent tactic
		if (opponentModel instanceof DefaultModel) {
			return getTimeDependentTargetUtility(range);
		}
		
		// If there is not enough round, return P immediately
		if (opponentRound == 1 || opponentRound == 2) {
			return P;
		}
		else {
			try {
				// Calculate P using Relative Tit-For-Tat 
				P = negotiationSession.getUtilitySpace().getUtility(negotiationSession.getOwnBidHistory().getHistory().get(negotiationSession.getOwnBidHistory().getHistory().size() - 2).getBid());
				P /= opponentModel.getBidEvaluation(opponentBids.getHistory().get(opponentBids.getHistory().size() - 3).getBid());
				P *= opponentModel.getBidEvaluation(opponentBids.getHistory().get(opponentBids.getHistory().size() - 1).getBid());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// Return P value but within the maximum and minimum bound
		return P < this.minUtil ? this.minUtil : (P > this.maxUtil ? this.maxUtil : P);
	}

	@Override
	public String getName() {
		return "Group13_bidding_strategy";
	}

}

