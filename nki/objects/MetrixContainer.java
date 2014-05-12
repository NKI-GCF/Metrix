package nki.objects;

import java.io.*;
import java.util.HashMap;
import java.text.*;
import java.util.Map;

import nki.objects.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;

import nki.constants.Constants;
import nki.parsers.illumina.QualityMetrics;
import nki.parsers.illumina.TileMetrics;
import nki.parsers.illumina.CorrectedIntensityMetrics;
import nki.parsers.illumina.IndexMetrics;
import nki.parsers.illumina.ErrorMetrics;
import nki.parsers.illumina.ExtractionMetrics;
import nki.parsers.xml.XmlDriver;

public class MetrixContainer {
  protected static final Logger log = LoggerFactory.getLogger(MetrixContainer.class);

  private Summary sum = new Summary();
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
  private QualityScores qsOut;
  private IntensityScores isOut;
  private ErrorCollection ecOut;
  private Indices indices;

  private DecimalFormat df = new DecimalFormat("##.##");

  public MetrixContainer(String runDir) {
    String tileMetrics = runDir + "/InterOp/" + Constants.TILE_METRICS;
    String qualityMetrics = runDir + "/InterOp/" + Constants.QMETRICS_METRICS;
    String intensityMetrics = runDir + "/InterOp/" + Constants.CORRECTED_INT_METRICS;
    String indexMetrics = runDir + "/InterOp/" + Constants.INDEX_METRICS;
    String errorMetrics = runDir + "/InterOp/" + Constants.ERROR_METRICS;
    String extractionMetrics = runDir + "/InterOp/" + Constants.EXTRACTION_METRICS;

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
      log.info("Can only generate Quality and Intensity metrics after cycle 25. Current cycle is: " + sum.getCurrentCycle());
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

    log.info("== Combined Read Q-Scores ==");
    if (qScoreDist != null) {
      log.info("- %>=Q20 = " + df.format(qScoreDist.aboveQ(20)) + "%");
      log.info("- %>=Q30 = " + df.format(qScoreDist.aboveQ(30)) + "%\n");
    }
    else {
      log.info("- %>=Q20 = N/A");
      log.info("- %>=Q30 = N/A");
    }

    log.info("== Q-Score distribution per lane %>Q20/%>Q30 ==");
    if (qScoreDist != null) {
      for (Integer laneNum : qScoreLaneDist.keySet()) {
        QScoreDist dist = qScoreLaneDist.get(laneNum);
        log.info(laneNum + "\t" + df.format(dist.aboveQ(20)) + "%/" + df.format(dist.aboveQ(30)) + "%");
      }
    }
    else {
      log.error("Q-Score distribution per lane not available.");
    }

    log.info("== Phasing / Prephasing Scores ==");
    if (phasingMap != null) {
      log.info("Lane\tRead\tScore");
      for (String line : phasingMap.toTab(prephasingMap, rds).split("\n")) {
        log.info(line);
      }
    }
    else {
      log.error("Phasing/prephasing scores not available.");
    }

    log.info("== Cluster Density / Cluster Density Passing Filter (k/mm2) ==");
    if (clusterDensity != null) {
      log.info(clusterDensity.toTab(clusterDensityPassingFilter));
    }
    else {
      log.error("Cluster Density Metrics not available.");
    }

    log.info("== Index Metrics ==");
    if (indices != null) {
      log.info(indices.toTab());
    }
    else {
      log.error("No Index Metrics information available.");
    }

    log.info("== Error Metrics ==");
    if (eDist != null) {
      log.info(eDist.toTab("rate"));
    }
    else {
      log.error("Error Metrics not available.");
    }

    log.info("== Run Progress of " + sum.getRunId() + "(" + sum.getRunType() + " - " + sum.getTotalCycles() + "bp)==");
    if (sum.getCurrentCycle() == sum.getTotalCycles()) {
      log.info("Run has finished: " + sum.getCurrentCycle() + "/" + sum.getTotalCycles() + ".");
    }
    else if (sum.getRunType().equals("Paired End")) {
      if (sum.getCurrentCycle() == rds.getPairedTurnCycle()) {
        log.info("Run needs turning. Currently at: " + sum.getCurrentCycle() + "/" + sum.getTotalCycles() + ".");
      }
    }
    else {
      log.info("Run has not finished yet. Currently at: " + sum.getCurrentCycle() + "/" + sum.getTotalCycles() + ".");
    }
  }
}
