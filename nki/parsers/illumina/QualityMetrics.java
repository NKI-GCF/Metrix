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

public class QualityMetrics {
	private String source = "";
	LittleEndianInputStream leis = null;

	QualityScores qScores;

	private int version = 0;
	private int recordLength = 0;
    private int sleepTime = 3000;
	private boolean fileMissing = false;	

	public QualityMetrics(String source, int state){

                try{
                        setSource(source);
                        if(state == 1){
                                Thread.sleep(sleepTime);
                        }
                        leis = new LittleEndianInputStream(new FileInputStream(source));
                }catch(IOException IO){
                        // Set fileMissing = true. --> Parse again later.
                        setFileMissing(true);
						System.out.println("Quality Metrics file not available for " + source);
                }catch(InterruptedException IEX){

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

	public void setFileMissing(boolean fileMissing){
		this.fileMissing = fileMissing;
	}

	public boolean getFileMissing(){
		return fileMissing;
	}

	public QualityScores digestData(){
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

			boolean qcFlag = false;
			
			while(true){
				int laneNr = leis.readUnsignedShort();
				int tileNr = leis.readUnsignedShort();
				int cycleNr = leis.readUnsignedShort();
				
				qcFlag=true;
				int qcRecord = 1;

				QualityMap qMap = new QualityMap();
				while(qcFlag){
	
					if(qcRecord == 50){
						qcFlag = false;
					}
					
					qMap.addMapping(tileNr, qcRecord, leis.readInt());
					qcRecord++;
				}
				cycleMap.put(cycleNr, qMap);				
				qScores.setLane(cycleMap, laneNr);
			}
		}catch(EOFException EOFEx){
			// Reached end of file
		}catch(IOException Ex){
			System.out.println("IO Error");
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
