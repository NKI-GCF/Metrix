// Illumina Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.objects;

import java.net.*;
import java.io.*;
import java.lang.*;
import java.util.Date;

public class Command implements Serializable{
	
	public static final long serialVersionUID = 42L;

	private String commandString = "";
	private Date dateTime;
	private String command;				// 'GET' / 'FETCH'
	private String commandDetail = "";
	private String mode = "CALL"; 			// 'TIMED' or 'CALL' (default)
							// 'TIMED' executes command repetitively based on variable timedInterval in ms
							// 'CALL' will execute command once and close the connection
	private long timedInterval 	= 10000;	// 10 seconds (use setTimedInterval() to change)
	private String format		= "XML";	// 'XML' or 'OBJ'
							// XML is a SummaryCollection XML format.
							// OBJ is a SummaryCollection POJO.
	private int state		= 12;		// 1 = Running
							// 2 = Finished
							// 3 = Halted
							// 4 = Turn
							// 5 = Init-phase
							// 12 = All
	private String info;
	private String message;

	public Command(){
		this.setDateTime();	// Set date time for instantiation of command object.
	}

	public void setCommandString(String command){
		this.commandString = command;
	}

	public String getCommandString(){
		return commandString;
	}
	
	public void setDateTime(){
		this.dateTime = new Date();
	}

	public void setCommand(String command){
		this.command = command;
	}

	public String getCommand(){
		return command;
	}

	public void setCommandDetail(String cd){
		this.commandDetail = cd;
	}

	public String getCommandDetail(){
		return commandDetail;
	}

	public Date getDateTime(){
		return dateTime;
	}

	public void setMessage(String msg){
		this.message = msg;
	}
	
	public String getMessage(){
		return message;
	}
	
	public void setInfo(String infomsg){
		this.info = infomsg;
	}
	
	public String getMode(){
		return mode;
	}

	public void setMode(String modeSet){
		if(modeSet == "TIMED"){
			this.mode = "TIMED";
		}else{
			this.mode = "CALL";
		}
	}

	public long getTimedInterval(){
		return timedInterval;
	}

	public void setTimedInterval(long ti){
		if(ti < 10000){
			this.timedInterval = 10000;
		}else{
			this.timedInterval = ti;
		}
	}

	public int getState(){
		return state;
	}

	public void setState(int st){
		if(st != (0 | 2 | 3 | 4 | 5 | 12)){
			setMessage("Invalid state (" + st + ")");
		}else{
			this.state = st;
		}
	}

	public String getFormat(){
		return format;
	}

	public void setFormat(String form){
		if(form.equals("OBJ")){
			this.format = form;
		}else{
			this.format = "XML";
		}
	}
}

