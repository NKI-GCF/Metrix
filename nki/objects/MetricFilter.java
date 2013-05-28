// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.objects;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import nki.constants.Constants;
import nki.objects.Threshold;

public class MetricFilter implements Serializable{
	public static final long serialVersionUID = 42L;
	private ArrayList<String> runIds = new ArrayList<String>();
	private HashMap<String, Threshold> thresHolds = new HashMap<String, Threshold>();

	
	public void appendRunId(String runId){
		runIds.add(runId);
	}

	public ArrayList<String> getRunIds(){
		return this.runIds;
	}

	public void setRunIds(ArrayList<String> runIds){
		this.runIds = runIds;
	}

	


}
