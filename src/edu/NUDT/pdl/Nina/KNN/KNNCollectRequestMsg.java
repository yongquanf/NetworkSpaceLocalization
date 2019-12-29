package edu.NUDT.pdl.Nina.KNN;

import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class KNNCollectRequestMsg extends ObjMessage {

	static final long serialVersionUID = 47L;
	public AddressIF from;
	long timeStamp = -1;

	KNNCollectRequestMsg(AddressIF _from) {

		from = _from;
	};
}
