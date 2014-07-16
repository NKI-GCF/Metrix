// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2014 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.objects;

import java.io.*;
import java.util.Map;
import java.util.HashMap;

import nki.constants.Constants;

public class IntensityMap implements Serializable {

  public static final long serialVersionUID = 42L;

  private Map<Integer, Map<String, Double>> sMap = new HashMap<>();

  public void addMapping(int tilenr, String metricType, Double metricVal) {
    Map<String, Double> iMap;
    if (sMap.containsKey(tilenr)) {
      iMap = sMap.get(tilenr);
    }
    else {
      iMap = new HashMap<>();
    }

    iMap.put(metricType, metricVal);
    sMap.put(tilenr, iMap);
  }

  public Integer getNumberOfTiles() {
    return sMap.size();
  }

  // Calculate the average intensity for this cycle for each metric constant [A, C, G, T]
  public Map<String, MutableInt> getCycleAverageInt() {
    Map<String, MutableInt> avgOverTiles = new HashMap<>();

    avgOverTiles.put(Constants.METRIC_VAR_ACI_A, new MutableInt());
    avgOverTiles.put(Constants.METRIC_VAR_ACI_C, new MutableInt());
    avgOverTiles.put(Constants.METRIC_VAR_ACI_G, new MutableInt());
    avgOverTiles.put(Constants.METRIC_VAR_ACI_T, new MutableInt());

    for (int tile : sMap.keySet()) {
      Map<String, Double> tileMapping = sMap.get(tile);

      Integer iA = tileMapping.get(Constants.METRIC_VAR_ACI_A).intValue();
      Integer iC = tileMapping.get(Constants.METRIC_VAR_ACI_C).intValue();
      Integer iG = tileMapping.get(Constants.METRIC_VAR_ACI_G).intValue();
      Integer iT = tileMapping.get(Constants.METRIC_VAR_ACI_T).intValue();

      avgOverTiles.get(Constants.METRIC_VAR_ACI_A).add(iA);
      avgOverTiles.get(Constants.METRIC_VAR_ACI_C).add(iC);
      avgOverTiles.get(Constants.METRIC_VAR_ACI_G).add(iG);
      avgOverTiles.get(Constants.METRIC_VAR_ACI_T).add(iT);
    }

    avgOverTiles.get(Constants.METRIC_VAR_ACI_A).avg();
    avgOverTiles.get(Constants.METRIC_VAR_ACI_C).avg();
    avgOverTiles.get(Constants.METRIC_VAR_ACI_G).avg();
    avgOverTiles.get(Constants.METRIC_VAR_ACI_T).avg();

    return avgOverTiles;
  }

  // Calculate the average intensity of called clusters for this cycle for each metric constant [A, C, G, T]
  public Map<String, MutableInt> getCycleAverageCCInt() {
    Map<String, MutableInt> avgOverTiles = new HashMap<>();

    avgOverTiles.put(Constants.METRIC_VAR_ACICC_A, new MutableInt());
    avgOverTiles.put(Constants.METRIC_VAR_ACICC_C, new MutableInt());
    avgOverTiles.put(Constants.METRIC_VAR_ACICC_G, new MutableInt());
    avgOverTiles.put(Constants.METRIC_VAR_ACICC_T, new MutableInt());

    for (int tile : sMap.keySet()) {
      Map<String, Double> tileMapping = sMap.get(tile);

      Integer iA = tileMapping.get(Constants.METRIC_VAR_ACICC_A).intValue();
      Integer iC = tileMapping.get(Constants.METRIC_VAR_ACICC_C).intValue();
      Integer iG = tileMapping.get(Constants.METRIC_VAR_ACICC_G).intValue();
      Integer iT = tileMapping.get(Constants.METRIC_VAR_ACICC_T).intValue();

      avgOverTiles.get(Constants.METRIC_VAR_ACICC_A).add(iA);
      avgOverTiles.get(Constants.METRIC_VAR_ACICC_C).add(iC);
      avgOverTiles.get(Constants.METRIC_VAR_ACICC_G).add(iG);
      avgOverTiles.get(Constants.METRIC_VAR_ACICC_T).add(iT);
    }

    avgOverTiles.get(Constants.METRIC_VAR_ACICC_A).avg();
    avgOverTiles.get(Constants.METRIC_VAR_ACICC_C).avg();
    avgOverTiles.get(Constants.METRIC_VAR_ACICC_G).avg();
    avgOverTiles.get(Constants.METRIC_VAR_ACICC_T).avg();

    return avgOverTiles;
  }

    // Calculate the average raw intensity for this cycle for each metric constant [A, C, G, T]
  public Map<String, MutableInt> getCycleAverageRawInt() {
    Map<String, MutableInt> avgOverTiles = new HashMap<>();

    avgOverTiles.put(Constants.METRIC_EX_RAWINT_A, new MutableInt());
    avgOverTiles.put(Constants.METRIC_EX_RAWINT_C, new MutableInt());
    avgOverTiles.put(Constants.METRIC_EX_RAWINT_G, new MutableInt());
    avgOverTiles.put(Constants.METRIC_EX_RAWINT_T, new MutableInt());

    for (int tile : sMap.keySet()) {
      Map<String, Double> tileMapping = sMap.get(tile);

      Integer iA = tileMapping.get(Constants.METRIC_EX_RAWINT_A).intValue();
      Integer iC = tileMapping.get(Constants.METRIC_EX_RAWINT_C).intValue();
      Integer iG = tileMapping.get(Constants.METRIC_EX_RAWINT_G).intValue();
      Integer iT = tileMapping.get(Constants.METRIC_EX_RAWINT_T).intValue();

      avgOverTiles.get(Constants.METRIC_EX_RAWINT_A).add(iA);
      avgOverTiles.get(Constants.METRIC_EX_RAWINT_C).add(iC);
      avgOverTiles.get(Constants.METRIC_EX_RAWINT_G).add(iG);
      avgOverTiles.get(Constants.METRIC_EX_RAWINT_T).add(iT);
    }

    avgOverTiles.get(Constants.METRIC_EX_RAWINT_A).avg();
    avgOverTiles.get(Constants.METRIC_EX_RAWINT_C).avg();
    avgOverTiles.get(Constants.METRIC_EX_RAWINT_G).avg();
    avgOverTiles.get(Constants.METRIC_EX_RAWINT_T).avg();

    return avgOverTiles;
  }
  
  // Return the number of called bases foreach channel [NC, A, C, G, T]
  public void getNumberCalledBases() {
    //public HashMap<String, float> getNumberCalledBases(){

  }

  // Calculate average Signal To Noise Ratio for this cycle
//	public float getAverageSTNRatio(){
  public void getAverageSTNRatio() {

  }
}
