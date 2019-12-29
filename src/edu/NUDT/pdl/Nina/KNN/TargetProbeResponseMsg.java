package edu.NUDT.pdl.Nina.KNN;

import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class TargetProbeResponseMsg extends ObjMessage {
	static final long serialVersionUID = 38L;
	AddressIF from;

	public TargetProbeResponseMsg(AddressIF _from) {
		from = _from;
	}

}
