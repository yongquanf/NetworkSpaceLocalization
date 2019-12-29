package edu.NUDT.pdl.Nina.KNN;

import java.util.Comparator;

public class DoubleComp implements Comparator {

	public int compare(Object o1, Object o2) {
		// TODO Auto-generated method stub
		double d1 = ((Double) o1).doubleValue();
		double d2 = ((Double) o2).doubleValue();
		if (d1 < d2) {
			return -1;
		} else if (d1 == d2) {
			return 0;
		} else {
			return 1;
		}

	}

}