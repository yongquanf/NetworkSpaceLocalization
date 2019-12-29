package edu.NUDT.pdl.Nina.KNN;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import edu.NUDT.pdl.Nina.Ninaloader;
import edu.NUDT.pdl.Nina.Clustering.ClusteringSet;
import edu.NUDT.pdl.Nina.StableNC.lib.RemoteState;
import edu.harvard.syrah.prp.Log;

public class RingSet_noBin<T> {

	static final Log log = new Log(RingSet.class);
	static int MAX_NUM_RINGS = 20;

	ArrayList<Vector<T>> primaryRing;// =new Vector<T>[MAX_NUM_RINGS];
	ArrayList<Vector<T>> secondaryRing;// [MAX_NUM_RINGS];
	boolean[] ringFrozen = new boolean[MAX_NUM_RINGS];
	public int primarySize;
	int secondarySize;
	int exponentBase;

	public volatile int CurrentNodes = 0;
	public Map<T, Integer> nodeLatencyUS;
	Map<T, RemoteState<T>> nodesCache;
	Map<T, ClusteringSet> clusteringVec; // for clustering vector

	private Lock lock = new ReentrantLock();
	/*
	 * private Lock lock2=new ReentrantLock(); private Lock lock3=new
	 * ReentrantLock(); private Lock lock4=new ReentrantLock(); private Lock
	 * lock5=new ReentrantLock(); private Lock lock6=new ReentrantLock();
	 */

	public Map<T, ClusteringSet> getClusteringVec() {
		return clusteringVec;
	}

	Map<T, T> rendvMapping;

	public RingSet_noBin(int prim_ring_size, int second_ring_size, int base) {
		primarySize = prim_ring_size;
		primaryRing = new ArrayList<Vector<T>>(MAX_NUM_RINGS);

		secondarySize = second_ring_size;
		secondaryRing = new ArrayList<Vector<T>>(MAX_NUM_RINGS);
		exponentBase = base;

		rendvMapping = new ConcurrentHashMap<T, T>(10);
		nodeLatencyUS = new ConcurrentHashMap<T, Integer>(50);
		nodesCache = new ConcurrentHashMap<T, RemoteState<T>>(10);
		clusteringVec = new ConcurrentHashMap<T, ClusteringSet>(10);

		for (int i = 0; i < MAX_NUM_RINGS; i++) {
			primaryRing.add(new Vector<T>(10));
			secondaryRing.add(new Vector<T>(10));
			ringFrozen[i] = false;
		}
	}

	/**
	 * the ring number starts from 0,
	 * 
	 * @param ringNum
	 * @return
	 */
	public Vector<T> returnPrimaryRing(int ringNum) {
		if (ringNum >= MAX_NUM_RINGS) {
			return null;
		}
		return primaryRing.get(ringNum);
	}

	public int getNodeLatency(T inNode, int[] latencyUS) {

		Integer it = nodeLatencyUS.get(inNode);
		if (it == null) {
			return -1;
		}
		// latencyUS[0] = it.intValue();
		latencyUS[0] = (int) Math.round(nodesCache.get(inNode).getSample());
		return 0;
	}

	boolean isPrimRingFull(int ringNum) {
		assert (ringNum < MAX_NUM_RINGS);
		if (primaryRing.get(ringNum).size() == primarySize) {
			return true;
		}
		return false;
	}

	boolean isSecondRingEmpty(int ringNum) {
		assert (ringNum < MAX_NUM_RINGS);
		if (secondaryRing.get(ringNum).isEmpty()) {
			return true;
		}
		return false;
	}

	// TODO: rendvLookup
	int rendvLookup(T remoteNode, T rendvNode) {
		T it = rendvMapping.get(remoteNode);
		if (it == null) {
			return -1;
		}
		rendvNode = it;
		return 0;
	}

	// ----------------------------------------------------
	public boolean eligibleForReplacement(int ringNum) {
		if (ringFrozen[ringNum]) {
			log.debug("Cannot update frozen ring\n");
			return false;
		}
		if (ringNum >= MAX_NUM_RINGS) {
			return false;
		}

		lock.lock();
		try {

			if (isPrimRingFull(ringNum) && !isSecondRingEmpty(ringNum)) {
				T dummy = null;
				int numEligible = 0;
				for (int i = 0; i < primaryRing.get(ringNum).size(); i++) {
					if (rendvLookup((primaryRing.get(ringNum).get(i)), dummy) == -1) {
						numEligible++; // No rendavous
					}
				}
				for (int i = 0; i < secondaryRing.get(ringNum).size(); i++) {
					if (rendvLookup((secondaryRing.get(ringNum).get(i)), dummy) == -1) {
						numEligible++; // No rendavous
					}
				}
				if (numEligible > primarySize) {
					System.out.println("********** Eligible for replacement *********\n");
					return true;
				}
				System.out.println("********** NOT Eligible for replacement *********\n");
				// Equality case. We want to move all the non firewalled host to
				// the primary ring, as firewalled hosts are really secondary
				// citizens. TODO: This needs to be tested
				if (numEligible == primarySize) {
					System.out.println("********** Performing swap *********\n");
					// Swap ineligble nodes from primary with eligible ones
					// in the secondary ring
					int j = 0;
					for (int i = 0; i < primaryRing.get(ringNum).size(); i++) {
						if (rendvLookup((primaryRing.get(ringNum).get(i)), dummy) != -1) {
							// Requires rendavous, find secondary that doesn't
							for (; j < secondaryRing.get(ringNum).size(); j++) {
								if (rendvLookup((secondaryRing.get(ringNum).get(j)), dummy) != -1) {
									continue; // Requires rendavous, skip
								}
								// Swap primary and secondary
								T tmpIdentJ = secondaryRing.get(ringNum).get(j);
								this.secondaryRing.get(ringNum).set(j, this.secondaryRing.get(ringNum).get(i));
								this.secondaryRing.get(ringNum).set(i, tmpIdentJ);
								j++; // Can increment one more in the loop
								break;
							}
						}
					}
				}
			}

		} finally {
			lock.unlock();
		}

		return false;
	}

	public int getRingNumber(int latencyUS) {

		// ms
		double latencyMS = (latencyUS);
		// double latencyMS = (latencyUS / 1000.0);
		int ringNumber = (int) Math.ceil((Math.log(latencyMS) / Math.log(exponentBase)));
		// If node is really far away, put it in the maximum ring
		if (ringNumber >= MAX_NUM_RINGS) {
			ringNumber = MAX_NUM_RINGS - 1;
		} else if (ringNumber < 0) {
			ringNumber = 0;
		}
		return ringNumber;
	}

	public int eraseNode(T inNode) {

		Integer findIt = nodeLatencyUS.get(inNode);
		if (findIt == null) {
			return -1; // Node does not exist
		}

		int ringNumber = getRingNumber(findIt.intValue());
		if (eraseNode(inNode, ringNumber, true) == -1) {
			// assert (false); // Logic error
			return -1; // Node does not exist
		}
		return 0;
	}

	int eraseNode(T inNode, int ring, boolean isRemovedOrReplacement) {
		if (ringFrozen[ring]) {
			System.out.println("Cannot erase node from frozen ring\n");
			return 0;
		}
		boolean foundNode = false;

		lock.lock();
		try {

			for (int i = 0; i < primaryRing.get(ring).size(); i++) {
				T tmp = primaryRing.get(ring).get(i);
				if (tmp.equals(inNode)) {
					foundNode = true;
					primaryRing.get(ring).remove(i);
					// Pick a random node from secondary ring to insert
					if (primaryRing.get(ring).size() > 0) {
						Vector<T> tmp1 = secondaryRing.get(ring);
						if (tmp1 != null && !tmp1.isEmpty()) {
							primaryRing.get(ring).add(tmp1.firstElement());
							secondaryRing.get(ring).remove(0);
						}
					}
					break; // Found it, exit loop
				}
			}
			// If not in primary, must be in secondary
			if (!foundNode) {
				Iterator dIt = secondaryRing.get(ring).iterator();
				while (dIt.hasNext()) {
					T tmp = (T) dIt.next();
					if (tmp.equals(inNode)) {
						// Found it, erase and end
						secondaryRing.get(ring).remove(tmp);
						foundNode = true;
						break;
					}
				}
			}
			CurrentNodes--;

			//
		} finally {
			lock.unlock();
		}
		//

		if (!foundNode) {
			return -1; // If deleting a node that doesn't exist, return -1
		}
		RemovedOrReplacement(inNode, isRemovedOrReplacement);

		nodeLatencyUS.remove(inNode); // Erase from latency map
		// this.nodesCache.remove(inNode);
		rendvMapping.remove(inNode); // Erase from rendavous map

		return 0;
	}

	/**
	 * Remove a node
	 * 
	 * @param inNode
	 * @param isRemovedOrReplacement
	 */
	void RemovedOrReplacement(T inNode, boolean isRemovedOrReplacement) {
		// remove
		if (isRemovedOrReplacement) {
			// log.info("remove the clustering set! @: "+inNode);
			clusteringVec.remove(inNode);
		} else {
			// replace old measurements, do nothing
		}
	}

	public int insertNode(T inNode, int latencyUS, long curr_time, double hasOffset) {
		T rend = null;
		T findRend = rendvMapping.get(inNode);
		if (findRend != null) {
			rend = findRend;

		}
		if (nodesCache.containsKey(inNode)) {
			nodesCache.get(inNode).addSample(latencyUS, curr_time);
		} else {
			nodesCache.put(inNode, new RemoteState<T>(inNode));
			nodesCache.get(inNode).addSample(latencyUS, curr_time);
		}
		return insertNode(inNode, latencyUS, rend, hasOffset);
	}

	// Preserves existing rendavous mapping
	public int insertNode(T inNode, int latencyUS, T rend, double hasOffset) {
		// Store/update rendavous information
		// Okay to update even if ring frozen

		if (rend != null) {
			rendvMapping.put(inNode, rend);
		}

		// smooth latency by nodesCache
		int smoothLat1 = (int) Math.round(nodesCache.get(inNode).getSample());
		// offset
		int smoothLat = smoothLat1 + Math.round((float) hasOffset);

		// use smooth latency
		int ringNum = getRingNumber(smoothLat); // New ring number
		if (ringFrozen[ringNum]) {
			System.out.println("Cannot update frozen ring\n");
			return 0;
		}
		Integer findIt = nodeLatencyUS.get(inNode);

		// not cached the clustering indicator yet
		if (!clusteringVec.containsKey(inNode)) {
			clusteringVec.put(inNode, new ClusteringSet());
		}

		if (findIt != null) {

			// previous ring
			int prevRingNum = getRingNumber(findIt.intValue());

			if (prevRingNum == ringNum) {
				// If old and new ring is the same, just need to update latency
				// System.out.println("$: update a node: "+inNode+" @ "+
				// latencyUS);
				nodeLatencyUS.put(inNode, Integer.valueOf(smoothLat));
				// nodesCache.get(inNode).addSample(latencyUS,curr_time);

				return 0;
			} else {
				// If old ring is frozen, just return
				if (ringFrozen[prevRingNum]) {
					System.out.println("Cannot update frozen ring\n");
					return 0;
				}
				// System.out.println("$: has changed from: "+prevRingNum+" to
				// "+ringNum);
				// If node has changed rings, remove node from old ring
				if (eraseNode(inNode, prevRingNum, false) == -1) {
					assert (false); // Logic error
				}
			}
		}
		T netAddr = inNode;

		// System.out.println("$: add a node: "+netAddr+" @"+ latencyUS);

		lock.lock();
		try {

			// Push new node into rings
			if (primaryRing.get(ringNum).size() < primarySize) {
				primaryRing.get(ringNum).add(inNode);
			} else {
				if (secondaryRing.get(ringNum).size() >= secondarySize) {
					// Remove oldest member of secondary ring
					if (eraseNode(secondaryRing.get(ringNum).elementAt(0), ringNum, false) == -1) {
						// assert (false); // Logic error
					}
				}
				secondaryRing.get(ringNum).add(inNode);
			}

			// System.out.println("$ inserted into: ring "+ringNum+" @latency:
			// "+smoothLat);
			CurrentNodes++;

		} finally {
			lock.unlock();
		}

		// Store latency of new node
		nodeLatencyUS.put(inNode, Integer.valueOf(smoothLat));

		return 0;
	}

	/*
	 * public int getRandomNodes(Set<T> randNodes) { System.out.println(
	 * "$: Current total nodes" +CurrentNodes); for (int i = 0; i <
	 * MAX_NUM_RINGS; i++) { if (primaryRing.get(i).size() > 0) { T cur =
	 * primaryRing.get(i).get((int)Math.random() primaryRing.get(i).size()); T
	 * nodeToInsert; nodeToInsert=cur; T findRend = rendvMapping.get(cur); if
	 * (findRend !=null) { nodeToInsert =findRend; }
	 * randNodes.add(nodeToInsert); } } System.out.println(
	 * "$: Rand nodes from rings: "+randNodes.size()); return 0; }
	 */

	/**
	 * test on containing an element in the ring
	 * 
	 * @param inNode
	 * @return
	 */
	public boolean Contains(T inNode) {

		Integer findIt = nodeLatencyUS.get(inNode);
		if (findIt == null) {
			return false;
		} else {
			return true;
		}

	}

	/**
	 * find k random nodes from each ring
	 * 
	 * @param randNodes
	 * @return
	 */
	public int getRandomNodes(Set<T> randNodes, int size) {

		/*
		 * FillVectors(); Collections.shuffle(SelectVector); System.out.println(
		 * "$: Current total nodes" +SelectVector.size()); int default_size=10;
		 * if(SelectVector.size()>0){
		 * 
		 * //scale down if(SelectVector.size()<default_size){
		 * default_size=SelectVector.size(); } int counter=0;
		 * while(counter<default_size){ System.out.println("$: selected:"
		 * +SelectVector.get(counter));
		 * randNodes.add(SelectVector.get(counter)); counter++; }
		 * 
		 * 
		 * return 1; }
		 * 
		 * return 0;
		 */
		// System.out.println("$: Current total nodes" +nodeLatencyUS.size());
		List<T> nodes = new ArrayList<T>(10);

		nodes.addAll(nodeLatencyUS.keySet());

		// remove redundant nodes
		if (randNodes != null && randNodes.size() > 0) {
			Iterator<T> ier = randNodes.iterator();
			while (ier.hasNext()) {
				T tmp = ier.next();
				if (nodes.contains(tmp)) {
					nodes.remove(tmp);
				}
			}
		}

		if (nodes.size() > 0) {
			int default_size = size;

			if (nodes.size() < default_size) {
				default_size = nodes.size();
			}
			List<Integer> index = new ArrayList<Integer>(10);
			for (int i = 0; i < nodes.size(); i++) {
				index.add(Integer.valueOf(i));
			}
			Collections.shuffle(index);
			for (int i = 0; i < default_size; i++) {
				randNodes.add(nodes.get(index.get(i).intValue()));
			}

			// release memory
			// nodes.clear();
			nodes = null;
			index.clear();
			index = null;
			return 1;
		}
		return 0;
	}

	/**
	 * 
	 * @param forbidden
	 * @param minAvgUS
	 * @param maxAvgUS
	 * @param betaRatio
	 * @param ringMembers
	 * @return new beta ratio
	 */
	public double fillVector(T forbidden, double minAvgUS, double maxAvgUS, double betaRatio, Vector<T> ringMembers,
			double hasOffset) {
		// System.out.println("$: beta ratio: "+betaRatio);
		if (betaRatio <= 0.0 || betaRatio >= 1.0) {
			log.error("Illegal Beta Ratio\n");
			betaRatio = 0.5; // Set it to default beta
		}
		// int upperBound = (int) ceil(1.5 * avgLatencyUS);
		// int lowerBound = (int) floor(0.5 * avgLatencyUS);
		int upperBound = (int) Math.ceil((1.0 + betaRatio) * (maxAvgUS + Math.round((float) hasOffset)));
		int lowerBound = (int) Math.floor((1.0 - betaRatio) * (minAvgUS + Math.round((float) hasOffset)));
		if (lowerBound > upperBound) { // underflow or overflow error?
			// This shouldn't ever happen since the timeouts is much
			// smaller than the overflow values
			upperBound = Integer.MAX_VALUE;
			lowerBound = 0;
		}
		// System.out.println("#############################\n
		// lowerBound"+lowerBound+", upperBound:
		// "+upperBound+"\n#############################");
		// System.out.println("Total nodes in rings:
		// "+nodeLatencyUS.keySet().size());

		double weight = 0.9;
		double percentile = .5;
		Vector<Double> betas = new Vector<Double>(10);
		// TODO: weigh the beta value

		int upperRing = getRingNumber(upperBound);
		int lowerRing = getRingNumber(lowerBound);

		lock.lock();
		try {

			for (int i = lowerRing; i <= upperRing; i++) {

				// System.out.println("Ring "+i+"'s size is:
				// "+primaryRing.get(i).size());
				for (int j = 0; j < primaryRing.get(i).size(); j++) {

					int[] curLatencyUS = new int[1];
					if (getNodeLatency(primaryRing.get(i).get(j), curLatencyUS) == -1) {
						assert (false); // This shouldn't happen
						continue;
					}

					// smaller
					if (curLatencyUS[0] < minAvgUS) {
						betas.add(Math.abs(minAvgUS - curLatencyUS[0]) / minAvgUS);
					}

					// System.out.println("$: Current Latency:
					// "+curLatencyUS[0]+
					// "bounds <"+lowerBound+", "+upperBound+">");
					if (((curLatencyUS[0] + hasOffset) <= upperBound)
							&& ((curLatencyUS[0] + hasOffset) >= lowerBound)) {
						// T rendvIdent=null;
						T tmpRendv;
						tmpRendv = primaryRing.get(i).get(j);
						/*
						 * if (rendvLookup(primaryRing.get(i).get(j),
						 * rendvIdent) != -1) { //tmpRendv.portRendv =
						 * rendvIdent.port; tmpRendv = rendvIdent; }
						 */
						if (tmpRendv.equals(forbidden)) {
							continue;
						} else {
							// ringMembers.insert(primaryRing[i][j]);
							// System.out.println("Found a candidate:
							// "+tmpRendv+" @latency: "+curLatencyUS[0]);
							ringMembers.add(tmpRendv);
						}
					}
				}
			}

		} finally {
			lock.unlock();
		}

		/*
		 * if(ringMembers.contains(forbidden)){ // System.err.println(
		 * "Remove forbidden node: "+forbidden); ringMembers.remove(forbidden);
		 * }
		 */
		double newBeta = Ninaloader.math.percentile(betas, percentile);
		// System.out.println("$: new betaRatio: "+betaRatio);
		if (newBeta > 1 || newBeta <= 0) {
			newBeta = betaRatio;
		}
		betas.clear();
		betas = null;
		// TODO: one beta for each ring
		// 9-29
		// keep
		newBeta = betaRatio;
		return weight * betaRatio + (1 - weight) * newBeta;
	}

	int getNumberOfRings() {
		return MAX_NUM_RINGS;
	}

	int nodesInPrimaryRing() {
		return primarySize;
	}

	int nodesInSecondaryRing() {
		return secondarySize;
	}

	int setRingMembers(int ringNum, Vector<T> primRing, Vector<T> secondRing) {
		// Make sure ringNum is valid
		if (ringNum < 0 || ringNum >= getNumberOfRings()) {
			return -1;
		}
		// Make sure ring size is not validated
		if ((primRing.size() > nodesInPrimaryRing()) || (secondRing.size() > nodesInSecondaryRing())) {
			return -1;
		}
		ArrayList<T> tmpSet = new ArrayList<T>(12);

		lock.lock();
		try {

			// Add all existing nodes to tmpSet

			for (int i = 0; i < primaryRing.get(ringNum).size(); i++) {
				tmpSet.add(primaryRing.get(ringNum).get(i));
			}
			Iterator it = secondaryRing.get(ringNum).iterator();
			while (it.hasNext()) {
				tmpSet.add((T) it.next());
			}

		} finally {
			lock.unlock();
		}
		//
		// Make sure the old set and new set are the same size
		if ((primRing.size() + secondRing.size()) != tmpSet.size()) {
			return -1;
		}
		// Now make sure every node in primRing and secondRing are in tmpSet
		for (int i = 0; i < primRing.size(); i++) {
			if (tmpSet.contains(primRing.get(i)) == false) {
				return -1;
			}
		}
		for (int i = 0; i < secondRing.size(); i++) {
			if (tmpSet.contains(primRing.get(i)) == false) {
				return -1;
			}
		}

		// lock.lock();
		// try{

		// Clear old ring members and relocate them to new position
		primaryRing.get(ringNum).clear();
		secondaryRing.get(ringNum).clear();
		for (int i = 0; i < primRing.size(); i++) {
			primaryRing.get(ringNum).add(primRing.get(i));
		}
		for (int i = 0; i < secondRing.size(); i++) {
			secondaryRing.get(ringNum).add(secondRing.get(i));
		}

		// }finally{
		// lock.unlock();
		// }

		return 0;
	}

}
