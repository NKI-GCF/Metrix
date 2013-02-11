// Illumina Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.objects;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import nki.objects.ClusterDensity;
import nki.objects.ErrorRate;
import nki.objects.Phasing;
import nki.objects.Reads;

public class Summary implements Serializable {

	// Object Specific
	private int		sumId = -1;
	private static final long serialVersionUID = 42L;	

	// Run Properties
	private int 		currentCycle;			// Current cycle in run
	private int 		totalCycles;			// Total amount of cycles 
	private String		flowcellID;			// Flowcell ID
	private String		instrument;			// Name of instrument
	private String		side;				// Side of flowcell
	private String		lastUpdated 	= "0";		// Last update time
	private String		phase; 				// Phase of run :: Imaging / Basecalling / RTAComplete
	private String		runType;			// Run Type: Single / Paired / Nextera
	private int		indexLength;			// Length of index read
        private int             tileCount;			// Number of Tiles
	private int 		state		= 3;		// State 0: Hanging / State 1: Running / State 2: Complete / State 3: Unknown / State 4: Flowcell needs turning
	private int		numReads;			// Total number of reads
	private	boolean		isNextera	= false; 	// Is this a nextera run?
	private boolean		isIndexed	= false;	// Is run indexed.
	private boolean		xmlInfo		= false;	// Has xmlInfo been parsed?
	private String		runId;				// Full run identifier
	private int		date;				// Run date
	private int		laneCount;			// Number of lanes
	private int 		surfaceCount;			// Number of surface sides
	private	int		swathCount;			// Number of swaths
	private int		machineRunNumber;		// Nth number run on this machine
	private Reads		reads;				// Read information
	private boolean		hasTurned	= false;
	private boolean		hasNotifyTurned	= false;

	// Run Metrics
	private HashMap<Object, ClusterDensity>		clusterDensity;		// Contains Cluster Density for all lanes
	private HashMap<Object, ClusterDensity>		clusterDensityPF;	// Contains Cluster Density Passing Filter for all lanes
	private Map<Integer, Map<Integer, Phasing>> phasingMap;			// Phasing values per lane
	private Map<Integer, Map<Integer, Phasing>> prephasingMap;		// Prephasing values per lane

	private	HashMap<Object, ErrorRate>		errorRate;
	private int 		firstCycleIntensity;

	public void setSumId(int id){
		this.sumId = id;
	}

	public int getSumId(){
		return sumId;
	}

	public void setCurrentCycle(int cc){
		if(cc >= this.currentCycle){
			this.currentCycle = cc;		
		}
	}

	public int getCurrentCycle(){
		return currentCycle;
	}

	public void setTotalCycles(int tc){
		this.totalCycles = tc;
	}

	public int getTotalCycles(){
		return totalCycles;
	}
	
	public void setFlowcellID(String FCID){
		this.flowcellID = FCID;
	}
	
	public String getFlowcellID(){
		return flowcellID;
	}

	public void setInstrument(String instr){
		this.instrument = instr;
	}
	
	public String getInstrument(){
		return instrument;
	}

	public void setSide(String s){
		if(s.equals("A")){
			this.side = "A";	
		}else{
			this.side = "B";
		}
	}

	public String getSide(){
		return side;
	}

	public void setLastUpdated(){
		this.lastUpdated = convertEpochToTime(System.currentTimeMillis());
	}

	public String getLastUpdated(){
		return lastUpdated;
	}

	public void setPhase(String phs){ // Change with constants
		this.phase = phs;
	}
	
	public String getPhase(){
		return phase;
	}

	public void setRunType(String rt){
		this.runType = rt;
	}	

	public String getRunType(){
		return runType;
	}
	
	public void setTileCount(String tiles){
		this.tileCount = Integer.parseInt(tiles);
	}
	
	public int getTileCount(){
		return tileCount;
	}

	public void setXmlInfo(boolean xmlInfo){
		this.xmlInfo = xmlInfo;
	}

	public boolean getXmlInfo(){
		return xmlInfo;
	}

	public void setRunDate(int runDate){
		this.date = runDate;
	}

	public int getRunDate(){
		return date;
	}

	public void setMachineRunNumber(String runnr){
		this.machineRunNumber = Integer.parseInt(runnr);
	}
	
	public int getMachineRunNumber(){
		return machineRunNumber;
	}

	public void setLaneCount(String numLanes){
		this.laneCount = Integer.parseInt(numLanes);
	}

	public int getLaneCount(){
		return laneCount;
	}

	public void setSurfaceCount(String surfaceCount){
		this.surfaceCount = Integer.parseInt(surfaceCount);
	}

	public int getSurfaceCount(){
		return surfaceCount;
	}

	public void setSwathCount(String swathCount){
		this.swathCount = Integer.parseInt(swathCount);
	}

	public int getSwathCount(){
		return swathCount;
	}
	
	public void setClusterDensity(HashMap<Object, ClusterDensity> cd){
		this.clusterDensity = cd;
	}

	public HashMap<Object, ClusterDensity> getClusterDensity(){
		return clusterDensity;
	}

	public void setClusterDensityPF(HashMap<Object, ClusterDensity> cdPf){
                this.clusterDensityPF = cdPf;
        }

        public HashMap<Object, ClusterDensity> getClusterDensityPF(){
                return clusterDensityPF;
        }

	public void setErrorRate(HashMap<Object, ErrorRate> er){
		this.errorRate = er;
	}

	public HashMap<Object, ErrorRate> getErrorRate(){
		return errorRate;
	}
	
	public void setFirstCycleIntensity(int fci){
		this.firstCycleIntensity = fci;
	}

	public int getFirstCycleintensity(){
		return firstCycleIntensity;
	}

	public void setState(int runState){
		this.state = runState;
	}

	public int getState(){
		return state;
	}

	public void setNumReads(int nr){
		this.numReads = nr;
	}

	public int getNumReads(){
		return numReads;
	}	
	
	public void setIsNextera(boolean setNT){
		this.isNextera = setNT;
	}

	public boolean getIsNextera(){
		return isNextera;
	}

	public void setIsIndexed(boolean indexed){
		this.isIndexed = indexed;
	}

	public boolean getIsIndexed(){
		return isIndexed;
	}

	public void setPhasingMap(Map<Integer, Map<Integer, Phasing>> map){
		this.phasingMap = map;
	}

	public Map<Integer, Map<Integer, Phasing>> getPhasingMap(){
		return phasingMap;
	}
	
	public void setPrephasingMap(Map<Integer, Map<Integer, Phasing>> preMap){
		this.prephasingMap = preMap;
	}

	public Map<Integer, Map<Integer, Phasing>> getPrephasingMap(){
		return prephasingMap;
	}

	public void setReads(Reads rds){
//		System.out.println(rds.getTotalNumberOfReads());
		this.reads = rds;
	}

	public Reads getReads(){
		return reads;
	}

	public void setRunId(String runID){
		this.runId = runID;
	}

	public String getRunId(){
		return runId;
	}

	public int getTurnCycle(){
		return reads.getPairedTurnCycle();
	}

	public void setHasTurned(boolean setTurned){
		this.hasTurned = setTurned;
	}

	public boolean getHasTurned(){
		return hasTurned;
	}

	public void setHasNotifyTurned(boolean setNotify){
                this.hasNotifyTurned = setNotify;
        }

        public boolean getHasNotifyTurned(){
                return hasNotifyTurned;
        }

	private String convertEpochToTime(long epoch){
		Date date = new Date(epoch);
	        DateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		TimeZone tz = Calendar.getInstance().getTimeZone();
	        format.setTimeZone(tz.getTimeZone(tz.getID()));
	        String formatted = format.format(date);
		return formatted;
	}
}

