// Illumina Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.objects;

import java.net.*;
import java.io.*;
import java.lang.*;
import java.util.Date;
import java.util.HashMap;
import java.util.ArrayList;

public class Reads implements Serializable{
	
	public static final long serialVersionUID = 42L;

	private HashMap<Integer, ArrayList<String>> readMap = new HashMap<Integer, ArrayList<String>>();	

	public void insertMapping(int readNum, String readDesc, String readVal){
		ArrayList<String> subMap;
		if(readMap.containsKey(readNum)){
			// Get subMap from hashmap.
			subMap = readMap.get(readNum);
		}else{
                        // Create new readnum entry and popup late with new hashmap
			subMap = new ArrayList<String>();
		}
		subMap.add(readDesc);
		subMap.add(readVal);
		readMap.put(readNum, subMap);
	}

	public int getNumberOfReads(){
		return readMap.size();
	}
	
	public int getPairedTurnCycle(){
		int readOneLength = Integer.parseInt((readMap.get(1)).get(0));
		int readTwoLength = Integer.parseInt((readMap.get(2)).get(0));
		return (readOneLength + readTwoLength);
	}

}
