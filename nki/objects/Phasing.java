// Metrix - A server / client interface for Illumina Sequencing Phasings.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.objects;

import java.io.*;
import java.util.*;
import nki.util.ArrayUtils;

public class Phasing implements Serializable {

	public static final long serialVersionUID = 42L;
	private Float phasing = 0.0f;
	private int tiles = 0;
	private List<Float> tileScores = new ArrayList<Float>();

	public void setPhasing(Float phasingScore){
                this.phasing = phasingScore;
				this.incrementTiles();
        }

        public Float getPhasing(){
                return phasing;
        }

        public void setTiles(int tileCount){
                this.tiles = tileCount;
        }

        public int getTiles(){
                return tiles;
        }

        public void incrementPhasing(Float phasingScore){
                this.phasing += phasingScore;
				this.incrementTiles();
        }

        public void incrementTiles(){
                this.tiles += 1;
        }

        public Float getLaneAvg(){
                return (phasing / tiles);
        }	

		public Float calcSum(){
			return ArrayUtils.sum(tileScores);
		}

		public double calcMean(){
			return ArrayUtils.mean(tileScores);
		}

		public double calcMedian(){
			return ArrayUtils.median(tileScores);
		}
		
		public double calcSD(){
			return ArrayUtils.sd(tileScores);
		}
}
