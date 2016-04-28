package nki.core;

// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2014 Bernd van der Veen

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.*;

import nki.io.DataStore;

public class MetrixServer {

  static boolean listening = true;

  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  // Instantiate Logger
  protected static final Logger log = LoggerFactory.getLogger(MetrixLive.class);

  public void run() throws IOException {
    // Use external properties file, outside of jar location.
    Properties configFile = new Properties();

    if (System.getProperty("properties") == null) {
      System.out.println("[Metrix] Error - 'properties' argument not specified at runtime. Use -Dproperties={Path to properties file}. ");
      System.exit(1);
    }

    String externalFileName = System.getProperty("properties");
    String absFile = (new File(externalFileName)).getAbsolutePath();

    try {
      InputStream fin = new FileInputStream(new File(absFile));
      configFile.load(fin);
      fin.close();
    }
    catch (FileNotFoundException FNFE) {
      log.error("Properties file not found.", FNFE);
      System.exit(1);
    }
    catch (IOException Ex) {
      log.error("Reading properties file.", Ex);
      System.exit(1);
    }

    int port = Integer.parseInt(configFile.getProperty("PORT", "10000"));
    String runDir = configFile.getProperty("RUNDIR", "/tmp/");
    String asDaemon = configFile.getProperty("DAEMON", "false");

    if (asDaemon.equals("false")) {
      System.out.println("Metrix - A server / client interface for Illumina Sequencing Metrics.");
      System.out.println("Copyright (C) 2014 Bernd van der Veen\n");
      System.out.println("This program comes with ABSOLUTELY NO WARRANTY;");
      System.out.println("This is free software, and you are welcome to redistribute it");
      System.out.println("under certain conditions; for more information please see LICENSE.txt\n");
    }

    log.info("Metrix Server initializing...");

    try {
      // Initialize datastore for sequence run summary data.
      DataStore ds = new DataStore();
      if (ds.conn == null) {
        log.error("Cannot establish MySQL connection.");
        System.exit(1);
      }

      log.info("Initializing Directory Watcher Service with directory: " + runDir);
      // Start Watcher service
      final MetrixWatch mw = new MetrixWatch(runDir, false, ds);
      mw.start();

      log.info("Directory Watcher Service started - Monitoring.");

      // Configure Server
      ServerSocketChannel ssChannel = ServerSocketChannel.open();
      ssChannel.configureBlocking(true);
      ssChannel.socket().bind(new InetSocketAddress(port)); // Call server /
                                                            // Bind socket and
                                                            // port.

      log.info("Metrix communication thread initialized.");
      log.info("Metrix backlog service initializing...");
      final Runnable backlog = new Runnable() {
        @Override
        public void run() {
          if (mw.watcher != null) {
            mw.checkForceTime();
          }
        }
      };

      final ScheduledFuture<?> backlogHandle = scheduler.scheduleAtFixedRate(backlog, 10, 20, TimeUnit.MINUTES);
      scheduler.schedule(new Runnable() {
        @Override
        public void run() {
          backlogHandle.cancel(true);
        }
      }, 365, TimeUnit.DAYS);

      // While server is alive, accept new connections.
      while (listening) {
        new MetrixThread(ssChannel.accept()).start();
      }
    }
    catch (IOException Ex) {
      log.error("IOException initializing server.", Ex);
      System.exit(1);
    }
  }
}
