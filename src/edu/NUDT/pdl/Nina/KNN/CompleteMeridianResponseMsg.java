package edu.NUDT.pdl.Nina.KNN;

import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class CompleteMeridianResponseMsg extends ObjMessage {

	static final long serialVersionUID = 3300L;
	AddressIF from; // me

	public CompleteMeridianResponseMsg(AddressIF _from) {

		from = _from;
	}

}
