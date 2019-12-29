package edu.NUDT.pdl.Nina.KNN;

import java.util.Comparator;

public class NodesPairComp implements Comparator {

	public int compare(Object o1, Object o2) {
		// TODO Auto-generated method stub
		double d1 = ((NodesPair) o1).rtt;
		double d2 = ((NodesPair) o2).rtt;
		if (d1 < d2) {
			return -1;
		} else if (d1 == d2) {
			return 0;
		} else {
			return 1;
		}

	}

}