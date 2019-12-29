package edu.NUDT.pdl.Nina.KNN;

import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

class AnswerUpdateResponseMsg extends ObjMessage {
	static final long serialVersionUID = 40L;
	public AddressIF from;
	double rtt;

	AnswerUpdateResponseMsg(AddressIF _from, double _rtt) {
		from = _from;
		rtt = _rtt;
	}
}
