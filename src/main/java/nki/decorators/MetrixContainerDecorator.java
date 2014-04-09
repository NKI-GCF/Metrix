package nki.decorators;

import net.sf.json.JSONObject;
import nki.core.MetrixContainer;
import nki.objects.Summary;

/**
 * Decorator for a MetrixContainer, comprising all JSON outputs of each individual decorator type.
 *
 * @author Rob Davey
 * @date 07/04/14
 * @since version
 */
public class MetrixContainerDecorator {
  private MetrixContainer metrixContainer;

  public MetrixContainerDecorator(MetrixContainer metrixContainer) {
    this.metrixContainer = metrixContainer;
  }

  public JSONObject toJSON() {
    JSONObject metrixJson = new JSONObject();

    JSONObject summary = new MetrixSummaryDecorator(metrixContainer.getSummary()).toJSON();
    JSONObject tileMetrics = new MetrixTileMetricsDecorator(metrixContainer.getTileMetrics(), metrixContainer.getSummary().getReads()).toJSON();
    JSONObject qualityMetrics = new MetrixQualityMetricsDecorator(metrixContainer.getQualityMetrics().getQualityScores()).toJSON();
    JSONObject errorMetrics = new MetrixErrorMetricsDecorator(metrixContainer.getErrorMetrics().getErrorScores()).toJSON();
    JSONObject indexMetrics = new MetrixIndexMetricsDecorator(metrixContainer.getIndexMetrics().getIndices()).toJSON();
    JSONObject extractionMetrics = new MetrixExtractionMetricsDecorator(metrixContainer.getExtractionMetrics()).toJSON();

    //TODO generates muchness output
    //JSONObject intensityMetrics = new MetrixIntensityMetricsDecorator(metrixContainer.getCorrectedIntensityMetrics().getIntensityScores()).toJSON();
    //metrixJson.put("intensityMetrics", intensityMetrics);

    metrixJson.put("summary", summary);
    metrixJson.put("tileMetrics", tileMetrics);
    metrixJson.put("qualityMetrics", qualityMetrics);
    metrixJson.put("errorMetrics", errorMetrics);
    metrixJson.put("indexMetrics", indexMetrics);
    metrixJson.put("extractionMetrics", extractionMetrics);

    return metrixJson;
  }
}
