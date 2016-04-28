package nki.core;

// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2014 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nki.exceptions.CommandValidityException;
import nki.exceptions.InvalidCredentialsException;
import nki.io.DataStore;
import nki.objects.Command;
import nki.parsers.metrix.CommandProcessor;

public class MetrixThread extends Thread {
  private SocketChannel sChannel = null;
  private boolean timedBool = false;

  // Server logging of client connections and interactions.
  protected static final Logger log = LoggerFactory.getLogger(MetrixThread.class);

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
          if (clientMsg instanceof Command) {
            commandClient = (Command) clientMsg;

            try {
              // LoggerWrapper.log.log(Level.FINE, "[SERVER] Received command
              // [{0}]: Fetch run(s) with state: {1} ({2}) in format {3}", new
              // Object[]{sChannel.socket().getInetAddress().getHostAddress(),
              // commandClient.getState(), commandClient.getRetType(),
              // commandClient.getFormat()});
              log.info("[SERVER] Calling Command processor.");
              CommandProcessor cp = new CommandProcessor(commandClient, oos, ds);
            }
            catch (CommandValidityException CVE) {
              log.warn("Command Validity Exception! ", CVE);
            }
            catch (InvalidCredentialsException ICE) {
              log.warn("Invalid Credentials Exception! ", ICE);
            } finally {
              // Close all channels and client streams.
              ds = null;
              sChannel.socket().close();
              sChannel.close();
              ois.close();
              oos.close();
            }
          }
          else if (clientMsg instanceof String) {
            log.info("[SERVER] Received command via socket. " + clientMsg);
          }
          else {
            log.warn("[SERVER] Command not understood [" + clientMsg + "]");
          }

          log.info("[SERVER] Finished processing command");
        }
      }
      catch (ClassNotFoundException CNFE) {
        CNFE.printStackTrace();
      }
      catch (Exception Ex) {
        log.info("Disconnect from client. ", Ex);
      }

    }
    catch (IOException Ex) {
      log.warn("[Log] Client disconnected.", Ex);
    }

  }
}
