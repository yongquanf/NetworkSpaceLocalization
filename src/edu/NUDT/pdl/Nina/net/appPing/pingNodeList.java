package edu.NUDT.pdl.Nina.net.appPing;

import java.util.List;
import java.util.Vector;

import edu.harvard.syrah.sbon.async.CallbacksIF.CB1;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB2;
import edu.harvard.syrah.sbon.async.comm.AddressIF;

public interface pingNodeList<T> {

	
	public void ping(Vector<T> nodes, CB2<Vector<String>, Vector<Double>> cbDone);
	public void ping(T node,CB1<Double> cbDone);
}
