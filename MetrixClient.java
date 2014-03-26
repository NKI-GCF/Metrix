// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

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
import java.util.*;
import java.io.*;
import java.util.Properties;
import java.util.logging.Level;
import nki.util.LoggerWrapper;

public class MetrixClient {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
		LoggerWrapper.log.info( "[CLIENT] Initiated");

		// Use external properties file, outside of jar location.
		Properties configFile = new Properties();
    	String externalFileName = System.getProperty("properties");
	    String absFile = (new File(externalFileName)).getAbsolutePath();

        try (InputStream fin = new FileInputStream(new File(absFile))) {
            configFile.load(fin);
		}catch(FileNotFoundException FNFE){
			System.out.println("[ERROR] Properties file not found.");
			System.exit(1);	
		}catch(IOException Ex){
			System.out.println("[ERROR] Reading properties file. " + Ex.toString());
			System.exit(1);
 		}

        int port = Integer.parseInt(configFile.getProperty("PORT", "10000"));
		String host = configFile.getProperty("HOST", "localhost");	

    	try{
	        SocketChannel sChannel = SocketChannel.open();
	        sChannel.configureBlocking(true);
	        
	        if(sChannel.connect(new InetSocketAddress(host, port))){

                // Create OutputStream for sending objects.
                ObjectOutputStream oos = new ObjectOutputStream(sChannel.socket().getOutputStream());

                // Cteate Inputstream for receiving objects.
		        ObjectInputStream ois = new ObjectInputStream(sChannel.socket().getInputStream());
				String prevUpdate = "";

				try{
					nki.objects.Command sendCommand = new nki.objects.Command();
					
					// Set a value for command
					sendCommand.setFormat(Constants.COM_FORMAT_OBJ);
					sendCommand.setState(12); // Select run state (1 - running, 2 - finished, 3 - errors / halted, 4 - FC needs turn, 5 - init) || 12 - ALL
					sendCommand.setCommand("FETCH");
					sendCommand.setMode("CALL");
					sendCommand.setType("DETAIL"); // You can also make use of the available Constants here.
//					sendCommand.setRunId(""); // Use run directory path as string (no trailing slash) or if a State is desired, use setState and comment out setRunId() method.
					oos.writeObject(sendCommand);
					oos.flush();
					
					boolean listen = true;
	
					Object serverAnswer = new Object();
	
					while(ois != null){
						serverAnswer = ois.readObject();
						if(serverAnswer instanceof Command){	// Answer is a Command with info message.
							nki.objects.Command commandIn = (nki.objects.Command) serverAnswer;
							if(commandIn.getCommand() != null){
								System.out.println("[SERVER] " + commandIn.getCommand());
							}
						}
	
						/*
						 * Requested Data collection
						 */

						if(serverAnswer instanceof SummaryCollection){
							SummaryCollection sc = (SummaryCollection) serverAnswer;
							ListIterator litr = sc.getSummaryIterator();

							while(litr.hasNext()){
								Summary sum = (Summary) litr.next();

					// The following is an example. You can use any 'get'-method described in the Summary object (nki/objects/Summary,java) to access the parsed information.
								System.out.println(sum.getRunId() + " - Current Cycle: " + sum.getCurrentCycle() + "/" + sum.getTotalCycles());
								listen = false;
							
								System.out.println("QScore Distribution");
								if(sum.hasQScoreDist()){
									System.out.println(sum.getQScoreDist().toTab());
								}else{
									System.out.println("No QScore Distribution available at this time.");
								}

								System.out.println("Cluster Density / Lane");
								if(sum.hasClusterDensity()){
									System.out.println(sum.getClusterDensity().toTab());
								}else{
									System.out.println("No Cluster Density metrics available at this time.");
								}

								System.out.println("Cluster Density Passing Filter / Lane");
								if(sum.hasClusterDensityPF()){
									System.out.println(sum.getClusterDensityPF().toTab());
								}else{
									System.out.println("No Cluster Density Passing Filter metrics available at this time.");
								}

								System.out.println("Phasing / Lane");
								if(sum.hasPhasing()){
									System.out.println(sum.getPhasingMap().toTab());
								}else{
									System.out.println("No Phasing metrics available at this time.");
								}

								System.out.println("Prephasing / Lane");
								if(sum.hasPhasing()){
									System.out.println(sum.getPrephasingMap().toTab());
								}else{
									System.out.println("No Prephasing metrics available at this time.");
								}

								System.out.println("IntensityScore Avg");
								if(sum.hasIntensityDistAvg()){
									System.out.println(sum.getIntensityDistAvg().toTab());
								}

								System.out.println("IntensityScore CC Avg");
								if(sum.hasIntensityDistCCAvg()){
									System.out.println(sum.getIntensityDistCCAvg().toTab());
								}

								System.out.println("Project/Sample overview");
								if(sum.hasSampleInfo()){
									System.out.println(sum.getSampleInfo().toTab());
								}
							}
						}
	
						if(serverAnswer instanceof String){ 			// Server returned a XML String with results.
							String srvResp = (String) serverAnswer;
							System.out.println(srvResp);
							listen = false;
						}

						/*
						* Update
						*/

						if(serverAnswer instanceof Update){
							Update up = (Update) serverAnswer;
							if(!up.getChecksum().toString().equals(prevUpdate)){
								System.out.println("Finished processing: " + up.getMsg() + "("+up.getCurrentProcessing() + "/" + up.getTotalProcessing() + ")");
								prevUpdate = up.getChecksum().toString();	
							}
						}

						/*
						 *	Exceptions
						 */

						if(serverAnswer instanceof EmptyResultSetCollection){
							System.out.println(serverAnswer.toString());
							listen = false;
						}

						if(serverAnswer instanceof InvalidCredentialsException){
							System.out.println(serverAnswer.toString());
							listen = false;
						}

						if(serverAnswer instanceof MissingCommandDetailException){
							System.out.println(serverAnswer.toString());
							listen = false;
						}

						if(serverAnswer instanceof UnimplementedCommandException){
							System.out.println(serverAnswer.toString());
							listen = false;
						}
					}
				}catch(IOException Ex){
				//	System.out.println("Error" + Ex);
                    LoggerWrapper.log.log(Level.WARNING, "");
				}
	        }
	}catch(  EOFException | NoConnectionPendingException | AsynchronousCloseException ex){
        LoggerWrapper.log.log( Level.INFO, "[CLIENT] Connection closed. ({0})", ex.toString());
	}

	
    }
}
