// Illumina Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.objects;

import java.io.*;


public class Metric implements Serializable {

	public static final long serialVersionUID = 42L;	
        private Float metric = 0.0f;
        private int tiles = 0;

        public void setMetric(Float metricScore){
                this.metric = metricScore;
        }

        public Float getMetric(){
                return metric;
        }

        public void setTiles(int tileCount){
                this.tiles = tileCount;
        }

        public int getTiles(){
                return tiles;
        }

        public void incrementMetric(Float metricScore){
                this.metric += metricScore;
        }

        public void incrementTiles(){
                this.tiles += 1;
        }

        public Float getAvg(){
                return (metric / tiles);
        }
	
}
