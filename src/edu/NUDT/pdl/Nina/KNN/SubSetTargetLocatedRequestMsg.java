package edu.NUDT.pdl.Nina.KNN;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import edu.NUDT.pdl.Nina.StableNC.lib.Coordinate;
import edu.NUDT.pdl.Nina.util.bloom.Apache.Filter;
import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class SubSetTargetLocatedRequestMsg extends ObjMessage {
	static final long serialVersionUID = 39000L;

	AddressIF from;
	AddressIF OriginFrom; // the query node
	AddressIF target;
	int K; // target number of NN
	private final Set<NodesPair> NearestNeighborIndex; // <target, nearest node,
														// rtt>
	// private final List<AddressIF> NearestNeighborIndex; // <target, nearest
	// node, rtt>
	// private final List<Double> KNNLatency;

	public Coordinate targetCoordinates = null;

	int direction = KNNManager.ClosestSearch; // 0, find k nearest nodes; 1,
												// find k farthest nodes

	/**
	 * multiple objective KNNs
	 */
	public ArrayList<AddressIF> targets;

	public void setTargetsForMultiObjKNN(ArrayList<AddressIF> _targets) {
		targets = _targets;
	}

	public void setDirection(int _direction) {
		direction = _direction;
	}

	public int getDirection() {
		return direction;
	}

	public Set<NodesPair> getNearestNeighborIndex() {
		return NearestNeighborIndex;
	}

	int version;
	final long seed;
	final AddressIF root;
	public List<AddressIF> HopRecords;

	// candidate KNNs
	public Filter filter;
	// forbidden KNNs
	public Filter ForbiddenFilter;

	public SubSetTargetLocatedRequestMsg(AddressIF _from, AddressIF _OriginFrom, AddressIF _target, int _K,
			Set<NodesPair> _nodes, int _version, long _seed, AddressIF _root) {
		from = _from;
		K = _K;
		NearestNeighborIndex = _nodes;
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

	public SubSetTargetLocatedRequestMsg(AddressIF _from, AddressIF _OriginFrom, AddressIF _target, int _K,
			Set<NodesPair> _nodes, int _version, long _seed, AddressIF _root, Filter _filter, Filter _ForbiddenFilter) {
		from = _from;
		K = _K;
		NearestNeighborIndex = _nodes;
		// KNNLatency=_KNNLatency;
		version = _version;
		OriginFrom = _OriginFrom;
		target = _target;
		seed = _seed;
		root = _root;
		HopRecords = new ArrayList<AddressIF>(2);
		filter = _filter;
		ForbiddenFilter = _ForbiddenFilter;
	}

	public void addCurrentHop(AddressIF curNode) {
		if (curNode != null) {
			HopRecords.add(curNode);
		}
	}
}