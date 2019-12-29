package edu.NUDT.pdl.Nina.KNN;

import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class RepeatedSubSetResponseMsg extends ObjMessage {
	static final long serialVersionUID = 49000L;

	AddressIF from;// current hop

	public RepeatedSubSetResponseMsg(AddressIF _from) {

		from = _from;

	}

}
