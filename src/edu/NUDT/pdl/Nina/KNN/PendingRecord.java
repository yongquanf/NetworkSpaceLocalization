package edu.NUDT.pdl.Nina.KNN;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import edu.NUDT.pdl.Nina.util.bloom.Apache.Filter;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB2;

public class PendingRecord<T> {
	boolean hasCalled;
	CB2<Set<NodesPair>, String> cbFunction;
	Set<NodesPair> nps;
	// String errorBuffer;
	volatile int PendingProbes;
	double timeStamp = -1;
	// Filter ForbiddenFilter;
	Set<T> allNodes;

	public PendingRecord(CB2<Set<NodesPair>, String> _cbFunction, int _PendingProbes) {
		nps = new HashSet<NodesPair>(5);
		cbFunction = _cbFunction;
		PendingProbes = _PendingProbes;
		hasCalled = false;
		allNodes = new HashSet<T>(2);
		// errorBuffer=_errorBuffer;
		// ForbiddenFilter=SubSetManager.getEmptyFilter();
	}

	/**
	 * add
	 * 
	 * @param nodes
	 */
	public void addAllNodes(Collection<T> nodes) {
		allNodes.addAll(nodes);
	}

	/**
	 * delete a node
	 * 
	 * @param node
	 */
	public void deleteNode(T node) {
		if (allNodes != null && node != null) {
			allNodes.remove(node);
		}

	}
}