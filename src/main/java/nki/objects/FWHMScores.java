// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2014 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.objects;

import java.io.*;
import java.util.Map;
import java.util.HashMap;

public class FWHMScores implements Serializable {

  public static final long serialVersionUID = 42L;
  public int version;
  public int recordLength;
  public String source;
  public Map<Integer, Map<Integer, FWHMMap>> fScores = new HashMap<>();

  public void setVersion(int version) {
    this.version = version;
  }

  public int getVersion() {
    return version;
  }

  public boolean isEmpty() {
    return fScores.isEmpty();
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

  public void setLane(Map<Integer, FWHMMap> content, int lanenr) {
    Map<Integer, FWHMMap> cycleMap = fScores.get(lanenr);

    if (cycleMap == null) {
      fScores.put(lanenr, content);
    }
    else {  // Merge maps and replace existing entries
      Map<Integer, FWHMMap> tmpMap = new HashMap<>(content);
      tmpMap.keySet().removeAll(cycleMap.keySet());
      cycleMap.putAll(content);
      fScores.put(lanenr, cycleMap);
    }
  }

  public Map<Integer, FWHMMap> getLane(int lanenr) {
    return fScores.get(lanenr);
  }

  public FWHMMap getCycle(int lane, int cycle) {
    return (fScores.get(lane)).get(cycle);
  }

  public void setCycle(int lane, int cycle, FWHMMap map) {
    fScores.get(lane).put(cycle, map);
  }

  public FWHMDist getAverageFWHMDist() {
    FWHMDist fDistAvg = new FWHMDist();

    // Lane -> CycleMap
    for (int lane : fScores.keySet()) {
      Map<Integer, FWHMMap> laneScores = fScores.get(lane);
      // Cycle -> IntensityMap
      for (int cycle : laneScores.keySet()) {
        FWHMMap fmap = laneScores.get(cycle);
        Map<String, MutableDouble> fMapAvg = fmap.getCycleAverageFWHM();
        fDistAvg.setFWHM(lane, cycle, fMapAvg);
      }
    }

    return fDistAvg;
  }
}
