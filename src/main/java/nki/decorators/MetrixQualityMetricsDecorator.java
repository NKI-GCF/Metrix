package nki.decorators;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import nki.objects.Metric;
import nki.objects.MutableLong;
import nki.objects.QScoreDist;
import nki.objects.QualityScores;
import nki.objects.Summary;

/**
 * Decorator to output objects contained within a MetrixContainer to TSV, CSV and JSON
 *
 * @author Rob Davey
 * @date 07/04/14
 * @since version
 */
public class MetrixQualityMetricsDecorator {
  private QualityScores qualityScores;
  private QScoreDist qScoreDist;
  private Map<Integer, QScoreDist> qScoreDistByLane;
  private Map<Integer, Metric> qScoreDistByCycle;
  private DecimalFormat df = new DecimalFormat("##.##", new DecimalFormatSymbols(Locale.US));

  public MetrixQualityMetricsDecorator(QualityScores qualityScores) {
    this.qualityScores = qualityScores;
    this.qScoreDistByLane = qualityScores.getQScoreDistributionByLane();
    this.qScoreDistByCycle = qualityScores.getQScoreDistributionByCycle();
  }

  public MetrixQualityMetricsDecorator( QScoreDist qScoreDist, 
                                        Map<Integer, QScoreDist> qScoreDistByLane, 
                                        Map<Integer, Metric> qScoreDistByCycle
  ){
      this.qScoreDist = qScoreDist;
      this.qScoreDistByLane = qScoreDistByLane;
      this.qScoreDistByCycle = qScoreDistByCycle;
  }
  
    public MetrixQualityMetricsDecorator( Summary sum ){
      this.qScoreDist = sum.getQScoreDist();
      //this.qScoreDistByLane = sum.getQScores().getQScoreDistributionByLane();
      //this.qScoreDistByCycle = sum.getQScores().getQScoreDistributionByCycle();
      this.qScoreDistByLane = sum.getQScoreDistByLane();
      this.qScoreDistByCycle = sum.getQScoreDistByCycle();
  }
  
  public JSONObject toJSON() {
    JSONObject json = new JSONObject();
    JSONObject combQs = new JSONObject();
    if(this.qualityScores != null && this.qScoreDist == null){
        qScoreDist = qualityScores.getQScoreDistribution();
    }

    if (qScoreDist.aboveQ(20) != -1d) {
      combQs.put(">Q20", qScoreDist.aboveQ(20));
    }
    if (qScoreDist.aboveQ(30) != -1d) {
      combQs.put(">Q30", qScoreDist.aboveQ(30));
    }

    JSONArray combA = new JSONArray();
    Map<Integer, MutableLong> combDist = qScoreDist.getQualityScoreDist();
    for (Integer i : combDist.keySet()) {
      combA.add(i-1,  Double.valueOf(df.format(combDist.get(i).get())));
    }
    combQs.put("raw", combA);

    json.put("combinedReadQualityScores", combQs);

    JSONArray laneQualities = new JSONArray();
    //Map<Integer, QScoreDist> qScoreLaneDist = qualityScores.getQScoreDistributionByLane();
    for (Integer lane : qScoreDistByLane.keySet()) {
      JSONObject lqLane = new JSONObject();
      QScoreDist dist = qScoreDistByLane.get(lane);
      lqLane.put("lane", lane);
      JSONArray a = new JSONArray();
      Map<Integer, MutableLong> sDist = dist.getQualityScoreDist();
      for (Integer i : sDist.keySet()) {
        a.add(i-1,  Double.valueOf(df.format(sDist.get(i).get())));
      }
      lqLane.put("raw", a);
      lqLane.put(">Q20", dist.aboveQ(20));
      lqLane.put(">Q30", dist.aboveQ(30));
      lqLane.put(">Q40", dist.aboveQ(40));
      laneQualities.add(lqLane);
    }
    json.put("perLaneQualityScores", laneQualities);

    JSONArray cycleQualities = new JSONArray();
    //Map<Integer, Metric> qScoreCycleDist = qualityScores.getQScoreDistributionByCycle();

    for (Integer cycle : qScoreDistByCycle.keySet()) {
      JSONObject qCycle = new JSONObject();

      Metric locMetric = qScoreDistByCycle.get(cycle);

      if (locMetric != null && !locMetric.getTileScores().isEmpty()) {
        qCycle.put("qMedian", Double.valueOf(df.format(locMetric.calcMedian())));
        qCycle.put("qMax", Double.valueOf(df.format(locMetric.calcMax())));
        qCycle.put("qMin", Double.valueOf(df.format(locMetric.calcMin())));
        qCycle.put("qQ1", Double.valueOf(df.format(locMetric.calcQ1())));
        qCycle.put("qQ3", Double.valueOf(df.format(locMetric.calcQ3())));
        qCycle.put("qSD", Double.valueOf(df.format(locMetric.calcSD())));
        cycleQualities.add(qCycle);
      }
    }
    json.put("perCycleQualityScores", cycleQualities);

    return json;
  }
}