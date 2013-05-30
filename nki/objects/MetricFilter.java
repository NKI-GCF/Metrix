// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.objects;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Iterator;
import nki.constants.Constants;
import nki.objects.Threshold;

public class MetricFilter implements Serializable{
	public static final long serialVersionUID = 42L;
	private ArrayList<String> runIds = new ArrayList<String>();
	// Thresholds:
	// Key: Constant value of requested metric threshold type
	// Value: Threshold object setting boundaries.
	
	private HashMap<String, Threshold> thresholds = new HashMap<String, Threshold>();
	
	public void appendRunId(String runId){
		runIds.add(runId);
	}

	public ArrayList<String> getRunIds(){
		return this.runIds;
	}

	public void setRunIds(ArrayList<String> runIds){
		this.runIds = runIds;
	}

	public void setThreshold(String metricType, Threshold threshold){
		thresholds.put(metricType, threshold);
	}

	public Threshold getThreshold(String metricType){
		return thresholds.get(metricType);
	}

	public Iterator getThresholdIterator(){
		return thresholds.entrySet().iterator();
	}

	public boolean checkType(String metricType){
		boolean check = false;

		if(Arrays.asList(Constants.METRIC_TYPE_REQUEST).contains(metricType)){
			check = true;	
		}

		return check;
	}


}
