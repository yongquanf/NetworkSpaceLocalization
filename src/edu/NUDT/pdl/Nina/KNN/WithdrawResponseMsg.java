package edu.NUDT.pdl.Nina.KNN;

import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class WithdrawResponseMsg extends ObjMessage {
	static final long serialVersionUID = 36L;
	AddressIF from; // me

	public WithdrawResponseMsg(AddressIF _from) {

		from = AddressFactory.create(_from);
	}

}
