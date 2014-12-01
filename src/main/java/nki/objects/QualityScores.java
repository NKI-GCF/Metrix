// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2014 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.objects;

import java.io.*;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.logging.Level;
import nki.util.LoggerWrapper;

public class QualityScores implements Serializable {

  public static final long serialVersionUID = 42L;
  public int version;
  public int recordLength;
  public String source;
  // Lane - QScore - QMap
  public Map<Integer, Map<Integer, QualityMap>> qScores = new HashMap<>();
  private QScoreDist qScoreDist = new QScoreDist();

  public void setVersion(int version) {
    this.version = version;
  }

  public int getVersion() {
    return version;
  }

  public boolean isEmpty() {
    return qScores.isEmpty();
  }

  public void setRecordLength(int recordLength) {
    this.recordLength = recordLength;
  }

  public int getRecordLength() {
    return recordLength;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getSource() {
    return source;
  }

  public void setLane(Map<Integer, QualityMap> content, int lanenr) {
    Map<Integer, QualityMap> cycleMap = qScores.get(lanenr);

    if (cycleMap == null) {
      qScores.put(lanenr, content);
    }
    else {  // Merge maps and replace existing entries
      Map<Integer, QualityMap> tmpMap = new HashMap<>(content);
      tmpMap.keySet().removeAll(cycleMap.keySet());
      cycleMap.putAll(content);
      qScores.put(lanenr, cycleMap);
    }
  }

  public Map<Integer, QualityMap> getLane(int lanenr) {
    return qScores.get(lanenr);
  }

  public QualityMap getCycle(int lane, int cycle) {
    return (qScores.get(lane)).get(cycle);
  }

  public void setCycle(int lane, int cycle, QualityMap map) {
    qScores.get(lane).put(cycle, map);
  }

  public QScoreDist getQScoreDistribution() {
    for (Integer lane : qScores.keySet()) {
      Map<Integer, QualityMap> laneScores = qScores.get(lane);
      for (Integer cycle : laneScores.keySet()) {
        QualityMap qmap = laneScores.get(cycle);
        for (Integer tile : qmap.getMappings().keySet()) {
          Map<Integer, Integer> qmetricMap = qmap.getMappings().get(tile);
          for (Integer qScore : qmetricMap.keySet()) {
            long metric = Long.valueOf(qmetricMap.get(qScore));
            qScoreDist.setScore(qScore, metric);  // Set the metric in the QScore Distribution
          }
        }
      }
    }
    return qScoreDist;
  }

  public Map<Integer, QScoreDist> getQScoreDistributionByLane() {
    Map<Integer, QScoreDist> laneDist = new TreeMap<>();

    for (Integer lane : qScores.keySet()) {
      Map<Integer, QualityMap> laneScores = qScores.get(lane);
      QScoreDist qScoreDistLane = new QScoreDist();
      for (Integer cycle : laneScores.keySet()) {
        QualityMap qmap = laneScores.get(cycle);
        for (Integer tile : qmap.getMappings().keySet()) {
          Map<Integer, Integer> qmetricMap = qmap.getMappings().get(tile);
          for (Integer qScore : qmetricMap.keySet()) {
            long metric = Long.valueOf(qmetricMap.get(qScore));
            qScoreDistLane.setScore(qScore, metric);  // Set the metric in the QScore Distribution
          }
        }
      }
      laneDist.put(lane, qScoreDistLane);
    }

    return laneDist;
  }

  public Map<Integer, Metric> getQScoreDistributionByCycle() {
    Map<Integer, Metric> cycleDist = new TreeMap<>();

    for (Integer lane : qScores.keySet()) {
      Map<Integer, QualityMap> laneScores = new TreeMap<>(qScores.get(lane));

      for (Integer cycle : laneScores.keySet()) {
        QualityMap qmap = laneScores.get(cycle);
        Metric m = new Metric();

        for (Integer tile : qmap.getMappings().keySet()) {
          Map<Integer, Integer> qmetricMap = qmap.getMappings().get(tile);

          double aboveClus = 0;
          double totalClus = 0;
          double percent = 0d;

          for (Integer qScore : qmetricMap.keySet()) {
            double metric = Double.valueOf(qmetricMap.get(qScore));

            if (qScore > 30) {
              aboveClus += metric;
            }
            totalClus += metric;
          }

          if (totalClus != 0) {
            percent = (aboveClus / totalClus) * 100;
          }

          if (cycleDist.get(cycle) != null) {
            m = cycleDist.get(cycle);
            m.incrementMetric(percent);
          }
          else {
            m.setMetric(percent);
          }
        }
        cycleDist.put(cycle, m);
        LoggerWrapper.log.log(Level.INFO, "Lane {0} Cycle {1} Metric {2} ", new Object[]{lane, cycle, m.tileScores.size()});
      }
    }
    return cycleDist;
  }

  public Map<Integer, Map<Integer, QualityMap>> getRawScores() {
    return qScores;
  }
}