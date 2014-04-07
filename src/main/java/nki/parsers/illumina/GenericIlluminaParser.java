// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.parsers.illumina;

import nki.io.LittleEndianInputStream;
import nki.util.LoggerWrapper;

import java.io.IOException;
import java.io.FileInputStream;
import java.io.*;
import java.util.logging.Level;

public class GenericIlluminaParser {
  protected String source = "";
  protected LittleEndianInputStream leis = null;

  protected int version = 0;
  protected int recordLength = 0;
  protected int sleepTime = 3000;
  protected boolean fileMissing = false;
  private long lastModTime = 0;
  private static final LoggerWrapper metrixLogger = LoggerWrapper.getInstance();

  public GenericIlluminaParser(Class<?> c, String source, int state) {
    try {
      setSource(source);
      if (state == 1) {
        Thread.sleep(sleepTime);
      }
      leis = new LittleEndianInputStream(new FileInputStream(source));
      // Check for last modified date
      setLastModifiedSource();
    }
    catch (FileNotFoundException IO) {
      // Set fileMissing = true. --> Parse again later.
      setFileMissing(true);
      metrixLogger.log.log(Level.WARNING, "{0} file not available for {1}", new Object[]{c.getSimpleName(), source});
    }
    catch (InterruptedException IEX) {

    }
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getSource() {
    return source;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public int getVersion() {
    return version;
  }

  public void setRecordLength(int recordLength) {
    this.recordLength = recordLength;
  }

  public int getRecordLength() {
    return recordLength;
  }

  public void setFileMissing(boolean fileMissing) {
    this.fileMissing = fileMissing;
  }

  public boolean getFileMissing() {
    return fileMissing;
  }

  public void setLastModifiedSource() {
    File lastModFile = new File(source);
    if (lastModFile.exists()) {
      this.lastModTime = lastModFile.lastModified();
    }
  }

  public long getLastModifiedSourceDiff() {
    return (System.currentTimeMillis() - this.lastModTime);
  }

  public long getLastModifiedSource() {
    return this.lastModTime;
  }

  public void closeSourceStream() {

    if (leis != null) {
      try {
        this.leis.close();
      }
      catch (IOException Ex) {
        metrixLogger.log.log(Level.WARNING, "Error in closing the source stream: {0}", Ex.toString());
      }
    }
  }
}
