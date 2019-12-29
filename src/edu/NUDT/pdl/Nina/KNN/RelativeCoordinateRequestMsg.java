package edu.NUDT.pdl.Nina.KNN;

import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class RelativeCoordinateRequestMsg extends ObjMessage {
	static final long serialVersionUID = 410000000L;

	AddressIF from; // original node

	public RelativeCoordinateRequestMsg(AddressIF _OriginFrom) {
		this.from = _OriginFrom;
	}

	public void clear() {
		// TODO Auto-generated method stub
		from = null;
	}

}
