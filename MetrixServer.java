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


public class MetrixServer{

	static boolean listening = true;
	private static final Logger metrixLogger = Logger.getLogger(MetrixServer.class.getName());
        private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
 
    public void run() throws IOException {
	System.out.println("Metrix - A server / client interface for Illumina Sequencing Metrics.");
	System.out.println("Copyright (C) 2013 Bernd van der Veen\n");
	System.out.println("This program comes with ABSOLUTELY NO WARRANTY;");
	System.out.println("This is free software, and you are welcome to redistribute it");
	System.out.println("under certain conditions; for more information please see LICENSE.txt\n");

	metrixLogger.log(Level.INFO, "Metrix Server initializing...");
	Properties configFile = new Properties();

	// Use external properties file, outside of jar location.
	String externalFileName = System.getProperty("properties");
	String absFile = (new File(externalFileName)).getAbsolutePath();

	InputStream fin = new FileInputStream(new File(absFile));
	configFile.load(fin);

	int port = Integer.parseInt(configFile.getProperty("PORT", "10000"));
	String runDir = configFile.getProperty("RUNDIR", "/tmp/");

	configFile = null;
	fin.close();

	try{
		// Initialize datastore for sequence run summary data.
		DataStore ds = new DataStore();
		if(ds.conn == null){
			metrixLogger.log(Level.SEVERE, "Cannot establish MySQL connection.");
		}
		
                metrixLogger.log(Level.INFO, "Initializing Directory Watcher Service.");
		// Start Watcher service
		final MetrixWatch mw = new MetrixWatch(runDir, false, ds);
                mw.start();
		metrixLogger.log(Level.INFO, "Directory Watcher Service started - Monitoring.");
	
		// Configure Server paramters
	        ServerSocketChannel ssChannel = ServerSocketChannel.open();
	        ssChannel.configureBlocking(true);
	        ssChannel.socket().bind(new InetSocketAddress(port));	// Call server / Bind socket and port.
	
        	metrixLogger.log(Level.INFO, "Metrix Thread Server initialized.");

		metrixLogger.log(Level.INFO, "Initializing backlog folder iterator.");
		final Runnable backlog = new Runnable() {
	                public void run() {
				mw.checkForceTime();
			}
        	};

		final ScheduledFuture<?> backlogHandle = scheduler.scheduleAtFixedRate(backlog, 10, 20, TimeUnit.MINUTES);
		       scheduler.schedule(new Runnable() {
		       	public void run() { backlogHandle.cancel(true); }
		       }, 365, TimeUnit.DAYS);
		
	
		// While server is alive, accept new connections.
        	while (listening) {
                        new MetrixThread(ssChannel.accept()).start();
        	}
	}catch(IOException Ex){
		metrixLogger.log(Level.SEVERE, "IOException initializing server.", Ex.toString());
		Ex.printStackTrace();
		System.exit(1);	
	}
    }
}
