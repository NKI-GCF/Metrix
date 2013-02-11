// Illumina Metrix - A server / client interface for Illumina Sequencing Metrics.
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
import java.util.Collections;

public class QualityMetrics {
	private String source = "";
	LittleEndianInputStream leis = null;

	private int version = 0;
	private int recordLength = 0;

	public QualityMetrics(String source){
		try{
			setSource(source);
			leis = new LittleEndianInputStream(new FileInputStream(source));
		}catch(IOException IO){
			System.out.println("Parser Error - Quality Metrics: " + IO.toString());
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

	public void outputData(){
		try{
			setVersion(leis.readByte());
			setRecordLength(leis.readByte());
		}catch(IOException Ex){
			System.out.println("Error in parsing version number and recordLength: " + Ex.toString());
		}

		try{
			HashMap<Integer,int[]> qcMap = new HashMap<Integer, int[]>();
			boolean qcFlag = false;

			while(true){
				int laneNr = leis.readUnsignedShort();
				int tileNr = leis.readUnsignedShort();
				int cycleNr = leis.readUnsignedShort();
				
				qcFlag=true;
				int qcRecord = 1;
				int[] clusterScore = new int[51];
				System.out.println(laneNr + "\t" + cycleNr + "\t" + tileNr);

				while(qcFlag){
					if(qcRecord == 50){
						qcFlag = false;
					}

					clusterScore[qcRecord] = leis.readInt();
					System.out.println("Record: " + qcRecord + "\tAssigned: " +  clusterScore[qcRecord]);
					qcRecord++;
				}

				qcMap.put(cycleNr, clusterScore); // Store in hashmap per cycle.
			}
		}catch(EOFException EOFEx){
			System.out.println("Reached end of file");
		}catch(IOException Ex){
			System.out.println("IO Error");
		}

	}
}
