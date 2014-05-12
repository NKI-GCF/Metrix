// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.objects;

import java.io.*;
import java.util.*;

import nki.util.ArrayUtils;

public class Metric implements Serializable {

  public static final long serialVersionUID = 42L;
  private Double metric = 0.0d;
  private int tiles = 0;
  private List<Double> tileScores = new ArrayList<>();

  public void setMetric(Double metricScore) {
    this.metric = metricScore;
    this.tileScores.add(metricScore);
    this.incrementTiles();
  }

  // Total for whole lane
  public Double getMetric() {
    return metric;
  }

  public void setTiles(int tileCount) {
    this.tiles = tileCount;
  }

  public int getTiles() {
    return tiles;
  }

  public List<Double> getTileScores() {
    return tileScores;
  }

  public void incrementMetric(Double metricScore) {
    this.metric += metricScore;
    this.tileScores.add(metricScore);
    this.incrementTiles();
  }

  public void incrementTiles() {
    this.tiles += 1;
  }

  // ClusterDensity averaged (metric value / #tiles) .
  public Double getLaneAvg() {
    return (metric / tiles);
  }

  public Double calcSum() {
    return ArrayUtils.sum(tileScores);
  }

  public double calcQ1() {
    return ArrayUtils.quartile(tileScores, 25);
  }

  public double calcQ3() {
    return ArrayUtils.quartile(tileScores, 75);
  }

  public double calcMean() {
    return ArrayUtils.mean(tileScores);
  }

  public double calcMedian() {
    return ArrayUtils.median(tileScores);
  }

  public double calcMax() {
    return ArrayUtils.max(tileScores);
  }

  public double calcMin() {
    return ArrayUtils.min(tileScores);
  }

  public double calcSD() {
    return ArrayUtils.sd(tileScores);
  }
}
