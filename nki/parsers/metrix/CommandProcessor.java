package nki.parsers.metrix;

import nki.objects.Command;
import nki.objects.Summary;
import nki.objects.SummaryCollection;
import nki.constants.Constants;
import nki.io.DataStore;
import nki.objects.QualityScores;
import nki.parsers.illumina.QualityMetrics;
import nki.parsers.illumina.TileMetrics;
import nki.exceptions.InvalidCredentialsException;
import nki.exceptions.CommandValidityException;
import nki.exceptions.UnimplementedCommandException;
import nki.exceptions.MissingCommandDetailException;
import nki.exceptions.EmptyResultSetCollection;
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
	private DataStore ds;

	public CommandProcessor(
				Command command, 
				ObjectOutputStream oos,
				DataStore ds
		)throws 	
			CommandValidityException,
			InvalidCredentialsException 
//			UnimplementedCommandException
		{
		// Process command.
		this.recCom = command;
		this.oos = oos;
		this.ds = ds;

		if(!checkAPI()){
			throw new InvalidCredentialsException("The supplied API key is incorrect for this user. Please check.");
		}

		// Perform validity checks
		if(recCom.checkParams()){
			// Set validity
			setIsValid(true);
			try{
				execute();
			}catch(UnimplementedCommandException UCE){
				// Create command and send back error.	
			}catch(MissingCommandDetailException MCDE){
				// Send back error over network in command.
			}catch(EmptyResultSetCollection ERSC){
				// Send back error over network in command.
			}catch(Exception Ex){
				// Send back error over network in command.
			}

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

	public void execute() throws 
						UnimplementedCommandException, 
						MissingCommandDetailException,
						EmptyResultSetCollection,
						Exception
		{
		// If true validity, start.
		if(recCom.getCommand().equals(Constants.COM_FUNCTION_SET)){
			throw new UnimplementedCommandException("This command ("+recCom.getCommand()+") has not been implemented. ");
		}

		if(recCom.getCommand().equals(Constants.COM_FUNCTION_FETCH)){

			if(recCom.getType().equals(Constants.COM_TYPE_SIMPLE) || recCom.getType().equals(Constants.COM_TYPE_DETAIL)){
				int state = -1;
				try{
						state = recCom.getState();
				}catch(Exception Ex){
					throw new MissingCommandDetailException("Summary State of received command is missing.");
				}
				// Retrieve set and return object.
				SummaryCollection sc = ds.getSummaryCollections();

				// If no active runs present return command with details.
				if(sc.getCollectionCount() == 0){
					throw new EmptyResultSetCollection("The argumented command did not produce results.");
				// If request format is in XML
				}else if(recCom.getFormat().equals(Constants.COM_FORMAT_XML)){
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

			if(recCom.getType().equals(Constants.COM_TYPE_METRIC)){
				// Retrieve summary from database and check metric availability.
				if(recCom.getRunId() == null){
					throw new MissingCommandDetailException("Please supply a runID for the requested metrics.");
				}
				Summary sum = ds.getSummaryByRunName(recCom.getRunId());

				if(!sum.equals(null)){
					String runDir = sum.getRunDirectory();
				    String extractionMetrics = runDir + "/InterOp/" + Constants.EXTRACTION_METRICS;
		       		String tileMetrics = runDir + "/InterOp/" + Constants.TILE_METRICS;
			       	String qualityMetrics = runDir + "/InterOp/" + Constants.QMETRICS_METRICS;
		
					if(!sum.hasClusterDensity() || !sum.hasClusterDensityPF() || !sum.hasPhasing() || !sum.hasPrephasing()){
						// Try parsing Tile Metrics.
				        TileMetrics tm = new TileMetrics(tileMetrics, 0);
						tm.digestData();
						sum.setClusterDensity(tm.getCDmap());
                        sum.setClusterDensityPF(tm.getCDpfMap());
				        sum.setPhasingMap(tm.getPhasingMap());              // Get all values for summary and populate
           		        sum.setPrephasingMap(tm.getPrephasingMap());
					}

					if(!sum.hasQScores()){
				            QualityMetrics qm = new QualityMetrics(qualityMetrics, 0);
							QualityScores qsOut = qm.digestData();
							sum.setQScores(qsOut);
							sum.setQScoreDist(qsOut.getQScoreDistribution());
					}

					if(sum.hasQScoreDist()){
						System.out.println("QScore Distribution available.");
						System.out.println(sum.getQScoreDist().toTab());
					}else{
						System.out.println("No Dist available.");
					}

					// Check output formatting method and return.
					if(recCom.getFormat().equals(Constants.COM_FORMAT_XML)){
						// Generate XML.
						SummaryCollection sc = new SummaryCollection();					
						sc.appendSummary(sum);
						System.out.println(sc.getSummaryCollectionXMLAsString(recCom));
						oos.writeObject(sc.getSummaryCollectionXMLAsString(recCom));
					}else if(recCom.getFormat().equals(Constants.COM_FORMAT_OBJ)){
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

