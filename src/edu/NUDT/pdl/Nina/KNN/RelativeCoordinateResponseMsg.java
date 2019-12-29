package edu.NUDT.pdl.Nina.KNN;

import edu.NUDT.pdl.Nina.StableNC.lib.Coordinate;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class RelativeCoordinateResponseMsg extends ObjMessage {
	static final long serialVersionUID = 410000000L;
	AddressIF from; // me
	Coordinate myRelativeCoordinate;

	final Coordinate remoteCoord;
	final double remoteError;

	public RelativeCoordinateResponseMsg(AddressIF _from, Coordinate _myRelativeCoordinate, Coordinate _remoteCoord,
			double _remoteError) {
		this.from = _from;
		this.myRelativeCoordinate = _myRelativeCoordinate;
		remoteCoord = _remoteCoord;
		remoteError = _remoteError;
	}

	public void clear() {
		// TODO Auto-generated method stub
		myRelativeCoordinate = null;
	}

}
