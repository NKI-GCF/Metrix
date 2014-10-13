package nki.decorators;

import org.json.simple.JSONObject;
import nki.objects.Summary;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Decorator to output objects contained within a MetrixContainer to TSV, CSV and JSON
 *
 * @author Rob Davey
 * @date 07/04/14
 * @since version
 */
public class MetrixSummaryDecorator {
  private Summary summary;

  public MetrixSummaryDecorator(Summary summary) {
    this.summary = summary;
  }
  
  public JSONObject toJSON() {
    JSONObject json = new JSONObject();

    json.put("runType", summary.getRunType());
    json.put("totalCycles", summary.getTotalCycles());
    json.put("currentCycle", summary.getCurrentCycle());
    json.put("runDirectory", summary.getRunDirectory());
    json.put("instrument", summary.getInstrument());
        
    if (summary.getReads() != null) {
      json.put("demultiplexIndex", summary.getReads().getDemultiplexIndex());
    }
    else {
      json.put("demultiplexIndex", "N/A");
    }

    json.put("flowcellId", summary.getFlowcellID());
    json.put("numLanes", summary.getLaneCount());

    return json;
  }
  
    public Element toXML(Element sumXml, Document xmlDoc) {
        sumXml.appendChild(createElement(xmlDoc, "runId", summary.getRunId()));
        sumXml.appendChild(createElement(xmlDoc, "runType", summary.getRunType()));
        sumXml.appendChild(createElement(xmlDoc, "flowcellId", summary.getFlowcellID()));
        sumXml.appendChild(createElement(xmlDoc, "runSide", summary.getSide()));
        sumXml.appendChild(createElement(xmlDoc, "runState", summary.getState() + ""));
        sumXml.appendChild(createElement(xmlDoc, "runPhase", summary.getPhase()));
        sumXml.appendChild(createElement(xmlDoc, "lastUpdated", summary.getLastUpdated() + ""));
        sumXml.appendChild(createElement(xmlDoc, "runDate", summary.getRunDate() + ""));
        sumXml.appendChild(createElement(xmlDoc, "currentCycle", summary.getCurrentCycle() + ""));
        sumXml.appendChild(createElement(xmlDoc, "totalCycle", summary.getTotalCycles() + ""));
        sumXml.appendChild(createElement(xmlDoc, "instrument", summary.getInstrument()));
    
        return sumXml;
  }
    
    
    
   private Element createElement(Document doc, String name, String text) {
    Element e = doc.createElement(name);
    if (text == null) {
      text = "";
    }
    e.appendChild(doc.createTextNode(text));

    return e;
  }
}