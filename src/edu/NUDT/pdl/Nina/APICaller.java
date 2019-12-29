package edu.NUDT.pdl.Nina;

import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.NUDT.pdl.Nina.KNN.NodesPair;
import edu.harvard.syrah.prp.ANSI;
import edu.harvard.syrah.prp.Log;
import edu.harvard.syrah.sbon.async.CBResult;
import edu.harvard.syrah.sbon.async.Config;
import edu.harvard.syrah.sbon.async.EL;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB1;
import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.xmlrpc.XMLRPCCB;
import edu.harvard.syrah.sbon.async.comm.xmlrpc.XMLRPCComm;
import edu.harvard.syrah.sbon.async.comm.xmlrpc.XMLRPCCommIF;

public class APICaller {

	protected static final Log log = new Log(APICaller.class);

	// final Ninaloader instance=new Ninaloader();

	boolean testKNN = false;

	static {

		// All config properties in the file must start with 'HSHer.'

		Config.read("Nina", System
				.getProperty("Nina.config", "config/Nina.cfg"));
	}

	static final int API_PORT = Integer.parseInt(Config.getConfigProps()
			.getProperty("api.port", "55501"));

	XMLRPCCommIF apiComm = new XMLRPCComm();

	public APICaller() {

	}

	/**
	 * at least two nodes
	 * 
	 * @param addr
	 */
	public void TestAPI(final String[] addr) {

		final List<AddressIF> addrs = new ArrayList<AddressIF>();

		AddressFactory.createResolved(Arrays.asList(addr),
				Ninaloader.COMM_PORT, new CB1<Map<String, AddressIF>>() {
					@Override
					protected void cb(CBResult result,
							Map<String, AddressIF> addrMap) {
						switch (result.state) {
						case OK: {

							for (String remoteNode : addrMap.keySet()) {
								System.out.println("remoteNode='" + remoteNode
										+ "'");
								AddressIF remoteAddr = addrMap.get(remoteNode);
								addrs.add(remoteAddr);
							}

							if (!testKNN) {
								// estimate latency
								log
										.info("Making XMLRPC call... Nina.estimateRTT"
												+ API_PORT);
								try {
									apiComm.call("http://192.168.3.185:"
											+ API_PORT + "/xmlrpc",
											"Nina.estimateRTT", new XMLRPCCB() {

												@Override
												protected void cb(
														CBResult arg0,
														Object arg1) {
													// TODO Auto-generated
													// method stub
													switch (arg0.state) {
													case OK: {
														// Initialise the log
														// manager
														System.out
																.println("\n======================\nResult of first call is: "
																		+ arg0
																		+ "\n======================\n");
														System.out
																.println("\n======================\n$: latency is: "
																		+ arg1
																		+ "\n======================\n");
														break;
													}
													}
												}

											}, addr[0], addr[1]);

									/*
									 * apiComm.call("http://192.168.3.189:"+API_PORT
									 * +"/xmlrpc", "Nina.getLocalCoord", new
									 * XMLRPCCB() {
									 * 
									 * @Override protected void cb(CBResult
									 * arg0, Object arg1) { // TODO
									 * Auto-generated method stub switch
									 * (arg0.state) { case OK: { // Initialise
									 * the log managerSystem.out.println(
									 * "\n======================\nResult of first call is: "
									 * +
									 * arg1.toString()+"\n======================\n"
									 * ); break; } } }
									 * 
									 * });
									 */

								} catch (MalformedURLException e1) {
									log.error("Wrong URL: " + e1);
								} catch (UnknownHostException e2) {
									log.error("Unknown host: " + e2);
								}

							}

							if (testKNN) {
								// KNN
								int K = 8;
								log.info("Calling Nina.queryKNearestNeighbors");
								try {
									apiComm.call("http://"
											+ addrs.get(0).getHostname() + ":"
											+ API_PORT + "/xmlrpc",
											"Nina.queryKNearestNeighbors",
											new XMLRPCCB() {

												@Override
												public void cb(CBResult result,
														Object KNNs) {
													System.out
															.println("Result of second call is: "
																	+ result
																			.toString());

													switch (result.state) {
													case OK: {
														// Initialise the log
														// manager
														List<NodesPair> arg1 = (List<NodesPair>) KNNs;
														// log.info("$: latency is: "+(Double)arg1);
														if (arg1 != null
																&& arg1.size() > 0) {
															System.out
																	.println("\n======================\n$: "
																			+ arg1
																					.size()
																			+ "nearest neighbors are as follows: \n======================\n");

															// record current
															// KNN

															Iterator<NodesPair> ier = arg1
																	.iterator();
															int ind = 0;
															NodesPair nxtNode;
															while (ier
																	.hasNext()) {
																nxtNode = ier
																		.next();

																// myself
																ind++;
																// otherwise
																System.out
																		.println(ind
																				+ "th NN: "
																				+ nxtNode
																						.SimtoString());

															}
															// only success, we
															// register
															// registerTestMeridian();
														} else {
															System.out
																	.println("************************\n FAILED to query KNN!\nSet<NodesPair> is empty!\n************************");
														}

														break;
													}
													}

												}
											}, addrs.get(1).getHostname(), K);
								} catch (MalformedURLException e1) {
									log.error("Wrong URL: " + e1);
								} catch (UnknownHostException e2) {
									log.error("Unknown host: " + e2);
								}

							}

							// sort

							break;
						}
						case TIMEOUT:
						case ERROR: {
							log.error("Could not resolve bootstrap list: "
									+ result.what);
							break;
						}
						}
					}
				});

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		// Turn on assertions
		boolean assertsEnabled = false;

		final String[] addr = { "192.168.2.45", "192.168.3.189" };
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

		final APICaller test = new APICaller();

		String hostname = "202.197.22.56"; // IP or DNS address

		int port = 55502;
		AddressIF apiAddress = AddressFactory.createUnresolved(hostname, port);

		// test.apiComm.registerXMLRPCHandler("examples", new
		// ExampleRPCHandler());
		test.apiComm.initServer(apiAddress, null);

		test.TestAPI(addr);

		/*
		 * test.instance.join(hostname, new CB0() {
		 * 
		 * @Override protected void cb(CBResult result) { switch (result.state)
		 * { case OK: {
		 * 
		 * System.out.println("Join correctly!");
		 * //test.apiComm=(XMLRPC_API)test
		 * .instance.getApiManager().apiInstances.get(0); test.TestAPI(addr);
		 * break; } case TIMEOUT: case ERROR: { System.err.println("Failed: " +
		 * result.what); break; } } }
		 * 
		 * });
		 */

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
