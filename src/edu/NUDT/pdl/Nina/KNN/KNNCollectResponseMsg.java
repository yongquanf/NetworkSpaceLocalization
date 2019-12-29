package edu.NUDT.pdl.Nina.KNN;

import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

class KNNCollectResponseMsg extends ObjMessage {
	static final long serialVersionUID = 47L;
	final AddressIF from;
	final long timestamp;

	KNNCollectResponseMsg(AddressIF _from, long _timestamp) {
		from = _from;
		timestamp = _timestamp;

	}
}