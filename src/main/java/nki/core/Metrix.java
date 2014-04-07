package nki.core;

import java.io.*;
import java.util.regex.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.text.*;
import org.xml.sax.SAXException;
import javax.xml.parsers.ParserConfigurationException;

import nki.constants.Constants;
import nki.objects.QualityScores;
import nki.objects.QScoreDist;
import nki.objects.IntensityScores;
import nki.objects.IntensityDist;
import nki.objects.Indices;
import nki.objects.ClusterDensity;
import nki.objects.PhasingCollection;
import nki.objects.ErrorCollection;
import nki.objects.ErrorDist;
import nki.objects.Summary;
import nki.objects.Reads;
import nki.parsers.illumina.QualityMetrics;
import nki.parsers.illumina.TileMetrics;
import nki.parsers.illumina.CorrectedIntensityMetrics;
import nki.parsers.illumina.IndexMetrics;
import nki.parsers.illumina.ErrorMetrics;
import nki.parsers.illumina.ExtractionMetrics;
import nki.parsers.xml.XmlDriver;

public class Metrix {

	public static void main(String[] args) {
	    Properties configFile;

		configFile = new Properties();
		// Use external properties file, outside of jar location.
    	String externalFileName = System.getProperty("properties");
		
		if(externalFileName == null){
			System.out.println("[FATAL] Properties file not argumented as parameter. (use: java -Dproperties=metrix.properties Metrix)");
			System.exit(1);
		}

	    String absFile = (new File(externalFileName)).getAbsolutePath();

        try (InputStream fin = new FileInputStream(new File(absFile))) {
                configFile.load(fin);
        }catch(FileNotFoundException FNFE){
			System.out.println("[ERROR] Properties file not found.");
			System.exit(1);	
		}catch(IOException Ex){
			System.out.println("[ERROR] Reading properties file. " + Ex.toString());
			System.exit(1);
 		}

		// BEFORE COMPILING; DEFINE RUN DIRECTORY BELOW
		String runDir = configFile.getProperty("RUNDIR", "/tmp/") + "/";

		String searchTerm 					= "";
		ArrayList<String> searchResults		= new ArrayList<>();
		String procResult 					= runDir;
		int arrIdx = 0;
	// If run string argumented : Search in rundir
		//	1  result 	: Parse and print
		//	>2 results 	: Print and prompt user for selection
	
	System.out.println("Metrix Illumina Sequencing Run Summary.\n");

	 if(args.length == 0){
		System.out.print("Enter atleast 3 characters of your run name of interest: ");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		try {
			boolean read = true;
			while (read){
				searchTerm = br.readLine();
				if(searchTerm.length() <= 2){
					System.out.print("Please use 3 or more characters: ");
				}else{
					read = false;
				}
			}
		} catch (IOException ioe) {
			System.out.println("IO error trying to read the search term.");
			System.exit(1);
		}
	 }else if(args.length == 1){
		searchTerm = args[0];
		
		if(searchTerm.length() <= 2){
			System.out.println("Please supply atleast 3 characters to search for.");
			System.exit(1);
		}

	 }else if(args.length > 1){
		System.out.println("[Error] Only one search term required.");
		System.exit(1);
	 }

		System.out.println("Searching for: " + searchTerm);
        File dir = new File(runDir);
	    File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isFile()) {
                continue;
            }
			if(file.isDirectory()){
				if(Pattern.compile(Pattern.quote(searchTerm), Pattern.CASE_INSENSITIVE).matcher(file.getName()).find()){
					searchResults.add(file.getName());
        	    }
			}
        }
	
		String choice = "";

		if(searchResults.size() > 0){
			if(searchResults.size() == 1){
				// Process single result
				arrIdx = 0;
				choice = "1";
			}else{
				int idx = 0;
				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				System.out.println("Multiple results have been found: ");
				for(String runRes : searchResults){
						System.out.println((idx+1)+") " + runRes);
						idx++;
				}
				try {
					boolean read = true;
					boolean num = false;
					System.out.print("\nPlease select a sequencing run: ");

					while (read){
						choice = br.readLine();
						if(!isInteger(choice)){
							System.out.print("Please enter a number between 1 and " + idx + ": ");
							num = false;
						}else{
					   		try {
								arrIdx = Integer.parseInt(choice)-1;
								num = true;
							} catch(NumberFormatException e) {
								read = true;
							}
						}

						if(arrIdx >= 0 && arrIdx <= idx-1 && num == true){
							read = false;
						}else if(num == true){
							System.out.print("Please enter a number between 1 and " + idx + ": ");
						}

					}
				} catch (IOException ioe) {
					System.out.println("IO error.");
					System.exit(1);
				}
			}
		}else{
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
		} catch(NumberFormatException e) { 
			return false; 
		}
   		return true;
	}	

	public static void processResult(String runName){
		DecimalFormat df = new DecimalFormat("##.##");
		Summary sum = new Summary();

		// Required paths
		String extractionMetrics			= runName + "/InterOp/" + Constants.EXTRACTION_METRICS;
		String tileMetrics 					= runName + "/InterOp/" + Constants.TILE_METRICS;
		String qualityMetrics 				= runName + "/InterOp/" + Constants.QMETRICS_METRICS;
		String intensityMetrics				= runName + "/InterOp/" + Constants.CORRECTED_INT_METRICS;
		String indexMetrics 				= runName + "/InterOp/" + Constants.INDEX_METRICS;
		String errorMetrics					= runName + "/InterOp/" + Constants.ERROR_METRICS;

		// Process result
		TileMetrics tm = new TileMetrics(tileMetrics, 0);
		QualityMetrics qm = new QualityMetrics(qualityMetrics, 0);
		CorrectedIntensityMetrics cim = new CorrectedIntensityMetrics(intensityMetrics, 0);
		IndexMetrics im = new IndexMetrics(indexMetrics, 0);
		ErrorMetrics em	= new ErrorMetrics(errorMetrics, 0);
		ExtractionMetrics exm = new ExtractionMetrics(extractionMetrics, 0);

		// Collections
		ClusterDensity					clusterDensity;
		ClusterDensity					clusterDensityPF;
		PhasingCollection 				phasingMap;
		PhasingCollection 				prephasingMap;
		QScoreDist 						qScoreDist;
		HashMap<Integer, QScoreDist> 	qScoreLaneDist;
		ErrorDist						eDist;
	
		IntensityDist 					iDistAvg;
		IntensityDist 					iDistCCAvg;		
		QualityScores 					qsOut;
		IntensityScores 				isOut;
		ErrorCollection					ecOut;
		Indices 						indices;


		System.out.println("Processing Run Details");
		try{
			if(!sum.getXmlInfo()){
				XmlDriver xmd = new XmlDriver(runName, sum);
				if(xmd.parseRunInfo()){
					sum = xmd.getSummary();
				}else{
					sum.setXmlInfo(false);
				}
			}
		}catch(SAXException SAX){
			System.out.println("Error Parsing XML Info");
		}catch(IOException Ex){
			System.out.println("IOException in parsing XML Info");
		}catch(ParserConfigurationException PXE){
			System.out.println("Parser Configuration Exception");
		}

		Reads rds = sum.getReads();

		// Metrics digestion
		System.out.println("Processing Tile Metrics");
		if(!tm.getFileMissing()){
			tm.digestData(rds);
			clusterDensity 		= tm.getCDmap();
			clusterDensityPF 	= tm.getCDpfMap();
			phasingMap			= tm.getPhasingMap();
			prephasingMap		= tm.getPrephasingMap();
		}else{
			System.out.println("Unable to process Tile Metrics");
			clusterDensity 		= null;
			clusterDensityPF 	= null;
			phasingMap			= null;
			prephasingMap		= null;
		}

		System.out.println("Processing Quality Metrics");
		if(!qm.getFileMissing()){
			qsOut				= qm.digestData(rds);
			qScoreDist 			= qsOut.getQScoreDistribution();
			qScoreLaneDist		= qsOut.getQScoreDistributionByLane();
		}else{
			System.out.println("Unable to process Quality Metrics");
			qScoreDist 			= null;
			qScoreLaneDist		= null;
		}

		System.out.println("Processing Corrected Intensity Metrics");
		if(!cim.getFileMissing()){
			isOut 				= cim.digestData();
			iDistAvg			= isOut.getAvgCorIntDist();
			iDistCCAvg			= isOut.getAvgCorIntCCDist();
		}else{
			System.out.println("Unable to process Corrected Intensity Metrics");
		}

		System.out.println("Processing Error Metrics");
		if(!em.getFileMissing()){
			ecOut				= em.digestData();
			eDist				= ecOut.getErrorDistribution();
		}else{
			System.out.println("Unable to process Error Metrics");
			eDist 				= null;
		}
		
		System.out.println("Processing Extraction Metrics");
		if(!exm.getFileMissing()){
			sum.setCurrentCycle(exm.getLastCycle());		
		}else{
			System.out.println("Unable to process Extraction Metrics");
		}
	
		System.out.println("Processing Index Metrics");
		if(!im.getFileMissing()){
			indices 			= im.digestData();
		}else{
			System.out.println("Unable to process Tile Metrics");
			indices 			= null;
		}

		System.out.println("\n== Combined Read Q-Scores ==");
		if(qScoreDist != null){
			System.out.println("- %>=Q20 = " + df.format(qScoreDist.aboveQ(20)) + "%");
			System.out.println("- %>=Q30 = " + df.format(qScoreDist.aboveQ(30)) + "%\n");
		}else{
			System.out.println("- %>=Q20 = N/A");
			System.out.println("- %>=Q30 = N/A");
		}

		System.out.println("== Q-Score distribution per lane %>Q20/%>Q30 ==");
		if(qScoreDist != null){
			for(Map.Entry<Integer, QScoreDist> laneDist : qScoreLaneDist.entrySet()){
				int lane = laneDist.getKey();
				QScoreDist dist = laneDist.getValue();

				System.out.println(lane + "\t" + df.format(dist.aboveQ(20)) + "%/" + df.format(dist.aboveQ(30)) + "%");
			}
		}else{
			System.out.println("Currently not available.");
		}

		System.out.println("\n== Phasing / Prephasing Scores ==");
		if(phasingMap != null){
			System.out.println("Lane\tRead\tScore");
			System.out.println(phasingMap.toTab(prephasingMap, rds)); 
		}else{
			System.out.println("Currently not available.");
		}

		System.out.println("== Cluster Density / Cluster Density Passing Filter (k/mm2) ==");
		if(clusterDensity != null){
			System.out.println(clusterDensity.toTab(clusterDensityPF));
		}else{
			System.out.println("Currently not available.");
		}

		System.out.println("== Index Metrics ==");
		if(indices != null){
			System.out.println(indices.toTab());
		}else{
			System.out.println("No Information available.\n");
		}

		System.out.println("== Error Metrics ==");
		if(eDist != null){
			System.out.println(eDist.toTab("rate"));
		}else{
			System.out.println("Currently not available.");
		}

		System.out.println("== Run Progress of "+ sum.getRunId() + "("+ sum.getRunType() + " - " + sum.getTotalCycles() + "bp)==");
		if(sum.getCurrentCycle() == sum.getTotalCycles()){
			System.out.println("Run has finished: " + sum.getCurrentCycle() + "/" + sum.getTotalCycles() + ".");
		}else if(sum.getRunType().equals("Paired End")){
			if(sum.getCurrentCycle() == rds.getPairedTurnCycle()){
				System.out.println("Run needs turning. Currently at: " + sum.getCurrentCycle() + "/" + sum.getTotalCycles() + ".");
			}	
		}else{
			System.out.println("Run has not finished yet. Currently at: " + sum.getCurrentCycle() + "/" + sum.getTotalCycles() + ".");
		}
	}
}
