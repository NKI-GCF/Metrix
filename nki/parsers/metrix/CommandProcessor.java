package nki.parsers.metrix;

import nki.objects.Command;
import nki.objects.Summary;
import nki.objects.SummaryCollection;
import nki.constants.Constants;
import nki.io.DataStore;
import nki.objects.QualityScores;
import nki.objects.QScoreDist;
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;

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
			InvalidCredentialsException, 
			EmptyResultSetCollection,
			IOException,
			UnimplementedCommandException
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
				oos.writeObject(UCE);
			}catch(MissingCommandDetailException MCDE){
				// Send back error over network in command.
				oos.writeObject(MCDE);
			}catch(EmptyResultSetCollection ERSC){
				// Send back error over network in command.
				oos.writeObject(ERSC);
			}catch(Exception Ex){
				// Send back error over network in command.
				oos.writeObject(Ex);
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

			/*	
			 *	Process a simple / detailed run info request.
			*/

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
					throw new EmptyResultSetCollection("The command parameters did not produce results.");
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

			/*	
			 *	Process a metrics request.
			*/

			if(recCom.getType().equals(Constants.COM_TYPE_METRIC)){
				// Retrieve summary from database and check metric availability.
				if(recCom.getRunId() == null && recCom.getState()+"" == ""){
					throw new MissingCommandDetailException("Please supply parameters (Run State or Run Id) for the requested metrics.");
				}

				SummaryCollection sc = new SummaryCollection();
				
				if(recCom.getRetType().equals(Constants.COM_RET_TYPE_BYRUN)){
					Summary sum = ds.getSummaryByRunName(recCom.getRunId());
					sc.appendSummary(sum);
				}else{
					sc = ds.getSummaryCollectionByState(recCom.getState());
				}
				
				if(sc.getCollectionCount() == 0){
					throw new EmptyResultSetCollection("No Results for your search query.");
				}

				ListIterator litr = sc.getSummaryIterator();

				while(litr.hasNext()){
					Summary sum = (Summary) litr.next();

					if(!sum.equals(null)){
						Boolean update = false;
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
							update = true;
						}
						
						if(!sum.hasQScores()){
								QualityMetrics qm = new QualityMetrics(qualityMetrics, 0);
								QualityScores qsOut = qm.digestData();
								sum.setQScores(qsOut);
								QScoreDist qScoreDist = qsOut.getQScoreDistribution();
								sum.setQScoreDist(qScoreDist);
								update = true;
						}

						if(update == true){				
							try{
                                   DataStore.updateSummaryByRunName(sum, runDir);
                            }catch(Exception SEx){
                                   System.out.println("Exception in update statement " + SEx.toString());
                            }
						}
					}else{
						// Throw error
					}
				}// End SC While iterator

				// Check output formatting method and return.
				if(recCom.getFormat().equals(Constants.COM_FORMAT_XML)){
					// Generate XML.
					oos.writeObject(sc.getSummaryCollectionXMLAsString(recCom));
				}else if(recCom.getFormat().equals(Constants.COM_FORMAT_OBJ)){
					oos.writeObject(sc); // Send as POJO
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

