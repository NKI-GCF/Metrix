package nki.decorators;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import nki.objects.*;
import nki.parsers.illumina.TileMetrics;

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

  private DecimalFormat df = new DecimalFormat("##", new DecimalFormatSymbols(Locale.US));
  private DecimalFormat dfTwo = new DecimalFormat("##.##", new DecimalFormatSymbols(Locale.US));
  private DecimalFormat phasingDf = new DecimalFormat("#.###", new DecimalFormatSymbols(Locale.US));
  private ClusterDensity clusterDensity;
  private ClusterDensity clusterDensityPassingFilter;
  private PhasingCollection phasingMap;
  private PhasingCollection prephasingMap;
  
  public MetrixTileMetricsDecorator(TileMetrics tileMetrics, Reads reads) {
    this.tileMetrics = tileMetrics;
    this.reads = reads;
  }

  public MetrixTileMetricsDecorator(ClusterDensity clusterDensity, 
                                    ClusterDensity clusterDensityPassingFilter, 
                                    PhasingCollection phasingMap,
                                    PhasingCollection prephasingMap,
                                    Reads reads){
      this.clusterDensity = clusterDensity;
      this.clusterDensityPassingFilter = clusterDensityPassingFilter;
      this.phasingMap = phasingMap;
      this.prephasingMap = prephasingMap;
      this.reads = reads;
  }
  
  public JSONObject toJSON() {
    JSONObject json = new JSONObject();

    if(clusterDensity == null || clusterDensityPassingFilter == null){
        clusterDensity = tileMetrics.getCDmap();
        clusterDensityPassingFilter = tileMetrics.getCDpfMap();
    }

    JSONArray clusterDensities = new JSONArray();
    for (int lane : clusterDensity.toObj().keySet()) {
      JSONObject cdLane = new JSONObject();
      Metric locMetric = clusterDensity.getDensity(lane);
      Metric extMetric = clusterDensityPassingFilter.getDensity(lane);
      if (locMetric != null && extMetric != null) {
        cdLane.put("lane", lane);
        cdLane.put("density", Double.valueOf(df.format(locMetric.calcMedian() / 1000)));
        cdLane.put("densityMax", Double.valueOf(df.format(locMetric.calcMax() / 1000)));
        cdLane.put("densityMin", Double.valueOf(df.format(locMetric.calcMin() / 1000)));
        cdLane.put("densityQ1", Double.valueOf(df.format(locMetric.calcQ1() / 1000)));
        cdLane.put("densityQ3", Double.valueOf(df.format(locMetric.calcQ3() / 1000)));
        cdLane.put("densitySD", Double.valueOf(df.format(locMetric.calcSD() / 1000)));
        cdLane.put("densityPassingFilter", Double.valueOf(df.format(extMetric.calcMedian() / 1000)));
        cdLane.put("densityPassingFilterMax", Double.valueOf(df.format(extMetric.calcMax() / 1000)));
        cdLane.put("densityPassingFilterMin", Double.valueOf(df.format(extMetric.calcMin() / 1000)));
        cdLane.put("densityPassingFilterQ1", Double.valueOf(df.format(extMetric.calcQ1() / 1000)));
        cdLane.put("densityPassingFilterQ3", Double.valueOf(df.format(extMetric.calcQ3() / 1000)));
        cdLane.put("densityPassingFilterSD", Double.valueOf(df.format(extMetric.calcSD() / 1000)));
        cdLane.put("densityPercentPassed", Double.valueOf(dfTwo.format(((extMetric.calcMean() / 1000) / (locMetric.calcMean() / 1000)) * 100)));
        cdLane.put("units", "K/mm2");
        clusterDensities.add(cdLane);
      }
    }
    json.put("clusterDensities", clusterDensities);

    if(phasingMap == null || prephasingMap == null){
        phasingMap = tileMetrics.getPhasingMap();
        prephasingMap = tileMetrics.getPrephasingMap();
    }
        
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