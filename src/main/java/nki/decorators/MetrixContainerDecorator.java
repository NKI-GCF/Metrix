package nki.decorators;

import org.json.simple.JSONObject;
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

  public JSONObject summaryMetricsToJSON() {
    JSONObject metrixJson = new JSONObject();
    JSONObject summary = new MetrixSummaryDecorator(metrixContainer.getSummary()).toJSON();
    metrixJson.put("summary", summary);
    return metrixJson;
  }

  public JSONObject tileMetricsToJSON() {
    JSONObject metrixJson = new JSONObject();
    JSONObject tileMetrics = new MetrixTileMetricsDecorator(metrixContainer.getTileMetrics(), metrixContainer.getSummary().getReads()).toJSON();
    metrixJson.put("tileMetrics", tileMetrics);
    return metrixJson;
  }

  public JSONObject qualityMetricsToJSON() {
    JSONObject metrixJson = new JSONObject();
    JSONObject qualityMetrics = new MetrixQualityMetricsDecorator(metrixContainer.getQualityMetrics().getQualityScores()).toJSON();
    metrixJson.put("qualityMetrics", qualityMetrics);
    return metrixJson;
  }

  public JSONObject errorMetricsToJSON() {
    JSONObject metrixJson = new JSONObject();
    JSONObject errorMetrics = new MetrixErrorMetricsDecorator(metrixContainer.getErrorMetrics().getErrorScores()).toJSON();
    metrixJson.put("errorMetrics", errorMetrics);
    return metrixJson;
  }

  public JSONObject indexMetricsToJSON() {
    JSONObject metrixJson = new JSONObject();
    JSONObject indexMetrics = new MetrixIndexMetricsDecorator(metrixContainer.getIndexMetrics().getIndices()).toJSON();
    metrixJson.put("indexMetrics", indexMetrics);
    return metrixJson;
  }

  public JSONObject extractionMetricsToJSON() {
    JSONObject metrixJson = new JSONObject();
    JSONObject extractionMetrics = new MetrixExtractionMetricsDecorator(metrixContainer.getExtractionMetrics()).toJSON();
    metrixJson.put("extractionMetrics", extractionMetrics);
    return metrixJson;
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
    JSONObject intensityMetrics = new MetrixIntensityMetricsDecorator(metrixContainer.getCorrectedIntensityMetrics().getIntensityScores()).toJSON();
    metrixJson.put("intensityMetrics", intensityMetrics);

    metrixJson.put("summary", summary);
    metrixJson.put("tileMetrics", tileMetrics);
    metrixJson.put("qualityMetrics", qualityMetrics);
    metrixJson.put("errorMetrics", errorMetrics);
    metrixJson.put("indexMetrics", indexMetrics);
    metrixJson.put("extractionMetrics", extractionMetrics);

    return metrixJson;
  }
}
