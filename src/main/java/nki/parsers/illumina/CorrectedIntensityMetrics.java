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

import nki.objects.IntensityScores;
import nki.objects.IntensityMap;
import nki.constants.Constants;
import nki.util.LoggerWrapper;

public class CorrectedIntensityMetrics extends GenericIlluminaParser {
  private IntensityScores iScores;

  // Instantiate Logger
  private static final LoggerWrapper metrixLogger = LoggerWrapper.getInstance();

  public CorrectedIntensityMetrics(String source, int state) {
    super(CorrectedIntensityMetrics.class, source, state);
  }

  public IntensityScores getIntensityScores() {
    if (iScores == null) {
      iScores = digestData();
    }
    return iScores;
  }

	/*
   * Binary structure:
	 * 	byte 0: file version number (2)
	 *	byte 1: length of each record
	 *	bytes (N * 48 + 2) - (N *48 + 49): record:
	 *	2 bytes: lane number (uint16)
	 *	2 bytes: tile number (uint16)
	 *	2 bytes: cycle number (uint16)
	 *	2 bytes: average intensity (uint16)
	 *	2 bytes: average corrected int for channel A (uint16)
	 *	2 bytes: average corrected int for channel C (uint16)
	 *	2 bytes: average corrected int for channel G (uint16)
	 *	2 bytes: average corrected int for channel T (uint16)
	 *	2 bytes: average corrected int for called clusters for base A (uint16)
	 *	2 bytes: average corrected int for called clusters for base C (uint16)
	 *	2 bytes: average corrected int for called clusters for base G (uint16)
	 *	2 bytes: average corrected int for called clusters for base T (uint16)
	 *	20 bytes: number of base calls (float) for No Call and channel [A, C, G, T] respectively
	 *	4 bytes: signal to noise ratio (float)
	 */

  public IntensityScores digestData() {
    iScores = new IntensityScores();

    try {
      iScores.setVersion(leis.readByte());
      iScores.setRecordLength(leis.readByte());
      iScores.setSource(this.getSource());
    }
    catch (IOException Ex) {
      metrixLogger.log.log(Level.SEVERE, "Error in parsing version number and recordLength: {0}", Ex.toString());
    }

    try {
      Map<Integer, IntensityMap> cycleMap;
      IntensityMap iMap;

      while (true) {
        int laneNr = leis.readUnsignedShort();
        int tileNr = leis.readUnsignedShort();
        int cycleNr = leis.readUnsignedShort();

        if (iScores.getLane(laneNr) != null) {
          cycleMap = iScores.getLane(laneNr);
        }
        else {
          cycleMap = new HashMap<>();
        }

        if (cycleMap.containsKey(cycleNr)) {
          iMap = cycleMap.get(cycleNr);
        }
        else {
          iMap = new IntensityMap();
        }

        // Avg Corrected Int
        iMap.addMapping(tileNr, Constants.METRIC_VAR_ACI, (double)leis.readUnsignedShort());

        //-- Avg Corrected Int A
        iMap.addMapping(tileNr, Constants.METRIC_VAR_ACI_A, (double)leis.readUnsignedShort());

        // Avg Corrected Int C
        iMap.addMapping(tileNr, Constants.METRIC_VAR_ACI_C, (double)leis.readUnsignedShort());

        // Avg Corrected Int G
        iMap.addMapping(tileNr, Constants.METRIC_VAR_ACI_G, (double)leis.readUnsignedShort());

        // Avg Corrected Int T
        iMap.addMapping(tileNr, Constants.METRIC_VAR_ACI_T, (double)leis.readUnsignedShort());

        //-- Avg Corrected Int Called Clusters A
        iMap.addMapping(tileNr, Constants.METRIC_VAR_ACICC_A, (double)leis.readUnsignedShort());

        // Avg Corrected Int Called Clusters C
        iMap.addMapping(tileNr, Constants.METRIC_VAR_ACICC_C, (double)leis.readUnsignedShort());

        // Avg Corrected Int Called Clusters G
        iMap.addMapping(tileNr, Constants.METRIC_VAR_ACICC_G, (double)leis.readUnsignedShort());

        // Avg Corrected Int Called Clusters T
        iMap.addMapping(tileNr, Constants.METRIC_VAR_ACICC_T, (double)leis.readUnsignedShort());

        //-- Num of base calls for No Call (Float)
        iMap.addMapping(tileNr, Constants.METRIC_VAR_NUM_BCS_NC, (double)leis.readFloat());

        // Num of base calls for A (Float)
        iMap.addMapping(tileNr, Constants.METRIC_VAR_NUM_BCS_A, (double)leis.readFloat());

        // Num of base calls for C (Float)
        iMap.addMapping(tileNr, Constants.METRIC_VAR_NUM_BCS_C, (double)leis.readFloat());

        // Num of base calls for G (Float)
        iMap.addMapping(tileNr, Constants.METRIC_VAR_NUM_BCS_G, (double)leis.readFloat());

        // Num of base calls for T (Float)
        iMap.addMapping(tileNr, Constants.METRIC_VAR_NUM_BCS_T, (double)leis.readFloat());

        // Signal to noise ratio
        iMap.addMapping(tileNr, Constants.METRIC_VAR_NUM_SIGNOISE, (double)leis.readFloat());

        cycleMap.put(cycleNr, iMap);
        iScores.setLane(cycleMap, laneNr);
      }
    }
    catch (EOFException eof) {
      // Reached end of file
      // Lazy EOF - Ignore checking.
    }
    catch (IOException ex) {
      ex.printStackTrace();
      LoggerWrapper.log.severe("IO Error - CorrectedIntensityMetrics digest");
    }

    // Return the qualityScores object.
    return iScores;
  }
}
