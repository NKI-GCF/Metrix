// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2014 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.parsers.metrix;

import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.*;

import org.w3c.dom.*;
import org.xml.sax.SAXException;
import nki.objects.Summary;
import nki.objects.PostProcess;
import nki.objects.FileOperation;
import nki.objects.Application;
import nki.util.LoggerWrapper;
import nki.util.PostProcessComparator;
import nki.util.OperationComparator;
import nki.util.FileOperations;

import java.nio.file.*;
import java.nio.file.Files;

import static java.nio.file.StandardCopyOption.*;

import java.util.logging.Level;
import nki.objects.DemuxOperation;

public class PostProcessing {
  private DocumentBuilderFactory documentBuilderFactory;
  private DocumentBuilder documentBuilder;
  private Document doc;
  private Summary sum;
  private String xmlFile;
  private List<PostProcess> blockList = new ArrayList<>();
  private List<PostProcess> processList = new ArrayList<>();
  private boolean isValid = true;
  private boolean hasFinished = false;

  private Map<String, String> tm = new HashMap<>();  // Template mapping collection for placeholder substitutions.
  // Instantiate Logger
  private static final LoggerWrapper metrixLogger = LoggerWrapper.getInstance();
  // Properties config
  private static final Properties configFile = new Properties();


  public PostProcessing(Summary s) {
    this.sum = s;  // Set summary
  }

  
  
  public void run() {
    LoggerWrapper.log.log(Level.INFO, "[Metrix Post-Processor] Initializing post processing for {0}", sum.getRunId());
    /*
		 *  Pre PP loaders and checks
		 */
    loadProperties();
    initXml();
    validateXmlStructure();
		
    /*
     *  Load post processing configuration
     */
    loadTemplate();

    /*
     *  Interpret processing
     */
    parseXmlStructure();

    /*
     *  Execute post processing
     */
    if (isValid) {
      executeProcessing();
    }
    else {
      LoggerWrapper.log.warning("[Metrix Post-Processor] One or more validation checks failed. Cannot execute processing.");
    }

    if (hasFinished) {
      LoggerWrapper.log.info("[Metrix Post-Processor] Post processing has successfully finished.");
    }
    else {
      LoggerWrapper.log.warning("[Metrix Post-Processor] One or more steps during post processing encountered errors. Please check your configuration.");
    }
  }

  private void loadProperties() {
    LoggerWrapper.log.finer("[Metrix Post-Processor] Loading properties...");
    try {
      // Use external properties file, outside of jar location.
      String externalFileName = System.getProperty("properties");
      String absFile = (new File(externalFileName)).getAbsolutePath();
      try (InputStream fin = new FileInputStream(new File(absFile))) {
        configFile.load(fin);
      }
      LoggerWrapper.log.log(Level.FINER, "[Metrix Post-Processor] Successfully loaded properties: {0}", absFile);
    }
    catch (FileNotFoundException FNFE) {
      LoggerWrapper.log.severe("[ERROR] Properties file not found.");
      isValid = false;
    }
    catch (IOException Ex) {
      LoggerWrapper.log.log(Level.SEVERE, "[ERROR] Reading properties file. {0}", Ex.toString());
      isValid = false;
    }
  }

  private void initXml() {
    LoggerWrapper.log.finer("[Metrix Post-Processor] Initializing post processing xml.");
    try {
      xmlFile = configFile.getProperty("POSTPROCESSING", "./postprocessing.mtx");
      documentBuilderFactory = DocumentBuilderFactory.newInstance();
      documentBuilder = documentBuilderFactory.newDocumentBuilder();
      doc = documentBuilder.parse(xmlFile);
      doc.getDocumentElement().normalize();
      LoggerWrapper.log.log(Level.FINER, "[Metrix Post-Processor] Successfully loaded post processing xml: {0}", new File(xmlFile).getAbsolutePath());
    }
    catch (IOException Ex) {
      LoggerWrapper.log.log(Level.SEVERE, "[Metrix Post-Processor] IOException. {0}", Ex.toString());
      isValid = false;
    }
    catch (SAXException SAX) {
      LoggerWrapper.log.log(Level.SEVERE, "[Metrix Post-Processor] SAXException. Cannot process post processing XML file ({0})", SAX.toString());
      isValid = false;
    }
    catch (ParserConfigurationException PCE) {
      LoggerWrapper.log.log(Level.SEVERE, "[Metrix Post-Processor] ParserConfigurationException. Cannot process post processing XML file ({0})", PCE.toString());
      isValid = false;
    }
  }

  /*
   * XML Structure validation method.
   * Contents of postprocessing.mtx (XML) are verified against postprocessing.xsd
   */
  private void validateXmlStructure() {
    LoggerWrapper.log.finer("[Metrix Post-Processor] Validating xml structure...");
    InputStream xml = null;
    InputStream xsd = null;
    try {
      // The required files:
      // - postprocessing.mtx	(properties file)
      // - postprocessing.xsd (default location in root)
      xml = new FileInputStream(xmlFile);
      String ppFormatPath = configFile.getProperty("POSTPROCESSING_FORMAT", "./postprocessing.xsd");
      String xsdFile = (new File(ppFormatPath).getAbsolutePath());
      xsd = new FileInputStream(xsdFile);

      SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      Schema schema = factory.newSchema(new StreamSource(xsd));
      Validator validator = schema.newValidator();
      validator.validate(new StreamSource(xml));
      LoggerWrapper.log.finer("[Metrix Post-Processor] Successfully validated XML structure.");
    }
    catch (IOException IO) {
      LoggerWrapper.log.log(Level.WARNING, "[Metrix Post-Processor] Failed to open required files. ({0})", IO.toString());
      isValid = false;
    }
    catch (SAXException ex) {  // Validation failed
      LoggerWrapper.log.log(Level.WARNING, "[Metrix Post-Processor] XML validation failed. ({0} || {1})", new Object[]{ex.toString(), ex.getLocalizedMessage()});
      isValid = false;
    }
    finally {
      try {
        if (xml != null) {
          xml.close();
        }

        if (xsd != null) {
          xsd.close();
        }
      }
      catch (IOException ex) {
        LoggerWrapper.log.log(Level.WARNING, "[Metrix Post-Processor] Error closing XML objects. ({0})", ex.toString());
      }
    }
  }

  private void loadTemplate() {
    LoggerWrapper.log.finer("[Metrix Post-Processor] Loading templates...");
    boolean check = true;

    tm.put("{TotalCycles}", Integer.toString(sum.getTotalCycles()));
    tm.put("{FlowcellID}", sum.getFlowcellID());
    tm.put("{FlowcellSide}", sum.getSide());
    tm.put("{SequencerRunNr}", Integer.toString(sum.getInstrumentRunNumber()));
    tm.put("{SequencerName}", sum.getInstrument());
    tm.put("{SequencerType}", sum.getInstrumentType());
    tm.put("{RunNameOptional}", sum.getRunNameOptional());
    tm.put("{RunDate}", Integer.toString(sum.getRunDate()));
    tm.put("{RunType}", sum.getRunType());
    tm.put("{RunDirectory}", sum.getRunDirectory());
    tm.put("{RunID}", sum.getRunId());
    tm.put("{DemuxIndex}", sum.getReads().getDemultiplexIndex());

    for (String a : tm.values()) {
      if (a == null) {
        LoggerWrapper.log.warning("[Metrix Post-Processor] Null values found in template set. Failed.");
        isValid = false;
        check = false;
      }
    }

    if (check) {
      LoggerWrapper.log.finer("[Metrix Post-Processor] Templates OK.");
    }
  }

  private void parseXmlStructure() {
    // By de facto the XSD validation should have caught the root element name formatting
    if (doc.getDocumentElement().getNodeName().equals("Metrix")) {
      NodeList nodeList = doc.getElementsByTagName("*");

      for (int i = 0; i < nodeList.getLength(); i++) {
        Node node = nodeList.item(i);
        if (node.getNodeName().equals("Metrix")) {
          continue;
        }
        if (node.getNodeType() == Node.ELEMENT_NODE) {
          String nodeName = node.getNodeName();
          if (nodeName.equals("FileOperation") || nodeName.equals("Application") || nodeName.equals("DemuxOperation")) {
            // Do block node processing
            PostProcess processBlock = new PostProcess(node);
            blockList.add(processBlock);

            NodeList foChildren = node.getChildNodes();
            // DemuxOperation is a singular process and does not contain any childnodes by default.
            if(nodeName.equals("DemuxOperation")){
                node = mapTemplate(node);
                DemuxOperation dmxObject = new DemuxOperation(node);
                processList.add(dmxObject);
            }else{
                // Iterate over defined children and create do Operation node processing.
                for (int x = 0; x < foChildren.getLength(); x++) {
                  Node fo = foChildren.item(x);
                  if (fo.getNodeName().equals("#text") || fo.getNodeName().equals("#comment")) {
                    continue;
                  }
                  switch (nodeName) {
                    case "FileOperation":
                      fo = mapTemplate(fo);
                      FileOperation foObject = new FileOperation(node, fo);
                      processList.add(foObject);
                      break;
                    case "Application":
                      fo = mapTemplate(fo);
                      Application appObject = new Application(node, fo);
                      processList.add(appObject);
                      break;
                  }
                }
            }
          }
        }
        else {
          isValid = false;
          LoggerWrapper.log.severe("[Metrix Post-Processor] Node Mismatch. This should not be possible");
        }
      }
      LoggerWrapper.log.finest("[Metrix Post-Processor] Sorting the process list...");
      Collections.sort(blockList, new PostProcessComparator());
      Collections.sort(processList, new OperationComparator<>());
      LoggerWrapper.log.finest("[Metrix Post-Processor] Done sorting.");
    }
    else {
      isValid = false;
      LoggerWrapper.log.severe("[Metrix Post-Processor] Incompatible document structure. This should not be possible.");
    }
  }

  private Node mapTemplate(Node childNode) {
    if (!childNode.hasChildNodes()) {
      return childNode;
    }
    NodeList childNodeList = childNode.getChildNodes();

    for (int i = 0; i < childNodeList.getLength(); i++) {
      Node innerNode = childNodeList.item(i);
      Node replacementNode = innerNode;

      if (innerNode.getNodeName().equals("#text")) {
        continue;
      }
      String oldVal = innerNode.getTextContent();

      if (oldVal == null) {
        continue;
      }
      Pattern pattern = Pattern.compile("\\{(.+?)\\}");
      Matcher matcher = pattern.matcher(oldVal);
      StringBuffer buffer = new StringBuffer();
      while (matcher.find()) {
        String replacement = tm.get(matcher.group());
        if (replacement != null) {
          matcher.appendReplacement(buffer, "");
          buffer.append(replacement);
        }
      }
      matcher.appendTail(buffer);

      replacementNode.setTextContent(buffer.toString());
      childNode.replaceChild(replacementNode, innerNode);
    }
    return childNode;
  }

  private void executeProcessing() {
    int status = 0;

    for (PostProcess pp : processList) {
      if (pp instanceof FileOperation) {
        FileOperation fo = (FileOperation) pp;
        if (fo.isValid()) {
          status += executeFileOperation(fo);
        }
        else {
          LoggerWrapper.log.log(Level.WARNING, "[Metrix Post-Processor] FileOperation {0} not populated correctly. Source and/or destination are empty.", fo.getTitle());
        }
      }else if (pp instanceof DemuxOperation) {
        DemuxOperation dmx = (DemuxOperation) pp;
        status += executeDemuxOperation(dmx);
      }
      else if (pp instanceof Application) {
        Application app = (Application) pp;
        status += executeApplication(app);
      }
    }
    if (status == 0) {
      hasFinished = true;
    }
  }

   private int executeDemuxOperation(DemuxOperation dmx) {
    // Default exitstatus 0 for return without encountering any errors during processing.
    int exitStatus = -255;
    
    LoggerWrapper.log.log(Level.INFO, "[Metrix Post-Processor] Starting script: {0}", dmx.getTitle());
    // The script / application to execute
    if(dmx.getBclToFastQPath().equals("")){
        LoggerWrapper.log.log(Level.WARNING, "[Metrix Post-Processor] Cannot find the Bcl conversion script. Argument is not set in postprocessing.");
        return exitStatus;
    }
    
    exitStatus = 0;
    // Split SampleSheet based on projects.
    try{
        dmx.generateSampleSheets();
    }catch(FileNotFoundException FNFE){
        LoggerWrapper.log.log(Level.SEVERE, "[Metrix Post-Processor] Samplesheets could not be generated, the untransformed samplesheet is not present. {0}", dmx.getSampleSheetPath());
    }catch(IOException ex){
        LoggerWrapper.log.log(Level.SEVERE, "[Metrix Post-Processor] IOException during samplesheet generation ", ex);
    }
    // Demux each project.
    for(File ss : dmx.getSamplesheetLocations()){
        // Demultiplex every samplesheet generated by DemuxOperation
        processSamplesheet(dmx, ss);
    }
    
    return exitStatus;
  }
  
  private int processSamplesheet(DemuxOperation dmx, File samplesheet){
    int exitStatus = 0;
    String sampleSheetNameNormal = samplesheet.getName().replace(".csv", "");
    LoggerWrapper.log.log(Level.FINE, "[Metrix Post-Processor] Starting demultiplexing for: {0}", sampleSheetNameNormal);
    
    // The output file. All console activity is written to this file.
    final File loggingFile;
    
    if(!dmx.getLoggingPath().equals("")){
        loggingFile = new File(dmx.getLoggingPath());
    }else{
        loggingFile = new File(dmx.getBaseOutputDir() +"/"+sampleSheetNameNormal+ ".postprocessing.log");
    }
    
    // Does Data output folder exist?
    File dmxBaseOut = new File(dmx.getBaseOutputDir() + "/Demux/" + sampleSheetNameNormal + "/");
    
    if(!dmxBaseOut.exists()){
        if(dmxBaseOut.mkdirs()){
            LoggerWrapper.log.log(Level.FINE, "[Metrix Post-Processor] Successfuly created folder {0}", dmxBaseOut);
        }else{
            LoggerWrapper.log.log(Level.WARNING, "[Metrix Post-Processor] Error creating directory! ");
        }
    }
    
    ArrayList<String> cmd = new ArrayList<>();
    // Add script path to command
    cmd.add(dmx.getBclToFastQPath());
    // Define other base arguments and build
    cmd.add("--input-dir");
    cmd.add(dmx.getBaseWorkingDir());
    cmd.add("--output-dir");
    cmd.add(dmxBaseOut.toString());
    cmd.add("--sample-sheet");
    cmd.add(samplesheet.getAbsolutePath());
    cmd.add("--use-bases-mask");
    cmd.add(dmx.ssBaseMaskMap.get(samplesheet.getAbsolutePath()));
    cmd.add("--force");
    
    LoggerWrapper.log.log(Level.FINE, "Script: {0}\nInput: {1}\nOutput: {2}\nSamplesheet: {3}\nBaseMask: {4}", new Object[]{dmx.getBclToFastQPath(), dmx.getBaseWorkingDir(), dmxBaseOut.toString(), samplesheet.getAbsolutePath(), dmx.ssBaseMaskMap.get(samplesheet.getAbsolutePath())});
    
    if(dmx.getArguments() != null){
        String[] spl = dmx.getArguments().split(" ");
        cmd.addAll(Arrays.asList(spl));
    }
    
    ProcessBuilder pb = new ProcessBuilder(cmd);
    LoggerWrapper.log.log(Level.INFO, "Executing : {0}", pb.command());
    
    if(dmx.getBaseWorkingDir() != null){
        LoggerWrapper.log.log(Level.FINE, "[Metrix Post-Processor] Setting base working directory for postprocessing to: {0}", dmx.getBaseWorkingDir());
        pb.directory(new File(dmx.getBaseWorkingDir())); // Data/Intensities/BaseCalls
    }
   
    // Instantiate the ProcessBuilder
    pb.redirectOutput(loggingFile);
    pb.redirectErrorStream(true);
    
    try {
      LoggerWrapper.log.log(Level.FINE, "[Metrix Post-Processor] Starting process for: {0}", pb.command());
      Process p = pb.start();
      // Start the process and wait for it to finish.
      exitStatus = p.waitFor();
    }
    catch (IOException IO) {
      LoggerWrapper.log.log(Level.SEVERE, "[Metrix Post-Processor] IOException while executing process ({0}): {1}", new Object[]{dmx.getId(), IO.toString()});
    }
    catch (InterruptedException IE) {
      LoggerWrapper.log.log(Level.SEVERE, "[Metrix Post-Processor] InterruptedException for process ( {0}): {1}", new Object[]{dmx.getId(), IE.toString()});
    }
    if(exitStatus == 0){
        LoggerWrapper.log.log(Level.FINE, "[Metrix Post-Processor] ConfigureBclToFastQ configured successfully - Starting make...");
        ArrayList<String> cmdMake = new ArrayList<>();
        cmdMake.addAll(Arrays.asList(dmx.getMakePath().split(" ")));
        
        String[] makeArgs = dmx.getMakeArguments().split(" ");
        cmdMake.addAll(Arrays.asList(makeArgs));

        ProcessBuilder pbMake = new ProcessBuilder(cmdMake);
        LoggerWrapper.log.log(Level.FINE, "[Metrix Post-Processor] Starting Make for : {0}", pbMake.command());
        pbMake.directory(dmxBaseOut);

        // Instantiate the ProcessBuilder for make
        pbMake.redirectOutput(new File(dmx.getBaseOutputDir() +"/"+samplesheet.getName().replace(".csv", "") +".make.log"));
        pbMake.redirectErrorStream(true);

        try {
          Process pMake = pbMake.start();
          // Start the process and wait for it to finish.
          exitStatus += pMake.waitFor(); 
        }
        catch (IOException IO) {
          LoggerWrapper.log.log(Level.SEVERE, "[Metrix Post-Processor] IOException while executing process ({0}): {1}", new Object[]{dmx.getId(), IO.toString()});
        }
        catch (InterruptedException IE) {
          LoggerWrapper.log.log(Level.SEVERE, "[Metrix Post-Processor] InterruptedException for process ( {0}): {1}", new Object[]{dmx.getId(), IE.toString()});
        }    

        if (exitStatus == 0) {
          LoggerWrapper.log.log(Level.INFO, "[Metrix Post-Processor] Application block ({0} :: {1} :: {2}) has finished successfully.  ", new Object[]{dmx.getOrder(), dmx.getSubOrder(), dmx.getId()});
        }
        else {
          LoggerWrapper.log.log(Level.WARNING, "[Metrix Post-Processor] Application block ''{0}'' has exited with errors.", dmx.getId());
        }
    }
    
    return exitStatus;
  }
   
  private int executeFileOperation(FileOperation fo) {
    LoggerWrapper.log.log(Level.INFO, "[Metrix Post-Processor] Starting file operation: {0}", fo.getTitle());
    int exitStatus = -255;

    File sourceFile = new File(fo.getSource());
    File destinationFile = new File(fo.getDestination());

    boolean sourceIsDir = false;
    boolean destinationIsDir = false;

    if (!sourceFile.exists()) {
      LoggerWrapper.log.log(Level.WARNING, "[Metrix Post-Processor] Source file: {0} does not exist.", sourceFile);
      exitStatus = -1;
      return exitStatus;
    }

    if (!sourceFile.canRead()) {
      LoggerWrapper.log.warning("[Metrix Post-Processor] Cannot read source file.");
      exitStatus = -1;
      return exitStatus;
    }
    else {
      LoggerWrapper.log.log(Level.FINEST, "[Metrix Post-Processor] Source file: {0} is readable.", sourceFile.toString());
    }

    // If destination is file (assuming that all destination files will have an extension)
    // 	Check parent
    File targetDir;
    if ((destinationFile.toString()).lastIndexOf(".") > 0) {
      destinationFile.getParentFile().mkdirs();
      targetDir = destinationFile.getParentFile();
    }
    else { // If destination is a directory
      targetDir = destinationFile;
      destinationFile.mkdirs();
      destinationIsDir = true;
      fo.setDestination(destinationFile + File.separator + sourceFile.getName());
      destinationFile = new File(fo.getDestination());
    }

    File sourceDir = null;
    if ((sourceFile.toString()).lastIndexOf(".") == -1) {
      sourceDir = sourceFile;
      sourceIsDir = true;
    }

    if (targetDir.canWrite()) {
      LoggerWrapper.log.info("[Metrix Post-Processor] Destination path is writeable.");
    }
    else {
      LoggerWrapper.log.log(Level.INFO, "[Metrix Post-Processor] Destination path is not writeable. {0}", targetDir);
      exitStatus = -1;
      return exitStatus;
    }

    if (fo.needOverwrite() && destinationFile.isFile()) {
      LoggerWrapper.log.info("[Metrix Post-Processor] Destination already exists. Overwriting.");
    }
    else if (!fo.needOverwrite() && !destinationFile.exists()) {
      LoggerWrapper.log.log(Level.INFO, "[Metrix Post-Processor] Creating file during copy: {0}", sourceFile.getName());
    }
    else if ((sourceDir != null) && (fo.hasGlobbing())) {
      LoggerWrapper.log.log(Level.INFO, "[Metrix Post-Processor] Copying files with extension: {0} from: {1}to: {2}", new Object[]{fo.getGlobbing(), sourceFile.getAbsoluteFile(), destinationFile});
    }
    else if ((sourceDir != null) && !(fo.hasGlobbing())) {
      LoggerWrapper.log.log(Level.INFO, "[Metrix Post-Processor] Copying directory: {0}\tto: {1}", new Object[]{sourceFile, destinationFile});
    }
    else if (destinationIsDir && destinationFile.exists() && sourceFile.isFile()) {
      LoggerWrapper.log.info("[Metrix Post-Processor] Source is a file - destination is directory.");
    }
    else if (destinationIsDir && fo.needOverwrite() && sourceFile.isFile()) {
      LoggerWrapper.log.info("[Metrix Post-Processor] Source is a file - destination is directory - Overwriting file.");
    }
    else {
      LoggerWrapper.log.info("[Metrix Post-Processor] Destination file already exists. Not overwriting.");
      exitStatus = -1;
      return exitStatus;
    }

    /*
    *  Is process a copy operation?
    */
    if (fo.isCopyOperation()) {

      // Is globbing used?
      if (fo.hasGlobbing()) {
        LoggerWrapper.log.log(Level.INFO, "[Metrix Post-Processor] Using globbing pattern: {0}", fo.getGlobbing());
        List<Path> foundFiles = findFilesGlobbing(Paths.get(fo.getSource()), fo.getGlobbing());

        // Execute the basic copy  operation for files that have been found with a pattern.
        exitStatus = executeGlobbingCopy(fo, foundFiles);
        return exitStatus;
      }

      try {
        CopyOption[] options = new CopyOption[]{};
        CopyOption[] replOpt = new CopyOption[]{
            COPY_ATTRIBUTES,
            REPLACE_EXISTING,
            java.nio.file.LinkOption.NOFOLLOW_LINKS
        };

        CopyOption[] stdOpt = new CopyOption[]{
            COPY_ATTRIBUTES,
            java.nio.file.LinkOption.NOFOLLOW_LINKS
        };

        // Overwrite in file operations?
        options = !fo.needOverwrite() ? stdOpt : replOpt;

        // Is the source path a file?
        if (sourceFile.isFile()) {
          LoggerWrapper.log.log(Level.FINE, "Copying {0}to: {1}", new Object[]{sourceFile, destinationFile});
          Files.copy(
              sourceFile.toPath(),
              destinationFile.toPath(),
              options
          );
          exitStatus = 0;
        }
        else if (sourceFile.isDirectory()) { // Is the sourcepath a directory?
          FileOperations fileops =
              new FileOperations(
                  sourceFile.toPath(),
                  destinationFile.toPath(),
                  options
              );

          try {
            fileops.recursiveCopy();
            exitStatus = 0;
          }
          catch (IOException IO) {
            LoggerWrapper.log.log(Level.WARNING, "[Metrix Post-Processor] IO Exception in recursive copy: {0}", IO.toString());
            exitStatus = -1;
          }

        }
        else {
          LoggerWrapper.log.warning("[Metrix Post-Processor] Source file does not exist. ");
          exitStatus = -1;
          return exitStatus;
        }
      }
      catch (IOException Iex) {
        LoggerWrapper.log.log(Level.WARNING, "[Metrix Post-Processor] IOException during file copy: {0}", Iex.toString());
        exitStatus = -1;
      }

      if (exitStatus == 0) {
        LoggerWrapper.log.log(Level.FINER, "[Metrix Post-Processor] Successfully copied {0} to {1}", new Object[]{sourceFile, destinationFile});
      }
    }

    /*
    * Is process a symlink operation?
    */
    if (fo.isSymlinkOperation()) {
      try {
        Files.createSymbolicLink(sourceFile.toPath(), destinationFile.toPath());
      }
      catch (IOException x) {
        LoggerWrapper.log.log(Level.WARNING, "[Metrix Post-Processor] IOException while creating a symbolic link: {0}", x.toString());
      }
      catch (UnsupportedOperationException x) {
        // Some file systems do not support symbolic links.
        LoggerWrapper.log.log(Level.WARNING, "[Metrix Post-Processor] This OS does not support creating symbolic links: {0}", x.toString());
      }
    }

    /*
    * Process is another operation type
    */
    // Not yet implemented.

    if (exitStatus == 0) {
      LoggerWrapper.log.log(Level.INFO, "[Metrix Post-Processor] File Operation block ({0} :: {1} :: {2}) has finished successfully.", new Object[]{fo.getOrder(), fo.getSubOrder(), fo.getId()});
    }
    else {
      LoggerWrapper.log.log(Level.WARNING, "[Metrix Post-Processor] File Operation block ''{0}'' has exited with errors.", fo.getId());
    }

    return exitStatus;
  }

  private List<Path> findFilesGlobbing(Path sourcePath, String pattern) {
    LoggerWrapper.log.log(Level.FINE, "[Metrix Post-Processor] Finding files with globbing pattern: {0} in {1}", new Object[]{pattern, sourcePath});
    FileOperations fileops = new FileOperations(sourcePath, pattern);

    try {
      fileops.findFilesGlobbing();
    }
    catch (IOException Ex) {
      LoggerWrapper.log.log(Level.WARNING, "[Metrix Post-Processor] IOException during globbing find operation. {0}", Ex.toString());
    }

    LoggerWrapper.log.log(Level.INFO, "[Metrix Post-Processor] Found {0} files.", fileops.getResultsSize());
    return fileops.getResults();
  }

  private int executeGlobbingCopy(FileOperation fo, List<Path> fileList) {
    File destinationFile = new File(fo.getDestination());
    int exitStatus = 0;

    CopyOption[] options = new CopyOption[]{};
    CopyOption[] replOpt = new CopyOption[]{
        COPY_ATTRIBUTES,
        REPLACE_EXISTING,
        java.nio.file.LinkOption.NOFOLLOW_LINKS
    };

    CopyOption[] stdOpt = new CopyOption[]{
        COPY_ATTRIBUTES,
        java.nio.file.LinkOption.NOFOLLOW_LINKS
    };


    if (!destinationFile.isDirectory()) {
      LoggerWrapper.log.fine("[Metrix Post-Processor] Destination path is not a directory.");
      return -1;
    }

    for (Path sourcePath : fileList) {
      options = !fo.needOverwrite() ? stdOpt : replOpt;
      try {
        Files.copy(
            sourcePath,
            (destinationFile.toPath()).resolve(sourcePath),
            options
        );
        exitStatus += 0;
      }
      catch (IOException Ex) {
        LoggerWrapper.log.log(Level.WARNING, "[Metrix Post-Processor] IOException in globbing file copy: {0}", Ex.toString());
        exitStatus += -1;
      }

      LoggerWrapper.log.log(Level.FINER, "[Metrix Post-Processor] Copied: {0} to: {1}", new Object[]{sourcePath, destinationFile});
    }

    return exitStatus; // All is good
  }

  private int executeApplication(Application app) {
    ArrayList<String> cmd = new ArrayList<>();
    LoggerWrapper.log.log(Level.INFO, "[Metrix Post-Processor] Starting script: {0}", app.getTitle());
    // The script / application to execute
    cmd.addAll(Arrays.asList(app.getScriptPath().split(" ", -1)));
    
    // The output file. All application activity is written to this file.
    final File outputFile = new File(app.getOutputPath());
    // The supplied arguments for the script
    cmd.addAll(Arrays.asList(app.getArguments().split(" ", -1)));
    ProcessBuilder pb = new ProcessBuilder(cmd);
    
    if(app.getWorkingDirectory() != null){
        pb.directory(new File(app.getWorkingDirectory()));
    }
    
    // Instantiate the ProcessBuilder
    pb.redirectOutput(outputFile);
    pb.redirectErrorStream(true);
    
    // Default exitstatus -255 for return without processing.
    int exitStatus = -255;

    try {
      Process p = pb.start();
      // Start the process and wait for it to finish.
      exitStatus = p.waitFor();
    }
    catch (IOException IO) {
      LoggerWrapper.log.log(Level.SEVERE, "[Metrix Post-Processor] IOException while executing process ({0}): {1}", new Object[]{app.getId(), IO.toString()});
    }
    catch (InterruptedException IE) {
      LoggerWrapper.log.log(Level.SEVERE, "[Metrix Post-Processor] InterruptedException for process ( {0}): {1}", new Object[]{app.getId(), IE.toString()});
    }

    if (exitStatus == 0) {
      LoggerWrapper.log.log(Level.INFO, "[Metrix Post-Processor] Application block ({0} :: {1} :: {2}) has finished successfully.  ", new Object[]{app.getOrder(), app.getSubOrder(), app.getId()});
    }
    else {
      LoggerWrapper.log.log(Level.WARNING, "[Metrix Post-Processor] Application block ''{0}'' has exited with errors.", app.getId());
    }

    return exitStatus;
  }
}

/*	Possible placeholders for parameters or variables.
	// Placeholder options
	- {TotalCycles}
	- {FlowcellID}
	- {FlowcellSide}
	- {SequencerRunNr}
	- {SequencerName}
	- {SequencerType}		
	- {RunDate}
	- {RunType}
	- {RunDirectory}
	- {RunID}
	- {DemuxIndex}
*/
