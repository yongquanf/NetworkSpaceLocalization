package edu.NUDT.pdl.Nina.KNN;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.NUDT.pdl.Nina.StableNC.lib.Coordinate;
import edu.NUDT.pdl.Nina.util.bloom.Apache.Filter;
import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class TargetLocatedRequestMsg extends ObjMessage {
	static final long serialVersionUID = 39L;

	public AddressIF from;
	public AddressIF OriginFrom; // the query node
	public AddressIF target;
	public int K; // target number of NN
	public final Set<NodesPair> NearestNeighborIndex; // <target, nearest node,
														// rtt>
	// private final List<AddressIF> NearestNeighborIndex; // <target, nearest
	// node, rtt>
	// private final List<Double> KNNLatency;

	public Coordinate targetCoordinates = null;

	public Set<NodesPair> getNearestNeighborIndex() {
		return NearestNeighborIndex;
	}

	public int operationsForOneNN = 0;

	public int version;
	public final long seed;
	public final AddressIF root;
	public List<AddressIF> HopRecords;

	// candidate KNNs
	public Filter filter;
	// forbidden KNNs
	public Filter ForbiddenFilter;

	public int hasRepeated = 0;
	/**
	 * multiple objective KNNs
	 */
	public ArrayList<AddressIF> targets = null;

	public void setTargetsForMultiObjKNN(ArrayList<AddressIF> _targets) {
		targets = new ArrayList<AddressIF>(2);
		targets.addAll(_targets);
	}

	/**
	 * record the total sending messages
	 */
	public int totalSendingMessages = 0;

	public void increaseMessage(int count) {
		totalSendingMessages += count;
	}

	public TargetLocatedRequestMsg(AddressIF _from, AddressIF _OriginFrom, AddressIF _target, int _K,
			Set<NodesPair> _nodes, int _version, long _seed, AddressIF _root) {
		from = _from;
		K = _K;
		NearestNeighborIndex = new HashSet<NodesPair>(2);
		NearestNeighborIndex.addAll(_nodes);
		// KNNLatency=_KNNLatency;
		version = _version;
		OriginFrom = _OriginFrom;
		target = _target;
		seed = _seed;
		root = _root;
		HopRecords = new ArrayList<AddressIF>(1);
		filter = null;
		ForbiddenFilter = null;
	}

	public TargetLocatedRequestMsg(AddressIF _from, AddressIF _OriginFrom, AddressIF _target, int _K,
			Set<NodesPair> _nodes, int _version, long _seed, AddressIF _root, Filter _filter, Filter _ForbiddenFilter) {
		from = _from;
		K = _K;
		NearestNeighborIndex = new HashSet<NodesPair>(2);
		NearestNeighborIndex.addAll(_nodes);
		// KNNLatency=_KNNLatency;
		version = _version;
		OriginFrom = _OriginFrom;
		target = _target;
		seed = _seed;
		root = _root;
		HopRecords = new ArrayList<AddressIF>(2);
		HopRecords.clear();
		filter = _filter;
		ForbiddenFilter = _ForbiddenFilter;
	}

	public void addCurrentHop(AddressIF curNode) {
		if (curNode != null) {
			HopRecords.add(curNode);
		}
	}

	public void clear() {
		// TODO Auto-generated method stub
		if (this.NearestNeighborIndex != null && !this.NearestNeighborIndex.isEmpty()) {
			this.NearestNeighborIndex.clear();
		}
		if (this.HopRecords != null && !this.HopRecords.isEmpty()) {
			this.HopRecords.clear();
		}
		if (this.targets != null && !this.targets.isEmpty()) {
			this.targets.clear();
		}
		if (this.targetCoordinates != null && this.targetCoordinates.coords != null) {
			this.targetCoordinates.clear();
		}
	}
}
