package edu.NUDT.pdl.Nina.KNN;

import java.util.ArrayList;

import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class WithdrawRequestMsg extends ObjMessage {
	static final long serialVersionUID = 36L;
	AddressIF OriginFrom; // original node
	public AddressIF from;// last hop
	AddressIF target; // target node
	int version;
	final int seed;

	public ArrayList<AddressIF> targets = null;

	public void setTargetsForMultiObjKNN(ArrayList<AddressIF> _targets) {
		targets = new ArrayList<AddressIF>(2);
		targets.addAll(_targets);
	}

	public WithdrawRequestMsg(AddressIF _from, AddressIF _OriginFrom, AddressIF _target, int _version, int _seed) {

		from = _from;
		OriginFrom = _OriginFrom;
		target = _target;
		version = _version;
		seed = _seed;
	}

}