package edu.NUDT.pdl.Nina.KNN;

import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class FinKNNResponseMsg extends ObjMessage {
	static final long serialVersionUID = 45L;
	AddressIF from; // me

	public FinKNNResponseMsg(AddressIF _from) {

		from = _from;
	}

}
