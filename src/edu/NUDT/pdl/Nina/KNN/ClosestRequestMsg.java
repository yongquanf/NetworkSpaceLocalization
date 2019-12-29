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

public class ClosestRequestMsg extends ObjMessage {
	static final long serialVersionUID = 41L;

	AddressIF OriginFrom; // original node
	AddressIF from;// last hop
	AddressIF target; // target node

	private int K; // target number of NN
	private final Set<NodesPair> NearestNeighborIndex; // <target, nearest node,
														// rtt>
	// private final List<AddressIF> NearestNeighborIndex; // <target, nearest
	// node, rtt>
	// private final List<Double> KNNLatency;

	public Coordinate targetCoordinates = null;

	public int operationsForOneNN = 0;

	public Set<NodesPair> getNearestNeighborIndex() {
		return NearestNeighborIndex;
	}

	private int version; // version of the KNN search process
	private final long seed;
	public AddressIF root; // the root node of the query process
	private List<AddressIF> HopRecords;

	// candidate KNNs
	// private SubSetManager filter;
	// forbidden KNNs
	// private SubSetManager ForbiddenFilter;
	Filter filter;
	Filter ForbiddenFilter;

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

	public ClosestRequestMsg(AddressIF _OriginFrom, AddressIF _from, AddressIF _target, int _K, int _version,
			long _seed, AddressIF _root) {
		setOriginFrom(_OriginFrom);
		setFrom(_from);
		setTarget(_target);
		setK(_K);
		setVersion(_version);
		seed = _seed;
		setRoot(_root);
		setHopRecords(new ArrayList<AddressIF>(1));
		NearestNeighborIndex = new HashSet<NodesPair>(1);
		// NearestNeighborIndex=new ArrayList<AddressIF>(5);
		// KNNLatency=new ArrayList<Double>(5);
		filter = null;
		ForbiddenFilter = null;
		// setFilter(null);
		// ForbiddenFilter = null;
	}

	public ClosestRequestMsg(AddressIF _OriginFrom, AddressIF _from, AddressIF _target, int _K,
			Set<NodesPair> _NearestNeighborIndex, int _version, long _seed, AddressIF _root, Filter _filter,
			Filter _ForbiddenFilter) {
		setOriginFrom(_OriginFrom);
		setFrom(_from);
		setTarget(_target);
		setK(_K);
		setVersion(_version);
		seed = _seed;
		setRoot(_root);
		setHopRecords(new ArrayList<AddressIF>(1));
		HopRecords.clear();
		NearestNeighborIndex = new HashSet<NodesPair>(5);
		if (_NearestNeighborIndex != null && !_NearestNeighborIndex.isEmpty()) {
			NearestNeighborIndex.addAll(_NearestNeighborIndex);
		}
		// NearestNeighborIndex=new ArrayList<AddressIF>(5);
		// KNNLatency=new ArrayList<Double>(5);
		// setFilter(_filter);
		// ForbiddenFilter = _ForbiddenFilter;
		filter = _filter;
		ForbiddenFilter = _ForbiddenFilter;

	}

	public static ClosestRequestMsg makeCopy(ClosestRequestMsg src) {
		ClosestRequestMsg msg = new ClosestRequestMsg(src.getOriginFrom(), src.getFrom(), src.getTarget(), src.getK(),
				src.getNearestNeighborIndex(), src.getVersion(), src.getSeed(), src.getRoot(), src.filter,
				src.ForbiddenFilter);

		if (src.targets != null && !src.targets.isEmpty()) {
			// targets
			msg.setTargetsForMultiObjKNN(src.targets);
		}
		if (src.HopRecords != null && !src.HopRecords.isEmpty()) {
			msg.HopRecords.addAll(src.HopRecords);
		}

		return msg;
	}

	public void addCurrentHop(AddressIF curNode) {
		if (curNode != null) {
			getHopRecords().add(curNode);
		}
	}

	public void addSubSetManager(Filter filter) {
		this.setFilter(filter);
	}

	public Filter getForbiddenFilter() {
		return ForbiddenFilter;
	}

	public void setForbiddenFilter(Filter forbiddenFilter) {
		ForbiddenFilter = forbiddenFilter;
	}

	public void setTarget(AddressIF target) {
		this.target = target;
	}

	public AddressIF getTarget() {
		return target;
	}

	public void setOriginFrom(AddressIF originFrom) {
		OriginFrom = originFrom;
	}

	public AddressIF getOriginFrom() {
		return OriginFrom;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public int getVersion() {
		return version;
	}

	public long getSeed() {
		return seed;
	}

	public void setFrom(AddressIF from) {
		this.from = from;
	}

	public AddressIF getFrom() {
		return from;
	}

	public void setRoot(AddressIF root) {
		this.root = root;
	}

	public AddressIF getRoot() {
		return root;
	}

	public void setFilter(Filter filter) {
		this.filter = filter;
	}

	public Filter getFilter() {
		return filter;
	}

	public void setK(int k) {
		K = k;
	}

	public int getK() {
		return K;
	}

	public void setHopRecords(List<AddressIF> hopRecords) {
		HopRecords = hopRecords;
	}

	public List<AddressIF> getHopRecords() {
		return HopRecords;
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
