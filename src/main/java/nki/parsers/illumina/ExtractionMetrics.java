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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nki.constants.Constants;
import nki.objects.IntensityMap;
import nki.objects.IntensityScores;
import nki.objects.FWHMMap;
import nki.objects.FWHMScores;

public class ExtractionMetrics extends GenericIlluminaParser {
  List<Integer> cycles = new ArrayList<>();
  private IntensityScores iScores;
  private FWHMScores fScores;
  // Instantiate Logger
  protected static final Logger log = LoggerFactory.getLogger(ExtractionMetrics.class);

  public ExtractionMetrics(String source, int state) {
    super(ExtractionMetrics.class, source, state);
  }

  public IntensityScores getIntensityScores() {
    if (iScores == null) {
      digestData();
    }
    return iScores;
  }

  public FWHMScores getFWHMScores() {
    if (fScores == null) {
      digestData();
    }
    return fScores;
  }

  /*
   * Binary structure: byte 0: file version number (2) byte 1: length of each
   * record bytes (N * 38 + 2) - (N *38 + 39): record: 2 bytes: lane number
   * (uint16) 2 bytes: tile number (uint16) 2 bytes: cycle number (uint16) 4 x 4
   * bytes: fwhm scores (float) for channel [A, C, G, T] respectively 2 x 4
   * bytes: intensities (uint16) for channel [A, C, G, T] respectively 8 bytes:
   * date/time of CIF creation
   *
   */

  public void digestData() {
    iScores = new IntensityScores();
    fScores = new FWHMScores();
    if (fileMissing) {
      log.debug("ExtractionMetrics file is missing for digest.");
      return;
    }

    try {
      this.version = leis.readByte();
      iScores.setVersion(this.version);
      fScores.setVersion(this.version);
      this.recordLength = leis.readByte();
      iScores.setRecordLength(this.recordLength);
      fScores.setRecordLength(this.recordLength);

      iScores.setSource(this.getSource());
      fScores.setSource(this.getSource());
    }
    catch (IOException Ex) {
      log.error("Error in parsing version number and recordLength.", Ex);
    }

    try {
      Map<Integer, IntensityMap> cycleMap;
      Map<Integer, FWHMMap> cycleFWHMMap;
      IntensityMap iMap;
      FWHMMap fMap;

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

        if (fScores.getLane(laneNr) != null) {
          cycleFWHMMap = fScores.getLane(laneNr);
        }
        else {
          cycleFWHMMap = new HashMap<>();
        }

        if (cycleMap.containsKey(cycleNr)) {
          iMap = cycleMap.get(cycleNr);
        }
        else {
          iMap = new IntensityMap();
        }

        if (cycleFWHMMap.containsKey(cycleNr)) {
          fMap = cycleFWHMMap.get(cycleNr);
        }
        else {
          fMap = new FWHMMap();
        }

        // -- FWHM Score A
        fMap.addMapping(tileNr, Constants.METRIC_VAR_FWHM_A, (double) leis.readFloat());

        // -- FWHM Score C
        fMap.addMapping(tileNr, Constants.METRIC_VAR_FWHM_C, (double) leis.readFloat());

        // -- FWHM Score G
        fMap.addMapping(tileNr, Constants.METRIC_VAR_FWHM_G, (double) leis.readFloat());

        // -- FWHM Score T
        fMap.addMapping(tileNr, Constants.METRIC_VAR_FWHM_T, (double) leis.readFloat());

        // -- Raw Int A
        iMap.addMapping(tileNr, Constants.METRIC_EX_RAWINT_A, (double) leis.readUnsignedShort());

        // Raw Int C
        iMap.addMapping(tileNr, Constants.METRIC_EX_RAWINT_C, (double) leis.readUnsignedShort());

        // Raw Int G
        iMap.addMapping(tileNr, Constants.METRIC_EX_RAWINT_G, (double) leis.readUnsignedShort());

        // Raw Int T
        iMap.addMapping(tileNr, Constants.METRIC_EX_RAWINT_T, (double) leis.readUnsignedShort());

        long dateTime = leis.readLong();

        cycleMap.put(cycleNr, iMap);
        cycleFWHMMap.put(cycleNr, fMap);

        iScores.setLane(cycleMap, laneNr);
        fScores.setLane(cycleFWHMMap, laneNr);
      }
    }
    catch (EOFException eof) {
      // Reached end of file
      // Lazy EOF - Ignore checking.
    }
    catch (IOException exMain) {
      log.error("Error in main parsing of metrics data.", exMain);
    }

    return;
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
      log.error("Exception in Unique Cycles", ex);
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
