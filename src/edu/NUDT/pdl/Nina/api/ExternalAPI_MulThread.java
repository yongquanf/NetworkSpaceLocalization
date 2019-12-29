package edu.NUDT.pdl.Nina.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.NUDT.pdl.Nina.Ninaloader;
import edu.NUDT.pdl.Nina.KNN.AbstractNNSearchManager;
import edu.NUDT.pdl.Nina.KNN.NodesPair;
import edu.NUDT.pdl.Nina.StableNC.lib.Coordinate;
import edu.NUDT.pdl.Nina.StableNC.nc.StableManager;
import edu.NUDT.pdl.Nina.net.appPing.NPair;
import edu.NUDT.pdl.Nina.net.appPing.PingNodes;
import edu.NUDT.pdl.Nina.net.appPing.pair;
import edu.NUDT.pdl.Nina.util.MainGeneric;
import edu.harvard.syrah.prp.Log;
import edu.harvard.syrah.prp.NetUtil;
import edu.harvard.syrah.prp.POut;
import edu.harvard.syrah.prp.SortedList;
import edu.harvard.syrah.sbon.async.CBResult;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB1;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB2;
import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.NetAddress;
import edu.harvard.syrah.sbon.async.comm.http.HTTPCallbackHandler;

public class ExternalAPI_MulThread extends HTTPCallbackHandler implements ExternalAPIIF{
	private static final Log log = new Log(ExternalAPI_MulThread.class);

	private static String HTTP_COORD_BEGIN = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n"
			+ "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">\n"
			+ "<head>\n"
			+ "<title>Pyxida Node</title>\n"
			+ "<style type=\"text/css\">\n"
			+ "body { margin-top:  1em; margin-bottom: 1em; margin-left: 1em; margin-right:  1em; }\n"
			+ "</style>\n"
			+ "</head>\n"
			+ "<body>\n"
			+ "<img style=\"float: left; margin-right: 1em; \" alt=\"Pyxida Logo\" src=\"http://pyxida.sourceforge.net/img/pyxida4-scaled.jpg\"/>\n"
			+ "<center><h2><a href=\"http://pyxida.sourceforge.net\">Pyxida</a> Node Information</h2>\n"
			+ "<table>\n"
			+ "<tr><td width=\"40%\" align=\"center\"><h3>Node Information</h3></td>\n"
			+ "<td width=\"10%\"> </td>\n"
			+ "<td><h3 width=\"50%\" align=\"center\">Neighbour Information</h3></td>\n"
			+ "</tr>\n"
			+ "<tr><td align=\"center\" valign=\"top\">\n"
			+ "<table>\n";

	private static String HTTP_COORD_MIDDLE = "</td>\n"
			+ "<td></td><td></table>\n"
			+ "</td><td> </td><td valign=\"top\"><table width=\"100%\">\n";

	private static String HTTP_COORD_END = "</table>\n"
			+ "</td></tr></table>\n" + "</center>\n" + "</body>\n"
			+ "</html>\n";

	private StableManager ncManager;
	private AbstractNNSearchManager NNManager;
	private PingNodes pingAllPairs;

	final ExecutorService execExternalAPI =Ninaloader.execNina;
	
	public ExternalAPI_MulThread(StableManager ncManager, AbstractNNSearchManager NNManager, PingNodes _pingAllPairs) {
		this.ncManager = ncManager;
		this.NNManager = NNManager;
		this.pingAllPairs=_pingAllPairs;
		
		
	}

	public void getProxyCoord(String remoteNodeStr,
			final CB2<Vector<Double>, String> cbProxyCoord) {
		AddressFactory.createResolved(remoteNodeStr, Ninaloader.COMM_PORT,
				new CB1<AddressIF>() {
					
					protected void cb(CBResult nsResult, AddressIF remoteNode) {
						switch (nsResult.state) {
						case OK: {
							log.debug("resolved node=" + remoteNode);
							Coordinate coord = null;
							// ncManager.getProxyCoord(remoteNode);
							Vector<Double> listCoord = getVectorFromCoord(coord);
							cbProxyCoord.call(CBResult.OK(), listCoord,
									"Success");
							break;
						}
						case TIMEOUT:
						case ERROR: {
							log.warn(nsResult.toString());
							cbProxyCoord.call(nsResult, null, nsResult
									.toString());
							break;
						}
						}
					}
				});
	}

	public void destroyProxyCoord(String remoteNodeStr,
			final CB1<String> cbResult) {
		AddressFactory.createResolved(remoteNodeStr, Ninaloader.COMM_PORT,
				new CB1<AddressIF>() {
					
					protected void cb(CBResult nsResult, AddressIF remoteNode) {
						switch (nsResult.state) {
						case OK: {
							log.debug("resolved node=" + remoteNode);
							String res = "";
							// ncManager.destroyProxyCoord(remoteNode);
							cbResult.call(CBResult.OK(), res);
							break;
						}
						case TIMEOUT:
						case ERROR: {
							log.warn(nsResult.toString());
							cbResult.call(nsResult, nsResult.toString());
							break;
						}
						}
					}
				});
	}

	
	public void MeridianFor1NN(final String Target,final CB1<Vector<Object>> cbKNN) {

			
						final String TargetNode = Target;
							
							//final Map<String, Object> map=new Hashtable<String, Object>();
							final Vector<Object> vec=new Vector<Object>(5);
							
							log.info("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
							AddressFactory.createResolved(TargetNode, Ninaloader.COMM_PORT,
									new CB1<AddressIF>() {

										
										protected void cb(CBResult result, AddressIF addr) {
											// TODO Auto-generated method stub
											switch (result.state) {
											case OK: {						
													log.info("#:XMLRPC: "+addr.toString());
													
													NNManager.queryNearestNeighbor(addr,1,
															new CB1<NodesPair>() {

																
																protected void cb(CBResult result,
																		NodesPair NP) {
																	// TODO Auto-generated method
																	switch (result.state) {
																	case OK: {
																		// stub
																		log.info(" Meridian query results are returned");
																		// yes, we've got KNN
																			//map.put(nxtNode.startNode.toString(), Double.valueOf(nxtNode.rtt));
																			
//																		vec.add("######################");
//																		vec.add(Ninaloader.me.toString());
//																		vec.add("######################");
																		

																		vec.add(NP.startNode.toString());
																		vec.add(Double.valueOf(NP.rtt));
																		vec.add(Double.valueOf(NP.elapsedTime));
																				
																	
																		
																		log.info("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
																		cbKNN.call(result,vec);
																		break;
																	}
																	case TIMEOUT:
																	case ERROR: {
																		log.warn(result.toString());
																		cbKNN.call(result,vec);
																		log.info("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
																		break;
																	}

																	}
																	vec.clear();
																}

															});
											
												
												break;
											}
											case TIMEOUT:
											case ERROR: {
												log.warn(result.toString());
												cbKNN.call(result,vec);
												break;
											}
											}

										}

									});
							
	
		
	}
	
	/**
	 * sorted nodes, Vector, format String, rtt, String, rtt...
	 * @param remoteNodesMap
	 * @param cbRemoteCoords
	 */
	public void PingAllNodes(final Vector<String> addrs, final CB1<Vector> cbRemoteCoords){
		
		
			
						
						
						pingAllPairs.ping(addrs, new CB2<Vector<String>, Vector<Double>>(){
							
							protected void cb(CBResult result, Vector<String> arg1,
									Vector<Double> arg2) {
								// TODO Auto-generated method stub
							Vector vec=new Vector();	
							int len=arg1.size();	
							if(len>0){
								for(int i=0;i<len;i++){
									vec.add(arg1.get(i));
									vec.add(arg2.get(i));					
								}
								
							}
							 cbRemoteCoords.call(CBResult.OK(), vec);
							
							 vec.clear();
							}
													
						});
			
	

	}
	
	public void PingAllNodesforKNearestNode(final Vector<String> addrs,final int K, final CB1<Vector> cbRemoteCoords){
	/*	
		execExternalAPI.execute(  
				new Runnable() {

					public void run() {*/			
						
						final int KK=K;
						final long curTime=System.currentTimeMillis();
						
						
						pingAllPairs.ping(addrs, new CB2<Vector<String>, Vector<Double>>(){
							
							protected void cb(CBResult result, Vector<String> arg1,
									Vector<Double> arg2) {
								// TODO Auto-generated method stub
							
												
							Vector vec=new Vector();
							
//							vec.add("######################");
//							vec.add(Ninaloader.me.toString());
//							vec.add("######################");
							
							
							
							int len=arg1.size();	
							if(len>0){
								
								SortedList<pair> sortedL = new SortedList<pair>(
										new NPair());	
									
									
								for(int i=0;i<arg1.size();i++){					
									sortedL.add(new pair(arg1.get(i),arg2.get(i)));
								}
								/*final Vector<String> addrs =new Vector<String>(2);
								final Vector<Double> rtts=new Vector<Double>(2);
								*/
								//remove me
								
								int num=Math.min(KK,sortedL.size() );
								long time2=System.currentTimeMillis();
								int interval=(int)(time2-curTime);
								for(int i=0;i<num;i++){
									vec.add(sortedL.get(i).getAddr());
									vec.add(Double.valueOf(sortedL.get(i).getRtt()));
									vec.add(i+1);
									vec.add(interval);
								}
									
								sortedL.clear();
							}
							 cbRemoteCoords.call(CBResult.OK(), vec);
							 
							 vec.clear();
							 	
							}
													
						});
					
	
	}
	
	/**
	 * KNN query
	 * 
	 * @param TargetNode
	 * @param K
	 * @param cbKNN
	 */
	public void queryKNearestNeighbors(final String Target,final int k,
			final CB1<Map<String, Object>> cbKNN) {

		//create a new thread	
		execExternalAPI.execute(new Runnable(){

			
			public void run() {
			
				final String TargetNode = Target;
				final int K = k;
				
				final Map<String, Object> map=new Hashtable<String, Object>();
				
				log.info("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
				AddressFactory.createResolved(TargetNode, Ninaloader.COMM_PORT,
						new CB1<AddressIF>() {

							
							protected void cb(CBResult result, AddressIF addr) {
								// TODO Auto-generated method stub
								switch (result.state) {
								case OK: {
									if (addr == null) {
										String error = "Could not resolve a="
												+ TargetNode;
										log.warn(error);
										cbKNN.call(result,map);
										break;
									} else {
										log.info("#:XMLRPC: "+addr.toString());
										
										NNManager.queryKNearestNeighbors(addr, K,
												new CB1<List<NodesPair>>() {

													
													protected void cb(CBResult result,
															List<NodesPair> KNNs) {
														// TODO Auto-generated method
														switch (result.state) {
														case OK: {
															// stub
															System.out
																	.println("KNN query results are returned");
															// yes, we've got KNN
															if (KNNs != null
																	&& KNNs.size() > 0) {
																Iterator<NodesPair> ier = KNNs
																		.iterator();
																int ind = 0;
																NodesPair nxtNode;
																while (ier.hasNext()) {
																	nxtNode = ier
																			.next();
																	// myself
																	/*if (nxtNode.startNode
																			.equals(nxtNode.endNode)) {
																		continue;
																	}*/
																	ind++;
																	// otherwise
																	System.out
																			.println("The "
																					+ ind
																					+ "th NN: "
																					+ nxtNode
																							.toString());
																	map.put(nxtNode.startNode.toString(), Double.valueOf(nxtNode.rtt));
//																	addrs
//																			.add(nxtNode.startNode
//																					.toString());
//																	rtts
//																			.add(Double
//																					.valueOf(nxtNode.rtt));

																}

															}

															cbKNN.call(result,map);
															log.info("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
															break;
														}
														case TIMEOUT:
														case ERROR: {
															log.warn(result.toString());
															cbKNN.call(result,map);
															log.info("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
															break;
														}

														}
														
														map.clear();

													}

												});

									}

									break;
								}
								case TIMEOUT:
								case ERROR: {
									log.warn(result.toString());
									cbKNN.call(result,map);
									break;
								}
								}

							}

						});
			}
			
		});
			

					
	}

	
	public void queryKNearestNeighborsAndElapsedTime(final String Target,final int k,
			final CB1<Vector<Object>> cbKNN) {
		
		//create a new thread	
		execExternalAPI.execute(new Runnable(){

			
			public void run() {
			
				final String TargetNode = Target;
				final int K = k;
				final Vector<String> addrs = new Vector<String>(1);
				final Vector<Double> rtts = new Vector<Double>(1);

				//final Map<String, Object> map=new Hashtable<String, Object>();
				final Vector<Object> vec=new Vector<Object>(5);
				
				AddressFactory.createResolved(Target, Ninaloader.COMM_PORT,
						new CB1<AddressIF>() {
							
							protected void cb(CBResult nsResult, AddressIF addr) {
								switch (nsResult.state) {
								case OK: {
								

									log.info("#:XMLRPC: "+addr.toString());
									
									NNManager.queryKNearestNeighbors(addr, K,
											new CB1<List<NodesPair>>() {

												
												protected void cb(CBResult result,
														List<NodesPair> KNNs) {
													// TODO Auto-generated method
													switch (result.state) {
													case OK: {
														// stub
														System.out
																.println("KNN query results are returned");
														// yes, we've got KNN
														if (KNNs != null
																&& KNNs.size() > 0) {
															Iterator<NodesPair> ier = KNNs
																	.iterator();
															int ind = 0;
															NodesPair nxtNode;
															
//															vec.add("######################");
//															vec.add(Ninaloader.me.toString());
//															vec.add("######################");
															
															
															while (ier.hasNext()) {
																nxtNode = ier
																		.next();
																// myself
																/*if (nxtNode.startNode
																		.equals(nxtNode.endNode)) {
																	continue;
																}*/
																ind++;
																// otherwise
																System.out
																		.println("The "
																				+ ind
																				+ "th NN: "
																				+ nxtNode
																						.toString());
																//map.put(nxtNode.startNode.toString(), Double.valueOf(nxtNode.rtt));
															
																vec.add(nxtNode.startNode.toString());
																
																vec.add(Double.valueOf(nxtNode.rtt));
																vec.add(ind);
																vec.add(Double.valueOf(nxtNode.elapsedTime));
																
//																addrs
//																		.add(nxtNode.startNode
//																				.toString());
//																rtts
//																		.add(Double
//																				.valueOf(nxtNode.rtt));

															}

														}

														cbKNN.call(result,vec);
														log.info("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
														break;
													}
													case TIMEOUT:
													case ERROR: {
														log.warn(result.toString());
														cbKNN.call(result,vec);
														log.info("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
														break;
													}

													}

												}

											});
									
									break;
								}
								case TIMEOUT:
								case ERROR: {
									cbKNN.call(nsResult, vec);
									break;
								}
								}
							}
						});
								
				

			}			
		});
					
	}
	
	/**
	 * estimate the RTT, as well as the error towards a set of nodes
	 * @param remoteNodesMap
	 * @param cbsortedNodes
	 */
	public void estimateRTTWithErrorElapsedTime(final Vector<String> remoteNodes,final int choiceofCoord,
			final CB1<Vector<Object>> cbsortedNodes){
		
		
		execExternalAPI.execute(new Runnable(){

			
			public void run() {
			

				//ncManager.estimateRTTWithErrorElapsedTime(remoteNodes,choiceofCoord ,cbsortedNodes);
				final Vector<Object> resultVector=new Vector<Object>(2);
				
				final double curTime=System.nanoTime();

				
				
				List<String> nodesStr=new ArrayList<String>(2);
				nodesStr.addAll(remoteNodes);
				
				AddressFactory.createResolved(nodesStr,Ninaloader.COMM_PORT, new CB1<Map<String, AddressIF>>() {
					protected void cb(CBResult result, Map<String, AddressIF> addrMap) {
						switch (result.state) {
							case OK:

								//TODO jonathan: why do you handle OK and TIMEOUT together and why is there an ERROR clause?
								// The way createResolved works is that it always returns OK but only includes the hostnames
								// that resolved correctly in the map. Let me know if you don't like this behaviour and I can change it.
							case TIMEOUT: {

								//StringBuffer dnsErrors = new StringBuffer();
								final List<AddressIF> AllAddrs = new ArrayList<AddressIF>();

								for (AddressIF node : addrMap.values()) {
									if (node != null && node.hasPort()) {
										AllAddrs.add(node);
									}
									//else {
									//dnsErrors.append("Could not resolve "+node);
									//}
								}

								ncManager.getRemoteCoords(
										 AllAddrs,
										new CB2<Map<AddressIF, Coordinate>, String>() {
											protected void cb(
													final CBResult ncResult,
													Map<AddressIF, Coordinate> addr2coords,
													String errorString) {

												final Map<AddressIF, Coordinate> addr2coord=addr2coords;
												/*SortedList<NodeRecord> sortedL = new SortedList<NodeRecord>(
															new EntryComarator());*/
													
													ncManager.doPing( AllAddrs, new CB1<Map<AddressIF,Double>>(){

														
														protected void cb(
																CBResult result,
																Map<AddressIF, Double> addr2rtt) {
															// TODO Auto-generated method stub
															
															for (final Entry<AddressIF, Coordinate> Entry : addr2coord
																	.entrySet()) {
																
																
																/*if(Entry.getValue()==null|| addr2rtt.containsKey( Entry.getKey())){
																	continue;
																}*/
																
																final AddressIF entry=Entry.getKey();
																
																									
																		// TODO Auto-generated method stub
																		double RTT=addr2rtt.get(entry);
																		// myselfy
																		/*	if (Ninaloader.me.equals(Entry.getKey())) {
																				continue;
																			}*/

																			
																			Coordinate me=null;
																			Coordinate you=null;
																			double myError=-1;
																			double yourError=-1;
																			
																			if(choiceofCoord==0){
																				
																				//synchronized(localNC.primaryNC){
																					
																				me=ncManager.localNC.primaryNC.sys_coord;
																				you=Entry.getValue();
																				myError=ncManager.localNC.primaryNC.getSystemError();
																				yourError=you.r_error;
																				
																				//}
																				
																				
																			}else if(choiceofCoord==1){
																				
																				//synchronized(localVivaldi.primaryNC){
																					
																				me=ncManager.localVivaldi.primaryNC.sys_coord;
																				you=Entry.getValue().VivaldiCoord;
																				
																				myError=ncManager.localVivaldi.primaryNC.getSystemError();
																				yourError=you.r_error;
																				
																				//}
																				
																			}else{
																				
																			}
																			
																		/*	if(me==null||you==null){
																				continue;
																				
																			}else{*/
																			//estimated RTT
																		/*	NodeRecord r = new NodeRecord(entry,me.distanceTo(you));
																			
																			
																			r.RTT=Entry.getValue().currentRTT;
																			r.error=Math.abs((r.coordDist-r.RTT)/r.RTT);
																			r.RTT_received=Entry.getValue().receivedRTT;
																			r.elapsedTime=Entry.getValue().receivedRTT;*/
																			//double RTT=Entry.getValue().currentRTT;
																			//sortedL.add(r);
																			if(RTT<=0){
																				RTT=Entry.getValue().elapsedTime;
																			}
																			resultVector.add(entry.toString()); //remote node
																			//resultVector		
																			double dist=me.distanceTo(you);
																			resultVector.add(dist);	//estimated RTT
																			
																			resultVector.add(RTT);      	//real RTT
																			//resultVector.add(r.RTT-r.coordDist);
																			resultVector.add(Math.abs((dist-RTT)/RTT));
																			resultVector.add(Entry.getValue().elapsedTime);
						
																
															}
															

															//Vector<String> sortedPerNode = new Vector<String>(1);
															//Iterator<NodeRecord> ier = sortedL.iterator();
																											
//																sortedPerNode.add((AddressFactory.create(ier.next().addr))
//																				.toString());
																//format: addr, estimated RTT, absolute error time, received RTT
																
															
															
															//double time=(System.nanoTime()-curTime)/1000000d;
															
															
														cbsortedNodes.call(ncResult,resultVector);
														//sortedL.clear();	
															
														 AllAddrs.clear();
														 resultVector.clear();
														}
														
														
														
													});

											
												
											}
										});
			
								break;
							}

							case ERROR: {
								// problem resolving one or more of the addresses
								log.warn(result.toString());
								cbsortedNodes.call(result, resultVector);
							}
						}
					}			
				});
				
			}
		});
		
		
	}
	/**
	 * estimates the closeness
	 * 
	 * @param remoteNodesMap
	 * @param cbsortedNodes
	 */
	public void sortNodesByDistances(Hashtable<String, Object> remoteNodesMap,
			CB1<Vector> cbsortedNodes) {
		ncManager.sortNodesByDistances(remoteNodesMap, -1, cbsortedNodes);
	}
	
	
	public void KNNBasedOnNetworkCoord(Hashtable<String, Object> remoteNodesMap,int K,
			CB1<Vector> cbsortedNodes) {
		ncManager.sortNodesByDistances(remoteNodesMap, K,cbsortedNodes);
	}
	
	/**
	 * constrained KNN search 
	 * @param target
	 * @param peers
	 * @param K
	 * @param cbDone
	 */
	public void queryKNearestNeighbors(final String target,final Vector<String> peer,final int K,
			final CB1<Map<String,Double>> cbDone){
			
		

						if(peer==null||peer.isEmpty()){
							cbDone.call(CBResult.ERROR(), null);
							return;
						}
						String[]peers=new String[peer.size()];
						for(int i=0;i<peer.size();i++){
							peers[i]=peer.get(i);
						}
						NNManager.queryKNearestNeighbors(target, peers, K, cbDone);
						
		
		
	}
	
	/**
	 * subset based KNN
	 * @param target
	 * @param peer
	 * @param K
	 * @param cbKNN
	 */
	public void constrainedKNN(final String target, final Vector<String> peer, final int K,final CB1<Vector<Object>> cbKNN){
		
		
		execExternalAPI.execute(new Runnable(){

			
			public void run() {
			

				final Vector<Object> vec=new Vector<Object>(2);
				if(target==null||peer==null||peer.isEmpty()){
					cbKNN.call(CBResult.ERROR(), vec);
				}else{
					
					int len=peer.size();
					final List<AddressIF> candidates =new ArrayList<AddressIF>(2);
					AddressIF TT=AddressFactory.createUnresolved(target, Ninaloader.COMM_PORT);
					for(int i=0;i<len;i++){
						candidates.add(AddressFactory.createUnresolved(peer.get(i), Ninaloader.COMM_PORT));
					}
					NNManager.queryKNearestNeighbors(TT, candidates, K, new CB1<List<NodesPair>>(){

						
						protected void cb(CBResult result, List<NodesPair> KNNs) {
							// TODO Auto-generated method stub
							
							switch (result.state) {
							case OK: {
								// stub
								System.out
										.println("constrained KNN branch results are returned");
								// yes, we've got KNN
								if (KNNs != null
										&& KNNs.size() > 0) {
									Iterator<NodesPair> ier = KNNs
											.iterator();
									int ind = 0;
									NodesPair nxtNode;
									
//									vec.add("######################");
//									vec.add(Ninaloader.me.toString());
//									vec.add("######################");
									
									
									while (ier.hasNext()) {
										nxtNode = ier
												.next();
										// myself
										/*if (nxtNode.startNode
												.equals(nxtNode.endNode)) {
											continue;
										}*/
										ind++;
										// otherwise
										System.out
												.println("The "
														+ ind
														+ "th NN: "
														+ nxtNode
																.toString());
										//map.put(nxtNode.startNode.toString(), Double.valueOf(nxtNode.rtt));
									
										vec.add(nxtNode.startNode.toString());
										
										vec.add(Double.valueOf(nxtNode.rtt));
										vec.add(Integer.valueOf(ind));
										vec.add(Double.valueOf(nxtNode.elapsedTime));
										
//										addrs
//												.add(nxtNode.startNode
//														.toString());
//										rtts
//												.add(Double
//														.valueOf(nxtNode.rtt));

									}

								}

								cbKNN.call(result,vec);
								log.info("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
								break;
							}
							case TIMEOUT:
							case ERROR: {
								log.warn(result.toString());
								cbKNN.call(result,vec);
								log.info("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
								break;
							}

							}
							vec.clear();
							candidates.clear();
						}		
						
					});
				}	
			
				
			}
		});

	}
	
	
	/**
	 * estimate latency
	 */
	public void estimateRTT(String ANode, String BNode,
			final CB1<Double> cbDistance) {
		String info = "\n\n\n\n===============\n$: In XML API: estimateRTT\n===============\n";
		log.info(info);
		ncManager.estimateRTT(ANode, BNode, cbDistance);
	}

	public static Vector<Double> getVectorFromCoord(Coordinate coord) {
		Vector<Double> listCoord = new Vector<Double>();
		double c[] = coord.asVectorFromZero(false).getComponents();
		for (int i = 0; i < c.length; i++) {
			listCoord.add(c[i]);
		}
		return listCoord;
	}
	
	/**
	 * the last dimension is the error
	 * @param coord
	 * @return
	 */
	public static Vector<Double> getVectorFromCoordAndError(Coordinate coord) {
		Vector<Double> listCoord = new Vector<Double>();
		double c[] = coord.asVectorFromZero(false).getComponents();
		for (int i = 0; i < c.length; i++) {
			listCoord.add(c[i]);
		}
		listCoord.add(coord.r_error);
		return listCoord;
	}
	public void getLocalCoord(CB1<Vector<Double>> cbLocalCoord) {
		Coordinate coord = ncManager.getLocalCoord();
		Vector<Double> listCoord = getVectorFromCoord(coord);
		cbLocalCoord.call(CBResult.OK(), listCoord);
	}

	
	/**
	 * 
	 * @param remoteNode
	 * @param cbRemoteCoords
	 */
	public void getRemoteCoords(final Vector<String> remoteNode,
			final CB1<Map<String, Object>> cbRemoteCoords) {
		
		
				
						List<AddressIF> remoteNodes = new ArrayList<AddressIF>();
						final Map<String, Object> resultMap = new Hashtable<String, Object>();
						for(int i=0;i<remoteNode.size();i++){
							remoteNodes.add(AddressFactory.createUnresolved(remoteNode.get(i),Ninaloader.COMM_PORT));
							// else {
							// dnsErrors.append("Could not resolve "+node);
							// }
						}

						ncManager.getRemoteCoords(
										remoteNodes,
										new CB2<Map<AddressIF, Coordinate>, String>() {
											
											protected void cb(
													CBResult ncResult,
													Map<AddressIF, Coordinate> addr2coord,
													String errorString) {
																					

												if(addr2coord!=null){
												log.info("responding to client addr2coord="
																+ addr2coord.size()+ " errorString=");
												for (Map.Entry<AddressIF, Coordinate> entry : addr2coord
														.entrySet()) {
													resultMap.put(
																	entry.getKey().toString(),
																	getVectorFromCoord(entry.getValue()));
												}
												
												}
												
												cbRemoteCoords.call(
														ncResult,
														resultMap);
											}
										});
						
		
	}
	
	
	public void getRemoteCoords(final Hashtable<String, Object> remoteNodesMap,
			final CB1<Map<String, Object>> cbRemoteCoords) {

					
						final String[] nodesStr = new String[remoteNodesMap.size()];
						int i = 0;
						for (Object obj : remoteNodesMap.keySet()) {
							String remoteNode = (String) obj;
							log.debug("getRemoteCoords[" + i + "]= " + remoteNode);
							nodesStr[i] = remoteNode;
							i++;
						}

						final Map<String, Object> resultMap = new Hashtable<String, Object>();

					/*	AddressFactory.createResolved(Arrays.asList(nodesStr),
								Ninaloader.COMM_PORT, new CB1<Map<String, AddressIF>>() {
									
									protected void cb(CBResult result,
											Map<String, AddressIF> addrMap) {
										switch (result.state) {
										case OK:

											// TODO jonathan: why do you handle OK and TIMEOUT
											// together and why is there an ERROR clause?
											// The way createResolved works is that it always
											// returns OK but only includes the hostnames
											// that resolved correctly in the map. Let me know
											// if you don't like this behaviour and I can change
											// it.
										case TIMEOUT: {*/

											// StringBuffer dnsErrors = new StringBuffer();
											final List<AddressIF> remoteNodes = new ArrayList<AddressIF>();

											for(i=0;i<nodesStr.length;i++){
												remoteNodes.add(AddressFactory.createUnresolved(nodesStr[i],Ninaloader.COMM_PORT));
												// else {
												// dnsErrors.append("Could not resolve "+node);
												// }
											}

											ncManager.getRemoteCoords(
															remoteNodes,
															new CB2<Map<AddressIF, Coordinate>, String>() {
																
																protected void cb(
																		CBResult ncResult,
																		Map<AddressIF, Coordinate> addr2coord,
																		String errorString) {
																	// TODO this does not work
																	// We want to return DNS
																	// failures to the client

																	// combine the two errors if
																	// there is already one in
																	// there
																	// caused through DNS
																	// resolution problem below												

																	if(addr2coord!=null&&!addr2coord.isEmpty()){
																	log.info("responding to client addr2coord="
																					+ addr2coord.size()+ " errorString=");
																	for (Map.Entry<AddressIF, Coordinate> entry : addr2coord
																			.entrySet()) {
																		resultMap.put(
																						entry.getKey().toString(),
																						getVectorFromCoord(entry.getValue()));
																	}
																	
																	}
																	
																	cbRemoteCoords.call(
																			ncResult,
																			resultMap);
																	
																	 resultMap.clear();
																	 remoteNodes.clear();
																}
															});
								/*			break;
										}

										case ERROR: {
											// problem resolving one or more of the addresses
											log.warn(result.toString());
											resultMap.put("result", result.toString());
											cbRemoteCoords.call(CBResult.OK(), resultMap);
										}
										}
									}
								});*/	
	

	}
	

	public void getRemoteCoord(String remoteNodeStr,
			final CB1<Vector> cbRemoteCoord) {
		
		
		AddressIF remoteNode=AddressFactory.createUnresolved(remoteNodeStr, Ninaloader.COMM_PORT);
	
					
					ncManager.getRemoteCoord(remoteNode, new CB1<Coordinate>() {
						
						protected void cb(CBResult ncResult,
								Coordinate remoteCoord) {
							//Map<String, Object> resultMap = new Hashtable<String, Object>();
							Vector vec=new Vector(2);
							switch (ncResult.state) {
							case OK: {
								Vector<Double> listCoord = getVectorFromCoord(remoteCoord);
								//resultMap.put("coord", listCoord);
								vec.add( listCoord);
								//resultMap.put("result", "Success");
								cbRemoteCoord.call(CBResult.OK(),vec);
								break;
							}
							case TIMEOUT:
							case ERROR: {
								log.warn(ncResult.toString());
								//resultMap.put("result", ncResult.toString());
								cbRemoteCoord.call(ncResult, vec);
								break;
							}
							}
						}
					});

	}

	public void getLocalError(CB1<Double> cbLocalError) {
		cbLocalError.call(CBResult.OK(), ncManager.getLocalError());
	}

	public void renewProxyCoord(String remoteNodeStr, int lease,
			final CB1<String> cbResult) {
		AddressFactory.createResolved(remoteNodeStr, new CB1<AddressIF>() {
			
			protected void cb(CBResult nsResult, AddressIF remoteNode) {
				switch (nsResult.state) {
				case OK: {
					log.debug("resolved node=" + remoteNode);
					String res = "";
					// ncManager.renewLeaseOnProxyCoord(remoteNode, lease);
					cbResult.call(CBResult.OK(), res);
					break;
				}
				case TIMEOUT:
				case ERROR: {
					log.warn(nsResult.toString());
					cbResult.call(nsResult, nsResult.toString());
					break;
				}
				}
			}
		});
	}

	
	
	
	
	protected void cb(CBResult result, AddressIF remoteAddr, String path,
			String requestData, CB2<String, byte[]> cbResponse) {

		Coordinate coord = ncManager.getLocalCoord();

		StringBuffer sb = new StringBuffer();

		sb.append(HTTP_COORD_BEGIN);

		// sb.append("<tr><td><em>Version:</em></td><td width=10></td><td>" +
		// Pyxida.VERSION + "</td></tr>\n");
		// sb.append("<tr><td><em>Uptime:</em></td><td width=10></td><td>" +
		// Pyxida.getUptimeStr() + "</td></tr>\n");

		sb.append("<tr><td>&nbsp;</td></tr>\n");

		sb
				.append("<tr><td><em>Primary Sys Coord:</em></td><td width=10></td><td>"
						+ ncManager.getLocalCoord() + "</td></tr>\n");
		sb
				.append("<tr><td><em>Primary Sys Coord Error:</em></td><td width=10></td><td>"
						+ POut.toString(ncManager.getLocalError())
						+ "</td></tr>\n");
		// sb.append("<tr><td><em>Primary App Coord:</em></td><td width=10></td><td>"
		// + ncManager.getStableCoord() + "</td></tr>\n");

		sb.append("<tr><td>&nbsp;</td></tr>\n");

		// sb.append("<tr><td><em>Secondary Sys Coord:</em></td><td width=10></td><td>"
		// + ncManager.getLocalSecondaryCoord() + "</td></tr>\n");
		// sb.append("<tr><td><em>Secondary Sys Coord Error:</em></td><td width=10></td><td>"
		// + POut.toString(ncManager.getLocalSecondaryError()) +
		// "</td></tr>\n");
		// sb.append("<tr><td><em>Secondary App Coord:</em></td><td width=10></td><td>"
		// + ncManager.getStableSecondaryCoord() + "</td></tr>\n");

		sb.append(HTTP_COORD_MIDDLE);

		Set<AddressIF> upNeighbours = ncManager.getUpNeighbours();
		sb.append("<tr><td align=\"center\"><em>Up Neighbours ("
				+ upNeighbours.size() + ")</em></td></tr>\n");
		for (AddressIF upNeighbour : upNeighbours) {
			sb.append("<tr><td align=\"left\"><a href=\"http://"
					+ upNeighbour.getHostname() + ":" + XMLRPC_API.API_PORT
					+ "/\">");
			sb.append(upNeighbour.getHostname() + ":" + XMLRPC_API.API_PORT
					+ "</a></td></tr>\n");
		}

		sb.append("<tr><td>&nbsp;</td></tr>\n");

		Set<AddressIF> pendingNeighbours = ncManager.getPendingNeighbours();
		sb.append("<tr><td align=\"center\"><em>Pending Neighbours ("
				+ pendingNeighbours.size() + ")</em></td></tr>\n");
		for (AddressIF pendingNeighbour : pendingNeighbours) {
			sb.append("<tr><td align=\"left\"><a href=\"http://"
					+ pendingNeighbour.getHostname() + ":"
					+ XMLRPC_API.API_PORT + "/\">");
			sb.append(pendingNeighbour.getHostname() + ":"
					+ XMLRPC_API.API_PORT + "</a></td></tr>\n");
		}

		sb.append("<tr><td>&nbsp;</td></tr>\n");

		Set<AddressIF> downNeighbours = ncManager.getDownNeighbours();
		sb.append("<tr><td align=\"center\"><em>Down Neighbours ("
				+ downNeighbours.size() + ")</em></td></tr>\n");
		for (AddressIF downNeighbour : downNeighbours) {
			sb.append("<tr><td align=\"left\"><a href=\"http://"
					+ downNeighbour.getHostname() + ":" + XMLRPC_API.API_PORT
					+ "/\">");
			sb.append(downNeighbour.getHostname() + ":" + XMLRPC_API.API_PORT
					+ "</a></td></tr>\n");
		}

		sb.append(HTTP_COORD_END);

		cbResponse
				.call(result, "text/html", NetUtil.toHTTPBytes(sb.toString()));
	}

	
	protected void cb(CBResult result, AddressIF arg1, String arg2,
			String arg3, Map<String, String> arg4, Map<String, String> arg5,
			String arg6, CB2<String, byte[]> arg7) {
		// TODO Auto-generated method stub
		
	}



}
