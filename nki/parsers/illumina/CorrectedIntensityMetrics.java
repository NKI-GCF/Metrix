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
import nki.objects.IntensityScores;
import nki.objects.IntensityMap;
import nki.constants.Constants;

public class CorrectedIntensityMetrics {
	private String source = "";
	LittleEndianInputStream leis = null;

	IntensityScores iScores;

	private int version = 0;
	private int recordLength = 0;
    private int sleepTime = 3000;
	private boolean fileMissing = false;	

	public CorrectedIntensityMetrics(String source, int state){

		try{
			setSource(source);
			if(state == 1){
				Thread.sleep(sleepTime);
			}
			leis = new LittleEndianInputStream(new FileInputStream(source));
		}catch(IOException IO){
			// Set fileMissing = true. --> Parse again later.
			setFileMissing(true);
			System.out.println("Corrected Intensity Metrics file not available for " + source);
		}catch(InterruptedException IEX){

		}
	}

	public void setSource(String source){
		this.source = source;
	}

	public String getSource(){
		return source;
	}

	public void setFileMissing(boolean fileMissing){
		this.fileMissing = fileMissing;
	}

	public boolean getFileMissing(){
		return fileMissing;
	}

	public IntensityScores digestData(){
        iScores = new IntensityScores();

		try{
			iScores.setVersion(leis.readByte());
			iScores.setRecordLength(leis.readByte());
			iScores.setSource(this.getSource());
		}catch(IOException Ex){
			System.out.println("Error in parsing version number and recordLength: " + Ex.toString());
		}

		try{
			HashMap<Integer, IntensityMap> cycleMap = new HashMap<Integer, IntensityMap>();
			IntensityMap iMap = new IntensityMap();
			int procLane = 0;
			int cnt = 0;

			while(true){
				int laneNr = leis.readUnsignedShort();
				int tileNr = leis.readUnsignedShort();
				int cycleNr = leis.readUnsignedShort();
		
			//	if(iScores.getLane(laneNr) == null){
			//		cycleMap = new HashMap<Integer, IntensityMap>();
			//	}

				if(cycleMap.containsKey(cycleNr)){
					iMap = cycleMap.get(cycleNr);
				}else{
					iMap = new IntensityMap();
				}

				// Avg Corrected Int
				iMap.addMapping(tileNr, Constants.METRIC_VAR_ACI, leis.readUnsignedShort());

			//-- Avg Corrected Int A
				iMap.addMapping(tileNr, Constants.METRIC_VAR_ACI_A, leis.readUnsignedShort());

				// Avg Corrected Int C
				iMap.addMapping(tileNr, Constants.METRIC_VAR_ACI_C, leis.readUnsignedShort());

				// Avg Corrected Int G
				iMap.addMapping(tileNr, Constants.METRIC_VAR_ACI_G, leis.readUnsignedShort());

				// Avg Corrected Int T
				iMap.addMapping(tileNr, Constants.METRIC_VAR_ACI_T, leis.readUnsignedShort());

			//-- Avg Corrected Int Called Clusters A
				iMap.addMapping(tileNr, Constants.METRIC_VAR_ACICC_A, leis.readUnsignedShort());

				// Avg Corrected Int Called Clusters C
				iMap.addMapping(tileNr, Constants.METRIC_VAR_ACICC_C, leis.readUnsignedShort());

				// Avg Corrected Int Called Clusters G
				iMap.addMapping(tileNr, Constants.METRIC_VAR_ACICC_G, leis.readUnsignedShort());

				// Avg Corrected Int Called Clusters T
				iMap.addMapping(tileNr, Constants.METRIC_VAR_ACICC_T, leis.readUnsignedShort());

			//-- Num of base calls for No Call (Float)
				iMap.addMapping(tileNr, Constants.METRIC_VAR_NUM_BCS_NC, leis.readFloat());

				// Num of base calls for A (Float)
				iMap.addMapping(tileNr, Constants.METRIC_VAR_NUM_BCS_A, leis.readFloat());

				// Num of base calls for C (Float)
				iMap.addMapping(tileNr, Constants.METRIC_VAR_NUM_BCS_C, leis.readFloat());

				// Num of base calls for G (Float)
				iMap.addMapping(tileNr, Constants.METRIC_VAR_NUM_BCS_G, leis.readFloat());
				
				// Num of base calls for T (Float)
				iMap.addMapping(tileNr, Constants.METRIC_VAR_NUM_BCS_T, leis.readFloat());

				// Signal to noise ratio
				iMap.addMapping(tileNr, Constants.METRIC_VAR_NUM_SIGNOISE, leis.readFloat());

				cycleMap.put(cycleNr, iMap);				
				if(procLane != laneNr){
					iScores.setLane(cycleMap, laneNr);
					procLane = laneNr;
					if(iScores.getLane(laneNr) != null){
						cycleMap = iScores.getLane(laneNr);
					}else{
						cycleMap = new HashMap<Integer, IntensityMap>();
					}
				}
			}
		}catch(EOFException EOFEx){
			// Reached end of file
			// Lazy EOF - Ignore checking.
		}catch(IOException Ex){
			System.out.println("IO Error - CorrectedIntensityMetrics digest");
		}
	
		// Return the qualityScores object.
		return iScores;
	}
}
