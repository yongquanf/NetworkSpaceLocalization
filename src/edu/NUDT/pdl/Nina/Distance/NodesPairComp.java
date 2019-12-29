package edu.NUDT.pdl.Nina.Distance;

import java.io.Serializable;
import java.util.Comparator;

import edu.NUDT.pdl.Nina.KNN.NodesPair;


public class NodesPairComp<T> implements Comparator<T>, Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2253718799647275600L;

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