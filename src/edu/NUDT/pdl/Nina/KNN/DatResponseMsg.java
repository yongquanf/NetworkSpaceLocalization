package edu.NUDT.pdl.Nina.KNN;

import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class DatResponseMsg extends ObjMessage {
	static final long serialVersionUID = 43L;
	public AddressIF from;

	DatResponseMsg(AddressIF _from) {
		from = _from;
	}
}
