// Illumina Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

import java.io.IOException;
import java.io.EOFException;
import java.lang.Exception;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.channels.*;
import nki.objects.Command;
import nki.objects.Summary;
import nki.objects.SummaryCollection;
import java.util.logging.*;
import java.util.*;
import java.io.*;
import java.util.HashMap;
import java.util.Properties;
import nki.objects.MutableInt;

public class MetrixClient {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        final Logger metrixLogger = Logger.getLogger(MetrixClient.class.getName());
	metrixLogger.log(Level.INFO, "[CLIENT] Initiated");

        Properties configFile = new Properties();

        // Use external properties file, outside of jar location.
        String externalFileName = System.getProperty("properties");
        String absFile = (new File(externalFileName)).getAbsolutePath();

        InputStream fin = new FileInputStream(new File(absFile));
        configFile.load(fin);

        int port = Integer.parseInt(configFile.getProperty("PORT", "10000"));
	String host = configFile.getProperty("HOST", "localhost");	

	try{
	        SocketChannel sChannel = SocketChannel.open();
	        sChannel.configureBlocking(true);
	        if (sChannel.connect(new InetSocketAddress(host, port))){

                        // Create OutputStream for sending objects.
                        ObjectOutputStream oos = new ObjectOutputStream(sChannel.socket().getOutputStream());

                        // Cteate Inputstream for receiving objects.
		        ObjectInputStream ois = new ObjectInputStream(sChannel.socket().getInputStream());

			try{
				Command sendCommand = new Command();
				// Set a value for command
				sendCommand.setFormat("XML");
				sendCommand.setState(2); // Only fetch finished runs. 
				sendCommand.setCommand("FETCH");
				oos.writeObject(sendCommand);
				oos.flush();

				Object serverAnswer = new Object();

				while(( serverAnswer = ois.readObject()) != null){
					if(serverAnswer instanceof Command){	// Answer is a Command with info message.
						Command commandIn = (Command) serverAnswer;
						if(commandIn.getCommandString() != null){
							System.out.println("[SERVER] " + commandIn.getCommandDetail());
						}
					}

					if(serverAnswer instanceof HashMap){	// Answer is a Summary. Cast object here and use methods from Summary to access details. 
						
					}
			
					if(serverAnswer instanceof SummaryCollection){
						SummaryCollection sc = (SummaryCollection) serverAnswer;
						metrixLogger.log(Level.INFO, "[CLIENT] The server answered with a SummaryCollection.");
						ListIterator litr = sc.getSummaryIterator();

						int count = 1;
						while(litr.hasNext()){
							Summary sum = (Summary) litr.next();
						//	System.out.println(count + " - " + sum.getRunId() + " - Current Cycle: " + sum.getCurrentCycle());
							count++;
						}

					}

					if(serverAnswer instanceof String){
						String srvResp = (String) serverAnswer;
						//System.out.println(srvResp);
					}
				}

			}catch(IOException Ex){
				metrixLogger.log(Level.SEVERE, "IOException in Metrix Client.", Ex.toString());
			}
	        }
	}catch(EOFException ex){
		metrixLogger.log(Level.INFO, "Server has shutdown.");
	}catch(NoConnectionPendingException NCPE){
		metrixLogger.log(Level.SEVERE, "Communication channel is not connection and no operation has been initiated.");
	}catch(AsynchronousCloseException ACE){
		metrixLogger.log(Level.SEVERE, "Another client has shutdown the server. Channel communication prohibited by issueing a direct command.");
	}

	
    }
}
