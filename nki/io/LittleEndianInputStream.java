// Illumina Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.io;

import java.io.*;

public class LittleEndianInputStream extends FilterInputStream {

  public LittleEndianInputStream(InputStream in) {
    super(in);
  }

  public boolean readBoolean() throws IOException {
    int bool = in.read();
    if (bool == -1) throw new EOFException();
    return (bool != 0);
  }

  public byte readByte() throws IOException {
    int temp = in.read();
    if (temp == -1) throw new EOFException();
    return (byte) temp;
  }

  public int readUnsignedByte() throws IOException {
    int temp = in.read();
    if (temp == -1) throw new EOFException();
    return temp;
  }

  public short readShort() throws IOException {
    int byte1 = in.read();
    int byte2 = in.read();
    // only need to test last byte read
    // if byte1 is -1 so is byte2
    if (byte2 == -1) throw new EOFException();
    return (short) (((byte2 << 24) >>> 16) + (byte1 << 24) >>> 24);
  }

  public int readUnsignedShort() throws IOException {
    int byte1 = in.read();
    int byte2 = in.read();
    if (byte2 == -1) throw new EOFException();
    return ((byte2 << 24) >>> 16) + ((byte1 << 24) >>> 24);
  }

  public char readChar() throws IOException {
    int byte1 = in.read();
    int byte2 = in.read();
    if (byte2 == -1) throw new EOFException();
    return (char) (((byte2 << 24) >>> 16) + ((byte1 << 24) >>> 24));
  }

  public int readInt() throws IOException {

    int byte1 = in.read();
    int byte2 = in.read();
    int byte3 = in.read();
    int byte4 = in.read();
    if (byte4 == -1) {
      throw new EOFException();
    }
    return (byte4 << 24) 
     + ((byte3 << 24) >>> 8) 
     + ((byte2 << 24) >>> 16) 
     + ((byte1 << 24) >>> 24);
    
  }

  public long readLong() throws IOException {

    long byte1 = in.read();
    long byte2 = in.read();
    long byte3 = in.read();
    long byte4 = in.read();
    long byte5 = in.read();
    long byte6 = in.read();
    long byte7 = in.read();
    long byte8 = in.read();
    if (byte8 == -1) {
      throw new EOFException();
    }
    return (byte8 << 56) 
     + ((byte7 << 56) >>> 8) 
     + ((byte6 << 56) >>> 16) 
     + ((byte5 << 56) >>> 24) 
     + ((byte4 << 56) >>> 32) 
     + ((byte3 << 56) >>> 40) 
     + ((byte2 << 56) >>> 48) 
     + ((byte1 << 56) >>> 56);
    
  }

  public final String readUTF8String(int numBytes) throws IOException {
    byte[] bytes = new byte[numBytes];

	for(int i=0; i < numBytes; i++){
		bytes[i] = (byte) in.read();
	}

	return new String(bytes, "UTF-8");

  }

  public final double readDouble() throws IOException {
    return Double.longBitsToDouble(this.readLong());
  }
  
  public final float readFloat() throws IOException {
    return Float.intBitsToFloat(this.readInt());  
  }

  public final int skipBytes(int n) throws IOException { 
    for (int i = 0; i < n; i += (int) skip(n - i));
    return n;  
  } 

}
