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
import nki.objects.PhasingCollection;
import nki.objects.Phasing;

public class TileMetrics {
	private String source = "";
	LittleEndianInputStream leis = null;
	private int version = 0;
	private int recordLength = 0;
	private final static int 		CLUSTER_DENSITY 	= 100;
	private final static int 		CLUSTER_DENSITY_PF 	= 101;
	private boolean fileMissing = false;

	// Lane --> ClusterDensities
	private ClusterDensity cdMap = new ClusterDensity();
	// Lane --> ClusterDensityPassingFilter
	private ClusterDensity cdPFMap = new ClusterDensity();
	// LANE --> READ --> PhasingScores
	private PhasingCollection pMap = new PhasingCollection();
	// LANE --> READ --> PrePhasingScores
	private PhasingCollection preMap = new PhasingCollection();

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
			System.out.println("Tile Metrics file not available for " + source);
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
				
				if(metricValue == 0){
					continue;
				}

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
	    List<Integer> digits = new ArrayList<Integer>();
	    while(i > 0) {
	        digits.add(i % 10);
	        i /= 10;
	    }
	    return digits;
	}

}
