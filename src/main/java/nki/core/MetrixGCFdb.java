package nki.core;
// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2014 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

import net.sf.json.JSONObject;
import nki.decorators.MetrixContainerDecorator;

import java.io.*;
import java.util.regex.*;
import java.util.ArrayList;
import java.util.Properties;

public class MetrixGCFdb {
  public static void main(String[] args) {
    Properties configFile;

    configFile = new Properties();
    // Use external properties file, outside of jar location.
    String externalFileName = System.getProperty("properties");

    if (externalFileName == null) {
      System.err.println("[FATAL] Properties file not argumented as parameter. (use: java -Dproperties=metrix.properties Metrix)");
      System.exit(1);
    }

    String absFile = (new File(externalFileName)).getAbsolutePath();

    try (InputStream fin = new FileInputStream(new File(absFile))) {
      configFile.load(fin);
    }
    catch (FileNotFoundException FNFE) {
      System.err.println("[ERROR] Properties file not found.");
      System.exit(1);
    }
    catch (IOException Ex) {
      System.err.println("[ERROR] Reading properties file. " + Ex.toString());
      System.exit(1);
    }

    // BEFORE COMPILING; DEFINE RUN DIRECTORY BELOW
    String runDir = configFile.getProperty("RUNDIR", "/tmp/") + "/";

    String searchTerm = "";
    ArrayList<String> searchResults = new ArrayList<>();
    String procResult = runDir;
    int arrIdx = 0;

    if (args.length == 0) {
        System.err.println("Invalid number of arguments.");
        System.exit(1);
    }
    else if (args.length == 1) {
      searchTerm = args[0];

      if (searchTerm.length() <= 2) {
        System.err.println("Need a bigger search string.");
        System.exit(1);
      }
    }
    else if (args.length > 1) {
      System.err.println("Invalid number of arguments.");
      System.exit(1);
    }

    File dir = new File(runDir);
    
    if(!dir.exists()){
      System.err.println("[Error] Search directory does not exist.");
      System.exit(1);
    }
    
    File[] files = dir.listFiles();
    for (File file : files) {
      if (file.isFile()) {
        continue;
      }
      if (file.isDirectory()) {
        if (Pattern.compile(Pattern.quote(searchTerm), Pattern.CASE_INSENSITIVE).matcher(file.getName()).find()) {
          searchResults.add(file.getName());
        }
      }
    }

    if (searchResults.size() > 0) {
      if (searchResults.size() == 1) {
        // Process single result
        arrIdx = 0;
      }
      else {
        // More than one result found. 
        System.err.println("Multiple results have been found. ");
        System.exit(1);
      }
    }
    else {
      System.err.println("No results for " + searchTerm);
      System.exit(1);
    }

    procResult += searchResults.get(arrIdx);
    processResult(procResult);
  }

  public static boolean isInteger(String s) {
    try {
      Integer.parseInt(s);
    }
    catch (NumberFormatException e) {
      return false;
    }
    return true;
  }

  public static void processResult(String runName) {
    MetrixContainer mc = new MetrixContainer(runName);

    JSONObject allOut = new MetrixContainerDecorator(mc).toJSON();
    System.out.print(allOut.toString());
  }
}
