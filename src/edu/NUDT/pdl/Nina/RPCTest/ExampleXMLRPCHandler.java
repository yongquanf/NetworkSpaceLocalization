package edu.NUDT.pdl.Nina.RPCTest;

import java.util.Map;
import java.util.Vector;

import edu.harvard.syrah.prp.Log;
import edu.harvard.syrah.sbon.async.CBResult;
import edu.harvard.syrah.sbon.async.EL;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB0;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB1;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB2;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.http.HTTPCallbackHandler;
import edu.harvard.syrah.sbon.async.comm.xmlrpc.ExampleRPCHandler;

public class ExampleXMLRPCHandler extends HTTPCallbackHandler {
	private static final Log log = new Log(ExampleRPCHandler.class);

	/**
	 * for XML
	 * 
	 * @param value
	 * @param cbInt
	 */
	public void getStateName(final Vector test, final String test2,
			final CB1<String> cbInt) {
		log.info("Called getStateName with =\n(1): " + test.get(0) + ","
				+ test.get(1) + "\n (2): " + test2);
		EL.get().registerTimerCB(5000, new CB0() {
			@Override
			public void cb(CBResult result) {
				cbInt.call(CBResult.OK(), "OK!");
			}
		});
	}

	public void estimateRTT(String ANode, String BNode,
			final CB1<Double> cbDistance) {
		log.info("Called estimateRTT with =\n(1): " + ANode + "\n (2): "
				+ BNode);

		cbDistance.call(CBResult.OK(), Double.valueOf(-2));
	}

	/**
	 * for text/html
	 */
	
	protected void cb(CBResult arg0, AddressIF arg1, String arg2, String arg3,
			CB2<String, byte[]> arg4) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void cb(CBResult result, AddressIF arg1, String arg2,
			String arg3, Map<String, String> arg4, Map<String, String> arg5,
			String arg6, CB2<String, byte[]> arg7) {
		// TODO Auto-generated method stub
		
	}
}
