package burlap.oomdp.singleagent.classbased;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;


import java.util.SortedSet;

import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;

public class ClassBasedValueFunction {
	
	public class LinkedAttribute implements Comparable<LinkedAttribute>{
		public String sourceClass;
		public String sourceAttribute;
		public String targetAttribute;
		public String targetClass;
		public LinkedAttribute(String cName, String aName, String fcName, String faName){
			sourceClass = cName;
			sourceAttribute = aName;
			targetClass = fcName;
			targetAttribute = faName;
		}
		
		public int compareTo(LinkedAttribute o){
			int first = sourceClass.compareTo(o.sourceClass);
			int second = sourceAttribute.compareTo(o.sourceAttribute);
			int third = targetClass.compareTo(o.targetClass);
			int fourth = targetAttribute.compareTo(o.targetAttribute);
			return first != 0 ? first : (second != 0 ? second : (third != 0 ? third : (fourth)));
		}
	}

	SortedSet<String> attributes;

	SortedSet<LinkedAttribute> foreignAttributes;
	
	String className;
	
	Map<List<Integer>,Double> valueFunction;
	
	public ClassBasedValueFunction( String initClassname, SortedSet<String> initAttributes, SortedSet<LinkedAttribute> initLinkedAttributes){
		className = initClassname;
		attributes = initAttributes;
		foreignAttributes = initLinkedAttributes;
		}

	public double evaluate(String oname, State s)
	{
		ObjectInstance o = s.getObject(oname);
		List<Integer> localState = new ArrayList<Integer>();
		for(String attribute : attributes)
		{
			localState.add((int)(o.getValueForAttribute(attribute).getNumericRepresentation()));
		}
		for(LinkedAttribute la : foreignAttributes)
		{
			String targetName = o.getAllRelationalTargets(la.sourceAttribute).iterator().next();
			localState.add((int)(s.getObject(targetName).getValueForAttribute(la.targetAttribute).getNumericRepresentation()));
		}
		return valueFunction.get(localState);
	}
	
	public SortedSet<String> getAttributes(){
		return attributes;
	}
	
	public SortedSet<LinkedAttribute> getForeignAttributes(){
		return foreignAttributes;
	}
	
	public String getClassName(){
		return className;
	}
	
	public void setValueFunction(Map<List<Integer>,Double> initFunction){
		valueFunction = initFunction;
	}
	
	public void addAttribute(String attribute){
		attributes.add(attribute);
		}
	
	public void addAttributes(Set<String> initAttributes){
		attributes.addAll( initAttributes);
		}
	
			
	public void clearAttribute(String attribute){
		attributes.remove(attribute);
	}
	
	public void clearAttributes(Set<String> initAttributes){
		attributes.removeAll( initAttributes);
		}
	
	public void clearAttributes(){
		attributes.clear();
	}
	
	public void addForeignAttribute(LinkedAttribute attribute){
		foreignAttributes.add(attribute);
		}
	
	public void addForeignAttributes(Set<LinkedAttribute> initForeignAttributes){
		foreignAttributes.addAll( initForeignAttributes);
		}
	
			
	public void clearForeignAttribute(LinkedAttribute attribute){
		foreignAttributes.remove(attribute);
	}
	
	public void clearForeignAttributes(Set<LinkedAttribute> initForeignAttributes){
		foreignAttributes.removeAll( initForeignAttributes);
		}
	
	public void clearForeignAttributes(){
		foreignAttributes.clear();
	}
	
	
	
	
}
