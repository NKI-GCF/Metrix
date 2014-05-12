// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

import java.util.regex.*;
import java.io.*;
import java.nio.file.*;

import static java.nio.file.StandardWatchEventKinds.*;

import java.util.*;
import java.util.logging.Level;

import nki.constants.Constants;
import nki.io.DataStore;
import nki.objects.Summary;
import nki.util.LoggerWrapper;

public class MetrixWatch extends Thread {

  // Variables
  protected WatchService watcher;
  private Map<WatchKey, Path> keys;
  private Map<WatchKey, Long> waitMap;
  private boolean recursive;
  private boolean trace = false;
  private Path runDirPath;
  private String runDirString;
  private final String illuDirRegex = "\\d*_.*_\\d*_\\d*.*";
  private final Pattern p = Pattern.compile(illuDirRegex);
  private long waitTime = 600000;  // Update every 10 minutes.		       (ms)
  private long forceTime = 1200000;  // If no update for 20 minutes, force parsing. (ms)
  private static final LoggerWrapper metrixLogger = LoggerWrapper.getInstance();

  private HashMap<String, Summary> results = new HashMap<String, Summary>();
  private MetrixLogic ml = new MetrixLogic();
  private DataStore dataStore;

  /**
   * Creates a WatchService and registers the given run directory
   *
   * @param dirN
   * @param rec
   * @param ds
   * @throws java.io.IOException
   */
  public MetrixWatch(String dirN, boolean rec, DataStore ds) throws IOException {
    this.runDirString = dirN;
    this.runDirPath = Paths.get(dirN);
    this.recursive = rec;
    this.watcher = FileSystems.getDefault().newWatchService();
    this.keys = new HashMap<WatchKey, Path>();
    this.waitMap = new HashMap<WatchKey, Long>();
    this.dataStore = ds;
  }

  @SuppressWarnings("unchecked")
  static <T> WatchEvent<T> cast(WatchEvent<?> event) {
    return (WatchEvent<T>) event;
  }

  /**
   * Executes the runnable Watcher service
   */
  public void run() {
    metrixLogger.log.info("MetrixWatch Service started...");
    File folder = new File(runDirString);
    if (folder.isDirectory()) {
      try {
        metrixLogger.log.log(Level.INFO, "Registering Illumina Run Directory ({0})", runDirString);
        register(Paths.get(runDirString), false);
      }
      catch (IOException Ex) {
        metrixLogger.log.log(Level.SEVERE, "IOException traversing watch directory. {0}", Ex.toString());
      }
    }

    File[] listOfFiles = folder.listFiles();
    String file;
    this.trace = true;

    for (int i = 0; i < listOfFiles.length; i++) {
      if (!checkRegisterIllumina(listOfFiles[i], false)) {
        continue;
      }
    }
    processEvents();
  }

  /**
   * Pass directory or filename to check for validity of Illumina run
   * directory format. If valid directory has been found, parse and register
   * for watchservice.
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
      metrixLogger.log.log(Level.INFO, "Argumented filepath cannot be resolved. {0}", Ex.toString());
      return false;
    }
    File fileRI = new File(file + "/RunInfo.xml");

    if (fileRI.isFile()) { // Valid Illumina Run Directory
      // Check for runs that are still running
      File fileComplete = new File(file + "/RTAComplete.txt");

      if (fileComplete.isFile()) {      // Run has finished
        metrixLogger.log.log(Level.INFO, "[CHECK] Illumina Run finished. Parsing available data for: {0}", file);
        ml.processMetrics(Paths.get(file), Constants.STATE_FINISHED, dataStore); // Parse available info with complete state
        return false;               // Run has completed.
      }

      File lastModCheck = new File(file + "/InterOp/");
      File[] files = lastModCheck.listFiles();

      if (!lastModCheck.isDirectory()) {
        // Print error.
        return false;  // Run dir is malformed - InterOp dir does not exist.
      }
      Arrays.sort(files, new Comparator<File>() {
        public int compare(File f1, File f2) {
          return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
        }
      });

      long difference = (System.currentTimeMillis() - files[files.length - 1].lastModified());

      if (difference > Constants.ACTIVE_TIMEOUT) { // If no updates for 24 hours. (86400000 milliseconds)
        LoggerWrapper.log.log(Level.INFO, "[CHECK] Illumina Run stopped. Parsing available data for: {0}", file);
        if (!ml.checkPaired(file, dataStore)) {  // Check if run is paired and at turn cycle.
          // Call MetrixLogic for parsing
          ml.processMetrics(Paths.get(file), Constants.STATE_HANG, dataStore);
        }
        else {
          try {
            register(Paths.get(file), false);
            register(Paths.get(file + "/InterOp/"), false);
          }
          catch (IOException Ex) {
            metrixLogger.log.log(Level.SEVERE, "IOException traversing watch directory. {0}", Ex.toString());
          }
        }
      }
      else {
        try {
          metrixLogger.log.log(Level.INFO, "[CHECK] Illumina Run detected: {0}", file);
          // Register rundir
          register(Paths.get(file + "/InterOp/"), false);
          register(Paths.get(file), newRun);
        }
        catch (IOException Ex) {
          metrixLogger.log.log(Level.SEVERE, "IOException traversing watch directory. {0}", Ex.toString());
        }
      }
    }
    else {
      metrixLogger.log.log(Level.INFO, "Directory: {0} does not comply with the Illumina run directory format. RunInfo.xml is missing.", file);
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
        metrixLogger.log.log(Level.INFO, "Registered new watch directory: {0}", dir);
        if (newRun) {
          ml.processMetrics(dir, Constants.STATE_INIT, dataStore);
        }
      }
      else {
        if (!dir.equals(prev)) {
          metrixLogger.log.log(Level.INFO, "Previously registered directory modified: {0}", dir);
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

        if (name == null) {
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
              //ml.finishRun(child.getParent()+"");
              // Remove keys from watch hash.
              keys.remove(watchKey);
              waitMap.remove(watchKey);
            }
          }
        }

        // If a new run gets started, register for monitoring. Wait for 5 seconds whilst files are being created.
        if (kind == ENTRY_CREATE) {
          File send = new File(child.toString());
          try {
            Thread.sleep(30000);
          }
          catch (InterruptedException IEX) {
            metrixLogger.log.severe("Sleeping of thread while creating a new run failed!");
          }
          if (checkRegisterIllumina(send, true)) {
            metrixLogger.log.log(Level.INFO, "New run with path: {0} registered", send);
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
              metrixLogger.log.log(Level.INFO, "Parsed {0} successfully. ", procFold);
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

      watchKey.reset();  // Reset the watchkey to put it back for monitoring.

    }  // End while loop line 198

    try {
      watcher.close();
    }
    catch (IOException ex) {
      metrixLogger.log.log(Level.SEVERE, "Error closing the watcher. {0}", ex.toString());
    }
  }

  private boolean checkPollTime(WatchKey localKey) {
    long currentTime = System.currentTimeMillis();

    if (waitMap.get(localKey) != null) {
      if (waitMap.get(localKey) == 0) {
        return true;  // Parse in first pass.
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
      waitMap.put(localKey, null);    // Reset time
      return true;      // Initiate parsing
    }
  }

  public void checkForceTime() {
    long currentTime = System.currentTimeMillis();

    Iterator it = waitMap.entrySet().iterator();
    while (it.hasNext()) {

      Map.Entry watchPairs = (Map.Entry) it.next();
      long mapTime = (Long) watchPairs.getValue();

      Path watchDir = keys.get(watchPairs.getKey());

      // Because the initial run directory watched is present in the waitMap as well,
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
        sum = (Summary) _ds.getSummaryByRunName(nonInterOp);
        metrixLogger.log.log(Level.INFO, "Backlog parsing {0}", sum.getRunId());
        _ds.closeAll();
      }
      catch (Exception Ex) {
        metrixLogger.log.log(Level.SEVERE, "Error in retrieving summary for forced check. {0}", Ex.toString());
      }

      if (sum.getState() == Constants.STATE_FINISHED || sum.getState() == Constants.STATE_HANG) {
        waitMap.remove(watchPairs.getKey());      // if watchkey is present, remove it from waitMap
        keys.remove(watchPairs.getKey());      // Remove watchkeys from Watcher Service
      }

      if ((currentTime - mapTime) > forceTime) {
        if (ml.processMetrics(Paths.get(nonInterOp), sum.getState(), dataStore)) {
          waitMap.put((WatchKey) watchPairs.getKey(), System.currentTimeMillis());
          metrixLogger.log.log(Level.INFO, "Forcefully parsed {0}", nonInterOp);
        }
      }
      else {
        metrixLogger.log.log(Level.INFO, "No update needed yet for {0}", nonInterOp);
      }
    }
  }
}
