package burlap.behavior.singleagent.composite;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import burlap.behavior.singleagent.Policy;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TransitionProbability;
import burlap.oomdp.singleagent.GroundedAction;

import burlap.oomdp.singleagent.classbased.*;

public class GreedyClassBasedPolicy extends Policy {
	Map<String,ClassBasedValueFunction> classValues; List<GroundedAction> actions;
	public GreedyClassBasedPolicy(Map<String,ClassBasedValueFunction> classValues,  List<GroundedAction> actions){
		this.classValues = classValues; this.actions = actions;
	}
	
	private class ActionValue implements Comparable<ActionValue> {
		double value;	GroundedAction action;
		public ActionValue(double v, GroundedAction a){	value = v; action = a;	}
		public int compareTo(ActionValue other){
			// "Note: this class has a natural ordering that is inconsistent with equals."
			return value == other.value ? 0 : (value < other.value ? -1 : 1);
		}
	}
	
	@Override
	public GroundedAction getAction(State s) {
		List<ActionValue> actionOptions = new ArrayList<ActionValue>(actions.size());
		for(GroundedAction action : actions){
			double totalValue = 0;
			for(TransitionProbability tp : action.action.getTransitions(s,action.params)){
				for(ObjectInstance o : tp.s.getAllObjects()) {
					totalValue += tp.p * classValues.get(o.getObjectClass().name).evaluate(o.getName(), tp.s);
				}
			}
			actionOptions.add(new ActionValue(totalValue,action));
		}
		java.util.Collections.sort(actionOptions);
		return actionOptions.get(actionOptions.size() - 1).action;
	}

	@Override
	public List<ActionProb> getActionDistributionForState(State s) {
		ArrayList<ActionProb> probs = new ArrayList<ActionProb>();
		probs.add(new ActionProb(getAction(s),1));
		return probs;
	}

	@Override
	public boolean isStochastic() {
		return false;
	}

	@Override
	public boolean isDefinedFor(State s) {
		for(String cname : s.getObjectClassesPresent()){
			if(classValues.get(cname) == null) return false;
		}
		return true;
	}

}
