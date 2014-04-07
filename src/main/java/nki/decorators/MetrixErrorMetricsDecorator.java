package nki.decorators;

import net.sf.json.JSONObject;
import nki.objects.ErrorCollection;

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

    return json;
  }
}