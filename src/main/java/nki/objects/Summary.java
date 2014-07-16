// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2014 Bernd van der Veen

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
import nki.objects.PhasingCollection;
//import nki.objects.ErrorRate;
import nki.objects.Phasing;
import nki.objects.Reads;
import nki.objects.QualityScores;
import nki.objects.QScoreDist;
import nki.objects.IntensityScores;
import nki.objects.IntensityDist;
import nki.objects.Indices;
import nki.constants.Constants;

public class Summary implements Serializable {

  // Object Specific
  private int sumId = -1;
  private static final long serialVersionUID = 42L;

  // Run Properties
  private int currentCycle;      // Current cycle in run
  private int totalCycles;      // Total amount of cycles
  private String flowcellID;      // Flowcell ID
  private String side = "";        // Side of flowcell
  private String lastUpdated = "0";    // Last update time
  private long lastUpdatedEpoch = 0;
  private String phase;        // Phase of run :: Imaging / Basecalling / RTAComplete
  private String runType = "Single End";      // Run Type: Single / Paired End / Nextera
  private int indexLength;      // Length of index read
  private int tileCount;        // Number of Tiles
  private int state = 5;    // State 0: Hanging / State 1: Running / State 2: Complete / State 3: Unknown / State 4: Flowcell needs turning
  private int numReads;      // Total number of reads
  private boolean isNextera = false;  // Is this a nextera run?
  private boolean isIndexed = false;  // Is run indexed.
  private boolean xmlInfo = false;  // Has xmlInfo been parsed?
  private String runId;        // Full run identifier
  private int date;        // Run date
  private int laneCount;      // Number of lanes
  private int surfaceCount;      // Number of surface sides
  private int swathCount;      // Number of swaths
  private int instrumentRunNumber;    // Nth number run on this instrument
  private String instrument;      // Name of instrument
  private String instrumentType = "";    // Type of instrument: HiSeq / MiSeq
  private String runNameOptional = "";
  private Reads reads;        // Read information
  private boolean hasTurned = false;
  private boolean hasFinished = false;  // Has the run finished?
  private boolean hasNotifyTurned = false;
  private String runDirectory = "";    // RunDirectory path
  private int parseError = 0;  // Number of parsing errors

  // Run Metrics
  private ClusterDensity clusterDensity;    // Contains Cluster Density for all lanes
  private ClusterDensity clusterDensityPF;      // Contains Cluster Density Passing Filter for all lanes
  private PhasingCollection phasingMap;      // Phasing values per lane
  private PhasingCollection prephasingMap;    // Prephasing values per lane

  private QualityScores qScores;            // QualityScores per lane, per cycle, per tile
  private QScoreDist qScoreDist;      // The stored distribution of num clusters / QScore

  private IntensityScores iScores;
  private IntensityDist iDistAvg;
  private IntensityDist iDistCCAvg;
  private IntensityDist iDistRaw;

  private ErrorDist eDist;
  
  private Indices sampleInfo;
  private int firstCycleIntensity;

  public void setSumId(int id) {
    this.sumId = id;
  }

  public int getSumId() {
    return sumId;
  }

  public void setCurrentCycle(int cc) {
    if (cc >= this.currentCycle) {
      this.currentCycle = cc;
    }
  }

  public int getCurrentCycle() {
    return currentCycle;
  }

  public void setTotalCycles(int tc) {
    this.totalCycles = tc;
  }

  public int getTotalCycles() {
    return totalCycles;
  }

  public void setFlowcellID(String FCID) {
    this.flowcellID = FCID;
  }

  public String getFlowcellID() {
    return flowcellID;
  }

  public void setInstrument(String instr) {
    this.instrument = instr;
  }

  public String getInstrument() {
    return instrument;
  }

  public void setSide(String s) {
    if (s.equals("A")) {
      this.side = "A";
    }
    else if (s.equals("B")) {
      this.side = "B";
    }
    else {
      this.side = "";
    }
  }

  public String getSide() {
    return side;
  }

  public void setLastUpdated() {
    this.lastUpdatedEpoch = System.currentTimeMillis();
    this.lastUpdated = convertEpochToTime(System.currentTimeMillis());
  }

  public String getLastUpdated() {
    return lastUpdated;
  }

  public long getLastUpdatedEpoch() {
    return this.lastUpdatedEpoch;
  }

  public void setPhase(String phs) { // Change with constants
    this.phase = phs;
  }

  public String getPhase() {
    return phase;
  }

  public void setRunType(String rt) {
    this.runType = rt;
  }

  public String getRunType() {
    return runType;
  }

  public void setTileCount(String tiles) {
    this.tileCount = Integer.parseInt(tiles);
  }

  public int getTileCount() {
    return tileCount;
  }

  public void setXmlInfo(boolean xmlInfo) {
    this.xmlInfo = xmlInfo;
  }

  public boolean getXmlInfo() {
    return xmlInfo;
  }

  public void setRunDate(int runDate) {
    this.date = runDate;
  }

  public int getRunDate() {
    return date;
  }

  public void setInstrumentRunNumber(String runnr) {
    this.instrumentRunNumber = Integer.parseInt(runnr);
  }

  public int getInstrumentRunNumber() {
    return instrumentRunNumber;
  }

  public void setInstrumentType(String instrumentType) {
    this.instrumentType = instrumentType;
  }

  public String getInstrumentType() {
    return instrumentType;
  }

  public void setLaneCount(String numLanes) {
    this.laneCount = Integer.parseInt(numLanes);
  }

  public int getLaneCount() {
    return laneCount;
  }

  public void setSurfaceCount(String surfaceCount) {
    this.surfaceCount = Integer.parseInt(surfaceCount);
  }

  public int getSurfaceCount() {
    return surfaceCount;
  }

  public void setSwathCount(String swathCount) {
    this.swathCount = Integer.parseInt(swathCount);
  }

  public int getSwathCount() {
    return swathCount;
  }

  public void setClusterDensity(ClusterDensity cd) {
    cd.setType("CD");
    this.clusterDensity = cd;
  }

  public ClusterDensity getClusterDensity() {
    return clusterDensity;
  }

  public void setClusterDensityPF(ClusterDensity cdPf) {
    cdPf.setType("PF");
    this.clusterDensityPF = cdPf;
  }

  public ClusterDensity getClusterDensityPF() {
    return clusterDensityPF;
  }

//	public void setErrorRate(HashMap<Object, ErrorRate> er){
//		this.errorRate = er;
//	}

//	public HashMap<Object, ErrorRate> getErrorRate(){
//		return errorRate;
//	}

  public void setFirstCycleIntensity(int fci) {
    this.firstCycleIntensity = fci;
  }

  public int getFirstCycleintensity() {
    return firstCycleIntensity;
  }

  public void setState(int runState) {
    this.state = runState;
  }

  public int getState() {
    return state;
  }

  public void setNumReads(int nr) {
    this.numReads = nr;
  }

  public int getNumReads() {
    return numReads;
  }

  public void setIsNextera(boolean setNT) {
    this.isNextera = setNT;
  }

  public boolean getIsNextera() {
    return isNextera;
  }

  public void setIsIndexed(boolean indexed) {
    this.isIndexed = indexed;
  }

  public boolean getIsIndexed() {
    return isIndexed;
  }

  public void setPhasingMap(PhasingCollection map) {
    this.phasingMap = map;
  }

  public PhasingCollection getPhasingMap() {
    return phasingMap;
  }

  public void setPrephasingMap(PhasingCollection preMap) {
    this.prephasingMap = preMap;
  }

  public PhasingCollection getPrephasingMap() {
    return prephasingMap;
  }

  public void setReads(Reads rds) {
    this.reads = rds;
  }

  public Reads getReads() {
    return reads;
  }

  public void setRunId(String runID) {
    this.runId = runID;
  }

  public String getRunId() {
    return runId;
  }

  public void setRunNameOptional(String opt) {
    this.runNameOptional = opt;
  }

  public String getRunNameOptional() {
    return runNameOptional;
  }

  public int getTurnCycle() {
    return reads.getPairedTurnCycle();
  }

  public void setHasTurned(boolean setTurned) {
    this.hasTurned = setTurned;
  }

  public boolean getHasTurned() {
    return hasTurned;
  }

  public void setHasNotifyTurned(boolean setNotify) {
    this.hasNotifyTurned = setNotify;
  }

  public boolean getHasNotifyTurned() {
    return hasNotifyTurned;
  }

  private String convertEpochToTime(long epoch) {
    Date date = new Date(epoch);
    DateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    TimeZone tz = Calendar.getInstance().getTimeZone();
    format.setTimeZone(tz.getTimeZone(tz.getID()));
    String formatted = format.format(date);
    return formatted;
  }

  public void setQScoreDist(QScoreDist qScoreDist) {
    this.qScoreDist = qScoreDist;
  }

  public QScoreDist getQScoreDist() {
    return qScoreDist;
  }

  public void setQScores(QualityScores qScores) {
    this.qScores = qScores;
  }

  public QualityScores getQScores() {
    return qScores;
  }

  public void setErrorDist(ErrorDist eDist){
      this.eDist = eDist;
  }
  
  public ErrorDist getErrorDist(){
      return this.eDist;
  }
  
  public void setIScores(IntensityScores iScores) {
    this.iScores = iScores;
  }

  public IntensityScores getIScores() {
    return iScores;
  }

  public void setIntensityDistAvg(IntensityDist iDistAvg) {
    this.iDistAvg = iDistAvg;
  }

  public IntensityDist getIntensityDistAvg() {
    return iDistAvg;
  }

  public void setIntensityDistCCAvg(IntensityDist iDistCCAvg) {
    this.iDistCCAvg = iDistCCAvg;
  }

  public IntensityDist getIntensityDistCCAvg() {
    return iDistCCAvg;
  }

  public void setIntensityDistRaw(IntensityDist iDistRaw) {
    this.iDistRaw = iDistRaw;
  }

  public IntensityDist getIntensityDistRaw() {
    return iDistRaw;
  }
  
  public void setSampleInfo(Indices sampleInfo) {
    this.sampleInfo = sampleInfo;
  }

  public Indices getSampleInfo() {
    return sampleInfo;
  }

  public void setRunDirectory(String runDirectory) {
    this.runDirectory = runDirectory;
  }

  public String getRunDirectory() {
    return runDirectory;
  }

  public boolean hasClusterDensity() {
    return clusterDensity != null;
  }

  public boolean hasClusterDensityPF() {
    return clusterDensityPF != null;
  }

  public boolean hasPrephasing() {
    return prephasingMap != null;
  }

  public boolean hasPhasing() {
    return phasingMap != null;
  }

  public boolean hasQScores() {
    return qScores != null;
  }

  public boolean hasQScoreDist() {
    return qScoreDist != null;
  }

  public boolean hasIScores() {
    return iScores != null;
  }

  public boolean hasIntensityDistAvg() {
    return iDistAvg != null;
  }

  public boolean hasIntensityDistCCAvg() {
    return iDistCCAvg != null;
  }

  public boolean hasIntensityDistRaw() {
    return iDistRaw != null;
  }
  
  public boolean hasSampleInfo() {
    return sampleInfo != null;
  }

  public boolean hasErrorDist(){
      return eDist != null;
  }
  
  public void setParseError(int parseError) {
    this.parseError = parseError;
  }

  public void incParseError() {
    this.parseError++;
  }

  public int getParseError() {
    return this.parseError;
  }

  public void setHasFinished(boolean hf) {
    this.hasFinished = hf;
  }

  public boolean getHasFinished() {
    return this.hasFinished;
  }

  public boolean getPairedTurnCheck() {
    return this.getRunType().equals("Paired End") && this.getState() != Constants.STATE_HANG && this.getCurrentCycle() == this.getTurnCycle();
  }
}

