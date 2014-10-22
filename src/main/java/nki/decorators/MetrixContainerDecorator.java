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
  private boolean remote = false;
          
  public MetrixContainerDecorator(MetrixContainer metrixContainer) {
    this.metrixContainer = metrixContainer;
  }

  public MetrixContainerDecorator(MetrixContainer metrixContainer, boolean isRemote) {
    this.metrixContainer = metrixContainer;
    this.remote = isRemote;
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
    JSONObject summary;
    JSONObject tileMetrics;
    JSONObject qualityMetrics;
    JSONObject errorMetrics;
    JSONObject indexMetrics;
    JSONObject extractionMetrics;
    JSONObject intensityMetrics;
    
    Summary s = metrixContainer.getSummary();
    
    if(this.remote){
        summary = new MetrixSummaryDecorator(s).toJSON();
        tileMetrics = new MetrixTileMetricsDecorator(s.getClusterDensity(), s.getClusterDensityPF(), s.getPhasingMap(), s.getPrephasingMap(), s.getReads()).toJSON();
        qualityMetrics = new MetrixQualityMetricsDecorator(s.getQScoreDist(), s.getQScoreDistByLane(), s.getQScoreDistByCycle()).toJSON();
        errorMetrics = new MetrixErrorMetricsDecorator(s.getErrorDist()).toJSON();
        indexMetrics = new MetrixIndexMetricsDecorator(s.getSampleInfo()).toJSON();
        extractionMetrics = new MetrixExtractionMetricsDecorator(s.getIntensityDistRaw(), s.getFWHMDist()).toJSON();
        //TODO generates muchness output
        intensityMetrics = new MetrixIntensityMetricsDecorator(s.getIntensityDistAvg(), s.getIntensityDistCCAvg()).toJSON();
    }else{
        summary = new MetrixSummaryDecorator(metrixContainer.getSummary()).toJSON();
        tileMetrics = new MetrixTileMetricsDecorator(metrixContainer.getTileMetrics(), metrixContainer.getSummary().getReads()).toJSON();
        qualityMetrics = new MetrixQualityMetricsDecorator(metrixContainer.getQualityMetrics().getQualityScores()).toJSON();
        errorMetrics = new MetrixErrorMetricsDecorator(metrixContainer.getErrorMetrics().getErrorScores()).toJSON();
        indexMetrics = new MetrixIndexMetricsDecorator(metrixContainer.getIndexMetrics().getIndices()).toJSON();
        extractionMetrics = new MetrixExtractionMetricsDecorator(metrixContainer.getExtractionMetrics()).toJSON();

        //TODO generates muchness output
        intensityMetrics = new MetrixIntensityMetricsDecorator(metrixContainer.getCorrectedIntensityMetrics().getIntensityScores()).toJSON();
    }

    metrixJson.put("summary", summary);
    metrixJson.put("tileMetrics", tileMetrics);
    metrixJson.put("qualityMetrics", qualityMetrics);
    metrixJson.put("errorMetrics", errorMetrics);
    metrixJson.put("indexMetrics", indexMetrics);
    metrixJson.put("extractionMetrics", extractionMetrics);
    metrixJson.put("intensityMetrics", intensityMetrics);
    
    return metrixJson;
  }
}
