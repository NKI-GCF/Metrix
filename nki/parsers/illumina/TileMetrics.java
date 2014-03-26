// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.parsers.illumina;

import java.io.IOException;
import java.io.EOFException;
import java.util.ArrayList;
import java.util.*;
import java.util.logging.Level;
import nki.objects.ClusterDensity;
import nki.objects.PhasingCollection;
import nki.objects.Reads;
import nki.util.LoggerWrapper;

public class TileMetrics extends GenericIlluminaParser {
	private final static int 		CLUSTER_DENSITY 	= 100;
	private final static int 		CLUSTER_DENSITY_PF 	= 101;

	// Instantiate Logger	
	private static final LoggerWrapper metrixLogger = LoggerWrapper.getInstance();

	// Lane --> ClusterDensities
	private ClusterDensity cdMap = new ClusterDensity();
	// Lane --> ClusterDensityPassingFilter
	private ClusterDensity cdPFMap = new ClusterDensity();
	// LANE --> READ --> PhasingScores
	private PhasingCollection pMap = new PhasingCollection();
	// LANE --> READ --> PrePhasingScores
	private PhasingCollection preMap = new PhasingCollection();

	ArrayList<Integer> cycles = new ArrayList<>();

	public TileMetrics(String source, int state){
		super(TileMetrics.class, source, state);
	}

	public ClusterDensity getCDmap(){
		return cdMap;
	}

	public void setCDmap(ClusterDensity cdMap){
		this.cdMap = cdMap;
	}

	public ClusterDensity getCDpfMap(){
		return cdPFMap;
	}

	public void setCDpfMap(ClusterDensity cdPFMap){
		this.cdPFMap = cdPFMap;
	}

	public PhasingCollection getPhasingMap(){
		return pMap;
	}

	public void setPhasingMap(PhasingCollection pMap){
		this.pMap = pMap;
	}

	public PhasingCollection getPrephasingMap(){
		return preMap;
	}

	public void setPrephasingMap(PhasingCollection preMap){
		this.preMap = preMap;
	}

	/*
	 *	Binary structure:
	 *	byte 0: file version number (2)
	 *	byte 1: length of each record
	 *	bytes (N * 10 + 2) - (N *10 + 11): record:
	 *	2 bytes: lane number (uint16)
	 *	2 bytes: tile number (uint16)
	 *	2 bytes: metric code (uint16)
	 *	4 bytes: metric value (float)
		Where N is the record index and possible metric codes are:
		code 100: cluster density (k/mm2)
		code 101: cluster density passing filters (k/mm2)
		code 102: number of clusters
		code 103: number of clusters passing filters
		code (200 + (N  1) * 2): phasing for read N
		code (201 + (N  1) * 2): prephasing for read N
		code (300 + N  1): percent aligned for read N
		code 400: control lane
	 */

	public void digestData(Reads rds){
		try{
			setVersion(leis.readByte());
			setRecordLength(leis.readByte());
		}catch(IOException Ex){
			metrixLogger.log.log(Level.SEVERE, "Error in parsing version number and recordLength: {0}", Ex.toString());
		}

		boolean eofCheck = true;

		try{
			int record = 1;
			int laneCheck = 0;
			while(eofCheck){
				int laneNr = leis.readUnsignedShort();
				int tileNr = leis.readUnsignedShort();
                int metricCode = leis.readUnsignedShort();
				float metricValue = leis.readFloat();
	
				// Cluster Density Parsing
				if(metricCode == CLUSTER_DENSITY){
					cdMap.setMetric(laneNr, metricValue);
				}else if(metricCode == CLUSTER_DENSITY_PF){
					cdPFMap.setMetric(laneNr, metricValue);
				}
				
				//
				// Possible catch number of clusters here (code 102 && 103)
				//

				List<Integer> codeMap = digits(metricCode);

				// Next in loop because were not looking at phasing or prephasing.
				if( metricCode == (102 |103)){
					continue;
				}
	
				// Calc Phasing code / Prephasing code	
				int PHAPRE = (metricCode - 200) % 2; 
				// Calc readnumber from metriccode
				int readNum = (int) Math.floor((metricCode - 200) / 2) + 1;
			
		//		if(rds.isIndexedRead(readNum)){
		//			continue;
		//		}

		//		if(metricValue == 0){
		//			continue;
		//		}

				// Phasing / Pre-phasing Parsing
				if(codeMap.get(2) == 2){	// Metric code starts with 2
					// Phasing
					if(PHAPRE == 0){
						pMap.setPhasing(laneNr, readNum, metricValue);
					}

					// Prephasing				
					if(PHAPRE == 1){
						preMap.setPhasing(laneNr, readNum, metricValue);
					}	
				}else{	// Skip the other codes
					continue;
				}
				record++;
			}
		
		}catch(EOFException EOFEx){
			eofCheck = false;
		}catch(IOException Ex){
			eofCheck = false;
		}

			// Set the collection objects.
			cdMap.setType("CD");
			setCDmap(cdMap);
			cdPFMap.setType("PF");
			setCDpfMap(cdPFMap);
			pMap.setType("PH");
			setPhasingMap(pMap);
			preMap.setType("PREPH");
			setPrephasingMap(preMap);
	}

	private String parseMetricCode(int code){
		String metricValue = "";
		if(code == CLUSTER_DENSITY){
			metricValue = "Cluster Density k/mm2 : ";
		}else if(code == CLUSTER_DENSITY_PF){
			metricValue = "Cluster Density passing filters k/mm2: ";
		}else if(code == 102){
			metricValue = "Number of Clusters: ";
		}else if(code == 103){
			metricValue = "Number of clusters passing filters: ";
		}else if(code == 200){
			metricValue= "Phasing for read N (Record Index): ";
		}else if(code == 201){
			metricValue = "Prephasing for read N (Record Index): ";
		}else if(code == 300){
			metricValue = "Percent aligned for read N: ";
		}else if(code == 400){
			metricValue = "Control Lane: ";
		}else{
			metricValue = "N/A ERROR.";
		}

		return metricValue;

	}
	
	private List<Integer> digits(int i) {
	    List<Integer> digits = new ArrayList<>();
	    while(i > 0) {
	        digits.add(i % 10);
	        i /= 10;
	    }
	    return digits;
	}

}
