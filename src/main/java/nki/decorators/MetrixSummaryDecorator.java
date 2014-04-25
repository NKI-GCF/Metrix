package nki.decorators;

import net.sf.json.JSONObject;
import nki.objects.Summary;

/**
 * Decorator to output objects contained within a MetrixContainer to TSV, CSV and JSON
 *
 * @author Rob Davey
 * @date 07/04/14
 * @since version
 */
public class MetrixSummaryDecorator {
  private Summary summary;

  public MetrixSummaryDecorator(Summary summary) {
    this.summary = summary;
  }

  public JSONObject toJSON() {
    JSONObject json = new JSONObject();

    json.put("runType", summary.getRunType());
    json.put("totalCycles", summary.getTotalCycles());
    json.put("currentCycle", summary.getCurrentCycle());

    if (summary.getReads() != null) {
      json.put("demultiplexIndex", summary.getReads().getDemultiplexIndex());
    }
    else {
      json.put("demultiplexIndex", "N/A");
    }

    json.put("flowcellId", summary.getFlowcellID());
    json.put("numLanes", summary.getLaneCount());

    return json;
  }
}