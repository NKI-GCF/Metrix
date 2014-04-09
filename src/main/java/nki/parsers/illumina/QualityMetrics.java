// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.parsers.illumina;

import java.io.IOException;
import java.io.EOFException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import nki.objects.QualityScores;
import nki.objects.QualityMap;
import nki.objects.Reads;
import nki.util.LoggerWrapper;

public class QualityMetrics extends GenericIlluminaParser {
  QualityScores qScores;

  // Instantiate Logger
  private static final LoggerWrapper metrixLogger = LoggerWrapper.getInstance();

  public QualityMetrics(String source, int state) {
    super(QualityMetrics.class, source, state);
  }

  public QualityScores getQualityScores() {
    if (qScores == null) {
      qScores = digestData();
    }
    return qScores;
  }

	/*
   * Binary structure:
	 *	byte 0: file version number (4)
	 *	byte 1: length of each record
	 *	bytes (N * 206 + 2) - (N *206 + 207): record:
	 *	2 bytes: lane number (uint16)
	 *	2 bytes: tile number (uint16)
	 *	2 bytes: cycle number (uint16)
	 *	4 x 50 bytes: number of clusters assigned score (uint32) Q1 through Q50
	 */

  public QualityScores digestData() {
    qScores = new QualityScores();
    if (fileMissing) {
      return qScores;
    }

    try {
      setVersion(leis.readByte());
      setRecordLength(leis.readByte());
    }
    catch (IOException Ex) {
      metrixLogger.log.log(Level.SEVERE, "Error in parsing version number and recordLength: {0}", Ex.toString());
    }

    try {
      Map<Integer, QualityMap> cycleMap;

      qScores.setSource(this.getSource());
      qScores.setVersion(this.getVersion());
      qScores.setRecordLength(this.getRecordLength());

      boolean qcFlag;
      QualityMap qMap;
      while (true) {
        int laneNr = leis.readUnsignedShort();
        int tileNr = leis.readUnsignedShort();
        int cycleNr = leis.readUnsignedShort();

        if (qScores.getLane(laneNr) != null) {
          cycleMap = qScores.getLane(laneNr);
        }
        else {
          cycleMap = new HashMap<>();
        }

        if (cycleMap.containsKey(cycleNr)) {
          qMap = cycleMap.get(cycleNr);
        }
        else {
          qMap = new QualityMap();
        }

        qcFlag = true;
        int qcRecord = 1;

        while (qcFlag) {

          if (qcRecord == 50) {
            qcFlag = false;
          }
          //if(!rds.cycleIsIndex(cycleNr)){
          qMap.addMapping(tileNr, qcRecord, leis.readInt());
          //}else{
          //	leis.readInt();
          //}
          qcRecord++;
        }
        cycleMap.put(cycleNr, qMap);
        qScores.setLane(cycleMap, laneNr);
      }
    }
    catch (EOFException EOFEx) {
      // Reached end of file
    }
    catch (IOException Ex) {
      LoggerWrapper.log.severe("IO Error in parsing Quality Metrics");
    }

    // Return the qualityScores object.
    return qScores;
  }

  public void iterateQS() {
    if (qScores != null) {
      for (Integer lane : qScores.getRawScores().keySet()) {
        Map<Integer, QualityMap> laneScores = qScores.getRawScores().get(lane);
        for (int cycle : laneScores.keySet()) {
          QualityMap qmap = laneScores.get(cycle);
          for (int tile : qmap.getMappings().keySet()) {
            Map<Integer, Integer> qmetricMap = qmap.getMappings().get(tile);
            for (int metric : qmetricMap.keySet()) {
              int value = qmetricMap.get(metric);
              System.out.println("Lane: " + lane + "\tCycle: " + cycle + "\tTile: " + tile + "\tQMetric: " + metric + "\t#Clust\\wScore: " + value);
            }
          }
        }
      }
    }
  }
}
