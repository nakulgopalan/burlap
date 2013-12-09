package burlap.oomdp.singleagent.compositereward;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import burlap.oomdp.singleagent.compositeaction.CompositeAction;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;

public class ClassBasedRewardFunction implements RewardFunction {

	protected Map<String, RewardFunction>  classRewards;
	
	public ClassBasedRewardFunction( Map<String, RewardFunction> initClasses){
		classRewards = initClasses;
		
	}
	
	public void setClassReward(String cname, RewardFunction creward){
		classRewards.put(cname, creward);
		
	}
	
	public void clearClassReward(String cname){
		classRewards.remove(cname);
	}
	
	@Override
	public double reward(State s, GroundedAction a, State sprime) {
		double r =0;
		try{
			
			
			Set<String> classNames = s.getObjectClassesPresent();
			List<GroundedAction> gas = ((CompositeAction)(a.action)).ground(a.params);
			Map<String,GroundedAction> gasByName = new HashMap<String,GroundedAction>();
			for(GroundedAction aa : gas){
				gasByName.put(aa.params[0], aa);
			}
			for(String x : classNames){
				List<ObjectInstance> objs = s.getObjectsOfTrueClass(x);
				RewardFunction rf = classRewards.get(x);
				for(ObjectInstance o : objs){
					State ostate = new State();
					ostate.addObject(o);
					State ostateprime = new State();
					ostateprime.addObject(sprime.getObject(o.getName()));
					r+=rf.reward(ostate, gasByName.get(o.getName()), ostateprime);
				}
			}			
		} catch (ClassCastException e) {
			throw new IllegalArgumentException("class based reward functions must take composite actions");
		}
		
		return r;
	}


}
