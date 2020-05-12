package group13;

import java.util.List;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import genius.core.bidding.BidDetails;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.NoModel;
import genius.core.boaframework.OMStrategy;
import genius.core.boaframework.OpponentModel;

/**
 * Opponent model strategy for agent NiceHardHeaded of group 13 of Multi-agents Systems project
 * In our design, we only consider the utility of a bid offered by opponent. 
 * So we made few changes, and the OMStrategy class will remain almost the same as default. 
 * 
 * @author Jiayuan Hu
 */
public class Group13_OMS extends OMStrategy {

	/**
	 * Initialize OMStrategy with given input. 
	 * 
	 * @param negotiationSession: includes all negotiation session information. 
	 * @param model: the opponent model we learned in our OM class.
	 * @param parameters: set of parameters. 
	 */
	@Override
	public void init(NegotiationSession negotiationSession, OpponentModel model, Map<String, Double> parameters) {
		super.init(negotiationSession, model, parameters);
	}

	/**
	 * Returns the best bid for the opponent given a set of similarly preferred
	 * bids.
	 * 
	 * @param list of the bids considered for offering.
	 * @return bid to be offered to opponent.
	 */
	@Override
	public BidDetails getBid(List<BidDetails> allBids) {

		// Return the only bid given.
		if (allBids.size() == 1) {
			return allBids.get(0);
		}
		
		// Return a random bid from list if model is not updated.
		if (model instanceof NoModel) {
			Random r = new Random();
			return allBids.get(r.nextInt(allBids.size()));
		}
		
		// Return the bid with highest utility that calculated by our model. 
		double utility = Integer.MIN_VALUE;
		BidDetails res = null;
		for (BidDetails bid : allBids) {
			if (model.getBidEvaluation(bid.getBid()) > utility) {
				utility = model.getBidEvaluation(bid.getBid());
				res = bid;
			}
		}
		return res;
	}

	/**
	 * Since we would like to update our model during the whole negotiationSession,
	 * this method will always return true. 
	 * 
	 * @return true if model may be updated.
	 */
	@Override
	public boolean canUpdateOM() {
		return true;
	}

	/**
	 * There is no parameter in OMS. 
	 * 
	 * @return An empty hashset.
	 */
	@Override
	public Set<BOAparameter> getParameterSpec() {
		Set<BOAparameter> set = new HashSet<BOAparameter>();
		return set;
	}

	@Override
	public String getName() {
		return "Group13_opponent_model_strategy";
	}
}