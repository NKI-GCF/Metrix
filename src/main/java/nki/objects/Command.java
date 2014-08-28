// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2014 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

/* TYPES:
    SIMPLE                  Constants.COM_TYPE_SIMPLE
    DETAIL                  Constants.COM_TYPE_DETAIL

   FORMATS:
     XML                    Constants.COM_FORMAT_XML
     OBJ                    Constants.COM_FORMAT_OBJ
     JSON                   Constants.COM_FORMAT_JSON

   STATES:
    1 = Running              Constants.STATE_RUNNING
    2 = Finished             Constants.STATE_FINISHED
    3 = Halted               Constants.STATE_HANG
    4 = Turn                 Constants.STATE_TURN
    5 = Initialization       Constants.STATE_INIT
    12 = All                 Constants.STATE_ALL_PSEUDO
*/

package nki.objects;

import java.io.*;
import java.util.Date;
import java.util.logging.Level;

import nki.constants.Constants;
import nki.util.LoggerWrapper;

public final class Command implements Serializable {

  public static final long serialVersionUID = 42L;
  private String type = Constants.COM_TYPE_SIMPLE;
  private String format = Constants.COM_FORMAT_XML;
  private int state = Constants.STATE_ALL_PSEUDO;
  private MetricFilter filter;
  private Date dateTime;
  private String runId = "";
  private String message;
  private String retType = Constants.COM_RET_TYPE_BYRUN;  // Type of retrieval.
  private String runIdSearch;
  
  public static final int[] STATES = {
      Constants.STATE_RUNNING,
      Constants.STATE_FINISHED,
      Constants.STATE_HANG,
      Constants.STATE_TURN,
      Constants.STATE_INIT,
      Constants.STATE_ALL_PSEUDO
  };

  public static final String[] TYPES = {
      Constants.COM_INITIALIZE,
      Constants.COM_SEARCH,
      Constants.COM_TYPE_SIMPLE,
      Constants.COM_TYPE_DETAIL
  };

  public Command() {
    this.setDateTime();  // Set date time for instantiation of command object.
  }

  // Instantiate with variables
  //public Command(String format, int state, String command, String mode, String type, String runId) {
  public Command(String format, int state, String type, String runId) {
    this.setFormat(format);
    this.setType(type);
    this.setRunId(runId);
    this.setDateTime();  // Set date time for instantiation of command object.
  }

  public void setDateTime() {
    this.dateTime = new Date();
  }

  public Date getDateTime() {
    return dateTime;
  }

  public void setMessage(String msg) {
    this.message = msg;
  }

  public String getMessage() {
    return message;
  }

    public void setRunIdSearch(String runIdSearch) {
    this.runIdSearch = runIdSearch;
  }

  public String getRunIdSearch() {
    return runIdSearch;
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
    if (format == null || type == null) {
      LoggerWrapper.log.log(Level.SEVERE, "Format or Type is null.");
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
    }else if(retType.equals(Constants.COM_SEARCH)){
        this.retType = Constants.COM_SEARCH;
    }else if(retType.equals(Constants.COM_PARSE)){
        this.retType = Constants.COM_PARSE;
    }else if(retType.equals(Constants.COM_INITIALIZE)){
        this.retType = Constants.COM_INITIALIZE;
    }else {
      // Default to search by run.
      LoggerWrapper.log.finer("Defaulting to COM_RET_TYPE_BYRUN.");
      this.retType = Constants.COM_RET_TYPE_BYRUN;
    }
  }

}

