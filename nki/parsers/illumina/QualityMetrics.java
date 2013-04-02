// Illumina Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.parsers.illumina;

import nki.io.LittleEndianInputStream;
import java.io.IOException;
import java.io.EOFException;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Collections;
import nki.objects.QualityScores;
import nki.objects.QualityMap;

public class QualityMetrics {
	private String source = "";
	LittleEndianInputStream leis = null;

	QualityScores qscores;

	private int version = 0;
	private int recordLength = 0;

	public QualityMetrics(String source){
		try{
			setSource(source);
			leis = new LittleEndianInputStream(new FileInputStream(source));
		}catch(IOException IO){
			System.out.println("Parser Error - Quality Metrics: " + IO.toString());
		}
	}

	public void setSource(String source){
		this.source = source;
	}

	public String getSource(){
		return source;
	}

	private void setVersion(int version){
		this.version = version;
	}

	public int getVersion(){
		return version;
	}

	private void setRecordLength(int recordLength){
		this.recordLength = recordLength;
	}

	public int getRecordLength(){
		return recordLength;
	}

	public QualityScores outputData(){
                qScores = new QualityScores();

		try{
			setVersion(leis.readByte());
			setRecordLength(leis.readByte());
		}catch(IOException Ex){
			System.out.println("Error in parsing version number and recordLength: " + Ex.toString());
		}

		try{
			HashMap<Integer, QualityMap> cycleMap = new HashMap<Integer, QualityMap>();

			qScores.setSource(this.getSource());
			qScores.setVersion(this.getVersion());
			qScores.setRecordLength(this.getRecordLength());

			//HashMap<Integer,int[]> qcMap = new HashMap<Integer, int[]>();
			boolean qcFlag = false;
			
			while(true){
				int laneNr = leis.readUnsignedShort();
				int tileNr = leis.readUnsignedShort();
				int cycleNr = leis.readUnsignedShort();
				
				qcFlag=true;
				int qcRecord = 1;
				int[] clusterScore = new int[51];
			//	System.out.println(laneNr + "\t" + cycleNr + "\t" + tileNr);

				QualityMap qMap = new QualityMap();
				while(qcFlag){
	
					if(qcRecord == 50){
						qcFlag = false;
					}
					
					qMap.addMapping(tileNr, qcRecord, leis.readInt());

//					clusterScore[qcRecord] = leis.readInt();
//					System.out.println("Record: " + qcRecord + "\tAssigned: " +  clusterScore[qcRecord]);
					qcRecord++;
				}
				cycleMap.put(cycleNr, qMap);				
				qScores.setLane(cycleMap, laneNr);
//				qcMap.put(cycleNr, clusterScore); // Store in hashmap per cycle.
			}
		}catch(EOFException EOFEx){
			System.out.println("Reached end of file");
		}catch(IOException Ex){
			System.out.println("IO Error");
		}
	
		// Return the qualityScores.
		return qScores;
	}

	public void iterateQS(QualityScores qscores){
		
		if(qscores != null) {

		Iterator qit = qscores.getQScoreIterator();

		while(qit.hasNext()){
			Map.Entry scorePairs = (Map.Entry) qit.next();
			int lane = (Integer) scorePairs.getKey();
			HashMap<Integer, QualityMap> laneScores = (HashMap<Integer, QualityMap>) scorePairs.getValue();
			
			for(Map.Entry<Integer, QualityMap> entry : laneScores.entrySet()){
				int cycle = (Integer) entry.getKey();
				QualityMap qmap = (QualityMap) entry.getValue();

				Iterator qmapIt = qmap.getScoreIterator();
				
				while(qmapIt.hasNext()){
					Map.Entry qmapPairs = (Map.Entry) qmapIt.next();
					int tile = (Integer) qmapPairs.getKey();
					HashMap<Integer, Integer> qmetricMap = (HashMap<Integer, Integer>) qmapPairs.getValue();

					for(Map.Entry<Integer, Integer> qmetric : qmetricMap.entrySet()){
						int metric = (Integer) qmetric.getKey();
						int value = (Integer) qmetric.getValue();
						System.out.println("Lane: " + lane + "\tCycle: " + cycle + "\tTile: " + tile + "\tQMetric: " + metric + "\t#Clust\\wScore: " + value);
					}
				}
			}
		}
		}
	}
}
