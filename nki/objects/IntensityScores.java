// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.objects;

import java.io.*;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

import nki.objects.MutableLong;
import nki.objects.IntensityMap;
import nki.objects.IntensityDist;
import nki.constants.Constants;

public class IntensityScores implements Serializable {

  public static final long serialVersionUID = 42L;
  public int version;
  public int recordLength;
  public String source;
  public Map<Integer, Map<Integer, IntensityMap>> iScores = new HashMap<>();

  public void setVersion(int version) {
    this.version = version;
  }

  public int getVersion() {
    return version;
  }

  public boolean isEmpty() {
    return iScores.isEmpty();
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

  public void setLane(Map<Integer, IntensityMap> content, int lanenr) {
    Map<Integer, IntensityMap> cycleMap = iScores.get(lanenr);

    if (cycleMap == null) {
      iScores.put(lanenr, content);
    }
    else {  // Merge maps and replace existing entries
      Map<Integer, IntensityMap> tmpMap = new HashMap<>(content);
      tmpMap.keySet().removeAll(cycleMap.keySet());
      cycleMap.putAll(content);
      iScores.put(lanenr, cycleMap);
    }
  }

  public Map<Integer, IntensityMap> getLane(int lanenr) {
    return iScores.get(lanenr);
  }

  public IntensityMap getCycle(int lane, int cycle) {
    return (iScores.get(lane)).get(cycle);
  }

  public void setCycle(int lane, int cycle, IntensityMap map) {
    iScores.get(lane).put(cycle, map);
  }

  public IntensityDist getAverageCorrectedIntensityDist() {
    IntensityDist iDistAvg = new IntensityDist();

    // Lane -> CycleMap
    for (int lane : iScores.keySet()) {
      Map<Integer, IntensityMap> laneScores = iScores.get(lane);
      // Cycle -> IntensityMap
      for (int cycle : laneScores.keySet()) {
        IntensityMap qmap = laneScores.get(cycle);
        Map<String, MutableInt> iMapMAvg = qmap.getCycleAverageInt();
        iDistAvg.setIntensity(lane, cycle, iMapMAvg);
      }
    }

    return iDistAvg;
  }

  public IntensityDist getCalledClustersAverageCorrectedIntensityDist() {
    IntensityDist iDistAvgCC = new IntensityDist();

    // Lane -> CycleMap
    for (int lane : iScores.keySet()) {
      Map<Integer, IntensityMap> laneScores = iScores.get(lane);
      // Cycle -> IntensityMap
      for (int cycle : laneScores.keySet()) {
        IntensityMap qmap = laneScores.get(cycle);
        Map<String, MutableInt> iMapMAvgCC = qmap.getCycleAverageCCInt();
        iDistAvgCC.setIntensity(lane, cycle, iMapMAvgCC);
      }
    }

    return iDistAvgCC;
  }
}
