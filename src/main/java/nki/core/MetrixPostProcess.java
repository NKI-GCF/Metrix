package nki.core;

// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2014 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

import org.json.simple.JSONObject;
import nki.decorators.MetrixContainerDecorator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.regex.*;
import java.util.ArrayList;
import java.util.Properties;
import nki.parsers.metrix.PostProcessing;

public class MetrixPostProcess {
  protected static final Logger log = LoggerFactory.getLogger(MetrixPostProcess.class);

  public static void main(String[] args) {
    Properties configFile;

    configFile = new Properties();
    // Use external properties file, outside of jar location.
    String externalFileName = System.getProperty("properties");

    if (externalFileName == null) {
      System.out.println("[FATAL] Properties file not argumented as parameter. (use: java -Dproperties=metrix.properties Metrix)");
      System.exit(1);
    }

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

    // BEFORE COMPILING; DEFINE RUN DIRECTORY BELOW
    String runDir = configFile.getProperty("RUNDIR", "/tmp/") + "/";

    String searchTerm = "";
    ArrayList<String> searchResults = new ArrayList<>();
    ArrayList<String> searchDirs = new ArrayList<>();
    String procResult = runDir;
    int arrIdx = 0;
    // If run string argumented : Search in rundir
    //	1  result 	: Parse and print
    //	>2 results 	: Print and prompt user for selection

    System.out.println("Metrix Illumina Sequencing Run Demultiplexing.\n");

    if (args.length == 0) {
      System.out.print("Enter atleast 3 characters of your run name of interest: ");
      BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

      try {
        boolean read = true;
        while (read) {
          searchTerm = br.readLine();
          if (searchTerm.length() <= 2) {
            System.out.print("Please use 3 or more characters: ");
          }
          else {
            read = false;
          }
        }
      }
      catch (IOException ioe) {
        System.out.println("IO error trying to read the search term.");
        System.exit(1);
      }
    }
    else if (args.length == 1) {
      searchTerm = args[0];

      if (searchTerm.length() <= 2) {
        System.out.println("Please supply atleast 3 characters to search for.");
        System.exit(1);
      }

    }
    else if (args.length > 1) {
      System.out.println("[Error] Only one search term required.");
      System.exit(1);
    }

    System.out.println("Searching for: " + searchTerm);
    File dir = new File(runDir);
    File[] files = dir.listFiles();
    
    if(files == null){
      System.out.println("[Error] Reading Illumina run directory path.");
      System.exit(1);
    }
    
    for (File file : files) {
      if (file.isFile()) {
        continue;
      }
      if (file.isDirectory()) {
        if (Pattern.compile(Pattern.quote(searchTerm), Pattern.CASE_INSENSITIVE).matcher(file.getName()).find()) {
          searchResults.add(file.getName());
          searchDirs.add(file.getAbsolutePath());
        }
      }
    }

    String choice = "";

    if (searchResults.size() > 0) {
      if (searchResults.size() == 1) {
        // Process single result
        arrIdx = 0;
        choice = "1";
      }
      else {
        int idx = 0;
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Multiple results have been found: ");
        for (String runRes : searchResults) {
          System.out.println((idx + 1) + ") " + runRes);
          idx++;
        }
        try {
          boolean read = true;
          boolean num = false;
          System.out.print("\nPlease select a sequencing run: ");

          while (read) {
            choice = br.readLine();
            if (!isInteger(choice)) {
              System.out.print("Please enter a number between 1 and " + idx + ": ");
              num = false;
            }
            else {
              try {
                arrIdx = Integer.parseInt(choice) - 1;
                num = true;
              }
              catch (NumberFormatException e) {
                read = true;
              }
            }

            if (arrIdx >= 0 && arrIdx <= idx - 1 && num == true) {
              read = false;
            }
            else if (num == true) {
              System.out.print("Please enter a number between 1 and " + idx + ": ");
            }

          }
        }
        catch (IOException ioe) {
          System.out.println("IO error.");
          System.exit(1);
        }
      }
    }
    else {
      System.out.println("\nNo results have been found. Please try again.");
      System.exit(1);
    }

    procResult += searchResults.get(arrIdx);
    System.out.println("Processing: " + procResult);
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
    PostProcessing pp = new PostProcessing(mc.getSummary());
    // Run PP
    pp.run();
  }
}
