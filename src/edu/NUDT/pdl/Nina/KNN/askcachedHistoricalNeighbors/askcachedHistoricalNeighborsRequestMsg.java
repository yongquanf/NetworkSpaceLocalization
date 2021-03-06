package edu.NUDT.pdl.Nina.KNN.askcachedHistoricalNeighbors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.NUDT.pdl.Nina.KNN.NodesPair;
import edu.NUDT.pdl.Nina.util.bloom.Apache.Filter;
import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class askcachedHistoricalNeighborsRequestMsg extends ObjMessage {
	static final long serialVersionUID = 3535353353535L;

	public AddressIF from;// last hop
	public AddressIF target; // target node

	public int K; // target number of NN

	public askcachedHistoricalNeighborsRequestMsg(AddressIF _from, AddressIF _target, int _K) {
		from = _from;
		target = _target;
		K = _K;
	}

}