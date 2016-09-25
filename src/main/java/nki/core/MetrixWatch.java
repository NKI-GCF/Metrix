package nki.core;

// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2014 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

import java.io.*;
import java.nio.file.*;

import static java.nio.file.StandardWatchEventKinds.*;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nki.constants.Constants;
import nki.io.DataStore;
import nki.objects.Summary;

public class MetrixWatch extends Thread {

  // Variables
  protected WatchService watcher;
  private Map<WatchKey, Path> keys;
  private Map<WatchKey, Long> waitMap;
  private ArrayList<String> finishedMap;
  private boolean recursive;
  private boolean trace = false;
  private Path runDirPath;
  private String runDirString;
  private final String illuDirRegex = "\\d*_.*_\\d*_\\d*.*";
  private final Pattern p = Pattern.compile(illuDirRegex);
  private long waitTime = 600000; // Update every 10 minutes. (ms)
  private long forceTime = 1200000; // If no update for 20 minutes, force
                                    // parsing. (ms)
  protected static final Logger log = LoggerFactory.getLogger(MetrixWatch.class);

  private HashMap<String, Summary> results = new HashMap<String, Summary>();
  private MetrixLogic ml = new MetrixLogic();
  private DataStore dataStore;

  public MetrixWatch(String dirN, boolean rec, DataStore ds) throws IOException {
    this.runDirString = dirN;
    this.runDirPath = Paths.get(dirN);
    this.recursive = rec;
    this.watcher = FileSystems.getDefault().newWatchService();
    this.keys = new HashMap<WatchKey, Path>();
    this.waitMap = new HashMap<WatchKey, Long>();
    this.dataStore = ds;
    this.finishedMap = new ArrayList<String>();
  }

  @SuppressWarnings("unchecked")
  static <T> WatchEvent<T> cast(WatchEvent<?> event) {
    return (WatchEvent<T>) event;
  }

  /**
   * Executes the runnable Watcher service
   */
  @Override
  public void run() {
    log.info("MetrixWatch Service started...");
    File folder = new File(runDirString);
    if (folder.isDirectory()) {
      try {
        log.info("Registering Illumina Run Directory (" + runDirString + ")");
        register(Paths.get(runDirString), false);
      }
      catch (IOException Ex) {
        log.error("IOException traversing watch directory. ", Ex);
      }
    }

    File[] listOfFiles = folder.listFiles();
    String file;
    this.trace = true;

    if (listOfFiles != null) {
      log.debug(listOfFiles.length + " files found in Illumina run directory. Scanning for directories...");
      for (int i = 0; i < listOfFiles.length; i++) {
        if (!checkRegisterIllumina(listOfFiles[i], false)) {
          continue;
        }
      }
      processEvents();
    }
    else {
      log.info("No files available in argumented Illumina run directory. ");
      log.info("Waiting for new directories...");
    }
  }

  /**
   * Pass directory or filename to check for validity of Illumina run directory
   * format. If valid directory has been found, parse and register for
   * watchservice.
   */
  public boolean checkRegisterIllumina(File fileArg, boolean newRun) {
    String fileName = fileArg.getName();
    String file;

    if (fileArg.isFile()) {
      return false;
    }

    Matcher m = p.matcher(fileName);
    if (!m.matches()) { // File is not a run directory
      return false;
    }

    try {
      file = fileArg.getCanonicalPath();
    }
    catch (IOException Ex) {
      log.warn("Argumented filepath cannot be resolved.", Ex);
      return false;
    }
    File fileRI = new File(file + "/RunInfo.xml");

    if (fileRI.isFile()) { // Valid Illumina Run Directory
      // Check for runs that are still running
      File fileComplete = new File(file + "/RTAComplete.txt");

      if (fileComplete.isFile()) { // Run has finished
        log.info("Illumina Run finished: " + file);
        // Only perform inital init if run exists in DB, else create.
        DataStore ds = null;
        try {
          ds = new DataStore();
          if (ds.checkSummaryByRunId(ds.conn, file) && (System.currentTimeMillis() - fileComplete.lastModified()) > 1814400000) {
            // Run is finished, available in database. But has completed over
            // three weeks ago.
            ml.quickLoad = true;
            log.debug("Old run - Quick loading a finished run. Available in database.");
          }
          else if (ds.checkSummaryByRunId(ds.conn, file)) {
            // Run is finished, available in database.
            ml.quickLoad = true;
            log.debug("Quick loading a finished run. Available in database.");
          }
          else {
            // Run has finished but not available in database.
            ml.quickLoad = false;
            log.warn("Run has finished. Not available in database. Parsing...");
          }
          log.debug("Closing connection.");
          ds.conn.close();
          log.debug("Started processing of finished run.");
          ml.processMetrics(Paths.get(file), Constants.STATE_FINISHED, dataStore); // Parse
                                                                                   // available
                                                                                   // info
                                                                                   // with
                                                                                   // complete
                                                                                   // state
        }
        catch (Exception Ex) {
          log.error("Exception while checking finished run in database. " + Ex);
        } finally {
          ds = null;
          log.debug("Clearing datastore connection for run finished check.");
        }

        // Run has completed.
        return false;
      }

      File lastModCheck = new File(file + "/InterOp/");
      File[] files = lastModCheck.listFiles();

      if (!lastModCheck.isDirectory()) {
        // Print error.
        return false; // Run dir is malformed - InterOp dir does not exist.
      }
      Arrays.sort(files, new Comparator<File>() {
        @Override
        public int compare(File f1, File f2) {
          return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
        }
      });

      long difference = (System.currentTimeMillis() - files[files.length - 1].lastModified());

      if (difference > Constants.ACTIVE_TIMEOUT) { // If no updates for 24
                                                   // hours. (86400000
                                                   // milliseconds)
        log.info("Illumina run stopped: " + file);
        if (!ml.checkPaired(file, dataStore)) { // Check if run is paired and at
                                                // turn cycle.
          // Call MetrixLogic for parsing stopped runs
          DataStore ds = null;
          try{
            ds = new DataStore();
            // Run is older than three weeks and is available in database.
            if (difference > 1814400000 && ds.checkSummaryByRunId(ds.conn, file)) {
              ml.quickLoad = true;
              log.debug("Quick loading a stopped run. Age is older than 3 weeks.");
              // Run is less than three weeks old and is available in database.
            }
            else if (difference < 1814400000 && ds.checkSummaryByRunId(ds.conn, file)) {
              ml.quickLoad = false;
              log.debug("Parsing a recent run which has stopped. Age is less than 3 weeks.");
              // Run is older than three weeks but hasn't been found in
              // database.
            }
            else if (!ds.checkSummaryByRunId(ds.conn, file)) {
              ml.quickLoad = false;
              log.debug("Parsing a run which has stopped but not found in database.");
            }
            else {
              log.error("Parsing a run which has stopped. Alternative processing.");
            }
            log.debug("Closing connection.");
            ds.conn.close();
            log.debug("Started processing of stopped run.");
            ml.processMetrics(Paths.get(file), Constants.STATE_HANG, dataStore);
          }
          catch (Exception Ex) {
            log.error("Exception while checking stopped run in database.", Ex);
          } finally {
            ds = null;
            log.debug("Datastore for stopped run check dismantled.");
          }
        }
        else {
          try {
            register(Paths.get(file), false);
            register(Paths.get(file + "/InterOp/"), false);
          }
          catch (IOException Ex) {
            log.error("Traversing watch directory.", Ex);
          }
        }
      }
      else {
        try {
          log.info("[NEW] Illumina run detected: " + file);
          // Register rundir
          register(Paths.get(file + "/InterOp/"), false);
          register(Paths.get(file), newRun);
        }
        catch (IOException Ex) {
          log.error("Traversing watch directory.", Ex);
        }
      }
    }
    else {
      // Add run directory to monitor to check again after 120 minutes.
      // Whenever a rundirectory is created several files have to be created
      // first (RunInfo.xml & RunParameters.xml e.g.)
      // First fail observed after updating the MiSeq software to the 2.5
      // version (September 2014).
      // Copy of files seems to be delayed till onboard cluster generation has
      // finished (120 minutes)
      // FAIL : If it fails after 120 minutes. Remove and ignore.
      // SUCCESS : Run will be registered and backlog parsed for the missing
      // time.
      log.info("Checking whether directory has been created less than 24 hours ago.");
      log.debug("Checking age of run directory.");
      // Check if file has been created the last 24 hours.
      long ageDiff = (System.currentTimeMillis() - fileArg.lastModified());
      if (ageDiff < 86400000) {
        log.debug("Created less than 24 hours ago.");
        // Create single thread executor.
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        log.debug("Delaying rescan for 120 minutes.");
        final File taskFile = fileArg;
        final Runnable task = new Runnable() {
          @Override
          public void run() {
            log.debug("Executing delayed task for " + taskFile.getName());
            checkRegisterIllumina(taskFile, true);
          }
        };
        executor.schedule(task, 120, TimeUnit.MINUTES);
        // Shutdown executor.
        log.debug("Gracefully shutting down executor service for delayed task.");
        executor.shutdown();
      }
      else {
        log.info("Directory " + file + " does not match standard format. RunInfo.xml is missing.");
        log.debug("Directory is older than 24 hours. Not creating a delayed task.");
      }
    }

    return true;
  }

  /**
   * Register the given directory with the WatchService
   */
  private void register(Path dir, boolean newRun) throws IOException {
    WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
    long pollTime = 0;

    if (trace) {
      Path prev = keys.get(key);
      if (prev == null) {
        log.info("Registered new watch directory: " + dir);
        if (newRun) {
          ml.quickLoad = false;
          ml.processMetrics(dir, Constants.STATE_INIT, dataStore);
        }
      }
      else {
        if (!dir.equals(prev)) {
          log.info("Previously registered directory modified: " + dir);
        }
      }
    }
    waitMap.put(key, pollTime);
    keys.put(key, dir);
  }

  /**
   * Process all events for keys queued to the watcher
   */
  public void processEvents() {

    // While currentThread is not interrupted
    WatchKey watchKey;
    _watchLoop:
    while (!Thread.currentThread().isInterrupted()) {

      // Wait for some event to occur in the directory
      try {
        watchKey = watcher.take();
        if (!watchKey.isValid()) {
          continue;
        }
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }

      Path dir = keys.get(watchKey);
      Path prev = Paths.get("");

      // Cycle through the events
      final List<WatchEvent<?>> watchEvents = watchKey.pollEvents();
      for (WatchEvent<?> event : watchEvents) {

        // Context for directory entry event is the file name of entry
        WatchEvent<Path> ev = cast(event);
        Path name = ev.context();

        if (name == null || dir == null) {
          continue;
        }
        Path child = dir.resolve(name);

        if (child.equals(prev)) {
          continue;
        }

        WatchEvent.Kind<?> kind = event.kind();

        if (kind == OVERFLOW) {
          continue;
        }

        // Once RTAComplete has been created, set run to finish.
        if (kind == ENTRY_CREATE) {
          if ((child.toString()).matches("^.+?RTAComplete.txt$")) {
            if (ml.checkFinished(child.getParent().toString())) {
              // Remove keys from watch hash.
              finishedMap.add(keys.get(watchKey).toString());
              watchKey.cancel();
              keys.remove(watchKey);
              waitMap.remove(watchKey);
            }
          }
        }

        // If a new run gets started, register for monitoring. Wait for 30
        // seconds whilst files are being created.
        if (kind == ENTRY_CREATE) {
          File send = new File(child.toString());
          try {
            log.debug("New run detected... Waiting 30 seconds to allow sequencer for file creation.");
            Thread.sleep(60000);
          }
          catch (InterruptedException IEX) {
            log.error("Sleeping of thread while creating a new run failed!");
          }
          if (checkRegisterIllumina(send, true)) {
            log.info("New run with path: " + send + " registered");
          }
          else {
            continue;
          }
        }

        if (!checkPollTime(watchKey)) {
          // Skip event -- Still waiting for polling time. Do not parse.
          continue;
        }
        else {
          if ((child + "").matches("^.+?Out\\.bin")) {
            Path procFold = (child.getParent()).getParent();
            // Parse summary object
            if (ml.processMetrics(procFold, Constants.STATE_RUNNING, dataStore)) {
              // Successfuly processed, continue watching.
              log.info("Parsed " + procFold + " successfully. ");
            }
            // ELSE Processing failed

          }
          waitMap.put(watchKey, System.currentTimeMillis());
        }
      }

      boolean valid = watchKey.reset();
      if (!valid) {
        keys.remove(watchKey);
      }

      watchKey.reset(); // Reset the watchkey to put it back for monitoring.

    }  // End while loop

    try {
      watcher.close();
    }
    catch (IOException ex) {
      log.error("Error closing the watcher.", ex);
    }
  }

  private boolean checkPollTime(WatchKey localKey) {
    long currentTime = System.currentTimeMillis();

    if (waitMap.get(localKey) != null) {
      if (waitMap.get(localKey) == 0) {
        return true; // Parse in first pass.
      }
    }
    else {
      return false;
    }

    long mapTime = waitMap.get(localKey);

    if ((currentTime - mapTime) < waitTime) {
      return false;
    }
    else {
      waitMap.put(localKey, null); // Reset time
      return true; // Initiate parsing
    }
  }

  public void checkForceTime() {
    long currentTime = System.currentTimeMillis();

    for (Iterator<WatchKey> it = waitMap.keySet().iterator(); it.hasNext();) {
      WatchKey watchDirKey = it.next();
      long mapTime = waitMap.get(watchDirKey);

      // Get the path for watchDir.
      Path watchDir = keys.get(watchDirKey);

      // Run has been marked finished.
      if (finishedMap.contains(watchDir.toString())) {
        log.debug("Key is in finished map; Removing entries in " + watchDir.toString());
        if (watchDirKey.isValid()) {
          watchDirKey.cancel();
        }
        if (keys.containsKey(watchDirKey)) {
          keys.remove(watchDirKey);
        }

        if (waitMap.containsKey(watchDirKey)) {
          waitMap.remove(watchDirKey);
        }
        it.remove();
        continue;
      }

      // Because the initial run directory watched is present in the waitMap as
      // well,
      // We need to skip this forced scan.
      if (watchDir.toString().equals(runDirString)) {
        continue;
      }

      if (watchDir.toString().matches("(.*)/InterOp(.*)")) {
        continue;
      }

      Summary sum = new Summary();
      String nonInterOp = watchDir.toString().replace("/InterOp", "");

      try {
        DataStore _ds = new DataStore();
        sum = _ds.getSummaryByRunName(nonInterOp);
        if (sum.getRunId() != null) {
          log.info("Backlog parsing " + sum.getRunId());
        }
        _ds.closeAll();
      }
      catch (Exception Ex) {
        log.error("Error in retrieving summary for forced check. ", Ex);
      }
      if (sum.getRunId() != null) {
        if (sum.getState() == Constants.STATE_FINISHED || sum.getState() == Constants.STATE_HANG) {
          waitMap.remove(watchDirKey); // if watchkey is present, remove it from
                                       // waitMap
          keys.remove(watchDirKey); // Remove watchkeys from Watcher Service
        }

        if ((currentTime - mapTime) > forceTime) {
          if (ml.processMetrics(Paths.get(nonInterOp), sum.getState(), dataStore)) {
            waitMap.put(watchDirKey, System.currentTimeMillis());
            log.info("Forcefully parsed " + nonInterOp);
          }
        }
        else {
          log.info("No update needed yet for " + nonInterOp);
        }
      }
    }
  }
}
