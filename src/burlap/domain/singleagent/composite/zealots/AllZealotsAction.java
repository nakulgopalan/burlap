package burlap.domain.singleagent.composite.zealots;

import java.util.LinkedList;
import java.util.List;

import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.classbased.CompositeAction;
import burlap.oomdp.singleagent.classbased.CompositeActionModel;

public class AllZealotsAction extends CompositeAction {
	
	List<List<String>> params;	
	public AllZealotsAction(String name, Domain domain, List<Action> subactions, List<List<String>> params, CompositeActionModel model){
		super(name,domain,subactions,model);
		//domain.getActions().remove(subactions);
		this.params = params;
	}
	
	@Override
	public List<GroundedAction> getAllGroundings(State s) {
		int[] indices = new int[params.size()];
		int[] maxes = new int[params.size()];
		long reps = 1;
		for(int i = 0; i < indices.length; i++){
			indices[i] = 0;
			maxes[i] = params.get(i).size();
			reps *= maxes[i];
		}
		
		return getNextGrounding(new LinkedList<GroundedAction>(), indices, maxes, reps);
	}
	
	public List<GroundedAction> getNextGrounding(List<GroundedAction> sofar, int[] indices, int[] maxes, long count){
		if(count == 0) return sofar;
		int[] nextIndices = getNextIndices(indices,maxes);
		String[] gParams = new String[params.size()];
		for(int i = 0; i < gParams.length; i++) gParams[i] = params.get(i).get(nextIndices[i]);
		sofar.add(new GroundedAction(this, gParams));
		return getNextGrounding(sofar, nextIndices, maxes, count-1);
		
	}
	
	public int[] getNextIndices(int[] indices, int[] maxes){
		int[] newindices = new int[indices.length];
		boolean carry = false;
		for(int i = 0; i < indices.length; i++){
			newindices[i] = indices[i] + 1 + (carry ?  1 : 0);
			carry = (newindices[i] >= maxes[i]);
			newindices[i] %= maxes[i];
		}
		return newindices;
	}

}
