package edu.NUDT.pdl.Nina.KNN;

import java.util.Set;

import edu.NUDT.pdl.Nina.util.bloom.Apache.Filter;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class CompleteMeridianRequestMsg extends ObjMessage {
	static final long serialVersionUID = 3300L;

	public AddressIF OriginFrom; // original node
	public AddressIF from;// last hop
	public AddressIF target; // target node

	public int K; // target number of NN
	private final Set<NodesPair> NearestNeighborIndex; // <target, nearest node,
														// rtt>
	// private final List<Double> KNNLatency;

	public int totalSendingMessage = 0;
	public int version; // version of the KNN search process
	public long seed = -1;

	public int hasRepeated = 0;

	Filter ForbiddenFilter;

	public CompleteMeridianRequestMsg(AddressIF _OriginFrom, AddressIF _from, AddressIF _target, int _K,
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

	public static CompleteMeridianRequestMsg makeCopy(TargetLocatedRequestMsg src) {
		CompleteMeridianRequestMsg msg = new CompleteMeridianRequestMsg(src.OriginFrom, src.from, src.target, src.K,
				src.getNearestNeighborIndex(), src.version, src.seed);
		return msg;
	}

	public static CompleteMeridianRequestMsg makeCopy(ClosestRequestMsg src) {
		CompleteMeridianRequestMsg msg = new CompleteMeridianRequestMsg(src.getOriginFrom(), src.getFrom(),
				src.getTarget(), src.getK(), src.getNearestNeighborIndex(), src.getVersion(), src.getSeed());
		return msg;
	}

	public static CompleteMeridianRequestMsg makeCopy(MeridClosestRequestMsg src) {
		CompleteMeridianRequestMsg msg = new CompleteMeridianRequestMsg(src.getOriginFrom(), src.getFrom(),
				src.getTarget(), src.getK(), src.getNearestNeighborIndex(), src.getVersion(), src.getSeed());
		msg.ForbiddenFilter = src.getForbiddenFilter();
		return msg;
	}

	public Set<NodesPair> getNearestNeighborIndex() {
		return NearestNeighborIndex;
	}
}
