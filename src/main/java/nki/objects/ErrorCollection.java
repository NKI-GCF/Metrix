// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2014 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.objects;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import nki.objects.ErrorDist;

public class ErrorCollection implements Serializable {
  public static final long serialVersionUID = 42L;
  public int version;
  public int recordLength;
  public String source;

  // Lane - Cycle - ErrorMap
  public Map<Integer, Map<Integer, ErrorMap>> eScores = new HashMap<>();
  private ErrorDist eDist = new ErrorDist();

  public void setVersion(int version) {
    this.version = version;
  }

  public int getVersion() {
    return version;
  }

  public void setRecordLength(int recordLength) {
    this.recordLength = recordLength;
  }

  public int getRecordLength() {
    return recordLength;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getSource() {
    return source;
  }

  public void setLane(Map<Integer, ErrorMap> content, int lanenr) {
    Map<Integer, ErrorMap> cycleMap = eScores.get(lanenr);

    if (cycleMap == null) {
      eScores.put(lanenr, content);
    }
    else { // Merge maps
      Map<Integer, ErrorMap> tmpMap = new HashMap<>(content);
      tmpMap.keySet().removeAll(cycleMap.keySet());
      cycleMap.putAll(content);
      eScores.put(lanenr, cycleMap);
    }
  }

  public Map<Integer, ErrorMap> getLane(int lanenr) {
    return eScores.get(lanenr);
  }

  public ErrorDist getErrorDistribution() {
    for (int lane : eScores.keySet()) {
      Map<Integer, ErrorMap> laneScores = eScores.get(lane);

      for (int cycle : laneScores.keySet()) {
        ErrorMap emap = laneScores.get(cycle);

        for (int tile : emap.getErrorMappings().keySet()) {
          Map<Integer, Double> emetricMap = emap.getErrorMappings().get(tile);

          for (int numErrors : emetricMap.keySet()) {
            double numReads = emetricMap.get(numErrors);

            if (numErrors == -1) { // Error rate
              eDist.setRunDistScore(lane, numReads);
            }
            else { // Number of reads with num errors
              eDist.setRunDistScoreByCycle(cycle, numErrors, numReads);
              eDist.setRunDistScoreByLane(lane, numErrors, numReads);
            }
          }
        }
      }
    }

    return eDist;
  }
}
