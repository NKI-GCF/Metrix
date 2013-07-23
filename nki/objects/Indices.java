// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.objects;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import nki.objects.MutableLong;
import nki.constants.Constants;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

public class Indices implements Serializable{

	public static final long serialVersionUID = 42L;
	
	// Project - Sample - Index/Lane/Clusters
	private HashMap<String, HashMap<String, HashMap<String, Object>>> indices = new HashMap<String, HashMap<String, HashMap<String, Object>>>();
	private long totalClusters = 0;

	public void setIndex(String projName, String sampName, String idx, int numClusters, int laneNr, int readNr){
		HashMap<String, HashMap<String, Object>> project = indices.get(projName);
		HashMap<String, Object> sampleMap;

		if(project == null){
			project = new HashMap<String, HashMap<String, Object>>();
			sampleMap = setSample(readNr, laneNr, numClusters, idx);

			project.put(sampName, sampleMap);	
		}else{
			sampleMap = project.get(sampName);

			if(sampleMap == null){
				sampleMap = setSample(readNr, laneNr, numClusters, idx);
				project.put(sampName, sampleMap);
			}else{	// Update num clusters.
				if(sampleMap.get(Constants.SAMPLE_NUM_CLUSTERS) instanceof MutableLong){
					MutableLong ml = (MutableLong) sampleMap.get(Constants.SAMPLE_NUM_CLUSTERS);
					ml.add(Long.valueOf(numClusters));
					sampleMap.put(Constants.SAMPLE_NUM_CLUSTERS, ml);
				}
			}

			project.put(sampName, sampleMap);
		}

		indices.put(projName, project);
	}

	private HashMap<String, Object> setSample(int readNr, int laneNr, int numClusters, String idx){
			HashMap<String, Object> sampleMap = new HashMap<String, Object>();
			sampleMap.put(Constants.SAMPLE_READNUM, readNr);
			MutableLong clusters = new MutableLong();
			clusters.add(Long.valueOf(numClusters));
			sampleMap.put(Constants.SAMPLE_NUM_CLUSTERS, clusters);
			sampleMap.put(Constants.SAMPLE_LANE, laneNr);
			sampleMap.put(Constants.SAMPLE_INDEX, idx);

			addTotalClusters(Long.valueOf(clusters.get()));

			return sampleMap;
	}

	public long getTotalClusters(){
		return totalClusters;
	}

	public void addTotalClusters(long metric){
		this.totalClusters += metric;
	}

	@SuppressWarnings("unchecked")
	public Element toXML(Element sumXml, Document xmlDoc){
		Iterator pit = indices.entrySet().iterator();

		while(pit.hasNext()){
			Map.Entry projects = (Map.Entry) pit.next();
			String project = (String) projects.getKey();

			Element projEle = xmlDoc.createElement("Project");
			projEle.setAttribute("name", project);

			HashMap<String, HashMap<String, Object>> samples = (HashMap<String, HashMap<String, Object>>) projects.getValue();
			Iterator sit = samples.entrySet().iterator();

			while(sit.hasNext()){
				Map.Entry sample = (Map.Entry) sit.next();
				String sampleName = (String) sample.getKey();

				Element sampleEle = xmlDoc.createElement("Sample");
				sampleEle.setAttribute("name", sampleName);

				HashMap<String, Object> sampleProp = (HashMap<String, Object>) sample.getValue();
				Iterator spit = sampleProp.entrySet().iterator();

				while(spit.hasNext()){
					Map.Entry prop = (Map.Entry) spit.next();

					// Num Clusters
					if(prop instanceof MutableLong){
						MutableLong ml = (MutableLong) prop.getValue();
						sampleEle.setAttribute(prop.getKey().toString(), ml.toString());
					} // Other
					else{
						sampleEle.setAttribute(prop.getKey().toString(), prop.getValue().toString());
					}
				}
				projEle.appendChild(sampleEle);
			}
			sumXml.appendChild(projEle);
		}
		return sumXml;
	}

	@SuppressWarnings("unchecked")
	public String toTab(){
		String out = "";

		Iterator pit = indices.entrySet().iterator();

		while(pit.hasNext()){
			Map.Entry projects = (Map.Entry) pit.next();
			String project = (String) projects.getKey();

			HashMap<String, HashMap<String, Object>> samples = (HashMap<String, HashMap<String, Object>>) projects.getValue();
			Iterator sit = samples.entrySet().iterator();

			while(sit.hasNext()){
				Map.Entry sample = (Map.Entry) sit.next();
				String sampleName = (String) sample.getKey();

				out += project + "\t" + sampleName;

				HashMap<String, Object> sampleProp = (HashMap<String, Object>) sample.getValue();
				Iterator spit = sampleProp.entrySet().iterator();

				while(spit.hasNext()){
					Map.Entry prop = (Map.Entry) spit.next();
					
					if(prop instanceof MutableLong){
						MutableLong ml = (MutableLong) prop.getValue();
						out += "\t" + prop.getKey() + ":" + ml.toString();
					}else{
						out += "\t" + prop.getKey() + ":" + prop.getValue().toString();
					}
				}

				out += "\n";

			}

		}
		return out;
	}

}
