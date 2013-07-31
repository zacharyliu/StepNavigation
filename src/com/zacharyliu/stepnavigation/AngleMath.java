package com.zacharyliu.stepnavigation;

public class AngleMath {
	public static final int RADIANS = 1;
	public static final int DEGREES = 2;
	
	public static double difference(int angleType, double angle1, double angle2) {
		double diff = angle1 - angle2;
		if (angleType == DEGREES) {
			diff = (diff+180) % 360 - 180;
		} else if (angleType == RADIANS) {
			diff = (diff+Math.PI) % (2*Math.PI) - Math.PI;
		}
		return diff;
	}
	
	public static double average(int angleType, double[] angles) {
		if (angleType == DEGREES) {
			for (int i=0; i<angles.length; i++) {
				angles[i] = Math.toRadians(angles[i]);
			}
		}
		
		double sumX = 0.0;
		double sumY = 0.0;
		
		for (int i=0; i<angles.length; i++) {
			sumX += Math.cos(angles[i]);
			sumY += Math.sin(angles[i]);
		}
		
		double mean = Math.atan2(sumY/angles.length, sumX/angles.length);
		if (angleType == DEGREES) {
			mean = Math.toDegrees(mean);
		}
		return mean;
	}
	
	public static double weightedAverage(int angleType, double[] angles, double[] weights) {
		if (angleType == DEGREES) {
			for (int i=0; i<angles.length; i++) {
				angles[i] = Math.toRadians(angles[i]);
			}
		}
		
		double sumX = 0.0;
		double sumY = 0.0;
		double sumWeights = 0.0;
		
		for (int i=0; i<angles.length; i++) {
			sumX += Math.cos(angles[i]) * weights[i];
			sumY += Math.sin(angles[i]) * weights[i];
			sumWeights += weights[i];
		}
		
		double mean = Math.atan2(sumY/sumWeights, sumX/sumWeights);
		if (angleType == DEGREES) {
			mean = Math.toDegrees(mean);
		}
		return mean;
	}
}
