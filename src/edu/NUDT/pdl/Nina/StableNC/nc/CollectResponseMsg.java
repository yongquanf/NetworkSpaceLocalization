package edu.NUDT.pdl.Nina.StableNC.nc;

import edu.NUDT.pdl.Nina.StableNC.lib.Coordinate;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class CollectResponseMsg extends ObjMessage {
	static final long serialVersionUID = 19L;

	Coordinate remoteCoord;
	double remoteError;
	
	public CollectResponseMsg(Coordinate _remoteCoord,double _remoteError) {
				
		remoteCoord=_remoteCoord;
		remoteError=_remoteError;		
	}
	
	public CollectResponseMsg(){
		
	}
}