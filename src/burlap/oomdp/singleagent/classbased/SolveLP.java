package burlap.oomdp.singleagent.classbased;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TransitionProbability;
import lpsolve.*;

public class SolveLP {

	class StateConverter{
		
		State currentState;
		Map<String,Integer> classToInt;
		Map<Integer,String> intToClass;
		int nClasses;
		int[] nObjects;
		int[] nAttributes;
		List<Map<String,Integer>> objectsToInts;
		List<Map<Integer,String>> intsToObjects;
		List<Map<String,Integer>> attributesToInts;
		List<Map<Integer,String>> intsToAttributes;
		
		StateConverter(Domain domain, State burlapState)
		{
			currentState = burlapState; 
			classToInt = HashingClassesToInt.hashingClassesToInt(burlapState);
			intToClass = HashingClassesToInt.hashingIntToClasses(burlapState);
			nClasses = domain.getObjectClasses().size();
			
			nObjects = new int[nClasses];
			nAttributes = new int[nClasses];
			objectsToInts = new ArrayList<Map<String,Integer>>();
			intsToObjects = new ArrayList<Map<Integer,String>>();
			attributesToInts = new ArrayList<Map<String,Integer>>();
			intsToAttributes = new ArrayList<Map<Integer,String>>();
			
			for(int i = 0; i < nClasses; i++)
			{
				String cname = intToClass.get(i);
				objectsToInts.add(HashingClassesToInt.hashingObjectToInt(burlapState,cname));
				intsToObjects.add(HashingClassesToInt.hashingIntToObject(burlapState,cname));
				attributesToInts.add(HashingClassesToInt.hashingAttributeToInt(domain,cname));
				intsToAttributes.add(HashingClassesToInt.hashingIntToAttribute(domain,cname));
				nObjects[i] = objectsToInts.get(i).size();
				nAttributes[i] = attributesToInts.get(i).size();
			}
		}
		
		public State mineToBurlap(int[][][] state)
		{
			for(int c = 0; c < state.length; c++)
			{
				for(int o = 0; o < state[c].length; o++)
				{
					for(int a = 0; a < state[c][o].length; a++)
					{
						currentState.getObject(intsToObjects.get(c).get(o)).setValue(intsToAttributes.get(c).get(a), state[c][o][a]);
					}
				}
			}
			return currentState;
		}

		public int[][][] burlapToMine(State burlapState) {
			int[][][] state = new int[nClasses][][];
			
			for(int c = 0; c < nClasses; c++)
			{
				state[c] = new int[nObjects[c]][nAttributes[c]];
				for(int o = 0; o < nObjects[c]; o++)
				{
					for(int a = 0; a < nAttributes[c]; a++)
					{
						state[c][o][a] = (int )(burlapState.getObject(intsToObjects.get(c).get(o)).getValueForAttribute(intsToAttributes.get(c).get(a)).getNumericRepresentation());
					}
				}
			}
			
			return state;
		}
		
		
	}
	
	
	
	double discountFactor;
	public SolveLP(double gamma){
		discountFactor = gamma;
	}
	
public void solveLP(Domain domain, State burlapState, Map<String, ClassBasedValueFunction> cbvfs, RewardFunction rewardFunction) {
	try {
		
		int nClasses = domain.getObjectClasses().size();
		
		int[] possibleInputs = new int[nClasses];
		int nAttributes[] = new int[nClasses];
		int nAttValues[][] = new int[nClasses][];
		
		StateConverter stateConverter = new StateConverter(domain, burlapState);
		
		Map<String,Integer> classToInt = stateConverter.classToInt;
		Map<Integer,String> intToClass = stateConverter.intToClass;
		
		List<Map<String,Integer>> objectsToInts = stateConverter.objectsToInts;
		List<Map<Integer,String>> intsToObjects = stateConverter.intsToObjects;
		List<Map<String,Integer>> attributesToInts = stateConverter.attributesToInts;
		List<Map<Integer,String>> intsToAttributes = stateConverter.intsToAttributes;
		
		for(int i = 0; i < nClasses; i++)
		{
			nAttributes[i] = intsToAttributes.get(i).size();
			nAttValues[i] = new int[nAttributes[i]];
			for(int j = 0; j < nAttributes[i]; j++)
			{
				nAttValues[i][j] = domain.getObjectClass(intToClass.get(i)).attributeMap.get(intsToAttributes.get(i).get(j)).discValues.size();
			}
		}
		

		int[][][] vFDependencies = new int[nClasses][][]; //class, dependency #, {class, attribute}
		
		

		for(int i = 0; i < nClasses; i++)
		{
			ClassBasedValueFunction cbvf = cbvfs.get(intToClass.get(i));
			
			vFDependencies [i] = new int[cbvf.getAttributes().size()+cbvf.getForeignAttributes().size()][2];
			int j = 0; 
			for(String attribute : cbvf.getAttributes())
			{
				vFDependencies[i][j][0] = i;
				vFDependencies[i][j][1] = attributesToInts.get(i).get(attribute);
				j++;
			}
			for(ClassBasedValueFunction.LinkedAttribute la : cbvf.getForeignAttributes())
			{
				vFDependencies[i][j][0] = classToInt.get(la.targetClass);
				vFDependencies[i][j][1] = attributesToInts.get(classToInt.get(la.targetClass)).get(la.targetAttribute);
				j++;
			}
		}
		
		
		int[] nObjects = new int[nClasses];
		int[][][] objectDependencies = new int[nClasses][][];//class, instance, dependency #: instance
			
		for(int c = 0; c < nClasses; c++)
		{
			 List<ObjectInstance> objects = burlapState.getObjectsOfTrueClass(intToClass.get(c));
			 nObjects[c] = objects.size();
			 objectDependencies[c] = new int[nObjects[c]][vFDependencies[c].length];
			 ClassBasedValueFunction cbvf = cbvfs.get(intToClass.get(c));
			 for(int i = 0; i < nObjects[c]; i++)
			 {
				 ObjectInstance o = burlapState.getObject(intsToObjects.get(c).get(i)); 
				 int j = 0;
				 for(@SuppressWarnings("unused") String attribute : cbvf.getAttributes())
				 {
					 objectDependencies[c][i][j] = i;
					 j++;
				 }
				 for(ClassBasedValueFunction.LinkedAttribute la : cbvf.getForeignAttributes())
				 {
					 String targetObject = o.getAllRelationalTargets(la.sourceAttribute).iterator().next();
					 objectDependencies[c][i][j] = objectsToInts.get(vFDependencies[c][j][0]).get(targetObject);
					 j++;
				 }
				 
			 }	
		}
		
	
		
		int[][][] state = new int[nClasses][][];  //state[class][object][attribute] = attribute value

		int stateSpaceSize = 1;
		
		for (int i = 0; i<nClasses; i++)
		{
			state[i] = new int[nObjects[i]][nAttributes[i]];
			
			for(int j=0; j<nAttributes[i];j++)
			{
				for(int k = 0; k<nObjects[i]; k++)
				{
					stateSpaceSize *= nAttValues[i][j];
				}
			}
		}
			

		for (int i =0; i < nClasses; i++)
		{
			possibleInputs[i]  = 1;
			for(int j = 0; j < vFDependencies[i].length; j++)
			{
				possibleInputs[i] *= nAttValues[vFDependencies[i][j][0]][vFDependencies[i][j][1]];
			}
			
		}
		
		int nVariables = 0;
		for (int i = 0; i< nClasses; i++)
		{
			nVariables += possibleInputs[i];
		}
		
		//int[] variableClass = new int[nVariables]; //variableClass[i] is the class to whose value function variable i belongs 
		//int[][][] variableAssignment = new int[nVariables][][];
		
		
		int nActions = 0;
		int[] nGroundedActions = new int[domain.getActions().size()];
		CompositeAction[] cActions = new CompositeAction[nGroundedActions.length];
		GroundedAction[][] gActions = new GroundedAction[nGroundedActions.length][];
		for(int i = 0; i < nGroundedActions.length; i++)
		{
			cActions[i] = (CompositeAction) domain.getActions().get(i);
			List<GroundedAction> gas = cActions[i].getAllGroundings(burlapState);
			nGroundedActions[i] = gas.size();
			gActions[i] = new GroundedAction[gas.size()];
			for(int j = 0; j< gas.size(); j++)
			{
				gActions[i][j] = gas.get(j);
			}
			nActions += gas.size();
		}
		
		
	    LpSolve lp = LpSolve.makeLp(0, nVariables);
	    lp.resizeLp(stateSpaceSize*nActions+1, nVariables);  //rows, columns
	    
	    double[] objective = new double[nVariables+1];

	    
		List<Map<List<Integer>,Integer>> whichVariable = new ArrayList<Map<List<Integer>,Integer>>();
		
		int col = 1;
		for(int c = 0; c < nClasses; c++)
		{
			Map<List<Integer>,Integer> map = new HashMap<List<Integer>,Integer>();
			List<Integer> list = new ArrayList<Integer>();
			for (int i = 0; i < vFDependencies[c].length; i++)
			{
				list.add(0);
			}
			List<Integer> zeroList = list;
			do
			{
				List<Integer> newList = new ArrayList<Integer>();
				for(int i = 0; i< list.size(); i++)
				{
					newList.add(list.get(i));
				}
				list = newList;
				for(int i = 0; i < vFDependencies[c].length; i++) //increment input
				{
					if(list.get(i)+1 < nAttValues[vFDependencies[c][i][0]][vFDependencies[c][i][1]])
					{
						list.set(i, list.get(i)+1);
						break;
					} else
					{
						list.set(i,0);
					}
				}
				map.put(list,col);
				col++;
			}while(!list.equals(zeroList));
			whichVariable.add(map);
		}
			
		
		
	    int[] beginVariables = new int[nClasses];
		col = 1;
	    for(int i = 0; i < nClasses; i++) //outer loop over classes, inner loop over possible VF input
	    {
	    	int instances = state[i].length;
	    	double weight = 1.0/possibleInputs[i]*instances;
	    	beginVariables[i] = col;
	    	for(int j = 0; j < possibleInputs[i]; j++)
	    	{
	    		objective[col] = weight;
	    		//lp.setBounds(col, -1*lp.getInfinite(), lp.getInfinite());
	    		col++;
	    	}
	    }
	    
	    lp.setObjFn(objective);
	    	
	    lp.setAddRowmode(true);
	    				
	    
	    for(int action = 0; action < nGroundedActions.length; action++)
	    {
	    	for(int gAction = 0; gAction < nGroundedActions[action]; gAction++)
	    	{
	    		boolean incrementClass = true; //used in incrementing loop below
	    		do //loop over states
	    		{
	    			double[] constraint = new double[nVariables+1];
	    			double reward = 0;

	    			burlapState = stateConverter.mineToBurlap(state);
	    			
	    			List<TransitionProbability> tpList = cActions[action].model.transitionProbsFor(burlapState, cActions[action].ground(gActions[action][gAction].params));
	    			double transitionProbability[] = new double[tpList.size()]; 
	    			int transitionedState[][][][] = new int[tpList.size()][][][];
	    			
	    			for(int i = 0; i < tpList.size(); i++)
	    			{
	    				transitionProbability[i] = tpList.get(i).p;
	    				transitionedState[i] = stateConverter.burlapToMine(tpList.get(i).s);
	    			}

	    			for (int s = 0; s < transitionProbability.length; s++) //iterate over next state
	    			{

	    				for(int c = 0; c < nClasses; c++) //iterate over values
	    				{
	    					for(int o = 0; o < nObjects[c]; o++)
	    					{
	    						List<Integer> input = new ArrayList<Integer>();
	    						for(int i = 0; i < vFDependencies[c].length; i++)
	    						{
	    							input.add(transitionedState[s][vFDependencies[c][i][0]][objectDependencies[c][o][i]][vFDependencies[c][i][1]]);
	    						}
	    						int matchingVariable = whichVariable.get(c).get(input);
	    						constraint[matchingVariable] += transitionProbability[s]*discountFactor;

	    					}
	    				}

	    				double subReward = rewardFunction.reward(burlapState, gActions[action][gAction], tpList.get(s).s); //Query reward function
	    				reward += subReward*transitionProbability[s];
	    			}

	    			for(int c = 0; c < nClasses; c++) //iterate over current state
	    			{
	    				for(int o = 0; o < nObjects[c]; o++)
	    				{
	    					List<Integer> input = new ArrayList<Integer>();
	    					for(int i = 0; i < vFDependencies[c].length; i++)
	    					{
	    						input.add(state[vFDependencies[c][i][0]][objectDependencies[c][o][i]][vFDependencies[c][i][1]]);
	    					}
	    					int matchingVariable = whichVariable.get(c).get(input);
	    					constraint[matchingVariable] += -1;

	    				}
	    			}

	    			lp.addConstraint(constraint, LpSolve.LE, -1.*reward);

	    			for (int c = 0; c < nClasses; c++)
	    			{
	    				incrementClass = true;
	    				for (int o = 0; o < nObjects[c]; o++)
	    				{
	    					boolean incrementObject = true;
	    					for (int a = 0; a < nAttributes[c]; a++)
	    					{
	    						if (state[c][o][a]+1 < nAttValues[c][a])
	    						{
	    							state[c][o][a]++;
	    							incrementClass = false;
	    							incrementObject = false;
	    							break;
	    						}
	    						state[c][o][a] = 0;
	    					}
	    					if(!incrementObject)
	    					{
	    						break;
	    					}
	    				}
	    				if(!incrementClass)
	    				{
	    					break;
	    				}
	    			}


	    		}while (incrementClass == false);
	    	}
	    }
	    

	    lp.setAddRowmode(false);
	    lp.setMinim();
	    
	    lp.solve();
	    double[] weights = lp.getPtrVariables();
	    
	    
	    for(int c = 0; c < nClasses; c++)
	    {
	    	Map<List<Integer>,Double> valueFunction = new HashMap<List<Integer>,Double>();
	    	for(List<Integer> key : whichVariable.get(c).keySet())
	    	{
				List<Integer> newList = new ArrayList<Integer>();
				for(int i = 0; i< key.size(); i++)
				{
					newList.add(key.get(i));
				}
				
				valueFunction.put(newList, new Double(weights[whichVariable.get(c).get(key).intValue()-1])
				);
	    		cbvfs.get(intToClass.get(c)).setValueFunction(valueFunction);
	    	
	    	}
	    	
	    }
	    

    	System.out.println("Enemies Health:");
    	for(int i = 1; i < 6; i++)
	    {
    		System.out.println(i%5 + ": " + weights[i-1]);
	    }

    	System.out.println("Enemies Health, My Health:");
	    for(int i = 6; i < weights.length+1; i++)
	    {
	    	System.out.println(((i)/5-1)%5 + ", " + (i)%5 + ": " + weights[i-1]);
	    }
	    System.out.println();
	    System.out.println();
	    /*
	    double[] constraint = new double[31]; 
	    for(int i = 0; i < lp.getNrows()+1; i++)
	    {
	    	lp.getRow(i, constraint);
    		System.out.print(lp.getRh(i) + " > ");
	    	for(int j = 0; j < constraint.length; j++)
		    {
		    	System.out.print(constraint[j] + ", ");
		    }
		    System.out.println();
	    	
	    }
	    */
	    
	    
	    lp.deleteLp();
	}
    catch (LpSolveException e) {
       e.printStackTrace();
	}
}

}
