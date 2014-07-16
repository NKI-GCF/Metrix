// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2014 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

// MODES:
// 'TIMED' executes command repetitively based on variable timedInterval in ms
// 'CALL' will execute command once and close the connection
// TYPES:
// 	SIMPLE
// 	DETAIL
// 	METRIC
// 	FULL
// FORMATS:
// XML is a SummaryCollection XML format.
// OBJ is a SummaryCollection POJO.
// STATES:
// 1 = Running
// 2 = Finished
// 3 = Halted
// 4 = Turn
// 5 = Initialization
// 12 = All

package nki.objects;

import java.net.*;
import java.io.*;
import java.lang.*;
import java.util.Date;
import java.util.Arrays;
import java.util.logging.Level;

import nki.objects.MetricFilter;
import nki.constants.Constants;
import nki.util.LoggerWrapper;

public class Command implements Serializable {

  public static final long serialVersionUID = 42L;
  private String mode = Constants.COM_MODE_CALL;
  private String type = Constants.COM_TYPE_SIMPLE;
  private String format = Constants.COM_FORMAT_XML;
  //private String command = Constants.COM_FUNCTION_FETCH;
  private int state = Constants.STATE_ALL_PSEUDO;
  private Object payload;
  private MetricFilter filter;
  private Date dateTime;
  private String runId = "";
  private long timedInterval = 10000;
  private String info;
  private String message;
  private String retType = Constants.COM_RET_TYPE_BYSTATE;  // Type of retrieval.

  public static final int[] STATES = {
      Constants.STATE_RUNNING,
      Constants.STATE_FINISHED,
      Constants.STATE_HANG,
      Constants.STATE_TURN,
      Constants.STATE_INIT,
      Constants.STATE_ALL_PSEUDO
  };

  public static final String[] TYPES = {
      Constants.COM_TYPE_SIMPLE,
      Constants.COM_TYPE_DETAIL,
      Constants.COM_TYPE_METRIC,
      Constants.COM_TYPE_FULL
  };

  public Command() {
    this.setDateTime();  // Set date time for instantiation of command object.
  }

  // Instantiate with variables
  //public Command(String format, int state, String command, String mode, String type, String runId) {
  public Command(String format, int state, String mode, String type, String runId) {
    this.setFormat(format);
    //this.setCommand(command);
    this.setMode(mode);
    this.setType(type);
    this.setRunId(runId);
    this.setDateTime();  // Set date time for instantiation of command object.
  }

  public void setDateTime() {
    this.dateTime = new Date();
  }

  /*public void setCommand(String command) {
    this.command = command;
  }

  public String getCommand() {
    return command;
  }
    */
  public Date getDateTime() {
    return dateTime;
  }

  public void setMessage(String msg) {
    this.message = msg;
  }

  public String getMessage() {
    return message;
  }

  public void setInfo(String infomsg) {
    this.info = infomsg;
  }

  public String getMode() {
    return mode;
  }

  public void setMode(String modeSet) {
    if (modeSet == Constants.COM_MODE_TIMED) {
      this.mode = Constants.COM_MODE_TIMED;
    }
    else {
      this.mode = Constants.COM_MODE_CALL;
    }
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    if (!checkType(type)) {
      setMessage("Invalid type (" + type + ")");
      type = null;
    }
    else {
      this.type = type;
    }
  }

  public long getTimedInterval() {
    return timedInterval;
  }

  public void setTimedInterval(long ti) {
    if (ti < 10000) {
      this.timedInterval = 10000;
    }
    else {
      this.timedInterval = ti;
    }
  }

  public int getState() {
    return state;
  }

  public void setState(int st) {
    if (!checkState(st)) {
      setMessage("Invalid state (" + st + ")");
      state = 0;
    }
    else {
      this.retType = Constants.COM_RET_TYPE_BYSTATE;
      this.state = st;
    }
  }

  public String getFormat() {
    return format;
  }

  public void setFormat(String form) {
    if (form.equals(Constants.COM_FORMAT_OBJ)) {
      this.format = Constants.COM_FORMAT_OBJ;
    }
    else if (form.equals(Constants.COM_FORMAT_TAB)) {
      this.format = Constants.COM_FORMAT_TAB;
    }
    else if (form.equals(Constants.COM_FORMAT_JSON)){
        this.format = Constants.COM_FORMAT_JSON;
    }else{
      this.format = Constants.COM_FORMAT_XML;
    }
  }

  public boolean checkParams() {
    if (format == null || mode == null || type == null) {
      LoggerWrapper.log.log(Level.INFO, "Something is null...");
      return false;
    }

    if (!checkState(state)) {
      LoggerWrapper.log.log(Level.INFO, "State invalid " + state);
      return false;
    }

    return true;
  }

  public void setRunId(String runId) {
    this.retType = Constants.COM_RET_TYPE_BYRUN;
    this.runId = runId;
  }

  public String getRunId() {
    return runId;
  }

  public boolean checkState(int st) {
    for (int STATE : STATES) {
      if (STATE == st) {
        return true;
      }
    }
    return false;
  }

  public boolean checkType(String type) {
    for (String TYPE : TYPES) {
      if (TYPE.equals(type)) {
        return true;
      }
    }
    return false;
  }

  private boolean hasMetricFilter() {
    if (filter != null) {
      return true;
    }
    else {
      return false;
    }
  }

  private void setMetricFilter(MetricFilter filter) {
    this.filter = filter;
  }

  private MetricFilter getMetricFilter() {
    return filter;
  }

  public String getRetType() {
    return retType;
  }

  public void setRetType(String retType) {
    if (retType.equals(Constants.COM_RET_TYPE_BYSTATE)) {
      this.retType = Constants.COM_RET_TYPE_BYSTATE;
    }
    else {
      this.retType = Constants.COM_RET_TYPE_BYRUN;
    }
  }

}

