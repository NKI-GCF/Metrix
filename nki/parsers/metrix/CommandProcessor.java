package nki.parsers;

import nki.objects.Command;
import nki.objects.Summary;
import nki.objects.SummaryCollection;
import nki.constants.Constants;
import nki.io.DataStore;
import nki.parsers.illumina.*;
import nki.exceptions.InvalidCredentialsException;
import nki.exceptions.CommandValidityException;
import nki.exceptions.UnimplementedCommandException;
import java.net.*;
import java.io.*;
import java.lang.Exception;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap; 

public class CommandProcessor {

	private boolean valCom = false;
	private boolean valApi = false;
	
	private Command retCom;
	private Command recCom;

	private ObjectOutputStream oos;
	private ObjectInputStream ois;
	private MetrixLogic ml;
	private DataStore ds;

	public CommandProcessor(Command command, 
				ObjectOutputStream oos,
				MetrixLogic ml,
				DataStore ds) 
		throws 	CommandValidityException,
			InvalidCredentialsException, 
			UnimplementedCommandException
		{
		// Process command.
		this.recCom = command;
		this.oos = oos;
		this.ml = ml;
		this.ds = ds;

		if(!checkAPI()){
			throw new InvalidCredentialsException();
		}

		// Perform validity checks
		if(recCom.checkParams()){
			// Set validity
			setIsValid(true);
//			execute();
		}else{
			setIsValid(false);
			throw new CommandValidityException("Command Parameters are invalid. Please check and try again.");
		}
	}

	// API Key Check Diffie-Hellman key exchange
	private boolean checkAPI(){
		valApi = true;
		return valApi;
	}

	public void setIsValid(boolean valid){
		this.valCom = valid;
	}

	public boolean isValid(){
		return valCom;
	}

	public void execute(){
		// If true validity, start.
		if(recCom.getCommand().equals("SET")){
			throw new UnimplementedCommandException();
		}

		if(recCom.getCommand().equals("FETCH")){

			if(recCom.getType().equals("SIMPLE") || recCom.getType().equals("DETAIL")){
				int state = -1;
	                        try{
	                                state = recCom.getState();
	                        }catch(Exception Ex){
	                                Command errorCommand = new Command();
	                                errorCommand.setCommand("ERROR");
	                                oos.writeObject(errorCommand);
	                        }
	                        // Retrieve set and return object.
	                        SummaryCollection sc = ds.getSummaryCollections();
	
	                        // If no active runs present return command with details.
	                        if(sc.getCollectionCount() == 0){
	                                Command serverAnswer = new Command();
	                                serverAnswer.setCommand("ERROR");
	                                oos.writeObject(serverAnswer);
	                        // If request format is in XML
	                        }else if(recCom.getFormat().equals("XML")){
	                                String collectionString = sc.getSummaryCollectionXMLAsString(recCom);
	                                if(collectionString.equals("")){
	                                        oos.writeObject("");
	                                }else{
	                                        oos.writeObject(collectionString);
	                                }
	                        }else{ // Else return the SummaryCollection
	                                oos.writeObject(sc);
	                        }
	                        sc = null;
			}

			if(recCom.getType().equals("METRIC")){
				// Retrieve summary from database and check metric availability.
				Summary sum = ds.getSummaryByRunName(recCom.getRunId());

				if(!sum.equals(null)){
					String runDir = sum.getRunDirectory();
				        String extractionMetrics = runDir + "/InterOp/" + Constants.EXTRACTION_METRICS;
		       		        String tileMetrics = runDir + "/InterOp/" + Constants.TILE_METRICS;
			       	        String qualityMetrics = runDir + "/InterOp/" + Constants.QMETRICS_METRICS;
		
					if(!sum.hasClusterDensity() || !sum.hasClusterDensityPF() || !sum.hasPhasingMap() || !has.prePhasingMap()){
						// Try parsing Tile Metrics.
				                TileMetrics tm = new TileMetrics(tileMetrics, 0);
						try{
							tm.digestData();
							sum.setClusterDensity(tm.getCDmap());
				                        sum.setClusterDensityPF(tm.getCDpfMap());
                				        sum.setPhasingMap(tm.getPhasingMap());              // Get all values for summary and populate
				                        sum.setPrephasingMap(tm.getPrephasingMap());
						}catch(IOException Ex){
							// Caught, dont print to log.
							System.out.println("TileMetrics Parsing Error - CommandParser - RecCom\n");
						}
					}

					if(!sum.hasQScoreDist()){
				                QualityMetrics qm = new QualityMetrics(qualityMetrics, 0);
						try{
							QualityScores qsOut = qm.digestData();
							sum.setQScoreDist(qsOut);
						}catch(IOException Ex){
							System.out.println("IOException in QScoreDist Parse - CP\n");
						}
					}

					// Check output formatting method and return.
					if(recCom.getFormat().equals("XML")){
						// Generate XML.
						SummaryCollection sc = new SummaryCollection();					
						sc.appendSummary(sum);
						System.out.println(sc.getSummaryCollectionXMLAsString(recCom));
					}else if(recCom.getFormat().equals("POJO")){
						oos.writeObject(sum); // Send as POJO
					}
				}else{
					// Throw error
				}
								
			}
		}

		if(valCom){
			// check mode
		}
		// return retCom with return value and potential error message
	}

	public void checkMetrics(){
		// To prevent timer delay -- state = 0
		// Check if 
	}
}

