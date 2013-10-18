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
			metrixLogger.log.finer("[Metrix Post-Processor] Succes");
    	}catch(IOException IO){
			metrixLogger.log.warning("[Metrix Post-Processor] Failed to open required files. ("+IO.toString()+")");
			isValid = false;
		}catch(SAXException ex){  // Validation failed
			metrixLogger.log.warning("[Metrix Post-Processor] Validation of XML failed. ("+ ex.toString() +" || " + ex.getLocalizedMessage() +")" );
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
						metrixLogger.log.severe("[Metrix Post Processor] Node Mismatch. This should not be possible");
					}
			}
			metrixLogger.log.finest("[Metrix Post Processor] Sorting the process list...");
			Collections.sort(blockList, new PostProcessComparator());
			Collections.sort(processList, new OperationComparator<PostProcess>());
			metrixLogger.log.finest("[Metrix Post Processor] Done sorting.");
		}else{
			isValid = false;
			metrixLogger.log.severe("[Metrix Post Processor] Incompatible document structure. This should not be possible.");
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
		int status = -255;

		for(PostProcess pp : processList){
			if(pp instanceof FileOperation){
				FileOperation fo = (FileOperation) pp;
				executeFileOperation(fo);
			}else if(pp instanceof Application){
				Application app = (Application) pp;
				executeApplication(app);
			}
		}
		if(status == 0){
			hasFinished = true;
		}
	}

	private int executeFileOperation(FileOperation fo){
		metrixLogger.log.info("[Metrix Post Processor] Starting file operation: " + fo.getTitle());
		int exitStatus = -255;

		return exitStatus;
	}

	private int executeApplication(Application app){
		metrixLogger.log.info("[Metrix Post Processor] Starting script: " + app.getTitle());
		// The script / application to execute
	    final File scriptPath = new File(app.getScriptPath());

		// The output file. All application activity is written to this file.
		final File outputFile = new File(String.format(app.getOutputPath() + "/output_"+sum.getRunId()+"_" + app.getId() + "_%tY%<tm%<td_%<tH%<tM%<tS.txt",
        System.currentTimeMillis()));
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
			metrixLogger.log.severe("[Metrix Post Processor] IOException while executing process (" + app.getId() + "): " + IO.toString());
		}catch(InterruptedException IE){
			metrixLogger.log.severe("[Metrix Post Processor] InterruptedException for process ( " + app.getId() + "): " + IE.toString());
		}
		
		if(exitStatus == 0){
			metrixLogger.log.info("[Metrix Post Processor] Application block ("+app.getOrder()+" :: "+app.getSubOrder()+" :: "+app.getId()+") has finished successfully.  ");
		}else{
			metrixLogger.log.warning("[Metrix Post Processor] Application block '"+ app.getId() +"' has exited with errors.");
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
