package edu.NUDT.pdl.Nina.KNN;

import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class UpdateResponseMsg extends ObjMessage {
	static final long serialVersionUID = 3737373737L;
	public AddressIF from;
	public long TimerCache;

	UpdateResponseMsg(AddressIF _from) {
		from = _from;
	}
}
