package nki.decorators;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import nki.objects.ErrorCollection;
import nki.objects.ErrorDist;
import nki.util.ArrayUtils;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

/**
 * Decorator to output objects contained within a MetrixContainer to TSV, CSV and JSON
 *
 * @author Rob Davey
 * @date 07/04/14
 * @since version
 */
public class MetrixErrorMetricsDecorator {
  private ErrorCollection errorCollection;

  public MetrixErrorMetricsDecorator(ErrorCollection errorCollection) {
    this.errorCollection = errorCollection;
  }

  public JSONObject toJSON() {
    JSONObject json = new JSONObject();
    ErrorDist eDist = errorCollection.getErrorDistribution();

    DecimalFormat df = new DecimalFormat("0.00");
    JSONArray rates = new JSONArray();
    for (int lane : eDist.getRunErrorDistribution().keySet()) {
      JSONObject lerr = new JSONObject();
      lerr.put("lane", lane);
      lerr.put("meanError", df.format(ArrayUtils.mean(eDist.getRunErrorDistribution().get(lane))));
      lerr.put("errorSD", df.format(ArrayUtils.sd(eDist.getRunErrorDistribution().get(lane))));
      rates.add(lerr);
    }
    json.put("rates", rates);

    JSONArray lanes = new JSONArray();
    for (int lane : eDist.getRunErrorDistributionByLane().keySet()) {
      JSONObject lerr = new JSONObject();
      lerr.put("lane", lane);
      Map<Integer, List<Double>> errorMap = eDist.getRunErrorDistributionByLane().get(lane);
      JSONArray errors = new JSONArray();
      for (int numErrors : errorMap.keySet()) {
        JSONObject err = new JSONObject();
        err.put("num", numErrors);
        err.put("meanError", df.format(ArrayUtils.mean(errorMap.get(numErrors))));
        errors.add(err);
      }
      lerr.put("errors", errors);
      lanes.add(lerr);
    }
    json.put("byLane", lanes);

    /*
    JSONArray cycles = new JSONArray();
    for (int cycle : eDist.getRunErrorDistributionByCycle().keySet()) {
      JSONObject lerr = new JSONObject();
      lerr.put("cycle", cycle);
      Map<Integer, List<Double>> errorMap = eDist.getRunErrorDistributionByCycle().get(cycle);
      JSONArray errors = new JSONArray();
      for (int numErrors : errorMap.keySet()) {
        JSONObject err = new JSONObject();
        err.put("num", numErrors);
        err.put("meanError", Math.floor(ArrayUtils.mean(errorMap.get(numErrors))));
        errors.add(err);
      }
      lerr.put("errors", errors);
      cycles.add(lerr);
    }
    json.put("byCycle", cycles);
    */

    return json;
  }
}