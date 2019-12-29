package edu.NUDT.pdl.Nina.KNN;

import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class SubSetClosestResponseMsg extends ObjMessage {
	static final long serialVersionUID = 330000L;
	AddressIF OriginFrom; // src

	AddressIF from; // me

	public SubSetClosestResponseMsg(AddressIF _OriginFrom, AddressIF _from) {
		OriginFrom = _OriginFrom;
		from = _from;
	}

}
