package edu.NUDT.pdl.Nina.KNN;

import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class TargetLocatedResponseMsg extends ObjMessage {
	static final long serialVersionUID = 39L;

	AddressIF from;

	public TargetLocatedResponseMsg(AddressIF _from) {
		from = _from;
	}
}
