package edu.NUDT.pdl.Nina.KNN;

import java.util.HashSet;
import java.util.Set;

import edu.NUDT.pdl.Nina.StableNC.lib.Coordinate;
import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class KNNGossipResponseMsg extends ObjMessage {

	static final long serialVersionUID = 46464646464646L;

	Set<AddressIF> nodes;
	AddressIF from;
	boolean cached;
	long sendingStamp = -1;
	boolean isNonRing;
	public Coordinate myCoordinate;
	int NumOfNonEmptyRings = -1;

	// final Coordinate remoteCoord;
	final double myError;

	public KNNGossipResponseMsg(Set<AddressIF> _nodes, AddressIF _from, boolean _cached, Coordinate _myCoordinate,
			double _myError) {

		nodes = new HashSet<AddressIF>(2);
		nodes.addAll(_nodes);

		from = _from;
		cached = _cached;
		isNonRing = false;
		myCoordinate = _myCoordinate.makeCopy();

		// this.remoteCoord=_remoteCoord;
		this.myError = _myError;
	}

}
