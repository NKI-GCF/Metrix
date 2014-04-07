package nki.decorators;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import nki.objects.*;
import nki.parsers.illumina.TileMetrics;

import java.text.DecimalFormat;

/**
 * Decorator to output objects contained within a MetrixContainer to TSV, CSV and JSON
 *
 * @author Rob Davey
 * @date 07/04/14
 * @since version
 */
public class MetrixTileMetricsDecorator {
  private TileMetrics tileMetrics;
  private Reads reads;

  private DecimalFormat df = new DecimalFormat("##");
  private DecimalFormat dfTwo = new DecimalFormat("##.##");
  private DecimalFormat phasingDf = new DecimalFormat("#.###");

  public MetrixTileMetricsDecorator(TileMetrics tileMetrics, Reads reads) {
    this.tileMetrics = tileMetrics;
    this.reads = reads;
  }

  public JSONObject toJSON() {
    JSONObject json = new JSONObject();

    ClusterDensity clusterDensity = tileMetrics.getCDmap();
    ClusterDensity clusterDensityPassingFilter = tileMetrics.getCDpfMap();

    JSONArray clusterDensities = new JSONArray();
    for (int lane : clusterDensity.toObj().keySet()) {
      JSONObject cdLane = new JSONObject();
      Metric locMetric = clusterDensity.getDensity(lane);
      Metric extMetric = clusterDensityPassingFilter.getDensity(lane);
      cdLane.put("lane", lane);
      cdLane.put("density", df.format(locMetric.calcMean() / 1000));
      cdLane.put("densitySD", df.format(locMetric.calcSD() / 1000));
      cdLane.put("densityPassingFilter", df.format(extMetric.calcMean() / 1000));
      cdLane.put("densityPassingFilterSD", df.format(extMetric.calcSD() / 1000));
      cdLane.put("densityPercentPassed", dfTwo.format(((extMetric.calcMean() / 1000) / (locMetric.calcMean() / 1000)) * 100));
      cdLane.put("units", "k/mm2");
      clusterDensities.add(cdLane);
    }
    json.put("clusterDensities", clusterDensities);

    PhasingCollection phasingMap = tileMetrics.getPhasingMap();
    PhasingCollection prephasingMap = tileMetrics.getPrephasingMap();

    JSONArray phasingMetrics = new JSONArray();
    for (int lane : phasingMap.toObj().keySet()) {
      JSONObject pLane = new JSONObject();
      pLane.put("lane", lane);

      for (int readNum : phasingMap.toObj().get(lane).keySet()) {
        JSONObject pRead = new JSONObject();
        pRead.put("phasing", phasingDf.format(phasingMap.getPhasing(lane, readNum).getPhasing()));
        pRead.put("prephasing", phasingDf.format(prephasingMap.getPhasing(lane, readNum).getPhasing()));
        if (reads.isIndexedRead(readNum)) {
          pLane.put("Index", pRead);
        }
        else {
          pLane.put(readNum, pRead);
        }
      }

      phasingMetrics.add(pLane);
    }

    json.put("phasingMetrics", phasingMetrics);

    return json;
  }
}