package edu.NUDT.pdl.Nina.KNN;

import edu.harvard.syrah.sbon.async.comm.AddressIF;

public class Commons {

	public static NodeIdentRendv NodeIdentRendvCreator = null;

	public class NodeIdent {
		AddressIF addr;
		int port;
	}

	public class NodeIdentConst {
		int addr;
		int port;
		int latencyConstMS;
	}

	public class NodeIdentLat {
		int addr;
		int port;
		int latencyUS;
	}

	public class MeasuredResult {
		int addr;
		int port;
		int latencyUS;
	}

}
