package nki.core;

// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2014 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

import java.io.*;
import java.text.*;
import java.util.Map;
import java.util.logging.Level;

import nki.objects.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;

import nki.constants.Constants;
import nki.io.DataStore;
import nki.parsers.illumina.QualityMetrics;
import nki.parsers.illumina.TileMetrics;
import nki.parsers.illumina.CorrectedIntensityMetrics;
import nki.parsers.illumina.IndexMetrics;
import nki.parsers.illumina.ErrorMetrics;
import nki.parsers.illumina.ExtractionMetrics;
import nki.parsers.xml.XmlDriver;
import nki.util.LoggerWrapper;

public class MetrixContainer {
  protected static final Logger log = LoggerFactory.getLogger(MetrixContainer.class);

  private Summary sum;
  private TileMetrics tm;
  private QualityMetrics qm;
  private CorrectedIntensityMetrics cim;
  private IndexMetrics im;
  private ErrorMetrics em;
  private ExtractionMetrics exm;

  private ClusterDensity clusterDensity;
  private ClusterDensity clusterDensityPassingFilter;
  private PhasingCollection phasingMap;
  private PhasingCollection prephasingMap;
  private QScoreDist qScoreDist;
  private Map<Integer, QScoreDist> qScoreLaneDist;
  private ErrorDist eDist;

  private IntensityDist iDistAvg;
  private IntensityDist iDistCCAvg;
  private FWHMDist fwhmDist;
 
  private QualityScores qsOut;
  private IntensityScores isOut;
  private ErrorCollection ecOut;
  private Indices indices;

  private DecimalFormat df = new DecimalFormat("##.##");
  private String extractionMetrics;
  private String tileMetrics;
  private String qualityMetrics;
  private String intensityMetrics;
  private String indexMetrics;
  private String errorMetrics;
  private String runDir;
  
  private boolean timeCheck;
  private boolean update = false;
  private boolean remote = false;
  private boolean force = false;
  
  public boolean hasUpdated = false;
  /*
  * MetrixContainer to support previously parsed Summary objects.
  */
  public MetrixContainer(Summary summary, boolean remote){
      this.sum = summary;
      this.remote = remote;
      
      if (sum != null) {
        runDir = sum.getRunDirectory();

        long currEpoch = System.currentTimeMillis();
        timeCheck = (currEpoch - sum.getLastUpdatedEpoch()) > Constants.METRIC_UPDATE_TIME &!
                    (this.sum.getHasFinished() || this.sum.getState() == Constants.STATE_FINISHED);

        initPaths(runDir);
        initSummary();
      }
      else {
        // Throw error
        LoggerWrapper.log.severe("[WARNING] Obtained an empty summary.");
        //throw new IOException();
      }
   }
 
    public MetrixContainer(Summary summary, boolean remote, boolean force){
      this.sum = summary;
      this.remote = remote;
      this.force = force;
      
      if (sum != null) {
        runDir = sum.getRunDirectory();

        long currEpoch = System.currentTimeMillis();
        timeCheck = (currEpoch - sum.getLastUpdatedEpoch()) > Constants.METRIC_UPDATE_TIME &!
                    (this.sum.getHasFinished() || this.sum.getState() == Constants.STATE_FINISHED);

        initPaths(runDir);
        initSummary();
      }
      else {
        // Throw error
        LoggerWrapper.log.severe("[WARNING] Obtained an empty summary.");
        //throw new IOException();
      }
  }  
  
  /*
  * MetrixContainer to support a standalone approach where the whole run directory
  * is parsed and formatted according to a set specification.
  */
  public MetrixContainer(String runDir) {
    /*
      Initialize the paths that point to the InterOp files required to be processed.
      */
    initPaths(runDir);

    this.sum = new Summary();
    
    // Process result
    tm = new TileMetrics(tileMetrics, 0);
    qm = new QualityMetrics(qualityMetrics, 0);
    cim = new CorrectedIntensityMetrics(intensityMetrics, 0);
    im = new IndexMetrics(indexMetrics, 0);
    em = new ErrorMetrics(errorMetrics, 0);
    exm = new ExtractionMetrics(extractionMetrics, 0);

    log.debug("Processing RunInfo details");
    try {
      if (!sum.getXmlInfo()) {
        XmlDriver xmd = new XmlDriver(runDir, sum);
        if (xmd.parseRunInfo()) {
          sum = xmd.getSummary();
        }
        else {
          sum.setXmlInfo(false);
        }
      }
    }
    catch (SAXException se) {
      se.printStackTrace();
      log.error("Error parsing RunInfo.xml info: " + se.getMessage());
    }
    catch (IOException ioe) {
      ioe.printStackTrace();
      log.error("Input/output error in parsing RunInfo.xml info: " + ioe.getMessage());
    }
    catch (ParserConfigurationException pce) {
      pce.printStackTrace();
      log.error("Error in XML parser configuration: " + pce.getMessage());
    }

    log.debug("Processing Extraction Metrics");
    if (!exm.getFileMissing()) {
      sum.setCurrentCycle(exm.getLastCycle());
    }
    else {
      log.error("Unable to process Extraction Metrics: " + Constants.EXTRACTION_METRICS + " file is missing.");
    }

    log.debug("Processing Index Metrics");
    if (!im.getFileMissing()) {
      indices = im.digestData();
    }
    else {
      log.error("Unable to process Index Metrics: " + Constants.INDEX_METRICS + " file is missing.");
      indices = null;
    }

    // Metrics digestion
    log.debug("Processing tile metrics");
    if (!tm.getFileMissing()) {
      //TODO - reads previously required
      //tm.digestData(rds);
      tm.digestData();
      clusterDensity = tm.getCDmap();
      clusterDensityPassingFilter = tm.getCDpfMap();
      phasingMap = tm.getPhasingMap();
      prephasingMap = tm.getPrephasingMap();
    }
    else {
      log.error("Unable to process Tile Metrics: " + Constants.TILE_METRICS + " file is missing.");
      clusterDensity = null;
      clusterDensityPassingFilter = null;
      phasingMap = null;
      prephasingMap = null;
    }

    if (sum.getCurrentCycle() > 25) {
      log.debug("Processing Quality Metrics");
      if (!qm.getFileMissing()) {
        //TODO - reads previously required
        //qsOut = qm.digestData(rds);
        qsOut = qm.digestData();
        qScoreDist = qsOut.getQScoreDistribution();
        qScoreLaneDist = qsOut.getQScoreDistributionByLane();
      }
      else {
        log.error("Unable to process Quality Metrics: " + Constants.QMETRICS_METRICS + " file is missing.");
        qScoreDist = null;
        qScoreLaneDist = null;
      }

      log.debug("Processing Corrected Intensity Metrics");
      if (!cim.getFileMissing()) {
        isOut = cim.digestData();
        iDistAvg = isOut.getAverageCorrectedIntensityDist();
        iDistCCAvg = isOut.getCalledClustersAverageCorrectedIntensityDist();
      }
      else {
        log.error("Unable to process Corrected Intensity Metrics: "  + Constants.CORRECTED_INT_METRICS + " file is missing.");
      }
    }
    else {
      System.out.println("Can only generate Quality and Intensity metrics after cycle 25. Current cycle is: " + sum.getCurrentCycle());
    }

    if (sum.getCurrentCycle() > 52) {
      log.debug("Processing Error Metrics");
      if (!em.getFileMissing()) {
        ecOut = em.digestData();
        eDist = ecOut.getErrorDistribution();
      }
      else {
        log.error("Unable to process Error Metrics: "  + Constants.ERROR_METRICS + " file is missing.");
        eDist = null;
      }
    }
  }

  private void initPaths(String runDir){
    this.extractionMetrics = runDir + "/InterOp/" + Constants.EXTRACTION_METRICS;
    this.tileMetrics = runDir + "/InterOp/" + Constants.TILE_METRICS;
    this.qualityMetrics = runDir + "/InterOp/" + Constants.QMETRICS_METRICS;
    this.intensityMetrics = runDir + "/InterOp/" + Constants.CORRECTED_INT_METRICS;
    this.indexMetrics = runDir + "/InterOp/" + Constants.INDEX_METRICS;
    this.errorMetrics = runDir + "/InterOp/" + Constants.ERROR_METRICS;
  }
  
  private void initSummary(){
        log.debug("Processing RunInfo details");
        try {
          if ((!sum.getXmlInfo() || force) && !this.remote) {
            XmlDriver xmd = new XmlDriver(runDir, sum);
            if (xmd.parseRunInfo()) {
              sum = xmd.getSummary();
            }
            else {
              sum.setXmlInfo(false);
            }
          }
        }
        catch (SAXException se) {
          se.printStackTrace();
          log.error("Error parsing RunInfo.xml info: " + se.getMessage());
        }
        catch (IOException ioe) {
          ioe.printStackTrace();
          log.error("Input/output error in parsing RunInfo.xml info: " + ioe.getMessage());
        }
        catch (ParserConfigurationException pce) {
          pce.printStackTrace();
          log.error("Error in XML parser configuration: " + pce.getMessage());
        }
      // Load TileMetrics
        // Process Cluster Density and phasing / prephasing
        if ((!sum.hasClusterDensity() ||
            !sum.hasClusterDensityPF() ||
            !sum.hasPhasing() ||
            !sum.hasPrephasing() ||
            timeCheck ||
            force) && !this.remote
            ) {
               TileMetrics tm = new TileMetrics(tileMetrics, 0);
              log.debug("Processing Tile Metrics");
              //tm.digestData(rds);
              tm.digestData();
              sum.setClusterDensity(tm.getCDmap());
              sum.setClusterDensityPF(tm.getCDpfMap());
              sum.setPhasingMap(tm.getPhasingMap());              // Get all values for summary and populate
              sum.setPrephasingMap(tm.getPrephasingMap());

              // Distribution present in ClusterDensity Object.
              update = true;
              tm.closeSourceStream();
              tm = null;
        }
      
        // Load QualityMetrics
        // Process QScore Dist
        log.debug("Checking Quality Metrics");
        if ((!sum.hasQScores() || 
             timeCheck ||
             force) && !this.remote){
           LoggerWrapper.log.fine("Processing Quality Metrics");
           QualityMetrics qm = new QualityMetrics(qualityMetrics, 0);
           //if (!qm.getFileMissing()) {
           if((!sum.hasQScoreDist() && !qm.getFileMissing()) || this.force){
             QualityScores qsOut = qm.digestData();
             //sum.setQScores(qsOut);
             // Calculate and set distributions
             sum.setQScoreDist(qsOut.getQScoreDistribution());
             sum.setQScoreDistByLane(qsOut.getQScoreDistributionByLane());
             sum.setQScoreDistByCycle(qsOut.getQScoreDistributionByCycle());
             
             update = true;
           }
           qm.closeSourceStream();
           qm = null; // Manual GC
         }        
        
        // Load CorrectedIntensityMetrics
        // Process Corrected Intensities (+ Avg Cor Int Called Clusters)
        log.debug("Checking Corrected Intensity Metrics");
        CorrectedIntensityMetrics cim = new CorrectedIntensityMetrics(intensityMetrics, 0);
        if ((( !sum.hasIntensityDistAvg() || 
                !sum.hasIntensityDistCCAvg() || 
                !sum.hasIntensityDistRaw()) || 
                timeCheck || 
                force) && !this.remote) {
            log.debug("Processing Corrected Intensity Metrics");
            if (!cim.getFileMissing()) {
              IntensityScores isOut = cim.digestData();
              //sum.setIScores(isOut);

              // Calculate distribution
              sum.setIntensityDistAvg(isOut.getAverageCorrectedIntensityDist());
              sum.setIntensityDistCCAvg(isOut.getCalledClustersAverageCorrectedIntensityDist());
              update = true;
           }
            cim.closeSourceStream();
            cim = null; // Manual GC     
        }

        // Load ExtractionMetrics
        // Process Raw Intensities
        if((!sum.hasIntensityDistRaw() || 
            timeCheck ||
            force) && !this.remote) {
            ExtractionMetrics eim = new ExtractionMetrics(extractionMetrics, 0);
            if (!eim.getFileMissing()) {
              eim.digestData();
              IntensityScores risOut = eim.getIntensityScores();
              FWHMScores fsOut = eim.getFWHMScores();

              // Calculate distribution
              sum.setIntensityDistRaw(risOut.getRawIntensityDist());
              sum.setFWHMDist(fsOut.getAverageFWHMDist());
              update = true;
            }
        eim.closeSourceStream();
        eim = null; // Manual GC
        }        
        
        // Load IndexMetrics
        if((!sum.hasSampleInfo() ||
            force) && !this.remote){
            IndexMetrics im = new IndexMetrics(indexMetrics, 0);
            log.debug("Processing Index Metrics");
            Indices indices = im.digestData();
            sum.setSampleInfo(indices);
            update = true;
            im.closeSourceStream();
            im = null; // Manual GC
        }
        
      // Load ErrorMetrics
      if((!sum.hasErrorDist() ||
            force) && !this.remote){
        if (sum.getCurrentCycle() > 52) {
          log.debug("Processing Error Metrics");
          ErrorMetrics em = new ErrorMetrics(errorMetrics, 0);
          if (!em.getFileMissing()) {
            ecOut = em.digestData();
            eDist = ecOut.getErrorDistribution();
            sum.setErrorDist(eDist);
            update = true;
          }
          else {
            log.error("Unable to process Error Metrics: "  + Constants.ERROR_METRICS + " file is missing.");
            eDist = null;
          }
        }else{
            log.debug("Run: " + sum.getRunId() + " has not passed cycle 52 yet.");
        }          
      }
        
      if ((update == true ||
            force) && !this.remote) {
          try {
            DataStore ds = new DataStore();
            sum.setLastUpdated();
            log.debug("Updating " + sum.getRunId() + " in database. ("+runDir+")");
            ds.updateSummaryByRunName(sum, runDir);
            ds.closeAll();
            log.debug("Done.");
            hasUpdated = true;
          }
          catch(IOException IOE){
              LoggerWrapper.log.log(Level.SEVERE, "IOException in update statement {0}", IOE.toString());
          }
          catch (Exception SEx) {
            LoggerWrapper.log.log(Level.SEVERE, "Exception in update statement {0}", SEx.toString());
            SEx.printStackTrace();
          }

      }
  }
  
  public Summary getSummary() {
    return sum;
  }

  public TileMetrics getTileMetrics() {
    return tm;
  }

  public QualityMetrics getQualityMetrics() {
    return qm;
  }

  public CorrectedIntensityMetrics getCorrectedIntensityMetrics() {
    return cim;
  }

  public IndexMetrics getIndexMetrics() {
    return im;
  }

  public ErrorMetrics getErrorMetrics() {
    return em;
  }

  public ExtractionMetrics getExtractionMetrics() {
    return exm;
  }
  
  public void outputSummaryLog() {
    Reads rds = sum.getReads();

    System.out.println("== Combined Read Q-Scores ==");
    if (qScoreDist != null) {
      System.out.println("- %>=Q20 = " + df.format(qScoreDist.aboveQ(20)) + "%");
      System.out.println("- %>=Q30 = " + df.format(qScoreDist.aboveQ(30)) + "%\n");
    }
    else {
      System.out.println("- %>=Q20 = N/A");
      System.out.println("- %>=Q30 = N/A");
    }

    System.out.println("== Q-Score distribution per lane %>Q20/%>Q30 ==");
    if (qScoreDist != null) {
      for (Integer laneNum : qScoreLaneDist.keySet()) {
        QScoreDist dist = qScoreLaneDist.get(laneNum);
        System.out.println(laneNum + "\t" + df.format(dist.aboveQ(20)) + "%/" + df.format(dist.aboveQ(30)) + "%");
      }
    }
    else {
      log.error("Q-Score distribution per lane not available.");
    }

    System.out.println("== Phasing / Prephasing Scores ==");
    if (phasingMap != null) {
      System.out.println("Lane\tRead\tScore");
      for (String line : phasingMap.toTab(prephasingMap, rds).split("\n")) {
        System.out.println(line);
      }
    }
    else {
      log.error("Phasing/prephasing scores not available.");
    }

    System.out.println("== Cluster Density / Cluster Density Passing Filter (k/mm2) ==");
    if (clusterDensity != null) {
      System.out.println(clusterDensity.toTab(clusterDensityPassingFilter));
    }
    else {
      log.error("Cluster Density Metrics not available.");
    }

    System.out.println("== Index Metrics ==");
    if (indices != null) {
      System.out.println(indices.toTab());
    }
    else {
      log.error("No Index Metrics information available.");
    }

    System.out.println("== Error Metrics ==");
    if (eDist != null) {
      System.out.println(eDist.toTab("rate"));
    }
    else {
      log.error("Error Metrics not available.");
    }

    System.out.println("== Run Progress of " + sum.getRunId() + "(" + sum.getRunType() + " - " + sum.getTotalCycles() + "bp)==");
    if (sum.getCurrentCycle() == sum.getTotalCycles()) {
      System.out.println("Run has finished: " + sum.getCurrentCycle() + "/" + sum.getTotalCycles() + ".");
    }
    else if (sum.getRunType().equals("Paired End")) {
      if (sum.getCurrentCycle() == rds.getPairedTurnCycle()) {
        System.out.println("Run needs turning. Currently at: " + sum.getCurrentCycle() + "/" + sum.getTotalCycles() + ".");
      }
    }
    else {
      System.out.println("Run has not finished yet. Currently at: " + sum.getCurrentCycle() + "/" + sum.getTotalCycles() + ".");
    }
  }
}
