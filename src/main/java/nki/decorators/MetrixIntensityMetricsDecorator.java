package nki.decorators;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
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
    IntensityDist iDistCCAvg = intensityScores.getCalledClustersAverageCorrectedIntensityDist();

    JSONArray averages = new JSONArray();
    for (int lane : iDistAvg.getIntensities().keySet()) {
      JSONObject l = new JSONObject();
      Map<Integer, Map<String, MutableInt>> cycleContent = iDistAvg.getIntensities().get(lane);

      JSONArray cycles = new JSONArray();
      for (int cycle : cycleContent.keySet()) {
        JSONObject cyc = new JSONObject();
        cyc.put("cycle", cycle);
        Map<String, MutableInt> cycleIntensities = cycleContent.get(cycle);
        for (String intensity : cycleIntensities.keySet()) {
          cyc.put(intensity, cycleIntensities.get(intensity));
        }
        cycles.add(cyc);
      }

      l.put("lane", lane);
      l.put("cycles", cycles);
      averages.add(l);
    }

    JSONArray cclanes = new JSONArray();
    for (int lane : iDistCCAvg.getIntensities().keySet()) {
      JSONObject l = new JSONObject();
      Map<Integer, Map<String, MutableInt>> cycleContent = iDistCCAvg.getIntensities().get(lane);

      JSONArray cycles = new JSONArray();
      for (int cycle : cycleContent.keySet()) {
        JSONObject cyc = new JSONObject();
        cyc.put("cycle", cycle);
        Map<String, MutableInt> cycleIntensities = cycleContent.get(cycle);
        for (String intensity : cycleIntensities.keySet()) {
          cyc.put(intensity, cycleIntensities.get(intensity));
        }
        cycles.add(cyc);
      }

      l.put("lane", lane);
      l.put("cycles", cycles);
      cclanes.add(l);
    }

    json.put("averages", averages);
    json.put("calledClusterAverages", cclanes);

   return json;
  }
}