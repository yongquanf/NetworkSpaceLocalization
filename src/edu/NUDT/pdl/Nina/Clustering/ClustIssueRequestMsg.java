package edu.NUDT.pdl.Nina.Clustering;

import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class ClustIssueRequestMsg extends ObjMessage {
	static final long serialVersionUID = 63L;
	public AddressIF from;
	public int IndexOfFrom;
	public double[][] H;
	public double[][] S;
	final long timeStamp;
	final long version;

	ClustIssueRequestMsg(AddressIF _from, double[][] _H, double[][] _S,
			int _IndexOfFrom, long _timeStamp, long _version) {
		from = _from;
		H = _H;
		S = _S;
		IndexOfFrom = _IndexOfFrom;
		timeStamp = _timeStamp;
		version = _version;
	}
}
