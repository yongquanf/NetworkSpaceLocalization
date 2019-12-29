package edu.NUDT.pdl.Nina.KNN;

import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class CompleteNNResponseMsg extends ObjMessage {

	static final long serialVersionUID = 42L;
	AddressIF from; // me

	public CompleteNNResponseMsg(AddressIF _from) {

		from = _from;
	}

}
