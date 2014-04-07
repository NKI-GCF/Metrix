// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.objects;

import java.net.*;
import java.io.*;
import java.lang.*;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.TreeMap;

public class Reads implements Serializable {

  public static final long serialVersionUID = 42L;

  private TreeMap<Integer, ArrayList<String>> readMap = new TreeMap<Integer, ArrayList<String>>();
  private ArrayList<Integer> nonIndexList = new ArrayList<Integer>();
  private int lastCycle = 1;

  public void insertMapping(int readNum, String cycles, String isIndexed) {
    ArrayList<String> subMap;
    if (readMap.containsKey(readNum)) {
      // Get subMap from hashmap.
      subMap = readMap.get(readNum);
    }
    else {
      // Create new readnum entry and popup late with new hashmap
      subMap = new ArrayList<String>();
    }
    subMap.add(cycles);
    subMap.add(isIndexed);

    int cycleInt = Integer.parseInt(cycles);

    if (subMap.get(1).equals("N")) {
      int cnt = lastCycle;
      for (int i = cnt; i < cnt + cycleInt; i++) {
        nonIndexList.add(i);
        lastCycle++;
      }
    }
    else {
      lastCycle += cycleInt;
    }

    readMap.put(readNum, subMap);
  }

  public int getNumberOfReads() {
    return readMap.size();
  }

  public int getPairedTurnCycle() {
    int readOneLength = Integer.parseInt((readMap.get(1)).get(0));
    int readTwoLength = 0;
    if (readMap.size() > 1) {
      readTwoLength = Integer.parseInt((readMap.get(2)).get(0));
    }
    return (readOneLength + readTwoLength);
  }

  public boolean cycleIsIndex(int cycle) {
    if (!Arrays.asList(nonIndexList).contains(cycle)) {
      return true;
    }
    else {
      return false;
    }
  }

  public boolean isIndexedRead(int readNum) {
    if (!readMap.containsKey(readNum)) {
      return false;
    }
    if ((readMap.get(readNum)).get(1).equals("Y")) {
      return true;
    }
    else {
      return false;
    }
  }

  public String getDemultiplexIndex() {
    String demuxIndex = "";
    for (ArrayList<String> l : readMap.values()) {
      if (l.get(1).equals("N")) {
        demuxIndex += "y";  // Read is not an index
      }
      else {
        demuxIndex += "I"; // Read is an index
      }
      demuxIndex += l.get(0);
    }

    return demuxIndex;
  }
}
