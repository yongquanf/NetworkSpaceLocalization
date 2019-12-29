package edu.NUDT.pdl.Nina.KNN;

import java.util.HashSet;
import java.util.Set;

import edu.NUDT.pdl.Nina.StableNC.lib.Coordinate;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class KNNGossipRequestMsg extends ObjMessage {

	static final long serialVersionUID = 46464646464646L;

	AddressIF from;
	Set<AddressIF> nodes;
	boolean cached;
	long sendingStamp = -1;
	boolean isNonRing;
	int NumOfNonEmptyRings = -1;

	public Coordinate myCoordinate;
	final double myError;

	public double RTTValue = -1;

	public KNNGossipRequestMsg(Set<AddressIF> _nodes, AddressIF _from, boolean _cached, Coordinate _myCoordinate,
			double _myError) {
		nodes = new HashSet<AddressIF>(2);
		nodes.addAll(_nodes);
		from = _from;
		cached = _cached;
		isNonRing = false;
		myCoordinate = _myCoordinate.makeCopy();

		this.myError = _myError;
	}

	public void clear() {
		// TODO Auto-generated method stub
		if (nodes != null && !nodes.isEmpty()) {
			nodes.clear();
		}
	}
}
