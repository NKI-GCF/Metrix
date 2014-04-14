package nki.decorators;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import nki.objects.QScoreDist;
import nki.objects.QualityScores;

import java.text.DecimalFormat;
import java.util.Map;

/**
 * Decorator to output objects contained within a MetrixContainer to TSV, CSV and JSON
 *
 * @author Rob Davey
 * @date 07/04/14
 * @since version
 */
public class MetrixQualityMetricsDecorator {
  private QualityScores qualityScores;

  private DecimalFormat df = new DecimalFormat("##.##");

  public MetrixQualityMetricsDecorator(QualityScores qualityScores) {
    this.qualityScores = qualityScores;
  }

  public JSONObject toJSON() {
    JSONObject json = new JSONObject();

    JSONObject combQs = new JSONObject();
    QScoreDist qScoreDist = qualityScores.getQScoreDistribution();

    if (qScoreDist.aboveQ(20) != -1d) {
      combQs.put(">Q20", qScoreDist.aboveQ(20));
    }
    if (qScoreDist.aboveQ(30) != -1d) {
      combQs.put(">Q30", qScoreDist.aboveQ(30));
    }
    json.put("combinedReadQualityScores", combQs);

    JSONArray laneQualities = new JSONArray();
    Map<Integer, QScoreDist> qScoreLaneDist = qualityScores.getQScoreDistributionByLane();
    for (Integer lane : qScoreLaneDist.keySet()) {
      JSONObject lqLane = new JSONObject();
      QScoreDist dist = qScoreLaneDist.get(lane);
      lqLane.put("lane", lane);
      JSONArray a = new JSONArray();
      for (Integer i : dist.getQualityScoreDist().keySet()) {
        JSONObject j = new JSONObject();
        j.put(i, df.format(dist.getQualityScoreDist().get(i).get()));
        a.add(j);
      }
      lqLane.put("raw", a);
      lqLane.put(">Q20", dist.aboveQ(20));
      lqLane.put(">Q30", dist.aboveQ(30));
      lqLane.put(">Q40", dist.aboveQ(40));
      laneQualities.add(lqLane);
    }
    json.put("perLaneQualityScores", laneQualities);

    return json;
  }
}