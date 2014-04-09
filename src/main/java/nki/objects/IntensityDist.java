// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.objects;

import java.io.*;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import org.w3c.dom.*;

public class IntensityDist implements Serializable {

  public static final long serialVersionUID = 42L;

  private final Map<Integer, Map<Integer, Map<String, MutableInt>>> iDist = new HashMap<>();

  public void setIntensity(int lane, int cycle, Map<String, MutableInt> iMapM) {
    Map<Integer, Map<String, MutableInt>> oMap = iDist.get(lane);

    if (oMap == null) {
      Map<Integer, Map<String, MutableInt>> cMap = new HashMap<>();
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

  @SuppressWarnings("unchecked")
  public Element toXML(Element sumXml, Document xmlDoc) {
    Iterator lit = iDist.entrySet().iterator();
    /*
		 * Key   = Lane			- Integer
		 * Value = CycleMap 	- HashMap<Integer, HashMap<String, Object>>
		 */
    while (lit.hasNext()) {
      Element laneEle = xmlDoc.createElement("Lane");
      Map.Entry lanePairs = (Map.Entry) lit.next();
      int lane = (Integer) lanePairs.getKey();
      laneEle.setAttribute("lane", Integer.toString(lane));

      HashMap<Integer, HashMap<String, MutableInt>> cycleContent;
      cycleContent = (HashMap<Integer, HashMap<String, MutableInt>>) lanePairs.getValue();
      // Cycle Iterator
      Iterator cit = (Iterator) cycleContent.entrySet().iterator();

      while (cit.hasNext()) {
        Element cycleEle = xmlDoc.createElement("Cycle");
        Map.Entry cycleEntries = (Map.Entry) cit.next();
        int cycle = (Integer) cycleEntries.getKey();
        cycleEle.setAttribute("num", Integer.toString(cycle));

        // Nested Intensities HashMap
        HashMap<String, MutableInt> cycleInt = (HashMap<String, MutableInt>) cycleEntries.getValue();

        Iterator iit = (Iterator) cycleInt.entrySet().iterator();

        Element intEle = xmlDoc.createElement("Intensities");
        while (iit.hasNext()) {
          Map.Entry intensityPairs = (Map.Entry) iit.next();
          String constName = (String) intensityPairs.getKey();
          MutableInt intValue = (MutableInt) intensityPairs.getValue();

          if (intValue instanceof MutableInt) {
            MutableInt in = (MutableInt) intValue;
            intEle.setAttribute(constName, Integer.toString(in.get()));
          }

          cycleEle.appendChild(intEle);
        }
        laneEle.appendChild(cycleEle);
      }
      sumXml.appendChild(laneEle);
    }

    return sumXml;
  }

  @SuppressWarnings("unchecked")
  public String toTab() {
    String out = "";
    for (Map.Entry lanePairs : iDist.entrySet()) {
      int lane = (Integer) lanePairs.getKey();

      HashMap<Integer, HashMap<String, MutableInt>> cycleContent = (HashMap<Integer, HashMap<String, MutableInt>>) lanePairs.getValue();
      // Cycle Iterator
      Iterator cit = (Iterator) cycleContent.entrySet().iterator();

      while (cit.hasNext()) {
        Map.Entry cycleEntries = (Map.Entry) cit.next();
        int cycle = (Integer) cycleEntries.getKey();

        // Nested Intensities HashMap
        HashMap<String, MutableInt> cycleInt = (HashMap<String, MutableInt>) cycleEntries.getValue();

        Iterator iit = (Iterator) cycleInt.entrySet().iterator();
        out += lane + "\t" + cycle;

        while (iit.hasNext()) {
          Map.Entry intensityPairs = (Map.Entry) iit.next();
          String constName = (String) intensityPairs.getKey();
          MutableInt intValue = (MutableInt) intensityPairs.getValue();

          out += "\t" + constName + ":" + intValue;
        }

        out += "\n";
      }
    }
    return out;
  }
}
