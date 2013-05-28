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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Iterator;
import java.util.Collections;
import java.util.*;
import nki.objects.ClusterDensity;
import nki.objects.Phasing;

public class TileMetrics {
	private String source = "";
	LittleEndianInputStream leis = null;
	private int version = 0;
	private int recordLength = 0;
	private float clusterDensity = 0;
	private float clusterDensityPF = 0;
	private int cdTileCount = 0;
	private int cdPFTileCount = 0;
	private final static int 		CLUSTER_DENSITY 	= 100;
	private final static int 		CLUSTER_DENSITY_PF 	= 101;
	private boolean fileMissing = false;

	// Lane --> ClusterDensities
	private Map<Object, ClusterDensity> cdMap = new HashMap<Object, ClusterDensity>();
	// Lane --> ClusterPassingFilterDensities
	private Map<Object, ClusterDensity> cdPFMap = new HashMap<Object, ClusterDensity>();
	// LANE --> READ --> PhasingScores
	private Map<Integer, Map<Integer, Phasing>> pMap = new HashMap<Integer, Map<Integer, Phasing>>();
	// LANE --> READ --> PrePhasingScores
	private Map<Integer, Map<Integer, Phasing>> preMap = new HashMap<Integer, Map<Integer, Phasing>>();

	ArrayList<Integer> cycles = new ArrayList<Integer>();

	public TileMetrics(String source, int state){
		try{
			setSource(source);
			if(state == 1){
				Thread.sleep(3000);
			}
			leis = new LittleEndianInputStream(new FileInputStream(source));
		}catch(IOException IO){
			setFileMissing(true);
		}catch(InterruptedException IEX){

		}
	}

	public void setFileMissing(boolean set){
		this.fileMissing = set;
	}

	public boolean getFileMissing(){
		return fileMissing;
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

	public Map<Object, ClusterDensity> getCDmap(){
		return cdMap;
	}

	public void setCDmap(Map<Object, ClusterDensity> cdMap){
		this.cdMap = cdMap;
	}

	public Map<Object, ClusterDensity> getCDpfMap(){
		return cdPFMap;
	}

	public void setCDpfMap(Map<Object, ClusterDensity> cdPFMap){
		this.cdPFMap = cdPFMap;
	}

	public Map<Integer, Map<Integer, Phasing>> getPhasingMap(){
		return pMap;
	}

	public void setPhasingMap(Map<Integer, Map<Integer, Phasing>> pMap){
		this.pMap = pMap;
	}

	public Map<Integer, Map<Integer, Phasing>> getPrephasingMap(){
		return preMap;
	}

	public void setPrephasingMap(Map<Integer, Map<Integer, Phasing>> preMap){
		this.preMap = preMap;
	}

	public void digestData(){
		try{
			setVersion(leis.readByte());
			setRecordLength(leis.readByte());
		}catch(IOException Ex){
			System.out.println("Error in parsing version number and recordLength: " + Ex.toString());
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
				ClusterDensity val = new ClusterDensity();
				if(metricCode == CLUSTER_DENSITY){
					if(cdMap.containsKey(laneNr)){
						val = cdMap.get(laneNr);
					}
					val.incrementMetric(metricValue);
					val.incrementTiles();
					
					cdMap.put(laneNr, val);
					
				}else if(metricCode == CLUSTER_DENSITY_PF){
					if(cdPFMap.containsKey(laneNr)){
						val = cdPFMap.get(laneNr);
					}
					val.incrementMetric(metricValue);
					val.incrementTiles();
					
					cdPFMap.put(laneNr, val);
				}
				//
				// Possible catch number of clusters here (code 102 && 103)
				//

				List<Integer> codeMap = digits(metricCode);

				// Next in loop because were not looking at phasing or prephasing.
				if(codeMap.get(2) != 2){
					continue;
				}				

				// Phasing / Pre-phasing Parsing
				Phasing p = new Phasing();
		
				// Calc different metric codes here and call respective method.
	
				// Calc Phasing code / Prephasing code	
				int PHAPRE = (metricCode - 200) % 2; 
				// Calc readnumber from metriccode
				int readNum = (int) Math.floor((metricCode - 200) / 2) + 1;
		
				// Phasing
				if(PHAPRE == 0){
					Map<Integer, Phasing> phaseMap = new HashMap<Integer, Phasing>();
					if(pMap.containsKey(laneNr)){
						phaseMap = pMap.get(laneNr);
					}

					p.incrementMetric(metricValue);
					p.incrementTiles();

					phaseMap.put(readNum,p);
					pMap.put(laneNr, phaseMap);
				}

				// Prephasing				
				if(PHAPRE == 1){ 
					Map<Integer, Phasing> prephaseMap = new HashMap<Integer, Phasing>();
					if(preMap.containsKey(laneNr)){
						prephaseMap = preMap.get(laneNr);
					}
					
					p.incrementMetric(metricValue);
					p.incrementTiles();

					prephaseMap.put(readNum,p);
					preMap.put(laneNr, prephaseMap);
				}	
				
				record++;
			}
			
			// Set the cikkectuib obhects.
			setCDmap(cdMap);
			setCDpfMap(cdPFMap);
			setPhasingMap(pMap);
			setPrephasingMap(preMap);

		}catch(EOFException EOFEx){
			eofCheck = false;
		}catch(IOException Ex){
			eofCheck = false;
		}
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
	    List<Integer> digits = new ArrayList<Integer>();
	    while(i > 0) {
	        digits.add(i % 10);
	        i /= 10;
	    }
	    return digits;
	}

}
