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

public class ErrorCollection implements Serializable{
	public static final long serialVersionUID = 42L;
	public int version;
	public int recordLength;
	public String source;
		
        public HashMap<Integer, HashMap<Integer, ErrorMap>> eScores = new HashMap<Integer, HashMap<Integer, ErrorMap>>();
	
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

	public void setLane(HashMap<Integer, ErrorMap> content, int lanenr){

                HashMap<Integer, ErrorMap> cycleMap = eScores.get(lanenr);

                if(cycleMap == null){
                        eScores.put(lanenr, content);
                }else{  // Merge maps
                        HashMap<Integer, ErrorMap> tmpMap = new HashMap<Integer, ErrorMap>(content);
                        tmpMap.keySet().removeKeys(cycleMap.keySet());
                        cycleMap.putAll(content);
                }

                eScores.put(lanenr, cycleMap);
        }

        public HashMap<Integer, ErrorMap> getLane(int lanenr){
                return eScores.get(lanenr);
        }


}
