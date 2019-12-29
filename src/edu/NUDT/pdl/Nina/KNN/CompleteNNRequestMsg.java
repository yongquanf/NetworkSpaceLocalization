package edu.NUDT.pdl.Nina.KNN;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.NUDT.pdl.Nina.StableNC.lib.Coordinate;
import edu.NUDT.pdl.Nina.util.bloom.Apache.Filter;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class CompleteNNRequestMsg extends ObjMessage {
	static final long serialVersionUID = 42L;

	public AddressIF OriginFrom; // original node
	public AddressIF from;// last hop
	public AddressIF target; // target node

	public int K; // target number of NN
	private final Set<NodesPair> NearestNeighborIndex; // <target, nearest node,
														// rtt>
	// private final List<Double> KNNLatency;

	int direction = 0; // 0, find k nearest nodes; 1, find k farthest nodes

	// private SubSetManager ForbiddenFilter;
	private Filter filter;
	private Filter ForbiddenFilter;

	Coordinate myCoord;

	public void setDirection(int _direction) {
		direction = _direction;
	}

	public int getDirection() {
		return direction;
	}

	// rtt>
	public int version; // version of the KNN search process
	public final long seed;
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

	public CompleteNNRequestMsg(AddressIF _OriginFrom, AddressIF _from, AddressIF _target, int _K,
			Set<NodesPair> _NearestNeighborIndex, int _version, long _seed) {
		OriginFrom = _OriginFrom;
		from = _from;
		target = _target;
		K = _K;
		NearestNeighborIndex = new HashSet<NodesPair>(2);
		NearestNeighborIndex.addAll(_NearestNeighborIndex);

		// KNNLatency=_KNNLatency;
		version = _version;
		seed = _seed;
	}

	public CompleteNNRequestMsg(AddressIF _OriginFrom, AddressIF _from, AddressIF _target, int _K,
			Set<NodesPair> _NearestNeighborIndex, int _version, long _seed, Filter _filter, Filter _ForbiddenFilter) {
		OriginFrom = _OriginFrom;
		from = _from;
		target = _target;
		K = _K;
		NearestNeighborIndex = new HashSet<NodesPair>(2);
		NearestNeighborIndex.addAll(_NearestNeighborIndex);

		// KNNLatency=_KNNLatency;
		version = _version;
		seed = _seed;
		this.filter = _filter;
		this.ForbiddenFilter = _ForbiddenFilter;
	}

	public static CompleteNNRequestMsg makeCopy(TargetLocatedRequestMsg src) {
		CompleteNNRequestMsg msg = new CompleteNNRequestMsg(src.OriginFrom, src.from, src.target, src.K,
				src.getNearestNeighborIndex(), src.version, src.seed);
		msg.totalSendingMessages = src.totalSendingMessages;
		msg.myCoord = src.targetCoordinates.makeCopy();

		if (src.targets != null && !src.targets.isEmpty()) {// targets
			msg.setTargetsForMultiObjKNN(src.targets);
		}
		msg.filter = src.filter;
		msg.ForbiddenFilter = src.ForbiddenFilter;
		return msg;
	}

	public static CompleteNNRequestMsg makeCopy(SubSetClosestRequestMsg src) {
		CompleteNNRequestMsg msg = new CompleteNNRequestMsg(src.OriginFrom, src.from, src.target, src.getK(),
				src.getNearestNeighborIndex(), src.getVersion(), src.getSeed());

		msg.setDirection(src.getDirection());
		if (src.targets != null && !src.targets.isEmpty()) {
			msg.setTargetsForMultiObjKNN(src.targets);
		}
		msg.filter = src.filter;
		msg.ForbiddenFilter = src.ForbiddenFilter;

		return msg;
	}

	public static CompleteNNRequestMsg makeCopy(ClosestRequestMsg src) {
		CompleteNNRequestMsg msg = new CompleteNNRequestMsg(src.getOriginFrom(), src.getFrom(), src.getTarget(),
				src.getK(), src.getNearestNeighborIndex(), src.getVersion(), src.getSeed());
		if (src.targets != null && !src.targets.isEmpty()) {
			// target
			msg.setTargetsForMultiObjKNN(src.targets);
		}
		msg.filter = src.filter;
		msg.ForbiddenFilter = src.ForbiddenFilter;

		msg.totalSendingMessages = src.totalSendingMessages;

		return msg;
	}

	/**
	 * _OriginFrom, AddressIF _from, AddressIF _target, int _K,Set
	 * <NodesPair> _NearestNeighborIndex, int _version, long _seed, AddressIF
	 * _root, Filter _filter, Filter _ForbiddenFilter
	 * 
	 * @param src
	 * @return
	 */

	public static ClosestRequestMsg makeCopy(CompleteNNRequestMsg src) {
		ClosestRequestMsg msg = new ClosestRequestMsg(src.OriginFrom, src.from, src.target, src.K,
				src.getNearestNeighborIndex(), src.version, src.seed, null, src.filter, src.ForbiddenFilter);
		if (src.targets != null && !src.targets.isEmpty()) {
			// target
			msg.setTargetsForMultiObjKNN(src.targets);
		}
		msg.hasRepeated = src.hasRepeated;

		msg.filter = src.filter;
		msg.ForbiddenFilter = src.ForbiddenFilter;

		msg.totalSendingMessages = src.totalSendingMessages;

		return msg;
	}

	public static CompleteNNRequestMsg makeCopy(MeridClosestRequestMsg src) {
		CompleteNNRequestMsg msg = new CompleteNNRequestMsg(src.getOriginFrom(), src.getFrom(), src.getTarget(),
				src.getK(), src.getNearestNeighborIndex(), src.getVersion(), src.getSeed());
		return msg;
	}

	public Set<NodesPair> getNearestNeighborIndex() {
		return NearestNeighborIndex;
	}

	public void clear() {
		// TODO Auto-generated method stub
		if (this.NearestNeighborIndex != null && !this.NearestNeighborIndex.isEmpty()) {
			this.NearestNeighborIndex.clear();
		}

		if (this.targets != null && !this.targets.isEmpty()) {
			this.targets.clear();
		}

	}
}
