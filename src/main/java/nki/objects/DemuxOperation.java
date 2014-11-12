// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2014 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.objects;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import nki.util.LoggerWrapper;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class DemuxOperation extends PostProcess {

  public static final long serialVersionUID = 42L;
  private String bclToFastQPath;
  private String hiseqHeader = "FCID,Lane,SampleID,SampleReferenceGenome,Index,Descr,Control,Recipe,Operator,SampleProject";
  private String arguments;
  private String baseRunDir;
  private String baseOutputDir;
  private String baseWorkingDir;
  private String loggingPath;
  private String makePath;
  private String makeArguments;
  private String splitBy = "project"; // Split by project or lane
  private ArrayList<File> samplesheetLocations;
  private String baseMask;  
  private List<String[]> fullSamplesheet = new ArrayList<>();
  private HashMap<Object, ArrayList<String>> sampleSheets = new HashMap<>(); // Store samplesheets by splitBy type.
  private HashMap<Object, String> baseMaskMap = new HashMap<>(); // Store baseMasks for every samplesheet generated.
  
  public DemuxOperation(Node parentNode, Node childNode) {
    NamedNodeMap parentAttr = parentNode.getAttributes();
    NamedNodeMap childAttr = childNode.getAttributes();

    // Set attribute values of inherited PostProcess
    this.setOrder(Integer.parseInt(parentAttr.getNamedItem("execOrder").getNodeValue()));
    this.setSubOrder(Integer.parseInt(childAttr.getNamedItem("execOrder").getNodeValue()));
    this.setId(childAttr.getNamedItem("id").getNodeValue());
    this.setTitle(childAttr.getNamedItem("title").getNodeValue());

    NodeList foProps = childNode.getChildNodes();

    for (int i = 0; i < foProps.getLength(); i++) {
      // Iterate over node properties
      Node p = foProps.item(i);
      LoggerWrapper.log.log(Level.INFO, "Prop: " + p.getNodeName() + " - " + p.getTextContent());
      
      if (p.getNodeName().equalsIgnoreCase("BclToFastQPath")) {
        this.setBclToFastQPath(p.getTextContent());
      }
      else if (p.getNodeName().equalsIgnoreCase("Arguments")) {
        this.setArguments(p.getTextContent());
      }
      else if (p.getNodeName().equalsIgnoreCase("BaseMask")) {
        this.setBaseMask(p.getTextContent());
      }
      else if (p.getNodeName().equalsIgnoreCase("BaseRunDir")) {
        LoggerWrapper.log.log(Level.INFO, "Setting Base run dir " + p.getTextContent());
        this.setBaseRunDir(p.getTextContent());
      }
      else if (p.getNodeName().equalsIgnoreCase("BaseOutputDir")) {
        this.setBaseOutputDir(p.getTextContent());
      }
      else if (p.getNodeName().equalsIgnoreCase("BaseWorkingDir")) {
        this.setBaseWorkingDir(p.getTextContent());
      }
      else if (p.getNodeName().equalsIgnoreCase("MakePath")) {
        this.setMakePath(p.getTextContent());
      }
      else if (p.getNodeName().equalsIgnoreCase("MakeArguments")) {
        this.setMakeArguments(p.getTextContent());
      }
      else if (p.getNodeName().equalsIgnoreCase("LoggingPath")) {
        this.setLoggingPath(p.getTextContent());
      }
      else if (p.getNodeName().equalsIgnoreCase("SplitBySamplesheetColumn")) {
        this.setSplitBy(p.getTextContent());
      }
    }
  }

  public void setBclToFastQPath(String sp) {
    this.bclToFastQPath = sp;
  }

  public String getBclToFastQPath() {
    return this.bclToFastQPath;
  }

  public void setArguments(String arg) {
    this.arguments = arg;
  }

  public String getArguments() {
    return this.arguments;
  }

  public void setBaseRunDir(String baseRunDir) {
    this.baseRunDir = baseRunDir;
  }

  public String getBaseRunDir() {
    return this.baseRunDir;
  }
  
  public void setBaseOutputDir(String baseOutputDir) {
    this.baseOutputDir = baseOutputDir;
  }

  public String getBaseOutputDir() {
    return this.baseOutputDir;
  }

  public void setBaseWorkingDir(String baseWorkingDir) {
    this.baseWorkingDir = baseWorkingDir;
  }

  public void setBaseMask(String baseMask) {
    this.baseMask = baseMask;
  }

  public String getBaseMask() {
    return this.baseMask;
  }    
  
  public String getBaseWorkingDir() {
    return this.baseOutputDir;
  }  
  
  public File getSampleSheetPath(){
      if(baseRunDir != null || !baseRunDir.equals("")){
        return new File(getBaseRunDir() + "/SampleSheet.csv");
      }else{
        return new File("./SampleSheet.csv");
      }
  }
  
  public void setLoggingPath(String loggingPath){
    this.loggingPath = loggingPath;
  }

  public String getLoggingPath() {
    return this.loggingPath;
  }
  
  public void setMakePath(String makePath) {
    this.makePath = makePath;
  }

  public String getMakePath() {
    return this.makePath;
  }
  
  public void setMakeArguments(String makeArguments) {
    this.makeArguments = makeArguments;
  }

  public String getMakeArguments() {
    return this.makeArguments;
  }  
  
  public void setSplitBy(String splitBy){
      this.splitBy = splitBy;
  }
  
  public String getSplitBy(){
      if(this.splitBy != null || this.splitBy.equals("")){
        return this.splitBy;
      }else{
          return "project";
      }
  }
  
  public ArrayList<File> getSamplesheetLocations(){
      return this.samplesheetLocations;
  }
  
  public boolean generateSampleSheets() throws FileNotFoundException, IOException{
    LoggerWrapper.log.log(Level.INFO, "Initializing Samplesheet processing for demultiplexing...");
    String[] nextLine;
    
    if(!this.getSampleSheetPath().exists()){
        throw new FileNotFoundException();
    }
    
    // Read full CSV.
    BufferedReader br = new BufferedReader(new FileReader(this.getSampleSheetPath()));
    while ((nextLine = br.readLine().split(",")) != null) {
       fullSamplesheet.add(nextLine);
    }
  
    // Determine header first (type of SampleSheet)
    String firstLineWord = fullSamplesheet.get(0)[0];
    
    LoggerWrapper.log.log(Level.FINE, "First line - {0}", Arrays.toString(fullSamplesheet.get(0)));
    LoggerWrapper.log.log(Level.FINE, "First word - {0}", firstLineWord);
    
    switch (firstLineWord) {
        // MiSeq format
        // Grab all contents and transform to Hiseq format.
        case "[Header]":
            transformMiSeq();
            break;
        // HiSeq format - Split by selected splitBy type.
        case "FCID":
            transformHiSeq();
            // Skip header line.
            break;
        default:
            LoggerWrapper.log.log(Level.FINE, "Unknown type - '{0}'", firstLineWord);
            return false;
    }
    
    return true;
  }
  
  public void transformMiSeq(){
      // Group by project (zero based)
      int lineIdx = 6;
      boolean addToSet = false;
      boolean foundData = false;
      // MiSeq only has one lane. Add to 1.
      if(getSplitBy().equals("lane")){
          // Just transform, everything is present in one lane.
          lineIdx = -1;
      }
      
      ArrayList<String> ssContents;
      
      for(String[] line : fullSamplesheet){
          Object splitValue;
          // Split by lane
          if(lineIdx == -1){
              splitValue = 1;
          }else{
              // Normally split by project (idx: 6)
              splitValue = line[lineIdx];
          }
          
          if(line[0].equals("[Data]")){
            foundData = true;
            continue;
          }
          if(foundData){
              addToSet = true;
              continue;
          }
          
          if(addToSet){
            if(sampleSheets.get(splitValue) == null){
                ssContents = new ArrayList<>();
                // Add default HiSeq header for demultiplexable samplesheets.
                ssContents.add(hiseqHeader);
            }else{
                ssContents = sampleSheets.get(line[lineIdx]);
            }
            
            ssContents.add(",1,"+line[1]+",,"+line[5]+","+line[7]+",,,Metrix,"+line[6]);
            sampleSheets.put(line[lineIdx], ssContents);
          }
      }
      
      printSamplesheets();      
  }
  
  public void transformHiSeq(){
      LoggerWrapper.log.log(Level.FINE, "Starting HiSeq Transformation and splitting into multiple samplesheets.");
      // Group by index (zero based)
      int lineIdx = 9; // Default to project based splitting.
              
      if(getSplitBy().equals("lane")){
         // Split lane id is in the second column.
          lineIdx = 1;
      }
      
      ArrayList<String> ssContents;
      
      for(String[] line : fullSamplesheet){
          ssContents = null;
          if(sampleSheets.get(line[lineIdx]) == null){
              ssContents = new ArrayList<>();
              // Add default HiSeq header.
              ssContents.add(hiseqHeader);
          }else{
              ssContents = sampleSheets.get(line[lineIdx]);
          }
         
          if(line[lineIdx] != null || !line[lineIdx].equals("") && !line[0].equals("FCID")){
            // Add the line to the contents of the samplesheet.
            StringBuilder builder = new StringBuilder();
            for(String s : line) {
                builder.append(s);
            }
            ssContents.add(builder.toString());
          }
          
          sampleSheets.put(line[lineIdx], ssContents);
      }
      
      printSamplesheets();
    }
  
  private void printSamplesheets(){
      File ssBaseDirectory = new File(getBaseOutputDir()+"/SampleSheets/");
      
      // Check if SampleSheets Directory exists if not, create it.
      if(!ssBaseDirectory.exists()){
          LoggerWrapper.log.log(Level.FINE, "SampleSheets folder does not exist, creating...");
          ssBaseDirectory.mkdirs();
      }
      
      ArrayList<String> ssContents;
      // Foreach samplesheet key generated new samplesheet with contents.
      for(Object key : sampleSheets.keySet()){
           ssContents = sampleSheets.get(key);
           try{
                String filename = "";
                if(getSplitBy().equals("lane")){
                    filename = ssBaseDirectory.toString()+"/L"+key.toString();
                }else{
                    filename = ssBaseDirectory.toString()+"/"+key.toString();
                }
                File samplesheetOut = new File(filename);
                samplesheetLocations.add(samplesheetOut);
                
                PrintWriter out = new PrintWriter(new FileWriter(samplesheetOut));
                
                // Write each string in the array on a separate line
                for (String s : ssContents) {
                    out.println(s + "\n");
                }
                
                out.close();
           }catch(IOException ioe){
               LoggerWrapper.log.log(Level.WARNING, "Error while create and or writing to samplesheet. {0}", ioe);
           }
        }
      
  }
}

