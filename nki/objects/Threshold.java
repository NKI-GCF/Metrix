// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.objects;

import java.io.*;
import java.util.HashMap;
import nki.constants.Constants;

public class Threshold implements Serializable {

	public static final long serialVersionUID = 42L;

	private String	metricType;		// Constant value of requested metric threshold type
	private int 	min;			// Minimum value
	private int 	max;			// Maximum value
	private int		exact;			// Exact value

	public void setMin(int min){
		this.min = min;
	}

	public int getMin(){
		return min;
	}

	public void setMax(int max){
		this.max = max;
	}

	public int getMax(){
		return max;
	}

	public void setExact(int exact){
		this.exact = exact;
	}

	public int getExact(){
		return exact;
	}


}
