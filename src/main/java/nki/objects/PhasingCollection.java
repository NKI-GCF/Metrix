// Metrix - A server / client interface for Illumina Sequencing Phasings.
// Copyright (C) 2014 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.objects;

import java.io.*;
import java.text.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import nki.objects.Phasing;
import nki.objects.Reads;

import org.w3c.dom.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

public class PhasingCollection implements Serializable {

  public static final long serialVersionUID = 42L;

  // Lane -> Read Num (1, 2, 3 or 4) -> Phasing Map for read.
  public Map<Integer, Map<Integer, Phasing>> phasingPerLane = new HashMap<>();

  public String type = "";

  public void setPhasing(int lane, int readNum, Double phasingScore) {
    Map<Integer, Phasing> lanePhaseMap;

    Phasing m;
    lanePhaseMap = phasingPerLane.get(lane);

    if (lanePhaseMap != null) {
      m = lanePhaseMap.get(readNum);
      if (m == null) {
        m = new Phasing();
      }
      m.incrementPhasing(phasingScore);
    }
    else {
      lanePhaseMap = new HashMap<>();
      m = new Phasing();
      m.incrementPhasing(phasingScore);
    }

    lanePhaseMap.put(readNum, m);
    phasingPerLane.put(lane, lanePhaseMap);

  }

  public Phasing getPhasing(int lane, int readNum) {
    return this.phasingPerLane.get(lane).get(readNum);
  }

  public Phasing getPhasing(int lane, int readNum, PhasingCollection pc) {
    return pc.phasingPerLane.get(lane).get(readNum);
  }

  public Element toXML(Element sumXml, Document xmlDoc) {
    Element distXml;
    if (type.equals("PH")) {
      distXml = xmlDoc.createElement("Phasing");
    }
    else {// or PREPH
      distXml = xmlDoc.createElement("Prephasing");
    }

    for (int lane : phasingPerLane.keySet()) {  // Lane
      Element laneEle = xmlDoc.createElement("Lane");
      laneEle.setAttribute("Index", lane + "");

      for (int readNum : phasingPerLane.get(lane).keySet()) {
        Element readEle = xmlDoc.createElement("Read");
        readEle.setAttribute("Read", readNum + "");
        readEle.setAttribute("PhasingScore", getPhasing(lane, readNum).getPhasing().toString());
        laneEle.appendChild(readEle);
      }
      distXml.appendChild(laneEle);
    }

    sumXml.appendChild(distXml);
    return sumXml;
  }

  public String toTab() {
    String out = "";
    DecimalFormat df = new DecimalFormat("#.###");

    for (int lane : phasingPerLane.keySet()) {  // Lane
      for (int readNum : phasingPerLane.get(lane).keySet()) {
        out += lane + "\t" + readNum + "\t" + df.format(getPhasing(lane, readNum).getPhasing()) + "\n";
      }
    }

    return out;
  }

  public String toTab(PhasingCollection ext, Reads rds) {
    String out = "";
    DecimalFormat df = new DecimalFormat("#.###");

    // Write merged prephasing / phasing collection
    if (ext != null) {
      if (phasingPerLane.size() != ext.phasingPerLane.size()) {
        return "Incompatible list size.";
      }
      for (int lane : phasingPerLane.keySet()) {  // Lane
        for (int readNum : phasingPerLane.get(lane).keySet()) {
          String idx = "";
          if (rds.isIndexedRead(readNum)) {
            idx = "Index";
          }
          else {
            idx = Integer.toString(readNum);
          }
          Phasing extPhasing = getPhasing(lane, readNum, ext);
          out += lane + "\t" + idx + "\t" + df.format(getPhasing(lane, readNum).getPhasing()) + " / " + df.format(extPhasing.getPhasing()) + "\n";
        }
      }
    }
    else {
      return this.toTab();
    }
    return out;
  }

  public Map<Integer, Map<Integer, Phasing>> toObj() {
    return phasingPerLane;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }
}
