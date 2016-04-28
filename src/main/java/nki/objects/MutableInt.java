// Illumina Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2014 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.objects;

import java.io.*;

public class MutableInt implements Serializable {
  public static final long serialVersionUID = 42L;
  public int val = 0;
  public int numTiles = 0;

  public MutableInt() {
  }

  public void add(Integer val) {
    this.val += val;
    incrementTiles();
  }

  public void increment() {
    ++val;
    incrementTiles();
  }

  public int get() {
    return val;
  }

  // public void avg(int numTiles){
  public void avg() {
    this.val = val / numTiles;
  }

  public int getAvg() {
    if (numTiles == 0) {
      return val;
    }
    return val / numTiles;
  }

  public String toString() {
    return val + "";
  }

  public void incrementTiles() {
    numTiles++;
  }

  public int getTiles() {
    return numTiles;
  }

}
