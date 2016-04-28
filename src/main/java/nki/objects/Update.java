// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2014 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.objects;

import java.io.*;
import java.security.MessageDigest;

public class Update implements Serializable {

  public static final long serialVersionUID = 42L;
  public byte[] checksum;
  private String msg;
  private int currentProcessing;
  private int totalProcessing;

  public void setChecksum(Summary sum) {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ObjectOutput out = null;
    try {
      try {
        out = new ObjectOutputStream(bos);
        out.writeObject(sum);
        byte[] sumObjArray = bos.toByteArray();

        MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
        digest.update(sumObjArray);
        this.checksum = digest.digest();
      }
      catch (Exception ex) {

      } finally {
        out.close();
        bos.close();
      }
    }
    catch (Exception Ex) {

    }
  }

  public byte[] getChecksum() {
    return this.checksum;
  }

  public void setMsg(String msg) {
    this.msg = msg;
  }

  public String getMsg() {
    return this.msg;
  }

  public void setCurrentProcessing(int currentProcessing) {
    this.currentProcessing = currentProcessing;
  }

  public int getCurrentProcessing() {
    return this.currentProcessing;
  }

  public void setTotalProcessing(int totalProcessing) {
    this.totalProcessing = totalProcessing;
  }

  public int getTotalProcessing() {
    return this.totalProcessing;
  }
}
