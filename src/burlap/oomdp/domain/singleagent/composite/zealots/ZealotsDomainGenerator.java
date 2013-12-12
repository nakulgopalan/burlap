package burlap.oomdp.domain.singleagent.composite.zealots;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import burlap.behavior.singleagent.composite.GreedyClassBasedPolicy;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.oomdp.auxiliary.DomainGenerator;
import burlap.oomdp.core.Attribute;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectClass;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.core.TransitionProbability;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.classbased.ClassBasedValueFunction;
import burlap.oomdp.singleagent.classbased.CompositeAction;
import burlap.oomdp.singleagent.classbased.CompositeActionModel;
import burlap.oomdp.singleagent.classbased.SolveLP;

public class ZealotsDomainGenerator implements DomainGenerator {

	int numguys, upperHealth, lowerHealth;
	public ZealotsDomainGenerator(int numguys,int lowerHealth,int upperHealth){
		this.numguys = numguys; this.upperHealth = upperHealth; this.lowerHealth = lowerHealth;
	}

	@Override
	public Domain generateDomain() {
		Domain ourDomain = new SADomain();
		ObjectClass goodguys = new ObjectClass(ourDomain, "good-zealot");
		ObjectClass badguys = new ObjectClass(ourDomain, "bad-zealot");

		Attribute health = new Attribute(ourDomain, "health", Attribute.AttributeType.DISC);
		health.setDiscValuesForRange(lowerHealth, upperHealth, 1);

		goodguys.addAttribute(health);
		badguys.addAttribute(health);

		Attribute enemy = new Attribute(ourDomain, "enemy", Attribute.AttributeType.RELATIONAL);
		goodguys.addAttribute(enemy);
		badguys.addAttribute(enemy);

		Attribute enemies = new Attribute(ourDomain, "enemies", Attribute.AttributeType.MULTITARGETRELATIONAL);
		goodguys.addAttribute(enemies);

		return ourDomain;
	}

	public State getInitState(Domain domain) {
		State state = new State();
		List<String> enemies = new ArrayList<String>(numguys);
		for(int i = 0; i < numguys; i++){
			state.addObject(new ObjectInstance(domain.getObjectClass("good-zealot"),"good-zealot " + i));
			state.addObject(new ObjectInstance(domain.getObjectClass("bad-zealot"),"bad-zealot " + (numguys+i)));
			enemies.add("bad-zealot " + (numguys+i));
		}

		for(int i = 0; i < numguys; i++){
			state.getObject("good-zealot " + i).addRelationalTarget("enemy", "bad-zealot " + (numguys+i));
			state.getObject("bad-zealot " + (numguys+i)).addRelationalTarget("enemy", "good-zealot " + i);
			state.getObject("good-zealot " + i).setValue("health", upperHealth);
			state.getObject("bad-zealot " + (numguys+i)).setValue("health", upperHealth);
			for (String enemy : enemies){
				state.getObject("good-zealot " + i).addRelationalTarget("enemies", enemy);
			}
		}

		return state;
	}

	public CompositeAction getAttackAction(Domain domain) {

		List<Action> actionList = new ArrayList<Action>(2*numguys);
		List<List<String>> paramsList = new ArrayList<List<String>>(2*numguys);
		List<String> enemies = new ArrayList<String>(2*numguys);
		final int curMin = lowerHealth;
		for(int i = 0; i < numguys; i++)enemies.add("bad-zealot " + (numguys + 3));
		for(int i = 0; i < numguys; i++){
			actionList.add(new Action("attack (good-zealot " + i + " )",domain, "bad-zealot"){

				@Override
				protected State performActionHelper(State s, String[] params) {
					State newState = s.copy();
					ObjectInstance target = newState.getObject(params[0]);
					target.setValue("health", Math.max(curMin, target.getDiscValForAttribute("health") - 1));
					return newState;
				}

			});
			paramsList.add(enemies);
		}

		for(int i = numguys; i < 2*numguys; i++){
			final int j = i;
			actionList.add(new Action("attack (bad-zealot " + i + " )",domain, ""){

				@Override
				protected State performActionHelper(State s, String[] params) {
					State newState = s.copy();
					ObjectInstance actor = newState.getObject("bad-zealot " + j);
					ObjectInstance target = newState.getObject(actor.getStringValForAttribute("enemy"));
					target.setValue("health", Math.max(curMin, target.getDiscValForAttribute("health") - 1));
					return newState;
				}

			});
			paramsList.add(new ArrayList<String>(0));
		}

		return new AllZealotsAction("attack",domain,actionList,paramsList,new CompositeActionModel(){
			public List<TransitionProbability> transitionProbsFor(State s, List<GroundedAction> ja){ return deterministicTransitionProbsFor(s, ja); }
			protected State actionHelper(State s, List<GroundedAction> ja){
				for(GroundedAction a : ja) s = a.executeIn(s); 
				return s;
			}
		});
	}

	public TerminalFunction getTerminalFunction(Domain domain){
		final int curMin = lowerHealth;
		return new TerminalFunction(){
			@Override
			public boolean isTerminal(State s) {
				int goodcount = 0;
				int badcount = 0;
				for(ObjectInstance o : s.getObjectsOfTrueClass("good-zealot")) goodcount += (o.getDiscValForAttribute("health") > curMin) ? 1 : 0;
				for(ObjectInstance o : s.getObjectsOfTrueClass("bad-zealot")) badcount += (o.getDiscValForAttribute("health") > curMin) ? 1 : 0;
				return goodcount == 0 || badcount == 0;
			}
		};
	}

	public RewardFunction getRewardFunction(Domain domain){
		final int curMin = lowerHealth;
		return new RewardFunction(){
			@Override
			public double reward(State s, GroundedAction a, State sprime) {
				double out = 0;
				for(ObjectInstance o : s.getObjectsOfTrueClass("bad-zealot")){
					out += (o.getDiscValForAttribute("health") == curMin) ? 10 : 0;
				}
				return out;
			}
		};
	}

	public HashMap<String,ClassBasedValueFunction> getFactoredSolution(Domain domain, double gamma){

		SolveLP solver = new SolveLP(gamma);
		HashMap<String,ClassBasedValueFunction> cbvfs = new HashMap<String,ClassBasedValueFunction>(3, 0.75f);

		ClassBasedValueFunction zealotValue = new ClassBasedValueFunction("good-zealot",new TreeSet<String>(), new TreeSet<ClassBasedValueFunction.LinkedAttribute>());
		zealotValue.addAttribute("health");
		zealotValue.addForeignAttribute(zealotValue.new LinkedAttribute("good-zealot","enemy","bad-zealot","health"));

		cbvfs.put("good-zealot", zealotValue);

		ClassBasedValueFunction enemyValue = new ClassBasedValueFunction("bad-zealot",new TreeSet<String>(), new TreeSet<ClassBasedValueFunction.LinkedAttribute>());
		enemyValue.addAttribute("health");

		cbvfs.put("bad-zealot", enemyValue);

		solver.solveLP(domain, getInitState(domain), cbvfs, getRewardFunction(domain));
		return cbvfs;
	}

	public GreedyClassBasedPolicy getPolicyFromFactoredSolution(Domain domain, HashMap<String,ClassBasedValueFunction> cbvfs){
		LinkedList<GroundedAction> gas = new LinkedList<GroundedAction>();
		State init = getInitState(domain);
		for(Action a : domain.getActions()){
			if (a instanceof CompositeAction) gas.addAll(((CompositeAction)a).getAllGroundings(init));
		}
		// Note that our actions are not state dependent. This makes life a lot easier. 
		// If yours are, don't fret, you can still use this code, you just need to instantiate different
		// Policies for different sets of States.
		return new GreedyClassBasedPolicy(cbvfs,gas);
	}

	public DiscreteStateHashFactory getHashFactory(Domain domain){
		HashMap<String,List<Attribute>> discAttrs = new HashMap<String,List<Attribute>>(3,0.75f);
		for(ObjectClass c : domain.getObjectClasses()){
			ArrayList<Attribute> attrs = new ArrayList<Attribute>(c.numAttributes());
			for(Attribute a : c.attributeList) if (a.type == Attribute.AttributeType.DISC) attrs.add(a);
			discAttrs.put(c.name, attrs);
		}
		return new DiscreteStateHashFactory(discAttrs);
	}

	public GreedyQPolicy getPolicyByVI(Domain domain, double gamma, double tol, int maxIterations){

		ValueIteration vi = new ValueIteration(domain, getRewardFunction(domain), getTerminalFunction(domain), gamma, getHashFactory(domain), tol, maxIterations);

		State init = getInitState(domain);
		vi.planFromState(init);
		System.out.printf("Found initial state to have value %f\n",vi.value(init));

		return new GreedyQPolicy(vi);
	}
	
	public static void main(String[] args){
		ZealotsDomainGenerator zdg = new ZealotsDomainGenerator(1,0,4);
		Domain oneVOne = zdg.generateDomain();
		long start = System.currentTimeMillis();
		zdg.getPolicyFromFactoredSolution(oneVOne, zdg.getFactoredSolution(oneVOne, 0.95));
		System.out.printf("Factored solution + factored policy took: %f seconds for 1v1\n",(System.currentTimeMillis() - start)/1000f);
		start = System.currentTimeMillis();
		zdg.getPolicyByVI(oneVOne, 0.95, 0.01, 1000);
		System.out.printf("Value iteration + greedy-q policy took: %f seconds for 1v1\n",(System.currentTimeMillis() - start)/1000f);
		
		
		zdg = new ZealotsDomainGenerator(2,0,4);
		Domain twoVTwo = zdg.generateDomain();
		start = System.currentTimeMillis();
		zdg.getFactoredSolution(twoVTwo, 0.95);
		System.out.printf("Factored solution + factored policy took: %f seconds for 2v2\n",(System.currentTimeMillis() - start)/1000f);
		start = System.currentTimeMillis();
		zdg.getPolicyByVI(twoVTwo, 0.95, 0.01, 1000);
		System.out.printf("Value iteration + greedy-q policy took: %f seconds for 2v2\n",(System.currentTimeMillis() - start)/1000f);
		
		
		zdg = new ZealotsDomainGenerator(1,0,4);
		Domain threeVThree = zdg.generateDomain();
		start = System.currentTimeMillis();
		zdg.getFactoredSolution(threeVThree, 0.95);
		System.out.printf("Factored solution + factored policy took: %f seconds for 3v3\n",(System.currentTimeMillis() - start)/1000f);
		start = System.currentTimeMillis();
		zdg.getPolicyByVI(threeVThree, 0.95, 0.01, 1000);
		System.out.printf("Value iteration + greedy-q policy took: %f seconds for 3v3\n",(System.currentTimeMillis() - start)/1000f);
		
	}
}
