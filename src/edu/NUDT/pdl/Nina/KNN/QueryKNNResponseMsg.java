package edu.NUDT.pdl.Nina.KNN;

import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class QueryKNNResponseMsg extends ObjMessage {
	private static final long serialVersionUID = 48L;
	AddressIF from;

	public QueryKNNResponseMsg(AddressIF _from) {
		from = _from;
	}
}
