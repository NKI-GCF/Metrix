// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.util;

import java.io.*;
import java.util.logging.*;
import java.util.Properties;

public class LoggerWrapper {
  public static final Logger log = Logger.getLogger(LoggerWrapper.class.getName());
  private static final Properties configFile;

  static {
    configFile = new Properties();
    // Use external properties file, outside of jar location.
    if (System.getProperty("properties") == null) {
      System.out.println("[Metrix] Error - 'properties' argument not specified at runtime. Use -Dproperties={Path to properties file}. ");
      System.exit(1);
    }
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
  }

  static String asDaemon = configFile.getProperty("DAEMON", "false");
  private static LoggerWrapper instance = null;

  public static LoggerWrapper getInstance() {
    if (instance == null) {
      prepareLogger();
      instance = new LoggerWrapper();
    }
    return instance;
  }

  private static void prepareLogger() {

    Level propLvl = getLevel(configFile.getProperty("LOG_LEVEL", "INFO"));
    try {
      FileHandler myFileHandler = new FileHandler("metrixDaemon.log", true);

      Logger globalLogger = Logger.getLogger("global");
      Handler[] handlers = globalLogger.getHandlers();
      for (Handler handler : handlers) {
        globalLogger.removeHandler(handler);
      }

      myFileHandler.setFormatter(new SingleLineFormatter());
      myFileHandler.setLevel(propLvl);
      log.addHandler(myFileHandler);

      if (!asDaemon.equalsIgnoreCase("true")) {
//				log.setUseParentHandlers(true);
        ConsoleHandler myConsoleHandler = new ConsoleHandler();
        myConsoleHandler.setFormatter(new SingleLineFormatter());
        myConsoleHandler.setLevel(propLvl);
        log.addHandler(myConsoleHandler);
      }
      log.setUseParentHandlers(false);

      log.setLevel(propLvl);
    }
    catch (IOException Ex) {
      System.out.println("[ERROR] Could not create logfile. " + Ex.toString());
      System.exit(1);
    }
  }

  private static Level getLevel(String lvl) {
    switch (lvl) {
      case "ALL":
        return Level.ALL;
      case "CONFIG":
        return Level.CONFIG;
      case "FINE":
        return Level.FINE;
      case "FINER":
        return Level.FINER;
      case "FINEST":
        return Level.FINEST;
      case "INFO":
        return Level.INFO;
      case "OFF":
        return Level.OFF;
      case "SEVERE":
        return Level.SEVERE;
      case "WARNING":
        return Level.WARNING;
      default:
        // Default Level INFO
        return Level.INFO;
    }
  }
}
