package edu.NUDT.pdl.Nina.KNN;

import java.util.Set;

import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class DatRequestMsg extends ObjMessage {

	static final long serialVersionUID = 43L;
	public AddressIF from;
	Set<NodesPair> latencySet;
	long timerCached;

	public int repeated = 0;

	DatRequestMsg(AddressIF _from, Set<NodesPair> _latencySet, long _timerCached) {

		from = _from;
		latencySet = _latencySet;
		timerCached = _timerCached;
	};
}
