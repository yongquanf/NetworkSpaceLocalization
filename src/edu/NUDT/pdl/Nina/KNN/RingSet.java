package edu.NUDT.pdl.Nina.KNN;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import edu.NUDT.pdl.Nina.Clustering.ClusteringSet;
import edu.NUDT.pdl.Nina.Distance.ScalableBinning;
import edu.NUDT.pdl.Nina.StableNC.lib.Coordinate;
import edu.NUDT.pdl.Nina.StableNC.lib.NCClient;
import edu.NUDT.pdl.Nina.StableNC.lib.RemoteState;
import edu.NUDT.pdl.Nina.util.MathUtil;
import edu.harvard.syrah.prp.Log;
import edu.harvard.syrah.prp.SortedList;
import edu.harvard.syrah.sbon.async.Config;
import edu.harvard.syrah.sbon.async.comm.AddressIF;

public class RingSet<T> {

	static final Log log = new Log(RingSet.class);
	static int MAX_NUM_RINGS = 20;
	// scale down the ring
	public static int scalarFactor = Integer.parseInt(Config.getProperty("scalarFactor", "1"));;

	ArrayList<Vector<T>> primaryRing;// =new Vector<T>[MAX_NUM_RINGS];
	ArrayList<Vector<T>> secondaryRing;// [MAX_NUM_RINGS];
	boolean[] ringFrozen = new boolean[MAX_NUM_RINGS];

	// old: it is the upper limit of each ring;
	// now: it is the upper limit of each bin
	//
	public int primarySize;
	int secondarySize;
	int exponentBase;

	public volatile int CurrentNodes = 0;

	public volatile int CurrentNonEmptyRings = 0;

	// public Map<T, Integer> nodeLatencyUS;

	/**
	 * the number of non-empty rings
	 */
	public Map<T, Integer> NodesNumOfNonEmptyRing;

	/**
	 * coordinates
	 */
	public Map<T, Coordinate> NodesCoords;

	/**
	 * the measurement history
	 */
	public Map<T, RemoteState<T>> nodesCache;
	// Map<T,ClusteringSet> clusteringVec; //for clustering vector

	MathUtil math = new MathUtil(10);

	// private Lock lock=new ReentrantLock();
	/*
	 * private Lock lock2=new ReentrantLock(); private Lock lock3=new
	 * ReentrantLock(); private Lock lock4=new ReentrantLock(); private Lock
	 * lock5=new ReentrantLock(); private Lock lock6=new ReentrantLock();
	 */

	/**
	 * my coordinate for binning
	 */
	/*
	 * RelativeCoordinate<T> myCoordinate;
	 * 
	 *//**
		 * binning structure
		 *//*
		 * ScalableBinning<T> binningProc; int dim=15; //default landmark int
		 * cutoff=2; //default cutoff for separate binning int listThreshold=3;
		 */

	/**
	 * allowed nodes for each bin in each ring
	 */
	public int allowedThreshold = 5;

	// Coordinate myCoordinate;
	/**
	 * use the adaptive ring, by changing the nodes according to the upper limit
	 * of of bin
	 */
	public static boolean useAdaptiveRing = Boolean.parseBoolean(Config.getProperty("useAdaptiveRing", "false"));
	/**
	 * rho, the inframetric model
	 * 
	 * @return
	 */
	float RHO_INFRAMETRIC = 3f;
	int ChoiceOfNextHop = 3;

	/**
	 * 
	 */
	boolean useInformationalNodeSelection = true;
	int NonEmptyRingThreshold = 4;

	public Map<T, ClusteringSet> getClusteringVec() {
		return null;
		// return clusteringVec;
	}

	// Map<T, T> rendvMapping;
	private T MyAddress;
	private int currentNonemptyRing = 0;
	private int currentPrimaryNodes = 0;
	private boolean useMaxVolumeForNodeRemoval = true;

	public RingSet(T me, int prim_ring_size, int second_ring_size, int base) {
		MyAddress = me;
		primarySize = prim_ring_size;
		primaryRing = new ArrayList<Vector<T>>(MAX_NUM_RINGS);

		secondarySize = second_ring_size;
		secondaryRing = new ArrayList<Vector<T>>(MAX_NUM_RINGS);
		exponentBase = base;

		// rendvMapping = new ConcurrentHashMap<T, T>(10);
		// nodeLatencyUS = new ConcurrentHashMap<T, Integer>(50);
		NodesNumOfNonEmptyRing = new ConcurrentHashMap<T, Integer>(50);
		NodesCoords = new ConcurrentHashMap<T, Coordinate>(50);
		nodesCache = new ConcurrentHashMap<T, RemoteState<T>>(10);
		// clusteringVec=new ConcurrentHashMap<T,ClusteringSet>(10);

		for (int i = 0; i < MAX_NUM_RINGS; i++) {
			primaryRing.add(new Vector<T>(10));
			secondaryRing.add(new Vector<T>(10));
			ringFrozen[i] = false;
		}

	}

	/**
	 * nonEmptyRings
	 * 
	 * @param ringNode
	 * @param nonEmptyRings
	 */
	public void addNodesNumOfNonEmptyRing(T ringNode, int nonEmptyRings) {
		this.NodesNumOfNonEmptyRing.put(ringNode, nonEmptyRings);
	}

	// =============================================

	/**
	 * nonempty rings
	 */
	public int getCurrentNonEmptyRings() {
		return CurrentNonEmptyRings;
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

		RemoteState<T> it = this.nodesCache.get(inNode);
		if (it == null) {
			return -1;
		}
		// latencyUS[0] = it.intValue();
		latencyUS[0] = (int) Math.round(nodesCache.get(inNode).getSample());
		return 0;
	}

	/**
	 * determine a ring is full
	 * 
	 * @param ringNum
	 * @return
	 */
	boolean isPrimRingFull(int ringNum) {
		// assert (ringNum < MAX_NUM_RINGS);

		if (primaryRing.get(ringNum).size() >= this.getMaximumAllowedSize()) {
			return true;
		}
		return false;
	}

	private int getMaximumAllowedSize() {
		// TODO Auto-generated method stub
		return this.primarySize;
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
		/*
		 * T it = rendvMapping.get(remoteNode); if (it == null) { return -1; }
		 * rendvNode = it;
		 */
		return 0;
	}

	// ----------------------------------------------------
	// TODO: select rings that are ready for replacement
	// we do not consider nodes with firewalls currently
	public boolean eligibleForReplacement(int ringNum) {

		if (isPrimRingFull(ringNum)) {
			return true;
		} else {
			return false;
		}
		/*
		 * 
		 * 
		 * if (ringFrozen[ringNum]) { log.debug("Cannot update frozen ring\n");
		 * return false; } if (ringNum >= MAX_NUM_RINGS) { return false; }
		 * 
		 * 
		 * 
		 * if (isPrimRingFull(ringNum) && !isSecondRingEmpty(ringNum)) { T dummy
		 * = null; int numEligible = 0; for (int i = 0; i <
		 * primaryRing.get(ringNum).size(); i++) { if
		 * (rendvLookup((primaryRing.get(ringNum).get(i)), dummy) == -1) {
		 * numEligible++; // No rendavous } } for (int i = 0; i <
		 * secondaryRing.get(ringNum).size(); i++) { if
		 * (rendvLookup((secondaryRing.get(ringNum).get(i)), dummy) == -1) {
		 * numEligible++; // No rendavous } } if (numEligible > primarySize) {
		 * System.out .println("********** Eligible for replacement *********\n"
		 * ); return true; } System.out .println(
		 * "********** NOT Eligible for replacement *********\n"); // Equality
		 * case. We want to move all the non firewalled host to // the primary
		 * ring, as firewalled hosts are really secondary // citizens. TODO:
		 * This needs to be tested if (numEligible == primarySize) {
		 * System.out.println("********** Performing swap *********\n"); // Swap
		 * ineligble nodes from primary with eligible ones // in the secondary
		 * ring int j = 0; for (int i = 0; i < primaryRing.get(ringNum).size();
		 * i++) { if (rendvLookup((primaryRing.get(ringNum).get(i)), dummy) !=
		 * -1) { // Requires rendavous, find secondary that doesn't for (; j <
		 * secondaryRing.get(ringNum).size(); j++) { if (rendvLookup(
		 * (secondaryRing.get(ringNum).get(j)), dummy) != -1) { continue; //
		 * Requires rendavous, skip } // Swap primary and secondary T tmpIdentJ
		 * = secondaryRing.get(ringNum).get(j);
		 * this.secondaryRing.get(ringNum).set(j,
		 * this.secondaryRing.get(ringNum).get(i));
		 * this.secondaryRing.get(ringNum).set(i, tmpIdentJ); j++; // Can
		 * increment one more in the loop break; } } } } }
		 * 
		 * 
		 * return false;
		 */
	}

	public int getRingNumber(double latencyUS) {

		// ms
		double latencyMS = latencyUS;
		// scale down the ring
		latencyMS = (latencyMS + 0.0);

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

		RemoteState<T> findIt = this.nodesCache.get(inNode);
		if (findIt == null) {
			return -1; // Node does not exist
		}

		int ringNumber = getRingNumber(findIt.getSample());
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
		int indexRing = -1;

		for (int i = 0; i < primaryRing.get(ring).size(); i++) {
			T tmp = primaryRing.get(ring).get(i);
			if (tmp.equals(inNode)) {
				foundNode = true;
				primaryRing.get(ring).remove(i);
				this.currentPrimaryNodes--;
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

		// another nonempty rings
		if (primaryRing.get(ring) == null || primaryRing.get(ring).isEmpty()) {
			this.CurrentNonEmptyRings--;
		}

		if (foundNode) {
			CurrentNodes--;
		}
		//

		if (!foundNode) {

			// return -1; // If deleting a node that doesn't exist, return -1
			return 0;
		}
		RemovedOrReplacement(inNode, isRemovedOrReplacement);
		// nodeLatencyUS.remove(inNode); // Erase from latency map

		// rendvMapping.remove(inNode); // Erase from rendavous map

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
			// clusteringVec.remove(inNode);
			this.nodesCache.remove(inNode);
			this.NodesNumOfNonEmptyRing.remove(inNode);
			this.NodesCoords.remove(inNode);

		} else {
			// replace old measurements, do nothing
			// do not remove from nodesCache

		}
	}

	/**
	 * print out all nodes in primary rings
	 */
	public void printAllRings() {

		boolean open = false;
		if (open) {
			Iterator<Vector<T>> ier = this.primaryRing.iterator();
			int counter = 0;
			StringBuffer buf = new StringBuffer();
			while (ier.hasNext()) {
				Vector<T> tmp = ier.next();
				Iterator<T> ier2 = tmp.iterator();
				buf.append("Ring: " + counter + "\n");
				while (ier2.hasNext()) {
					T node = ier2.next();
					double lat = this.nodesCache.get(node).getSample();
					buf.append(node + "\t" + lat + "\n");
				}
				counter++;
			}
			log.info(buf.toString());
		}
	}

	public synchronized int insertNode(T inNode, int latencyUS, long curr_time, double hasOffset) {

		/*
		 * T rend = null; T findRend = rendvMapping.get(inNode); if (findRend !=
		 * null) { rend = findRend;
		 * 
		 * }
		 */

		if (!AbstractNNSearchManager.startMeridian) {
			/*
			 * AbstractNNSearchManager.lockRing2.lock(); try{
			 */
			return insertNode(inNode, latencyUS, null, hasOffset, curr_time);
			/*
			 * }finally{ AbstractNNSearchManager.lockRing2.unlock(); }
			 */
		} else {

			// Meridian procedure
			return insertNode_Meridian(inNode, latencyUS, null, hasOffset, curr_time);
		}
	}

	/**
	 * Meridian process
	 * 
	 * @param inNode
	 * @param latencyUS
	 * @param object
	 * @param hasOffset
	 * @param curr_time
	 * @return
	 */
	private synchronized int insertNode_Meridian(T inNode, int latencyUS, T rend, double hasOffset, long curr_time) {
		// Store/update rendavous information
		// Okay to update even if ring frozen

		RemoteState<T> findIt = this.nodesCache.get(inNode);

		// not cached the clustering indicator yet
		/*
		 * if(!clusteringVec.containsKey(inNode)){ clusteringVec.put(inNode, new
		 * ClusteringSet()); }
		 */

		// already in
		if (findIt != null) {

			// previous ring
			int prevRingNum = getRingNumber(findIt.getSample() + hasOffset);

			// add the new sample
			// avoid hugh rtt
			if (latencyUS > NCClient.OUTRAGEOUSLY_LARGE_RTT) {
				log.warn("skip hugh RTT!");
			} else {
				nodesCache.get(inNode).addSample(latencyUS, curr_time);
			}
			// use smooth latency
			int ringNum = getRingNumber(findIt.getSample() + hasOffset);

			if (prevRingNum == ringNum) {
				// If old and new ring is the same, just need to update latency
				// System.out.println("$: update a node: "+inNode+" @ "+
				// latencyUS);
				// nodeLatencyUS.put(inNode, Integer.valueOf(smoothLat));
				// nodesCache.get(inNode).addSample(latencyUS,curr_time);

				return 0;
			} else {
				// If old ring is frozen, just return
				if (ringFrozen[prevRingNum]) {
					System.out.println("Cannot update frozen ring\n");
					return 0;
				}
				log.info("$: has changed from: " + prevRingNum + " to " + ringNum);
				// If node has changed rings, remove node from old ring
				// but do not remove from nodesCache
				if (eraseNode(inNode, prevRingNum, false) == -1) {
					assert (false); // Logic error
				}

			}

		}
		T netAddr = inNode;

		// log.info("$: add a node: "+netAddr+" @"+ latencyUS);

		if (!nodesCache.containsKey(inNode)) {
			nodesCache.put(inNode, new RemoteState<T>(inNode));
			nodesCache.get(inNode).addSample(latencyUS, curr_time);
		}

		// find new ring number
		int ringNum = getRingNumber(nodesCache.get(inNode).getSample() + hasOffset);

		// another nonempty rings
		if (primaryRing.get(ringNum).isEmpty()) {
			CurrentNonEmptyRings++;
		}

		// if(!useAdaptiveRing) {
		// Push new node into rings
		if (primaryRing.get(ringNum).size() < primarySize) {
			primaryRing.get(ringNum).add(inNode);
			// System.out.println("$ inserted into: ring "+ringNum+" @latency:
			// "+smoothLat);
			CurrentNodes++;
		} else {
			if (secondaryRing.get(ringNum).size() >= secondarySize) {
				// Remove oldest member of secondary ring
				if (eraseNode(secondaryRing.get(ringNum).elementAt(0), ringNum, false) == -1) {
					// assert (false); // Logic error
				}
			} else {
				CurrentNodes++;
			}
			secondaryRing.get(ringNum).add(inNode);
		}

		// Store latency of new node
		// nodeLatencyUS.put(inNode, Integer.valueOf(smoothLat));

		/*
		 * }else{ //use the adaptive ring //you do not have the relative
		 * coordinate, so you have to insert them into the ring
		 * primaryRing.get(ringNum).add(inNode); // System.out.println(
		 * "$ inserted into: ring "+ringNum+" @latency: "+smoothLat);
		 * CurrentNodes++;
		 * 
		 * // Store latency of new node //nodeLatencyUS.put(inNode,
		 * Integer.valueOf(smoothLat));
		 * 
		 * }
		 */

		return 0;
	}

	/**
	 * Nina process
	 * 
	 * @param inNode
	 * @param latencyUS
	 * @param rend
	 * @param hasOffset
	 * @param curr_time
	 * @return
	 */
	public int insertNode(T inNode, int latencyUS, T rend, double hasOffset, long curr_time) {

		int ringNum = -1;
		// smooth latency by nodesCache
		// int smoothLat1 = (int)
		// Math.round(nodesCache.get(inNode).getSample());
		// offset
		// int smoothLat=latencyUS+ Math.round((float)hasOffset);
		//
		//
		// // use smooth latency
		// int ringNum = getRingNumber(smoothLat); // New ring number
		// if (ringFrozen[ringNum]) {
		// System.out.println("Cannot update frozen ring\n");
		// return 0;
		// }
		// Integer findIt = nodeLatencyUS.get(inNode);

		RemoteState<T> findIt = this.nodesCache.get(inNode);

		/*
		 * //not cached the clustering indicator yet
		 * if(clusteringVec!=null&&!clusteringVec.containsKey(inNode)){
		 * clusteringVec.put(inNode, new ClusteringSet()); }
		 */

		if (findIt != null) {

			// previous ring
			int prevRingNum = getRingNumber(findIt.getSample() + hasOffset);

			// add the new sample
			// avoid hugh rtt
			if (latencyUS > NCClient.OUTRAGEOUSLY_LARGE_RTT) {
				log.warn("skip hugh RTT!");
			} else {
				nodesCache.get(inNode).addSample(latencyUS, curr_time);
			}
			// use smooth latency
			ringNum = getRingNumber(findIt.getSample() + hasOffset);

			if (prevRingNum == ringNum) {
				// If old and new ring is the same, just need to update latency
				// System.out.println("$: update a node: "+inNode+" @ "+
				// latencyUS);
				// nodeLatencyUS.put(inNode, Integer.valueOf(smoothLat));
				// nodesCache.get(inNode).addSample(latencyUS,curr_time);

				return 0;
			} else {
				// If old ring is frozen, just return
				if (ringFrozen[prevRingNum]) {
					System.out.println("Cannot update frozen ring\n");
					return 0;
				}
				log.info("$: has changed from: " + prevRingNum + " to " + ringNum);
				// If node has changed rings, remove node from old ring
				// but do not remove from nodesCache
				if (eraseNode(inNode, prevRingNum, false) == -1) {
					assert (false); // Logic error
				}

			}
		}

		T netAddr = inNode;

		// System.out.println("$: add a node: "+netAddr+" @"+ latencyUS);
		if (!nodesCache.containsKey(inNode)) {
			nodesCache.put(inNode, new RemoteState<T>(inNode));
			nodesCache.get(inNode).addSample(latencyUS, curr_time);
		}

		// find new ring number
		ringNum = getRingNumber(nodesCache.get(inNode).getSample() + hasOffset);

		// another nonempty rings
		if (primaryRing.get(ringNum) != null && primaryRing.get(ringNum).isEmpty()) {
			CurrentNonEmptyRings++;
		}

		boolean remainInnode = true;
		boolean isFull = false;
		// Push new node into rings
		if (primaryRing.get(ringNum).size() < primarySize) {
			primaryRing.get(ringNum).add(inNode);
			currentPrimaryNodes++;
		} else {
			if (secondaryRing.get(ringNum) != null && secondaryRing.get(ringNum).size() >= secondarySize) {

				if (!useMaxVolumeForNodeRemoval) {
					// Remove oldest member of secondary ring
					if (eraseNode(secondaryRing.get(ringNum).elementAt(0), ringNum, false) == -1) {
						// assert (false); // Logic error
					}
					secondaryRing.get(ringNum).add(inNode);

				} else {

					// online optimization based on the network coordinate

					final Set<NodesPair> cachedM = new HashSet<NodesPair>(10);
					int len = primaryRing.get(ringNum).size() + secondaryRing.get(ringNum).size() + 1;
					// all nodes
					Vector<T> candidates = new Vector<T>(10);
					candidates.add(inNode);

					Iterator<T> ier = primaryRing.get(ringNum).iterator();

					while (ier.hasNext()) {
						candidates.add(ier.next());
					}
					ier = secondaryRing.get(ringNum).iterator();

					while (ier.hasNext()) {
						candidates.add(ier.next());
					}

					double[] latencyMatrix = new double[len * len];

					double rtt2 = -1;
					for (int i = 0; i < len; i++) {
						for (int j = 0; j < len; j++) {
							latencyMatrix[i * len + j] = 0;

							if (i != j) {
								// coordinate distance
								rtt2 = getCoordinateDistance((T) candidates.get(i), (T) candidates.get(j));

								if (rtt2 > 0) {

									cachedM.add(new NodesPair(candidates.get(i), candidates.get(j), rtt2));
								}
							}
						}
					}
					// -----------------------------------------
					Iterator<NodesPair> ier3 = cachedM.iterator();
					int from = 0, to = 0;
					while (ier3.hasNext()) {
						NodesPair tmp = ier3.next();
						from = candidates.indexOf(tmp.startNode);
						to = candidates.indexOf(tmp.endNode);
						// use offset
						latencyMatrix[from * len + to] = tmp.rtt;
					}
					// -----------------------------------------
					Vector<T> deletedNodes = new Vector<T>(10);

					reduceSetByN((Vector<T>) candidates, deletedNodes,
							candidates.size() - (primarySize + secondarySize), latencyMatrix);

					// remove a node here
					CurrentNodes -= deletedNodes.size();
					ier = deletedNodes.iterator();
					while (ier.hasNext()) {
						T tmp = ier.next();
						this.eraseNode(tmp);
					}
					primaryRing.get(ringNum).clear();
					primaryRing.get(ringNum).addAll(candidates.subList(0, primarySize));
					secondaryRing.get(ringNum).clear();
					secondaryRing.get(ringNum).addAll(candidates.subList(primarySize, primarySize + secondarySize));

					/**
					 * clear data structure
					 */
					deletedNodes.clear();
					cachedM.clear();
					candidates.clear();
					latencyMatrix = null;

					if (!candidates.contains(inNode)) {
						remainInnode = false;
					}
					isFull = true;
				}
			}
		}

		/**
		 * can not add it, since newly found nodes are replaced outside
		 */
		if (isFull && !remainInnode) {
			return 0;
		}

		// System.out.println("$ inserted into: ring "+ringNum+" @latency:
		// "+smoothLat);
		CurrentNodes++;

		// Store latency of new node
		// nodeLatencyUS.put(inNode, Integer.valueOf(smoothLat));

		return 0;
	}

	/**
	 * coordinate distance
	 * 
	 * @param t
	 * @param t2
	 * @return
	 */
	private double getCoordinateDistance(T t, T t2) {
		// TODO Auto-generated method stub
		if (!this.NodesCoords.containsKey(t) || !this.NodesCoords.containsKey(t2)) {
			return Double.MAX_VALUE;
		} else {
			return this.NodesCoords.get(t).distanceTo(this.NodesCoords.get(t2));

		}
	}

	// // Preserves existing rendavous mapping
	// public int insertNode(T inNode, int latencyUS, T rend,double
	// hasOffset,long curr_time) {
	// // Store/update rendavous information
	// // Okay to update even if ring frozen
	//
	// /*if (rend != null) {
	// rendvMapping.put(inNode, rend);
	// }*/
	//
	// /* // smooth latency by nodesCache
	// int smoothLat1 = (int) Math.round(nodesCache.get(inNode).getSample());
	// //offset
	// int smoothLat=smoothLat1+ Math.round((float)hasOffset);*/
	//
	/// *
	// if (ringFrozen[ringNum]) {
	// System.out.println("Cannot update frozen ring\n");
	// return 0;
	// }*/
	//
	// RemoteState<T> findIt = this.nodesCache.get(inNode);
	//
	// //not cached the clustering indicator yet
	// /* if(!clusteringVec.containsKey(inNode)){
	// clusteringVec.put(inNode, new ClusteringSet());
	// }*/
	//
	// //already in
	// if (findIt != null) {
	//
	//
	// // previous ring
	// int prevRingNum = getRingNumber(findIt.getSample()+hasOffset);
	//
	// //add the new sample
	// //avoid hugh rtt
	// if(latencyUS>NCClient.OUTRAGEOUSLY_LARGE_RTT){
	// log.warn("skip hugh RTT!");
	// }else{
	// nodesCache.get(inNode).addSample(latencyUS, curr_time);
	// }
	// // use smooth latency
	// int ringNum = getRingNumber(findIt.getSample()+hasOffset);
	//
	// if (prevRingNum == ringNum) {
	// // If old and new ring is the same, just need to update latency
	// // System.out.println("$: update a node: "+inNode+" @ "+
	// // latencyUS);
	// //nodeLatencyUS.put(inNode, Integer.valueOf(smoothLat));
	// // nodesCache.get(inNode).addSample(latencyUS,curr_time);
	//
	// return 0;
	// } else {
	// // If old ring is frozen, just return
	// if (ringFrozen[prevRingNum]) {
	// System.out.println("Cannot update frozen ring\n");
	// return 0;
	// }
	// log.info("$: has changed from: "+prevRingNum+" to "+ringNum);
	// // If node has changed rings, remove node from old ring
	// //but do not remove from nodesCache
	// if (eraseNode(inNode, prevRingNum,false) == -1) {
	// assert (false); // Logic error
	// }
	//
	// }
	//
	// }
	// T netAddr = inNode;
	//
	// //log.info("$: add a node: "+netAddr+" @"+ latencyUS);
	//
	//
	// if (!nodesCache.containsKey(inNode)) {
	// nodesCache.put(inNode, new RemoteState<T>(inNode));
	// nodesCache.get(inNode).addSample(latencyUS, curr_time);
	// }
	//
	// //find new ring number
	// int ringNum =
	// getRingNumber(nodesCache.get(inNode).getSample()+hasOffset);
	//
	// //another nonempty rings
	// if(primaryRing.get(ringNum).isEmpty()){
	// CurrentNonEmptyRings++;
	// }
	//
	//
	// if(!useAdaptiveRing) {
	// // Push new node into rings
	// if (primaryRing.get(ringNum).size() < this.primarySize) {
	// primaryRing.get(ringNum).add(inNode);
	// } else {
	// if (secondaryRing.get(ringNum).size() >= secondarySize) {
	// // Remove oldest member of secondary ring
	// if (eraseNode(secondaryRing.get(ringNum).elementAt(0), ringNum,false) ==
	// -1) {
	// //assert (false); // Logic error
	// }
	// }
	// secondaryRing.get(ringNum).add(inNode);
	// }
	//
	// // System.out.println("$ inserted into: ring "+ringNum+" @latency:
	// "+smoothLat);
	// CurrentNodes++;
	//
	// // Store latency of new node
	// //nodeLatencyUS.put(inNode, Integer.valueOf(smoothLat));
	//
	// }else{
	// // Push new node into rings
	// //get the size for the ring
	// if (primaryRing.get(ringNum).size() < getAdaptiveRingSize()) {
	// primaryRing.get(ringNum).add(inNode);
	// } else {
	// if (secondaryRing.get(ringNum).size() >= getAdaptiveRingSize()) {
	// // Remove oldest member of secondary ring
	// if (eraseNode(secondaryRing.get(ringNum).elementAt(0), ringNum,false) ==
	// -1) {
	// //assert (false); // Logic error
	// }
	// }
	// secondaryRing.get(ringNum).add(inNode);
	// }
	//
	// // System.out.println("$ inserted into: ring "+ringNum+" @latency:
	// "+smoothLat);
	// CurrentNodes++;
	//
	// }
	//
	// return 0;
	// }

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

		RemoteState<T> findIt = this.nodesCache.get(inNode);
		if (findIt == null) {
			return false;
		} else {
			return true;
		}

	}

	public int getRandomNodes(Vector<T> randNodes, int size) {

		Set<T> randomNodes = new HashSet<T>(5);
		int returnedSignal = this.getRandomNodes(randomNodes, size);
		randNodes.addAll(randomNodes);
		randomNodes.clear();
		return returnedSignal;
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

		nodes.addAll(this.nodesCache.keySet());

		/*
		 * // remove redundant nodes if (randNodes != null && randNodes.size() >
		 * 0) { Iterator<T> ier = randNodes.iterator(); while (ier.hasNext()) {
		 * T tmp = ier.next(); if (nodes.contains(tmp)) { nodes.remove(tmp); } }
		 * }
		 */

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
			nodes.clear();
			nodes = null;
			index.clear();
			index = null;
			return 1;
		}
		return 0;
	}

	/**
	 * find random nodes except forbidden nodes
	 * 
	 * @param randNodes
	 * @param forbidden
	 * @param size
	 * @return
	 */
	public int getRandomNodes(Set<T> randNodes, Set<T> forbidden, int size) {

		List<T> nodes = new ArrayList<T>(10);

		nodes.addAll(this.nodesCache.keySet());

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
			int ind = 0;
			while (ind < index.size()) {
				// found enough
				if (randNodes.size() == default_size) {
					break;
				} else {
					// not enough
					T id = nodes.get(index.get(ind).intValue());
					if (!forbidden.contains(id)) {
						randNodes.add(id);
					}
					ind++;
				}
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
	 * nonEmptyrings
	 * 
	 * @param randNodes
	 * @param size
	 * @return
	 */
	public int getNodesWithMaximumNonEmptyRings(Set<T> randNodes, int size) {

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

		SortedList<NodesPair> sortedL = new SortedList<NodesPair>(new NodesPairComp());

		Iterator<Entry<T, Integer>> ier = this.NodesNumOfNonEmptyRing.entrySet().iterator();
		while (ier.hasNext()) {
			Entry<T, Integer> tmp = ier.next();
			sortedL.add(new NodesPair<T>(null, tmp.getKey(), tmp.getValue().intValue()));
		}

		if (sortedL.size() > 0) {
			int default_size = size;

			if (sortedL.size() < default_size) {
				default_size = sortedL.size();
			}
			int starter = sortedL.size() - 1;
			for (; starter > sortedL.size() - 1 - default_size; starter--) {
				randNodes.add((T) sortedL.get(starter).endNode);
			}
			return 1;
		}

		sortedL.clear();
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
		/*
		 * if (betaRatio <= 0.0 || betaRatio >= 1.0) { log.warn(
		 * "Illegal Beta Ratio\n"); betaRatio = 0.5; // Set it to default beta }
		 */

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

		for (int i = lowerRing; i <= upperRing; i++) {

			// System.out.println("Ring "+i+"'s size is:
			// "+primaryRing.get(i).size());
			for (int j = 0; j < primaryRing.get(i).size(); j++) {

				int[] curLatencyUS = new int[1];

				if (getNodeLatency(primaryRing.get(i).get(j), curLatencyUS) == -1) {
					// missing items
					// assert (false); // This shouldn't happen
					continue;
				}

				// smaller
				if (curLatencyUS[0] < minAvgUS) {
					betas.add(Math.abs(minAvgUS - curLatencyUS[0]) / minAvgUS);
				}

				// System.out.println("$: Current Latency: "+curLatencyUS[0]+
				// "bounds <"+lowerBound+", "+upperBound+">");
				if (((curLatencyUS[0] + hasOffset) <= upperBound) && ((curLatencyUS[0] + hasOffset) >= lowerBound)) {
					// T rendvIdent=null;
					T tmpRendv;
					tmpRendv = primaryRing.get(i).get(j);
					/*
					 * if (rendvLookup(primaryRing.get(i).get(j), rendvIdent) !=
					 * -1) { //tmpRendv.portRendv = rendvIdent.port; tmpRendv =
					 * rendvIdent; }
					 */
					if (forbidden != null && tmpRendv.equals(forbidden)) {
						continue;
					} else {
						// ringMembers.insert(primaryRing[i][j]);
						// System.out.println("Found a candidate: "+tmpRendv+"
						// @latency: "+curLatencyUS[0]);
						ringMembers.add(tmpRendv);
					}
				}
			}

			if (!AbstractNNSearchManager.startMeridian) {

				for (int j = 0; j < this.secondaryRing.get(i).size(); j++) {

					int[] curLatencyUS = new int[1];

					if (getNodeLatency(this.secondaryRing.get(i).get(j), curLatencyUS) == -1) {
						// missing items
						// assert (false); // This shouldn't happen
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
						tmpRendv = this.secondaryRing.get(i).get(j);
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

		}

		/*
		 * if(ringMembers.contains(forbidden)){ // System.err.println(
		 * "Remove forbidden node: "+forbidden); ringMembers.remove(forbidden);
		 * }
		 */

		double newBeta = math.percentile(betas, percentile);
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

	/**
	 * 
	 * @param forbidden
	 * @param forbiddenNodes
	 * @param maxAvgUS
	 * @param betaRatio
	 * @param ringMembers
	 * @param hasOffset
	 */
	public void fillVector(T forbidden, Set<T> forbiddenNodes, double maxAvgUS, double betaRatio, Vector<T> ringMembers,
			double hasOffset) {

		// List<T> candidates=new ArrayList<T>(5);

		Vector<T> nodes = new Vector<T>(5);

		this.fillVector(forbidden, 0, RHO_INFRAMETRIC * maxAvgUS, 0, nodes, hasOffset);

		// candidates.addAll(nodes);

		// nodes in the ring
		Iterator<T> ier = nodes.iterator();
		while (ier.hasNext()) {

			T nodeRec = ier.next();
			if (forbiddenNodes != null && forbiddenNodes.contains(nodeRec)) {
				ier.remove();
			}
		}

		ringMembers.addAll(nodes);

		nodes.clear();
	}

	public void fillVector(T forbidden, Set<T> forbiddenNodes, double minAvgUS, double maxAvgUS, double betaRatio,
			Vector<T> ringMembers, double hasOffset) {

		// List<T> candidates=new ArrayList<T>(5);

		Vector<T> nodes = new Vector<T>(5);

		this.fillVector(forbidden, minAvgUS, maxAvgUS, betaRatio, nodes, hasOffset);

		// candidates.addAll(nodes);

		// nodes in the ring
		Iterator<T> ier = nodes.iterator();
		while (ier.hasNext()) {

			T nodeRec = ier.next();
			if (forbiddenNodes != null && forbiddenNodes.contains(nodeRec)) {
				ier.remove();
			}
		}

		ringMembers.addAll(nodes);

		nodes.clear();
	}

	// =====================================================
	public Set<T> getKFarthestPeerFromListWithCoord(T target, Coordinate targetCoord, Collection<T> ringMembers,
			int k) {

		// squeeze the candidates
		reduceCandidateBasedOnInformationGain(ringMembers);

		int realK = Math.min(ringMembers.size(), k);

		SortedList<NodesPair> ss = new SortedList<NodesPair>(new NodesPairComp());

		// SortedList<NodesPair> ssReal=new SortedList<NodesPair>(new
		// NodesPairComp());

		double rtt2 = 0;

		Iterator<T> ier = ringMembers.iterator();
		while (ier.hasNext()) {
			T next = ier.next();
			if (NodesCoords.containsKey(next)) {
				double dist = targetCoord.distanceTo(this.NodesCoords.get(next));
				ss.add(new NodesPair(next, target, dist));
			}
		}

		Set<T> farthestNodes = new HashSet<T>(2);
		Set<T> farthestNodesReal = new HashSet<T>(2);
		int N = ss.size();
		for (int i = 0; i < realK; i++) {
			T next = (T) ss.get(N - i - 1).startNode;
			// errorneous coordinate
			Coordinate coord = NodesCoords.get(next);
			if (coord.r_error > 0.7) {
				continue;
			}
			// tiv coordinate
			// coord.distanceTo()

			farthestNodes.add(next);
		}

		farthestNodesReal.clear();
		ss.clear();
		return farthestNodes;
	}

	/**
	 * squeeze the nodes such that nodes with too few non-empty rings are
	 * removed
	 * 
	 * @param ringMembers
	 */
	private void reduceCandidateBasedOnInformationGain(Collection<T> ringMembers) {
		if (ringMembers == null || ringMembers.isEmpty()) {
			log.warn("the ring member is empty!");
			return;
		} else {
			// TODO Auto-generated method stub
			if (useInformationalNodeSelection) {
				Iterator<T> ier = ringMembers.iterator();
				while (ier.hasNext()) {
					T rec = ier.next();
					/**
					 * avoid the unknowns nodes
					 */
					if (NodesNumOfNonEmptyRing.containsKey(rec) && NodesNumOfNonEmptyRing.get(rec) > 0
							&& NodesNumOfNonEmptyRing.get(rec) < NonEmptyRingThreshold) {
						// log.warn("too few ring, remove "+rec);
						ier.remove();
					}
				}
			}
		}
	}

	public Set<T> getKClosestPeerFromListWithCoord(T target, Coordinate targetCoord, Vector<T> ringMembers, int k) {

		reduceCandidateBasedOnInformationGain(ringMembers);

		if (ringMembers.size() <= k) {
			Set<T> closestNodes = new HashSet<T>(2);
			closestNodes.addAll(ringMembers);
			return closestNodes;
		} else {

			Set<T> farthestNodes = getKFarthestPeerFromListWithCoord(target, targetCoord, ringMembers,
					ringMembers.size() - k);
			Set<T> closestNodes = new HashSet<T>(2);
			closestNodes.addAll(ringMembers);
			closestNodes.removeAll(farthestNodes);

			// add additional nodes

			int realK = Math.min(ringMembers.size(), k);

			// log.info("KNN: "+stat.metric.coverage(target,closestNodes,
			// closestNodesReal));
			return closestNodes;
		}
	}

	// ======================================================
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

		// Add all existing nodes to tmpSet

		for (int i = 0; i < primaryRing.get(ringNum).size(); i++) {
			tmpSet.add(primaryRing.get(ringNum).get(i));
		}
		Iterator it = secondaryRing.get(ringNum).iterator();
		while (it.hasNext()) {
			tmpSet.add((T) it.next());
		}

		//
		if (false) {
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

	/**
	 * remove redundant nodes
	 * 
	 * @param nearestNeighborIndex
	 * @param target
	 * @param lat
	 * @param lat2
	 * @param betaRatio
	 * @param ringMembers
	 * @param offsetLatency
	 * @return
	 */
	public double fillVector(Set<NodesPair> nearestNeighborIndex, T forbidden, double minAvgUS, double maxAvgUS,
			double betaRatio, Vector<T> ringMembers, double hasOffset) {

		Set<T> foundKNNs = new HashSet<T>(5);
		// remove candidate nodes in the nearest nodes
		Iterator<NodesPair> ier = nearestNeighborIndex.iterator();
		while (ier.hasNext()) {
			NodesPair existedNodePair = ier.next();
			T remoteNode = (T) existedNodePair.startNode;
			foundKNNs.add(remoteNode);
		}

		// TODO Auto-generated method stub
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

		for (int i = lowerRing; i <= upperRing; i++) {

			log.debug("Ring " + i + "'s size is: " + primaryRing.get(i).size());
			for (int j = 0; j < primaryRing.get(i).size(); j++) {

				int[] curLatencyUS = new int[1];

				if (getNodeLatency(primaryRing.get(i).get(j), curLatencyUS) == -1) {
					// missing items
					// assert (false); // This shouldn't happen
					continue;
				}

				/*
				 * // smaller if (curLatencyUS[0] < minAvgUS) {
				 * betas.add(Math.abs(minAvgUS - curLatencyUS[0]) / minAvgUS); }
				 */

				// System.out.println("$: Current Latency: "+curLatencyUS[0]+
				// "bounds <"+lowerBound+", "+upperBound+">");
				if (((curLatencyUS[0] + hasOffset) <= upperBound) && ((curLatencyUS[0] + hasOffset) >= lowerBound)) {
					// T rendvIdent=null;
					T tmpRendv;
					tmpRendv = primaryRing.get(i).get(j);

					/*
					 * if (rendvLookup(primaryRing.get(i).get(j), rendvIdent) !=
					 * -1) { //tmpRendv.portRendv = rendvIdent.port; tmpRendv =
					 * rendvIdent; }
					 */
					if (forbidden != null && tmpRendv.equals(forbidden) || foundKNNs.contains(tmpRendv)) {
						continue;
					} else {
						// ringMembers.insert(primaryRing[i][j]);
						// System.out.println("Found a candidate: "+tmpRendv+"
						// @latency: "+curLatencyUS[0]);
						ringMembers.add(tmpRendv);
					}
				}
			}
		}

		/*
		 * if(ringMembers.contains(forbidden)){ // System.err.println(
		 * "Remove forbidden node: "+forbidden); ringMembers.remove(forbidden);
		 * }
		 */

		double newBeta = math.percentile(betas, percentile);
		// System.out.println("$: new betaRatio: "+betaRatio);
		if (newBeta > 1 || newBeta <= 0) {
			newBeta = betaRatio;
		}

		betas.clear();
		foundKNNs.clear();
		betas = null;
		// TODO: one beta for each ring
		// 9-29
		// keep
		newBeta = betaRatio;
		return weight * betaRatio + (1 - weight) * newBeta;

	}

	/**
	 * 
	 * @param nearestNeighborIndex
	 * @param randomSeeds
	 * @param defaultNodesForRandomContact
	 */
	public int getNodesWithMaximumNonEmptyRings(Set<NodesPair> nearestNeighborIndex, Collection<T> randNodes,
			int size) {
		// TODO Auto-generated method stub

		Set<T> foundKNNs = new HashSet<T>(5);
		// remove candidate nodes in the nearest nodes
		Iterator<NodesPair> ier2 = nearestNeighborIndex.iterator();
		while (ier2.hasNext()) {
			NodesPair<T> existedNodePair = (NodesPair<T>) ier2.next();
			T remoteNode = (T) existedNodePair.startNode;
			foundKNNs.add(remoteNode);
		}

		SortedList<NodesPair> sortedL = new SortedList<NodesPair>(new NodesPairComp());

		Iterator<Entry<T, Integer>> ier = this.NodesNumOfNonEmptyRing.entrySet().iterator();
		while (ier.hasNext()) {
			Entry<T, Integer> tmp = ier.next();
			if (foundKNNs.contains(tmp.getKey())) {
				continue;
			} else {
				sortedL.add(new NodesPair<T>(null, tmp.getKey(), tmp.getValue().intValue()));
			}
		}

		if (sortedL.size() > 0) {
			int default_size = size;

			if (sortedL.size() < default_size) {
				default_size = sortedL.size();
			}
			int starter = sortedL.size() - 1;
			for (; starter > sortedL.size() - 1 - default_size; starter--) {
				randNodes.add((T) sortedL.get(starter).endNode);
			}
			return 1;
		}

		sortedL.clear();
		foundKNNs.clear();
		return 0;

	}

	/*
	 * Used in diverse set formation, it reduces at set of nodes by N nodes,
	 * where the remaining nodes have the approximately highest hypervolume
	 */
	public double reduceSetByN(Vector<T> inVector, // Vector of nodes
			Vector<T> deletedNodes, int numReduction, // How many
			// nodes to
			// remove
			double[] latencyMatrix) // Pointer to latencyMatrix
	{
		int N = inVector.size(); // Dimension of matrix
		int colSize = N;
		int rowSize = N;
		double maxHyperVolume = 0.0;
		// Perform reductions iteratively
		for (int rCount = 0; rCount < numReduction; rCount++) {
			boolean maxHVNodeFound = false;
			T maxHVNode = null;
			double maxHV = 0.0;
			maxHyperVolume = 0.0; // Reset

			/*
			 * Iterate through the nodes
			 */
			for (int k = 0; k < inVector.size(); k++) {
				// if (anchorNodes != NULL &&
				// anchorNodes->find(*eIt) != anchorNodes->end()) {
				// continue; // We want to skip this anchor, as we can't
				// remove it
				// }
				// Swap out the current working column
				for (int i = 0; i < rowSize; i++) {
					double tmpValue = latencyMatrix[i * N + k];
					latencyMatrix[i * N + k] = latencyMatrix[i * N + colSize - 1];
					latencyMatrix[i * N + colSize - 1] = tmpValue;
				}
				colSize--;
				// And the corresponding row information
				// cblas_dswap(colSize,
				// &latencyMatrix[k * N], 1, &latencyMatrix[(rowSize-1) *
				// N], 1);
				dswap(colSize, latencyMatrix, k * N, latencyMatrix, (rowSize - 1) * N);
				rowSize--;
				assert (rowSize == colSize);
				// Calcuate the hypervolume without this node
				double hyperVolume = calculateHV(rowSize, latencyMatrix);
				/*
				 * See if it is the minimum so far Rationale: By removing this
				 * node, we still have the maxHV comparing to removing any other
				 * node. Therefore, we want to remove this node to keep a big HV
				 */
				if (hyperVolume >= maxHV) {
					maxHVNodeFound = true;
					maxHVNode = inVector.get(k);
					maxHV = hyperVolume;
				}
				// The max hypervolume at this reduction level
				if (hyperVolume > maxHyperVolume) {
					maxHyperVolume = hyperVolume;
				}
				// Undo row and column swap
				rowSize++;
				// cblas_dswap(colSize,
				// &latencyMatrix[k * N], 1, &latencyMatrix[(rowSize-1) *
				// N], 1);
				// dswap(colSize, &latencyMatrix[k * N],
				// &latencyMatrix[(rowSize-1) * N]);
				dswap(colSize, latencyMatrix, k * N, latencyMatrix, (rowSize - 1) * N);

				colSize++;
				for (int i = 0; i < rowSize; i++) {
					double tmpValue = latencyMatrix[i * N + k];
					latencyMatrix[i * N + k] = latencyMatrix[i * N + colSize - 1];
					latencyMatrix[i * N + colSize - 1] = tmpValue;
				}
			}
			if (maxHVNodeFound == false) {
				// Could not reduce any further, all anchors
				// assert (false); // This shouldn't really happen for any
				// valid case
				return 0.0;
			}
			// For the node that we have removed, remove it from the latency
			// matrix as well as from the vector of nodes
			for (int k = 0; k < inVector.size(); k++) {
				if (inVector.get(k).equals(maxHVNode)) {
					for (int i = 0; i < rowSize; i++) {
						double tmpValue = latencyMatrix[i * N + k];
						latencyMatrix[i * N + k] = latencyMatrix[i * N + colSize - 1];
						latencyMatrix[i * N + colSize - 1] = tmpValue;
					}
					colSize--;
					// cblas_dswap(colSize,
					// &latencyMatrix[k * N], 1, &latencyMatrix[(rowSize-1)
					// * N], 1);
					// dswap(colSize, &latencyMatrix[k * N],
					// &latencyMatrix[(rowSize-1) * N]);
					dswap(colSize, latencyMatrix, k * N, latencyMatrix, (rowSize - 1) * N);

					rowSize--;
					deletedNodes.add(inVector.get(k));
					inVector.remove(k);
				}
			}
		}
		return maxHyperVolume;
	}

	public static double calculateHV(int NPrime, // Size of the latencyMatrix in
													// use
			double[] latencyMatrix) // Pointer to latencyMatrix
	{
		double totalSum = 0.0;
		for (int i = 0; i < NPrime; i++) {
			for (int j = 0; j < NPrime; j++) {
				totalSum += latencyMatrix[i * NPrime + j];
			}
		}
		return totalSum;
	}

	public static void dswap(int N, double[] X, int Xstart, double[] Y, int Ystart) {
		double tmp_buf;
		for (int i = 0; i < N; i++) {
			tmp_buf = X[Xstart + i];
			X[Xstart + i] = Y[Ystart + i];
			Y[Ystart + i] = tmp_buf;
		}
	}

	public int getAllNodes() {
		// TODO Auto-generated method stub
		if (AbstractNNSearchManager.startMeridian) {
			return this.nodesCache.size();
		} else {
			return this.currentPrimaryNodes;
		}
	}

	public Set<T> getKClosestPeerFromListWithCoord(NCClient<T> primaryNC, T target, Coordinate targetCoordinates,
			Vector<T> ringMembers, int k) {
		// TODO Auto-generated method stub
		reduceCandidateBasedOnInformationGain(ringMembers);

		if (ringMembers.size() <= k) {
			Set<T> closestNodes = new HashSet<T>(2);
			closestNodes.addAll(ringMembers);
			return closestNodes;
		} else {

			Set<T> farthestNodes = getKFarthestPeerFromListWithCoord(primaryNC, target, targetCoordinates, ringMembers,
					ringMembers.size() - k);
			Set<T> closestNodes = new HashSet<T>(2);
			closestNodes.addAll(ringMembers);
			closestNodes.removeAll(farthestNodes);

			// add additional nodes

			int realK = Math.min(ringMembers.size(), k);

			// log.info("KNN: "+stat.metric.coverage(target,closestNodes,
			// closestNodesReal));
			return closestNodes;
		}
	}

	private Set<T> getKFarthestPeerFromListWithCoord(NCClient<T> primaryNC, T target, Coordinate targetCoord,
			Vector<T> ringMembers, int k) {
		// TODO Auto-generated method stub
		// squeeze the candidates
		reduceCandidateBasedOnInformationGain(ringMembers);

		int realK = Math.min(ringMembers.size(), k);

		SortedList<NodesPair> ss = new SortedList<NodesPair>(new NodesPairComp());

		// SortedList<NodesPair> ssReal=new SortedList<NodesPair>(new
		// NodesPairComp());

		double rtt2 = 0;

		Iterator<T> ier = ringMembers.iterator();
		while (ier.hasNext()) {
			T next = ier.next();
			if (NodesCoords.containsKey(next)) {
				double dist = targetCoord.distanceTo(this.NodesCoords.get(next));
				ss.add(new NodesPair(next, target, dist));
			}
		}

		Set<T> farthestNodes = new HashSet<T>(2);
		Set<T> farthestNodesReal = new HashSet<T>(2);
		int N = ss.size();
		for (int i = 0; i < realK; i++) {
			T next = (T) ss.get(N - i - 1).startNode;
			// errorneous coordinate
			Coordinate coord = NodesCoords.get(next);
			if (coord.r_error > 0.7) {
				continue;
			}
			// tiv coordinate
			double absError = Math
					.abs(coord.distanceTo(primaryNC.getSystemCoords()) - this.nodesCache.get(next).getSample());
			if (absError > 50) {
				continue;
			}

			farthestNodes.add(next);
		}

		farthestNodesReal.clear();
		ss.clear();
		return farthestNodes;
	}

}
