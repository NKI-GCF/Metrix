// Metrix - A server / client floaterface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.util;

import java.io.*;
import java.util.*;

public class ArrayUtils{
    public static float sum (List<Float> a){
        if (a.size() > 0) {
            float sum = 0;

            for (Float i : a) {
                sum += i;
            }
            return sum;
        }
        return 0;
    }
    public static double mean (List<Float> a){
        float sum = sum(a);
        double mean = 0;
        mean = sum / (a.size() * 1.0);
        return mean;
    }
    public static double median (List<Float> a){
        int middle = a.size()/2;

        if (a.size() % 2 == 1) {
            return a.get(middle);
        } else {
           return (a.get(middle-1) + a.get(middle)) / 2.0;
        }
    }
    public static double sd (List<Float> a){
        float sum = 0;
        double mean = mean(a);

        for (Float i : a)
            sum += Math.pow((i - mean), 2);
        return Math.sqrt( sum / ( a.size() - 1 ) ); // sample
    }

	public double getVariance(List<Float> data){
        double mean = mean(data);
        double temp = 0;
		double size = data.size();

        for(double a :data){
			temp += (mean-a)*(mean-a);
		}

		return temp/size;
    }
}
