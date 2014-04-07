package nki.decorators;

import net.sf.json.JSONObject;
import nki.objects.Indices;
import nki.objects.SampleInfo;
import java.util.Map;

/**
 * Decorator to output objects contained within a MetrixContainer to TSV, CSV and JSON
 *
 * @author Rob Davey
 * @date 07/04/14
 * @since version
 */
public class MetrixIndexMetricsDecorator {
  private Indices indices;

  public MetrixIndexMetricsDecorator(Indices indices) {
    this.indices = indices;
  }

  public JSONObject toJSON() {
    JSONObject json = new JSONObject();
    Map<String, Map<String, SampleInfo>> indexMap = indices.getIndices();

    for (String projectName : indexMap.keySet()) {
      Map<String, SampleInfo> samples = indexMap.get(projectName);

      for (String sampleName : samples.keySet()) {

      }
    }
    return json;
  }
}