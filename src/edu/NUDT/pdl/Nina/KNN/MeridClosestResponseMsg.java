package edu.NUDT.pdl.Nina.KNN;

import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class MeridClosestResponseMsg extends ObjMessage {
	static final long serialVersionUID = 35L;
	AddressIF OriginFrom; // src

	AddressIF from; // me

	public MeridClosestResponseMsg(AddressIF _OriginFrom, AddressIF _from) {
		OriginFrom = _OriginFrom;
		from = _from;
	}

}
