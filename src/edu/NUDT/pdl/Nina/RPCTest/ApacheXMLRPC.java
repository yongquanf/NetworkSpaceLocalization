package edu.NUDT.pdl.Nina.RPCTest;

import edu.harvard.syrah.prp.ANSI;
import edu.harvard.syrah.prp.Log;
import edu.harvard.syrah.sbon.async.Config;
import edu.harvard.syrah.sbon.async.EL;
import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.xmlrpc.XMLRPCComm;
import edu.harvard.syrah.sbon.async.comm.xmlrpc.XMLRPCCommIF;

public class ApacheXMLRPC {
	protected static final Log log = new Log(ApacheXMLRPC.class);
	private static final String XMLRPC_OBJECT_NAME = "Nina";

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
		int port = 16182;
		AddressIF apiAddress = AddressFactory.createUnresolved("202.197.22.56",
				port);

		// AddressFactory.createLocalhost(port);
		ExampleXMLRPCHandler api = new ExampleXMLRPCHandler();
		apiComm.registerXMLRPCHandler(XMLRPC_OBJECT_NAME, api);
		apiComm.registerHandler("/", api);

		apiComm.initServer(apiAddress, null);

		/*
		 * log.main("Making XMLRPC call..."); try {
		 * //apiComm.call("http://localhost:16181/", "examples.getStateName",
		 * new XMLRPCCB() {
		 * //apiComm.call("http://xmlrpc.usefulinc.com/demo/server.php",
		 * "system.listMethods", new XMLRPCCB() {
		 * apiComm.call("http://www.mirrorproject.com/xmlrpc/", "mirror.Random",
		 * new XMLRPCCB() {
		 * 
		 * public void cb(Object arg0) { log.info("Result of call is: " + arg0);
		 * }
		 * 
		 * }); } catch (MalformedURLException e1) { log.error("Wrong URL: " +
		 * e1); } catch (UnknownHostException e2) { log.error("Unknown host: " +
		 * e2); }
		 */

		/*
		 * log.main("Calling http://xmlrpc.usefulinc.com/demo/server.php
		 * system.listMethods"); try {
		 * apiComm.call("http://xmlrpc.usefulinc.com/demo/server.php",
		 * "system.listMethods", new XMLRPCCB() { public void cb(Object arg0) {
		 * log.info("Result of second call is: " + arg0.toString()); } }); }
		 * catch (MalformedURLException e1) { log.error("Wrong URL: " + e1); }
		 * catch (UnknownHostException e2) { log.error("Unknown host: " + e2); }
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
