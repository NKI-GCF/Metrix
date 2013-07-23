// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.objects;

import java.io.*;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import nki.objects.IntensityMap;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

public class IntensityDist implements Serializable{

	public static final long serialVersionUID = 42L;

	private HashMap<Integer, HashMap<Integer, HashMap<String, MutableInt>>> iDist = new HashMap<Integer, HashMap<Integer, HashMap<String, MutableInt>>>();

	public void setIntensity(int lane, int cycle, HashMap<String, MutableInt> iMapM){
		HashMap<Integer, HashMap<String, MutableInt>> oMap = iDist.get(lane);

//		System.out.println("Setting Int for: " + lane + "\tCycle: " + cycle + "Avg: " + iMapp.getCycleAverageInt());

		if(oMap == null){
			HashMap <Integer, HashMap<String, MutableInt>> cMap = new HashMap<Integer, HashMap<String, MutableInt>>();
			cMap.put(cycle, iMapM);
			iDist.put(lane, cMap);
		}else{
			if(oMap.get(cycle) == null){
				oMap.put(cycle, iMapM);
			}

			iDist.put(lane, oMap);
		}
	}

	@SuppressWarnings("unchecked")
	public Element toXML(Element sumXml, Document xmlDoc){
		Iterator lit = iDist.entrySet().iterator();
		/*
		 * Key   = Lane			- Integer
		 * Value = CycleMap 	- HashMap<Integer, HashMap<String, Object>>
		 */
		while(lit.hasNext()){
			Element laneEle = xmlDoc.createElement("Lane");
			Map.Entry lanePairs = (Map.Entry) lit.next();
			int lane = (Integer) lanePairs.getKey();
			laneEle.setAttribute("lane", Integer.toString(lane));

			HashMap<Integer, HashMap<String, MutableInt>> cycleContent = (HashMap<Integer, HashMap<String, MutableInt>>) lanePairs.getValue();
			// Cycle Iterator
			Iterator cit = (Iterator) cycleContent.entrySet().iterator();

			while(cit.hasNext()){
				Element cycleEle = xmlDoc.createElement("Cycle");
				Map.Entry cycleEntries = (Map.Entry) cit.next();
				int cycle = (Integer) cycleEntries.getKey();
				cycleEle.setAttribute("num", Integer.toString(cycle));
			
				// Nested Intensities HashMap
				HashMap<String, MutableInt> cycleInt = (HashMap<String, MutableInt>) cycleEntries.getValue();

				Iterator iit = (Iterator) cycleInt.entrySet().iterator();
	
				Element intEle = xmlDoc.createElement("Intensities");
				while(iit.hasNext()){
					Map.Entry intensityPairs = (Map.Entry) iit.next();
					String constName = (String) intensityPairs.getKey();
					MutableInt intValue = (MutableInt) intensityPairs.getValue();

					if(intValue instanceof MutableInt){
						MutableInt in = (MutableInt) intValue;
						intEle.setAttribute(constName, Integer.toString(in.get()));
					}

					cycleEle.appendChild(intEle);
				}
				laneEle.appendChild(cycleEle);
			}
			sumXml.appendChild(laneEle);
		}

		return sumXml;
	}

	@SuppressWarnings("unchecked")
	public String toTab(){
		String out = "";

		Iterator lit = iDist.entrySet().iterator();
		/*
		 * Key   = Lane			- Integer
		 * Value = CycleMap 	- HashMap<Integer, HashMap<String, Object>>
		 */
		while(lit.hasNext()){
			Map.Entry lanePairs = (Map.Entry) lit.next();
			int lane = (Integer) lanePairs.getKey();

			HashMap<Integer, HashMap<String, MutableInt>> cycleContent = (HashMap<Integer, HashMap<String, MutableInt>>) lanePairs.getValue();
			// Cycle Iterator
			Iterator cit = (Iterator) cycleContent.entrySet().iterator();

			while(cit.hasNext()){
				Map.Entry cycleEntries = (Map.Entry) cit.next();
				int cycle = (Integer) cycleEntries.getKey();
			
				// Nested Intensities HashMap
				HashMap<String, MutableInt> cycleInt = (HashMap<String, MutableInt>) cycleEntries.getValue();

				Iterator iit = (Iterator) cycleInt.entrySet().iterator();
				out += lane + "\t" + cycle;
				
				while(iit.hasNext()){
					Map.Entry intensityPairs = (Map.Entry) iit.next();
					String constName = (String) intensityPairs.getKey();
					MutableInt intValue = (MutableInt) intensityPairs.getValue();

					out += "\t" +constName + ":" + intValue;
				
				}

				out += "\n";
			}
		}
		return out;
	}

/*	public HashMap<Integer, MutableLong> toObj(){
		return iDist;
	}
*/
}
