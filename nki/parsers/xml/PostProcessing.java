// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.parsers.xml;

import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
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
import java.util.EnumSet;
import java.nio.file.attribute.*;
import java.nio.file.Files;
import static java.nio.file.StandardCopyOption.*;

public class PostProcessing {
	
	private DocumentBuilderFactory documentBuilderFactory;
	private DocumentBuilder documentBuilder;
	private Document doc;
	private Summary sum;
	private String xmlFile;
	private ArrayList<PostProcess> blockList = new ArrayList<PostProcess>();
	private ArrayList<PostProcess> processList = new ArrayList<PostProcess>();
	private boolean isValid = true;
	private boolean hasFinished = false;

	private HashMap<String, String> tm = new HashMap<String, String>();	// Template mapping collection for placeholder substitutions.
	// Instantiate Logger
	private static final LoggerWrapper metrixLogger = LoggerWrapper.getInstance();
    // Properties config
	private static final Properties configFile = new Properties();
		

	public PostProcessing(Summary s){
		this.sum = s;	// Set summary
	}

	public void run(){
		metrixLogger.log.info("[Metrix Post-Processor] Initializing post processing for " + sum.getRunId());
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
		if(isValid){
			executeProcessing();
		}else{
			metrixLogger.log.warning("[Metrix Post-Processor] One or more validation checks failed. Cannot execute processing.");
		}

		if(hasFinished){
			metrixLogger.log.info("[Metrix Post-Processor] Post processing has successfully finished.");
		}else{
			metrixLogger.log.warning("[Metrix Post-Processor] One or more steps during post processing encountered errors. Please check your configuration.");
		}
	}

	private void loadProperties(){
		metrixLogger.log.finer("[Metrix Post-Processor] Loading properties...");
        try{
			// Use external properties file, outside of jar location.
    	    String externalFileName = System.getProperty("properties");
	        String absFile = (new File(externalFileName)).getAbsolutePath();
            InputStream fin = new FileInputStream(new File(absFile));
            configFile.load(fin);
            fin.close();
			metrixLogger.log.finer("[Metrix Post-Processor] Successfully loaded properties: " + absFile);
        }catch(FileNotFoundException FNFE){
            metrixLogger.log.severe("[ERROR] Properties file not found.");
			isValid = false;
        }catch(IOException Ex){
            metrixLogger.log.severe("[ERROR] Reading properties file. " + Ex.toString());
			isValid = false;
        }
	}

	private void initXml(){
		metrixLogger.log.finer("[Metrix Post-Processor] Initializing post processing xml.");
		try{
			xmlFile = configFile.getProperty("POSTPROCESSING", "./postprocessing.mtx");
			documentBuilderFactory = DocumentBuilderFactory.newInstance();
			documentBuilder = documentBuilderFactory.newDocumentBuilder();
			doc = documentBuilder.parse(xmlFile);
			doc.getDocumentElement().normalize();
			metrixLogger.log.finer("[Metrix Post-Processor] Successfully loaded post processing xml: " + xmlFile);
		}catch(IOException Ex){
			metrixLogger.log.severe("[Metrix Post-Processor] IOException. " + Ex.toString());
			isValid = false;
		}catch(SAXException SAX){
			metrixLogger.log.severe("[Metrix Post-Processor] SAXException. Cannot process post processing XML file (" + SAX.toString() + ")");
			isValid = false;
		}catch(ParserConfigurationException PCE){
			metrixLogger.log.severe("[Metrix Post-Processor] ParserConfigurationException. Cannot process post processing XML file (" + PCE.toString() + ")" );
			isValid = false;
		}
	}

	/*
	 * XML Structure validation method.
	 * Contents of postprocessing.mtx (XML) are verified against postprocessing.xsd
	 */
	private void validateXmlStructure(){
		metrixLogger.log.finer("[Metrix Post-Processor] Validating xml structure...");
		InputStream xml = null;
		InputStream xsd = null;
	    try {
			// The required files:
			// - postprocessing.mtx	(properties file)
			// - postprocessing.xsd (default location in root)
			xml = new FileInputStream(xmlFile);
			xsd = new FileInputStream("./postprocessing.xsd");

    	    SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
	        Schema schema = factory.newSchema(new StreamSource(xsd));
	        Validator validator = schema.newValidator();
	        validator.validate(new StreamSource(xml));
			metrixLogger.log.finer("[Metrix Post-Processor] Successfully validated XML structure.");
    	}catch(IOException IO){
			metrixLogger.log.warning("[Metrix Post-Processor] Failed to open required files. ("+IO.toString()+")");
			isValid = false;
		}catch(SAXException ex){  // Validation failed
			metrixLogger.log.warning("[Metrix Post-Processor] XML validation failed. ("+ ex.toString() +" || " + ex.getLocalizedMessage() +")" );
			isValid = false;
	    }finally{
			try{
				if(xml != null){
					xml.close();
				}

				if(xsd != null){
					xsd.close();
				}
			}catch(IOException ex){
				metrixLogger.log.warning("[Metrix Post-Processor] Error closing XML objects. ("+ ex.toString() +")" );
			}
		}
	}

	private void loadTemplate(){
		metrixLogger.log.finer("[Metrix Post-Processor] Loading templates...");
		boolean check = true;

		tm.put("{TotalCycles}",Integer.toString(sum.getTotalCycles()));
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
			if(a == null){
				metrixLogger.log.warning("[Metrix Post-Processor] Null values found in template set. Failed.");		
				isValid = false;
				check = false;
			}
		}

		if(check){
			metrixLogger.log.finer("[Metrix Post-Processor] Templates OK.");
		}
	}

	private void parseXmlStructure(){
		// By de facto the XSD validation should have caught the root element name formatting
		if(doc.getDocumentElement().getNodeName().equals("Metrix")){
			NodeList nodeList = doc.getElementsByTagName("*");
	
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node node = nodeList.item(i);
				if(node.getNodeName() == "Metrix"){continue;}
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					String nodeName = node.getNodeName();
						if(nodeName.equals("FileOperation") || nodeName.equals("Application")){
							// Do block node processing
							PostProcess processBlock = new PostProcess(node);
							blockList.add(processBlock);

							NodeList foChildren = node.getChildNodes();

							// Iterate over defined children and create do Operation node processing.
							for(int x = 0; x < foChildren.getLength(); x++){
								Node fo = foChildren.item(x);
								if(fo.getNodeName().equals("#text") || fo.getNodeName().equals("#comment")){continue;}
								if(nodeName.equals("FileOperation")){
									fo = mapTemplate(fo);
									FileOperation foObject = new FileOperation(node, fo);
									processList.add(foObject);
								}else if(nodeName.equals("Application")){
									fo = mapTemplate(fo);
									Application appObject = new Application(node, fo);
									processList.add(appObject);
								}
							}
						}else{
							continue;
						}
					}else{
						isValid = false;
						metrixLogger.log.severe("[Metrix Post-Processor] Node Mismatch. This should not be possible");
					}
			}
			metrixLogger.log.finest("[Metrix Post-Processor] Sorting the process list...");
			Collections.sort(blockList, new PostProcessComparator());
			Collections.sort(processList, new OperationComparator<PostProcess>());
			metrixLogger.log.finest("[Metrix Post-Processor] Done sorting.");
		}else{
			isValid = false;
			metrixLogger.log.severe("[Metrix Post-Processor] Incompatible document structure. This should not be possible.");
		}
	}

	private Node mapTemplate(Node childNode){
		if(!childNode.hasChildNodes()){return childNode;}
		NodeList childNodeList = childNode.getChildNodes();

		for (int i = 0; i < childNodeList.getLength(); i++) {
			Node innerNode = childNodeList.item(i);
			Node replacementNode = innerNode;

			if(innerNode.getNodeName().equals("#text")){continue;}
			String oldVal = innerNode.getTextContent();
			
			if(oldVal == null){continue;}
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

	private void executeProcessing(){
		int status = 0;

		for(PostProcess pp : processList){
			if(pp instanceof FileOperation){
				FileOperation fo = (FileOperation) pp;
				if(fo.isValid()){
					status += executeFileOperation(fo);
				}else{
					metrixLogger.log.warning("[Metrix Post-Processor] FileOperation " + fo.getTitle() + " not populated correctly. Source and/or destination are empty.");
				}
			}else if(pp instanceof Application){
				Application app = (Application) pp;
				status += executeApplication(app);
			}
		}
		if(status == 0){
			hasFinished = true;
		}
	}

	private int executeFileOperation(FileOperation fo){
		metrixLogger.log.info("[Metrix Post-Processor] Starting file operation: " + fo.getTitle());
		int exitStatus = -255;
		
		File sourceFile = new File(fo.getSource());
		File destinationFile = new File(fo.getDestination());
	
		boolean sourceIsDir = false;
		boolean destinationIsDir = false;

		if(!sourceFile.exists()){
			metrixLogger.log.warning("[Metrix Post-Processor] Source file: " + sourceFile + " does not exist.");
			exitStatus = -1;
			return exitStatus;
		}

		if(!sourceFile.canRead()){
			metrixLogger.log.warning("[Metrix Post-Processor] Cannot read source file.");
			exitStatus = -1;
			return exitStatus;
		}else{
			metrixLogger.log.finest("[Metrix Post-Processor] Source file: " + sourceFile.toString() + " is readable.");
		}

		// If destination is file (assuming that all destination files will have an extension)
		// 	Check parent
		File targetDir;
		if((destinationFile.toString()).lastIndexOf(".") > 0){
			destinationFile.getParentFile().mkdirs();
			targetDir = destinationFile.getParentFile();
		}else{ // If destination is a directory
			targetDir = destinationFile;
			destinationFile.mkdirs(); 
			destinationIsDir = true;
			fo.setDestination(destinationFile + File.separator + sourceFile.getName());
			destinationFile = new File(fo.getDestination());
		}

		File sourceDir = null;
		if((sourceFile.toString()).lastIndexOf(".") == -1){
			sourceDir = sourceFile;
			sourceIsDir = true;
		}

		if(targetDir.canWrite()){
			metrixLogger.log.info("[Metrix Post-Processor] Destination path is writeable.");
		}else{
			metrixLogger.log.info("[Metrix Post-Processor] Destination path is not writeable. " + targetDir);
			exitStatus = -1;
			return exitStatus;
		}

		if(fo.needOverwrite() && destinationFile.isFile()){
			metrixLogger.log.info("[Metrix Post-Processor] Destination already exists. Overwriting.");
		}else if(!fo.needOverwrite() && !destinationFile.exists()){
			metrixLogger.log.info("[Metrix Post-Processor] Creating file during copy: " + sourceFile.getName());
		}else if((sourceDir != null) && (fo.hasGlobbing())){
			metrixLogger.log.info("[Metrix Post-Processor] Copying files with extension: " + fo.getGlobbing() + " from: " + sourceFile.getAbsoluteFile() + "to: " + destinationFile);		
		}else if((sourceDir != null) && !(fo.hasGlobbing())){
			metrixLogger.log.info("[Metrix Post-Processor] Copying directory: " + sourceFile + "\tto: " + destinationFile);			
		}else if(destinationIsDir && destinationFile.exists() && sourceFile.isFile()){
			metrixLogger.log.info("[Metrix Post-Processor] Source is a file - destination is directory.");
		}else if(destinationIsDir && fo.needOverwrite() && sourceFile.isFile()){
			metrixLogger.log.info("[Metrix Post-Processor] Source is a file - destination is directory - Overwriting file.");
		}else{
			metrixLogger.log.info("[Metrix Post-Processor] Destination file already exists. Not overwriting.");
			exitStatus = -1;
			return exitStatus;
		}

		/*
		*  Is process a copy operation?
		*/
		if(fo.isCopyOperation()){

			// Is globbing used?
			if(fo.hasGlobbing()){
				metrixLogger.log.info("[Metrix Post-Processor] Using globbing pattern: " + fo.getGlobbing());
				ArrayList<Path> foundFiles = findFilesGlobbing(Paths.get(fo.getSource()), fo.getGlobbing());
				
				// Execute the basic copy  operation for files that have been found with a pattern.
				exitStatus = executeGlobbingCopy(fo, foundFiles);
				return exitStatus;
			}

			try{
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
				if(sourceFile.isFile()){
					metrixLogger.log.fine("Copying " + sourceFile + "to: " + destinationFile);
					Files.copy( 
   		   	            sourceFile.toPath(), 
						destinationFile.toPath(),
						options
				    );
					exitStatus = 0;
				}else if(sourceFile.isDirectory()){ // Is the sourcepath a directory?
					FileOperations fileops = 
						new FileOperations(
							sourceFile.toPath(),
							destinationFile.toPath(),
							options
						);
						
						try{
							fileops.recursiveCopy();
							exitStatus = 0;
						}catch(IOException IO){
							metrixLogger.log.warning("[Metrix Post-Processor] IO Exception in recursive copy: " + IO.toString());
							exitStatus = -1;
						}
		
				}else{
					metrixLogger.log.warning("[Metrix Post-Processor] Source file does not exist. ");
					exitStatus = -1;
					return exitStatus;
				}
			}catch(IOException Iex){
				metrixLogger.log.warning("[Metrix Post-Processor] IOException during file copy: " + Iex.toString());
				exitStatus = -1; 
			}
		
			if(exitStatus == 0){
				metrixLogger.log.finer("[Metrix Post-Processor] Successfully copied " + sourceFile + " to " + destinationFile);
			}
		}

		/*
		* Is process a symlink operation?
		*/
		if(fo.isSymlinkOperation()){
			try {
				Files.createSymbolicLink(sourceFile.toPath(), destinationFile.toPath());
			}catch (IOException x) {
				metrixLogger.log.warning("[Metrix Post-Processor] IOException while creating a symbolic link: " + x.toString());
			} catch (UnsupportedOperationException x) {
			    // Some file systems do not support symbolic links.
				metrixLogger.log.warning("[Metrix Post-Processor] This OS does not support creating symbolic links: " + x.toString());
			}
		}

		/*
		* Process is another operation type
		*/
		// Not yet implemented.

		if(exitStatus == 0){
			metrixLogger.log.info("[Metrix Post-Processor] File Operation block ("+fo.getOrder()+" :: "+fo.getSubOrder()+" :: "+fo.getId()+") has finished successfully.");
		}else{
			metrixLogger.log.warning("[Metrix Post-Processor] File Operation block '"+ fo.getId() +"' has exited with errors.");
		}

		return exitStatus;
	}

	private ArrayList<Path> findFilesGlobbing(Path sourcePath, String pattern){
		metrixLogger.log.fine("[Metrix Post-Processor] Finding files with globbing pattern: " + pattern + " in " + sourcePath);
		FileOperations fileops = new FileOperations(sourcePath, pattern);
	
		try {
			fileops.findFilesGlobbing();
		}catch(IOException Ex){
			metrixLogger.log.warning("[Metrix Post-Processor] IOException during globbing find operation. " +Ex.toString());
		}

		metrixLogger.log.info("[Metrix Post-Processor] Found " +  fileops.getResultsSize() + " files.");
		return fileops.getResults();
	}

	private int executeGlobbingCopy(FileOperation fo, ArrayList<Path> fileList){
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


		if(!destinationFile.isDirectory()){
			metrixLogger.log.fine("[Metrix Post-Processor] Destination path is not a directory.");
			return -1;
		}
	
		for(Path sourcePath : fileList){
			options = !fo.needOverwrite() ? stdOpt : replOpt;
			try{
				Files.copy(
					sourcePath,
					(destinationFile.toPath()).resolve(sourcePath),
					options
				);
				exitStatus += 0;
			}catch(IOException Ex){
				metrixLogger.log.warning("[Metrix Post-Processor] IOException in globbing file copy: " + Ex.toString());
				exitStatus += -1;
			}

			metrixLogger.log.finer("[Metrix Post-Processor] Copied: " + sourcePath + " to: " + destinationFile);
		}

		return exitStatus; // All is good
	}

	private int executeApplication(Application app){
		metrixLogger.log.info("[Metrix Post-Processor] Starting script: " + app.getTitle());
		// The script / application to execute
	    final File scriptPath = new File(app.getScriptPath());

		// The output file. All application activity is written to this file.
		final File outputFile = new File(app.getOutputPath());
		// The supplied arguments for the script 
	    final String arguments = app.getArguments();

		ProcessBuilder pb = new ProcessBuilder(scriptPath.toString(), arguments);
		
		// Instantiate the ProcessBuilder
		pb.redirectOutput(outputFile);
		pb.redirectErrorStream(true);

		// Default exitstatus -255 for return without processing.
		int exitStatus = -255;

		try{
			Process p = pb.start();
			// Start the process and wait for it to finish. 
			exitStatus = p.waitFor();
		}catch(IOException IO){
			metrixLogger.log.severe("[Metrix Post-Processor] IOException while executing process (" + app.getId() + "): " + IO.toString());
		}catch(InterruptedException IE){
			metrixLogger.log.severe("[Metrix Post-Processor] InterruptedException for process ( " + app.getId() + "): " + IE.toString());
		}
		
		if(exitStatus == 0){
			metrixLogger.log.info("[Metrix Post-Processor] Application block ("+app.getOrder()+" :: "+app.getSubOrder()+" :: "+app.getId()+") has finished successfully.  ");
		}else{
			metrixLogger.log.warning("[Metrix Post-Processor] Application block '"+ app.getId() +"' has exited with errors.");
		}

		return exitStatus;
	}
}

/*	Possible placeholders for parameters or variables.
	Applies to:
	=> FileOperations
	- Copy (Source, Destination)
	- Symlink (Source, Destination)
		
	=> Applications
	- ScriptPath
	- Arguments

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
