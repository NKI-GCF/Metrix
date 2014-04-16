package nki.decorators;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import nki.constants.Constants;
import nki.objects.IntensityDist;
import nki.objects.IntensityScores;
import nki.objects.MutableInt;

import java.util.Map;

/**
 * Decorator to output objects contained within a MetrixContainer to TSV, CSV and JSON
 *
 * @author Rob Davey
 * @date 07/04/14
 * @since version
 */
public class MetrixIntensityMetricsDecorator {
  private IntensityScores intensityScores;

  public MetrixIntensityMetricsDecorator(IntensityScores intensityScores) {
    this.intensityScores = intensityScores;
  }

  public JSONObject toJSON() {
    JSONObject json = new JSONObject();

    IntensityDist iDistAvg = intensityScores.getAverageCorrectedIntensityDist();

    JSONArray averages = new JSONArray();
    for (int lane : iDistAvg.getIntensities().keySet()) {
      JSONObject l = new JSONObject();
      Map<Integer, Map<String, MutableInt>> cycleContent = iDistAvg.getIntensities().get(lane);

      JSONArray cyclesA = new JSONArray();
      JSONArray cyclesC = new JSONArray();
      JSONArray cyclesT = new JSONArray();
      JSONArray cyclesG = new JSONArray();

      for (int cycle : cycleContent.keySet()) {
        Map<String, MutableInt> cycleIntensities = cycleContent.get(cycle);
        for (String intensity : cycleIntensities.keySet()) {
          if (intensity.equals(Constants.METRIC_VAR_ACI_A)) {
            cyclesA.add(cycleIntensities.get(intensity).get());
          }
          if (intensity.equals(Constants.METRIC_VAR_ACI_C)) {
            cyclesC.add(cycleIntensities.get(intensity).get());
          }
          if (intensity.equals(Constants.METRIC_VAR_ACI_T)) {
            cyclesT.add(cycleIntensities.get(intensity).get());
          }
          if (intensity.equals(Constants.METRIC_VAR_ACI_G)) {
            cyclesG.add(cycleIntensities.get(intensity).get());
          }
        }
      }

      l.put("lane", lane);
      l.put("intA", cyclesA);
      l.put("intC", cyclesC);
      l.put("intT", cyclesT);
      l.put("intG", cyclesG);
      averages.add(l);
    }

    json.put("averages", averages);

    /*
    IntensityDist iDistCCAvg = intensityScores.getCalledClustersAverageCorrectedIntensityDist();

    JSONArray cclanes = new JSONArray();
    for (int lane : iDistCCAvg.getIntensities().keySet()) {
      JSONObject l = new JSONObject();
      Map<Integer, Map<String, MutableInt>> cycleContent = iDistCCAvg.getIntensities().get(lane);

      JSONArray cyclesA = new JSONArray();
      JSONArray cyclesC = new JSONArray();
      JSONArray cyclesT = new JSONArray();
      JSONArray cyclesG = new JSONArray();

      for (int cycle : cycleContent.keySet()) {
        Map<String, MutableInt> cycleIntensities = cycleContent.get(cycle);
        for (String intensity : cycleIntensities.keySet()) {
          if (intensity.equals(Constants.METRIC_VAR_ACICC_A)) {
            cyclesA.add(cycleIntensities.get(intensity).get());
          }
          if (intensity.equals(Constants.METRIC_VAR_ACICC_C)) {
            cyclesC.add(cycleIntensities.get(intensity).get());
          }
          if (intensity.equals(Constants.METRIC_VAR_ACICC_T)) {
            cyclesT.add(cycleIntensities.get(intensity).get());
          }
          if (intensity.equals(Constants.METRIC_VAR_ACICC_G)) {
            cyclesG.add(cycleIntensities.get(intensity).get());
          }
        }
      }

      l.put("lane", lane);
      l.put("intA", cyclesA);
      l.put("intC", cyclesC);
      l.put("intT", cyclesT);
      l.put("intG", cyclesG);
      cclanes.add(l);
    }

    json.put("calledClusterAverages", cclanes);
    */

    return json;
  }
}