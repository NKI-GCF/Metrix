// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.parsers.illumina;

import nki.io.LittleEndianInputStream; 
import java.io.*;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Collections;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class ExtractionMetrics {
	private String source = "";	
	LittleEndianInputStream leis = null;
	private int version = 0;
	private int recordLength = 0;	
	ArrayList<Integer> cycles = new ArrayList<Integer>();
	boolean fileMissing = false;
	int sleepTime = 3000;

	public ExtractionMetrics(String source, int state){
		
		String tmpPath = ""; 

		try{
			setSource(source);
			if(state == 1){
				Thread.sleep(sleepTime);
			}
			leis = new LittleEndianInputStream(new FileInputStream(source));	
		}catch(IOException IO){
			// Set fileMissing = true. --> Parse again later. 
			setFileMissing(true);
		}catch(InterruptedException IEX){

        }
	}


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

	public void setFileMissing(boolean set){
		this.fileMissing = set;
	}
	
	public boolean getFileMissing(){
		return fileMissing;
	}

	public void setSleepTime(int st){
		this.sleepTime = st;
	}

	public int getSleepTime(){
		return sleepTime;
	}

	public void outputData(){
			// First catch version and record length of metrics file.
		try{
			setVersion(leis.readByte());		// Set Version
			setRecordLength(leis.readByte()); 	// Set Record Length
		}catch(IOException Ex){
			System.out.println("Error in parsing version number and recordlength: " + Ex.toString());
		}

		try{
			while(true){
				int laneNr = leis.readUnsignedShort();
				int tileNr = leis.readUnsignedShort();
				int cycleNr = leis.readUnsignedShort();
				
				float fA = leis.readFloat();
				float fC = leis.readFloat();
				float fG = leis.readFloat();
				float fT = leis.readFloat();
				
				int iA = leis.readUnsignedShort();
				int iC = leis.readUnsignedShort();				
				int iG = leis.readUnsignedShort();				
				int iT = leis.readUnsignedShort();				
				
				long dateTime = leis.readLong();
			}
		}catch(IOException ExMain){
			System.out.println("Error in main parsing of metrics data: " + ExMain.toString());
		}
	
		
	}

	public List<Integer> getUniqueCycles(){

		try{
			leis.skipBytes(6);
			
			while(true){
				int cycleNr = leis.readUnsignedShort();
				cycles.add(cycleNr);
				leis.skipBytes(36);			
			}
		}catch(IOException Ex){
			System.out.println("IOException in Unique Cycles " + Ex.toString());
		}		
		
		List<Integer> newList = new ArrayList<Integer>(new HashSet<Integer>(cycles));
		Collections.sort(newList);

		return newList;
		
	}

	public int getLastCycle(){
		try{
			if(leis != null){
				leis.skipBytes(6);
			
				while(true){
					int cycleNr = leis.readUnsignedShort();
					cycles.add(cycleNr);
					leis.skipBytes(36);
				}
			}
			if(leis != null){
				leis.close();
			}
		}catch(IOException Ex){
		
		}

		int max = 0;

		for(int c : cycles){
			if(c > max){ max = c; }
		}
		return max;
	}
		
		

}
