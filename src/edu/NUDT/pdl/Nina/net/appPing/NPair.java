package edu.NUDT.pdl.Nina.net.appPing;

import java.util.Comparator;

public class NPair implements Comparator {

	
	public int compare(Object o1, Object o2) {
		// TODO Auto-generated method stub
		double d1 = ((pair) o1).getRtt();
		double d2 = ((pair) o2).getRtt();
		if (d1 < d2) {
			return -1;
		} else if (d1 == d2) {
			return 0;
		} else {
			return 1;
		}

	}

}