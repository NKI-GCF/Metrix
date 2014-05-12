// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.util;

import java.util.Comparator;

import nki.objects.PostProcess;

public class OperationComparator<T extends PostProcess> implements Comparator<T> {
  @Override
  public int compare(T p1, T p2) {
    int mainOrderCompare = Integer.compare(p1.getOrder(), p2.getOrder());
    if (mainOrderCompare != 0) {
      return mainOrderCompare;
    }
    else {
      return (p1.getSubOrder() < p2.getSubOrder()) ? -1 : (p1.getSubOrder() > p2.getSubOrder()) ? 1 : 0;
    }
  }
}
