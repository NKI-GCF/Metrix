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

public class QScoreDist implements Serializable{

	public static final long serialVersionUID = 42L;
	
	// QualityMap - Integer scores
	private HashMap<Integer, MutableLong> qScoreDist = new HashMap<Integer, MutableLong>();

	public MutableLong getScore(int qScore){
		return qScoreDist.get(qScore);
	}

	public void setScore(int qScore, long metric){
		MutableLong val = qScoreDist.get(qScore);
		if(val == null){
			val = new MutableLong();
			val.add(metric);
			qScoreDist.put(qScore, val);
		}else{
			qScoreDist.get(qScore).add(metric);
		}
	}

	public Element toXML(Element sumXml, Document xmlDoc){
/*		String out = "<qscores>\n";
		
//		List sortedKeys = new ArrayList<HashMap>(QScoreDist.keySet());
//		Collections.sort(sortedKeys);
		
		for(int score : qScoreDist.keySet()){
			MutableLong metric = this.getScore(score);
			out += "<entry>\n\t<quality>"+score +"</quality>\n\t<numClusters>" + metric + "</numClusters>\n";
		}

		out += "\n</qscores>\n";
		return out;

*/
//		Element sumXml = xmlDoc.createElement("Dist");
		Element distXml = xmlDoc.createElement("QScores");
		for(int scoreVal : qScoreDist.keySet()){
			Element score = createElement(xmlDoc, "QScore", scoreVal+"");
			MutableLong metric = this.getScore(scoreVal);
			Element clusters = createElement(xmlDoc, "Clusters", metric+"");
			distXml.appendChild(score);
			distXml.appendChild(clusters);
			sumXml.appendChild(distXml);
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

//		List sortedKeys = new ArrayList(QScoreDist.keySet());
//		Collections.sort(sortedKeys);
		
		for(int score : qScoreDist.keySet()){
			MutableLong metric = this.getScore(score);
			out += score +"\t" + metric + "\n";
		}

		return out;
	}

	public HashMap<Integer, MutableLong> toObj(){
		return qScoreDist;
	}

}
