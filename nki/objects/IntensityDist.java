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
import nki.objects.MutableLong;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

public class IntensityDist implements Serializable{

	public static final long serialVersionUID = 42L;

	// QualityMap - Integer scores
	private HashMap<Integer, MutableLong> intensityDist = new HashMap<Integer, MutableLong>();
	private long totalClusters = 0;

	public MutableLong getScore(int qScore){
		return intensityDist.get(qScore);
	}

	public void setScore(int qScore, long metric){
		MutableLong val = intensityDist.get(qScore);
		if(val == null){
			val = new MutableLong();
			val.add(metric);
			intensityDist.put(qScore, val);
		}else{
			intensityDist.get(qScore).add(metric);
		}
		addTotalClusters(metric); // append to total
	}

	public Element toXML(Element sumXml, Document xmlDoc){
		for(int scoreVal : intensityDist.keySet()){
			Element scoreEle = xmlDoc.createElement("QScore");
			scoreEle.setAttribute("score", scoreVal+"");
			MutableLong metric = this.getScore(scoreVal);
			scoreEle.setAttribute("clusters", metric.get()+"");
			sumXml.appendChild(scoreEle);
		}

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
		String out = "";

		for(int score : intensityDist.keySet()){
			MutableLong metric = this.getScore(score);
			out += score +"\t" + metric.get() + "\n";
		}

		return out;
	}

	public HashMap<Integer, MutableLong> toObj(){
		return intensityDist;
	}

	public long getTotalClusters(){
		return totalClusters;
	}

	public void addTotalClusters(long metric){
		this.totalClusters += metric;
	}

}
