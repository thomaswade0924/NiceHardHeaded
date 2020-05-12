package group13;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import group13.Group13_AS;
import group13.Group13_BS;
import group13.Group13_OM;
import group13.Group13_OMS;
import genius.core.boaframework.AcceptanceStrategy;
import genius.core.boaframework.BoaParty;
import genius.core.boaframework.OMStrategy;
import genius.core.boaframework.OfferingStrategy;
import genius.core.boaframework.OpponentModel;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.ValueDiscrete;
import genius.core.parties.NegotiationInfo;
import genius.core.uncertainty.AdditiveUtilitySpaceFactory;
import genius.core.utility.AbstractUtilitySpace;

@SuppressWarnings("serial")
public class Group13_NiceHardHead extends BoaParty {

	@Override
	public void init(NegotiationInfo info) {
		AcceptanceStrategy ac  = new Group13_AS();
		OfferingStrategy   os  = new Group13_BS();
		OpponentModel      om  = new Group13_OM();
		OMStrategy         oms = new Group13_OMS();
		
		Map<String, Double> noParams  = Collections.emptyMap();
		
		// Initialize all the components of this party to the choices defined above
		configure(ac, noParams, 
				os,	noParams, 
				om, noParams,
				oms, noParams);
		super.init(info);
	}

	@Override
	public AbstractUtilitySpace estimateUtilitySpace() 
	{
		AdditiveUtilitySpaceFactory additiveUtilitySpaceFactory = new AdditiveUtilitySpaceFactory(getDomain());
		List<IssueDiscrete> issues = additiveUtilitySpaceFactory.getIssues();
		for (IssueDiscrete i : issues)
		{
			additiveUtilitySpaceFactory.setWeight(i, rand.nextDouble());
			for (ValueDiscrete v : i.getValues())
				additiveUtilitySpaceFactory.setUtility(i, v, rand.nextDouble());
		}
		
		// Normalize the weights, since we picked them randomly in [0, 1]
		additiveUtilitySpaceFactory.normalizeWeights();
		
		// The factory is done with setting all parameters, now return the estimated utility space
		return additiveUtilitySpaceFactory.getUtilitySpace();
	}
	
	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return "MAS_Group13_NiceHardHead";
	}

}
