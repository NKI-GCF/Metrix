package nki.parsers.metrix;

import nki.objects.Command;
import nki.objects.Summary;
import nki.objects.SummaryCollection;
import nki.constants.Constants;
import nki.io.DataStore;
import nki.objects.QualityScores;
import nki.objects.QScoreDist;
import nki.objects.IntensityScores;
import nki.objects.IntensityDist;
import nki.objects.Indices;
import nki.objects.Update;
import nki.objects.Reads;
import nki.parsers.illumina.QualityMetrics;
import nki.parsers.illumina.TileMetrics;
import nki.parsers.illumina.CorrectedIntensityMetrics;
import nki.parsers.illumina.IndexMetrics;
import nki.exceptions.InvalidCredentialsException;
import nki.exceptions.CommandValidityException;
import nki.exceptions.UnimplementedCommandException;
import nki.exceptions.MissingCommandDetailException;
import nki.exceptions.EmptyResultSetCollection;
import nki.util.LoggerWrapper;
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
	private LoggerWrapper metrixLogger = LoggerWrapper.getInstance();

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
				metrixLogger.log.warning("Unimplemented Command Exception: " + UCE.toString());
			}catch(MissingCommandDetailException MCDE){
				// Send back error over network in command.
				oos.writeObject(MCDE);
				metrixLogger.log.warning("Missing Command Detail Exception: " + MCDE.toString());
			}catch(EmptyResultSetCollection ERSC){
				// Send back error over network in command.
				oos.writeObject(ERSC);
				metrixLogger.log.warning("Empty Result Set Collection Exception: " + ERSC.toString());
			}catch(Exception Ex){
				// Send back error over network in command.
				Ex.printStackTrace();
				oos.writeObject(Ex);
				metrixLogger.log.severe("Uncaught exception in CommandProcessor: " + Ex.toString());
			}

		}else{
			setIsValid(false);
			throw new CommandValidityException("Command Parameters are invalid. Please check and try again.");
		}
	}

	// API Key Check
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

				// Check is state is set and required.
				int state = -1;
				if(recCom.getRetType().equals(Constants.COM_RET_TYPE_BYSTATE) && !recCom.checkState(recCom.getState())){
					throw new MissingCommandDetailException("Summary State of received command is missing.");
				}else{
					state = recCom.getState();
				}
			
				// Setup new SummaryCollection and fill using command parameters (RunID or State)
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
				if(recCom.getRunId() == null && Integer.toString(recCom.getState()) == ""){
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
				int curCount = 1;

				while(litr.hasNext()){
					Summary sum = (Summary) litr.next();
					metrixLogger.log.info("Processing " + sum.getRunId());
					if(!sum.equals(null)){
						Boolean update = false;
						String runDir = sum.getRunDirectory();
						if(runDir.equals("")){continue;}
						String extractionMetrics = runDir + "/InterOp/" + Constants.EXTRACTION_METRICS;
						String tileMetrics = runDir + "/InterOp/" + Constants.TILE_METRICS;
						String qualityMetrics = runDir + "/InterOp/" + Constants.QMETRICS_METRICS;
						String intensityMetrics = runDir + "/InterOp/" + Constants.CORRECTED_INT_METRICS;
						String indexMetrics = runDir + "/InterOp/" + Constants.INDEX_METRICS;
						long currEpoch = System.currentTimeMillis();
						boolean timeCheck = (currEpoch - sum.getLastUpdatedEpoch()) > Constants.METRIC_UPDATE_TIME;
				
						// Process Extraction Metrics
						Reads rds = sum.getReads();

						// Process Cluster Density and phasing / prephasing
						if(	!sum.hasClusterDensity() 	||
							!sum.hasClusterDensityPF() 	||
							!sum.hasPhasing() 			||
							!sum.hasPrephasing() 		||
							timeCheck
						){
							TileMetrics tm = new TileMetrics(tileMetrics, 0);

							if(!tm.getFileMissing()){								// If TileMetrics File is present - process.
								tm.digestData(rds);
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
									QualityScores qsOut = qm.digestData(rds);
								//	sum.setQScores(qsOut);
									// Calculate distribution
									QScoreDist qScoreDist = qsOut.getQScoreDistribution();
									sum.setQScoreDist(qScoreDist);
									update = true;
								}
						}

						// Process Corrected Intensities (+ Avg Cor Int Called Clusters)
						if(!sum.hasIScores() || timeCheck){
							CorrectedIntensityMetrics im = new CorrectedIntensityMetrics(intensityMetrics, 0);
							if(!im.getFileMissing()){
								IntensityScores isOut = im.digestData();

								/* If you would like to store the intensity object, increase the max_allowed_packet with:
								* SET GLOBAL max_allowed_packet = 1024 * 1024 * 50 to prevent a PacketTooBigException for the SQL Update.
								*/

								//sum.setIScores(isOut);

								// Calculate distribution
								sum.setIntensityDistAvg(isOut.getAvgCorIntDist());
								sum.setIntensityDistCCAvg(isOut.getAvgCorIntCCDist());
								update = true;
							}
						}

						// Process Index metrics
						// sum.hasIndexMetrics()
						IndexMetrics im = new IndexMetrics(indexMetrics, 0);
						Indices indices = im.digestData();
						sum.setSampleInfo(indices);
					
						if(update == true){
							try{
								sum.setLastUpdated();
								DataStore.updateSummaryByRunName(sum, runDir);
                            }catch(Exception SEx){
								metrixLogger.log.severe("Exception in update statement " + SEx.toString());
                            }
						}
					}else{
						// Throw error
						metrixLogger.log.severe("[WARNING] Obtained an empty summary.");
					}

					// Generate and send update object.
					Update update = new Update();
					update.setMsg(sum.getRunId());
					update.setCurrentProcessing(curCount);
					update.setTotalProcessing(sc.getCollectionCount());
					update.setChecksum(sum);
					// Reset the stream to ensure proper update objects being sent - Prevent caching.
					oos.reset();
					oos.writeObject(update);
					curCount++;
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
				oos.flush();
				DataStore.closeAll();
				ds.closeAll();
			}
		}

		if(valCom){
			// check mode
		}
		// return retCom with return value and potential error message
	}

}

