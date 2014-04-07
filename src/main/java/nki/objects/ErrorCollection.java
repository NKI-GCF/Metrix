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
import nki.objects.ErrorDist;

public class ErrorCollection implements Serializable{
	public static final long serialVersionUID = 42L;
	public int version;
	public int recordLength;
	public String source;

	// Lane - Cycle - ErrorMap
    public HashMap<Integer, HashMap<Integer, ErrorMap>> eScores = new HashMap<Integer, HashMap<Integer, ErrorMap>>();
	private ErrorDist eDist = new ErrorDist();

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
                        tmpMap.keySet().removeAll(cycleMap.keySet());
                        cycleMap.putAll(content);
		                eScores.put(lanenr, cycleMap);
                }

        }

    public HashMap<Integer, ErrorMap> getLane(int lanenr){
		return eScores.get(lanenr);
	}

	public Iterator getEScoreIterator(){
		return eScores.entrySet().iterator();
    }

	@SuppressWarnings("unchecked")
	public ErrorDist getErrorDistribution(){ 
		Iterator eit = this.getEScoreIterator();
		int globalCnt = 0;

		while(eit.hasNext()){
				Map.Entry scorePairs = (Map.Entry) eit.next();
				int lane = (Integer) scorePairs.getKey();
				HashMap<Integer, ErrorMap> laneScores = (HashMap<Integer, ErrorMap>) scorePairs.getValue();
				
//				int nestedCnt = 0;
				for(Map.Entry<Integer, ErrorMap> entry : laneScores.entrySet()){
						int cycle = (Integer) entry.getKey();
						ErrorMap emap = (ErrorMap) entry.getValue();
						Iterator emapIt = emap.getErrorIterator();

						while(emapIt.hasNext()){
								Map.Entry emapPairs = (Map.Entry) emapIt.next();
								int tile = (Integer) emapPairs.getKey();
								HashMap<Integer, Float> emetricMap = (HashMap<Integer, Float>) emapPairs.getValue();
								
								for(Map.Entry<Integer, Float> emetric : emetricMap.entrySet()){
									int numErrors = (Integer) emetric.getKey();
									float numReads = (float) emetric.getValue();

									if(numErrors == -1){		// Error rate
										eDist.setRunDistScore(lane, numReads);
									}else{						// Number of reads with num errors
//										System.out.println("Num Errors and Score" + numErrors + "\tScore" + numReads);
										eDist.setRunDistScoreByCycle(cycle, numErrors, numReads);
										eDist.setRunDistScoreByLane(lane, numErrors, numReads);
									}
								}
						}
				}
		}

		return eDist;
	}

}
