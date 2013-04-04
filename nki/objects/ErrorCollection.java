// Illumina Metrix - A server / client interface for Illumina Sequencing Metrics.
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
//	public HashMap<Integer, ErrorMap> 
	
}
