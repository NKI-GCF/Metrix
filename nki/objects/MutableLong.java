// Illumina Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.objects;

import java.io.*;

public class MutableLong implements Serializable{
	public static final long serialVersionUID = 42L;
	public long val = 0;

	public MutableLong(){
	}

	public void increment(){
		++val;
	}
	
	public long get() {
		return val;
	}

	public void add(long addVal){
		val += addVal;
	}

}
