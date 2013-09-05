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

public class GenericIlluminaParser {
	protected String source = "";
	protected LittleEndianInputStream leis = null;

	protected int version = 0;
	protected int recordLength = 0;
    protected int sleepTime = 3000;
	protected boolean fileMissing = false;	
	
	public GenericIlluminaParser(Class<?> c, String source, int state){
		try{
			setSource(source);
			if(state == 1){
				Thread.sleep(sleepTime);
			}
			leis = new LittleEndianInputStream(new FileInputStream(source));	
		}catch(IOException IO){
			// Set fileMissing = true. --> Parse again later. 
			setFileMissing(true);
			System.out.println(c.getSimpleName() + " file not available for " + source);
		}catch(InterruptedException IEX){

        }
	}

	public void setSource(String source){
		this.source = source;
	}

	public String getSource(){
		return source;
	}

	public void setVersion(int version){
		this.version = version;
	}

	public int getVersion(){
		return version;
	}

	public void setRecordLength(int recordLength){
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
}