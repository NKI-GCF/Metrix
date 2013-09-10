import java.util.regex.*;
import java.io.*;
import java.nio.file.*;
import static java.nio.file.StandardWatchEventKinds.*;
import static java.nio.file.LinkOption.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.logging.*;
import nki.objects.Summary;
import nki.parsers.illumina.*;
import nki.constants.Constants;
import nki.io.DataStore;
import nki.objects.Summary;
import nki.util.LoggerWrapper;

public class MetrixWatch extends Thread{
	
    // Variables
    protected WatchService watcher;
    private Map<WatchKey,Path> keys;
    private Map<WatchKey, Long> waitMap;
	private boolean recursive;
    private boolean trace = false;
	private Path runDirPath;
	private String runDirString;
	private String illuDirRegex = "\\d*_.*_\\d*_\\d*.*";
	private Pattern p = Pattern.compile(illuDirRegex);
	private long waitTime =  1800000;	// Update every 5 minutes.		       (ms)
	private long forceTime = 1200000;	// If no update for 20 minutes, force parsing. (ms)
	private LoggerWrapper metrixLogger = LoggerWrapper.getInstance();

	private HashMap<String, Summary> results = new HashMap<String, Summary>();
	private MetrixLogic ml = new MetrixLogic();
	private DataStore dataStore;

        /**
        * Creates a WatchService and registers the given run directory
        */
        public MetrixWatch(String dirN, boolean rec, DataStore ds) throws IOException {
			this.runDirString = dirN;
		   	this.runDirPath = Paths.get(dirN);
      		this.recursive = rec;
            this.watcher = FileSystems.getDefault().newWatchService();
            this.keys = new HashMap<WatchKey,Path>();
			this.waitMap = new HashMap<WatchKey, Long>();
			this.dataStore = ds;
        }


	@SuppressWarnings("unchecked")
	static <T> WatchEvent<T> cast(WatchEvent<?> event) {
		return (WatchEvent<T>)event;
	}	

	/**
	*	Executes the runnable Watcher service
	**/
	public void run() {
			metrixLogger.log.info( "MetrixWatch Service started...");
                        File folder = new File(runDirString);
			if(folder.isDirectory()){
				try{
					metrixLogger.log.info("Registering Illumina Run Directory.");
					register(Paths.get(runDirString), false);
				 }catch(IOException Ex){
                    metrixLogger.log.severe( "IOException traversing watch directory. "+ Ex.toString());
                 }
			}

			File[] listOfFiles = folder.listFiles();
            String file;
			this.trace = true;

            for(int i = 0; i < listOfFiles.length; i++){
				if(!checkRegisterIllumina(listOfFiles[i], false)){
					continue;
				}
            }
		processEvents();
	}
	
	/**
	*	Pass directory or filename to check for validity of Illumina run directory format.
	*	If valid directory has been found, parse and register for watchservice.
	**/

	
	public boolean checkRegisterIllumina(File fileArg, boolean newRun){
		String fileName = fileArg.getName();
		String file;

		if(fileArg.isFile()){
			return false;
		}

		Matcher m = p.matcher(fileName);
        if(!m.matches()){ // File is not a run directory
             return false;
        }

//                file = fileArg.getAbsolutePath();
        try{
        	file = fileArg.getCanonicalPath();
		}catch(IOException Ex){
			metrixLogger.log.info( "Argumented filepath cannot be resolved. " + Ex.toString());
			return false;
		}
        File fileRI = new File(file + "/RunInfo.xml");

        if(fileRI.isFile()){ // Valid Illumina Run Directory
        	// Check for runs that are still running
            File fileComplete = new File(file + "/RTAComplete.txt");

			if(fileComplete.isFile()){      // Run has finished
            	metrixLogger.log.info( "[CHECK] Illumina Run finished. Parsing available data for: " + file);
            	ml.processMetrics(Paths.get(file), 2, dataStore); // Parse available info with complete state
                return false;               // Run has completed.
            }

			File lastModCheck = new File(file+"/InterOp/");
			File[] files = lastModCheck.listFiles();

			if(!lastModCheck.isDirectory()){
				// Print error.
				return false;	// Run dir is malformed - InterOp dir does not exist.
			}
			Arrays.sort(files, new Comparator<File>(){
			    public int compare(File f1, File f2)
			    {
			        return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
			    } });
			
			long difference = (System.currentTimeMillis() - files[files.length-1].lastModified());

            if(difference > 18000000){ // If no updates for 5 hours. (18000000 milliseconds)
				metrixLogger.log.info( "[CHECK] Illumina Run stopped. Parsing available data for: " + file);
				if(!ml.checkPaired(file, dataStore)){	// Check if run is paired and at turn cycle.
					// Call MetrixLogic for parsing
					ml.processMetrics(Paths.get(file), 3, dataStore); 
  				 }else{
				 	try{
						 register(Paths.get(file), false);
						 register(Paths.get(file+"/InterOp/"), false);
					}catch(IOException Ex){
						metrixLogger.log.severe( "IOException traversing watch directory. " + Ex.toString());
					}
				}
			}else{
				try{
					metrixLogger.log.info( "[CHECK] Illumina Run detected: " + file);
                    	// Register rundir
                    register(Paths.get(file+"/InterOp/"), false);
					register(Paths.get(file),newRun);
				}catch(IOException Ex){
					metrixLogger.log.severe( "IOException traversing watch directory. " + Ex.toString());
				}
			}
		}else{
			metrixLogger.log.info( "Directory: " + file + " does not comply with the Illumina run directory format. RunInfo.xml is missing.");
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
					metrixLogger.log.info( "Registered new watch directory: " +dir);
					if(newRun){
						ml.processMetrics(dir, 5, dataStore);
					}
	            } else {
	                if (!dir.equals(prev)) {
						metrixLogger.log.info( "Previously registered directory modified: " + dir);
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
            } catch (InterruptedException e) {
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

			if(name == null){
				continue;
			}
					Path child = dir.resolve(name);
			
			if(child.equals(prev)){
				continue;
			}
		
			WatchEvent.Kind<?> kind = event.kind();
			
			if(kind == OVERFLOW){
				continue;
			}	

			// Once RTAComplete has been created, set run to finish.	
			if(kind == ENTRY_CREATE){
				if((child+"").matches("^.+?RTAComplete.txt$")){
					ml.finishRun(child.getParent()+"");
					// Remove keys from watch hash.
					keys.remove(watchKey);
					waitMap.remove(watchKey);
				}
			}

			// If a new run gets started, register for monitoring. Wait for 5 seconds whilst files are being created.
			if(kind == ENTRY_CREATE){
				File send = new File(child+"");
				try{
					Thread.sleep(30000);
				}catch(InterruptedException IEX){
					metrixLogger.log.severe("Sleeping of thread while creating a new run failed!");
				}
				if(checkRegisterIllumina(send, true)){
					metrixLogger.log.info( "New run with path: " + send+" registered");
				}else{
					continue;
				}	
			}

			if(!checkPollTime(watchKey)){
				// Skip event -- Still waiting for polling time. Do not parse.
				continue;
			}else{
				if((child+"").matches("^.+?Out\\.bin")){
					Path procFold = (child.getParent()).getParent();
								// Parse summary object
					if(ml.processMetrics(procFold, 1, dataStore)){
						// Successfuly processed, continue watching.
						metrixLogger.log.info( "Parsed " + procFold  + " successfully. ");
					}
						// ELSE Processing failed
					
				}
				waitMap.put(watchKey, System.currentTimeMillis());
			}
        }
	
		boolean valid = watchKey.reset();
		if(!valid){
			keys.remove(watchKey);
		}

        watchKey.reset();	// Reset the watchkey to put it back for monitoring.
 
    }	// End while loop line 198

	try{ 
	        watcher.close();
	}catch(IOException ex){
		metrixLogger.log.severe( "Error closing the watcher. " + ex.toString());
	}
    }

	private boolean checkPollTime(WatchKey localKey){
		long currentTime = System.currentTimeMillis();

		if(waitMap.get(localKey) != null){
			if(waitMap.get(localKey) == 0){
				return true;	// Parse in first pass.
			}
		}else{
			return false;
		}

		long mapTime = waitMap.get(localKey);

		if((currentTime - mapTime) < waitTime){
			return false;
		}else{
			waitMap.put(localKey, null);    // Reset time
			return true;			// Initiate parsing
		}
	}

	public void checkForceTime(){
		long currentTime = System.currentTimeMillis();
		
		Iterator it = waitMap.entrySet().iterator();
		while (it.hasNext()) {

			Map.Entry watchPairs = (Map.Entry)it.next();
			long mapTime = (long) watchPairs.getValue();
			
			Path watchDir = keys.get(watchPairs.getKey());


			// Because the initial run directory watched is present in the waitMap as well,
			// We need to skip this forced scan.
			if(watchDir.toString().equals(runDirString)){
				continue;
			}

			if(watchDir.toString().matches("(.*)/InterOp(.*)")){
				continue;
			}

			Summary sum = new Summary();

			String nonInterOp = watchDir.toString().replace("/InterOp","");

			try{
				metrixLogger.log.info( "Backlog parsing " + nonInterOp);
				sum = (Summary) dataStore.getSummaryByRunName(nonInterOp);
			}catch(Exception Ex){
				metrixLogger.log.severe( "Error in retrieving summary for forced check. " + Ex.toString());
			}

			if(sum.getState() == Constants.STATE_FINISHED || sum.getState() == Constants.STATE_HANG){
				waitMap.remove(watchPairs.getKey());			// if watchkey is present, remove it from waitMap
				keys.remove(watchPairs.getKey());			// Remove watchkeys from watch
			}

			if((currentTime - mapTime) > forceTime){
				if(ml.processMetrics(Paths.get(nonInterOp), sum.getState(), dataStore)){
					waitMap.put((WatchKey) watchPairs.getKey(), System.currentTimeMillis());
					metrixLogger.log.info( "Forcefully parsed " + nonInterOp);
				}
			}
		}
	}
}

