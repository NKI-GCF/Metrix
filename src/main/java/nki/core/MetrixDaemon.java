package nki.core;

// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

import java.io.*;

public class MetrixDaemon {

  public static void main(String[] args) {
    try {
      MetrixServer ms = new MetrixServer();
      ms.run();
    }
    catch (IOException ex) {
      System.err.println("Error! : " + ex.toString());
    }
  }
}

