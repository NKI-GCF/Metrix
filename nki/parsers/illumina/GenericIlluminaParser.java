// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.parsers.illumina;

import nki.io.LittleEndianInputStream;
import java.io.IOException;
import java.io.EOFException;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Collections;

public class GenericIlluminaParser {
	private String source = "";
	LittleEndianInputStream leis = null;

	private int version = 0;
	private int recordLength = 0;
    private int sleepTime = 3000;
	private boolean fileMissing = false;	

	public void setSource(String source){
		this.source = source;
	}

	public String getSource(){
		return source;
	}

	private void setVersion(int version){
		this.version = version;
	}

	public int getVersion(){
		return version;
	}

	private void setRecordLength(int recordLength){
		this.recordLength = recordLength;
	}

	public int getRecordLength(){
		return recordLength;
	}

	public void setFileMissing(boolean fileMissing){
		this.fileMissing = fileMissing;
	}

	public boolean getFileMissing(){
		return fileMissing;
	}
	/*
	* Override function
	*/
	public void digestData(){
		try{
			setVersion(leis.readByte());
			setRecordLength(leis.readByte());
		}catch(IOException Ex){
			System.out.println("Error in parsing version number and recordLength: " + Ex.toString());
		}

		try{
			// Parse structure here -- Override function
		}catch(EOFException EOFEx){
			// Reached end of file
		}catch(IOException Ex){
			System.out.println("IO Error");
		}
	
	}
}
