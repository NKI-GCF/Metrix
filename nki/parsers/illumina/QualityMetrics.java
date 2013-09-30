// Metrix - A server / client interface for Illumina Sequencing Metrics.
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
import nki.objects.Reads;
import nki.util.LoggerWrapper;

public class QualityMetrics extends GenericIlluminaParser{
	QualityScores qScores;

	// Instantiate Logger	
	LoggerWrapper metrixLogger = LoggerWrapper.getInstance();

	public QualityMetrics(String source, int state){
		super(QualityMetrics.class, source, state);
	}

	/*
	 * Binary structure:
	 *	byte 0: file version number (4)
	 *	byte 1: length of each record
	 *	bytes (N * 206 + 2) - (N *206 + 207): record:
	 *	2 bytes: lane number (uint16)
	 *	2 bytes: tile number (uint16)
	 *	2 bytes: cycle number (uint16)
	 *	4 x 50 bytes: number of clusters assigned score (uint32) Q1 through Q50
	 */

	public QualityScores digestData(Reads rds){
       qScores = new QualityScores();

		try{
			setVersion(leis.readByte());
			setRecordLength(leis.readByte());
		}catch(IOException Ex){
			metrixLogger.log.severe("Error in parsing version number and recordLength: " + Ex.toString());
		}

		try{
			HashMap<Integer, QualityMap> cycleMap = new HashMap<Integer, QualityMap>();

			qScores.setSource(this.getSource());
			qScores.setVersion(this.getVersion());
			qScores.setRecordLength(this.getRecordLength());

			boolean qcFlag = false;
			QualityMap qMap = new QualityMap();
			while(true){
				int laneNr = leis.readUnsignedShort();
				int tileNr = leis.readUnsignedShort();
				int cycleNr = leis.readUnsignedShort();

				if(qScores.getLane(laneNr) != null){
					cycleMap = qScores.getLane(laneNr);
				}else{
					cycleMap = new HashMap<Integer, QualityMap>();
				}

				if(cycleMap.containsKey(cycleNr)){
					qMap = cycleMap.get(cycleNr);
				}else{
					qMap = new QualityMap();
				}

				qcFlag=true;
				int qcRecord = 1;

				while(qcFlag){
	
					if(qcRecord == 50){
						qcFlag = false;
					}
					//if(!rds.cycleIsIndex(cycleNr)){
						qMap.addMapping(tileNr, qcRecord, leis.readInt());
					//}else{
					//	leis.readInt();
					//}
					qcRecord++;
				}
				cycleMap.put(cycleNr, qMap);				
				qScores.setLane(cycleMap, laneNr);
			}
		}catch(EOFException EOFEx){
			// Reached end of file
		}catch(IOException Ex){
			metrixLogger.log.severe("IO Error in parsing Quality Metrics");
		}

		// Return the qualityScores object.
		return qScores;
	}
	
	@SuppressWarnings("unchecked")
	public void iterateQS(){
		
		if(qScores != null) {

		Iterator qit = qScores.getQScoreIterator();

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
