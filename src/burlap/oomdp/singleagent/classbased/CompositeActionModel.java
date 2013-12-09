package burlap.oomdp.singleagent.classbased;
import java.util.ArrayList;
import java.util.List;

import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.core.TransitionProbability;

public abstract class CompositeActionModel {

	
	/**
	 * Performs {@link JointAction} ja in {@link burlap.oomdp.core.State} s and returns the result.
	 * The input state is not modified by this operation.
	 * @param s the state in which the joint action is performed.
	 * @param ja the joint action to be performed
	 * @return the resulting state.
	 */
	public State performCompositeAction(State s, List<GroundedAction> ja){
		State sp = s.copy();
		this.actionHelper(sp, ja);
		return sp;
	}

	
	/**
	 * Returns the transition probabilities for applying the provided {@link JointAction} action in the given state.
	 * Transition probabilities are specified as list of {@link burlap.oomdp.core.TransitionProbability} objects. The list
	 * is only required to contain transitions with non-zero probability.
	 * @param s the state in which the joint action is performed
	 * @param ja the joint action performed
	 * @return a list of state {@link burlap.oomdp.core.TransitionProbability} objects.
	 */
	public abstract List<TransitionProbability> transitionProbsFor(State s, List<GroundedAction> ja);
	
	
	
	/**
	 * This method is what determines the state when {@link JointAction} ja is executed in {@link burlap.oomdp.core.State} s.
	 * The input state should be directly modified.
	 * @param s the state in which the joint action is performed.
	 * @param ja the joint action to be performed.
	 */
	protected abstract void actionHelper(State s, List<GroundedAction> ja);
	

	
	/**
	 * A helper method for deterministic transition dynamics. This method will return a list containing
	 * one {@link burlap.oomdp.core.TransitionProbability} object which is assigned probability 1
	 * and whose state is determined by querying the {@link performJointAction(State, JointAction)}
	 * method.
	 * @param s the state in which the joint action would be executed
	 * @param ja the joint action to be performed in the state.
	 * @return a list containing one {@link burlap.oomdp.core.TransitionProbability} object which is assigned probability 1
	 */
	protected List<TransitionProbability> deterministicTransitionProbsFor(State s, List<GroundedAction> ja){
		List <TransitionProbability> res = new ArrayList<TransitionProbability>();
		State sp = performCompositeAction(s, ja);
		TransitionProbability tp = new TransitionProbability(sp, 1.);
		res.add(tp);
		return res;
	}

}
