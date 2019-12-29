package edu.NUDT.pdl.Nina.KNN;

import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;

//Used in GOSSIP
public class NodeIdentRendv {
	AddressIF addr;

	AddressIF addrRendv;

	NodeIdentRendv() {

	}

	public void copy(AddressIF addr1, AddressIF addrRendv1) {

		addr = addr1;

		addrRendv = addrRendv1;

	}
}
