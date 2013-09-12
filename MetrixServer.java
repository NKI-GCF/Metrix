// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.*;
import java.nio.*;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.logging.*;
import java.util.Properties;
import java.util.concurrent.*;
import nki.io.DataStore;
import nki.util.LoggerWrapper;

public class MetrixServer{

	static boolean listening = true;
    
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	// Instantiate Logger	
	private LoggerWrapper metrixLogger = LoggerWrapper.getInstance();

    public void run() throws IOException {
		// Use external properties file, outside of jar location.
		Properties configFile = new Properties();
    	String externalFileName = System.getProperty("properties");
	    String absFile = (new File(externalFileName)).getAbsolutePath();

    	try{
			InputStream fin = new FileInputStream(new File(absFile));
		    configFile.load(fin);
			fin.close();
		}catch(FileNotFoundException FNFE){
			System.out.println("[ERROR] Properties file not found.");
			System.exit(1);	
		}catch(IOException Ex){
			System.out.println("[ERROR] Reading properties file. " + Ex.toString());
			System.exit(1);
 		}

		int port = Integer.parseInt(configFile.getProperty("PORT", "10000"));
		String runDir = configFile.getProperty("RUNDIR", "/tmp/");
	    String asDaemon = configFile.getProperty("DAEMON", "false");
		
		if(asDaemon.equals("false")){
			System.out.println("Metrix - A server / client interface for Illumina Sequencing Metrics.");
			System.out.println("Copyright (C) 2013 Bernd van der Veen\n");
			System.out.println("This program comes with ABSOLUTELY NO WARRANTY;");
			System.out.println("This is free software, and you are welcome to redistribute it");
			System.out.println("under certain conditions; for more information please see LICENSE.txt\n");
		}

		metrixLogger.log.info( "Metrix Server initializing...");

		try{
			// Initialize datastore for sequence run summary data.
			DataStore ds = new DataStore();
			if(ds.conn == null){
				metrixLogger.log.severe( "Cannot establish MySQL connection.");
				System.exit(1);
			}
		
        	metrixLogger.log.info( "Initializing Directory Watcher Service with directory: " + runDir);
			// Start Watcher service
				final MetrixWatch mw = new MetrixWatch(runDir, false, ds);
    	    	mw.start();
		
				metrixLogger.log.info( "Directory Watcher Service started - Monitoring.");
	
			// Configure Server paramters
	        	ServerSocketChannel ssChannel = ServerSocketChannel.open();
		        ssChannel.configureBlocking(true);
		        ssChannel.socket().bind(new InetSocketAddress(port));	// Call server / Bind socket and port.
		
    	    	metrixLogger.log.info( "Metrix Thread Server initialized.");

				metrixLogger.log.info( "Initializing backlog service.");
			final Runnable backlog = new Runnable() {
	            public void run() {
					if(mw.watcher != null){
						mw.checkForceTime();
					}
				}
        	};

			final ScheduledFuture<?> backlogHandle = scheduler.scheduleAtFixedRate(backlog, 10, 20, TimeUnit.MINUTES);
			scheduler.schedule(new Runnable() {
				public void run(){
					backlogHandle.cancel(true);
				}
		    }, 365, TimeUnit.DAYS);
		
			// While server is alive, accept new connections.
    	    while (listening) {
				new MetrixThread(ssChannel.accept()).start();
        	}
		}catch(IOException Ex){
			metrixLogger.log.severe( "IOException initializing server. " + Ex.toString());
			Ex.printStackTrace();
			System.exit(1);	
		}
    }
}
