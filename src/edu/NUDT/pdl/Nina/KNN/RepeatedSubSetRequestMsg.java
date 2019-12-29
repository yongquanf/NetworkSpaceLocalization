package edu.NUDT.pdl.Nina.KNN;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import edu.NUDT.pdl.Nina.StableNC.lib.Coordinate;
import edu.NUDT.pdl.Nina.util.bloom.Apache.Filter;
import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class RepeatedSubSetRequestMsg extends ObjMessage {
	static final long serialVersionUID = 49000L;

	AddressIF OriginFrom; // original node

	AddressIF from;// current hop
	AddressIF target;

	int K; // target number of NN
	Set<NodesPair> NearestNeighborIndex; // <target, nearest node, rtt>
	int version; // version of the KNN search process
	final long seed;
	AddressIF root;
	public List<AddressIF> HopRecords;

	int direction = KNNManager.ClosestSearch; // 0, find k nearest nodes; 1,
												// find k farthest nodes

	/**
	 * multiple objective KNNs
	 */
	public ArrayList<AddressIF> targets = null;

	public void setTargetsForMultiObjKNN(ArrayList<AddressIF> _targets) {
		targets = new ArrayList<AddressIF>(2);
		targets.addAll(_targets);
	}

	public void setDirection(int _direction) {
		direction = _direction;
	}

	public int getDirection() {
		return direction;
	}

	// candidate KNNs
	public Filter filter;
	// forbidden KNNs
	public Filter ForbiddenFilter;

	public Coordinate targetCoordinates = null;

	public RepeatedSubSetRequestMsg(AddressIF _OriginFrom, AddressIF _from, AddressIF _target, int _K,
			Set<NodesPair> _NearestNeighborIndex, int _version, long _seed, AddressIF _root) {
		OriginFrom = _OriginFrom;
		from = _from;
		target = _target;
		K = _K;
		NearestNeighborIndex = _NearestNeighborIndex;
		version = _version;
		seed = _seed;
		root = _root;
		HopRecords = new ArrayList<AddressIF>(1);
		filter = null;
		ForbiddenFilter = null;
	}

	public RepeatedSubSetRequestMsg(AddressIF _OriginFrom, AddressIF _from, AddressIF _target, int _K,
			Set<NodesPair> _NearestNeighborIndex, int _version, long _seed, AddressIF _root, Filter _filter,
			Filter _ForbiddenFilter) {
		OriginFrom = _OriginFrom;
		from = _from;
		target = _target;
		K = _K;
		NearestNeighborIndex = _NearestNeighborIndex;
		version = _version;
		seed = _seed;
		root = _root;
		HopRecords = new ArrayList<AddressIF>(1);
		filter = _filter;
		ForbiddenFilter = _ForbiddenFilter;
	}

	public void addCurrentHop(AddressIF curNode) {
		if (curNode != null) {
			HopRecords.add(curNode);
		}
	}

}
