package edu.NUDT.pdl.Nina.KNN;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import edu.harvard.syrah.prp.Log;
import edu.harvard.syrah.prp.SortedList;
import edu.harvard.syrah.sbon.async.comm.AddressIF;

public class KNNStatistics<T> {

	static Log log = new Log(KNNStatistics.class);

	public static double NotDefined = Double.NaN;
	public static double OKResult = 1;
	public static double BadResult = 0;

	/**
	 * whether contain closest nodes
	 * 
	 * @param foundKNNs
	 * @return -1 not definied, 0 no, 1 yes
	 */
	public static double containClosestNodes(Collection<NodesPair> foundKNNs) {
		if (foundKNNs == null || foundKNNs.isEmpty()) {
			return NotDefined;
		}
		Iterator<NodesPair> ier = foundKNNs.iterator();
		double threshold = 3;
		while (ier.hasNext()) {
			if (ier.next().rtt <= threshold) {
				return OKResult;
			} else {
				continue;
			}

		}
		return BadResult;
	}

	/**
	 * gains compared to k best nearest neighbors
	 * 
	 * @param foundKNNs
	 * @param worstKNNs
	 * @param bestKNNs
	 * @return
	 */
	public static double KNNGains(Collection<NodesPair> foundKNNs, SortedList<NodesPair> worstKNNs,
			SortedList<NodesPair> bestKNNs) {
		if (foundKNNs == null || worstKNNs == null || bestKNNs == null || worstKNNs.isEmpty() || foundKNNs.isEmpty()
				|| bestKNNs.isEmpty()) {
			return NotDefined;
		}
		/**
		 * minimum size
		 */
		int realSize = Math.min(foundKNNs.size(), worstKNNs.size());
		realSize = Math.min(realSize, bestKNNs.size());

		double rtt1;
		// found
		double sumFounded = 0;
		int count = 0;
		Iterator<NodesPair> ier = foundKNNs.iterator();
		while (count < realSize && ier.hasNext()) {
			rtt1 = ier.next().rtt;
			if (rtt1 < 0) {
				continue;
			} else {
				sumFounded += rtt1;
			}
			count++;
		}

		// worst
		double sumWorst = 0;
		for (count = 0; count < realSize; count++) {
			rtt1 = worstKNNs.get(worstKNNs.size() - count - 1).rtt;
			sumWorst += rtt1;
		}

		// best
		double sumBest = 0;
		for (count = 0; count < realSize; count++) {
			rtt1 = bestKNNs.get(count).rtt;
			sumBest += rtt1;
		}

		double d1 = Math.abs(sumBest - sumWorst);
		if (d1 == 0) {
			return NotDefined;
		} else {
			return Math.abs(sumFounded - sumWorst) / d1;
		}

	}

	/**
	 * absolute error with best results
	 * 
	 * @param foundKNNs
	 * @param bestKNNs
	 * @return
	 */
	public static double absoluteError(Collection<NodesPair> foundKNNs, SortedList<NodesPair> bestKNNs) {

		if (foundKNNs == null || bestKNNs == null || foundKNNs.isEmpty() || bestKNNs.isEmpty()) {
			return NotDefined;
		}
		/**
		 * minimum size
		 */
		int realSize = Math.min(foundKNNs.size(), bestKNNs.size());

		double rtt1;
		// found
		double sumFounded = 0;
		int count = 0;
		Iterator<NodesPair> ier = foundKNNs.iterator();
		while (count < realSize && ier.hasNext()) {
			rtt1 = ier.next().rtt;
			if (rtt1 < 0) {
				continue;
			} else {
				sumFounded += rtt1;
			}
			count++;
		}

		// best
		double sumBest = 0;
		for (count = 0; count < realSize; count++) {
			rtt1 = bestKNNs.get(count).rtt;
			sumBest += rtt1;
		}

		return (sumFounded - sumBest) / realSize;
	}

	/**
	 * relative error
	 * 
	 * @param foundKNNs
	 * @param bestKNNs
	 * @return
	 */
	public static double RelativeError(Collection<NodesPair> foundKNNs, SortedList<NodesPair> bestKNNs) {

		if (foundKNNs == null || bestKNNs == null || foundKNNs.isEmpty() || bestKNNs.isEmpty()) {
			return NotDefined;
		}
		/**
		 * minimum size
		 */
		int realSize = Math.min(foundKNNs.size(), bestKNNs.size());

		double rtt1;
		// found
		double sumFounded = 0;
		int count = 0;
		Iterator<NodesPair> ier = foundKNNs.iterator();
		while (count < realSize && ier.hasNext()) {
			rtt1 = ier.next().rtt;
			if (rtt1 < 0) {
				continue;
			} else {
				sumFounded += rtt1;
			}
			count++;
		}

		// best
		double sumBest = 0;
		for (count = 0; count < realSize; count++) {
			rtt1 = bestKNNs.get(count).rtt;
			sumBest += rtt1;
		}

		if (sumBest == 0) {
			return NotDefined;
		} else {
			return (sumFounded - sumBest) / sumBest;
		}
	}

	/**
	 * KNN gains
	 * 
	 * @param foundKNNs
	 * @param worstKNNs
	 * @param bestKNNs
	 * @return
	 */
	public static double KNNGains(List<NodesPair> foundKNNs, SortedList<NodesPair<AddressIF>> worstKNNs,
			SortedList<NodesPair<AddressIF>> bestKNNs) {
		// TODO Auto-generated method stub
		if (foundKNNs == null || worstKNNs == null || bestKNNs == null || worstKNNs.isEmpty() || foundKNNs.isEmpty()
				|| bestKNNs.isEmpty()) {
			log.warn("can not find knn gains");
			return NotDefined;
		}
		/**
		 * minimum size
		 */
		int realSize = Math.min(foundKNNs.size(), worstKNNs.size());
		realSize = Math.min(realSize, bestKNNs.size());

		double rtt1;
		// found
		double sumFounded = 0;
		int count = 0;
		Iterator<NodesPair> ier = foundKNNs.iterator();
		while (count < realSize && ier.hasNext()) {
			rtt1 = ier.next().rtt;
			if (rtt1 < 0) {
				continue;
			} else {
				sumFounded += rtt1;
			}
			count++;
		}

		// worst
		double sumWorst = 0;
		for (count = 0; count < realSize; count++) {
			rtt1 = worstKNNs.get(worstKNNs.size() - count - 1).rtt;
			sumWorst += rtt1;
		}

		// best
		double sumBest = 0;
		for (count = 0; count < realSize; count++) {
			rtt1 = bestKNNs.get(count).rtt;
			sumBest += rtt1;
		}

		double d1 = Math.abs(sumBest - sumWorst);
		if (d1 == 0) {
			return NotDefined;
		} else {
			return Math.abs(sumFounded - sumWorst) / d1;
		}

	}

	public static double absoluteError(List<NodesPair> foundKNNs, SortedList<NodesPair<AddressIF>> bestKNNs) {
		// TODO Auto-generated method stub

		if (foundKNNs == null || bestKNNs == null || foundKNNs.isEmpty() || bestKNNs.isEmpty()) {
			return NotDefined;
		}
		/**
		 * minimum size
		 */
		int realSize = Math.min(foundKNNs.size(), bestKNNs.size());

		double rtt1;
		// found
		double sumFounded = 0;
		int count = 0;
		Iterator<NodesPair> ier = foundKNNs.iterator();
		while (count < realSize && ier.hasNext()) {
			rtt1 = ier.next().rtt;
			if (rtt1 < 0) {
				continue;
			} else {
				sumFounded += rtt1;
			}
			count++;
		}

		// best
		double sumBest = 0;
		for (count = 0; count < realSize; count++) {
			rtt1 = bestKNNs.get(count).rtt;
			if (rtt1 < 0) {
				continue;
			}
			sumBest += rtt1;
		}

		return (sumFounded - sumBest) / realSize;
	}

	public static double RelativeError(List<NodesPair> foundKNNs, SortedList<NodesPair<AddressIF>> bestKNNs) {
		// TODO Auto-generated method stub
		if (foundKNNs == null || bestKNNs == null || foundKNNs.isEmpty() || bestKNNs.isEmpty()) {
			return NotDefined;
		}
		/**
		 * minimum size
		 */
		int realSize = Math.min(foundKNNs.size(), bestKNNs.size());

		double rtt1;
		// found
		double sumFounded = 0;
		int count = 0;
		Iterator<NodesPair> ier = foundKNNs.iterator();
		while (count < realSize && ier.hasNext()) {
			rtt1 = ier.next().rtt;
			if (rtt1 < 0) {
				continue;
			} else {
				sumFounded += rtt1;
			}
			count++;
		}

		// best
		double sumBest = 0;
		for (count = 0; count < realSize; count++) {
			rtt1 = bestKNNs.get(count).rtt;
			if (rtt1 < 0) {
				continue;
			}
			sumBest += rtt1;
		}

		if (sumBest == 0) {
			return NotDefined;
		} else {
			return (sumFounded - sumBest) / sumBest;
		}
	}

	/**
	 * coverage rate
	 * 
	 * @param arg1
	 * @param currentBestNearestNeighbors
	 * @return
	 */
	public static double coverage(List<NodesPair> arg1, SortedList<NodesPair<AddressIF>> currentBestNearestNeighbors) {
		// TODO Auto-generated method stub

		HashSet<AddressIF> found = new HashSet<AddressIF>(2);
		HashSet<AddressIF> real = new HashSet<AddressIF>(2);

		int realSize = Math.min(arg1.size(), currentBestNearestNeighbors.size());

		int counter = 0;
		Iterator<NodesPair> ier = arg1.iterator();
		while (counter < realSize && ier.hasNext()) {
			NodesPair tmp = ier.next();
			AddressIF node = (AddressIF) tmp.startNode;
			found.add(node);
			counter++;
		}

		counter = 0;

		Iterator<NodesPair<AddressIF>> ier1 = currentBestNearestNeighbors.iterator();
		while (counter < realSize && ier1.hasNext()) {
			NodesPair tmp = ier1.next();
			AddressIF node = (AddressIF) tmp.startNode;
			real.add(node);
			counter++;
		}

		int foundNodes = 0;
		int totalNodes = 0;
		Iterator<AddressIF> ier2 = found.iterator();
		while (ier2.hasNext()) {
			AddressIF tmp = ier2.next();
			if (real.contains(tmp)) {
				foundNodes++;
			}
			totalNodes++;
		}

		found.clear();
		real.clear();
		return foundNodes / (totalNodes + 0.0);
	}

}
