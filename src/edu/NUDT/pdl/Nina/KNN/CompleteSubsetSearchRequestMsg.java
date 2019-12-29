package edu.NUDT.pdl.Nina.KNN;

import java.util.ArrayList;
import java.util.Set;

import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class CompleteSubsetSearchRequestMsg extends ObjMessage {
	static final long serialVersionUID = 33000000L;

	public AddressIF OriginFrom; // original node
	public AddressIF from;// last hop
	public AddressIF target; // target node

	public int K; // target number of NN
	private final Set<NodesPair> NearestNeighborIndex; // <target, nearest node,
														// rtt>
	// private final List<Double> KNNLatency;

	// rtt>
	public int version; // version of the KNN search process
	public final long seed;

	int direction = KNNManager.ClosestSearch; // 0, find k nearest nodes; 1,
												// find k farthest nodes

	public void setDirection(int _direction) {
		direction = _direction;
	}

	public int getDirection() {
		return direction;
	}

	/**
	 * multiple objective KNNs
	 */
	public ArrayList<AddressIF> targets = null;

	public void setTargetsForMultiObjKNN(ArrayList<AddressIF> _targets) {
		targets = _targets;
	}

	public CompleteSubsetSearchRequestMsg(AddressIF _OriginFrom, AddressIF _from, AddressIF _target, int _K,
			Set<NodesPair> _NearestNeighborIndex, int _version, long _seed) {
		OriginFrom = _OriginFrom;
		from = _from;
		target = _target;
		K = _K;
		NearestNeighborIndex = _NearestNeighborIndex;
		// KNNLatency=_KNNLatency;
		version = _version;
		seed = _seed;
	}

	public static CompleteSubsetSearchRequestMsg makeCopy(SubSetTargetLocatedRequestMsg src) {
		CompleteSubsetSearchRequestMsg msg = new CompleteSubsetSearchRequestMsg(src.OriginFrom, src.from, src.target,
				src.K, src.getNearestNeighborIndex(), src.version, src.seed);

		// target
		msg.setTargetsForMultiObjKNN(src.targets);
		return msg;
	}

	public static CompleteSubsetSearchRequestMsg makeCopy(SubSetClosestRequestMsg src) {
		CompleteSubsetSearchRequestMsg msg = new CompleteSubsetSearchRequestMsg(src.getOriginFrom(), src.getFrom(),
				src.getTarget(), src.getK(), src.getNearestNeighborIndex(), src.getVersion(), src.getSeed());

		msg.setTargetsForMultiObjKNN(src.targets);
		return msg;
	}

	public Set<NodesPair> getNearestNeighborIndex() {
		return NearestNeighborIndex;
	}
}