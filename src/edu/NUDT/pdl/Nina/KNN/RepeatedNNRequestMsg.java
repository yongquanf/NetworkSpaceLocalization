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

public class RepeatedNNRequestMsg extends ObjMessage {
	static final long serialVersionUID = 49L;

	public AddressIF OriginFrom; // original node

	public AddressIF from;// current hop
	public AddressIF target;

	public int K; // target number of NN
	public Set<NodesPair> NearestNeighborIndex; // <target, nearest node, rtt>
	public int version; // version of the KNN search process
	public final long seed;
	public AddressIF root;
	public List<AddressIF> HopRecords;

	// candidate KNNs
	public Filter filter;
	// forbidden KNNs
	public Filter ForbiddenFilter;

	public Coordinate targetCoordinates = null;

	public int operationsForOneNN = 0;

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

	public RepeatedNNRequestMsg(AddressIF _OriginFrom, AddressIF _from, AddressIF _target, int _K,
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

	public RepeatedNNRequestMsg(AddressIF _OriginFrom, AddressIF _from, AddressIF _target, int _K,
			Set<NodesPair> _NearestNeighborIndex, int _version, long _seed, AddressIF _root, Filter _filter,
			Filter _ForbiddenFilter) {
		OriginFrom = _OriginFrom;
		from = _from;
		target = _target;
		K = _K;
		NearestNeighborIndex = new HashSet<NodesPair>(5);
		NearestNeighborIndex.addAll(_NearestNeighborIndex);
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