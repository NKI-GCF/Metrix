// Illumina Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2014 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.parsers.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import nki.objects.Summary;
import nki.objects.Reads;

public class RunInfoHandler {

  public static Summary parseAll(Document doc, Summary sum) { // Also argument Summary file.
    Element root = doc.getDocumentElement();
    // Run -id -Number
    Node runNode = doc.getElementsByTagName("Run").item(0);
    String runID = runNode.getAttributes().getNamedItem("Id").getTextContent();
    sum.setRunId(runID);

    String[] spl = runID.split("_");
    switch (spl[3].substring(0, 1)) {
      case "A":
        sum.setSide("A");
        sum.setInstrumentType("HiSeq");
        break;
      case "B":
        sum.setSide("B");
        sum.setInstrumentType("HiSeq");
        break;
      default:
        // Instrument type is MiSeq - No Side
        sum.setSide("");
        sum.setInstrumentType("MiSeq");
        break;
    }

    if (spl.length > 4) {
      sum.setRunNameOptional(spl[4]);
    }

    String runInstrumentNr = runNode.getAttributes().getNamedItem("Number").getTextContent();
    sum.setInstrumentRunNumber(runInstrumentNr);

    // Flowcell
    String flowCell = "XXXXXXXXXX";  // Default empty flowcell ID
    if (doc.getElementsByTagName("Flowcell").getLength() != 0) {
      flowCell = doc.getElementsByTagName("Flowcell").item(0).getTextContent();
    }
    sum.setFlowcellID(flowCell);

    // Instrument
    String instrument = doc.getElementsByTagName("Instrument").item(0).getTextContent();
    sum.setInstrument(instrument);

    // Date
    String date = "000000";
    if (doc.getElementsByTagName("Date").getLength() != 0) {
      date = doc.getElementsByTagName("Date").item(0).getTextContent();
    }
    sum.setRunDate(Integer.parseInt(date));

    // Reads (w/ children of number of reads)
    NodeList readNodes = doc.getElementsByTagName("Read");
    Reads rd = new Reads();
    int totalCycles = 0;
    for (int i = 0; i < readNodes.getLength(); i++) {
      // Read  -Number -NumCycles -IsIndexedRead
      Node readNode = readNodes.item(i);
      String readNumber = "";
      if (readNode.getAttributes().getLength() >= 3) {
        readNumber = readNode.getAttributes().getNamedItem("Number").getTextContent();
      }
      else {
        readNumber = Integer.toString(i);
      }

      String numCycles = "0";
      if (readNode.getAttributes().getLength() >= 3) {
        numCycles = readNode.getAttributes().getNamedItem("NumCycles").getTextContent();
      }

      String isIndexedRead = "";
      if (readNode.getAttributes().getLength() >= 3) {
        isIndexedRead = readNode.getAttributes().getNamedItem("IsIndexedRead").getTextContent();
      }

      totalCycles += Integer.parseInt(numCycles);
      rd.insertMapping(Integer.parseInt(readNumber), numCycles, isIndexedRead);
    }

    sum.setTotalCycles(totalCycles);

    System.out.println("Number of reads :" + readNodes.getLength());
    
    if (readNodes.getLength() == 1) {
      sum.setRunType("Single Read");
      sum.setIsIndexed(false);
    }else if(readNodes.getLength() == 2){
        if (readNodes.item(1).getAttributes().getLength() >= 3) {
            if(readNodes.item(1).getAttributes().getNamedItem("IsIndexedRead").getTextContent().equalsIgnoreCase("N")){
                sum.setRunType("Paired End");
                sum.setIsIndexed(false);
            }else{
                sum.setRunType("Single Read");
                sum.setIsIndexed(true);           
            }
        }
    }else if(readNodes.getLength() == 3) {    // Run Type = Paired End Run
      sum.setRunType("Paired End");
      sum.setIsIndexed(true);
    }else if(readNodes.getLength() == 4) {    // Run Type = Nextera Run
      sum.setRunType("Nextera");
      sum.setIsIndexed(true);
    }

    sum.setReads(rd);  // Store in Summary object

    // FlowcellLayout -LaneCount -SurfaceCount -SwathCount -TileCount
    Node fcLayout;

    if (doc.getElementsByTagName("FlowcellLayout").getLength() != 0) {
      fcLayout = doc.getElementsByTagName("FlowcellLayout").item(0);
      String lc = fcLayout.getAttributes().getNamedItem("LaneCount").getTextContent();
      sum.setLaneCount(lc);

      String sc = fcLayout.getAttributes().getNamedItem("SurfaceCount").getTextContent();
      sum.setSurfaceCount(sc);

      String swC = fcLayout.getAttributes().getNamedItem("SwathCount").getTextContent();
      sum.setSwathCount(swC);

      String tc = fcLayout.getAttributes().getNamedItem("TileCount").getTextContent();
      sum.setTileCount(tc);
    }
    sum.setXmlInfo(true);

    return sum;
  }

}
