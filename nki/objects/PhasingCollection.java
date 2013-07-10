// Metrix - A server / client interface for Illumina Sequencing Phasings.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.objects;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import nki.objects.Phasing;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

public class PhasingCollection implements Serializable{

	public static final long serialVersionUID = 42L;
	private int tiles = 0;

	// Lane -> Read Num (1, 2, 3 or 4) -> Phasing Map for read.
	public HashMap<Integer, HashMap<Integer, Phasing>> phasingPerLane = new HashMap<Integer, HashMap<Integer, Phasing>>();

	public String type = "";

	public void setPhasing(int lane, int readNum, Float phasingScore){
		HashMap<Integer, Phasing> lanePhaseMap;

		Phasing m;
		lanePhaseMap = phasingPerLane.get(lane);

		if(lanePhaseMap != null){
			m = lanePhaseMap.get(readNum);
			if(m == null){
				m = new Phasing();
			}
			m.incrementPhasing(phasingScore);
		}else{
			lanePhaseMap = new HashMap<Integer, Phasing>();
			m = new Phasing();
//			m.setPhasing(phasingScore);
			m.incrementPhasing(phasingScore);
		}

		lanePhaseMap.put(readNum, m);
		phasingPerLane.put(lane, lanePhaseMap);

	}

	public Phasing getPhasing(int lane, int readNum){
		return this.phasingPerLane.get(lane).get(readNum);
	}

	public Iterator getLanePhasingIterator(){
		return phasingPerLane.entrySet().iterator();
	}

	public Element toXML(Element sumXml, Document xmlDoc){
		Element distXml;
		if(type.equals("PH")){
			distXml = xmlDoc.createElement("Phasing");
		}else{// or PREPH
			distXml = xmlDoc.createElement("Prephasing");
		}

		for(int lane : phasingPerLane.keySet()){	// Lane
			Element laneEle = xmlDoc.createElement("Lane");
			laneEle.setAttribute("Index", lane+"");

			for(int readNum : phasingPerLane.get(lane).keySet()){
				Element readEle = xmlDoc.createElement("Read");
				readEle.setAttribute("Read", readNum+"");
				readEle.setAttribute("PhasingScore", getPhasing(lane, readNum).getPhasing().toString());
				laneEle.appendChild(readEle);
			}
			distXml.appendChild(laneEle);
		}

		sumXml.appendChild(distXml);
		return sumXml;
	}

	private Element createElement(Document doc, String name, String text){
		Element e = doc.createElement(name);
		if(text == null){
			text = "";
		}
		e.appendChild(doc.createTextNode(text));

		return e;
	}

	public String toTab(){
		String out = "Lane\tRead\tPhasing-Score\n";

		for(int lane : phasingPerLane.keySet()){	// Lane
			for(int readNum : phasingPerLane.get(lane).keySet()){
				out += lane + "\t" + readNum + "\t" + getPhasing(lane, readNum).getPhasing().toString() + "\n";
			}
		}

		return out;
	}

	public HashMap<Integer, HashMap<Integer, Phasing>> toObj(){
		return phasingPerLane;
	}
	
	public void setType(String type){
		this.type = type;
	}

	public String getType(){
		return type;
	}


}
