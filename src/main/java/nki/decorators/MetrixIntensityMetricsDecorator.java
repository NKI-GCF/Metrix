package nki.decorators;

import net.sf.json.JSONObject;
import nki.objects.IntensityScores;

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
    //iDistAvg = intensityScores.getAvgCorIntDist();
    //iDistCCAvg = intensityScores.getAvgCorIntCCDist();
    return json;
  }
}