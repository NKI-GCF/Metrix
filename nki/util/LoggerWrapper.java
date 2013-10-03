// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.util;

import java.io.*;
import java.util.logging.*;
import java.util.Properties;

public class LoggerWrapper{
	public static final Logger log = Logger.getLogger(LoggerWrapper.class.getName());  
    private static final Properties configFile;

	static{
		configFile = new Properties();
		// Use external properties file, outside of jar location.
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
	}
    static String asDaemon = configFile.getProperty("DAEMON", "false");
	private static LoggerWrapper instance = null;  
     
	public static LoggerWrapper getInstance() {  
	      if(instance == null) {  
			prepareLogger();  
			instance = new LoggerWrapper ();  
	      } 
		  return instance;  
	   }  
   
	private static void prepareLogger() {
		try{
			FileHandler myFileHandler = new FileHandler("metrixDaemon.log", true);  
		    myFileHandler.setFormatter(new SimpleFormatter());
	   		log.addHandler(myFileHandler);  
			
			if(asDaemon.equals("true")){
				log.setUseParentHandlers(false);
			}else{
				log.setUseParentHandlers(true);
			}
		    log.setLevel(getLevel(configFile.getProperty("LOG_LEVEL", "INFO")));
		}catch(IOException Ex){
			System.out.println("[ERROR] Could not create logfile. " + Ex.toString());
			System.exit(1);
		}
	}

	private static Level getLevel(String lvl){
		if(lvl.equals("ALL")){
			return Level.ALL;
		}else if(lvl.equals("CONFIG")){
			return Level.CONFIG;
		}else if(lvl.equals("FINE")){
			return Level.FINE;
		}else if(lvl.equals("FINER")){
			return Level.FINER;
		}else if(lvl.equals("FINEST")){
			return Level.FINEST;
		}else if(lvl.equals("INFO")){
			return Level.INFO;
		}else if(lvl.equals("OFF")){
			return Level.OFF;
		}else if(lvl.equals("SEVERE")){
			return Level.SEVERE;
		}else if(lvl.equals("WARNING")){
			return Level.WARNING;
		}else{	// Default Level INFO
			return Level.INFO;
		}
	}
}
