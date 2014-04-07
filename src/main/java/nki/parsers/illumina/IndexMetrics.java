// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.parsers.illumina;

import java.io.IOException;
import java.util.logging.Level;

import nki.objects.Indices;
import nki.util.LoggerWrapper;

public class IndexMetrics extends GenericIlluminaParser {
  // Instantiate Logger
  private static final LoggerWrapper metrixLogger = LoggerWrapper.getInstance();

  public IndexMetrics(String source, int state) {
    super(IndexMetrics.class, source, state);
  }

	/*
   * Binary structure
	 * byte 0: file version number (1)
	 * bytes (variable length): record:
	 * 2 bytes: lane number (uint16)
	 * 2 bytes: tile number (uint16)
	 * 2 bytes: read number (uint16)
	 * 2 bytes: number of bytes Y for index sequence (uint16)
	 * Y bytes: index sequence (string encoded in UTF-8)
	 * 4 bytes: number of clusters identified as index (uint32)
	 * 2 bytes: number of bytes V for sample name (uint16)
	 * V bytes: sample name string (string encoded in UTF-8)
	 * 2 bytes: number of bytes W for sample project name (uint16)
	 * W bytes: sample project name string (string encoded in UTF-8
	 */

  public Indices digestData() {
    if (fileMissing) {
      return new Indices();
    }
    Indices indices = new Indices();

    // First catch version of metrics file.
    try {
      setVersion(leis.readByte());    // Set Version
    }
    catch (IOException Ex) {
      LoggerWrapper.log.log(Level.SEVERE, "Error in parsing version number and recordlength: {0}", Ex.toString());
    }

    boolean readBool = true;

    try {
      while (readBool) {
        int laneNr = leis.readUnsignedShort();
        int tileNr = leis.readUnsignedShort();
        int readNr = leis.readUnsignedShort();

        int numBytesIdx = leis.readUnsignedShort();

        String indexSeq = leis.readUTF8String(numBytesIdx);

        int numClustersIdx = leis.readInt();
        int numBytesSample = leis.readUnsignedShort();

        String sampleSeq = leis.readUTF8String(numBytesSample);

        int numBytesProject = leis.readUnsignedShort();

        String projectSeq = leis.readUTF8String(numBytesProject);

        indices.setIndex(projectSeq, sampleSeq, indexSeq, numClustersIdx, laneNr, readNr);
//				System.out.println(laneNr + "\t" + tileNr + "\t" + readNr + "\t" + projectSeq + "\t" + sampleSeq + "\t" + indexSeq + "\n");

      }
    }
    catch (IOException ExMain) {
      readBool = false;
    }

    return indices;
  }

}	
