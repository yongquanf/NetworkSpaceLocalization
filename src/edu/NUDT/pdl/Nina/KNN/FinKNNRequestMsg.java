package edu.NUDT.pdl.Nina.KNN;

import java.util.ArrayList;
import java.util.List;

import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class FinKNNRequestMsg extends ObjMessage {
	static final long serialVersionUID = 45L;

	public AddressIF from;// last hop
	public AddressIF target; // target node

	public final List<NodesPair> NearestNeighborIndex; // <target, nearest node,
														// rtt>
	public int version; // version of the KNN search process

	/**
	 * multiple objective KNNs
	 */
	public ArrayList<AddressIF> targets = null;

	public void setTargetsForMultiObjKNN(ArrayList<AddressIF> _targets) {
		targets = new ArrayList<AddressIF>(2);
		targets.addAll(_targets);
	}

	public FinKNNRequestMsg(AddressIF _from, AddressIF _target, List<NodesPair> _NearestNeighborIndex, int _version) {
		from = _from;
		target = _target;

		NearestNeighborIndex = _NearestNeighborIndex;
		version = _version;
	}

	public static FinKNNRequestMsg makeCopy(FinKNNRequestMsg src) {
		FinKNNRequestMsg msg = new FinKNNRequestMsg(src.from, src.target, src.NearestNeighborIndex, src.version);
		if (src.targets != null && !src.targets.isEmpty()) {
			msg.setTargetsForMultiObjKNN(src.targets);
		}
		return msg;
	}

}