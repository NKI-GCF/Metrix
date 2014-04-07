// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.objects;

import java.io.*;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import nki.constants.Constants;
import nki.objects.MutableInt;

public class IntensityMap implements Serializable{

	public static final long serialVersionUID = 42L;
	// Multiple types in hashmap
	// HashMap<String, Object>
		// 			 - float
		// 			 - int
	
	private HashMap<Integer, HashMap<String,  Object>> sMap  = new HashMap<Integer, HashMap<String, Object>>();
	
	public void addMapping(int tilenr, String metricType, Object metricVal){ 		
				
                HashMap<String, Object> iMap;
                if(sMap.containsKey(tilenr)){
                        // Get subMap from hashmap.
                        iMap = sMap.get(tilenr);
                }else{
                        // Create new readnum entry and populate with new hashmap
                        iMap = new HashMap<String, Object>();
                }

		iMap.put(metricType, metricVal);
		sMap.put(tilenr, iMap);
	}

	public Integer getNumberOfTiles(){
		return sMap.size();
	}

	public Iterator getScoreIterator(){
		return sMap.entrySet().iterator();
	}

	@SuppressWarnings("unchecked")
		// Calculate the average intensity for this cycle for each metric constant [A, C, G, T]
	public HashMap<String, MutableInt> getCycleAverageInt(){
		Iterator sMi = this.getScoreIterator();
		HashMap<String, MutableInt> avgOverTiles = new HashMap<String, MutableInt>();

		avgOverTiles.put(Constants.METRIC_VAR_ACI_A, new MutableInt());
		avgOverTiles.put(Constants.METRIC_VAR_ACI_C, new MutableInt());
		avgOverTiles.put(Constants.METRIC_VAR_ACI_G, new MutableInt());
		avgOverTiles.put(Constants.METRIC_VAR_ACI_T, new MutableInt());

		while(sMi.hasNext()){
			Map.Entry tileEntry = (Map.Entry) sMi.next();

			int tile = (Integer) tileEntry.getKey();
			HashMap<String, Object> tileMapping = (HashMap<String, Object>) tileEntry.getValue();
			
			Integer iA = (Integer) tileMapping.get(Constants.METRIC_VAR_ACI_A);
			Integer iC = (Integer) tileMapping.get(Constants.METRIC_VAR_ACI_C);
			Integer iG = (Integer) tileMapping.get(Constants.METRIC_VAR_ACI_G);
			Integer iT = (Integer) tileMapping.get(Constants.METRIC_VAR_ACI_T);

			avgOverTiles.get(Constants.METRIC_VAR_ACI_A).add(iA);
			avgOverTiles.get(Constants.METRIC_VAR_ACI_C).add(iC);
			avgOverTiles.get(Constants.METRIC_VAR_ACI_G).add(iG);
			avgOverTiles.get(Constants.METRIC_VAR_ACI_T).add(iT);
		}

		avgOverTiles.get(Constants.METRIC_VAR_ACI_A).avg();
		avgOverTiles.get(Constants.METRIC_VAR_ACI_C).avg();
		avgOverTiles.get(Constants.METRIC_VAR_ACI_G).avg();
		avgOverTiles.get(Constants.METRIC_VAR_ACI_T).avg();

		return avgOverTiles;
	}

	@SuppressWarnings("unchecked")
		// Calculate the average intensity of called clusters for this cycle for each metric constant [A, C, G, T]
	public HashMap<String, MutableInt> getCycleAverageCCInt(){
		Iterator sMi = this.getScoreIterator();
		HashMap<String, MutableInt> avgOverTiles = new HashMap<String, MutableInt>();

		avgOverTiles.put(Constants.METRIC_VAR_ACICC_A, new MutableInt());
		avgOverTiles.put(Constants.METRIC_VAR_ACICC_C, new MutableInt());
		avgOverTiles.put(Constants.METRIC_VAR_ACICC_G, new MutableInt());
		avgOverTiles.put(Constants.METRIC_VAR_ACICC_T, new MutableInt());

		while(sMi.hasNext()){
			Map.Entry tileEntry = (Map.Entry) sMi.next();

			int tile = (Integer) tileEntry.getKey();
			HashMap<String, Object> tileMapping = (HashMap<String, Object>) tileEntry.getValue();
			
			Integer iA = (Integer) tileMapping.get(Constants.METRIC_VAR_ACICC_A);
			Integer iC = (Integer) tileMapping.get(Constants.METRIC_VAR_ACICC_C);
			Integer iG = (Integer) tileMapping.get(Constants.METRIC_VAR_ACICC_G);
			Integer iT = (Integer) tileMapping.get(Constants.METRIC_VAR_ACICC_T);

			avgOverTiles.get(Constants.METRIC_VAR_ACICC_A).add(iA);
			avgOverTiles.get(Constants.METRIC_VAR_ACICC_C).add(iC);
			avgOverTiles.get(Constants.METRIC_VAR_ACICC_G).add(iG);
			avgOverTiles.get(Constants.METRIC_VAR_ACICC_T).add(iT);
		}

		avgOverTiles.get(Constants.METRIC_VAR_ACICC_A).avg();
		avgOverTiles.get(Constants.METRIC_VAR_ACICC_C).avg();
		avgOverTiles.get(Constants.METRIC_VAR_ACICC_G).avg();
		avgOverTiles.get(Constants.METRIC_VAR_ACICC_T).avg();

		return avgOverTiles;
	}
	
		// Return the number of called bases foreach channel [NC, A, C, G, T]
	public void getNumberCalledBases(){
	//public HashMap<String, float> getNumberCalledBases(){
		
	}

		// Calculate average Signal To Noise Ratio for this cycle
//	public float getAverageSTNRatio(){
	public void getAverageSTNRatio(){
		
	}
}
