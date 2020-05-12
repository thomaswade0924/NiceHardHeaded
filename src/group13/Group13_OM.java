package group13;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import genius.core.Bid;
import genius.core.bidding.BidDetails;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OpponentModel;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Objective;
import genius.core.issue.ValueDiscrete;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.Evaluator;
import genius.core.utility.EvaluatorDiscrete;

/**
 * Opponent model for agent NiceHardHead of group 13 for Multi-Agent Systems project
 * 
 * @author Siyang Qian
 */
public class Group13_OM extends OpponentModel {

	// Weight coefficient
	private final double learningRate = 0.25;
	// Number of issues
    private int issueNumber;
    // Learning weight
    // private final double beta = 5.0;
    // The value to be added to weights of unchanged issues before normalization.
    private double valueAddition;

    /**
     * Initialize method
     */
	@Override
	public void init(NegotiationSession negotiationSession,	Map<String, Double> parameters) {
		this.negotiationSession = negotiationSession;
		opponentUtilitySpace = (AdditiveUtilitySpace) negotiationSession.getUtilitySpace().copy();
		issueNumber = opponentUtilitySpace.getDomain().getIssues().size();
		valueAddition = learningRate / issueNumber;
		// Set all issue equal weights, and set value weights to 1.
        double oriWeight = 1.0 / issueNumber;
        try {
        	for (Entry<Objective, Evaluator> eva : opponentUtilitySpace.getEvaluators()) {
        		opponentUtilitySpace.unlock(eva.getKey());
        		eva.getValue().setWeight(oriWeight);
        		for (ValueDiscrete value : ((IssueDiscrete) eva.getKey()).getValues()) {
        			((EvaluatorDiscrete) eva.getValue()).setEvaluation(value, 1);
        		}
        	}
        } catch (Exception e) {
        	e.printStackTrace();
        }
	}
	
	/**
	 * Update opponent model using last bid from opponent and current time.
	 */
	@Override
	public void updateModel(Bid opponentBid, double time) {
		if (negotiationSession.getOpponentBidHistory().size() < 2) {
			return;
		}
		double weightSum = 1.0, maxWeight = 1.0;
		Set<Integer> unchangedIssue = new HashSet<>();
		BidDetails currOpponentBid = null, prevOpponentBid = null;
		try {
			currOpponentBid= negotiationSession.getOpponentBidHistory().getHistory().get(negotiationSession.getOpponentBidHistory().size() - 1);
			prevOpponentBid= negotiationSession.getOpponentBidHistory().getHistory().get(negotiationSession.getOpponentBidHistory().size() - 2);
			for (Issue i : opponentUtilitySpace.getDomain().getIssues()) {
				if (currOpponentBid.getBid().getValue(i.getNumber()).equals(prevOpponentBid.getBid().getValue(i.getNumber()))) {
					unchangedIssue.add(i.getNumber());
				}
			}
			// The total sum of issue weights before normalization
			weightSum += valueAddition * unchangedIssue.size();
			maxWeight -= issueNumber * valueAddition / weightSum;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// Set new weight for each issue
		for (int i : unchangedIssue) {
			Objective issue = opponentUtilitySpace.getDomain().getObjectivesRoot().getObjective(i);
			double weight = opponentUtilitySpace.getWeight(i);
			double newWeight;
			newWeight = (unchangedIssue.contains(i) && weight < maxWeight) ? (weight + valueAddition) / weightSum : weight / weightSum;
			opponentUtilitySpace.setWeight(issue, newWeight);
		}
		
		// Update value to be added to weight.
		// This value will decays as time passes. 
		// valueAddition = valueAddition * (1 - Math.pow(time, beta)); //the effect decays over time

		try {
			// Increase the improtance to those issue that offered last time
			for (Entry<Objective, Evaluator> eva : opponentUtilitySpace.getEvaluators()) {
				IssueDiscrete issue = (IssueDiscrete) eva.getKey();
				ValueDiscrete value = (ValueDiscrete) currOpponentBid.getBid().getValue(issue.getNumber());
				((EvaluatorDiscrete) eva.getValue()).setEvaluation(value, ((EvaluatorDiscrete) eva.getValue()).getEvaluationNotNormalized(value) + 1);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Evaluate the utility of input bid for opponent
	 * 
	 * @return utility value
	 */
	@SuppressWarnings("finally")
	@Override
	public double getBidEvaluation(Bid bid) {
		double result = 0;
		try {
			result = opponentUtilitySpace.getUtility(bid);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			return result;
		}
	}

	@Override
	public String getName() {
		return "Group13_opponent_model";
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
}