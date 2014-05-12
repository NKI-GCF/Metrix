// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;

import nki.objects.Command;
import nki.exceptions.CommandValidityException;
import nki.exceptions.InvalidCredentialsException;
import nki.io.DataStore;
import nki.parsers.metrix.CommandProcessor;
import nki.constants.Constants;
import nki.util.LoggerWrapper;

public class MetrixThread extends Thread {
  private SocketChannel sChannel = null;
  private boolean timedBool = false;

  // Server logging of client connections and interactions.
  private static final LoggerWrapper metrixLogger = LoggerWrapper.getInstance();


  public MetrixThread(SocketChannel sChannel) {
    super("MetrixThread");
    this.sChannel = sChannel;
  }

  @Override
  public void run() {

    try {
      // Store socket address for future use.
      String clientSocketDetails = sChannel.socket().getRemoteSocketAddress().toString();

      // Create OutputStream for sending objects.
      ObjectOutputStream oos = new ObjectOutputStream(sChannel.socket().getOutputStream());

      // Cteate Inputstream for receiving objects.
      ObjectInputStream ois = new ObjectInputStream(sChannel.socket().getInputStream());

      // DataStore instantiation
      DataStore ds = new DataStore();

      try {
        Command commandClient;
        Object clientMsg;
        while ((clientMsg = ois.readObject()) != null) {
          String mode = "";
          if (clientMsg instanceof Command) {
            commandClient = (Command) clientMsg;
            mode = commandClient.getMode();

            CommandProcessor cp;

            try {
              // Mode Check
              if (mode.equals(Constants.COM_MODE_TIMED)) {  // Keep alive repetitive command
                timedBool = true;
                while (timedBool) {
                  cp = new CommandProcessor(commandClient, oos, ds);
                  Thread.sleep(commandClient.getTimedInterval());
                }
              }

              if (mode.equals(Constants.COM_MODE_CALL)) {  // Single call
                LoggerWrapper.log.log(Level.INFO, "[SERVER] Received command [{0}]: {1} run(s) with state: {2} ({3}) in format {4}", new Object[]{sChannel.socket().getInetAddress().getHostAddress(), commandClient.getCommand(), commandClient.getState(), commandClient.getRetType(), commandClient.getFormat()});
                cp = new CommandProcessor(commandClient, oos, ds);
              }

              // Server Exceptions and important logging.
            }
            catch (CommandValidityException CVE) {
              metrixLogger.log.log(Level.WARNING, "Command Validity Exception! {0}", CVE);
            }
            catch (InvalidCredentialsException ICE) {
              metrixLogger.log.log(Level.WARNING, "Invalid Credentials Exception! {0}", ICE);
            }
            finally {
              // Close all channels and client streams.
              ds = null;
              sChannel.socket().close();
              sChannel.close();
              ois.close();
              oos.close();
            }
          }
          else if (clientMsg instanceof String) {
            LoggerWrapper.log.log(Level.INFO, "[SERVER] Received command via socket. {0}", clientMsg);
          }
          else {
            metrixLogger.log.log(Level.WARNING, "[SERVER] Command not understood [{0}]", clientMsg);
          }

          metrixLogger.log.info("[SERVER] Finished processing command");
        }
      }
      catch (ClassNotFoundException CNFE) {
        CNFE.printStackTrace();
      }
      catch (Exception Ex) {
        //	metrixLogger.log.info( "Disconnect from client. ");
      }

    }
    catch (IOException Ex) {
      metrixLogger.log.log(Level.WARNING, "[Log] Client disconnected or IOException {0}", Ex.toString());
    }

  }
}	

