package edu.NUDT.pdl.Nina.KNN;

import java.util.ArrayList;

import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class QueryKNNRequestMsg extends ObjMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 48L;

	AddressIF from;
	AddressIF target;
	int K;
	int version;

	/**
	 * multiple objective KNNs
	 */
	public ArrayList<AddressIF> targets = null;

	public void setTargetsForMultiObjKNN(ArrayList<AddressIF> _targets) {
		targets = new ArrayList<AddressIF>(2);
		targets.addAll(_targets);
	}

	public QueryKNNRequestMsg(AddressIF _from, AddressIF _target, int _K, int _version) {
		from = _from;
		target = _target;
		K = _K;
		version = _version;
	}

}
