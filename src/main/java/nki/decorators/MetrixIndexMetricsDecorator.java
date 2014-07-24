package nki.decorators;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import nki.objects.Indices;
import nki.objects.SampleInfo;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Decorator to output objects contained within a MetrixContainer to TSV, CSV and JSON
 *
 * @author Rob Davey
 * @date 07/04/14
 * @since version
 */
public class MetrixIndexMetricsDecorator {
  private Indices indices;

  public MetrixIndexMetricsDecorator(Indices indices) {
    this.indices = indices;
  }

  public JSONObject toJSON() {
    JSONObject json = new JSONObject();
    Map<String, Map<String, SampleInfo>> indexMap = indices.getIndices();

    for (String projectName : indexMap.keySet()) {
      Map<String, SampleInfo> samples = indexMap.get(projectName);
      JSONArray sampleJson = new JSONArray();
      for (String sampleName : samples.keySet()) {
        SampleInfo si = samples.get(sampleName);

        JSONObject s = new JSONObject();
        s.put("sampleName", sampleName);
        s.put("lane", si.getLaneNum());
        s.put("readNum", si.getReadNum());
        s.put("clusters", si.getNumClusters());
        s.put("barcode", si.getIndexBarcode());
        sampleJson.add(s);
      }
      json.put(projectName, sampleJson);
    }

    json.put("totalClusters", indices.getTotalClusters());

    return json;
  }
  
   public Element toXML() {
    Document xmlDoc = null;
    Element root = null;
    try {
      // Build the XML document
      DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
      xmlDoc = docBuilder.newDocument();
      Element idxStats = xmlDoc.createElement("Indices");
      
      for (String projectName : indices.getIndices().keySet()) {
        Element projEle = xmlDoc.createElement("Project");
        projEle.setAttribute("name", projectName);

        Map<String, SampleInfo> samples = indices.getIndices().get(projectName);
        for (String sampleName : samples.keySet()) {
          Element sampleEle = xmlDoc.createElement("Sample");
          sampleEle.setAttribute("name", sampleName);

          SampleInfo si = samples.get(sampleName);
          sampleEle.setAttribute("lane", String.valueOf(si.getLaneNum()));
          sampleEle.setAttribute("read", String.valueOf(si.getReadNum()));
          sampleEle.setAttribute("clusters", String.valueOf(si.getNumClusters()));
          sampleEle.setAttribute("index", si.getIndexBarcode());
          projEle.appendChild(sampleEle);
        }
        idxStats.appendChild(projEle);
       }
       return idxStats;
    }catch(Exception Ex){
        
    }
    
    return root;
   }
}