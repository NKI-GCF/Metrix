// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.util;

import java.net.*;
import java.io.*;
import java.lang.*;
import java.util.Collections;
import java.util.Comparator;
import nki.objects.PostProcess;

public class PostProcessComparator implements Comparator<PostProcess> {

  @Override
  public int compare(PostProcess p1, PostProcess p2) {
	return (p1.getOrder() < p2.getOrder() ) ? -1 : (p1.getOrder() > p2.getOrder()) ? 1 : 0;
  }
}
