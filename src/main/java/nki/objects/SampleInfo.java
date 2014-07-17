package nki.objects;

import java.io.Serializable;

/**
 * nki.objects
 * <p/>
 * <p/>
 * Info
 *
 * @author Rob Davey
 * @date 07/04/14
 * @since version
 */
public class SampleInfo implements Serializable {
  private int readNum;
  private int laneNum;
  private long numClusters;
  private String indexBarcode = "";

  public int getReadNum() {
    return readNum;
  }

  public void setReadNum(int readNum) {
    this.readNum = readNum;
  }

  public int getLaneNum() {
    return laneNum;
  }

  public void setLaneNum(int laneNum) {
    this.laneNum = laneNum;
  }

  public long getNumClusters() {
    return numClusters;
  }

  public void setNumClusters(long numClusters) {
    this.numClusters = numClusters;
  }

  public String getIndexBarcode() {
    return indexBarcode;
  }

  public void setIndexBarcode(String indexBarcode) {
    this.indexBarcode = indexBarcode;
  }
}
