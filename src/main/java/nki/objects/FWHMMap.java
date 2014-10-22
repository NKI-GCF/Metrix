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

public class FWHMMap implements Serializable {

  public static final long serialVersionUID = 42L;

  private final Map<Integer, Map<String, Double>> sMap = new HashMap<>();

  public void addMapping(int tilenr, String metricType, Double metricVal) {
    Map<String, Double> fMap;
    if (sMap.containsKey(tilenr)) {
      fMap = sMap.get(tilenr);
    }
    else {
      fMap = new HashMap<>();
    }

    fMap.put(metricType, metricVal);
    sMap.put(tilenr, fMap);
  }

  public Integer getNumberOfTiles() {
    return sMap.size();
  }

  // Calculate the average FWHM for this cycle for each metric constant [A, C, G, T]
  public Map<String, MutableDouble> getCycleAverageFWHM() {
    Map<String, MutableDouble> avgOverTiles = new HashMap<>();

    avgOverTiles.put(Constants.METRIC_VAR_FWHM_A, new MutableDouble());
    avgOverTiles.put(Constants.METRIC_VAR_FWHM_C, new MutableDouble());
    avgOverTiles.put(Constants.METRIC_VAR_FWHM_G, new MutableDouble());
    avgOverTiles.put(Constants.METRIC_VAR_FWHM_T, new MutableDouble());

    for (int tile : sMap.keySet()) {
      Map<String, Double> tileMapping = sMap.get(tile);

      Double fA = tileMapping.get(Constants.METRIC_VAR_FWHM_A);
      Double fC = tileMapping.get(Constants.METRIC_VAR_FWHM_C);
      Double fG = tileMapping.get(Constants.METRIC_VAR_FWHM_G);
      Double fT = tileMapping.get(Constants.METRIC_VAR_FWHM_T);
      
      avgOverTiles.get(Constants.METRIC_VAR_FWHM_A).add(fA);
      avgOverTiles.get(Constants.METRIC_VAR_FWHM_C).add(fC);
      avgOverTiles.get(Constants.METRIC_VAR_FWHM_G).add(fG);
      avgOverTiles.get(Constants.METRIC_VAR_FWHM_T).add(fT);
    }

    avgOverTiles.get(Constants.METRIC_VAR_FWHM_A).avg();
    avgOverTiles.get(Constants.METRIC_VAR_FWHM_C).avg();
    avgOverTiles.get(Constants.METRIC_VAR_FWHM_G).avg();
    avgOverTiles.get(Constants.METRIC_VAR_FWHM_T).avg();

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
