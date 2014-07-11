// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2014 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.objects;

import java.io.*;
import java.util.HashMap;

import nki.constants.Constants;

public class Threshold implements Serializable {

  public static final long serialVersionUID = 42L;

  private String metricType;        // Constant value of requested metric threshold type
  private Integer min = 0;      // Minimum value
  private Integer max = 0;      // Maximum value
  private Integer exact = 0;      // Exact value

  public void setMin(int min) {
    if (max != null && min > max) {
      this.min = 0;
    }
    else if (this.max == null) {
      this.min = 0;
    }
    else {
      this.min = min;
    }
  }

  public int getMin() {
    return min;
  }

  public void setMax(int max) {
    if (min != null && max < min) {
      this.max = 0;
    }
    else if (this.min == null) {
      this.max = max;
    }
    else {
      this.max = max;
    }
  }

  public int getMax() {
    return max;
  }

  public void setExact(int exact) {
    this.exact = exact;
  }

  public int getExact() {
    return exact;
  }

}
