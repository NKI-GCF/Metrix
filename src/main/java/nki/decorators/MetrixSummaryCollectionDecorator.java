package nki.decorators;

import java.util.ListIterator;
import java.util.logging.Level;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import nki.constants.Constants;
import nki.core.MetrixContainer;
import nki.objects.Summary;
import nki.objects.SummaryCollection;
import nki.util.LoggerWrapper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Decorator for a SummaryCollection object.
 *
 * @author Bernd van der Veen
 * @date 14/07/14
 * @since version
 */
public class MetrixSummaryCollectionDecorator {
  private SummaryCollection sc;
  private String expectedType = Constants.COM_TYPE_SIMPLE;
  
  public MetrixSummaryCollectionDecorator(SummaryCollection sc) {
    this.sc = sc;
  }

  public void setExpectedType(String expectedType){
      if(expectedType.equals(Constants.COM_TYPE_DETAIL)){
        this.expectedType = Constants.COM_TYPE_DETAIL;
      }else{
        this.expectedType = Constants.COM_TYPE_SIMPLE;
      }
  }
  
  
  public JSONObject toJSON(){
      JSONObject json = new JSONObject();
      JSONArray jsonCollection = new JSONArray();
      
      //for(Summary sum : sc.getSummaryCollection()){
      for(ListIterator<Summary> iter = sc.getSummaryCollection().listIterator(); iter.hasNext();){
          Summary sum = iter.next();
          LoggerWrapper.log.log(Level.INFO, "Processing {0}", sum.getRunId());
          JSONObject metrixJson = new JSONObject();
          MetrixContainer mc = new MetrixContainer(sum);
          
          if(this.expectedType.equals(Constants.COM_TYPE_SIMPLE)){
            JSONObject summary = new MetrixSummaryDecorator(mc.getSummary()).toJSON();
            metrixJson.put("summary", summary);
          }else if(this.expectedType.equals(Constants.COM_TYPE_DETAIL)){
            JSONObject summary = new MetrixSummaryDecorator(mc.getSummary()).toJSON();
            JSONObject tileMetrics = new MetrixTileMetricsDecorator(mc.getSummary().getClusterDensity(),
                                                                    mc.getSummary().getClusterDensityPF(),
                                                                    mc.getSummary().getPhasingMap(),
                                                                    mc.getSummary().getPrephasingMap(),
                                                                    mc.getSummary().getReads()
                                     ).toJSON();
            
            JSONObject qualityMetrics = new MetrixQualityMetricsDecorator(mc.getSummary()).toJSON();
            JSONObject errorMetrics = new MetrixErrorMetricsDecorator(mc.getSummary().getErrorDist()).toJSON();
            JSONObject indexMetrics = new MetrixIndexMetricsDecorator(mc.getSummary().getSampleInfo()).toJSON();
            JSONObject extractionMetrics = new MetrixExtractionMetricsDecorator(mc.getSummary().getIntensityDistRaw()).toJSON();
            JSONObject intensityMetrics = new MetrixIntensityMetricsDecorator(mc.getSummary().getIntensityDistAvg(), mc.getSummary().getIntensityDistCCAvg()).toJSON();
          
            metrixJson.put("summary", summary);
            metrixJson.put("tileMetrics", tileMetrics);
            metrixJson.put("qualityMetrics", qualityMetrics);
            metrixJson.put("errorMetrics", errorMetrics);
            metrixJson.put("indexMetrics", indexMetrics);
            metrixJson.put("extractionMetrics", extractionMetrics);
            metrixJson.put("intensityMetrics", intensityMetrics);
            
            LoggerWrapper.log.log(Level.FINEST, "Emptying MetrixContainer");
            mc = null;
            iter.remove();
            LoggerWrapper.log.log(Level.INFO, "Removed summary from list.");
          }else{
              metrixJson.put("Unknown request type. ", new JSONObject());
          }
          
          jsonCollection.add(metrixJson);
          
      }
      // Add statistics to json object.
      json.accumulate("summaries", jsonCollection);
      return json;
  }
  
  public String toCSV(){
      
      for(Summary sum : sc.getSummaryCollection()){
          JSONObject metrixJson = new JSONObject();
          MetrixContainer metrixContainer = new MetrixContainer(sum);
          
      }
      return "";
  }
  
  public String toTab(){
      
      for(Summary sum : sc.getSummaryCollection()){
          
      }      
      return "";
  }
  
  public Element toXML(){
    Document xmlDoc = null;
    Element root = null;
    try {
      // Build the XML document
      DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
      xmlDoc = docBuilder.newDocument();

      root = xmlDoc.createElement("SummaryCollection");
      xmlDoc.appendChild(root);
      
        for(ListIterator<Summary> iter = sc.getSummaryCollection().listIterator(); iter.hasNext();){
          Summary sum = iter.next();
          LoggerWrapper.log.log(Level.INFO, "Processing {0}", sum.getRunId());
          MetrixContainer mc = new MetrixContainer(sum);          
          
          Element sumXml = xmlDoc.createElement("Summary");
          sumXml.setAttribute("runId", sum.getRunId());
          if (this.expectedType.equals(Constants.COM_TYPE_SIMPLE)) {
            sumXml = new MetrixSummaryDecorator(mc.getSummary()).toXML(sumXml, xmlDoc);
            root.appendChild(sumXml);
          }
          else if (this.expectedType.equals(Constants.COM_TYPE_DETAIL)) {
            Element runinfo = xmlDoc.createElement("RunInfo");
            runinfo = new MetrixSummaryDecorator(mc.getSummary()).toXML(sumXml, xmlDoc);
            sumXml.appendChild(runinfo);
            
            Element tile = xmlDoc.createElement("tileMetrics");
            //tile = new MetrixTileMetricsDecorator(mc.getSummary()).toXML(sumXml, xmlDoc);
            sumXml.appendChild(tile);
            
            Element quality = xmlDoc.createElement("qualityMetrics");
            //quality = new MetrixQualityMetricsDecorator(mc.getSummary().getQScoreDist()).toXML();
            sumXml.appendChild(quality);
            
            Element error = xmlDoc.createElement("errorMetrics");
            //error = new MetrixErrorMetricsDecorator(mc.getSummary().getErrorDist()).toXML();
            sumXml.appendChild(error);
            
            Element index = xmlDoc.createElement("indexMetrics");
            index = new MetrixIndexMetricsDecorator(mc.getSummary().getSampleInfo()).toXML();
            sumXml.appendChild(index);
            
            Element extraction = xmlDoc.createElement("extractionMetrics");
            extraction = new MetrixExtractionMetricsDecorator(mc.getSummary().getIntensityDistRaw()).toXML();
            sumXml.appendChild(extraction);
            
            Element intensity = xmlDoc.createElement("intensityMetrics");
            intensity = new MetrixIntensityMetricsDecorator(mc.getSummary().getIntensityDistAvg(), mc.getSummary().getIntensityDistCCAvg()).toXML();
            sumXml.appendChild(intensity);
            
            // Add to main summary element for each run.
            root.appendChild(sumXml);
          }
          else {
            sumXml = new MetrixSummaryDecorator(mc.getSummary()).toXML(sumXml, xmlDoc);
            root.appendChild(sumXml);
          }
          iter.remove();
          LoggerWrapper.log.log(Level.INFO, "Removed from list.");
      }
        return root; 
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
    return root;
  }
}
