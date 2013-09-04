// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.objects;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.text.*;

import nki.util.ArrayUtils;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

public class ErrorDist implements Serializable{

	public static final long serialVersionUID = 42L;
	
	// Num Errors - Error rate
	private HashMap<Integer, List<Float>> 					eScoreDistRun 	= new HashMap<Integer, List<Float>>();

	// Lane - Num Errors - Num Reads with Error
	private HashMap<Integer, HashMap<Integer, List<Float>>> 	eScoreDistLane 	= new HashMap<Integer, HashMap<Integer, List<Float>>>();

	// Cycle - Num Errors - Num Reads with Error
	private HashMap<Integer, HashMap<Integer, List<Float>>> 	eScoreDistCycle = new HashMap<Integer, HashMap<Integer, List<Float>>>();

//	public float getScore(int qScore){
//		return eScoreDistRun.get(qScore);
//	}

	public void setRunDistScore(int lane, float score){
		if(eScoreDistRun.containsKey(lane)){
			(eScoreDistRun.get(lane)).add(score);
		}else{
			List<Float> scores = new ArrayList<Float>();
			scores.add(score);
			eScoreDistRun.put(lane, scores);
		}
	}
	public void setRunDistScoreByLane(int lane, int numErrors, float score){	
		if(eScoreDistLane.containsKey(lane)){
			if((eScoreDistLane.get(lane)).containsKey(numErrors)){
				((eScoreDistLane.get(lane)).get(numErrors)).add(score);
			}
		}else{
			List<Float> l = new ArrayList<Float>();
			l.add(score);
			HashMap<Integer, List<Float>> cycleM = new HashMap<Integer, List<Float>>();
			cycleM.put(numErrors, l);
			eScoreDistLane.put(lane, cycleM);
		}
	}

	public void setRunDistScoreByCycle(int cycle, int numErrors, float score){
//	System.out.println("Cycle:" + cycle + "\tNum Errors: " + numErrors + "\tScore: " + score );
//	if(cycle <11){
//		System.out.println("Cycle:" + cycle + "\tNum Errors: " + numErrors + "\tScore: " + score );
//	}
		if(eScoreDistCycle.containsKey(cycle)){
//			System.out.println("Cycle Exists (" + cycle + ")");
		
		/*
			if((eScoreDistCycle.get(cycle)).containsKey(numErrors)){
				((eScoreDistCycle.get(cycle)).get(numErrors)).add(score);
			}else{
				HashMap<Integer, List<Float>> eMap = eScoreDistCycle.get(cycle);
				List<Float> l = new ArrayList<Float>();
				l.add(score);
				eMap.put(numErrors, l);
				eScoreDistCycle.put(cycle, eMap);
			}
		*/
			HashMap<Integer, List<Float>> eMap = eScoreDistCycle.get(cycle);
			
			List<Float> l;
			if((eScoreDistCycle.get(cycle)).containsKey(numErrors)){
				l = (eScoreDistCycle.get(cycle)).get(numErrors);
			}else{
				l = new ArrayList<Float>();
			}
			l.add(score);
//			System.out.println("Length of array: " + l.size());
			eMap.put(numErrors, l);
			eScoreDistCycle.put(cycle, eMap);
		}else{
//			System.out.println("New Cycle " + cycle); 
			List<Float> l = new ArrayList<Float>();
			l.add(score);
			HashMap<Integer, List<Float>> cycleM = new HashMap<Integer, List<Float>>();
			cycleM.put(numErrors, l);
			eScoreDistCycle.put(cycle, cycleM);
		}
	}

/*	public Element toXML(Element sumXml, Document xmlDoc){
		for(int scoreVal : qScoreDist.keySet()){
			Element scoreEle = xmlDoc.createElement("QScore");
			scoreEle.setAttribute("score", scoreVal+"");
			MutableLong metric = this.getScore(scoreVal);
			scoreEle.setAttribute("clusters", metric.get()+"");
			sumXml.appendChild(scoreEle);
		}

		return sumXml;
	}

	public void incrementCycleTrack(int lane, int cycle){
		
	}

	private Element createElement(Document doc, String name, String text){
		Element e = doc.createElement(name);
		if(text == null){
			text = "";
		}
		e.appendChild(doc.createTextNode(text));

		return e;
	}
*/

	public String toTab(String source){
		String out = "";
		DecimalFormat df = new DecimalFormat("0.00");
		// Avg error rate per lane
		if(source.equals("rate")){
			for(int lane : eScoreDistRun.keySet()){	
				out += lane +"\t" + df.format(ArrayUtils.mean(eScoreDistRun.get(lane))) + "\t(+/- "+ df.format(ArrayUtils.sd(eScoreDistRun.get(lane))) +")\n";
			}	
		}else if(source.equals("lane")){
			for(int lane : eScoreDistLane.keySet()){
				out += lane;
				HashMap<Integer, List<Float>> errorMap = new HashMap<Integer, List<Float>>();
				errorMap = eScoreDistLane.get(lane);
				for(int numErrors : errorMap.keySet()){
					out += "\t" + ArrayUtils.mean(errorMap.get(numErrors));
				}
				out += "\n";
			}
		}else if(source.equals("cycle")){
			for(int cycle : eScoreDistCycle.keySet()){
				out += cycle;
				HashMap<Integer, List<Float>> errorMap = new HashMap<Integer, List<Float>>();
				errorMap = eScoreDistCycle.get(cycle);
				for(int numErrors : errorMap.keySet()){
//					System.out.println("Num Errors: " + numErrors);
					out += "\t" + Math.floor(ArrayUtils.mean(errorMap.get(numErrors)));
				}
				out += "\n";
			}
		}
		return out;
	}
}
