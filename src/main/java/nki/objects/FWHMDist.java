// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2014 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.objects;

import java.io.*;
import java.util.Map;
import java.util.TreeMap;

public class FWHMDist implements Serializable {

  public static final long serialVersionUID = 42L;

  private final Map<Integer, Map<Integer, Map<String, MutableDouble>>> iDist = new TreeMap<>();

  public void setFWHM(int lane, int cycle, Map<String, MutableDouble> fMapM) {
    Map<Integer, Map<String, MutableDouble>> oMap = iDist.get(lane);

    if (oMap == null) {
      Map<Integer, Map<String, MutableDouble>> cMap = new TreeMap<>();
      cMap.put(cycle, fMapM);
      iDist.put(lane, cMap);
    }
    else {
      if (oMap.get(cycle) == null) {
        oMap.put(cycle, fMapM);
      }

      iDist.put(lane, oMap);
    }
  }

  public Map<Integer, Map<Integer, Map<String, MutableDouble>>> getFWHMvalues() {
    return iDist;
  }

}
