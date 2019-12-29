package edu.NUDT.pdl.Nina.KNN;

import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class FinFarthestResponseMsg extends ObjMessage {
	static final long serialVersionUID = 44000000L;
	public AddressIF from;

	FinFarthestResponseMsg(AddressIF _from) {
		from = _from;

	}
}
