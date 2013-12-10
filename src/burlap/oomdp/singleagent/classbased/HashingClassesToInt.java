package burlap.oomdp.singleagent.classbased;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import burlap.oomdp.core.Attribute;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
//import java.util.;
import burlap.oomdp.core.State;

public class HashingClassesToInt {

	public static Map<String,Integer> hashingClassesToInt(State s){
		List<String> a = new ArrayList<String>();
		a.addAll(s.getObjectClassesPresent());
		Collections.sort(a,String.CASE_INSENSITIVE_ORDER);
		Map<String, Integer> b = new HashMap<String, Integer>();
		int itr = 0;
		for(String str: a){
			b.put(str, itr);
			itr++;
		}
		return b;
	}


	public static Map<Integer,String> hashingIntToClasses(State s){
		List<String> a = new ArrayList<String>();
		a.addAll(s.getObjectClassesPresent());
		Collections.sort(a,String.CASE_INSENSITIVE_ORDER);
		Map<Integer,String> b = new HashMap<Integer,String>();
		int itr = 0;
		for(String str: a){
			b.put(itr, str);
			itr++;
		}
		return b;
	}

	public static Map<Integer,String> hashingIntToObject(State s, String cname){
		List<String> a = new ArrayList<String>();
		for(ObjectInstance o : s.getObjectsOfTrueClass(cname)){
			a.add(o.getName());
		}
		Collections.sort(a,String.CASE_INSENSITIVE_ORDER);
		Map<Integer,String> b = new HashMap<Integer,String>();
		int itr = 0;
		for(String str: a){
			b.put(itr, str);
			itr++;
		}
		return b;
	}
	
	public static Map<String,Integer> hashingObjectToInt(State s, String cname){
		List<String> a = new ArrayList<String>();
		for(ObjectInstance o : s.getObjectsOfTrueClass(cname)){
			a.add(o.getName());
		}
		Collections.sort(a,String.CASE_INSENSITIVE_ORDER);
		Map<String,Integer> b = new HashMap<String,Integer>();
		int itr = 0;
		for(String str: a){
			b.put(str,itr);
			itr++;
		}
		return b;
	}

	public static Map<String,Integer> hashingAttributeToInt(Domain d, String cname){
		List<String> string = new ArrayList<String>();
		for(Attribute a : d.getObjectClass(cname).attributeList){
			if(a.type == Attribute.AttributeType.DISC)
			{
				string.add(a.name);
			}
		}
		Collections.sort(string,String.CASE_INSENSITIVE_ORDER);
		Map<String,Integer> b = new HashMap<String,Integer>();
		int itr = 0;
		for(String str: string){
			b.put(str,itr);
			itr++;
		}
		return b;
	}
	
	public static Map<Integer,String> hashingIntToAttribute(Domain d, String cname){
		List<String> string = new ArrayList<String>();
		for(Attribute a : d.getObjectClass(cname).attributeList){
			if(a.type == Attribute.AttributeType.DISC)
			{
				string.add(a.name);
			}
		}
		Collections.sort(string,String.CASE_INSENSITIVE_ORDER);
		Map<Integer,String> b = new HashMap<Integer,String>();
		int itr = 0;
		for(String str: string){
			b.put(itr,str);
			itr++;
		}
		return b;
	}

}
