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
import nki.objects.MutableLong;
import nki.objects.IntensityMap;
import nki.objects.IntensityDist;
import nki.constants.Constants;

public class IntensityScores implements Serializable{

	public static final long serialVersionUID = 42L;
	public int version;
	public int recordLength;
	public String source;
	public HashMap<Integer, HashMap<Integer, IntensityMap>> iScores = new HashMap<Integer, HashMap<Integer, IntensityMap>>();	

	public void setVersion(int version){
		this.version = version;
	}

	public int getVersion(){
		return version;
	}

	public boolean isEmpty(){
		if(iScores.size() == 0){
			return true;
		}else{
			return false;
		}
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

	public void setLane(HashMap<Integer, IntensityMap> content, int lanenr){

		HashMap<Integer, IntensityMap> cycleMap = iScores.get(lanenr);

		if(cycleMap == null){
			iScores.put(lanenr, content);
		}else{	// Merge maps and replace existing entries
			HashMap<Integer, IntensityMap> tmpMap = new HashMap<Integer, IntensityMap>(content);
			tmpMap.keySet().removeAll(cycleMap.keySet());
			cycleMap.putAll(content);
			iScores.put(lanenr, cycleMap);
		}
	}

	public HashMap<Integer, IntensityMap> getLane(int lanenr){
		return iScores.get(lanenr);
    }

	public IntensityMap getCycle(int lane, int cycle){
		return (iScores.get(lane)).get(cycle);
	}

	public void setCycle(int lane, int cycle, IntensityMap map){
		iScores.get(lane).put(cycle, map);
	}

	public Iterator getIScoreIterator(){
		return iScores.entrySet().iterator();
    }


	/*
	 *	Lane -> Cycle -> Intensity Set
	 */
	@SuppressWarnings("unchecked")
	public IntensityDist getAvgCorIntDist(){
        Iterator iit = this.getIScoreIterator();
		IntensityDist iDistAvg = new IntensityDist();

		// Lane -> CycleMap
		while(iit.hasNext()){
				Map.Entry scorePairs = (Map.Entry) iit.next();
				int lane = (Integer) scorePairs.getKey();
				HashMap<Integer, IntensityMap> laneScores = (HashMap<Integer, IntensityMap>) scorePairs.getValue();
				// Cycle -> IntensityMap
				for(Map.Entry<Integer, IntensityMap> entry : laneScores.entrySet()){
						int cycle = (Integer) entry.getKey();
						IntensityMap qmap = (IntensityMap) entry.getValue();
						
						HashMap<String, MutableInt> iMapMAvg = qmap.getCycleAverageInt();
						iDistAvg.setIntensity(lane, cycle, (HashMap<String, MutableInt>) iMapMAvg);
				}
		}

		return iDistAvg;
	}

	@SuppressWarnings("unchecked")
	public IntensityDist getAvgCorIntCCDist(){
        Iterator iit = this.getIScoreIterator();
		IntensityDist iDistAvgCC = new IntensityDist();

		// Lane -> CycleMap
		while(iit.hasNext()){
				Map.Entry scorePairs = (Map.Entry) iit.next();
				int lane = (Integer) scorePairs.getKey();
				HashMap<Integer, IntensityMap> laneScores = (HashMap<Integer, IntensityMap>) scorePairs.getValue();

				// Cycle -> IntensityMap
				for(Map.Entry<Integer, IntensityMap> entry : laneScores.entrySet()){
						int cycle = (Integer) entry.getKey();
						IntensityMap qmap = (IntensityMap) entry.getValue();
						
						HashMap<String, MutableInt> iMapMAvgCC = qmap.getCycleAverageCCInt();
						iDistAvgCC.setIntensity(lane, cycle, (HashMap<String, MutableInt>) iMapMAvgCC);
				}
		}

		return iDistAvgCC;
	}
}
