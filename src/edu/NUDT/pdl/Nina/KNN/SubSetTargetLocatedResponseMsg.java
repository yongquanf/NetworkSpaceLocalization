package edu.NUDT.pdl.Nina.KNN;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import edu.NUDT.pdl.Nina.util.bloom.Apache.Filter;
import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class SubSetTargetLocatedResponseMsg extends ObjMessage {
	static final long serialVersionUID = 39000L;

	AddressIF from;

	public RelativeCoordinate<AddressIF> targetCoordinates = null;

	public SubSetTargetLocatedResponseMsg(AddressIF _from) {
		from = _from;
	}
}