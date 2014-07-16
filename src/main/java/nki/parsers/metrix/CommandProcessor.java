package nki.parsers.metrix;

import nki.objects.Command;
import nki.objects.Summary;
import nki.objects.SummaryCollection;
import nki.constants.Constants;
import nki.io.DataStore;
import nki.objects.QualityScores;
import nki.objects.QScoreDist;
import nki.objects.IntensityScores;
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

import java.io.*;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ListIterator;
import java.util.logging.Level;
import nki.decorators.MetrixSummaryCollectionDecorator;

public final class CommandProcessor {

  private boolean valCom = false;
  private boolean valApi = false;

  private Command retCom;
  private Command recCom;

  private ObjectOutputStream oos;
  private ObjectInputStream ois;
  private DataStore ds;
  private static final LoggerWrapper metrixLogger = LoggerWrapper.getInstance();

  public CommandProcessor(
      Command command,
      ObjectOutputStream oos,
      DataStore ds
  ) throws
    CommandValidityException,
    InvalidCredentialsException,
    EmptyResultSetCollection,
    IOException,
    UnimplementedCommandException {
    // Process command.
    this.recCom = command;
    this.oos = oos;
    this.ds = ds;

    if (!checkAPI()) {
      InvalidCredentialsException ICE = new InvalidCredentialsException("The supplied API key is incorrect for this user. Please check.");
      oos.writeObject(ICE);    // Write to client
      throw ICE;          // Throw to server
    }

    // Perform validity checks
    if (recCom.checkParams()) {
      // Set validity
      setIsValid(true);
      try {
        execute();
      }
      catch (UnimplementedCommandException UCE) {
        // Create command and send back error.
        oos.writeObject(UCE);
        LoggerWrapper.log.log(Level.WARNING, "Unimplemented Command Exception: {0}", UCE.toString());
      }
      catch (MissingCommandDetailException MCDE) {
        // Send back error over network in command.
        oos.writeObject(MCDE);
        LoggerWrapper.log.log(Level.WARNING, "Missing Command Detail Exception: {0}", MCDE.toString());
      }
      catch (EmptyResultSetCollection ERSC) {
        // Send back error over network in command.
        oos.writeObject(ERSC);
        LoggerWrapper.log.log(Level.WARNING, "Empty Result Set Collection Exception: {0}", ERSC.toString());
      }
      catch (Exception Ex) {
        oos.writeObject(Ex);
        LoggerWrapper.log.log(Level.SEVERE, "Uncaught exception in CommandProcessor: {0}", Ex);
      }

    }
    else {
      setIsValid(false);
      throw new CommandValidityException("Command Parameters are invalid. Please check and try again.");
    }
  }

  // API Key Check
  private boolean checkAPI() {
    valApi = true;
    return valApi;
  }

  public void setIsValid(boolean valid) {
    this.valCom = valid;
  }

  public boolean isValid() {
    return valCom;
  }

  public void execute() throws
                        UnimplementedCommandException,
                        MissingCommandDetailException,
                        EmptyResultSetCollection,
                        Exception {
    /*
    *  Retrieve Summary Collection 
    */
    SummaryCollection sc = new SummaryCollection();
    
    // Check is state is set and required.
    if (recCom.getRetType().equals(Constants.COM_RET_TYPE_BYSTATE) && !recCom.checkState(recCom.getState())) {
      throw new MissingCommandDetailException("Summary State of received command is missing.");
    }
    
    if (recCom.getRetType().equals(Constants.COM_RET_TYPE_BYRUN)) {
      Summary sum = DataStore.getSummaryByRunName(recCom.getRunId());
      sc.appendSummary(sum);
    }
    else if (recCom.getState() == Constants.STATE_ALL_PSEUDO) {
      sc = DataStore.getSummaryCollections();
    }
    else {
      sc = DataStore.getSummaryCollectionByState(recCom.getState());
    }
    
    // If no runs present in collection, throw message.
    if (sc.getCollectionCount() == 0) {
      throw new EmptyResultSetCollection("The results for your search query.");
    }
    
    /*
    * Format Summary Collection according to command specifications.
    */

    MetrixSummaryCollectionDecorator mscd = new MetrixSummaryCollectionDecorator(sc);
    mscd.setExpectedType(recCom.getType()); // SIMPLE or DETAIL
    
        if (recCom.getFormat().equals(Constants.COM_FORMAT_XML)) {
            // Set formatting of summary collection. 
            // Default is SIMPLE
            //String formattedSummaryCollection = sc.toXML();
            //oos.writeObject(formattedSummaryCollection);
        }else if(recCom.getFormat().equals(Constants.COM_FORMAT_JSON)){
            //String formattedSummaryCollection = sc.toJSON();
            oos.writeObject(mscd.toJSON().toString());
        }else if(recCom.getFormat().equals(Constants.COM_FORMAT_TAB)){
            //String formattedSummaryCollection = mscd.toXML().toString();
        }else if(recCom.getFormat().equals(Constants.COM_FORMAT_CSV)){
            String formattedSummaryCollection = mscd.toCSV();
        }else if(recCom.getFormat().equals(Constants.COM_FORMAT_OBJ)){
            oos.writeObject(sc);
        }else{
            // Return plain text
            oos.writeObject("I dont understand.");
        }

        sc = null;
        oos.flush();
        DataStore.closeAll();
  }
}