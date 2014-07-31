package nki.core;

// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2014 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

import java.io.IOException;
import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.channels.*;

import nki.constants.Constants;
import nki.objects.Command;
import nki.objects.Update;
import nki.objects.Summary;
import nki.objects.SummaryCollection;
import nki.exceptions.EmptyResultSetCollection;
import nki.exceptions.MissingCommandDetailException;
import nki.exceptions.UnimplementedCommandException;
import nki.exceptions.InvalidCredentialsException;

import java.io.*;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;

import nki.util.LoggerWrapper;

public class MetrixClient {
  public static void main(String[] args) throws IOException, ClassNotFoundException {
    LoggerWrapper.log.info("[CLIENT] Initiated");

    // Use external properties file, outside of jar location.
    Properties configFile = new Properties();
    String externalFileName = System.getProperty("properties");
    String absFile = (new File(externalFileName)).getAbsolutePath();

    try (InputStream fin = new FileInputStream(new File(absFile))) {
      configFile.load(fin);
    }
    catch (FileNotFoundException FNFE) {
      System.out.println("[ERROR] Properties file not found.");
      System.exit(1);
    }
    catch (IOException Ex) {
      System.out.println("[ERROR] Reading properties file. " + Ex.toString());
      System.exit(1);
    }

    int port = Integer.parseInt(configFile.getProperty("PORT", "10000"));
    String host = configFile.getProperty("HOST", "localhost");

    String searchTerm = "";
    ArrayList<String> searchResults = new ArrayList<>();
    int arrIdx = 0;
    
    if (args.length == 0) {
        System.err.println("[ERROR] Invalid number of arguments.");
        System.exit(1);
    }
    else if (args.length == 1) {
      searchTerm = args[0];

      if (searchTerm.length() <= 2) {
        System.err.println("[ERROR] Need a bigger search string.");
        System.exit(1);
      }
    }
    else if (args.length > 1) {
      System.err.println("[ERROR] Invalid number of arguments.");
      System.exit(1);
    }
    
    
    try {
      SocketChannel sChannel = SocketChannel.open();
      sChannel.configureBlocking(true);

      if (sChannel.connect(new InetSocketAddress(host, port))) {

        // Create OutputStream for sending objects.
        ObjectOutputStream oos = new ObjectOutputStream(sChannel.socket().getOutputStream());

        // Cteate Inputstream for receiving objects.
        ObjectInputStream ois = new ObjectInputStream(sChannel.socket().getInputStream());
        String prevUpdate = "";

        try {
          nki.objects.Command sendCommand = new nki.objects.Command();

          // Set a value for command
          sendCommand.setFormat(Constants.COM_FORMAT_JSON);
          sendCommand.setRunIdSearch(searchTerm);
          sendCommand.setRetType(Constants.COM_SEARCH);

          oos.writeObject(sendCommand);
          oos.flush();

          Object serverAnswer = new Object();

          while (ois != null) {
            serverAnswer = ois.readObject();
            if (serverAnswer instanceof Command) {  // Answer is a Command with info message.
              nki.objects.Command commandIn = (nki.objects.Command) serverAnswer;
              if (commandIn.getMessage()!= null) {
                System.out.println("[SERVER] " + commandIn.getMessage());
              }
            }

            /*
             * Requested Data collection
             */
            if (serverAnswer instanceof SummaryCollection) {
              SummaryCollection sc = (SummaryCollection) serverAnswer;
              for (Summary sum : sc.getSummaryCollection()) {
                // The following is an example. You can use any 'get'-method described in the Summary object (nki/objects/Summary,java) to access the parsed information.
                System.out.println(sum.getRunId() + " - Current Cycle: " + sum.getCurrentCycle() + "/" + sum.getTotalCycles());

              }
            }

            if (serverAnswer instanceof String) {      // Server returned a XML String with results.
              String srvResp = (String) serverAnswer;
              System.out.println(srvResp);
            }

            /*
            * Update
            */

            if (serverAnswer instanceof Update) {
              Update up = (Update) serverAnswer;
              if (!up.getChecksum().toString().equals(prevUpdate)) {
                System.out.println("Finished processing: " + up.getMsg() + "(" + up.getCurrentProcessing() + "/" + up.getTotalProcessing() + ")");
                prevUpdate = up.getChecksum().toString();
              }
            }

            /*
             *	Exceptions
             */
            if (serverAnswer instanceof EmptyResultSetCollection) {
              System.out.println(serverAnswer.toString());
            }

            if (serverAnswer instanceof InvalidCredentialsException) {
              System.out.println(serverAnswer.toString());
            }

            if (serverAnswer instanceof MissingCommandDetailException) {
              System.out.println(serverAnswer.toString());
            }

            if (serverAnswer instanceof UnimplementedCommandException) {
              System.out.println(serverAnswer.toString());
            }
          }
        }
        catch (IOException Ex) {
          //	System.out.println("Error" + Ex);
        }
      }
    }
    catch (EOFException | NoConnectionPendingException | AsynchronousCloseException ex) {
      LoggerWrapper.log.log(Level.INFO, "[CLIENT] Connection closed. ({0})", ex.toString());
    }
  }
}
