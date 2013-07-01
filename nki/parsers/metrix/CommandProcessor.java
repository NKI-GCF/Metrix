package nki.parsers.metrix;

import nki.objects.Command;
import nki.objects.Summary;
import nki.objects.SummaryCollection;
import nki.constants.Constants;
import nki.io.DataStore;
import nki.objects.QualityScores;
import nki.objects.QScoreDist;
import nki.objects.IntensityScores;
import nki.parsers.illumina.QualityMetrics;
import nki.parsers.illumina.TileMetrics;
import nki.parsers.illumina.CorrectedIntensityMetrics;
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
			InvalidCredentialsException ICE = new InvalidCredentialsException("The supplied API key is incorrect for this user. Please check.");
			oos.writeObject(ICE);		// Write to client
			throw ICE;					// Throw to server
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
				SummaryCollection sc = new SummaryCollection();

				if(recCom.getRetType().equals(Constants.COM_RET_TYPE_BYRUN)){
					Summary sum = ds.getSummaryByRunName(recCom.getRunId());
					sc.appendSummary(sum);
				}else if(recCom.getState() == Constants.STATE_ALL_PSEUDO){
					sc = ds.getSummaryCollections();
				}else{
					sc = ds.getSummaryCollectionByState(recCom.getState());
				}

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
					if(recCom.getState() == Constants.STATE_ALL_PSEUDO){
						sc = ds.getSummaryCollections();
					}else{
						sc = ds.getSummaryCollectionByState(recCom.getState());
					}
				}
				
				if(sc.getCollectionCount() == 0){
					throw new EmptyResultSetCollection("No Results for your search query.");
				}

				ListIterator litr = sc.getSummaryIterator();

				while(litr.hasNext()){
					Summary sum = (Summary) litr.next();
					System.out.println("Processing " + sum.getRunId());
					if(!sum.equals(null)){
						Boolean update = false;
						String runDir = sum.getRunDirectory();
						if(runDir.equals("")){continue;}
						String extractionMetrics = runDir + "/InterOp/" + Constants.EXTRACTION_METRICS;
						String tileMetrics = runDir + "/InterOp/" + Constants.TILE_METRICS;
						String qualityMetrics = runDir + "/InterOp/" + Constants.QMETRICS_METRICS;
						String intensityMetrics = runDir + "/InterOp/" + Constants.CORRECTED_INT_METRICS;
						long currEpoch = System.currentTimeMillis();
						boolean timeCheck = (currEpoch - sum.getLastUpdatedEpoch()) > Constants.METRIC_UPDATE_TIME;
				
						System.out.println("Current epoch: " + currEpoch + "\tDifference: " + (currEpoch - sum.getLastUpdatedEpoch()) + "\tUpdate Threshold: " + Constants.METRIC_UPDATE_TIME);

						/*
						 * Iterate over checks for metrics parsing.
						 */

						// Process Extraction Metrics

						/*
						 * 
						 */

						if(	!sum.hasClusterDensity() 	||
							!sum.hasClusterDensityPF() 	||
							!sum.hasPhasing() 			||
							!sum.hasPrephasing() 		||
							timeCheck
						){
							// Process Cluster Density and phasing / prephasing
							TileMetrics tm = new TileMetrics(tileMetrics, 0);
							if(!tm.getFileMissing()){								// If TileMetrics File is present - process.
								tm.digestData();
								sum.setClusterDensity(tm.getCDmap());
								sum.setClusterDensityPF(tm.getCDpfMap());
								sum.setPhasingMap(tm.getPhasingMap());              // Get all values for summary and populate
								sum.setPrephasingMap(tm.getPrephasingMap());
								
								// Distribution present in ClusterDensity Object.
								update = true;
							}
						}
						
						// Process QScore Dist
						if(!sum.hasQScores() || timeCheck){
								QualityMetrics qm = new QualityMetrics(qualityMetrics, 0);
								if(!qm.getFileMissing()){
									QualityScores qsOut = qm.digestData();
									sum.setQScores(qsOut);
									// Calculate distribution
									QScoreDist qScoreDist = qsOut.getQScoreDistribution();
									sum.setQScoreDist(qScoreDist);
									update = true;
								}
						}

						// Process Corrected Intensities
						if(!sum.hasIScores() || timeCheck){
							CorrectedIntensityMetrics im = new CorrectedIntensityMetrics(intensityMetrics, 0);
							if(!im.getFileMissing()){
								IntensityScores isOut = im.digestData();
								sum.setIScores(isOut);
								// Calculate distribution
								isOut.getAvgCorIntDist();
								update = true;
							}
						}

						if(update == true){
							try{
								sum.setLastUpdated();
								DataStore.updateSummaryByRunName(sum, runDir);
                            }catch(Exception SEx){
								System.out.println("Exception in update statement " + SEx.toString());
                            }
						}
					}else{
						// Throw error
					}
				}// End SC While iterator
				
				/*
				*  Check output formatting method and return.
				*/
				if(recCom.getFormat().equals(Constants.COM_FORMAT_XML)){
					// Generate XML.
					oos.writeObject(sc.getSummaryCollectionXMLAsString(recCom));
				}else if(recCom.getFormat().equals(Constants.COM_FORMAT_TAB)){
					// Generate TAB separated string
					oos.writeObject(sc.toTab(recCom));
				}else if(recCom.getFormat().equals(Constants.COM_FORMAT_OBJ)){
					// Send back the SummaryCollection POJO
					oos.writeObject(sc); 
				}else{
					// Exception
						throw new MissingCommandDetailException("Requested output format not understood (" + recCom.getFormat() + ")");
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

