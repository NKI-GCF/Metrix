// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2014 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.parsers.illumina;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import nki.constants.Constants;
import nki.objects.IntensityMap;
import nki.objects.IntensityScores;

import nki.util.LoggerWrapper;

public class ExtractionMetrics extends GenericIlluminaParser {
  List<Integer> cycles = new ArrayList<>();
  private IntensityScores iScores;
  // Instantiate Logger
  private static final LoggerWrapper metrixLogger = LoggerWrapper.getInstance();

  public ExtractionMetrics(String source, int state) {
    super(ExtractionMetrics.class, source, state);
  }
  
  public IntensityScores getIntensityScores() {
    if (iScores == null) {
      iScores = digestData();
    }
    return iScores;
  }

    /*
     * Binary structure:
     *	byte 0: file version number (2)
     *	byte 1: length of each record
     *	bytes (N * 38 + 2) - (N *38 + 39): record:
     *	2 bytes: lane number (uint16)
     *	2 bytes: tile number (uint16)
     *	2 bytes: cycle number (uint16)
     *	4 x 4 bytes: fwhm scores (float) for channel [A, C, G, T] respectively
     *	2 x 4 bytes: intensities (uint16) for channel [A, C, G, T] respectively
     *	8 bytes: date/time of CIF creation
     *
     */

  public IntensityScores digestData() {
    iScores = new IntensityScores();
    if (fileMissing) {
      return iScores;
    }

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
      
      while (leis.available() > 40) {
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
        
        float fA = leis.readFloat();
        float fC = leis.readFloat();
        float fG = leis.readFloat();
        float fT = leis.readFloat();

        //-- Raw Int A
        iMap.addMapping(tileNr, Constants.METRIC_EX_RAWINT_A, (double)leis.readUnsignedShort());

        // Raw Int C
        iMap.addMapping(tileNr, Constants.METRIC_EX_RAWINT_C, (double)leis.readUnsignedShort());

        // Raw Int G
        iMap.addMapping(tileNr, Constants.METRIC_EX_RAWINT_G, (double)leis.readUnsignedShort());

        // Raw Int T
        iMap.addMapping(tileNr, Constants.METRIC_EX_RAWINT_T, (double)leis.readUnsignedShort());
        
        long dateTime = leis.readLong();
        
        cycleMap.put(cycleNr, iMap);
        iScores.setLane(cycleMap, laneNr);
      }
    }catch (EOFException eof) {
      // Reached end of file
      // Lazy EOF - Ignore checking.
    }
    catch (IOException exMain) {
      exMain.printStackTrace();
      metrixLogger.log.log(Level.SEVERE, "Error in main parsing of metrics data: {0}", exMain.toString());
    }

    // Return the qualityScores object.
    return iScores;
  }

  public List<Integer> getUniqueCycles() {
    try {
      leis.skipBytes(6);

      while (true) {
        int cycleNr = leis.readUnsignedShort();
        cycles.add(cycleNr);
        leis.skipBytes(36);
      }
    }
    catch (IOException ex) {
      ex.printStackTrace();
      metrixLogger.log.log(Level.SEVERE, "IOException in Unique Cycles {0}", ex.toString());
    }

    List<Integer> newList = new ArrayList<>(new HashSet<>(cycles));
    Collections.sort(newList);

    return newList;
  }

  public int getLastCycle() {
    try {
      if (leis != null) {
        leis.skipBytes(6);

        while (true) {
          int cycleNr = leis.readUnsignedShort();
          cycles.add(cycleNr);
          leis.skipBytes(36);
        }
      }
      if (leis != null) {
        leis.close();
      }
    }
    catch (IOException ex) {

    }

    int max = 0;

    for (int c : cycles) {
      if (c > max) {
        max = c;
      }
    }
    return max;
  }
}
