// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.objects;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.w3c.dom.*;

public class Indices implements Serializable {

  public static final long serialVersionUID = 42L;

  // Project - Sample - Index/Lane/Clusters
  private Map<String, Map<String, SampleInfo>> indices = new HashMap<>();
  private long totalClusters = 0;

  public Map<String, Map<String, SampleInfo>> getIndices() {
    return indices;
  }

  public void setIndex(String projName, String sampName, String idx, long numClusters, int laneNr, int readNr) {
    Map<String, SampleInfo> project = indices.get(projName);
    SampleInfo sampleMap;

    if (project == null) {
      project = new HashMap<>();
      sampleMap = setSample(readNr, laneNr, numClusters, idx);
      project.put(sampName, sampleMap);
    }
    else {
      sampleMap = project.get(sampName);

      if (sampleMap == null) {
        sampleMap = setSample(readNr, laneNr, numClusters, idx);
        project.put(sampName, sampleMap);
      }
      else {  // Update num clusters.
        sampleMap.setNumClusters(sampleMap.getNumClusters() + numClusters);
      }

      project.put(sampName, sampleMap);
    }

    addTotalClusters(numClusters);

    indices.put(projName, project);
  }

  private SampleInfo setSample(int readNr, int laneNr, long numClusters, String idx) {
    SampleInfo si = new SampleInfo();
    si.setReadNum(readNr);
    si.setNumClusters(numClusters);
    si.setLaneNum(laneNr);
    si.setIndexBarcode(idx);
    return si;
  }

  public long getTotalClusters() {
    return totalClusters;
  }

  public void addTotalClusters(long metric) {
    this.totalClusters += metric;
  }

  public Element toXML(Element sumXml, Document xmlDoc) {
    for (String projectName : indices.keySet()) {
      Element projEle = xmlDoc.createElement("Project");
      projEle.setAttribute("name", projectName);

      Map<String, SampleInfo> samples = indices.get(projectName);
      for (String sampleName : samples.keySet()) {
        Element sampleEle = xmlDoc.createElement("Sample");
        sampleEle.setAttribute("name", sampleName);

        SampleInfo si = samples.get(sampleName);
        sampleEle.setAttribute("lane", String.valueOf(si.getLaneNum()));
        sampleEle.setAttribute("read", String.valueOf(si.getReadNum()));
        sampleEle.setAttribute("clusters", String.valueOf(si.getNumClusters()));
        sampleEle.setAttribute("index", si.getIndexBarcode());
        projEle.appendChild(sampleEle);
      }
      sumXml.appendChild(projEle);
    }
    return sumXml;
  }

  public String toTab() {
    StringBuilder out = new StringBuilder();

    for (String projectName : indices.keySet()) {
      Map<String, SampleInfo> samples = indices.get(projectName);
      for (String sampleName : samples.keySet()) {
        SampleInfo si = samples.get(sampleName);
        out.append(projectName + "\t" + sampleName);
        out.append("\t lane :" + si.getLaneNum() + "\n");
        out.append("\t read :" + si.getReadNum() + "\n");
        out.append("\t clusters :" + si.getNumClusters() + "\n");
        out.append("\t index :" + si.getIndexBarcode() + "\n\n");
      }
    }
    return out.toString();
  }

}
