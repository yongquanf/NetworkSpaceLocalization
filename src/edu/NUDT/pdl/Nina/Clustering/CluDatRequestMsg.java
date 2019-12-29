package edu.NUDT.pdl.Nina.Clustering;

import java.util.Set;

import edu.NUDT.pdl.Nina.KNN.NodesPair;
import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class CluDatRequestMsg extends ObjMessage {

	static final long serialVersionUID = 61L;
	public AddressIF from;
	Set<NodesPair> latencySet;
	final long timeStamp;

	CluDatRequestMsg(AddressIF _from, Set<NodesPair> _latencySet,
			long _timeStamp) {

		from = AddressFactory.create(_from);
		latencySet = _latencySet;
		timeStamp = _timeStamp;
	};
}
