package burlap.oomdp.singleagent.classbased;

import java.util.Map;
import java.util.Set;


import java.util.SortedSet;

import burlap.oomdp.core.State;

public class ClassBasedValueFunction {
	
	public class LinkedAttribute implements Comparable<LinkedAttribute>{
		public String sourceClass;
		public String sourceAttribute;
		public String targetAttribute;
		public String targetClass;
		LinkedAttribute(String cName, String aName, String faName){
			sourceClass = cName;
			sourceAttribute = aName;
			targetAttribute = faName;
		}
		
		public int compareTo(LinkedAttribute o){
			int first = sourceClass.compareTo(o.sourceClass);
			int second = sourceAttribute.compareTo(o.sourceAttribute);
			int third = targetAttribute.compareTo(o.targetAttribute);
			int fourth = targetClass.compareTo(o.targetClass);
			return first != 0 ? first : (second != 0 ? second : (third != 0 ? third : (fourth)));
		}
	}

	Map<State, Double>  classValues;
	
	SortedSet<String> attributes;

	SortedSet<LinkedAttribute> foreignAttributes;
	
	String className;
	
	public ClassBasedValueFunction( String initClassname, SortedSet<String> initAttributes, SortedSet<LinkedAttribute> initLinkedAttributes, Map<State, Double> initValues){
		classValues = initValues;
		className = initClassname;
		attributes = initAttributes;
		foreignAttributes = initLinkedAttributes;
		}

	public Map<State, Double> getClassValues(){
		return classValues;
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
	
	
	public void setClassValue(State sname, Double svalue){
		classValues.put(sname, svalue);
		}
	
	
	public void setClassValues(Map<State, Double> initValues){
		classValues = initValues;
		}
			
	public void clearClassValues(State sname){
		classValues.remove(sname);
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
