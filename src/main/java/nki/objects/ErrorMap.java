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

public class ErrorMap implements Serializable {
  public static final long serialVersionUID = 42L;

  private Map<Integer, Map<Integer, Double>> eMap = new HashMap<>();

  public void addMetric(int tilenr, int numE, double eVal) {
    Map<Integer, Double> eSubMap;
    if (eMap.containsKey(tilenr)) {
      // Get subMap from hashmap.
      eSubMap = eMap.get(tilenr);
    }
    else {
      // Create new readnum entry and popup late with new hashmap
      eSubMap = new HashMap<>();
    }
    eSubMap.put(numE, eVal);
    eMap.put(tilenr, eSubMap);
  }

  public Integer getNumberOfTiles() {
    return eMap.size();
  }

  public Map<Integer, Map<Integer, Double>> getErrorMappings() {
    return eMap;
  }
}
