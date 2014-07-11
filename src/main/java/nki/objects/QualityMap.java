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

public class QualityMap implements Serializable {

  public static final long serialVersionUID = 42L;

  // Lane - Integer scores
  private Map<Integer, Map<Integer, Integer>> sMap = new HashMap<>();

  public void addMapping(int tilenr, int qmetric, int qscore) {
    Map<Integer, Integer> qMap;
    if (sMap.containsKey(tilenr)) {
      // Get subMap from hashmap.
      qMap = sMap.get(tilenr);
    }
    else {
      // Create new readnum entry and populate with new hashmap
      qMap = new HashMap<>();
    }

    qMap.put(qmetric, qscore);
    sMap.put(tilenr, qMap);
  }

  public Integer getNumberOfTiles() {
    return sMap.size();
  }

  public Map<Integer, Map<Integer, Integer>> getMappings() {
    return sMap;
  }
}
