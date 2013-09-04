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

public class ExtractionMetrics extends GenericIlluminaParser{
	ArrayList<Integer> cycles = new ArrayList<Integer>();

	public ExtractionMetrics(String source, int state){
		super(ExtractionMetrics.class, source, state);
	}

	/*
	 * Binary structure:
	 *	byte 0: file version number (2)
	 *	byte 1: length of each record
	 *	bytes (N * 38 + 2) - (N *38 + 39): record:
	 *	2 bytes: lane number (uint16)
	 *	2 bytes: tile number (uint16)
	 *	2 bytes: cycle number (uint16)
	 *	4 x 4 bytes: fwhm scores (float) for channel [A, C, G, T] respectively
	 *	2 x 4 bytes: intensities (uint16) for channel [A, C, G, T] respectively
	 *	8 bytes: date/time of CIF creation
	 *
	 */

	public void digestData(){
		try{
			setVersion(leis.readByte());	
			setRecordLength(leis.readByte()); 
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
