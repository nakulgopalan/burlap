package burlap.domain.singleagent.composite.zealots;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import burlap.behavior.singleagent.Policy;
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
		
		Action attackAction = getAttackAction(ourDomain);

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
		for(int i = 0; i < numguys; i++)enemies.add("bad-zealot " + (numguys + i));
		for(int i = 0; i < numguys; i++){
			final int j = i;
			actionList.add(new Action("attack (good-zealot " + i + " )",domain, "bad-zealot",true){

				@Override
				protected State performActionHelper(State s, String[] params) {
					State newState = s.copy();
					ObjectInstance actor = newState.getObject("good-zealot " + j);
					if (actor.getDiscValForAttribute("health") > 0){
						ObjectInstance target = newState.getObject(params[0]);
						target.setValue("health", Math.max(curMin, target.getDiscValForAttribute("health") - 1));
					}
					return newState;
				}

			});
			paramsList.add(enemies);
		}

		for(int i = numguys; i < 2*numguys; i++){
			final int j = i;
			actionList.add(new Action("attack (bad-zealot " + i + " )",domain, "", true){

				@Override
				protected State performActionHelper(State s, String[] params) {
					State newState = s.copy();
					ObjectInstance actor = newState.getObject("bad-zealot " + j);
					if (actor.getDiscValForAttribute("health") > 0){
						ObjectInstance target = newState.getObject(actor.getStringValForAttribute("enemy"));
						target.setValue("health", Math.max(curMin, target.getDiscValForAttribute("health") - 1));
					}
					return newState;
				}

			});
		}

		return new AllZealotsAction("attack",domain,actionList,paramsList,new CompositeActionModel(){
			public List<TransitionProbability> transitionProbsFor(State s, List<GroundedAction> ja){ return deterministicTransitionProbsFor(s, ja); }
			protected State actionHelper(State s, List<GroundedAction> ja){
				HashMap<String,Integer> deltas = new HashMap<String,Integer>((2*numguys*4/3),0.75f);
				for(ObjectInstance o : s.getAllObjects()) deltas.put(o.getName(), 0);
				for(GroundedAction a : ja){
					State ns = a.executeIn(s);
					for(ObjectInstance o : ns.getAllObjects()){
						deltas.put	(o.getName(),
										(deltas.get(o.getName()) + 
											(o.getDiscValForAttribute("health") - s.getObject(o.getName()).getDiscValForAttribute("health"))
										)
									);
					}
				}
				s = s.copy();
				for(ObjectInstance o : s.getAllObjects()) o.setValue("health", Math.max(0, o.getDiscValForAttribute("health") + deltas.get(o.getName())));
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
		final Domain curDomain = domain;
		return new RewardFunction(){
			private TerminalFunction tf = getTerminalFunction(curDomain);
			
			@Override
			public double reward(State s, GroundedAction a, State sprime) {
				double out = 0;
				/*if (!tf.isTerminal(s))*/ for(ObjectInstance o : sprime.getObjectsOfTrueClass("bad-zealot")){
					out += (o.getDiscValForAttribute("health") != curMin) ? -10 : 0;
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
		//enemyValue.addAttribute("health");

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
		HashMap<String,ClassBasedValueFunction> cbvf1 = zdg.getFactoredSolution(oneVOne, 0.4);
		Policy fs1 = zdg.getPolicyFromFactoredSolution(oneVOne,cbvf1);
		System.out.printf("Factored solution + factored policy took: %f seconds for 1v1\n",(System.currentTimeMillis() - start)/1000f);
		start = System.currentTimeMillis();
		Policy gq = zdg.getPolicyByVI(oneVOne, 0.4, 0.001, 10000);
		System.out.printf("Value iteration + greedy-q policy took: %f seconds for 1v1\n",(System.currentTimeMillis() - start)/1000f);
		
		System.out.println();
		
		//*
		System.out.println("Running 1v1 factored policy on 1v1");
		State current = zdg.getInitState(oneVOne);
		double cumulativeReward = 0;
		TerminalFunction tf = zdg.getTerminalFunction(oneVOne);
		RewardFunction rf = zdg.getRewardFunction(oneVOne);
		while(!tf.isTerminal(current)){
			GroundedAction ga = fs1.getAction(current);
			State next = ga.executeIn(current);
			cumulativeReward += rf.reward(current, ga, next);
			
			System.out.print("Stats:\n");
			for(ObjectInstance o : current.getAllObjects() ) 
				System.out.printf("%s - %s\t;\t",o.getName(),o.getDiscValForAttribute("health"));
			System.out.print("\n");
			for(ObjectInstance o : next.getAllObjects() ) 
				System.out.printf("%s - %s\t;\t",o.getName(),o.getDiscValForAttribute("health"));
			System.out.print("\n");
			System.out.println(cumulativeReward + "\n");
			
			current = next;
		}
		
		System.out.println("\n\n\n");
		
		System.out.println("Running 1v1 greedy-q policy on 1v1");
		current = zdg.getInitState(oneVOne);
		cumulativeReward = 0;
		while(!tf.isTerminal(current)){
			GroundedAction ga = gq.getAction(current);
			State next = ga.executeIn(current);
			cumulativeReward += rf.reward(current, ga, next);
			
			System.out.print("Stats:\n");
			for(ObjectInstance o : current.getAllObjects() ) 
				System.out.printf("%s - %s\t;\t",o.getName(),o.getDiscValForAttribute("health"));
			System.out.print("\n");
			for(ObjectInstance o : next.getAllObjects() ) 
				System.out.printf("%s - %s\t;\t",o.getName(),o.getDiscValForAttribute("health"));
			System.out.print("\n");
			System.out.println(cumulativeReward + "\n");
			
			current = next;
		}
		//*/

		System.out.println("\n\n----------------------------------\n\n");
		
		zdg = new ZealotsDomainGenerator(2,0,4);
		Domain twoVTwo = zdg.generateDomain();
		start = System.currentTimeMillis();
		HashMap<String,ClassBasedValueFunction> cbvf2 = zdg.getFactoredSolution(twoVTwo, 0.4);
		Policy fs2 = zdg.getPolicyFromFactoredSolution(twoVTwo,cbvf2);
		System.out.printf("Factored solution + factored policy took: %f seconds for 2v2\n",(System.currentTimeMillis() - start)/1000f);
		start = System.currentTimeMillis();
		gq = zdg.getPolicyByVI(twoVTwo, 0.4, 0.001, 10000);
		System.out.printf("Value iteration + greedy-q policy took: %f seconds for 2v2\n",(System.currentTimeMillis() - start)/1000f);

		System.out.println("");
		
		//*
		System.out.println("Running 2v2 factored policy on 2v2");
		current = zdg.getInitState(twoVTwo);
		cumulativeReward = 0;
		tf = zdg.getTerminalFunction(twoVTwo);
		rf = zdg.getRewardFunction(twoVTwo);
		while(!tf.isTerminal(current)){
			GroundedAction ga = fs2.getAction(current);
			State next = ga.executeIn(current);
			cumulativeReward += rf.reward(current, ga, next);
			
			System.out.print("Stats:\n");
			for(ObjectInstance o : current.getAllObjects() ) 
				System.out.printf("%s - %s\t;\t",o.getName(),o.getDiscValForAttribute("health"));
			System.out.print("\n");
			for(ObjectInstance o : next.getAllObjects() ) 
				System.out.printf("%s - %s\t;\t",o.getName(),o.getDiscValForAttribute("health"));
			System.out.print("\n");
			System.out.println(cumulativeReward + "\n");
			
			current = next;
		}
		
		System.out.println("\n\n\n");
		
		/*
		Policy fs12 = zdg.getPolicyFromFactoredSolution(twoVTwo,cbvf1);
		System.out.println("Running 1v1 factored policy on 2v2");
		current = zdg.getInitState(twoVTwo);
		cumulativeReward = 0;
		while(!tf.isTerminal(current)){
			GroundedAction ga = fs12.getAction(current);
			State next = ga.executeIn(current);
			cumulativeReward += rf.reward(current, ga, next);
			
			System.out.print("Stats:\n");
			for(ObjectInstance o : current.getAllObjects() ) 
				System.out.printf("%s - %s\t;\t",o.getName(),o.getDiscValForAttribute("health"));
			System.out.print("\n");
			for(ObjectInstance o : next.getAllObjects() ) 
				System.out.printf("%s - %s\t;\t",o.getName(),o.getDiscValForAttribute("health"));
			System.out.print("\n");
			System.out.println(cumulativeReward + "\n");
			
			current = next;
		}
		//*/
		//*
		System.out.println("\n\n\n");
		
		System.out.println("Running 2v2 greedy-q policy on 2v2");
		current = zdg.getInitState(twoVTwo);
		cumulativeReward = 0;
		while(!tf.isTerminal(current)){
			GroundedAction ga = gq.getAction(current);
			State next = ga.executeIn(current);
			cumulativeReward += rf.reward(current, ga, next);
			
			System.out.print("Stats:\n");
			for(ObjectInstance o : current.getAllObjects() ) 
				System.out.printf("%s - %s\t;\t",o.getName(),o.getDiscValForAttribute("health"));
			System.out.print("\n");
			for(ObjectInstance o : next.getAllObjects() ) 
				System.out.printf("%s - %s\t;\t",o.getName(),o.getDiscValForAttribute("health"));
			System.out.print("\n");
			System.out.println(cumulativeReward + "\n");
			
			current = next;
		}
		//*/
		
		//*
		System.out.println("\n\n----------------------------------\n\n");
		
		zdg = new ZealotsDomainGenerator(3,0,4);
		Domain threeVThree = zdg.generateDomain();
		start = System.currentTimeMillis();
		HashMap<String,ClassBasedValueFunction> cbvf3 = zdg.getFactoredSolution(threeVThree, 0.4);
		Policy fs3 = zdg.getPolicyFromFactoredSolution(threeVThree,cbvf3);
		System.out.printf("Factored solution + factored policy took: %f seconds for 3v3\n",(System.currentTimeMillis() - start)/1000f);
		start = System.currentTimeMillis();
		gq = zdg.getPolicyByVI(threeVThree, 0.4, 0.001, 10000);
		System.out.printf("Value iteration + greedy-q policy took: %f seconds for 3v3\n",(System.currentTimeMillis() - start)/1000f);
		
		//*/
		
		System.out.println("");
		
		//*
		System.out.println("Running 3v3 factored policy on 3v3");
		current = zdg.getInitState(threeVThree);
		cumulativeReward = 0;
		tf = zdg.getTerminalFunction(threeVThree);
		rf = zdg.getRewardFunction(threeVThree);
		while(!tf.isTerminal(current)){
			GroundedAction ga = fs3.getAction(current);
			State next = ga.executeIn(current);
			cumulativeReward += rf.reward(current, ga, next);
			
			System.out.print("Stats:\n");
			for(ObjectInstance o : current.getAllObjects() ) 
				System.out.printf("%s - %s\t;\t",o.getName(),o.getDiscValForAttribute("health"));
			System.out.print("\n");
			for(ObjectInstance o : next.getAllObjects() ) 
				System.out.printf("%s - %s\t;\t",o.getName(),o.getDiscValForAttribute("health"));
			System.out.print("\n");
			System.out.println(cumulativeReward + "\n");
			
			current = next;
		}
		
		System.out.println("\n\n\n");
		
		//*
		Policy fs23 = zdg.getPolicyFromFactoredSolution(threeVThree,cbvf2);
		System.out.println("Running 2v2 factored policy on 3v3");
		current = zdg.getInitState(threeVThree);
		cumulativeReward = 0;
		while(!tf.isTerminal(current)){
			GroundedAction ga = fs23.getAction(current);
			State next = ga.executeIn(current);
			cumulativeReward += rf.reward(current, ga, next);
			
			System.out.print("Stats:\n");
			for(ObjectInstance o : current.getAllObjects() ) 
				System.out.printf("%s - %s\t;\t",o.getName(),o.getDiscValForAttribute("health"));
			System.out.print("\n");
			for(ObjectInstance o : next.getAllObjects() ) 
				System.out.printf("%s - %s\t;\t",o.getName(),o.getDiscValForAttribute("health"));
			System.out.print("\n");
			System.out.println(cumulativeReward + "\n");
			
			current = next;
		}
		
		System.out.println("\n\n\n");
		/*
		Policy fs13 = zdg.getPolicyFromFactoredSolution(threeVThree,cbvf1);
		System.out.println("Running 1v1 factored policy on 3v3");
		current = zdg.getInitState(threeVThree);
		cumulativeReward = 0;
		while(!tf.isTerminal(current)){
			GroundedAction ga = fs13.getAction(current);
			State next = ga.executeIn(current);
			cumulativeReward += rf.reward(current, ga, next);
			
			System.out.print("Stats:\n");
			for(ObjectInstance o : current.getAllObjects() ) 
				System.out.printf("%s - %s\t;\t",o.getName(),o.getDiscValForAttribute("health"));
			System.out.print("\n");
			for(ObjectInstance o : next.getAllObjects() ) 
				System.out.printf("%s - %s\t;\t",o.getName(),o.getDiscValForAttribute("health"));
			System.out.print("\n");
			System.out.println(cumulativeReward + "\n");
			
			current = next;
		}
		
		System.out.println("\n\n\n");
		//*/
		System.out.println("Running 3v3 greedy-q policy on 3v3");
		current = zdg.getInitState(threeVThree);
		cumulativeReward = 0;
		while(!tf.isTerminal(current)){
			GroundedAction ga = gq.getAction(current);
			State next = ga.executeIn(current);
			cumulativeReward += rf.reward(current, ga, next);
			
			System.out.print("Stats:\n");
			for(ObjectInstance o : current.getAllObjects() ) 
				System.out.printf("%s - %s\t;\t",o.getName(),o.getDiscValForAttribute("health"));
			System.out.print("\n");
			for(ObjectInstance o : next.getAllObjects() ) 
				System.out.printf("%s - %s\t;\t",o.getName(),o.getDiscValForAttribute("health"));
			System.out.print("\n");
			System.out.println(cumulativeReward + "\n");
			
			current = next;
		}
		//*/

		System.out.println("\n\n----------------------------------\n\n");
		System.out.println("Can't get any 'solutions here', but let's try our factored policies!");
		zdg = new ZealotsDomainGenerator(4,0,4);
		Domain fourVFour = zdg.generateDomain();
		System.out.println("");
		
		//*
		Policy fs34 = zdg.getPolicyFromFactoredSolution(fourVFour,cbvf3);
		System.out.println("Running 3v3 factored policy on 4v4");
		current = zdg.getInitState(fourVFour);
		cumulativeReward = 0;
		tf = zdg.getTerminalFunction(fourVFour);
		rf = zdg.getRewardFunction(fourVFour);
		while(!tf.isTerminal(current)){
			GroundedAction ga = fs34.getAction(current);
			State next = ga.executeIn(current);
			cumulativeReward += rf.reward(current, ga, next);
			
			System.out.print("Stats:\n");
			for(ObjectInstance o : current.getAllObjects() ) 
				System.out.printf("%s - %s\t;\t",o.getName(),o.getDiscValForAttribute("health"));
			System.out.print("\n");
			for(ObjectInstance o : next.getAllObjects() ) 
				System.out.printf("%s - %s\t;\t",o.getName(),o.getDiscValForAttribute("health"));
			System.out.print("\n");
			System.out.println(cumulativeReward + "\n");
			
			current = next;
		}
		
		System.out.println("\n\n\n");
		

		System.out.println("Running 2v2 factored policy on 4v4");
		Policy fs24 = zdg.getPolicyFromFactoredSolution(fourVFour,cbvf2);
		current = zdg.getInitState(fourVFour);
		cumulativeReward = 0;
		while(!tf.isTerminal(current)){
			GroundedAction ga = fs24.getAction(current);
			State next = ga.executeIn(current);
			cumulativeReward += rf.reward(current, ga, next);
			
			System.out.print("Stats:\n");
			for(ObjectInstance o : current.getAllObjects() ) 
				System.out.printf("%s - %s\t;\t",o.getName(),o.getDiscValForAttribute("health"));
			System.out.print("\n");
			for(ObjectInstance o : next.getAllObjects() ) 
				System.out.printf("%s - %s\t;\t",o.getName(),o.getDiscValForAttribute("health"));
			System.out.print("\n");
			System.out.println(cumulativeReward + "\n");
			
			current = next;
		}
		
		System.out.println("\n\n\n");
		

		System.out.println("Running 1v1 factored policy on 4v4");
		Policy fs14 = zdg.getPolicyFromFactoredSolution(fourVFour,cbvf1);
		current = zdg.getInitState(fourVFour);
		cumulativeReward = 0;
		while(!tf.isTerminal(current)){
			GroundedAction ga = fs14.getAction(current);
			State next = ga.executeIn(current);
			cumulativeReward += rf.reward(current, ga, next);
			
			System.out.print("Stats:\n");
			for(ObjectInstance o : current.getAllObjects() ) 
				System.out.printf("%s - %s\t;\t",o.getName(),o.getDiscValForAttribute("health"));
			System.out.print("\n");
			for(ObjectInstance o : next.getAllObjects() ) 
				System.out.printf("%s - %s\t;\t",o.getName(),o.getDiscValForAttribute("health"));
			System.out.print("\n");
			System.out.println(cumulativeReward + "\n");
			
			current = next;
		}
		
		System.out.println("\n\n\n");
		
		//*/
		
		//*
		// RUN THIS CODE FOR A GOOD TIME
		
		System.out.println("");
		
		zdg = new ZealotsDomainGenerator(4,0,4);
		/*Domain*/ fourVFour = zdg.generateDomain();
		start = System.currentTimeMillis();
		//zdg.getFactoredSolution(fourVFour, 0.95);
		//System.out.printf("Factored solution + factored policy took: %f seconds for 4v4\n",(System.currentTimeMillis() - start)/1000f);
		start = System.currentTimeMillis();
		gq = zdg.getPolicyByVI(fourVFour, 0.95, 0.01, 1000);
		System.out.printf("Value iteration + greedy-q policy took: %f seconds for 4v4\n",(System.currentTimeMillis() - start)/1000f);
		
		
		System.out.println("Running 4v4 greedy-q policy on 4v4");
		current = zdg.getInitState(fourVFour);
		cumulativeReward = 0;
		while(!tf.isTerminal(current)){
			GroundedAction ga = gq.getAction(current);
			State next = ga.executeIn(current);
			cumulativeReward += rf.reward(current, ga, next);
			
			System.out.print("Stats:\n");
			for(ObjectInstance o : current.getAllObjects() ) 
				System.out.printf("%s - %s\t;\t",o.getName(),o.getDiscValForAttribute("health"));
			System.out.print("\n");
			for(ObjectInstance o : next.getAllObjects() ) 
				System.out.printf("%s - %s\t;\t",o.getName(),o.getDiscValForAttribute("health"));
			System.out.print("\n");
			System.out.println(cumulativeReward + "\n");
			
			current = next;
		}
		
		System.out.println("\n\n\n");
		
		
		//*/
	}
}
