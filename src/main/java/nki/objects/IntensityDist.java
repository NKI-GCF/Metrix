// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2014 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.objects;

import java.io.*;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;

import org.w3c.dom.*;

public class IntensityDist implements Serializable {

  public static final long serialVersionUID = 42L;

  private final Map<Integer, Map<Integer, Map<String, MutableInt>>> iDist = new TreeMap<>();

  public void setIntensity(int lane, int cycle, Map<String, MutableInt> iMapM) {
    Map<Integer, Map<String, MutableInt>> oMap = iDist.get(lane);

    if (oMap == null) {
      Map<Integer, Map<String, MutableInt>> cMap = new TreeMap<>();
      cMap.put(cycle, iMapM);
      iDist.put(lane, cMap);
    }
    else {
      if (oMap.get(cycle) == null) {
        oMap.put(cycle, iMapM);
      }

      iDist.put(lane, oMap);
    }
  }

  public Map<Integer, Map<Integer, Map<String, MutableInt>>> getIntensities() {
    return iDist;
  }

}
