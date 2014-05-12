package nki.decorators;

import net.sf.json.JSONObject;
import nki.parsers.illumina.ExtractionMetrics;

/**
 * Decorator to output objects contained within a MetrixContainer to TSV, CSV and JSON
 *
 * @author Rob Davey
 * @date 07/04/14
 * @since version
 */
public class MetrixExtractionMetricsDecorator {
  private ExtractionMetrics extractionMetrics;

  public MetrixExtractionMetricsDecorator(ExtractionMetrics extractionMetrics) {
    this.extractionMetrics = extractionMetrics;
  }

  public JSONObject toJSON() {
    JSONObject json = new JSONObject();

    return json;
  }
}