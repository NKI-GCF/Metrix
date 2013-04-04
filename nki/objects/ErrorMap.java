// Illumina Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.objects;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;

public class ErrorMap implements Serializable{
	public static final long serialVersionUID = 42L;

	private HashMap<Integer, HashMap<Integer, Integer>> eMap = new HashMap<Integer, HashMap<Integer, Integer>>();
	
	public void addMetric(int tilenr, int numE, int eVal){
                HashMap<Integer, Integer> eSubMap;
                if(eMap.containsKey(tilenr)){
                        // Get subMap from hashmap.
                        eSubMap = sMap.get(tilenr);
                }else{
                        // Create new readnum entry and popup late with new hashmap
                        eSubMap = new HashMap<Integer, Integer>();
                }

		eSubMap.put(numE, eVal);
		eMap.put(tilenr, eSubMap);
	}

	public Integer getNumberOfTiles(){
		return eMap.size();
	}

	public Iterator getErrorIteratorForTiles(){
		return eMap.entrySet().iterator();
	}
	
	
}
