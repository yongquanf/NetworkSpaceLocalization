package edu.NUDT.pdl.Nina.KNN;

import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class ClosestResponseMsg extends ObjMessage {
	static final long serialVersionUID = 41L;
	AddressIF OriginFrom; // src

	AddressIF from; // me

	public ClosestResponseMsg(AddressIF _OriginFrom, AddressIF _from) {
		OriginFrom = _OriginFrom;
		from = _from;
	}

}
