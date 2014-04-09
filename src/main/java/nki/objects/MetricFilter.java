// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.objects;

import java.io.*;
import java.util.*;

import nki.constants.Constants;

public class MetricFilter implements Serializable {
  public static final long serialVersionUID = 42L;
  private List<String> runIds = new ArrayList<>();
  // Thresholds:
  // Key: Constant value of requested metric threshold type
  // Value: Threshold object setting boundaries.

  private Map<String, Threshold> thresholds = new HashMap<>();

  public void appendRunId(String runId) {
    runIds.add(runId);
  }

  public List<String> getRunIds() {
    return this.runIds;
  }

  public void setRunIds(List<String> runIds) {
    this.runIds = runIds;
  }

  public void setThreshold(String metricType, Threshold threshold) {
    thresholds.put(metricType, threshold);
  }

  public Threshold getThreshold(String metricType) {
    return thresholds.get(metricType);
  }

  public Iterator getThresholdIterator() {
    return thresholds.entrySet().iterator();
  }

  public boolean checkType(String metricType) {
    return Arrays.asList(Constants.METRIC_TYPE_REQUEST).contains(metricType);
  }
}
