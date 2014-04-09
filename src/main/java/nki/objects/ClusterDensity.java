// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.objects;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;

import nki.objects.Metric;

import java.text.*;
import java.util.Map;

import org.w3c.dom.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

public class ClusterDensity implements Serializable {

  public static final long serialVersionUID = 42L;
  private int tiles = 0;
  public long totalClusters = 0;

  public Map<Integer, Metric> clusterDensity = new HashMap<>();

  public String type = "";

  public void setMetric(int lane, Double metricScore) {
    Metric m;
    if (clusterDensity.get(lane) != null) {
      m = clusterDensity.get(lane);
      m.incrementMetric(metricScore);
    }
    else {
      m = new Metric();
      m.setMetric(metricScore);
    }
    this.clusterDensity.put(lane, m);
  }

  public Metric getMetric(int lane) {
    return this.clusterDensity.get(lane);
  }

  public void incrementTotalClusters(long metric) {
    this.totalClusters += metric;
  }

  public long getTotalClusters() {
    return this.totalClusters;
  }

  public Iterator getClusterDensityIterator() {
    return clusterDensity.entrySet().iterator();
  }

  public Element toXML(Element sumXml, Document xmlDoc) {
    Element distXml;
    if (type.equals("CD")) {
      distXml = xmlDoc.createElement("ClusterDensity");
    }
    else {
      distXml = xmlDoc.createElement("ClusterDensityPF");
    }

    for (int scoreVal : clusterDensity.keySet()) {
      Element score = xmlDoc.createElement("Lane");
      score.setAttribute("Index", scoreVal + "");
      Metric metric = this.getDensity(scoreVal);
      score.setAttribute("Density", metric.getLaneAvg() + "");
      distXml.appendChild(score);

      sumXml.appendChild(distXml);
    }

    return sumXml;
  }

  public Metric getDensity(int lane) {
    return clusterDensity.get(lane);
  }

  private Element createElement(Document doc, String name, String text) {
    Element e = doc.createElement(name);
    if (text == null) {
      text = "";
    }
    e.appendChild(doc.createTextNode(text));

    return e;
  }

  public String toTab() {
    String out = "";
    DecimalFormat df = new DecimalFormat("##");

    for (int lane : clusterDensity.keySet()) {
      Metric metric = this.getDensity(lane);
      out += lane + "\t" + df.format(metric.calcMean() / 1000) + " +/- " + df.format(metric.calcSD() / 1000) + "\n";
    }

    return out;
  }

  public String toTabPrecise() {
    String out = "";
    DecimalFormat df = new DecimalFormat("##");

    for (int lane : clusterDensity.keySet()) {
      Metric metric = this.getDensity(lane);
      out += lane + "\t" + df.format(metric.calcMean()) + " +/- " + df.format(metric.calcSD()) + "\n";
    }

    return out;
  }

  public String toTab(ClusterDensity ocd) {
    String out = "";
    DecimalFormat df = new DecimalFormat("##");
    DecimalFormat dfTwo = new DecimalFormat("##.##");
    if (ocd instanceof ClusterDensity) {
      if (ocd.clusterDensity.size() != this.clusterDensity.size()) {
        return "Incompatible list size.";
      }
      for (int lane : clusterDensity.keySet()) {
        Metric locMetric = this.getDensity(lane);
        Metric extMetric = ocd.getDensity(lane);
        out += lane + "\t" + df.format(locMetric.calcMean() / 1000) + " +/- " + df.format(locMetric.calcSD() / 1000) + "\t" + df.format(extMetric.calcMean() / 1000) + " +/- " + df.format(extMetric.calcSD() / 1000) + "\t" + dfTwo.format(((extMetric.calcMean() / 1000) / (locMetric.calcMean() / 1000)) * 100) + "%\n";

      }

    }
    return out;

  }

  public Map<Integer, Metric> toObj() {
    return clusterDensity;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }


}
