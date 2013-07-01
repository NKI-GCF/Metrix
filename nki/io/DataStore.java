// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.io;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.Properties;
import nki.objects.Summary;
import nki.objects.SummaryCollection;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

public class DataStore {
	static final String WRITE_OBJECT_SQL = "INSERT INTO metrix_objects(run_id, object_value, state) VALUES (?, ?, ?)";
	static final String UPDATE_OBJECT_SQL_ID = "UPDATE metrix_objects SET object_value = ?, state = ? WHERE id = ?";
	static final String UPDATE_OBJECT_SQL_RUNNAME = "UPDATE metrix_objects SET object_value = ?, state = ? WHERE run_id = ?";
	static final String READ_OBJECT_SQL_ID = "SELECT object_value FROM metrix_objects WHERE id = ?";
	static final String READ_OBJECT_SQL_RUNNAME = "SELECT object_value FROM metrix_objects WHERE run_id = ?";
	static final String READ_OBJECT_SQL_STATE = "SELECT object_value FROM metrix_objects WHERE state = ?";
	static final String READ_OBJECT_SQL_ALL = "SELECT object_value FROM metrix_objects;";

	private HashMap<String, Summary> results;
	private boolean emptyCheck = false;
	private static Logger metrixLogger;
	private static Properties configFile = new Properties();
 	public static Connection conn = null;

	public DataStore() throws IOException{
		metrixLogger = Logger.getLogger(DataStore.class.getName());
       
	        String externalFileName = System.getProperty("properties");
	        String absFile = (new File(externalFileName)).getAbsolutePath();

	        InputStream fin = new FileInputStream(new File(absFile));
        	configFile.load(fin);
	
		try{
			conn = getConnection();
		}catch(Exception ex){
			ex.printStackTrace();
			System.exit(0);
		}
	}

	public static Connection getConnection() throws Exception{
		// host
		String host = configFile.getProperty("SQL_HOST", "localhost");

		// port
		String port = configFile.getProperty("SQL_PORT", "3306");

		// user
		String user = configFile.getProperty("SQL_USER", "root");

		// pass
		String pass = configFile.getProperty("SQL_PASS", "test");

		// db
		String db = configFile.getProperty("SQL_DB", "metrix"); 

		String driver = "com.mysql.jdbc.Driver";
		String url = "jdbc:mysql://"+ host +":"+port+"/"+db+"?autoReconnect=true&characterEncoding=UTF-8&useUnicode=true";
		Class.forName(driver);
		Connection conn = DriverManager.getConnection(url, user, pass);
		return conn;
	}

	public static long appendedWrite(Summary sum, String runId) throws Exception{
		String className = sum.getClass().getName();
		PreparedStatement pstmt = conn.prepareStatement(WRITE_OBJECT_SQL);
		
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
		if (rs.next()){
			id = rs.getInt(1);
		}

		rs.close();
		pstmt.close();
		return id;
	}

	public static Summary getSummaryById(long id) throws Exception{
		PreparedStatement pstmt = conn.prepareStatement(READ_OBJECT_SQL_ID);
		pstmt.setLong(1, id);
		ResultSet rs = pstmt.executeQuery();
		rs.next();
		Summary sum = (Summary) rs.getObject(1);
		String className = sum.getClass().getName();
		rs.close();
		pstmt.close();

		return sum;		
	}

	public static Summary getSummaryByRunName(String runName) throws Exception{
		PreparedStatement pstmt = conn.prepareStatement(READ_OBJECT_SQL_RUNNAME);
		pstmt.setString(1, runName);

		ResultSet rs = pstmt.executeQuery();
		Summary sum = new Summary();

		while(rs.next()){
			byte[] buf = rs.getBytes(1);
			ObjectInputStream objectIn = null;
			if (buf != null){
				objectIn = new ObjectInputStream(new ByteArrayInputStream(buf));
			}
			Object sumObject = objectIn.readObject();
			sum = (Summary) sumObject;
		}
		
		rs.close();
		pstmt.close();

		return sum;
	}

	public static SummaryCollection getSummaryCollectionByState(int state) throws Exception {
		PreparedStatement pstmt = conn.prepareStatement(READ_OBJECT_SQL_STATE);
		pstmt.setInt(1, state);

		ResultSet rs = pstmt.executeQuery();
		SummaryCollection sc = new SummaryCollection();
			
		while(rs.next()){
			byte[] buf = rs.getBytes(1);
			ObjectInputStream objectIn = null;
			if (buf != null){
				objectIn = new ObjectInputStream(new ByteArrayInputStream(buf));
			}
			Object sumObject = objectIn.readObject();
			Summary sum = (Summary) sumObject;
			sc.appendSummary(sum);
		}
		rs.close();
		pstmt.close();

		return sc;
	}

	public static SummaryCollection getSummaryCollections() throws Exception {
		PreparedStatement pstmt = conn.prepareStatement(READ_OBJECT_SQL_ALL);
		
		ResultSet rs = pstmt.executeQuery();
		SummaryCollection sc  = new SummaryCollection();

		while(rs.next()){
			byte[] buf = rs.getBytes(1);
			ObjectInputStream objectIn = null;
			if(buf != null){
				objectIn = new ObjectInputStream(new ByteArrayInputStream(buf));
			}
			Object sumObject = objectIn.readObject();
			Summary sum = (Summary) sumObject;
			sc.appendSummary(sum);
		}
		rs.close();
		pstmt.close();

		return sc;
	}

	public static void updateSummaryByRunName(Summary sum, String runName) throws Exception {
		PreparedStatement pstmt = conn.prepareStatement(UPDATE_OBJECT_SQL_RUNNAME);
		sum.setLastUpdated();
		pstmt.setObject(1, sum);
		pstmt.setInt(2, sum.getState());
		pstmt.setString(3, runName);
		
		pstmt.executeUpdate();
		pstmt.close();
	}
	
	public static void updateSummaryById(Summary sum, int id) throws Exception {
		PreparedStatement pstmt = conn.prepareStatement(UPDATE_OBJECT_SQL_ID);
		sum.setLastUpdated();
		pstmt.setObject(1, sum);
		pstmt.setInt(2, id);

		pstmt.executeUpdate();
		pstmt.close();
	}

	public static int getMaxId() throws Exception{
		int maxID = 0;
		Statement s2 = conn.createStatement();
		s2.execute("SELECT MAX(id) FROM metrix_objects");    
		ResultSet rs2 = s2.getResultSet(); // 
		while ( rs2.next() ){
			maxID = rs2.getInt(1);
		}
		
		return maxID;
	}

	public static boolean checkSummaryByRunId(String run) throws Exception{
		PreparedStatement pstmt = conn.prepareStatement(READ_OBJECT_SQL_RUNNAME);
		pstmt.setString(1, run);
	
		boolean ret = false;
	
		ResultSet rs = pstmt.executeQuery();
		if(rs.next()){
			ret = true;
		}else{
			ret = false;
		}
	
		rs.close();
		pstmt.close();
		return ret;
	}

	public static void closeAll(){
		try{
			conn.close();
		}catch(SQLException ex){
			metrixLogger.log(Level.SEVERE, "Error closing SQL Connection! " + ex.toString());
		}
	}	

}
