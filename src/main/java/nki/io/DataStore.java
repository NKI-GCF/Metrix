// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2014 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.io;

import java.io.*;
import java.util.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.Properties;

import nki.objects.Summary;
import nki.objects.SummaryCollection;
import nki.util.LoggerWrapper;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.logging.Level;

public class DataStore {
  static final String WRITE_OBJECT_SQL = "INSERT INTO metrix_objects(run_id, object_value, state) VALUES (?, ?, ?)";
  static final String UPDATE_OBJECT_SQL_ID = "UPDATE metrix_objects SET object_value = ?, state = ? WHERE id = ?";
  static final String UPDATE_OBJECT_SQL_RUNNAME = "UPDATE metrix_objects SET object_value = ?, state = ? WHERE run_id = ?";
  static final String READ_OBJECT_SQL_ID = "SELECT object_value FROM metrix_objects WHERE id = ?";
  static final String READ_OBJECT_SQL_RUNNAME = "SELECT run_id FROM metrix_objects WHERE run_id = ?";
  static final String READ_OBJECT_SQL_STATE = "SELECT object_value FROM metrix_objects WHERE state = ?";
  static final String READ_OBJECT_SQL_ALL = "SELECT object_value FROM metrix_objects;";

  private static final LoggerWrapper metrixLogger = LoggerWrapper.getInstance();

  private static Properties configFile = new Properties();
  public static Connection conn = null;

  public DataStore() throws IOException {
    String externalFileName = System.getProperty("properties");
    String absFile = (new File(externalFileName)).getAbsolutePath();

    InputStream fin = new FileInputStream(new File(absFile));
    configFile.load(fin);

    fin.close();
    try {
      LoggerWrapper.log.log(Level.FINER, "Opening connection");
      conn = getConnection();
      LoggerWrapper.log.log(Level.FINER, "Opened connection");
    }
    catch (Exception ex) {
      metrixLogger.log.severe("Error setting up database connection. " + ex.toString());
      ex.printStackTrace();
      System.exit(0);
    }
  }

  public static Connection getConnection() throws Exception {
    // Load configuration settings for database connection enabling respective default values if no value set.
    // host
    String host = configFile.getProperty("SQL_HOST", "localhost");

    // port
    String port = configFile.getProperty("SQL_PORT", "3306");

    // user
    String user = configFile.getProperty("SQL_USER", "root");

    // pass
    String pass = configFile.getProperty("SQL_PASS", "root");

    // database name
    String db = configFile.getProperty("SQL_DB", "metrix");

    // database server type
    String db_type = configFile.getProperty("DB_SERVER_TYPE", "MYSQL");

    String driver;
    String url;

    // Server type is Microsoft SQL Server
    if (db_type.equals("MSSQL")) {
      driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
      url = "jdbc:sqlserver://" + host + ":" + port + ";DatabaseName=" + db + ";user=" + user + ";Password=" + pass;
      metrixLogger.log.finest("Connecting to database (MSSQL) with url: " + url + "\tDriver: " + driver);
    }
    else {  // Server type is Mysql (default)
      driver = "com.mysql.jdbc.Driver";
      url = "jdbc:mysql://" + host + ":" + port + "/" + db + "?autoReconnect=true&characterEncoding=UTF-8&useUnicode=true";
      metrixLogger.log.finest("Connecting to database (MYSQL) with url: " + url + "\tDriver: " + driver);
    }

    metrixLogger.log.finest("Connecting to database using driver: " + driver + ". Using url: " + url);

    Class.forName(driver);
    Connection conn = DriverManager.getConnection(url, user, pass);
    return conn;
  }

  public static long appendedWrite(Summary sum, String runId) throws Exception {
    String className = sum.getClass().getName();
    PreparedStatement pstmt = conn.prepareStatement(WRITE_OBJECT_SQL, Statement.RETURN_GENERATED_KEYS);

    sum.setLastUpdated();

    Object sum2 = (Object) sum;
    // Set input parameters
    pstmt.setString(1, runId);
    pstmt.setObject(2, sum2);
    pstmt.setInt(3, sum.getState());
    pstmt.executeUpdate();

    // get the generated key for the id
    ResultSet rs = pstmt.getGeneratedKeys();
    int id = -1;
    if (rs.next()) {
      id = rs.getInt(1);
    }
    try {
      if (rs != null) {
        rs.close();
      }
      if (pstmt != null) {
        pstmt.close();
      }
    }
    catch (Exception E) {
      metrixLogger.log.severe("Error in closing resource sets of SQL Connection. " + E.toString());
    }
    return id;
  }

  public static Summary getSummaryById(long id) throws Exception {
    PreparedStatement pstmt = conn.prepareStatement(READ_OBJECT_SQL_ID);
    metrixLogger.log.fine("Fetching summary by ID --.");
    pstmt.setLong(1, id);
    ResultSet rs = pstmt.executeQuery();
    rs.next();
    Summary sum = (Summary) rs.getObject(1);
    String className = sum.getClass().getName();

    try {
      if (rs != null) {
        rs.close();
      }
      if (pstmt != null) {
        pstmt.close();
      }
    }
    catch (Exception E) {
      metrixLogger.log.severe("Error in closing resource sets of SQL Connection. " + E.toString());
    }

    return sum;
  }

  public static Summary getSummaryByRunName(String runName) throws Exception {
    PreparedStatement pstmt = conn.prepareStatement(READ_OBJECT_SQL_RUNNAME, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    metrixLogger.log.fine("Fetching summary by run name . " + runName);
    pstmt.setFetchSize(Integer.MIN_VALUE);
    pstmt.setString(1, runName);

    ResultSet rs = pstmt.executeQuery();
    Summary sum = new Summary();

    while (rs.next()) {
      byte[] buf = rs.getBytes(1);
      ObjectInputStream objectIn = null;
      if (buf != null) {
        objectIn = new ObjectInputStream(new ByteArrayInputStream(buf));
      }
      Object sumObject = objectIn.readObject();
      sum = (Summary) sumObject;
    }

    try {
      if (rs != null) {
        rs.close();
      }
      if (pstmt != null) {
        pstmt.close();
      }
    }
    catch (Exception E) {
      metrixLogger.log.severe("Error in closing resource sets of SQL Connection. " + E.toString());
    }

    return sum;
  }

  public static SummaryCollection getSummaryCollectionByState(int state) throws Exception {
    PreparedStatement pstmt = conn.prepareStatement(READ_OBJECT_SQL_STATE);
    metrixLogger.log.fine("Fetching by state.");
    
    pstmt.setInt(1, state);
   
    ResultSet rs = pstmt.executeQuery();
    SummaryCollection sc = new SummaryCollection();

    while (rs.next()) {
      byte[] buf = rs.getBytes(1);
      ObjectInputStream objectIn = null;
      if (buf != null) {
        objectIn = new ObjectInputStream(new ByteArrayInputStream(buf));
      }
      Object sumObject = objectIn.readObject();
      Summary sum = (Summary) sumObject;
      sc.appendSummary(sum);
    }

    try {
      if (rs != null) {
        rs.close();
      }
      if (pstmt != null) {
        pstmt.close();
      }
    }
    catch (Exception E) {
      metrixLogger.log.severe("Error in closing resource sets of SQL Connection. " + E.toString());
    }

    return sc;
  }

  public static SummaryCollection getSummaryCollections() throws Exception {
    PreparedStatement pstmt = conn.prepareStatement(READ_OBJECT_SQL_ALL);
    metrixLogger.log.fine("Fetching all summaries.");

    ResultSet rs = pstmt.executeQuery();
    SummaryCollection sc = new SummaryCollection();

    while (rs.next()) {
      byte[] buf = rs.getBytes(1);
      ObjectInputStream objectIn = null;
      if (buf != null) {
        objectIn = new ObjectInputStream(new ByteArrayInputStream(buf));
      }
      Object sumObject = objectIn.readObject();
      Summary sum = (Summary) sumObject;
      sc.appendSummary(sum);
    }

    try {
      if (rs != null) {
        rs.close();
      }
      if (pstmt != null) {
        pstmt.close();
      }
    }
    catch (Exception E) {
      metrixLogger.log.severe("Error in closing resource sets of SQL Connection. " + E.toString());
    }

    return sc;
  }

  public static void updateSummaryByRunName(Summary sum, String runName) throws Exception {
    PreparedStatement pstmt = conn.prepareStatement(UPDATE_OBJECT_SQL_RUNNAME);
    sum.setLastUpdated();
    pstmt.setObject(1, sum);
    pstmt.setInt(2, sum.getState());
    pstmt.setString(3, sum.getRunDirectory());
    LoggerWrapper.log.log(Level.FINE, "Updating summary object " + runName);
    pstmt.executeUpdate();

    try {
      if (pstmt != null) {
        pstmt.close();
      }
    }
    catch (Exception E) {
      metrixLogger.log.severe("Error in closing resource sets of SQL Connection. " + E.toString());
    }
  }

  public static void updateSummaryById(Summary sum, int id) throws Exception {
    PreparedStatement pstmt = conn.prepareStatement(UPDATE_OBJECT_SQL_ID);
    sum.setLastUpdated();
    pstmt.setObject(1, sum);
    pstmt.setInt(2, id);

    pstmt.executeUpdate();

    try {
      if (pstmt != null) {
        pstmt.close();
      }
    }
    catch (Exception E) {
      metrixLogger.log.severe("Error in closing resource sets of SQL Connection. " + E.toString());
    }
  }

  public static int getMaxId() throws Exception {
    int maxID = 0;
    PreparedStatement s2 = conn.prepareStatement("SELECT MAX(id) FROM metrix_objects", Statement.RETURN_GENERATED_KEYS);
//    s2.execute("SELECT MAX(id) FROM metrix_objects", Statement.RETURN_GENERATED_KEYS);
    s2.executeQuery();
    ResultSet rs2 = s2.getResultSet(); //
    while (rs2.next()) {
      maxID = rs2.getInt(1);
    }

    try {
      if (s2 != null) {
        s2.close();
      }
      if (rs2 != null) {
        rs2.close();
      }
    }
    catch (Exception E) {
      metrixLogger.log.severe("Error in closing resource sets of SQL Connection. " + E.toString());
    }

    return maxID;
  }

  public static boolean checkSummaryByRunId(String run) throws Exception {
    PreparedStatement pstmt = conn.prepareStatement(READ_OBJECT_SQL_RUNNAME, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    metrixLogger.log.fine("Fetching run by ID. " + run);
    pstmt.setFetchSize(Integer.MIN_VALUE);
    pstmt.setString(1, run);

    boolean ret = false;

    ResultSet rs = pstmt.executeQuery();
    if (rs.next()) {
      ret = true;
    }
    else {
      ret = false;
    }

    try {
      if (rs != null) {
        rs.close();
      }
      if (pstmt != null) {
        pstmt.close();
      }
    }
    catch (Exception E) {
      metrixLogger.log.severe("Error in closing resource sets of SQL Connection. " + E.toString());
    }

    return ret;
  }

  public static void closeAll() {
    try {
      conn.close();
    }
    catch (SQLException ex) {
      metrixLogger.log.severe("Error closing SQL Connection! " + ex.toString());
    }
  }
}
