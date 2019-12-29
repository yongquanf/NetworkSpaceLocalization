package edu.NUDT.pdl.Nina.KNN;

import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class RepeatedNNResponseMsg extends ObjMessage {
	static final long serialVersionUID = 49L;

	AddressIF from;// current hop

	public RepeatedNNResponseMsg(AddressIF _from) {

		from = _from;

	}

}
