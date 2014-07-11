// Metrix - A server / client interface for Illumina Sequencing Phasings.
// Copyright (C) 2014 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.objects;

import java.io.*;
import java.util.*;

import nki.util.ArrayUtils;

public class Phasing implements Serializable {

  public static final long serialVersionUID = 42L;
  private Double phasing = 0.0d;
  private int tiles = 0;
  private List<Double> tileScores = new ArrayList<>();

  public void setPhasing(Double phasingScore) {
    this.phasing = phasingScore;
    this.incrementTiles();
  }

  public Double getPhasing() {
    return phasing;
  }

  public void setTiles(int tileCount) {
    this.tiles = tileCount;
  }

  public int getTiles() {
    return tiles;
  }

  public void incrementPhasing(Double phasingScore) {
    this.phasing += phasingScore;
    this.incrementTiles();
  }

  public void incrementTiles() {
    this.tiles += 1;
  }

  public Double getLaneAvg() {
    return (phasing / tiles);
  }

  public Double calcSum() {
    return ArrayUtils.sum(tileScores);
  }

  public double calcMean() {
    return ArrayUtils.mean(tileScores);
  }

  public double calcMedian() {
    return ArrayUtils.median(tileScores);
  }

  public double calcSD() {
    return ArrayUtils.sd(tileScores);
  }
}
