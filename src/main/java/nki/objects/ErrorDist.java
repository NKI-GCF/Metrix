// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2014 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.objects;

import java.io.*;
import java.util.*;
import java.text.*;

import nki.util.ArrayUtils;

import org.w3c.dom.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

public class ErrorDist implements Serializable {

  public static final long serialVersionUID = 42L;

  // Num Errors - Error rate
  private Map<Integer, List<Double>> eScoreDistRun = new HashMap<>();

  // Lane - Num Errors - Num Reads with Error
  private Map<Integer, Map<Integer, List<Double>>> eScoreDistLane = new HashMap<>();

  // Cycle - Num Errors - Num Reads with Error
  private Map<Integer, Map<Integer, List<Double>>> eScoreDistCycle = new HashMap<>();

  public void setRunDistScore(int lane, double score) {
    if (eScoreDistRun.containsKey(lane)) {
      (eScoreDistRun.get(lane)).add(score);
    }
    else {
      List<Double> scores = new ArrayList<>();
      scores.add(score);
      eScoreDistRun.put(lane, scores);
    }
  }

  public Map<Integer, List<Double>> getRunErrorDistribution() {
    return eScoreDistRun;
  }

  public void setRunDistScoreByLane(int lane, int numErrors, double score) {
    if (eScoreDistLane.containsKey(lane)) {
      if ((eScoreDistLane.get(lane)).containsKey(numErrors)) {
        ((eScoreDistLane.get(lane)).get(numErrors)).add(score);
      }
    }
    else {
      List<Double> l = new ArrayList<>();
      l.add(score);
      Map<Integer, List<Double>> cycleM = new HashMap<>();
      cycleM.put(numErrors, l);
      eScoreDistLane.put(lane, cycleM);
    }
  }

  public Map<Integer, Map<Integer, List<Double>>> getRunErrorDistributionByLane() {
    return eScoreDistLane;
  }

  public void setRunDistScoreByCycle(int cycle, int numErrors, double score) {
    if (eScoreDistCycle.containsKey(cycle)) {

      /*
       * if((eScoreDistCycle.get(cycle)).containsKey(numErrors)){
       * ((eScoreDistCycle.get(cycle)).get(numErrors)).add(score); }else{
       * HashMap<Integer, List<Float>> eMap = eScoreDistCycle.get(cycle);
       * List<Float> l = new ArrayList<Float>(); l.add(score);
       * eMap.put(numErrors, l); eScoreDistCycle.put(cycle, eMap); }
       */
      Map<Integer, List<Double>> eMap = eScoreDistCycle.get(cycle);

      List<Double> l;
      if ((eScoreDistCycle.get(cycle)).containsKey(numErrors)) {
        l = (eScoreDistCycle.get(cycle)).get(numErrors);
      }
      else {
        l = new ArrayList<>();
      }
      l.add(score);
      eMap.put(numErrors, l);
      eScoreDistCycle.put(cycle, eMap);
    }
    else {
      List<Double> l = new ArrayList<>();
      l.add(score);
      Map<Integer, List<Double>> cycleM = new HashMap<>();
      cycleM.put(numErrors, l);
      eScoreDistCycle.put(cycle, cycleM);
    }
  }

  public Map<Integer, Map<Integer, List<Double>>> getRunErrorDistributionByCycle() {
    return eScoreDistCycle;
  }

  public String toTab(String source) {
    String out = "";
    DecimalFormat df = new DecimalFormat("0.00");
    // Avg error rate per lane
    if (source.equals("rate")) {
      for (int lane : eScoreDistRun.keySet()) {
        out += lane + "\t" + df.format(ArrayUtils.mean(eScoreDistRun.get(lane))) + "\t(+/- " + df.format(ArrayUtils.sd(eScoreDistRun.get(lane))) + ")\n";
      }
    }
    else if (source.equals("lane")) {
      for (int lane : eScoreDistLane.keySet()) {
        out += lane;
        Map<Integer, List<Double>> errorMap = eScoreDistLane.get(lane);
        for (int numErrors : errorMap.keySet()) {
          out += "\t" + ArrayUtils.mean(errorMap.get(numErrors));
        }
        out += "\n";
      }
    }
    else if (source.equals("cycle")) {
      for (int cycle : eScoreDistCycle.keySet()) {
        out += cycle;
        Map<Integer, List<Double>> errorMap = eScoreDistCycle.get(cycle);
        for (int numErrors : errorMap.keySet()) {
          out += "\t" + Math.floor(ArrayUtils.mean(errorMap.get(numErrors)));
        }
        out += "\n";
      }
    }
    return out;
  }
}
