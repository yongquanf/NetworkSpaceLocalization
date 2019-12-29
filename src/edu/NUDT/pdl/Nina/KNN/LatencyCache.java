package edu.NUDT.pdl.Nina.KNN;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.NUDT.pdl.Nina.KNN.Commons.NodeIdent;

public class LatencyCache {
	// struct timeval, vector<NodeIdent>*
	Map<Long, List<NodeIdent>> timeoutMap;

	class pair<T1, T2> {
		public pair(T1 nextTimeOut, T2 latencyUS) {
			// TODO Auto-generated constructor stub
			timeval = nextTimeOut;
			uint32_t = latencyUS;
		}

		T1 timeval;
		T2 uint32_t;
	}

	Map<NodeIdent, pair<Long, Integer>> latencyMap;

	int maxSize;
	int periodUS;

	public LatencyCache(int in_maxSize, int in_periodUS) {

		maxSize = in_maxSize;
		periodUS = in_periodUS;
	}

	public int getLatency(final NodeIdent inNode, int[] latencyUS) {

		pair<Long, Integer> findIt = latencyMap.get(inNode);
		if (findIt != null) {
			// Get current normalized time
			long curTime = System.nanoTime();

			// QueryTable.normalizeTime(curTime);
			// Get timeout time of entry
			long s1 = findIt.timeval;
			if (s1 <= curTime) {
				// Timed out. Don't even bother to remove
				return -1;
			}
			latencyUS[0] = findIt.uint32_t.intValue();
			return 0;
		}
		return -1;
	}

	public int insertMeasurement(final NodeIdent inNode, int latencyUS) {

		if (maxSize == 0) {
			return 0;
		}
		// Remove existing measurements of this node
		if (eraseEntry(inNode) == -1) {
			return -1; // Error on removing node, just return
		}
		if (latencyMap.size() >= maxSize) {
			// Erase oldest entry. There should always be at least
			// one entry in a vector
			NodeIdent tmp = timeoutMap.get(timeoutMap.keySet().iterator().next()).get(0);
			eraseEntry(tmp);
		}
		// Get the next timeout
		long curTime, nextTimeOut;
		curTime = System.nanoTime();

		long offsetTV = Math.round(periodUS * Math.pow(10, 9));
		nextTimeOut = curTime + offsetTV;

		// See if the timeout is already there. If it is, add to the vector
		// If not, create a new vector

		List<NodeIdent> findIt = timeoutMap.get(Long.valueOf(nextTimeOut));
		if (findIt != null) {
			findIt.add(inNode);
		} else {
			List<NodeIdent> tmpVect = new ArrayList<NodeIdent>(10);
			tmpVect.add(inNode);
			timeoutMap.put(Long.valueOf(nextTimeOut), tmpVect);
		}
		// Add the new pair into the latencyMap
		latencyMap.put(inNode, new pair<Long, Integer>(nextTimeOut, latencyUS));
		return 0;
	}

	public int eraseEntry(final NodeIdent inNode) {

		pair<Long, Integer> findIt = latencyMap.get(inNode);
		if (findIt != null) {
			// Get the pair that contains the latency and timeout info
			pair<Long, Integer> curPair = findIt;
			// We can remove the entry from latencyMap now
			latencyMap.remove(findIt);
			// Get the corresponding vector from timeoutMap
			List<NodeIdent> findTO = timeoutMap.get(curPair.timeval);
			if (findTO == null) {
				System.err.println("Data structure in inconsistent state\n");
				assert (false);
			}

			List<NodeIdent> toVect = findTO;
			// Iterate through the vector to remove inNode
			for (int i = 0; i < toVect.size(); i++) {
				if (toVect.get(i).addr.equals(inNode.addr)) {
					toVect.remove(i);
					break;
				}
			}
			// If vector is now empty, remove the entry from timeoutMap
			// and also delete the vector
			if (toVect.isEmpty()) {
				timeoutMap.remove(findTO);
			}
		}
		return 0;
	}
}
