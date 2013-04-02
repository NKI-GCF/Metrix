// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.objects;

import java.io.*;
import java.util.Iterator;
import java.util.HashMap;
import nki.objects.QualityMap;

public class QualityScores implements Serializable{

	public static final long serialVersionUID = 42L;
	public int version;
	public int recordLength;
	public String source;
	public HashMap<Integer, HashMap<Integer, QualityMap>> qScores = new HashMap<Integer, HashMap<Integer, QualityMap>>();	

	public void setVersion(int version){
		this.version = version;
	}

	public int getVersion(){
		return version;
	}
	
	public void setRecordLength(int recordLength){
		this.recordLength = recordLength;
	}

	public int getRecordLength(){
		return recordLength;
	}

	public void setSource(String source){
		this.source = source;
	}

	public String getSource(){
		return source;
	}

	public void setLane(HashMap<Integer, QualityMap> content, int lanenr){
		qScores.put(lanenr, content);
	}

        public HashMap<Integer, QualityMap> getLane(int lanenr){
                return qScores.get(lanenr);
        }

	public QualityMap getCycle(int lane, int cycle){
		return (qScores.get(lane)).get(cycle);
	}

	public void setCycle(int lane, int cycle, QualityMap map){
		qScores.get(lane).put(cycle, map);
	}

	public Iterator getQScoreIterator(){
		return qScores.entrySet().iterator();
        }
}
