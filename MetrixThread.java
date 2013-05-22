// Illumina Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

import java.net.*;
import java.io.*;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.logging.*;
import nki.objects.Command;
import nki.objects.Summary;
import nki.objects.SummaryCollection;
import nki.io.DataStore;
import nki.constants.Constants;

public class MetrixThread extends Thread {
	private SocketChannel sChannel = null;
	private boolean timedBool = false;	

	// SERVER LOGGING HERE FOR INSTANTIATION OF CLIENT...
	final Logger metrixLogger = Logger.getLogger(MetrixThread.class.getName());

	public MetrixThread(SocketChannel sChannel){
		super("MetrixThread");
		this.sChannel = sChannel;
	}

	public void run(){
		
		try{
//		        final Logger metrixLogger = Logger.getLogger(MetrixThread.class.getName());

			String clientSocketDetails = sChannel.socket().getRemoteSocketAddress().toString();

			metrixLogger.log(Level.INFO, "[SERVER] Client connection accepted at: " + clientSocketDetails);

			// Create OutputStream for sending objects.
			ObjectOutputStream  oos = new ObjectOutputStream(sChannel.socket().getOutputStream());
			
			// Cteate Inputstream for receiving objects.
			ObjectInputStream ois = new ObjectInputStream(sChannel.socket().getInputStream());
			
			// MetrixLogic instantiation
			MetrixLogic ml = new MetrixLogic();

			// DataStore instantiation
			DataStore ds = new DataStore();

			try{
				Command commandClient;

				while (( commandClient = (Command) ois.readObject()) != null){
					String mode = "";
					if(commandClient instanceof Command){
						mode = commandClient.getMode();
						
						// Mode Check
						if(mode.equals("TIMED")){	// Keep alive repetitive command
							timedBool = true;
							while(timedBool){
								processCommand(oos, ois,ml, ds, commandClient);
								Thread.sleep(commandClient.getTimedInterval());	
							}
						}

						if(mode.equals("CALL")){	// Single call
							processCommand(oos, ois, ml, ds, commandClient);
						}
						
					}else{
						metrixLogger.log(Level.WARNING, "[SERVER] Command not understood [" + commandClient + "]");
					}
					
					if(mode.equals("CALL")){
			                        // Close all channels and client streams.
						ml = null;
						ds = null;
						sChannel.socket().close();
						sChannel.close();
						ois.close();
						oos.close();
					}
					metrixLogger.log(Level.INFO, "[SERVER] Finished processing command");
				}
			}catch(ClassNotFoundException CNFE){
				CNFE.printStackTrace();
			}catch(Exception Ex){
			//	metrixLogger.log(Level.INFO, "Disconnect from client. ");
			}

		}catch(IOException Ex){
			System.err.println("[Log] Client disconnected or IOException " + Ex.toString());
		}
	}

	private void processCommand(ObjectOutputStream oos, ObjectInputStream ois, MetrixLogic ml, DataStore ds, Command commandClient) throws IOException, Exception{
		// Get active runs
		if(commandClient.getCommand().equals("FETCH")){
			int state = -1;

            metrixLogger.log(Level.INFO, "[CLIENT] Fetch runs.");

			try{
				state = commandClient.getState();
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
			}else if(commandClient.getFormat().equals("XML")){
				String collectionString = sc.getSummaryCollectionXMLAsString(commandClient);
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
	}
}	

