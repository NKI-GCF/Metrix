// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.objects;

import java.io.*;
import java.util.*;
import java.text.*;

import nki.objects.MutableLong;

import org.w3c.dom.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

public class QScoreDist implements Serializable {

  public static final long serialVersionUID = 42L;

  // QualityMap - Integer scores
  private Map<Integer, MutableLong> qScoreDist = new HashMap<>();
  private long totalClusters = 0;

  public MutableLong getScore(int qScore) {
    return qScoreDist.get(qScore);
  }

  public void setScore(int qScore, long metric) {
    MutableLong val = qScoreDist.get(qScore);
    if (val == null) {
      val = new MutableLong();
      val.add(metric);
      qScoreDist.put(qScore, val);
    }
    else {
      qScoreDist.get(qScore).add(metric);
    }
    addTotalClusters(metric); // append to total
  }

  public Element toXML(Element sumXml, Document xmlDoc) {
    for (int scoreVal : qScoreDist.keySet()) {
      Element scoreEle = xmlDoc.createElement("QScore");
      scoreEle.setAttribute("score", scoreVal + "");
      MutableLong metric = this.getScore(scoreVal);
      scoreEle.setAttribute("clusters", metric.get() + "");
      sumXml.appendChild(scoreEle);
    }

    return sumXml;
  }

  public String toTab() {
    String out = "";

    for (int score : qScoreDist.keySet()) {
      MutableLong metric = this.getScore(score);
      out += score + "\t" + metric.get() + "\n";
    }

    return out;
  }

  public double aboveQ(int qscore) {
    double aboveClus = 0;
    double totalClus = 0;

    for (int score : qScoreDist.keySet()) {
      MutableLong metric = this.getScore(score);
      if (score >= qscore) {
        aboveClus += (double) metric.get();
      }
      totalClus += (double) metric.get();
    }
    return (aboveClus / totalClus) * 100;
  }

  public Map<Integer, MutableLong> getQualityScoreDist() {
    return qScoreDist;
  }

  public long getTotalClusters() {
    return totalClusters;
  }

  public void addTotalClusters(long metric) {
    this.totalClusters += metric;
  }
}
