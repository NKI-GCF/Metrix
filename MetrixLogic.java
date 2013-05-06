// Illumina Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

import java.net.*;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import nki.objects.Command;
import java.io.IOException;
import org.xml.sax.SAXException;
import javax.xml.parsers.ParserConfigurationException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import nki.parsers.illumina.*;
import nki.io.DataStore;
import nki.constants.Constants;
import nki.objects.Summary;
import nki.objects.QualityScores;
import java.util.logging.*;
import nki.parsers.xml.XmlDriver;
import java.util.regex.*;


public class MetrixLogic {

	// Instantiate Logger	
        private static final Logger metrixLogger = Logger.getLogger(MetrixLogic.class.getName());
	static{
                try{
                        boolean append = true;
                        FileHandler fh = new FileHandler("metrixDaemon.log", append);
                        fh.setFormatter(new SimpleFormatter());
                        metrixLogger.addHandler(fh);
                }catch(IOException Ex){
                        System.out.println("Error setting up logger " + Ex.toString());
                }
        }


	// Call inits
        private HashMap<String, Summary> results = new HashMap<String, Summary>();
	private Summary summary = null;

	public MetrixLogic(){
	}

	public boolean processMetrics(Path runDir, int state, DataStore ds){
		boolean success = false;
                String extractionMetrics = runDir.toString() + "/InterOp/" + Constants.EXTRACTION_METRICS;
                String tileMetrics = runDir.toString() + "/InterOp/" + Constants.TILE_METRICS;
		String qualityMetrics = runDir.toString() + "/InterOp/" + Constants.QMETRICS_METRICS; 

		String path = runDir.toString();
		// Retrieve Summary if exists else create new instance.
		this.checkSummary(path);

		// Check if run has finished uncaught.
		this.checkFinished(path);

		// Save entry for init phase of run.
		if(state == 5){
			summary.setState(5);
			summary.setLastUpdated();
			try{
				
				if(!summary.getXmlInfo()){
					XmlDriver xmd = new XmlDriver(runDir + "", summary);
					if(xmd.parseRunInfo()){
						summary = xmd.getSummary();
					}else{
						summary.setXmlInfo(false);
					}
				}
			}catch(SAXException SAX){
	                        metrixLogger.log(Level.SEVERE, "Error parsing XML with SAX. " + SAX.toString());			
			}catch(IOException Ex){
				metrixLogger.log(Level.SEVERE, "IOException Error. " + Ex.toString());
			}catch(ParserConfigurationException PXE){
	                        metrixLogger.log(Level.SEVERE, "Parser Configuration Exception. " + PXE.toString());
			}

			results.put(path, summary);
			saveEntry(path);
			return false;
		}

		summary.setRunDirectory(path);

		// Instantiate processing modules.
                ExtractionMetrics em = new ExtractionMetrics(extractionMetrics, state);
                TileMetrics tm = new TileMetrics(tileMetrics, state);
		QualityMetrics qm = new QualityMetrics(qualityMetrics, state);

		if(em.getFileMissing() || tm.getFileMissing() || qm.getFileMissing()){ // Extraction or TileMetrics file missing. Parse again later.
			return false;
		}

                try{
			if(summary.getState() != 2){			
	                        summary.setState(state);
			}

			if(!summary.getXmlInfo()){
				XmlDriver xmd = new XmlDriver(runDir + "", summary);
				if(xmd.parseRunInfo()){
					summary = xmd.getSummary();
				}else{
					summary.setXmlInfo(false);
				}
			}

                        int currentCycle = em.getLastCycle(); 
			
			// If run == paired end. Check for FC turn at (numReads read 1 + index 1). Set state accordingly.
			if((summary.getRunType() == "Paired End") && (currentCycle == summary.getTurnCycle())){
				// State has not been set yet and user has not yet been notified for the turning of the flowcell.
				if(!summary.getHasNotifyTurned()){
					summary.setState(4);
					metrixLogger.log(Level.INFO, "Flowcell of run: " + path + " has to be turned. Current cycle: " + currentCycle);				
					summary.setHasNotifyTurned(true);
				}
			}

	                summary.setCurrentCycle(currentCycle);
                        summary.setLastUpdated();

			// Catch event for when the flowcell has turned and the sequencer has continued sequencing the other side.
			if((summary.getRunType() == "Paired End") && (currentCycle > summary.getTurnCycle()) && (summary.getState() == 4)){
				summary.setHasTurned(true);
				summary.setState(1);
			}else{
				summary.setState(state);
			}
                        results.put(path, summary);

			saveEntry(path);	// Store summary entry in SQL database
	
			metrixLogger.log(Level.INFO, "Finished processing: " + runDir + "\tState: "+state);
			success = true;
                }catch(NullPointerException NPE){
			NPE.printStackTrace();
		}catch(SAXException SAX){
			metrixLogger.log(Level.SEVERE, "Error parsing XML with SAX. " + SAX.toString());
		}catch(IOException Ex){
			metrixLogger.log(Level.SEVERE, "IOException Error. " + Ex.toString());
		}catch(ParserConfigurationException PXE){
			metrixLogger.log(Level.SEVERE, "Parser Configuration Exception. " + PXE.toString());
		}

                return success;
	}

	private void checkSummary(String path){
		if(results.containsKey(path)){
			summary = results.get(path);				// Path has a summary stored in hash.
		}else{
			summary = new Summary();				// New hashmap entry for path
		}
	}

	public void finishRun(String path){
		this.checkSummary(path);
		summary.setState(2);	// Set state to 2: Complete
		metrixLogger.log(Level.INFO, "Run " + path +" has finished.");
		saveEntry(path);	
	}

	public void saveEntry(String path){
		int lastId = 0;
                try{
                     if(!DataStore.checkSummaryByRunId(path)){
                             try{
                                      lastId = DataStore.getMaxId();
                                      summary.setSumId(lastId+1);
                                      DataStore.appendedWrite(summary, path);
                             }catch(Exception Ex){
                                      System.out.println("Exception in write statement lastID = " + lastId+ " Error: " + Ex.toString());
                             }
                      }else{
                      // Run has been parsed before. Update instead of insert
                            try{
                                   DataStore.updateSummaryByRunName(summary, path);
                            }catch(Exception SEx){
                                   System.out.println("Exception in update statement " + SEx.toString());
                            }
                      }
               }catch(Exception Ex){
                      System.out.println("Run ID Checking error." + Ex.toString());
               }
	}
	
	public boolean checkFinished(String path){
		File RTAComplete = new File(path + "/RTAComplete.txt");
		
		if(RTAComplete.isFile()){
			this.finishRun(path);
			return true;
		}else{
			return false;
		}

	}

	public boolean checkPaired(String path, DataStore ds){
		System.out.println("Checking PATH: " + path);
		this.checkSummary(path);
		boolean check = false;
		if(this.processMetrics(Paths.get(path), 4, ds)){

			int cCycle = summary.getCurrentCycle();	
			 if((summary.getRunType() == "Paired End") && (cCycle == summary.getTurnCycle())){
				// State has not been set yet and user has not yet been notified for the turning of the flowcell.
				if(!summary.getHasNotifyTurned()){
					summary.setState(4);
					metrixLogger.log(Level.INFO, "Flowcell of run: " + path + " has to be turned. Current cycle: " + cCycle);
					summary.setHasNotifyTurned(true);
				}
				check = true;
                 	}
		}else{
			check = false;
		}
		return check;
	}
}
