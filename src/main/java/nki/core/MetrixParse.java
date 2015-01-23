package nki.core;
// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2014 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Properties;
import nki.constants.Constants;
import nki.exceptions.EmptyResultSetCollection;
import nki.exceptions.InvalidCredentialsException;
import nki.exceptions.MissingCommandDetailException;
import nki.exceptions.UnimplementedCommandException;
import nki.objects.Command;

public class MetrixParse {
  public static void main(String[] args) {
    Properties configFile;

    configFile = new Properties();
    // Use external properties file, outside of jar location.
    String externalFileName = System.getProperty("properties");

    if (externalFileName == null) {
      System.err.println("[FATAL] Properties file not argumented as parameter. (use: java -Dproperties=metrix.properties Metrix)");
      System.exit(1);
    }

    String absFile = (new File(externalFileName)).getAbsolutePath();

    try (InputStream fin = new FileInputStream(new File(absFile))) {
      configFile.load(fin);
    }
    catch (FileNotFoundException FNFE) {
      System.err.println("[ERROR] Properties file not found.");
      System.exit(1);
    }
    catch (IOException Ex) {
      System.err.println("[ERROR] Reading properties file. " + Ex.toString());
      System.exit(1);
    }

    String searchTerm = "";

    if (args.length == 0) {
        System.err.println("Invalid number of arguments.");
        System.exit(1);
    }
    else if (args.length == 1) {
      searchTerm = args[0];

      if (searchTerm.length() <= 2) {
        System.err.println("Need a bigger search string.");
        System.exit(1);
      }
    }
    else if (args.length > 1) {
      System.err.println("Invalid number of arguments.");
      System.exit(1);
    }

    int port = Integer.parseInt(configFile.getProperty("PORT", "10000"));
    String host = configFile.getProperty("HOST", "localhost");
    
    try{
        SocketChannel sChannel = SocketChannel.open();
        sChannel.configureBlocking(true);

        if (sChannel.connect(new InetSocketAddress(host, port))) {
            // Create OutputStream for sending objects.
            ObjectOutputStream oos = new ObjectOutputStream(sChannel.socket().getOutputStream());

            // Cteate Inputstream for receiving objects.
            ObjectInputStream ois = new ObjectInputStream(sChannel.socket().getInputStream());

            Command cmd = new Command();
            
            cmd.setFormat(Constants.COM_FORMAT_OBJ);
            cmd.setRunIdSearch(searchTerm);
            cmd.setRetType(Constants.COM_PARSE);
            
            oos.writeObject(cmd);
            oos.flush();
            
            Object srvResp = new Object();
            
            while(ois != null){
                srvResp = ois.readObject();
                // Process expected response
                if (srvResp instanceof String) {
                    String s = (String) srvResp;
                    System.out.println(s);
                }
                
                /*
                 *	Exceptions
                 */
                if (srvResp instanceof EmptyResultSetCollection) {
                  System.out.println(srvResp.toString());
                }

                if (srvResp instanceof InvalidCredentialsException) {
                  System.out.println(srvResp.toString());
                }

                if (srvResp instanceof MissingCommandDetailException) {
                  System.out.println(srvResp.toString());
                }

                if (srvResp instanceof UnimplementedCommandException) {
                  System.out.println(srvResp.toString());
                }            
            }
        }
    }catch(EOFException EOF){
    
    }catch(Exception Ex){
        Ex.printStackTrace();
    }
  }
}
