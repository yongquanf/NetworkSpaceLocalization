package edu.NUDT.pdl.Nina.KNN;

import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class FarthestResponseMsg extends ObjMessage {
	static final long serialVersionUID = 4444444444L;
	public AddressIF from;

	int marker = -1;

	public FarthestResponseMsg(AddressIF _from) {
		from = _from;

	}
}