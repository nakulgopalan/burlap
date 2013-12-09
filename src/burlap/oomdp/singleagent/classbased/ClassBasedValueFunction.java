package burlap.oomdp.singleagent.classbased;

import java.util.Map;
import java.util.Set;


import burlap.oomdp.core.State;

public class ClassBasedValueFunction {

	protected Map<State, Double>  classValues;
	
	protected Set<String> attributes;
	
	protected String className;
	
	public ClassBasedValueFunction( String initClassname, Set<String> initAttributes, Map<State, Double> initValues){
		classValues = initValues;
		className = initClassname;
		attributes = initAttributes;
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
	
	public void addAttribute(State sname, Double svalue){
		classValues.put(sname, svalue);
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
	
	
	
	
	
}
