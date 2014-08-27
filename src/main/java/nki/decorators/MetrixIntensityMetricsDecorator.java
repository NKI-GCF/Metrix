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
import nki.objects.IntensityScores;
import nki.objects.MutableInt;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Decorator to output objects contained within a MetrixContainer to TSV, CSV, XML and JSON
 * The MetrixIntensityMetricsDecorator can be instantiated with a set of IntensityScores
 * or with a set of pre-calculated IntensityDistributions for the AverageCorrectedIntensity
 * distribution and the AverageCorrectedIntensityCalledClusters distribution.
 *
 * @author Rob Davey
 * @date 07/04/14
 * @since version
 * @updated-by Bernd van der Veen
 */
public class MetrixIntensityMetricsDecorator {
  private IntensityScores intensityScores;
  private IntensityDist iDistAvg;
  private IntensityDist iDistAvgCC;
  
  public MetrixIntensityMetricsDecorator(IntensityScores intensityScores) {
    this.intensityScores = intensityScores;
    if(this.intensityScores != null){
        iDistAvg = intensityScores.getAverageCorrectedIntensityDist();
        iDistAvgCC = intensityScores.getCalledClustersAverageCorrectedIntensityDist();
    }
  }
  
  public MetrixIntensityMetricsDecorator(IntensityDist iDistAvg, IntensityDist iDistAvgCC) {
      this.iDistAvg = iDistAvg;
      this.iDistAvgCC = iDistAvgCC;
  }

  /*
   * Generate JSON for both distributions (AvgCorrectedInt and AvgCalledClusterCorrectedInt)
   * and return a json object.
   */
  public JSONObject toJSON() {
    JSONObject jsonCombined = new JSONObject();
    
    if(iDistAvg != null){
        jsonCombined.put("averageCorrected", generateJSON(iDistAvg));
    }else{
        jsonCombined.put("averageCorrected", "NoDistAvailable");
    }
    
    if(iDistAvgCC != null){
        jsonCombined.put("averageCorrectedCalledClusters", generateJSON(iDistAvgCC));
    }else{
        jsonCombined.put("averageCorrectedCalledClusters", "NoDistAvailable");
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
          if (intensity.equals(Constants.METRIC_VAR_ACI_A) || intensity.equals(Constants.METRIC_VAR_ACICC_A)) {
            cyclesA.add(cycleIntensities.get(intensity).get());
          }
          if (intensity.equals(Constants.METRIC_VAR_ACI_C) || intensity.equals(Constants.METRIC_VAR_ACICC_C)) {
            cyclesC.add(cycleIntensities.get(intensity).get());
          }
          if (intensity.equals(Constants.METRIC_VAR_ACI_T) || intensity.equals(Constants.METRIC_VAR_ACICC_T)) {
            cyclesT.add(cycleIntensities.get(intensity).get());
          }
          if (intensity.equals(Constants.METRIC_VAR_ACI_G) || intensity.equals(Constants.METRIC_VAR_ACICC_G)) {
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

      root = xmlDoc.createElement("IntensityDistributions");
      xmlDoc.appendChild(root);
      
      // Generate as XML and add: 
      // - averageCorrectedIntensity
      // - averageCorrectIntensitiesCalledClusters
      Element sumXml = xmlDoc.createElement("averageCorrected");
      sumXml = generateXML(sumXml, xmlDoc, iDistAvg);
      root.appendChild(sumXml);
      
      sumXml = xmlDoc.createElement("averageCorrectedCalledClusters");
      sumXml = generateXML(sumXml, xmlDoc, iDistAvgCC);
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

        Element intEle = xmlDoc.createElement("Intensities");
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
      String allTab = "Average Corrected Intensities: \n";
      // Add the average Corrected Intensities
      allTab += generateTab(iDistAvg);
      
      allTab += "\nAverage Corrected Intensities Called Clusters: \n";
      // Add the average Corrected Called Cluster Intensities
      allTab += generateTab(iDistAvgCC);
      
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