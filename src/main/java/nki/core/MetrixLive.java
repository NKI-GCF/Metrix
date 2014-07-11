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

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

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
import java.util.Properties;
import java.util.logging.Level;

import nki.decorators.MetrixContainerDecorator;

import nki.util.LoggerWrapper;

public class MetrixLive {
  public static void main(String[] args) throws IOException, ClassNotFoundException {
    LoggerWrapper.log.info("[CLIENT] Initiated");

    // Use external properties file, outside of jar location.
    Properties configFile = new Properties();
    String externalFileName = System.getProperty("properties");
    String absFile = (new File(externalFileName)).getAbsolutePath();

   // String jsonArg = args[0];
    
    //if(jsonArg == null || jsonArg.equals("")){
    //    System.out.println("[ERROR] No input json argumented.");
    //    System.exit(1);
   // }
    
    // Convert jsonCommand to Metrix Command
    Command sendCommand = new Command();

  //  final JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonArg);

     // Set a value for command
    sendCommand.setFormat(Constants.COM_FORMAT_JSON); // Always return JSON
    sendCommand.setState(Constants.STATE_FINISHED); // Select run state (1 - running, 2 - finished, 3 - errors / halted, 4 - FC needs turn, 5 - init) || 12 - ALL
    sendCommand.setType(Constants.COM_TYPE_SIMPLE); // You can also make use of the available Constants here.    
    
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
            System.out.println("Command: " + sendCommand.getFormat() + "\t  " + sendCommand.getState());
          // Send the parsed command
          oos.writeObject(sendCommand);
          oos.flush();

          boolean listen = true;

          Object serverAnswer = new Object();

          while (ois != null) {
            serverAnswer = ois.readObject();
            if (serverAnswer instanceof Command) {  // Answer is a Command with info message.
              nki.objects.Command commandIn = (nki.objects.Command) serverAnswer;
              if (commandIn.getCommand() != null) {
                System.out.println("[SERVER] " + commandIn.getCommand());
              }
            }

            /*
             * Requested Data collection
             */
            String consoleOut = "";
            
            if (serverAnswer instanceof SummaryCollection) {
              SummaryCollection sc = (SummaryCollection) serverAnswer;
              for (Summary sum : sc.getSummaryCollection()) {
                  
                MetrixContainer mc = new MetrixContainer(sum.getRunDirectory());
                JSONObject allOut = new MetrixContainerDecorator(mc).toJSON();
                consoleOut += ("," + allOut.toString());
              }
              System.out.print(consoleOut);
            }

            if (serverAnswer instanceof String) {      // Server returned a XML String with results.
              String srvResp = (String) serverAnswer;
              System.out.println("RESPONSE " + srvResp);
              listen = false;
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
              listen = false;
            }

            if (serverAnswer instanceof InvalidCredentialsException) {
              System.out.println(serverAnswer.toString());
              listen = false;
            }

            if (serverAnswer instanceof MissingCommandDetailException) {
              System.out.println(serverAnswer.toString());
              listen = false;
            }

            if (serverAnswer instanceof UnimplementedCommandException) {
              System.out.println(serverAnswer.toString());
              listen = false;
            }
          }
        }
        catch (IOException Ex) {
          //	System.out.println("Error" + Ex);
          LoggerWrapper.log.log(Level.WARNING, "EXCEPTION! " + Ex);
        }
      }
    }
    catch (EOFException | NoConnectionPendingException | AsynchronousCloseException ex) {
      LoggerWrapper.log.log(Level.INFO, "[CLIENT] Connection closed. ({0})", ex.toString());
    }
  }
}
