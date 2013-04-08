// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.objects;

import java.io.*;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import nki.objects.QualityMap;
import nki.objects.MutableLong;

public class QualityScores implements Serializable{

	public static final long serialVersionUID = 42L;
	public int version;
	public int recordLength;
	public String source;
	public HashMap<Integer, HashMap<Integer, QualityMap>> qScores = new HashMap<Integer, HashMap<Integer, QualityMap>>();	
        private HashMap<Integer, MutableLong> qScoreDist = new HashMap<Integer, MutableLong>();

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

		HashMap<Integer, QualityMap> cycleMap = qScores.get(lanenr);

		if(cycleMap == null){
			qScores.put(lanenr, content);
		}else{	// Merge maps
			HashMap<Integer, QualityMap> tmpMap = new HashMap<Integer, QualityMap>(content);
			tmpMap.keySet().removeAll(cycleMap.keySet());
			cycleMap.putAll(content);
		}

		qScores.put(lanenr, cycleMap);
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

	public HashMap<Integer, MutableLong> getQScoreDistribution(){
                Iterator qit = this.getQScoreIterator();
	
                while(qit.hasNext()){
                        Map.Entry scorePairs = (Map.Entry) qit.next();
                        int lane = (Integer) scorePairs.getKey();
                        HashMap<Integer, QualityMap> laneScores = (HashMap<Integer, QualityMap>) scorePairs.getValue();

                        for(Map.Entry<Integer, QualityMap> entry : laneScores.entrySet()){
                                int cycle = (Integer) entry.getKey();
                                QualityMap qmap = (QualityMap) entry.getValue();

                                Iterator qmapIt = qmap.getScoreIterator();

                                while(qmapIt.hasNext()){
                                        Map.Entry qmapPairs = (Map.Entry) qmapIt.next();
                                        int tile = (Integer) qmapPairs.getKey();
                                        HashMap<Integer, Integer> qmetricMap = (HashMap<Integer, Integer>) qmapPairs.getValue();

                                        for(Map.Entry<Integer, Integer> qmetric : qmetricMap.entrySet()){
						int qScore = (Integer) qmetric.getKey();
						long metric = Long.valueOf(qmetric.getValue());
	
						if(metric == 0){
							continue;
						}					
	
						MutableLong val = qScoreDist.get(qScore);
						if(val == null){
							val.add(metric);
							qScoreDist.put(qScore, val);
						}else{
							qScoreDist.get(qScore).add(metric);
						}
	
       //System.out.println("Lane: " + lane + "\tCycle: " + cycle + "\tTile: " + tile + "\tQMetric: " + metric + "\t#Clust\\wScore: " + value);
                                        }
                                }
                        }
                }
		
	return qScoreDist;


        }
}
