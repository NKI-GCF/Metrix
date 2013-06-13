// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.objects;

import java.io.*;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.Iterator;
import java.util.ListIterator;
import nki.objects.Summary;
import nki.objects.MutableInt;
import nki.constants.Constants;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

public class SummaryCollection implements Serializable {
	// Object Specific
	private static final long serialVersionUID = 42L;

	// Object Collection
	private ArrayList<Summary> summaryCollection = new ArrayList<Summary>();
	private HashMap<Integer, MutableInt> summaryStateMapping = new HashMap<Integer, MutableInt>();
	private String xmlAsString = "";
	private String collectionFormat = Constants.COM_FORMAT_OBJ;			// Default

	public void appendSummary(Summary sum){
		summaryCollection.add(sum);
		
		MutableInt frq = summaryStateMapping.get(sum.getState());
		if(frq == null){
			summaryStateMapping.put(sum.getState(), new MutableInt());
		}else{
			summaryStateMapping.get(sum.getState()).increment();
		}
	}

	public ListIterator<Summary> getSummaryIterator(){
		return summaryCollection.listIterator();
	}

	public int getCollectionCount(){
		return summaryCollection.size();
	}

	public Iterator getStateCount(){
		return summaryStateMapping.entrySet().iterator();
	}

	public Document getSummaryCollectionAsXML(Command com){
		Document xmlDoc = null;
		try{
			// Build the XML document
			DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
			xmlDoc = docBuilder.newDocument();

			Element root = xmlDoc.createElement("SummaryCollection");
			xmlDoc.appendChild(root);
			
			// Set run counts as root attributes
			root.setAttribute("active", convertStateInt(Constants.STATE_RUNNING));
			root.setAttribute("finished", convertStateInt(Constants.STATE_FINISHED));
			root.setAttribute("error", convertStateInt(Constants.STATE_HANG));
			root.setAttribute("turn", convertStateInt(Constants.STATE_TURN));
			root.setAttribute("init", convertStateInt(Constants.STATE_INIT));

			ListIterator litr = this.getSummaryIterator();
		
			// Iterate over Summary Collection and add values to XmlDocFac
			while(litr.hasNext()){
				Summary sumObj = (Summary) litr.next();

				// If state is 12, fetch all objects.
				if(sumObj.getState() == com.getState() || com.getState() == Constants.STATE_ALL_PSEUDO){
					Element sumXml = xmlDoc.createElement("Summary");

					if(com.getType().equals(Constants.COM_TYPE_SIMPLE)){
						sumXml = summaryAsSimple(sumObj, sumXml, xmlDoc);
					}else if(com.getType().equals(Constants.COM_TYPE_DETAIL)){
						sumXml = summaryAsDetailed(sumObj, sumXml, xmlDoc);
			//		}else if(com.getType().equals(Constants.COM_TYPE_METRIC)){
			//			sumXml = summaryAsMetric(sumObj, sumXml, xmlDoc);			
					}else{
						sumXml = summaryAsSimple(sumObj, sumXml, xmlDoc);
					}

					root.appendChild(sumXml);
				}

				if(com.getType().equals(Constants.COM_TYPE_METRIC)){
					// create QScore element
					Element sumXml = xmlDoc.createElement("QScoreDist");
					sumXml.setAttribute("runId", sumObj.getRunId());
					QScoreDist dist = sumObj.getQScoreDist();
					sumXml = dist.toXML(sumXml,xmlDoc);
				}
			}			
		}catch(Exception ex){
			ex.printStackTrace();
		}
		
		setCollectionFormat(Constants.COM_FORMAT_XML);		

		// Return in the form of a XML Document
		return xmlDoc;
	}

	/*
	 *	XML Builders
	 */


	private Element summaryAsSimple(Summary sumObj, Element sumXml, Document xmlDoc){

		sumXml.appendChild(createElement(xmlDoc, "runId", sumObj.getRunId()));
		sumXml.appendChild(createElement(xmlDoc, "runType", sumObj.getRunType()));
		sumXml.appendChild(createElement(xmlDoc, "runState", sumObj.getState()+""));
		sumXml.appendChild(createElement(xmlDoc, "lastUpdated", sumObj.getLastUpdated()+""));
		sumXml.appendChild(createElement(xmlDoc, "runDate", sumObj.getRunDate()+""));
		sumXml.appendChild(createElement(xmlDoc, "totalCycle", sumObj.getTotalCycles()+""));
		sumXml.appendChild(createElement(xmlDoc, "instrument", sumObj.getInstrument()));		

		return sumXml;
	}

	private Element summaryAsDetailed(Summary sumObj, Element sumXml, Document xmlDoc){

		sumXml.appendChild(createElement(xmlDoc, "runId", sumObj.getRunId()));
		sumXml.appendChild(createElement(xmlDoc, "runType", sumObj.getRunType()));
		sumXml.appendChild(createElement(xmlDoc, "flowcellId", sumObj.getFlowcellID()));
		sumXml.appendChild(createElement(xmlDoc, "runSide", sumObj.getSide()));
		sumXml.appendChild(createElement(xmlDoc, "runState", sumObj.getState()+""));
		sumXml.appendChild(createElement(xmlDoc, "runPhase", sumObj.getPhase()));
		sumXml.appendChild(createElement(xmlDoc, "lastUpdated", sumObj.getLastUpdated()+""));
		sumXml.appendChild(createElement(xmlDoc, "runDate", sumObj.getRunDate()+""));
		sumXml.appendChild(createElement(xmlDoc, "currentCycle", sumObj.getCurrentCycle()+""));
		sumXml.appendChild(createElement(xmlDoc, "totalCycle", sumObj.getTotalCycles()+""));
		sumXml.appendChild(createElement(xmlDoc, "instrument", sumObj.getInstrument()));

		return sumXml;

	}

	private Element summaryAsMetric(Summary sumObj, Element sumXml, Document xmlDoc){

		

		sumXml.appendChild(createElement(xmlDoc, "TestLalal", sumObj.getRunId()));
		return sumXml;
	}

	private String convertStateInt(int mapping){

		if(summaryStateMapping.containsKey(mapping)){
			return Integer.toString(summaryStateMapping.get(mapping).get());
		}else{
			return "";
		}
	}

	private Element createElement(Document doc, String name, String text){
		Element e = doc.createElement(name);
		if(text == null){
			text = "";
		}
		e.appendChild(doc.createTextNode(text));

		return e;
	}

	public String getSummaryCollectionXMLAsString(Command com){
		// Call getSummaryCollectionAsXML
		Document xmlDoc = this.getSummaryCollectionAsXML(com);
		StringWriter writer = new StringWriter();

		try {
			TransformerFactory tFact = TransformerFactory.newInstance();
			Transformer trans = tFact.newTransformer();
			
			trans.setOutputProperty("omit-xml-declaration", "yes");

			StreamResult result = new StreamResult(writer);
			DOMSource source = new DOMSource(xmlDoc);
			trans.transform(source, result);
		}catch(TransformerConfigurationException TCE){
			System.out.println("TransformerConfigurationException: " + TCE.toString());
		}catch(TransformerException TE){
			System.out.println("TransformerException: " + TE.toString());
		}

		return writer.toString();
	}

	public void setCollectionFormat(String format){
		this.collectionFormat = format;
	}

	public String getCollectionFormat(){
		return collectionFormat;
	}
}
