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
import nki.objects.Indices;

public class IndexMetrics {
	private String source = "";	
	LittleEndianInputStream leis = null;
	private int version = 0;
	private int recordLength = 0;	
	boolean fileMissing = false;
	int sleepTime = 3000;

	public IndexMetrics(String source, int state){
		
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

	public Indices digestData(){
		Indices indices = new Indices();

		// First catch version of metrics file.
		try{
			setVersion(leis.readByte());		// Set Version
		}catch(IOException Ex){
			System.out.println("Error in parsing version number and recordlength: " + Ex.toString());
		}
		
		boolean readBool = true;

		try{
			while(readBool){
				int laneNr = leis.readUnsignedShort();
				int tileNr = leis.readUnsignedShort();
				int readNr = leis.readUnsignedShort();

				int numBytesIdx = leis.readUnsignedShort();
				
				String indexSeq = leis.readUTF8String(numBytesIdx);

				int numClustersIdx = leis.readInt();
				int numBytesSample = leis.readUnsignedShort();

				String sampleSeq = leis.readUTF8String(numBytesSample);

				int numBytesProject = leis.readUnsignedShort();
				
				String projectSeq = leis.readUTF8String(numBytesProject);

				indices.setIndex(projectSeq, sampleSeq, indexSeq, numClustersIdx, laneNr, readNr);
//				System.out.println(laneNr + "\t" + tileNr + "\t" + readNr + "\t" + projectSeq + "\t" + sampleSeq + "\t" + indexSeq + "\n");
				
			}
		}catch(IOException ExMain){
			readBool = false;
		}

		return indices;
	}

}	
