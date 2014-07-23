// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2014 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.objects;

import java.io.*;
import java.util.*;

import nki.constants.Constants;
import nki.util.LoggerWrapper;

import org.w3c.dom.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

public class SummaryCollection implements Serializable {
  // Object Specific
  private static final long serialVersionUID = 42L;

  // Instantiate Logger
  private static final LoggerWrapper metrixLogger = LoggerWrapper.getInstance();

  // Object Collection
  private List<Summary> summaryCollection = new ArrayList<>();
  private Map<Integer, MutableInt> summaryStateMapping = new HashMap<>();
  private String collectionFormat = Constants.COM_FORMAT_OBJ;      // Default

  public void appendSummary(Summary sum) {
    summaryCollection.add(sum);

    MutableInt frq = summaryStateMapping.get(sum.getState());
    if (frq == null) {
      summaryStateMapping.put(sum.getState(), new MutableInt());
    }
    else {
      summaryStateMapping.get(sum.getState()).increment();
    }
  }

  public Document toXML(Command com) {
    Document xmlDoc = null;
    try {
      // Build the XML document
      DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
      xmlDoc = docBuilder.newDocument();

      Element root = xmlDoc.createElement("SummaryCollection");
      xmlDoc.appendChild(root);

      // Set run counts as orot attributes
      root.setAttribute("active", convertStateInt(Constants.STATE_RUNNING));
      root.setAttribute("finished", convertStateInt(Constants.STATE_FINISHED));
      root.setAttribute("error", convertStateInt(Constants.STATE_HANG));
      root.setAttribute("turn", convertStateInt(Constants.STATE_TURN));
      root.setAttribute("init", convertStateInt(Constants.STATE_INIT));

      for (Summary sumObj : summaryCollection) {
        // If state is 12, fetch all objects.
        if (sumObj.getState() == com.getState() || com.getState() == Constants.STATE_ALL_PSEUDO) {
          Element sumXml = xmlDoc.createElement("Summary");
          sumXml.setAttribute("runId", sumObj.getRunId());
          if (com.getType().equals(Constants.COM_TYPE_SIMPLE)) {
            sumXml = summaryAsSimple(sumObj, sumXml, xmlDoc);
          }
          else if (com.getType().equals(Constants.COM_TYPE_DETAIL)) {
            sumXml = summaryAsDetailed(sumObj, sumXml, xmlDoc);
          }
          else {
            sumXml = summaryAsSimple(sumObj, sumXml, xmlDoc);
          }

          root.appendChild(sumXml);
        }
      }
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    setCollectionFormat(Constants.COM_FORMAT_XML);

    // Return in the form of a XML Document
    return xmlDoc;
  }

	/*
   *	XML Builders
	 */

  private Element summaryAsSimple(Summary sumObj, Element sumXml, Document xmlDoc) {
    sumXml.appendChild(createElement(xmlDoc, "runId", sumObj.getRunId()));
    sumXml.appendChild(createElement(xmlDoc, "runType", sumObj.getRunType()));
    sumXml.appendChild(createElement(xmlDoc, "runState", sumObj.getState() + ""));
    sumXml.appendChild(createElement(xmlDoc, "lastUpdated", sumObj.getLastUpdated() + ""));
    sumXml.appendChild(createElement(xmlDoc, "runDate", sumObj.getRunDate() + ""));
    sumXml.appendChild(createElement(xmlDoc, "totalCycle", sumObj.getTotalCycles() + ""));
    sumXml.appendChild(createElement(xmlDoc, "instrument", sumObj.getInstrument()));
    return sumXml;
  }

  private Element summaryAsDetailed(Summary sumObj, Element sumXml, Document xmlDoc) {
    sumXml.appendChild(createElement(xmlDoc, "runId", sumObj.getRunId()));
    sumXml.appendChild(createElement(xmlDoc, "runType", sumObj.getRunType()));
    sumXml.appendChild(createElement(xmlDoc, "flowcellId", sumObj.getFlowcellID()));
    sumXml.appendChild(createElement(xmlDoc, "runSide", sumObj.getSide()));
    sumXml.appendChild(createElement(xmlDoc, "runState", sumObj.getState() + ""));
    sumXml.appendChild(createElement(xmlDoc, "runPhase", sumObj.getPhase()));
    sumXml.appendChild(createElement(xmlDoc, "lastUpdated", sumObj.getLastUpdated() + ""));
    sumXml.appendChild(createElement(xmlDoc, "runDate", sumObj.getRunDate() + ""));
    sumXml.appendChild(createElement(xmlDoc, "currentCycle", sumObj.getCurrentCycle() + ""));
    sumXml.appendChild(createElement(xmlDoc, "totalCycle", sumObj.getTotalCycles() + ""));
    sumXml.appendChild(createElement(xmlDoc, "instrument", sumObj.getInstrument()));
    return sumXml;
  }

  private Element summaryAsMetric(Summary sumObj, Element sumXml, Document xmlDoc) {
    if (sumObj.getParseError() == 0) {
      // QScore Dist
      Element xml = xmlDoc.createElement("QScoreDist");
      QScoreDist dist = sumObj.getQScoreDist();
      if (dist != null) {
        xml.setAttribute("totalClusters", dist.getTotalClusters() + "");
        xml = dist.toXML(xml, xmlDoc);
      }

      if (sumObj.hasSampleInfo()) {
        sumXml = sumObj.getSampleInfo().toXML(sumXml, xmlDoc);
      }

      // Cluster Density
      if (sumObj.hasClusterDensity()) {
        sumXml = sumObj.getClusterDensity().toXML(sumXml, xmlDoc);
      }

      // Cluster Density Passing Filter
      if (sumObj.hasClusterDensityPF()) {
        sumXml = sumObj.getClusterDensityPF().toXML(sumXml, xmlDoc);
      }

      // Intensities
      if (sumObj.hasIntensityDistCCAvg()) {
    //    sumXml = sumObj.getIntensityDistCCAvg().toXML(sumXml, xmlDoc);
      }

      // Prephasing
      if (sumObj.hasPrephasing()) {
        sumXml = sumObj.getPrephasingMap().toXML(sumXml, xmlDoc);
      }

      // Phasing
      if (sumObj.hasPhasing()) {
        sumXml = sumObj.getPhasingMap().toXML(sumXml, xmlDoc);
      }
    }
    else {
      sumXml = xmlDoc.createElement("ParseError");
      sumXml.setAttribute("runId", sumObj.getRunId());
    }

    return sumXml;
  }

	/*
	 *  Helpers
	 */
  public String convertStateInt(int mapping) {
    if (summaryStateMapping.containsKey(mapping)) {
      return Integer.toString(summaryStateMapping.get(mapping).get());
    }
    return "";
  }

  private Element createElement(Document doc, String name, String text) {
    Element e = doc.createElement(name);
    if (text == null) {
      text = "";
    }
    e.appendChild(doc.createTextNode(text));

    return e;
  }

/*
 *	Converters 
 */

  public String getSummaryCollectionXMLAsString(Command com) {
    // Call getSummaryCollectionAsXML
    Document xmlDoc = this.toXML(com);
    StringWriter writer = new StringWriter();

    try {
      TransformerFactory tFact = TransformerFactory.newInstance();
      Transformer trans = tFact.newTransformer();

      trans.setOutputProperty("omit-xml-declaration", "yes");

      StreamResult result = new StreamResult(writer);
      DOMSource source = new DOMSource(xmlDoc);
      trans.transform(source, result);
    }
    catch (TransformerConfigurationException TCE) {
      metrixLogger.log.severe("TransformerConfigurationException: " + TCE.toString());
    }
    catch (TransformerException TE) {
      metrixLogger.log.severe("TransformerException: " + TE.toString());
    }

    return writer.toString();
  }

  public void setCollectionFormat(String format) {
    this.collectionFormat = format;
  }

  public String getCollectionFormat() {
    return collectionFormat;
  }

  public int getCollectionCount() {
    return summaryCollection.size();
  }

  public List<Summary> getSummaryCollection() {
    return summaryCollection;
  }
}
