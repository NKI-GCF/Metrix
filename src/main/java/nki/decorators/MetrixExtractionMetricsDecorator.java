package nki.decorators;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import nki.constants.Constants;
import nki.objects.IntensityDist;
import nki.objects.MutableInt;
import nki.parsers.illumina.ExtractionMetrics;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Decorator to output objects contained within a MetrixContainer to TSV, CSV and JSON
 *
 * @author Bernd van der Veen
 * @date 14/07/14
 * @since version
 */
public class MetrixExtractionMetricsDecorator {
  private ExtractionMetrics extractionMetrics;
  private IntensityDist riDist;

  public MetrixExtractionMetricsDecorator(ExtractionMetrics extractionMetrics) {
    this.extractionMetrics = extractionMetrics;
    this.riDist = extractionMetrics.getIntensityScores().getRawIntensityDist();
  }
  
  public MetrixExtractionMetricsDecorator(IntensityDist iDist){
      this.riDist = iDist;
  }

public JSONObject toJSON() {
    JSONObject jsonCombined = new JSONObject();
    if(riDist != null){
        jsonCombined.put("rawIntensities", generateJSON(riDist));
    }else{
        jsonCombined.put("rawIntensities", "NoDistAvailable");
    }
    
    return jsonCombined;
  }
  
  private JSONArray generateJSON(IntensityDist id){
    JSONArray averages = new JSONArray();

    for (int lane : id.getIntensities().keySet()) {
      JSONObject l = new JSONObject();
      Map<Integer, Map<String, MutableInt>> cycleContent = new TreeMap<>(id.getIntensities().get(lane));

      JSONArray cyclesA = new JSONArray();
      JSONArray cyclesC = new JSONArray();
      JSONArray cyclesT = new JSONArray();
      JSONArray cyclesG = new JSONArray();

      for (int cycle : cycleContent.keySet()) {
        Map<String, MutableInt> cycleIntensities = cycleContent.get(cycle);
        for (String intensity : cycleIntensities.keySet()) {
          if (intensity.equals(Constants.METRIC_EX_RAWINT_A)) {
            cyclesA.add(cycleIntensities.get(intensity).get());
          }
          if (intensity.equals(Constants.METRIC_EX_RAWINT_C)) {
            cyclesC.add(cycleIntensities.get(intensity).get());
          }
          if (intensity.equals(Constants.METRIC_EX_RAWINT_T)) {
            cyclesT.add(cycleIntensities.get(intensity).get());
          }
          if (intensity.equals(Constants.METRIC_EX_RAWINT_G)) {
            cyclesG.add(cycleIntensities.get(intensity).get());
          }
        }
      }

      l.put("lane", lane);
      l.put("intA", cyclesA);
      l.put("intC", cyclesC);
      l.put("intT", cyclesT);
      l.put("intG", cyclesG);
      averages.add(l);
    }
      return averages;
  }

  public Element toXML() {
    Document xmlDoc = null;
    Element root = null;
    try {
      // Build the XML document
      DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
      xmlDoc = docBuilder.newDocument();

      root = xmlDoc.createElement("RawIntensityDistribution");
      xmlDoc.appendChild(root);
      
      // Generate as XML and add: 
      // - rawIntensities from extraction metrics
      Element sumXml = xmlDoc.createElement("rawIntensity");
      sumXml = generateXML(sumXml, xmlDoc, riDist);
      root.appendChild(sumXml);
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
    
    return root;
  }
    
  @SuppressWarnings("unchecked")
  public Element generateXML(Element sumXml, Document xmlDoc, IntensityDist id) {
    Iterator lit = id.getIntensities().entrySet().iterator();
    /*
    * Key   = Lane	- Integer
    * Value = CycleMap 	- HashMap<Integer, HashMap<String, Object>>
    */
    while (lit.hasNext()) {
      Element laneEle = xmlDoc.createElement("Lane");
      Map.Entry lanePairs = (Map.Entry) lit.next();
      int lane = (Integer) lanePairs.getKey();
      laneEle.setAttribute("lane", Integer.toString(lane));

      HashMap<Integer, HashMap<String, MutableInt>> cycleContent;
      cycleContent = (HashMap<Integer, HashMap<String, MutableInt>>) lanePairs.getValue();
      // Cycle Iterator
      Iterator cit = (Iterator) cycleContent.entrySet().iterator();

      while (cit.hasNext()) {
        Element cycleEle = xmlDoc.createElement("Cycle");
        Map.Entry cycleEntries = (Map.Entry) cit.next();
        int cycle = (Integer) cycleEntries.getKey();
        cycleEle.setAttribute("num", Integer.toString(cycle));

        // Nested Intensities HashMap
        HashMap<String, MutableInt> cycleInt = (HashMap<String, MutableInt>) cycleEntries.getValue();

        Iterator iit = (Iterator) cycleInt.entrySet().iterator();

        Element intEle = xmlDoc.createElement("RawIntensities");
        while (iit.hasNext()) {
          Map.Entry intensityPairs = (Map.Entry) iit.next();
          String constName = (String) intensityPairs.getKey();
          MutableInt intValue = (MutableInt) intensityPairs.getValue();

          if (intValue instanceof MutableInt) {
            MutableInt in = (MutableInt) intValue;
            intEle.setAttribute(constName, Integer.toString(in.get()));
          }

          cycleEle.appendChild(intEle);
        }
        laneEle.appendChild(cycleEle);
      }
      sumXml.appendChild(laneEle);
    }

    return sumXml;
  }

  public String toTab(){
      String allTab = "Raw Intensities: \n";
      // Add the average Corrected Intensities
      allTab += generateTab(riDist);
      
      return allTab;
  }
  
  @SuppressWarnings("unchecked")
  public String generateTab(IntensityDist id) {
    String out = "";
    for (Integer lane : id.getIntensities().keySet()) {
      Map<Integer, Map<String, MutableInt>> cycleContent = id.getIntensities().get(lane);
      for (Integer cycle : cycleContent.keySet()) {
        Map<String, MutableInt> cycleInt = cycleContent.get(cycle);
        out += lane + "\t" + cycle;

        for (String constName : cycleInt.keySet()) {
          MutableInt intValue = cycleInt.get(constName);
          out += "\t" + constName + ":" + intValue;
        }

        out += "\n";
      }
    }
    return out;
  }
}