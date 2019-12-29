package edu.NUDT.pdl.Nina.KNN;

import java.util.ArrayList;

import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class UpdateRequestMsg extends ObjMessage {
	static final long serialVersionUID = 3737373737L;
	public AddressIF from;
	public AddressIF target;
	public long TimerCache;

	UpdateRequestMsg(AddressIF _from, AddressIF _target, long _TimerCache) {
		from = _from;
		target = _target;
		TimerCache = _TimerCache;
	}

	public ArrayList<AddressIF> targets;

	public void setTargetsForMultiObjKNN(ArrayList<AddressIF> _targets) {
		targets = _targets;
	}

	public void clear() {
		// TODO Auto-generated method stub
		this.from = null;
		this.target = null;
	}
}
