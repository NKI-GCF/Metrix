// Illumina Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.objects;

import java.io.*;

public class MutableInt implements Serializable{
	public static final long serialVersionUID = 42L;
	public int val = 0;

	public MutableInt(){
	}

	public void add(Integer val){
		this.val += val;
	}

	public void increment(){
		++val;
	}
	
	public int get() {
		return val;
	}

	public void avg(int numTiles){
		this.val = val / numTiles;
	}

	public String toString(){
		return val + "";
	}

}
