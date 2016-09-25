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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import org.xml.sax.SAXException;
import nki.objects.Summary;
import nki.objects.PostProcess;
import nki.objects.FileOperation;
import nki.objects.Application;
import nki.util.PostProcessComparator;
import nki.util.OperationComparator;
import nki.util.FileOperations;

import java.nio.file.*;
import java.nio.file.Files;

import static java.nio.file.StandardCopyOption.*;

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

  private Map<String, String> tm = new HashMap<>(); // Template mapping
                                                    // collection for
                                                    // placeholder
                                                    // substitutions.
  // Instantiate Logger
  protected static final Logger log = LoggerFactory.getLogger(PostProcessing.class);
  // Properties config
  private static final Properties configFile = new Properties();

  public PostProcessing(Summary s) {
    this.sum = s; // Set summary
  }

  public void run() {
    log.info("Initializing post processing for " + sum.getRunId());
    /*
     * Pre PP loaders and checks
     */
    loadProperties();
    initXml();
    validateXmlStructure();

    /*
     * Load post processing configuration
     */
    loadTemplate();

    /*
     * Interpret processing
     */
    parseXmlStructure();

    /*
     * Execute post processing
     */
    if (isValid) {
      executeProcessing();
    }
    else {
      log.warn("[Metrix Post-Processor] One or more validation checks failed. Cannot execute processing.");
    }

    if (hasFinished) {
      log.info("[Metrix Post-Processor] Post processing has successfully finished.");
    }
    else {
      log.warn("[Metrix Post-Processor] One or more steps during post processing encountered errors. Please check your configuration.");
    }
  }

  private void loadProperties() {
    log.debug("[Metrix Post-Processor] Loading properties...");
    try {
      // Use external properties file, outside of jar location.
      String externalFileName = System.getProperty("properties");
      String absFile = (new File(externalFileName)).getAbsolutePath();
      try (InputStream fin = new FileInputStream(new File(absFile))) {
        configFile.load(fin);
      }
      log.debug("[Metrix Post-Processor] Successfully loaded properties: " + absFile);
    }
    catch (FileNotFoundException FNFE) {
      log.error("Properties file not found.", FNFE);
      isValid = false;
    }
    catch (IOException Ex) {
      log.error("Reading properties file.", Ex);
      isValid = false;
    }
  }

  private void initXml() {
    log.debug("[Metrix Post-Processor] Initializing post processing xml.");
    try {
      xmlFile = configFile.getProperty("POSTPROCESSING", "./postprocessing.mtx");
      documentBuilderFactory = DocumentBuilderFactory.newInstance();
      documentBuilder = documentBuilderFactory.newDocumentBuilder();
      doc = documentBuilder.parse(xmlFile);
      doc.getDocumentElement().normalize();
      log.debug("[Metrix Post-Processor] Successfully loaded post processing xml: " + new File(xmlFile).getAbsolutePath());
    }
    catch (IOException Ex) {
      log.error("[Metrix Post-Processor] Post processing XML", Ex);
      isValid = false;
    }
    catch (SAXException SAX) {
      log.error("[Metrix Post-Processor] Cannot process post processing XML file.", SAX);
      isValid = false;
    }
    catch (ParserConfigurationException PCE) {
      log.error("[Metrix Post-Processor] Cannot process post processing XML file.", PCE);
      isValid = false;
    }
  }

  /*
   * XML Structure validation method. Contents of postprocessing.mtx (XML) are
   * verified against postprocessing.xsd
   */
  private void validateXmlStructure() {
    log.debug("[Metrix Post-Processor] Validating xml structure...");
    InputStream xml = null;
    InputStream xsd = null;
    try {
      // The required files:
      // - postprocessing.mtx (properties file)
      // - postprocessing.xsd (default location in root)
      xml = new FileInputStream(xmlFile);
      String ppFormatPath = configFile.getProperty("POSTPROCESSING_FORMAT", "./postprocessing.xsd");
      String xsdFile = (new File(ppFormatPath).getAbsolutePath());
      xsd = new FileInputStream(xsdFile);

      SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      Schema schema = factory.newSchema(new StreamSource(xsd));
      Validator validator = schema.newValidator();
      validator.validate(new StreamSource(xml));
      log.debug("[Metrix Post-Processor] Successfully validated XML structure.");
    }
    catch (IOException IO) {
      log.warn("[Metrix Post-Processor] Failed to open required files.", IO);
      isValid = false;
    }
    catch (SAXException ex) { // Validation failed
      log.warn("[Metrix Post-Processor] XML validation failed.", ex);
      isValid = false;
    } finally {
      try {
        if (xml != null) {
          xml.close();
        }

        if (xsd != null) {
          xsd.close();
        }
      }
      catch (IOException ex) {
        log.warn("[Metrix Post-Processor] Error closing XML objects.", ex);
      }
    }
  }

  private void loadTemplate() {
    log.debug("[Metrix Post-Processor] Loading templates...");
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
        log.warn("[Metrix Post-Processor] Null values found in template set. Failed.");
        isValid = false;
        check = false;
      }
    }

    if (check) {
      log.debug("[Metrix Post-Processor] Templates OK.");
    }
  }

  private void parseXmlStructure() {
    // By de facto the XSD validation should have caught the root element
    // name formatting
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
            // DemuxOperation is a singular process and does not contain any
            // childnodes by default.
            if (nodeName.equals("DemuxOperation")) {
              node = mapTemplate(node);
              DemuxOperation dmxObject = new DemuxOperation(node);
              processList.add(dmxObject);
            }
            else {
              // Iterate over defined children and create do Operation node
              // processing.
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
          log.error("[Metrix Post-Processor] Node Mismatch. This should not be possible");
        }
      }
      log.debug("[Metrix Post-Processor] Sorting the process list...");
      Collections.sort(blockList, new PostProcessComparator());
      Collections.sort(processList, new OperationComparator<>());
      log.debug("[Metrix Post-Processor] Done sorting.");
    }
    else {
      isValid = false;
      log.error("[Metrix Post-Processor] Incompatible document structure. This should not be possible.");
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
          log.warn("[Metrix Post-Processor] FileOperation " + fo.getTitle() + " not populated correctly. Source and/or destination are empty.");
        }
      }
      else if (pp instanceof DemuxOperation) {
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
    // Default exitstatus 0 for return without encountering any errors
    // during processing.
    int exitStatus = -255;

    log.info("[Metrix Post-Processor] Starting script: " + dmx.getTitle());
    // The script / application to execute
    if (dmx.getBclToFastQPath().equals("")) {
      log.warn("[Metrix Post-Processor] Cannot find the Bcl conversion script. Argument is not set in postprocessing.");
      return exitStatus;
    }

    exitStatus = 0;
    // Split SampleSheet based on projects.
    try {
      dmx.generateSampleSheets();
    }
    catch (FileNotFoundException FNFE) {
      log.error("[Metrix Post-Processor] Samplesheets could not be generated, the untransformed samplesheet is not present: " + dmx.getSampleSheetPath(), FNFE);
    }
    catch (IOException ex) {
      log.error("[Metrix Post-Processor] IOException during samplesheet generation ", ex);
    }
    // Demux each project.
    for (File ss : dmx.getSamplesheetLocations()) {
      // Demultiplex every samplesheet generated by DemuxOperation
      processSamplesheet(dmx, ss);
    }

    return exitStatus;
  }

  private int processSamplesheet(DemuxOperation dmx, File samplesheet) {
    int exitStatus = 0;
    String sampleSheetNameNormal = samplesheet.getName().replace(".csv", "");
    log.debug("[Metrix Post-Processor] Starting demultiplexing for: " + sampleSheetNameNormal);

    // The output file. All console activity is written to this file.
    final File loggingFile;

    if (!dmx.getLoggingPath().equals("")) {
      loggingFile = new File(dmx.getLoggingPath());
    }
    else {
      loggingFile = new File(dmx.getBaseOutputDir() + "/" + sampleSheetNameNormal + ".postprocessing.log");
    }

    // Does Data output folder exist?
    File dmxBaseOut = new File(dmx.getBaseOutputDir() + "/Demux/" + sampleSheetNameNormal + "/");

    if (!dmxBaseOut.exists()) {
      if (dmxBaseOut.mkdirs()) {
        log.debug("[Metrix Post-Processor] Successfuly created folder: " + dmxBaseOut);
      }
      else {
        log.warn("[Metrix Post-Processor] Error creating directory! ");
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

    log.debug("Script: " + dmx.getBclToFastQPath() + "\nInput: " + dmx.getBaseWorkingDir() + "\nOutput: " + dmxBaseOut.toString() + "\nSamplesheet: " + samplesheet.getAbsolutePath() + "\nBaseMask: " + dmx.ssBaseMaskMap.get(samplesheet.getAbsolutePath()));

    if (dmx.getArguments() != null) {
      String[] spl = dmx.getArguments().split(" ");
      cmd.addAll(Arrays.asList(spl));
    }

    ProcessBuilder pb = new ProcessBuilder(cmd);
    log.info("Executing : " + pb.command());

    if (dmx.getBaseWorkingDir() != null) {
      log.debug("[Metrix Post-Processor] Setting base working directory for postprocessing to: " + dmx.getBaseWorkingDir());
      pb.directory(new File(dmx.getBaseWorkingDir())); // Data/Intensities/BaseCalls
    }

    // Instantiate the ProcessBuilder
    pb.redirectOutput(loggingFile);
    pb.redirectErrorStream(true);

    try {
      log.debug("[Metrix Post-Processor] Starting process for: " + pb.command());
      Process p = pb.start();
      // Start the process and wait for it to finish.
      exitStatus = p.waitFor();
    }
    catch (IOException IO) {
      log.error("[Metrix Post-Processor] IOException while executing process (" + dmx.getId() + ")", IO);
    }
    catch (InterruptedException IE) {
      log.error("[Metrix Post-Processor] InterruptedException for process ( " + dmx.getId() + ")", IE);
    }
    if (exitStatus == 0) {
      log.debug("[Metrix Post-Processor] ConfigureBclToFastQ configured successfully - Starting make...");
      ArrayList<String> cmdMake = new ArrayList<>();
      cmdMake.addAll(Arrays.asList(dmx.getMakePath().split(" ")));

      String[] makeArgs = dmx.getMakeArguments().split(" ");
      cmdMake.addAll(Arrays.asList(makeArgs));

      ProcessBuilder pbMake = new ProcessBuilder(cmdMake);
      log.debug("[Metrix Post-Processor] Starting Make for : " + pbMake.command());
      pbMake.directory(dmxBaseOut);

      // Instantiate the ProcessBuilder for make
      pbMake.redirectOutput(new File(dmx.getBaseOutputDir() + "/" + samplesheet.getName().replace(".csv", "") + ".make.log"));
      pbMake.redirectErrorStream(true);

      try {
        Process pMake = pbMake.start();
        // Start the process and wait for it to finish.
        exitStatus += pMake.waitFor();
      }
      catch (IOException IO) {
        log.error("[Metrix Post-Processor] IOException while executing process (" + dmx.getId() + ")", IO);
      }
      catch (InterruptedException IE) {
        log.error("[Metrix Post-Processor] InterruptedException for process ( " + dmx.getId() + ")", IE);
      }

      if (exitStatus == 0) {
        log.info("[Metrix Post-Processor] Application block (" + dmx.getOrder() + " :: " + dmx.getSubOrder() + " :: " + dmx.getId() + ") has finished successfully.");
      }
      else {
        log.warn("[Metrix Post-Processor] Application block ''" + dmx.getId() + "'' has exited with errors.");
      }
    }

    return exitStatus;
  }

  private int executeFileOperation(FileOperation fo) {
    log.info("[Metrix Post-Processor] Starting file operation: " + fo.getTitle());
    int exitStatus = -255;

    File sourceFile = new File(fo.getSource());
    File destinationFile = new File(fo.getDestination());

    boolean sourceIsDir = false;
    boolean destinationIsDir = false;

    if (!sourceFile.exists()) {
      log.warn("[Metrix Post-Processor] Source file: " + sourceFile + " does not exist.");
      exitStatus = -1;
      return exitStatus;
    }

    if (!sourceFile.canRead()) {
      log.warn("[Metrix Post-Processor] Cannot read source file.");
      exitStatus = -1;
      return exitStatus;
    }
    else {
      log.debug("[Metrix Post-Processor] Source file: " + sourceFile + " is readable.");
    }

    // If destination is file (assuming that all destination files will have an
    // extension)
    // Check parent
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
      log.info("[Metrix Post-Processor] Destination path is writeable.");
    }
    else {
      log.info("[Metrix Post-Processor] Destination path is not writeable. " + targetDir);
      exitStatus = -1;
      return exitStatus;
    }

    if (fo.needOverwrite() && destinationFile.isFile()) {
      log.info("[Metrix Post-Processor] Destination already exists. Overwriting.");
    }
    else if (!fo.needOverwrite() && !destinationFile.exists()) {
      log.info("[Metrix Post-Processor] Creating file during copy: " + sourceFile.getName());
    }
    else if ((sourceDir != null) && (fo.hasGlobbing())) {
      log.info("[Metrix Post-Processor] Copying files with extension: " + fo.getGlobbing() + " from: " + sourceFile.getAbsoluteFile() + "to: " + destinationFile);
    }
    else if ((sourceDir != null) && !(fo.hasGlobbing())) {
      log.info("[Metrix Post-Processor] Copying directory: " + sourceFile + "\tto: " + destinationFile);
    }
    else if (destinationIsDir && destinationFile.exists() && sourceFile.isFile()) {
      log.info("[Metrix Post-Processor] Source is a file - destination is directory.");
    }
    else if (destinationIsDir && fo.needOverwrite() && sourceFile.isFile()) {
      log.info("[Metrix Post-Processor] Source is a file - destination is directory - Overwriting file.");
    }
    else {
      log.info("[Metrix Post-Processor] Destination file already exists. Not overwriting.");
      exitStatus = -1;
      return exitStatus;
    }

    /*
     * Is process a copy operation?
     */
    if (fo.isCopyOperation()) {

      // Is globbing used?
      if (fo.hasGlobbing()) {
        log.info("[Metrix Post-Processor] Using globbing pattern: " + fo.getGlobbing());
        List<Path> foundFiles = findFilesGlobbing(Paths.get(fo.getSource()), fo.getGlobbing());

        // Execute the basic copy operation for files that have been found with
        // a pattern.
        exitStatus = executeGlobbingCopy(fo, foundFiles);
        return exitStatus;
      }

      try {
        CopyOption[] options = new CopyOption[] {};
        CopyOption[] replOpt = new CopyOption[] {
            COPY_ATTRIBUTES, REPLACE_EXISTING, java.nio.file.LinkOption.NOFOLLOW_LINKS
        };

        CopyOption[] stdOpt = new CopyOption[] {
            COPY_ATTRIBUTES, java.nio.file.LinkOption.NOFOLLOW_LINKS
        };

        // Overwrite in file operations?
        options = !fo.needOverwrite() ? stdOpt : replOpt;

        // Is the source path a file?
        if (sourceFile.isFile()) {
          log.debug("Copying " + sourceFile + "to: " + destinationFile);
          Files.copy(sourceFile.toPath(), destinationFile.toPath(), options);
          exitStatus = 0;
        }
        else if (sourceFile.isDirectory()) { // Is the sourcepath a directory?
          FileOperations fileops = new FileOperations(sourceFile.toPath(), destinationFile.toPath(), options);

          try {
            fileops.recursiveCopy();
            exitStatus = 0;
          }
          catch (IOException IO) {
            log.warn("[Metrix Post-Processor] Failed recursive copy", IO);
            exitStatus = -1;
          }

        }
        else {
          log.warn("[Metrix Post-Processor] Source file does not exist. ");
          exitStatus = -1;
          return exitStatus;
        }
      }
      catch (IOException Iex) {
        log.warn("[Metrix Post-Processor] Failed file copy.", Iex);
        exitStatus = -1;
      }

      if (exitStatus == 0) {
        log.debug("[Metrix Post-Processor] Successfully copied " + sourceFile + " to " + destinationFile);
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
        log.warn("[Metrix Post-Processor] Failed to create symbolic link.", x);
      }
      catch (UnsupportedOperationException x) {
        // Some file systems do not support symbolic links.
        log.warn("[Metrix Post-Processor] This OS does not support creating symbolic links.", x);
      }
    }

    /*
     * Process is another operation type
     */
    // Not yet implemented.

    if (exitStatus == 0) {
      log.info("[Metrix Post-Processor] File Operation block (" + fo.getOrder() + " :: " + fo.getSubOrder() + " :: " + fo.getId() + ") has finished successfully.");
    }
    else {
      log.warn("[Metrix Post-Processor] File Operation block ''" + fo.getId() + "'' has exited with errors.");
    }

    return exitStatus;
  }

  private List<Path> findFilesGlobbing(Path sourcePath, String pattern) {
    log.debug("[Metrix Post-Processor] Finding files with globbing pattern: " + pattern + " in " + sourcePath);
    FileOperations fileops = new FileOperations(sourcePath, pattern);

    try {
      fileops.findFilesGlobbing();
    }
    catch (IOException Ex) {
      log.warn("[Metrix Post-Processor] IOException during globbing find operation.", Ex);
    }

    log.info("[Metrix Post-Processor] Found " + fileops.getResultsSize() + " files.");
    return fileops.getResults();
  }

  private int executeGlobbingCopy(FileOperation fo, List<Path> fileList) {
    File destinationFile = new File(fo.getDestination());
    int exitStatus = 0;

    CopyOption[] options = new CopyOption[] {};
    CopyOption[] replOpt = new CopyOption[] {
        COPY_ATTRIBUTES, REPLACE_EXISTING, java.nio.file.LinkOption.NOFOLLOW_LINKS
    };

    CopyOption[] stdOpt = new CopyOption[] {
        COPY_ATTRIBUTES, java.nio.file.LinkOption.NOFOLLOW_LINKS
    };

    if (!destinationFile.isDirectory()) {
      log.debug("[Metrix Post-Processor] Destination path is not a directory.");
      return -1;
    }

    for (Path sourcePath : fileList) {
      options = !fo.needOverwrite() ? stdOpt : replOpt;
      try {
        Files.copy(sourcePath, (destinationFile.toPath()).resolve(sourcePath), options);
        exitStatus += 0;
      }
      catch (IOException Ex) {
        log.warn("[Metrix Post-Processor] Error globbing file copy.", Ex);
        exitStatus += -1;
      }

      log.debug("[Metrix Post-Processor] Copied: " + sourcePath + " to: " + destinationFile);
    }

    return exitStatus; // All is good
  }

  private int executeApplication(Application app) {
    ArrayList<String> cmd = new ArrayList<>();
    log.info("[Metrix Post-Processor] Starting script: " + app.getTitle());
    // The script / application to execute
    cmd.addAll(Arrays.asList(app.getScriptPath().split(" ", -1)));

    // The output file. All application activity is written to this file.
    final File outputFile = new File(app.getOutputPath());
    // The supplied arguments for the script
    cmd.addAll(Arrays.asList(app.getArguments().split(" ", -1)));
    ProcessBuilder pb = new ProcessBuilder(cmd);

    if (app.getWorkingDirectory() != null) {
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
      log.error("[Metrix Post-Processor] IOException while executing process (" + app.getId() + ")", IO);
    }
    catch (InterruptedException IE) {
      log.error("[Metrix Post-Processor] InterruptedException for process ( " + app.getId() + ")", IE);
    }

    if (exitStatus == 0) {
      log.info("[Metrix Post-Processor] Application block (" + app.getOrder() + " :: " + app.getSubOrder() + " :: " + app.getId() + ") has finished successfully.  ");
    }
    else {
      log.warn("[Metrix Post-Processor] Application block ''" + app.getId() + "'' has exited with errors.");
    }

    return exitStatus;
  }
}

/*
 * Possible placeholders for parameters or variables. // Placeholder options -
 * {TotalCycles} - {FlowcellID} - {FlowcellSide} - {SequencerRunNr} -
 * {SequencerName} - {SequencerType} - {RunDate} - {RunType} - {RunDirectory} -
 * {RunID} - {DemuxIndex}
 */
