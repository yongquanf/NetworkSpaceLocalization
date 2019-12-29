package edu.NUDT.pdl.Nina.RPCTest;

import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.Vector;

import edu.harvard.syrah.prp.ANSI;
import edu.harvard.syrah.prp.Log;
import edu.harvard.syrah.sbon.async.CBResult;
import edu.harvard.syrah.sbon.async.Config;
import edu.harvard.syrah.sbon.async.EL;
import edu.harvard.syrah.sbon.async.comm.xmlrpc.XMLRPCCB;
import edu.harvard.syrah.sbon.async.comm.xmlrpc.XMLRPCComm;
import edu.harvard.syrah.sbon.async.comm.xmlrpc.XMLRPCCommIF;

public class ClientXMLRPC {
	protected static final Log log = new Log(ApacheXMLRPC.class);

	public static void main(String[] args) {

		// Turn on assertions
		boolean assertsEnabled = false;
		/*
		 * assert assertsEnabled = true; if (!assertsEnabled)
		 * log.error("Please run the code with assertions turned on: java -ea ..."
		 * );
		 */
		// Turn on colour support
		ANSI.use(Boolean.valueOf(Config.getConfigProps().getProperty(
				"sbon.console.ansi", "true")));

		/*
		 * Create the event loop
		 */
		EL.set(new EL(Long.valueOf(Config.getConfigProps().getProperty(
				"sbon.eventloop.statedump", "600000")), Boolean.valueOf(Config
				.getConfigProps().getProperty("sbon.eventloop.showidle",
						"false"))));

		log.info("Testing the implementation of XMLRPCCommIF.");
		
		
		
		XMLRPCCommIF apiComm = new XMLRPCComm();

		Vector vec = new Vector();
		vec.add("String1");
		vec.add(1.55);
		
		log.main("Calling Nina.estimateRTT");
		try {
			apiComm.call("http://192.168.3.185:55502/xmlrpc",
					"Nina.estimateRTT", new XMLRPCCB() {

						@Override
						public void cb(CBResult result, Object arg0) {
							System.out.println("Result of estimateRTT is: "
									+ arg0.toString());
							
							
						}
						
						
					}, "192.168.1.141", "192.168.3.188");
		} catch (MalformedURLException e1) {
			log.error("Wrong URL: " + e1);
		} catch (UnknownHostException e2) {
			log.error("Unknown host: " + e2);
		}

		
		
		
		/*
		 * log.main("Calling http://xmlrpc.usefulinc.com/demo/server.php
		 * system.listMethods"); try {
		 * apiComm.call("http://xmlrpc.usefulinc.com/demo/server.php",
		 * "system.listMethods", new XMLRPCCB() { public void cb(Object arg0) {
		 * System.out.println("Result of second call is: " + arg0.toString()); }
		 * }); } catch (MalformedURLException e1) { log.error("Wrong URL: " +
		 * e1); } catch (UnknownHostException e2) { log.error("Unknown host: " +
		 * e2); }
		 */

		try {
			EL.get().main();
		} catch (OutOfMemoryError e) {
			EL.get().dumpState(true);
			e.printStackTrace();
			log.error("Error: Out of memory: " + e);
		}

		log.main("Shutdown");
		System.exit(0);
	}
}
