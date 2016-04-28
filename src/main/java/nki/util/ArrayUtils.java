// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2014 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.util;

import java.util.*;
import java.util.logging.Level;
import nki.util.LoggerWrapper;

public class ArrayUtils {
  public static double sum(List<Double> a) {
    if (a.size() > 0) {
      double sum = 0;

      for (Double i : a) {
        sum += i;
      }
      return sum;
    }
    return 0;
  }

  public static double mean(List<Double> a) {
    double sum = sum(a);
    double mean = 0;
    mean = sum / (a.size() * 1.0);
    return mean;
  }

  public static double median(List<Double> a) {
    Collections.sort(a);

    int middle = a.size() / 2;

    if (a.size() % 2 == 1) {
      return a.get(middle);
    }
    else {
      return (a.get(middle - 1) + a.get(middle)) / 2.0;
    }
  }

  public static double max(List<Double> a) {
    return Collections.max(a);
  }

  public static double min(List<Double> a) {
    return Collections.min(a);
  }

  public static double sd(List<Double> a) {
    double sum = 0;
    double mean = mean(a);

    for (Double i : a)
      sum += Math.pow((i - mean), 2);
    return Math.sqrt(sum / (a.size() - 1)); // sample
  }

  public static double quartile(double[] values, double lowerPercent) {
    if (values == null || values.length == 0) {
      throw new IllegalArgumentException("The data array either is null or does not contain any data.");
    }

    // Rank order the values
    double[] v = new double[values.length];
    System.arraycopy(values, 0, v, 0, values.length);
    Arrays.sort(v);

    int n = (int) Math.round(v.length * lowerPercent / 100);

    return v[n];
  }

  public static double quartile(List<Double> values, double lowerPercent) {
    if (values == null || values.isEmpty()) {
      throw new IllegalArgumentException("The data array either is null or does not contain any data.");
    }

    double[] prival = new double[values.size()];
    for (int i = 0; i < values.size(); i++) {
      prival[i] = values.get(i);
    }

    // Rank order the values
    Arrays.sort(prival);

    int n = (int) Math.round(prival.length * lowerPercent / 100);

    if (prival.length > n) {
      return prival[n];
    }
    else {
      return 0;
    }
  }

  public double getVariance(List<Double> data) {
    double mean = mean(data);
    double temp = 0;
    double size = data.size();

    for (double a : data) {
      temp += (mean - a) * (mean - a);
    }

    return temp / size;
  }
}
