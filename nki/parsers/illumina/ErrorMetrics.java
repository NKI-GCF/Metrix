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
import java.util.HashSet;
import java.util.HashMap;
import java.util.Collections;
import nki.objects.ErrorCollection;
import nki.objects.ErrorMap;
import nki.util.LoggerWrapper;

public class ErrorMetrics extends GenericIlluminaParser{
	private ErrorCollection eScores;

	// Instantiate Logger	
	LoggerWrapper metrixLogger = LoggerWrapper.getInstance();

	public ErrorMetrics(String source, int state){
		super(ErrorMetrics.class, source, state);
	}

	/*
	 * Binary structure:
	 * 	byte 0: file version number (3)
	 *	byte 1: length of each record (uint8)
		bytes (N * 30 + 2) - (N *30 + 11): record: 
	 *	2 bytes: lane number (uint16)
	 *	2 bytes: tile number (uint16)
	 *	2 bytes: cycle number (uint16)
	 *	4 bytes: error rate (float) 
	 *	4 bytes: number of perfect reads (uint32)
	 *	4 bytes: number of reads with 1 error (uint32)
	 *	4 bytes: number of reads with 2 errors (uint32)
	 *	4 bytes: number of reads with 3 errors (uint32)
	 *	4 bytes: number of reads with 4 errors (uint32)
		Where N is the record index
	 */
	public ErrorCollection digestData(){
		eScores = new ErrorCollection();
		HashMap<Integer, ErrorMap> cycleMap;		

		try{
			eScores.setVersion(leis.readByte());
			eScores.setRecordLength(leis.readByte());		
		}catch(IOException Ex){
			metrixLogger.log.severe("Error in parsing version number and recordLength: " + Ex.toString());
		}

		try{
			int record = 1;
			ErrorMap eMap = new ErrorMap();

			while(true){
				int laneNr = leis.readUnsignedShort();
				int tileNr = leis.readUnsignedShort();
				int cycleNr = leis.readUnsignedShort();

				// If errormap exists in cycle map retrieve. 
				// Check cycle after parsing.
				if(eScores.getLane(laneNr) != null){
					cycleMap = eScores.getLane(laneNr);
				}else{
					cycleMap = new HashMap<Integer, ErrorMap>();
				}

				if(cycleMap.containsKey(cycleNr)){
					eMap = cycleMap.get(cycleNr);
				}else{
					eMap = new ErrorMap();
				}

				float errorRate = leis.readFloat();
				float numPerfectReads = (float) leis.readInt();
				float numReads1E = (float) leis.readInt();
				float numReads2E = (float) leis.readInt();
				float numReads3E = (float) leis.readInt();
				float numReads4E = (float) leis.readInt();
			
				eMap.addMetric(tileNr,-1, errorRate);
				eMap.addMetric(tileNr,0, numPerfectReads);
				eMap.addMetric(tileNr,1, numReads1E);
				eMap.addMetric(tileNr,2, numReads2E);
				eMap.addMetric(tileNr,3, numReads3E);
				eMap.addMetric(tileNr,4, numReads4E);
	
				cycleMap.put(cycleNr, eMap);
				eScores.setLane(cycleMap, laneNr);
	//			System.out.println(laneNr + "\t" + cycleNr + "\t" + tileNr + "\t" + errorRate + "\t" + numPerfectReads + "\t" + numReads1E + "\t" + numReads2E + "\t" + numReads3E + "\t" + numReads4E);

			}
		}catch(EOFException EOFEx){
			// Reached end of file
		}catch(IOException Ex){
			metrixLogger.log.severe("IO Error in parsing the Error Metrics file.");
		}
		return eScores;
	}
}
