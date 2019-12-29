package edu.NUDT.pdl.Nina.Clustering;

import java.util.List;

import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class ClustReleaseResponseMsg extends ObjMessage {
	static final long serialVersionUID = 64L;
	public AddressIF from;
	double[][] H;
	double[][] S;
	final List<AddressIF> landmarks;
	final long version;

	ClustReleaseResponseMsg(AddressIF _from, double[][] _H, double[][] _S,
			List<AddressIF> _landmarks, long _version) {
		from = AddressFactory.create(_from);
		H = _H;
		S = _S;
		landmarks = _landmarks;
		version = _version;
	}
}