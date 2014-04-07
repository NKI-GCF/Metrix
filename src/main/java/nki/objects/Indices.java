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

  public void setIndex(String projName, String sampName, String idx, int numClusters, int laneNr, int readNr) {
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

    indices.put(projName, project);
  }

  private SampleInfo setSample(int readNr, int laneNr, int numClusters, String idx) {
    SampleInfo si = new SampleInfo();
    si.setReadNum(readNr);
    si.setNumClusters(numClusters);
    si.setLaneNum(laneNr);
    si.setIndexBarcode(idx);

    addTotalClusters(numClusters);

    return si;
  }

  public long getTotalClusters() {
    return totalClusters;
  }

  public void addTotalClusters(long metric) {
    this.totalClusters += metric;
  }

  @SuppressWarnings("unchecked")
  public Element toXML(Element sumXml, Document xmlDoc) {
    Iterator pit = indices.entrySet().iterator();

    while (pit.hasNext()) {
      Map.Entry projects = (Map.Entry) pit.next();
      String project = (String) projects.getKey();

      Element projEle = xmlDoc.createElement("Project");
      projEle.setAttribute("name", project);

      HashMap<String, HashMap<String, Object>> samples = (HashMap<String, HashMap<String, Object>>) projects.getValue();
      Iterator sit = samples.entrySet().iterator();

      while (sit.hasNext()) {
        Map.Entry sample = (Map.Entry) sit.next();
        String sampleName = (String) sample.getKey();

        Element sampleEle = xmlDoc.createElement("Sample");
        sampleEle.setAttribute("name", sampleName);

        HashMap<String, Object> sampleProp = (HashMap<String, Object>) sample.getValue();
        Iterator spit = sampleProp.entrySet().iterator();

        while (spit.hasNext()) {
          Map.Entry prop = (Map.Entry) spit.next();

          // Num Clusters
          if (prop instanceof MutableLong) {
            MutableLong ml = (MutableLong) prop.getValue();
            sampleEle.setAttribute(prop.getKey().toString(), ml.toString());
          } // Other
          else {
            sampleEle.setAttribute(prop.getKey().toString(), prop.getValue().toString());
          }
        }
        projEle.appendChild(sampleEle);
      }
      sumXml.appendChild(projEle);
    }
    return sumXml;
  }

  @SuppressWarnings("unchecked")
  public String toTab() {
    String out = "";

    Iterator pit = indices.entrySet().iterator();

    while (pit.hasNext()) {
      Map.Entry projects = (Map.Entry) pit.next();
      String project = (String) projects.getKey();

      HashMap<String, HashMap<String, Object>> samples = (HashMap<String, HashMap<String, Object>>) projects.getValue();
      Iterator sit = samples.entrySet().iterator();

      while (sit.hasNext()) {
        Map.Entry sample = (Map.Entry) sit.next();
        String sampleName = (String) sample.getKey();

        out += project + "\t" + sampleName;

        HashMap<String, Object> sampleProp = (HashMap<String, Object>) sample.getValue();
        Iterator spit = sampleProp.entrySet().iterator();

        while (spit.hasNext()) {
          Map.Entry prop = (Map.Entry) spit.next();

          if (prop instanceof MutableLong) {
            MutableLong ml = (MutableLong) prop.getValue();
            out += "\t" + prop.getKey() + ":" + ml.toString();
          }
          else {
            out += "\t" + prop.getKey() + ":" + prop.getValue().toString();
          }
        }

        out += "\n";

      }

    }
    return out;
  }

}
