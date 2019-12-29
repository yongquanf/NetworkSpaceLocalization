package edu.NUDT.pdl.Nina.Demo_ivce;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import edu.NUDT.pdl.Nina.Ninaloader;
import edu.NUDT.pdl.Nina.util.MainGeneric;
import edu.harvard.syrah.prp.ANSI;
import edu.harvard.syrah.prp.Log;
import edu.harvard.syrah.sbon.async.CBResult;
import edu.harvard.syrah.sbon.async.Config;
import edu.harvard.syrah.sbon.async.EL;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB0;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB1;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB2;
import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;

public class SimpleDemo {

	private static final Log log = new Log(Ninaloader.class);

	Ninaloader instance = null;

	public SimpleDemo() {

		instance = new Ninaloader();

	}

	public void testRTTEstimate(final String[] addr) {

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

							// =======================================================================
							/*if (instance != null) {

								instance.ncManager.estimateRTT(addrs.get(0),
										addrs.get(1), new CB1<Double>() {

											@Override
											protected void cb(CBResult result1,
													Double latency) { //

												switch (result1.state) {
												case OK: {
													// TODO Auto-generated
													// method stub
													System.out
															.println("Latency query results are returned as: "
																	+ latency
																			.doubleValue());

													break;
												}

												case TIMEOUT:
												case ERROR: {
													System.err
															.println("Failed: to measure RTT! "
																	+ result1.what);
													break;
												}

												}

											}
										});
							} else {
								System.err
										.println("\n\n================\n\nNULL ! System.err.println");
								// System.exit(-1);
							}
							*/

							// =======================================================================

							// estimate the distance ranking
							String target = addr[0];
							String[] peers = new String[addr.length - 1];
							for (int i = 0; i < peers.length; i++) {
								peers[i] = addr[i + 1];
							}

							/*instance.ncManager.sortNodesAccordingToTarget(
									target, peers,
									new CB2<Vector<String>, Vector<Double>>() {

										@Override
										protected void cb(CBResult result2,
												Vector<String> arg1,
												Vector<Double> arg2) {
											// TODO Auto-generated method stub
											switch (result2.state) {
											case OK: {
												System.out
														.println("\n\n===================\n\n");
												if (arg1.size() > 0) {
													for (int i = 0; i < arg1
															.size(); i++) {
														System.out
																.print(arg1
																		.get(i)
																		+ "  \tRTT: "
																		+ Math
																				.round(arg2
																						.get(
																								i)
																						.doubleValue())
																		+ "\n");

													}

												}

												System.out
														.println("\n\n===================\n\n");

											}
												break;
											case TIMEOUT:
											case ERROR: {
												System.err
														.println("Failed: to sort Nodes According To Target! "
																+ result2.what);
												break;
											}

											}
										}

									});
							*/
							// =======================================================================

							//estimate KNN 
						    int k=peers.length;
							instance.NNManager.queryKNearestNeighbors(
									target, peers,k,
									new CB1<Map<String,Double>>() {

										@Override
										protected void cb(CBResult result2,Map<String,Double> arg0) {
											// TODO Auto-generated method stub
											switch (result2.state) {
											case OK: {
												System.out
														.println("\n\n===================\n\n");
												if (arg0.size() > 0) {
													

												}

												System.out
														.println("\n\n===================\n\n");

											}
												break;
											case TIMEOUT:
											case ERROR: {
												System.err
														.println("Failed: to sort Nodes According To Target! "
																+ result2.what);
												break;
											}

											}
										}

									});
							
							
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

	// instance

	/**
	 * @param args
	 */
	@SuppressWarnings("static-access")
	public static void main(String[] args) {

		// --------------------------------------------

		// join the Nina measurement system

		final boolean[] runed={false};
		final SimpleDemo test = new SimpleDemo();

		final String hostname = args[0]; // IP or DNS address
		final String[] addr = { "192.168.1.141",
				"192.168.1.140", "192.168.3.189", "192.168.3.190",
				};

		MainGeneric.createThread("MainThread", new Runnable() {

			public void run() {
				
				// Turn on colour support
				ANSI.use(Boolean.valueOf(Config.getConfigProps().getProperty(
						"sbon.console.ansi", "true")));

				EL.set(new EL(Long.valueOf(Config.getConfigProps().getProperty(
						"sbon.eventloop.statedump", "600000")), Boolean.valueOf(Config
						.getConfigProps().getProperty("sbon.eventloop.showidle",
								"false"))));
				// --------------------------------------------
								
				test.instance.join(hostname, new CB0() {
					@Override
					protected void cb(CBResult result) {
						switch (result.state) {
						case OK: {

							System.out.println("Join correctly!");
							// System.exit(-1);
							runed[0]=true;				
							// change !
							// EL.get().registerTimerCB(delay, test.updateCB);

							break;
						}
						case TIMEOUT:
						case ERROR: {
							System.err.println("Failed: " + result.what);
							break;
						}
						}
					}

				});
				// ----------------------------------------------
				try {
					EL.get().main();
				} catch (OutOfMemoryError e) {
					EL.get().dumpState(true);
					e.printStackTrace();
					log.error("Error: Out of memory: " + e);
				}

				// ----------------------------------------------	
			}				
		
		});
		
		
		
	
		
		try {
			Thread.currentThread().sleep(60000);
			test.testRTTEstimate(addr);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		

		/*
		 * //query the KNN int k=5; AddressIF
		 * target=AddressFactory.createUnresolved(hostname,
		 * Ninaloader.COMM_PORT);
		 * instance.NNManager.queryKNearestNeighbors(target, k, new
		 * CB1<List<NodesPair>>(){
		 * 
		 * protected void cb(CBResult result, List<NodesPair> KNNs) { // TODO
		 * Auto-generated method stubSystem.out.println(
		 * "KNN query results are returned, and saved into the Set KNNs");
		 * 
		 * }
		 * 
		 * });
		 * 
		 * 
		 * 
		 * //estimate the latency String hostA=""; String hostB="";
		 * 
		 * AddressIF nodeA=AddressFactory.createUnresolved(hostA,
		 * Ninaloader.COMM_PORT); AddressIF
		 * nodeB=AddressFactory.createUnresolved(hostB, Ninaloader.COMM_PORT);
		 * 
		 * 
		 * 
		 * }
		 * 
		 * });
		 */

	}
	

}
