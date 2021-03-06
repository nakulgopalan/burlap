package burlap.oomdp.singleagent.classbased;
import java.util.Map;
import java.util.List;

import burlap.oomdp.core.State;
import burlap.oomdp.core.Domain;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
public abstract class CompositeAction extends Action{
	List<Action> actions;
	public CompositeActionModel model;
	
	public CompositeAction(String n, Domain d, List<Action> a, CompositeActionModel m){
		super(n,d,concatArgClasses(a));
		actions = a;
		model = m;
		
	}
	

	@Override
	public String toString(){
		StringBuffer buf = new StringBuffer();
		for (Action a : actions) {
			buf.append(a.toString());
			buf.append(";");
		}		
		return buf.toString();
	}
	
	
	@Override
	public boolean equals(Object other){
		
		if(this == other){
			return true;
		}
		
		if(!(other instanceof CompositeAction)){
			return false;
		}
		
		
		return this.actions.equals(((CompositeAction)other).actions) && this.model.equals(((CompositeAction)other).model);
	}
	
	private static String[] concatArgClasses(List<Action> la){
		int numArgs = 0;
		for(Action a : la) {
			numArgs += a.getParameterClasses().length;
		}
		
		String [] ourParameterClasses = new String[numArgs];
		numArgs = 0;
		for(Action a : la){
			System.arraycopy(a.getParameterClasses(), 0, ourParameterClasses, numArgs, a.getParameterClasses().length);
			numArgs += a.getParameterClasses().length;
		}
		return ourParameterClasses;
	}
	
	public List<GroundedAction> ground(String[] params){
		List<GroundedAction> gas = new java.util.LinkedList<GroundedAction>();
		int numArgs = 0;
		for(Action a : actions) {
			  gas.add(new GroundedAction(a,java.util.Arrays.copyOfRange(params, numArgs, numArgs + a.getParameterClasses().length)));
			  numArgs += a.getParameterClasses().length;
		}
		return gas;
	}
	
	public abstract List<GroundedAction> getAllGroundings(State s);


	@Override
	protected State performActionHelper(State s, String[] params) {
		return model.performCompositeAction(s, ground(params));
	}

}
