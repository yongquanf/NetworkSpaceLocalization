package edu.NUDT.pdl.Nina.KNN;

import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class CompleteSubSubsetSearchResponseMsg extends ObjMessage {

	static final long serialVersionUID = 33000000L;
	AddressIF from; // me

	public CompleteSubSubsetSearchResponseMsg(AddressIF _from) {

		from = _from;
	}

}
