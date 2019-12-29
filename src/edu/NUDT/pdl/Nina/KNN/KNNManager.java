package edu.NUDT.pdl.Nina.KNN;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;

import edu.NUDT.pdl.Nina.MeasureComm;
import edu.NUDT.pdl.Nina.Ninaloader;
import edu.NUDT.pdl.Nina.Clustering.HSH_Manager;
import edu.NUDT.pdl.Nina.Distance.NodesPairComp;
import edu.NUDT.pdl.Nina.KNN.AbstractNNSearchManager.ClosestReqHandler;
import edu.NUDT.pdl.Nina.KNN.AbstractNNSearchManager.CollectHandler;
import edu.NUDT.pdl.Nina.KNN.AbstractNNSearchManager.CoordGossipHandler;
import edu.NUDT.pdl.Nina.KNN.AbstractNNSearchManager.DatHandler;
import edu.NUDT.pdl.Nina.KNN.AbstractNNSearchManager.FinKNNHandler;
import edu.NUDT.pdl.Nina.KNN.AbstractNNSearchManager.GossipHandler;
import edu.NUDT.pdl.Nina.KNN.AbstractNNSearchManager.RelativeCoordinateHandler;
import edu.NUDT.pdl.Nina.KNN.AbstractNNSearchManager.ResponseObjCommCB;
import edu.NUDT.pdl.Nina.KNN.AbstractNNSearchManager.UpdateReqHandler;
import edu.NUDT.pdl.Nina.KNN.AbstractNNSearchManager.askcachedHistoricalNeighborsHandler;
import edu.NUDT.pdl.Nina.KNN.AbstractNNSearchManager.updateTargetHandler;
import edu.NUDT.pdl.Nina.KNN.askcachedHistoricalNeighbors.askcachedHistoricalNeighborsRequestMsg;
import edu.NUDT.pdl.Nina.StableNC.lib.Coordinate;
import edu.NUDT.pdl.Nina.StableNC.lib.EWMAStatistic;
import edu.NUDT.pdl.Nina.StableNC.lib.NCClient;
import edu.NUDT.pdl.Nina.StableNC.lib.RemoteState;
import edu.NUDT.pdl.Nina.StableNC.lib.WindowStatistic;
import edu.NUDT.pdl.Nina.StableNC.nc.CoordGossipRequestMsg;
import edu.NUDT.pdl.Nina.StableNC.nc.StableManager;
import edu.NUDT.pdl.Nina.util.bloom.Apache.Filter;
import edu.NUDT.pdl.pyxida.ping.PingManager;
import edu.harvard.syrah.prp.SortedList;
import edu.harvard.syrah.sbon.async.Barrier;
import edu.harvard.syrah.sbon.async.CBResult;
import edu.harvard.syrah.sbon.async.Config;
import edu.harvard.syrah.sbon.async.EL;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB0;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB1;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB2;
import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjCommIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjCommRRCB;

public class KNNManager extends AbstractNNSearchManager {

	// braches of the KNN search
	public static int MultipleRestart = Integer.parseInt(Config.getProperty("MultipleRestart", "1"));

	public static long MsxMSToRemove = 10 * 60 * 1000;

	// search direction
	public static int FarthestSearch = 1;
	public static int ClosestSearch = 0;

	public static int NeighborsForTargetCoordInit = Integer
			.parseInt(Config.getProperty("NeighborsForTargetCoordInit", "1"));;

	// SubSetSearchManager subManager;

	// for KNN query CB
	// Map<AddressIF,CB1<Set<NodesPair>>> cachedKNNCB=new
	// HashMap<AddressIF,CB1<Set<NodesPair>>>(10);
	// for forbidden nodes in querying process
	// Map<AddressIF, Vector> forbiddenSet =new HashMap<AddressIF, Vector>(10);
	// for already registered NNs, target Original
	// for query cache

	// -----------------------------------------------------------------

	public KNNManager(ObjCommIF _GossipComm, ObjCommIF _comm, ObjCommIF _MeasureComm, PingManager pingManager,
			StableManager ncManager) {
		super(_GossipComm, _comm, _MeasureComm, pingManager, ncManager);

		// ElapsedKNNSearchTime = new EWMAStatistic();
		// subManager = _subManager;
	}

	public void init(final CB0 cbDone) {
		super.init(new CB0() {

			protected void cb(CBResult result) {

				switch (result.state) {
				case OK: {
					// TODO Auto-generated method stub
					log.debug("$: MeridianManager Initialized");

					// non-ring node
					if (IsNonRingNode) {
						MClient.initNonRing();
					}

					if (measureComm != null) {
					} else {
						log.warn("measureComm.getComm() is null!");

					}

					// register the askcachedHistoricalNeighborsRequest handler
					gossipComm.registerMessageCB(askcachedHistoricalNeighborsRequestMsg.class,
							new askcachedHistoricalNeighborsHandler());
					// get the coordinate of neighbors
					gossipComm.registerMessageCB(CoordGossipRequestMsg.class, new CoordGossipHandler());
					gossipComm.registerMessageCB(KNNGossipRequestMsg.class, new GossipHandler());

					// use the measurement com
					measureComm.registerMessageCB(UpdateRequestMsg.class, new updateTargetHandler());
					// ==================================================
					// data request
					measureComm.registerMessageCB(DatRequestMsg.class, new DatHandler());

					measureComm.registerMessageCB(AnswerUpdateRequestMsg.class, new UpdateReqHandler());

					// comm.registerMessageCB(RelativeCoordinateRequestMsg.class,
					// new RelativeCoordinateHandler());
					comm.registerMessageCB(FinFarthestRequestMsg.class, new FinKNNHandler());

					// on ring nodes
					// if (!IsNonRingNode) {

					comm.registerMessageCB(RepeatedNNRequestMsg.class, new RepeatedNNReqHandler());
					comm.registerMessageCB(TargetLocatedRequestMsg.class, new TargetLockReqHandler());
					comm.registerMessageCB(CompleteNNRequestMsg.class, new CompleteNNReqHandler());
					comm.registerMessageCB(WithdrawRequestMsg.class, new WithdrawReqHandler());

					comm.registerMessageCB(ClosestRequestMsg.class, new ClosestReqHandler());

					comm.registerMessageCB(FarthestRequestMsg.class, new FarthestReqHandler());

					comm.registerMessageCB(FinKNNRequestMsg.class, new FinKNNRequestMsgHandler());
					// =====================================
					/*
					 * //constrained KNN query
					 * comm.registerMessageCB(SubSetClosestRequestMsg.class, new
					 * SubSetClosestReqHandler());
					 * comm.registerMessageCB(CompleteSubsetSearchRequestMsg.
					 * class, new SubSetCompleteNNReqHandler() );
					 * comm.registerMessageCB(SubSetTargetLocatedRequestMsg.
					 * class, new SubSetTargetLockReqHandler() );
					 * comm.registerMessageCB(RepeatedSubSetRequestMsg.class,
					 * new SubSetRepeatedNNReqHandler() );
					 */
					/*
					 * comm.registerMessageCB(QueryKNNRequestMsg.class, new
					 * QueryKNNReqHandler());
					 * comm.registerMessageCB(FinKNNRequestMsg.class, new
					 * FinKNNRequestMsgHandler());
					 */

					comm.registerMessageCB(KNNCollectRequestMsg.class, new CollectHandler());

					// =====================================
					// constrained KNN query
					comm.registerMessageCB(SubSetClosestRequestMsg.class, new SubSetClosestReqHandler());
					comm.registerMessageCB(CompleteSubsetSearchRequestMsg.class, new SubSetCompleteNNReqHandler());
					comm.registerMessageCB(SubSetTargetLocatedRequestMsg.class, new SubSetTargetLockReqHandler());
					comm.registerMessageCB(RepeatedSubSetRequestMsg.class, new SubSetRepeatedNNReqHandler());
					// MClient.init();

					comm.registerMessageCB(KNNCollectRequestMsg.class, new CollectHandler());

					comm.registerMessageCB(QueryKNNRequestMsg.class, new QueryKNNReqHandler());
					comm.registerMessageCB(FinKNNRequestMsg.class, new FinKNNRequestMsgHandler());

					execKNN.execute(MClient);

					log.info("####################\n$: Register handlers\n####################");

					// log.debug("$: KNNManager Initialized");
					// init Meridian
					cbDone.callOK();
					break;
				}
				default: {
					cbDone.call(result);
					break;
				}

				}

			}

		});

		/*
		 * MClient.performGossip(pendingNeighbors, true,new CB0() { protected
		 * void cb(CBResult result) { switch (result.state) { case OK: { //
		 * Initialise the Rings
		 * 
		 * cbDone.call(result); break; } default: { cbDone.call(result); } } }
		 * });
		 */

	}

	/**
	 * nodes that are NOT on rings use this method ask a node residing the
	 * rings, we are not necessarily a node on the ring
	 * 
	 * @param peeringNeighbor
	 */

	public void queryKNN(final String peeringNeighbor, final String target, final int K,
			final CB1<List<NodesPair>> cbDone) {

		String[] nodes = { peeringNeighbor, target };

		AddressFactory.createResolved(Arrays.asList(nodes), Ninaloader.COMM_PORT, new CB1<Map<String, AddressIF>>() {

			protected void cb(CBResult result, Map<String, AddressIF> addrMap) {
				// TODO Auto-generated method stub
				switch (result.state) {
				case OK: {

					AddressIF remoteAddr = addrMap.get(peeringNeighbor);
					// must not me
					assert (!remoteAddr.equals(Ninaloader.me));

					final AddressIF targetAddr = addrMap.get(target);

					final int version = Ninaloader.random.nextInt();

					QueryKNNRequestMsg msg = new QueryKNNRequestMsg(Ninaloader.me, targetAddr, K, version);
					comm.sendRequestMessage(msg, remoteAddr, new ObjCommRRCB<QueryKNNResponseMsg>() {

						protected void cb(CBResult result, QueryKNNResponseMsg arg1, AddressIF arg2, Long arg3) {
							switch (result.state) {
							case OK: {
								log.info("Successfully send out KNN query");
								long curMs = System.currentTimeMillis();

								// record the KNNs
								if (!pendingKNNs4NonRings.containsKey(targetAddr)) {
									Vector<cachedKNNs4NonRingNodes> vec = new Vector<cachedKNNs4NonRingNodes>(1);
									vec.add(new cachedKNNs4NonRingNodes(version, targetAddr, cbDone, curMs));
									pendingKNNs4NonRings.put(targetAddr, vec);

								} else {
									Vector<cachedKNNs4NonRingNodes> vec = pendingKNNs4NonRings.get(targetAddr);
									vec.add(new cachedKNNs4NonRingNodes(version, targetAddr, cbDone, curMs));
									pendingKNNs4NonRings.put(targetAddr, vec);
								}

								break;
							}
							case TIMEOUT:
							case ERROR: {
								// error
								log.info("FAILED to send out KNN query! " + result.toString());
								break;
							}

							}

						}

					});

					break;
				}
				case TIMEOUT:
				case ERROR: {
					log.error("Could not resolve my address: " + result.what);
					// cbDone.call(result, addrs, estRTTs);
					break;
				}

				}

			}

		});

	}

	/**
	 * receive
	 * 
	 * @author ericfu
	 * 
	 */
	public class QueryKNNReqHandler extends ResponseObjCommCB<QueryKNNRequestMsg> {

		protected void cb(CBResult result, QueryKNNRequestMsg arg1, AddressIF arg2, Long arg3, CB1<Boolean> arg4) {
			// TODO Auto-generated method stub

			final AddressIF nonRingNode = arg1.from;
			final AddressIF target = arg1.target;
			final int version = arg1.version;

			queryKNearestNeighbors(arg1.target, arg1.K, new CB1<List<NodesPair>>() {

				protected void cb(CBResult result, List<NodesPair> knns) {
					// TODO Auto-generated method stub

					// send KNN results to query nodes
					FinKNNRequestMsg msg = new FinKNNRequestMsg(Ninaloader.me, target, knns, version);
					comm.sendRequestMessage(msg, nonRingNode, new ObjCommRRCB<FinKNNResponseMsg>() {

						protected void cb(CBResult result, FinKNNResponseMsg finKNN, AddressIF arg2, Long arg3) {
							// TODO Auto-generated method stub
							switch (result.state) {
							case OK: {
								log.info("Successfully finish KNN queries from non-ring nodes");
								break;
							}
							case TIMEOUT:
							case ERROR: {
								// error
								log.info("FAILED to send out KNN query! " + result.toString());
								break;
							}
							}

						}

					});
				}

			});

			QueryKNNResponseMsg msg = new QueryKNNResponseMsg(Ninaloader.me);
			sendResponseMessage("QueryKNNRequest", nonRingNode, msg, arg1.getMsgId(), null, arg4);

		}

	}

	public class FinKNNRequestMsgHandler extends ResponseObjCommCB<FinKNNRequestMsg> {

		protected void cb(CBResult result, FinKNNRequestMsg arg1, AddressIF arg2, Long arg3, CB1<Boolean> arg4) {
			// TODO Auto-generated method stub

			switch (result.state) {
			case OK: {
				AddressIF target = arg1.target;
				int version = arg1.version;
				if (pendingKNNs4NonRings.containsKey(target)) {

					Vector<cachedKNNs4NonRingNodes> tmpVec = pendingKNNs4NonRings.get(target);
					Iterator<cachedKNNs4NonRingNodes> ier = tmpVec.iterator();
					while (ier.hasNext()) {
						cachedKNNs4NonRingNodes rec = ier.next();

						if (rec.version == version) {
							// found
							// log.debug("FOUND the pending KNN callbacks!");

							rec.cbDone.call(CBResult.OK(), arg1.NearestNeighborIndex);
							ier.remove();
							break;
						}
					}

				}

				// log.info("Successfully finish KNN queries!");
				break;
			}
			case TIMEOUT:
			case ERROR: {
				// error
				log.info("FAILED to receive KNN query! " + result.toString());
				break;
			}

			}

			FinKNNResponseMsg msg2 = new FinKNNResponseMsg(Ninaloader.me);
			sendResponseMessage("FinKNNRequest", arg1.from, msg2, arg1.getMsgId(), null, arg4);

		}

	}

	/**
	 * the interface to query the knn
	 */
	public void queryKNearestNeighbors(final AddressIF target, final int expectedKNN,
			final CB1<List<NodesPair>> cbDone) {

		if (MClient.instance.g_rings.NodesCoords.containsKey(target)) {
			Coordinate targetCoordinate = MClient.instance.g_rings.NodesCoords.get(target).makeCopy();
			log.info("cached target Coord: " + targetCoordinate.toString());
			queryKNearestNeighbors_original(target, targetCoordinate, expectedKNN, cbDone);
		} else if (target.equals(Ninaloader.me)) {

			Coordinate targetCoordinate = MClient.primaryNC.getSystemCoords();
			log.info("my target Coord: " + targetCoordinate.toString());
			queryKNearestNeighbors_original(target, targetCoordinate, expectedKNN, cbDone);
		} else {
			// not cached, not mine
			this.initTargetCoordinate(target, new CB1<Coordinate>() {
				protected void cb(CBResult result, Coordinate targetCoordinate) {
					// TODO Auto-generated method stub
					switch (result.state) {
					case OK: {
						log.info("initialized target Coord: " + targetCoordinate.toString());
						queryKNearestNeighbors_original(target, targetCoordinate.makeCopy(), expectedKNN, cbDone);
						break;
					}
					case ERROR:
					case TIMEOUT: {
						log.warn("can not compute the target's coordinate");
						cbDone.call(result, null);

						break;
					}
					}

				}

			});
		}

	}

	/**
	 * nodes on rings use this method;
	 * 
	 * @param targetCoordinate
	 */

	public void queryKNearestNeighbors_original(final AddressIF target, Coordinate targetCoordinate,
			final int expectedKNN, final CB1<List<NodesPair>> cbDone) {

		int branches = KNNManager.MultipleRestart;
		final Set<AddressIF> randNodes = new HashSet<AddressIF>(10);

		// rings
		MClient.instance.g_rings.getNodesWithMaximumNonEmptyRings(randNodes, branches);

		if (randNodes.contains(target)) {
			log.debug("$: we remove the target, then we start the KNN search process! ");
			randNodes.remove(target);
		}

		int realNodeNo = randNodes.size();

		log.debug("!!!!!!!!!!!!!!!!!!!!!!!\nTotal branches: " + realNodeNo + "\n!!!!!!!!!!!!!!!!!!!!!!!");

		final long timeStamp = System.currentTimeMillis();

		if (!randNodes.isEmpty()) {
			final Barrier barrierUpdate = new Barrier(true);
			// generate the KNN's seed
			final long seed = Ninaloader.random.nextLong();

			// save the CB
			// ========================================
			long timer = System.currentTimeMillis();
			if (!cachedCB.containsKey(Long.valueOf(seed))) {
				// Vector<CBcached> cached = new Vector<CBcached>(
				// 5);
				CBcached cb = new CBcached(cbDone, barrierUpdate, seed, timer);

				cb.ExpectedNumber = expectedKNN;
				cachedCB.put(Long.valueOf(seed), cb);
			}

			// =========================================
			Iterator<AddressIF> ier = randNodes.iterator();
			for (int index = 0; index < realNodeNo; index++) {

				final AddressIF dest = ier.next();
				// test dest
				if (dest.equals(target)) {
					continue;
				}

				// version number
				final int version = Ninaloader.random.nextInt();
				// added K neighbors
				Set<NodesPair> NearestNeighborIndex = new HashSet<NodesPair>(10);
				// save the target into cache, to avoid target being select as
				// candidate KNN node
				// Note: NearestNeighborIndex-1=k means there are k nodes

				NearestNeighborIndex.add(new NodesPair(target, target, -1));

				// SubSetManager forbiddenFilter = new SubSetManager();
				// add the target
				// forbiddenFilter.insertPeers2Subset(target);

				ClosestRequestMsg msg22 = new ClosestRequestMsg(Ninaloader.me, Ninaloader.me, target, expectedKNN,
						NearestNeighborIndex, version, seed, null, SubSetManager.getEmptyFilter(),
						SubSetManager.getEmptyFilter());

				msg22.hasRepeated = 0;
				// first find the farthest node
				final FarthestRequestMsg FMsg = new FarthestRequestMsg(Ninaloader.me, target, Ninaloader.me, msg22);

				msg22.targetCoordinates = null;
				FMsg.targetCoordinates = targetCoordinate.makeCopy();

				FMsg.CMsg.totalSendingMessages = 0;
				FMsg.totalSendingMessages = 0;
				// targetCoordinate.clear();
				// send to a random neighbor in Set upNeighbors
				// select a node in the ring

				// log.debug("###############\n$: send out" + index
				// + "th KNN query of " + Ninaloader.me + " to Neighbor "
				// + dest + "\n###############");
				barrierUpdate.fork();
				// barrierUpdate.fork();

				comm.sendRequestMessage(FMsg, dest, new ObjCommRRCB<FarthestResponseMsg>() {

					protected void cb(CBResult result, final FarthestResponseMsg responseMsg, AddressIF node, Long ts) {
						switch (result.state) {
						case OK: {
							log.debug("K Nearest Request received by new query node: ");
							// cdDone.call(result);
							// save the callbacks
							// add into a list

							break;
						}
						case ERROR:
						case TIMEOUT: {
							cbDone.call(result, null); // the returned
							// map is null
							break;
						}
						}
						// ----------------------
					}
					// --------------------------
				});

			}

			randNodes.clear();
			long interval = 10 * 1000;
			EL.get().registerTimerCB(barrierUpdate, new CB0() {

				@SuppressWarnings("unchecked")
				protected void cb(CBResult result) {
					String errorString;
					// merge the results

					// if it does not appear, add
					// long curTime = System.currentTimeMillis();
					// double time=(curTime-)/1000;
					// log.debug("\n@@:\n All KNN branches are reported after
					// "+time+" seconds\n\n");

					CBcached np = cachedCB.get(Long.valueOf(seed));

					// odd, we can not find the record
					if (np == null) {
						log.warn("can not find the record!");
						return;
					}

					int k = np.ExpectedNumber;

					int totalSendingMessage = np.totalSendingMessage;

					if (np != null) {

						long curTimer = System.currentTimeMillis();
						long elapsedTime = curTimer - np.timeStamp;
						// ElapsedKNNSearchTime.add(elapsedTime);

						List<NodesPair> nps = new ArrayList<NodesPair>(5);
						// List<AddressIF> tmpKNN = new ArrayList<AddressIF>(5);

						// List rtts=new ArrayList(5);
						Map<AddressIF, RemoteState<AddressIF>> maps = new HashMap<AddressIF, RemoteState<AddressIF>>(5);

						// copy of the original hashset
						Set<NodesPair> nodePairs = new HashSet<NodesPair>(np.nps);
						// iterate, we use the latency as the hash key
						if (nodePairs != null) {
							Iterator<NodesPair> ierNp = nodePairs.iterator();
							while (ierNp.hasNext()) {
								NodesPair cachedNP = ierNp.next();

								// skip myself
								if (cachedNP.startNode.equals(target)) {
									continue;
								}

								if (cachedNP.rtt < 0) {
									continue;
								}

								if (!maps.containsKey((AddressIF) cachedNP.startNode)) {
									RemoteState<AddressIF> cached = new RemoteState<AddressIF>(
											(AddressIF) cachedNP.startNode);

									cached.elapsedHops = cachedNP.ElapsedSearchHops;

									cached.addSample(cachedNP.rtt);
									maps.put((AddressIF) cachedNP.startNode, cached);

								} else {
									// averaged
									RemoteState<AddressIF> cached = maps.get(cachedNP.startNode);
									cached.addSample(cachedNP.rtt);
									maps.put((AddressIF) cachedNP.startNode, cached);
								}
							}
						}
						// }

						SortedList<Double> sortedL = new SortedList<Double>(new DoubleComp());
						HashMap<Double, AddressIF> unSorted = new HashMap<Double, AddressIF>(2);

						Iterator<Entry<AddressIF, RemoteState<AddressIF>>> ierMap = maps.entrySet().iterator();
						while (ierMap.hasNext()) {
							Entry<AddressIF, RemoteState<AddressIF>> tmp = ierMap.next();
							unSorted.put(Double.valueOf(tmp.getValue().getSample()), tmp.getKey());
							sortedL.add(Double.valueOf(tmp.getValue().getSample()));
						}

						if (sortedL.size() > 0) {
							// yes the results

							Iterator<Double> ierRTT = sortedL.iterator();

							int counter = 0;
							while (ierRTT.hasNext() && (counter < k)) {
								Double item = ierRTT.next();

								AddressIF tmpNode = unSorted.get(item);
								// target
								if (tmpNode.equals(target)) {
									continue;
								}

								// add the node
								nps.add(new NodesPair(tmpNode, null, item.doubleValue(), elapsedTime,
										maps.get(tmpNode).elapsedHops, totalSendingMessage));

								counter++;
							}

							//
							log.info("KNN Number: " + nps.size() + " required: " + k + ", elapsedTime: " + elapsedTime);

							cbDone.call(CBResult.OK(), nps);

							// remove the callbacks
							cachedCB.remove(Long.valueOf(seed));

							// cachedCB.get(target).clear();
							// cachedCB.remove(target); //remove the callback
							// registered for arg1.target
							// nps.clear();
							sortedL.clear();
							unSorted.clear();
							maps.clear();

						} else {
							log.warn("No RTT measurements!");
							randNodes.clear();
							cbDone.call(CBResult.ERROR(), null);
						}
					} else {
						log.warn("NO cachedCB!");
						randNodes.clear();
						cbDone.call(CBResult.ERROR(), null);
					}

				}
			});

		} // index
		else {
			randNodes.clear();
			cbDone.call(CBResult.ERROR(), null);
		}
	}

	/**
	 * init the target's coordinate
	 * 
	 * @param target
	 * @return
	 */
	private boolean initTargetCoordinate(final AddressIF target, final CB1<Coordinate> cbDone) {
		// TODO Auto-generated method stub

		// init the rtt measurements
		askCoordinate(target, new CB1<Coordinate>() {

			@Override
			protected void cb(CBResult result, Coordinate arg1) {
				// TODO Auto-generated method stub
				switch (result.state) {
				case OK: {
					// return immediately
					cbDone.call(result, arg1);
					break;
				}
				case TIMEOUT:
				case ERROR: {

					int branches = NeighborsForTargetCoordInit;
					int realSize = MClient.instance.g_rings.NodesCoords.size();
					if (realSize < branches) {
						log.warn("too few cached nodes with coordinates! required " + branches + ", but " + realSize);
						cbDone.call(CBResult.ERROR(), null);

					} else {

						final Set<AddressIF> randNodes = new HashSet<AddressIF>(10);
						// rings
						MClient.instance.g_rings.getNodesWithMaximumNonEmptyRings(randNodes, branches);

						final Vector<AddressIF> ringMembers = new Vector<AddressIF>(10);
						ringMembers.addAll(randNodes);
						randNodes.clear();

						TargetProbes(ringMembers, target, new CB1<String>() {

							protected void cb(CBResult ncResult, String errorString) {
								switch (ncResult.state) {
								case OK: {
									// log.debug("$: Target Probes are issued
									// for KNN");
								}
								case ERROR:
								case TIMEOUT: {
									break;
								}
								}
							}
						}, new CB2<Set<NodesPair>, String>() {

							protected void cb(CBResult ncResult, Set<NodesPair> nps2, String errorString) {

								Coordinate targetCoord = new Coordinate(myNCDimension);// new
																						// Coordinate(MClient.primaryNC.sys_coord);
								targetCoord.reset();

								switch (ncResult.state) {
								case OK: {

									int repeated = 15;
									// TODO: update the target's coordinate
									for (int i = 0; i < repeated; i++) {
										Iterator<NodesPair> ier = nps2.iterator();
										while (ier.hasNext()) {
											NodesPair rec = ier.next();
											if (rec.rtt <= 0) {
												continue;
											}
											AddressIF remote = (AddressIF) rec.startNode;
											if (MClient.instance.g_rings.NodesCoords.containsKey(remote)) {
												Coordinate refCoord = MClient.instance.g_rings.NodesCoords.get(remote);
												if (refCoord != null && targetCoord != null) {
													targetCoord = NCClient.updateProxyTargetCoordinate(target,
															targetCoord, targetCoord.r_error, refCoord,
															refCoord.r_error, rec.rtt);
												}
											}
										}
									}
									log.info("Target coordinate's error: " + targetCoord.r_error);
									cbDone.call(ncResult, targetCoord);
									break;
								}
								case TIMEOUT:
								case ERROR: {
									cbDone.call(ncResult, targetCoord);
									break;
								}
								}
							}
						});

					}

					break;
				}

				}
			}

		});

		return true;
	}

	Map<AddressIF, Set<NodesPair>> cachedSortedNN = new HashMap<AddressIF, Set<NodesPair>>(10);

	/**
	 * sort a list of nodes
	 */

	public void sortNodesByDistances(final Vector<AddressIF> addrs, int N, final CB1<Set<NodesPair>> cbDone) {
		// TODO sort nodes in addrs according to the distance

		// TODO: add estimate the number of nodes

		// query N nearest neighbors for each node in addrs
		// barriers
		if (addrs != null && !addrs.isEmpty()) {
			Iterator<AddressIF> ier = addrs.iterator();
			final Barrier barrierSort = new Barrier(true); // sort

			while (ier.hasNext()) {
				final AddressIF target = ier.next();
				barrierSort.fork();

				queryKNearestNeighbors(target, N, new CB1<List<NodesPair>>() {

					protected void cb(CBResult result, List<NodesPair> arg1) {
						// TODO Auto-generated method stub
						switch (result.state) {
						case OK: {
							// clear cache, and update
							if (arg1 != null && !arg1.isEmpty()) {
								if (cachedSortedNN.containsKey(target)) {
									cachedSortedNN.remove(target);
								}
								Set<NodesPair> cpy = new HashSet<NodesPair>(10);
								cpy.addAll(arg1);
								cachedSortedNN.put(target, cpy);
							}

						}
						case ERROR:
						case TIMEOUT: {

							break;
						}
						}
						barrierSort.join();
					}

				});

			}
			// ----------------------------------------------
			long interval = 2 * 1000;
			EL.get().registerTimerCB(barrierSort, interval, new CB0() {

				protected void cb(CBResult result) {
					String errorString;
					Set<NodesPair> tmp = new HashSet<NodesPair>(10);
					Iterator<AddressIF> ier = addrs.iterator();
					while (ier.hasNext()) {
						tmp.addAll(cachedSortedNN.get(ier.next()));
					}
					log.debug("$@ @ Sort Completed");
					cbDone.call(CBResult.OK(), tmp);
				}
			});

		}
		cbDone.call(CBResult.ERROR(), null);
	}

	/**
	 * current search process
	 * 
	 * @param msg3
	 */
	public void returnKNN2Origin(final CompleteNNRequestMsg msg3) {

		//

		boolean stop = false;
		// end the knn search process

		float thresholdOfKNN = 0.8f;
		// TODO: avoid the loop process ??
		if ((msg3.K > msg3.getNearestNeighborIndex().size() - 1) && msg3.hasRepeated < allowedRepeated) {
			// restart the search process
			// forbidden nodes
			Set<NodesPair> KNNsIndex = msg3.getNearestNeighborIndex();

			int branches = MultipleRestart;
			// final List<AddressIF> randNodes = new ArrayList<AddressIF>(10);

			// rings
			// MClient.instance.g_rings.getNodesWithMaximumNonEmptyRings(KNNsIndex,randNodes,
			// branches);

			Set<AddressIF> candidates = new HashSet<AddressIF>(2);
			candidates.addAll(MClient.instance.g_rings.NodesCoords.keySet());
			Iterator<NodesPair> ier = msg3.getNearestNeighborIndex().iterator();
			while (ier.hasNext()) {
				NodesPair rec = ier.next();
				AddressIF node = (AddressIF) rec.startNode;
				if (candidates.contains(node)) {
					candidates.remove(node);
				}
			}

			final Set<AddressIF> randNodes = MClient.instance.g_rings.getKFarthestPeerFromListWithCoord(msg3.target,
					msg3.myCoord, candidates, 5);

			int realNodeNo = randNodes.size();

			if (realNodeNo > 0) {

				// Collections.shuffle(randNodes);
				AddressIF dest = randNodes.iterator().next();

				ClosestRequestMsg _CMsg = CompleteNNRequestMsg.makeCopy(msg3);
				_CMsg.targets = msg3.targets;
				_CMsg.target = msg3.target;
				_CMsg.targetCoordinates = msg3.myCoord.makeCopy();
				// repeated
				_CMsg.hasRepeated = msg3.hasRepeated;
				_CMsg.hasRepeated++;

				_CMsg.totalSendingMessages = msg3.totalSendingMessages + 1;
				_CMsg.addCurrentHop(Ninaloader.me);

				_CMsg.root = AddressFactory.create(dest);

				/*
				 * // first find the farthest node, set the target address
				 * FarthestRequestMsg FMsg = new
				 * FarthestRequestMsg(Ninaloader.me, msg3.target, Ninaloader.me,
				 * _CMsg);
				 * 
				 * FMsg.target=msg3.target; FMsg.targets=msg3.targets;
				 * FMsg.targetCoordinates=msg3.myCoord.makeCopy(); //add current
				 * hop FMsg.HopRecords.add(Ninaloader.me);
				 * 
				 * FMsg.totalSendingMessages=msg3.totalSendingMessages+1;
				 */
				// send to a random neighbor in Set upNeighbors
				// select a node in the ring

				// barrierUpdate.fork();

				comm.sendRequestMessage(_CMsg, dest, new ObjCommRRCB<ClosestResponseMsg>() {

					protected void cb(CBResult result, final ClosestResponseMsg responseMsg, AddressIF node, Long ts) {
						switch (result.state) {
						case OK: {
							log.debug("K Nearest Request received by new query node: ");
							// cdDone.call(result);
							// save the callbacks
							// add into a list
							msg3.clear();
							break;
						}
						case ERROR:
						case TIMEOUT: {
							// we have not succeeded!, we have to return
							// TODO:
							normalEndKNNprocess(msg3);
							// map is null
							break;
						}
						}
						// ----------------------
						// msg3.clear();
					}
					// --------------------------
				});
				/*
				 * if(FMsg!=null){ FMsg=null;
				 * 
				 * }
				 */

				stop = true;
			} else {
				log.warn("we have no repeated neighbors to continue the search process");
				stop = false;
			}
		}

		if (stop) {
			// we can stop the answering process, since we have to reset the
			// search process
			return;
		} else {
			log.warn("has restarted " + msg3.hasRepeated);
			// end it
			normalEndKNNprocess(msg3);
		}

	}

	void normalEndKNNprocess(CompleteNNRequestMsg msg3) {

		// I should not work, since I have been a KNNer
		if (!Ninaloader.me.equals(msg3.OriginFrom)) {

			log.debug("!!!!!!!!!!!!!!!!!!!!!!\nComplete @ TargetLockReqHandler\n!!!!!!!!!!!!!!!!!!!!!!");
			// TODO: after the normal search process, we add the extra search
			// steps here

			msg3.totalSendingMessages += 1;
			// query completed, send msg to query nodes
			// send the result
			comm.sendRequestMessage(msg3, msg3.OriginFrom, new ObjCommRRCB<CompleteNNResponseMsg>() {

				protected void cb(CBResult result, final CompleteNNResponseMsg responseMsg, AddressIF node, Long ts) {
					switch (result.state) {
					case OK: {

						// acked by original query node
						// cdDone.call(result);
						break;
					}
					case ERROR:
					case TIMEOUT: {
						// cdDone.call(result);
						break;
					}
					}

				}
			});

			if (msg3 != null) {
				msg3 = null;
			}

		} else {
			// yes I am the originFrom
			postProcessKNN(msg3);
		}

	}

	/**
	 * I may not be the originFrom, but I have to return now
	 * 
	 * @param msg3
	 */
	public void returnKNN2Origin_backupOrigin(CompleteNNRequestMsg msg3) {

		// I should not work, since I have been a KNNer
		if (!Ninaloader.me.equals(msg3.OriginFrom)) {

			log.debug("!!!!!!!!!!!!!!!!!!!!!!\nComplete @ TargetLockReqHandler\n!!!!!!!!!!!!!!!!!!!!!!");
			// TODO: after the normal search process, we add the extra search
			// steps here

			// query completed, send msg to query nodes
			// send the result
			comm.sendRequestMessage(msg3, msg3.OriginFrom, new ObjCommRRCB<CompleteNNResponseMsg>() {

				protected void cb(CBResult result, final CompleteNNResponseMsg responseMsg, AddressIF node, Long ts) {
					switch (result.state) {
					case OK: {

						// acked by original query node
						// cdDone.call(result);
						break;
					}
					case ERROR:
					case TIMEOUT: {
						// cdDone.call(result);
						break;
					}
					}
				}
			});

		} else {
			// yes I am the originFrom
			this.postProcessKNN(msg3);
		}

	}

	/**
	 * answer KNN request form lastHop
	 * 
	 * @param ClosestRequestMsg
	 * @param errorBuffer
	 */

	public void receiveClosestQuery(final Coordinate targetCoordinate, final ClosestRequestMsg req,
			final StringBuffer errorBuffer) {

		log.debug("~~~~~~~~~~~~~~\n$: Receive KNN search from: " + req.from + " to " + req.target + " Root: " + req.root
				+ "\n~~~~~~~~~~~~~~");

		if (req != null) {

			// ===========================================================
			// last hop node
			AddressIF from = null;
			if (req.getHopRecords() != null && !req.getHopRecords().isEmpty()) {
				from = req.getHopRecords().get(req.getHopRecords().size() - 1);
			}

			// not cached yet, and this node is not the request node
			// if the root,
			if (req.getRoot() == null) {
				log.warn("root is null");

			} else if (Ninaloader.me.equals(req.getRoot())) {
				from = null;
			}
			final AddressIF LastHopNode = from;
			// log.info("$: message is from: "+LastHopNode );
			// ===========================================================

			// repeated KNN request, since I have been included
			// TODO: need to be changed to a list of vectors

			SubSetManager subset = new SubSetManager(req.getForbiddenFilter());
			// found, repeated KNN
			if (subset.checkNode(Ninaloader.me) && !Ninaloader.me.equals(req.target)) {

				// double[] rtt=new double[1];
				// ncManager.isCached(req.target, rtt, -1);
				// NodesPair np=new NodesPair(Ninaloader.me,req.target,rtt[0]);

				// if (!req.getNearestNeighborIndex().contains(np)) {
				// log.info("FALSE assert in the forbidden filter!, we do not
				// found a match!");

				// not null, repeated query msg
				// I have registered the address for target req.target,
				// since I am a KNN of req.target
				// not null, not myself
				log.warn("+++++++++++++++++++\n Repeated KNN from " + req.getFrom() + "\n++++++++++++++++++++");

				if (req.getFrom() != null && !req.getFrom().equals(Ninaloader.me)) {
					// not in

					// in, but not saved

					RepeatedNNRequestMsg rmsg = new RepeatedNNRequestMsg(req.getOriginFrom(), Ninaloader.me,
							req.getTarget(), req.getK(), req.getNearestNeighborIndex(), req.getVersion(), req.getSeed(),
							req.getRoot(), req.getFilter(), req.getForbiddenFilter());
					// cache the relative coordinate
					rmsg.targetCoordinates = req.targetCoordinates.makeCopy();

					rmsg.hasRepeated = req.hasRepeated;
					// record the message cost
					rmsg.totalSendingMessages = req.totalSendingMessages;
					rmsg.increaseMessage(1);

					rmsg.operationsForOneNN = req.operationsForOneNN;
					int len = req.getHopRecords().size();
					// remove last element in hops
					if (len > 0) {
						req.getHopRecords().remove(len - 1);
					}
					rmsg.HopRecords.addAll(req.getHopRecords());

					// ====================================================
					comm.sendRequestMessage(rmsg, req.getFrom(), new ObjCommRRCB<RepeatedNNResponseMsg>() {

						protected void cb(CBResult result, final RepeatedNNResponseMsg responseMsg, AddressIF node,
								Long ts) {
							switch (result.state) {
							case OK: {
								log.debug("repeated request acked ");
								// cdDone.call(result);
								break;
							}
							case ERROR:
							case TIMEOUT: {
								// cdDone.call(result);
								break;
							}
							}
							// ----------------------
						}
						// --------------------------
					});
					// clear the message
					if (rmsg != null) {
						rmsg.clear();
					}

					return;
				} else {
					// yes, myself, backtracking
					CompleteNNRequestMsg msg3 = CompleteNNRequestMsg.makeCopy(req);
					msg3.from = Ninaloader.me;
					msg3.myCoord = req.targetCoordinates.makeCopy();
					msg3.totalSendingMessages = req.totalSendingMessages;
					msg3.hasRepeated = req.hasRepeated;

					returnKNN2Origin(msg3);

					/*
					 * double []lat=new double[1];
					 * ncManager.isCached(req.target, lat, -1);
					 * backtracking(req,lat[0], errorBuffer);
					 */
					return;

				}
			}

			// not the target itself, not repeated KNN node, here is the main
			// logic

			// if the nearest nodes reach requirements, return
			if (req.getNearestNeighborIndex() != null && (req.getNearestNeighborIndex().size() - 1) >= req.getK()) {
				log.debug(" $: completed! ");
				CompleteNNRequestMsg msg3 = CompleteNNRequestMsg.makeCopy(req);
				msg3.from = Ninaloader.me;
				msg3.myCoord = req.targetCoordinates.makeCopy();
				msg3.totalSendingMessages = req.totalSendingMessages;
				msg3.hasRepeated = req.hasRepeated;

				this.returnKNN2Origin(msg3);
				return;
			}

			// else continue

			// TODO: if last hop fails

			// log.debug("$: last hop: " + LastHopNode);
			// to confirm whether I am a new nearest node for target

			// send the target a gossip msg
			// LOWTODO could bias which nodes are sent based on his coord
			final long sendStamp = System.nanoTime();

			// req.totalSendingMessages+=1;

			ncManager.doPing(req.getTarget(), new CB2<Double, Long>() {

				protected void cb(CBResult result, Double latency, Long timer) {
					// TODO Auto-generated method stub
					// final double[] rtt = { (System.nanoTime() - sendStamp) /
					// 1000000d };
					final double[] rtt = new double[1];
					rtt[0] = -1;
					switch (result.state) {
					case OK: {

						// if (latency.doubleValue() >= 0) {

						rtt[0] = latency.doubleValue();
						// }
						//
						// TODO: smoothed RTT
						// save the latency measurements

						/*
						 * double[] latencyNC=new double[1];
						 * if(isCached(req.getTarget(), rtt)){
						 * rtt[0]=latencyNC[0]; }
						 */
						// update the target node's coordinate

						// add the coordinate of remote node
						// instance.g_rings.NodesCoords.put(req.getTarget(),
						// targetCoordinate);
						// TODO: debug

						Coordinate new_targetCoordinate = NCClient.updateProxyTargetCoordinate(req.getTarget(),
								req.targetCoordinates, req.targetCoordinates.r_error,
								MClient.primaryNC.getSystemCoords(), MClient.primaryNC.getSystemError(), rtt[0]);

						// req.targetCoordinates.clear();
						req.targetCoordinates = new_targetCoordinate.makeCopy();

						// update my coordinate
						MClient.primaryNC.simpleCoordinateUpdate(req.getTarget(), req.targetCoordinates,
								req.targetCoordinates.r_error, rtt[0], System.currentTimeMillis());

						/*
						 * 
						 * instance.processSample(req.getTarget(),rtt[0], true,
						 * System.currentTimeMillis(),
						 * pendingNeighbors,AbstractNNSearchManager.
						 * offsetLatency);
						 */

						// cachedHistoricalNeighbors.addElement(new
						// NodesPair<AddressIF>(req.getTarget(),Ninaloader.me,rtt[0]));

						// final double lat
						// =rtt[0]+AbstractNNSearchManager.offsetLatency;
						final double lat = rtt[0];

						// request nodes to probe to target nodes
						final Vector<AddressIF> ringMembers = new Vector<AddressIF>(10);

						/*
						 * log.info("@@@@@@@@@@@@@@@@@@@@@@@@\n Node: " +
						 * Ninaloader.me + " is: " +
						 * (lat-AbstractNNSearchManager.offsetLatency) +
						 * " from Node: " + req.getTarget() +
						 * "\n@@@@@@@@@@@@@@@@@@@@@@@@");
						 */

						if (!AbstractNNSearchManager.useNormalRingRange) {

							MClient.instance.g_rings.fillVector(req.getNearestNeighborIndex(), req.getTarget(), lat,
									lat, betaRatio, ringMembers, AbstractNNSearchManager.offsetLatency);

							// add perturbation
							Set<AddressIF> randomSeeds = new HashSet<AddressIF>(
									KNNManager.defaultNodesForRandomContact);
							// randomSeeds.addAll(ringMembers);
							if (useRandomSeedsInSearching) {
								MClient.instance.g_rings.getNodesWithMaximumNonEmptyRings(req.getNearestNeighborIndex(),
										randomSeeds, KNNManager.defaultNodesForRandomContact);

								ringMembers.addAll(randomSeeds);
								randomSeeds.clear();
							}

						} else {

							// List<NodesPair>
							// targetRelCoord=targetCoordinate.coords;

							// let ring members to ping the target
							// instance.g_rings.fillVectorBasedOnKClosestNode(Ninaloader.me,req.getNearestNeighborIndex()
							// ,targetRelCoord, lat, ringMembers,
							// AbstractNNSearchManager.offsetLatency);

							Set<AddressIF> ForbiddenNodes = new HashSet<AddressIF>();
							Iterator<NodesPair> ier33 = req.getNearestNeighborIndex().iterator();
							while (ier33.hasNext()) {
								ForbiddenNodes.add((AddressIF) ier33.next().startNode);
							}
							ForbiddenNodes.addAll(req.getHopRecords());

							MClient.instance.g_rings.fillVector(Ninaloader.me, ForbiddenNodes, lat, betaRatio,
									ringMembers, AbstractNNSearchManager.offsetLatency);

							// closest node to the target
							/*
							 * Integer closestNode =
							 * this.getClosestPeerFromListWithCoord(event.
							 * getTarget(), ringMembers); //if closestNode is
							 * null, it means you are the closest
							 * 
							 * ringMembers.clear(); if(closestNode!=null){
							 * ringMembers.add(closestNode); }
							 */

							Set<AddressIF> nodes = MClient.instance.g_rings.getKClosestPeerFromListWithCoord(
									MClient.primaryNC, req.target, req.targetCoordinates, ringMembers,
									returnedNodesForNextHop);
							ringMembers.clear();
							ringMembers.addAll(nodes);
							nodes.clear();

						}

						/*
						 * // not target from rings if
						 * (ringMembers.contains(req.getTarget())) {
						 * ringMembers.remove(req.getTarget()); }
						 * 
						 * // log.debug("The size of returned members: " // +
						 * ringMembers.size());
						 * 
						 * SubSetManager subset=new
						 * SubSetManager(req.getForbiddenFilter()); //
						 * postprocess the ring members Iterator<AddressIF>
						 * ierRing = ringMembers.iterator(); while
						 * (ierRing.hasNext()) { AddressIF tmpNode =
						 * ierRing.next(); if (subset.checkNode(tmpNode)) { //
						 * found forbidden nodes ierRing.remove(); } }
						 * 
						 * 
						 * // also remove the backtracking node
						 * Iterator<AddressIF> ierBack = req.getHopRecords()
						 * .iterator(); while (ierBack.hasNext()) {
						 * 
						 * AddressIF tmp = ierBack.next(); if
						 * (ringMembers.contains(tmp)) { log.debug(
						 * "Remove forbidden backtracking node: "+ tmp);
						 * ringMembers.remove(tmp); } }
						 */

						// ---------------------------------

						// boolean for current nodes
						boolean isNearest = false;
						boolean another = false;
						// returned ID is null
						// Note: if the ringMembers contain the target
						// node, current node may not be the nearest
						// nodes
						if (ringMembers == null || (ringMembers.size() == 0)) {

							// the originNode is the only one node
							// no nearer nodes, so return me
							// log.debug(Ninaloader.me
							// + " is the closest to: "
							// + req.target
							// + " from g_rings.fillVector @: "
							// + Ninaloader.me + " Target: "
							// + req.target);

							// TODO: add the clustering based 1-step
							// search, the ring must be balanced

							// backtrack
							// find last hop
							// TODO: assume that K<=N
							// AddressIF lastHop=null;

							// I am the lastHop
							// TODO change the beta according to
							// current rings, 9-24
							// remove condition:
							// ||req.K<=req.NearestNeighborIndex.size()-1,
							// 9-24
							if (LastHopNode == null || LastHopNode.equals(Ninaloader.me)) {
								log.debug("Finish! ");

								SubSetManager subset = new SubSetManager(req.getForbiddenFilter());
								req.getNearestNeighborIndex().add(new NodesPair(Ninaloader.me, req.target, lat,
										LastHopNode, req.operationsForOneNN));
								subset = new SubSetManager(req.getForbiddenFilter());
								subset.insertPeers2Subset(Ninaloader.me);

								CompleteNNRequestMsg msg3 = CompleteNNRequestMsg.makeCopy(req);

								msg3.totalSendingMessages = req.totalSendingMessages;
								msg3.from = Ninaloader.me;
								// CompleteNNRequestMsg
								// msg3=CompleteNNRequestMsg.makeCopy(req);
								msg3.myCoord = req.targetCoordinates.makeCopy();
								msg3.hasRepeated = req.hasRepeated;

								returnKNN2Origin(msg3);
								return;

							} else {

								log.debug(" find a new last hop!!!");

								backtracking(req, lat, errorBuffer);
							}

						} else {

							// final double refRTT=lat;
							// TODO: ask a set of nodes to ping a set of
							// nodes

							TargetProbes(ringMembers, req.getTarget(), new CB1<String>() {

								protected void cb(CBResult ncResult, String errorString) {
									switch (ncResult.state) {
									case OK: {
										// log.debug("$: Target Probes are
										// issued for KNN");
									}
									case ERROR:
									case TIMEOUT: {
										break;
									}
									}
								}
							}, new CB2<Set<NodesPair>, String>() {

								protected void cb(CBResult ncResult, Set<NodesPair> nps2, String errorString) {

									switch (ncResult.state) {
									case OK: {

										final Set<NodesPair> nps1 = new HashSet<NodesPair>(1);
										nps1.addAll(nps2);

										req.increaseMessage(nps1.size());
										// ============================================
										// update the target's Coordinate
										Iterator<NodesPair> ier = nps1.iterator();
										while (ier.hasNext()) {
											NodesPair rec = ier.next();
											AddressIF remoteNode = (AddressIF) rec.startNode;
											if (MClient.instance.g_rings.NodesCoords.containsKey(remoteNode)) {
												Coordinate refCoord = MClient.instance.g_rings.NodesCoords
														.get(remoteNode).makeCopy();
												if (refCoord != null && req.targetCoordinates != null) {
													Coordinate new_targetCoordinate = NCClient
															.updateProxyTargetCoordinate(req.getTarget(),
																	req.targetCoordinates,
																	req.targetCoordinates.r_error, refCoord,
																	refCoord.r_error, rec.rtt);
													// req.targetCoordinates.clear();
													req.targetCoordinates = new_targetCoordinate.makeCopy();
													new_targetCoordinate.clear();
												}
											}
										}

										// ============================================
										// +++++++++++++++++++++++++++++++++++++
										// use the rule to select next hop nodes
										AddressIF target = req.target;
										Set<NodesPair> closestNodes = new HashSet<NodesPair>(1);
										SubSetManager subset = new SubSetManager(req.getForbiddenFilter());
										// select based rules
										AddressIF returnID = ClosestNodeRule(lat, target, nps1, closestNodes, subset,
												LastHopNode);

										// record new nearest nodes
										if (closestNodes != null && !closestNodes.isEmpty()) {
											Iterator<NodesPair> ierNode = closestNodes.iterator();

											while (ierNode.hasNext()) {
												NodesPair tmp = ierNode.next();
												tmp.ElapsedSearchHops = req.operationsForOneNN;

												AddressIF from = (AddressIF) tmp.startNode;
												// add, only when it is closer
												// than me, and does not reside
												// on the backtrack path
												if (!subset.checkNode(from)) {
													subset.insertPeers2Subset(from);
													req.getNearestNeighborIndex().add(tmp);
												}

											}
										}
										req.operationsForOneNN = 0;

										// enough
										if (req.getNearestNeighborIndex() != null
												&& (req.getNearestNeighborIndex().size() - 1) >= req.getK()) {
											log.debug(" $: completed! ");
											CompleteNNRequestMsg msg3 = CompleteNNRequestMsg.makeCopy(req);
											msg3.from = Ninaloader.me;
											msg3.myCoord = req.targetCoordinates.makeCopy();
											msg3.totalSendingMessages = req.totalSendingMessages;
											msg3.hasRepeated = req.hasRepeated;

											returnKNN2Origin(msg3);
											return;
										}

										// +++++++++++++++++++++++++++++++++++++
										if (returnID == null
												|| (LastHopNode != null) && (returnID.equals(LastHopNode))) {
											// found nearest, we backtrack
											// I am the lastHop
											// remove 9-24
											// remove last hop

											backtracking(req, lat, errorBuffer);
											return;
										} else {

											/**
											 * me as well
											 */
											if (returnID.equals(Ninaloader.me)) {
												receiveClosestQuery(req.targetCoordinates, req, errorBuffer);
												return;
											}

											// returnID is not
											// Ninaloader.me
											//
											final ClosestRequestMsg msg = new ClosestRequestMsg(req.getOriginFrom(),
													Ninaloader.me, req.getTarget(), req.getK(),
													req.getNearestNeighborIndex(), req.getVersion(), req.getSeed(),
													req.getRoot(), req.getFilter(), req.getForbiddenFilter());

											msg.targetCoordinates = req.targetCoordinates.makeCopy();
											// current hop
											msg.getHopRecords().addAll(req.getHopRecords());
											msg.addCurrentHop(Ninaloader.me);

											printHops("KNN visited hops", msg.getHopRecords());

											// record the hops for finding a nn
											msg.operationsForOneNN = req.operationsForOneNN;
											msg.operationsForOneNN++;

											// increase the message cost
											msg.totalSendingMessages = req.totalSendingMessages;
											msg.increaseMessage(1);

											msg.hasRepeated = req.hasRepeated;

											ringMembers.clear();
											// me
											/*
											 * if(returnID.equals(Ninaloader.me)
											 * ){ receiveClosestQuery(msg,
											 * errorBuffer); }else{
											 */

											log.debug("receiveClosestQuery_singleTargets: next hop: "
													+ returnID.toString() + ", from: " + Ninaloader.me.toString()
													+ ", found " + msg.getNearestNeighborIndex().size() + "KNNs");

											comm.sendRequestMessage(msg, returnID,
													new ObjCommRRCB<ClosestResponseMsg>() {

												protected void cb(CBResult result, final ClosestResponseMsg responseMsg,
														AddressIF node, Long ts) {
													switch (result.state) {
													case OK: {
														log.debug("Request Acked by new query node: ");
														// cdDone.call(result);
														break;
													}
													case ERROR:
													case TIMEOUT: {
														// cdDone.call(result);
														/*
														 * findNewClosestNxtNode(
														 * sortedL, msg,
														 * LastHopNode, lat);
														 */
														Iterator ier2 = nps1.iterator();
														SortedList<NodesPair> sortedL = new SortedList<NodesPair>(
																new NodesPairComp());
														while (ier2.hasNext()) {

															NodesPair tmp = (NodesPair) ier2.next();
															// invalid
															// measurements
															if (tmp.rtt < 0) {
																continue;
															}
															// the same with
															// target
															if (tmp.startNode.equals(req.getTarget())) {
																continue;
															}
															// if tmp.lat <
															// current
															// latency from
															// me

															// 10-18- add the
															// beta constriant,
															// to reduce the
															// elapsed time
															// if < beta, return
															// current node
															if (tmp.rtt <= lat) {
																sortedL.add(tmp);
															}
														}
														findNewClosestNxtNode(sortedL, msg, lat, errorBuffer);
														break;
													}
													}
													// ----------------------
													if (msg != null) {
														msg.clear();
													}
													if (req != null) {
														req.clear();
													}
												}
												// --------------------------

											});// }
										}
										break;
									}
									case ERROR:
									case TIMEOUT: {
										System.err.println(
												"KNN Search: Target probes to " + req.getTarget() + " FAILED!"); // the
										// returned
										// map
										// is
										// null
										// we move backward
										backtracking(req, lat, errorBuffer);
										break;
									}
									}

								}//
							}

							);
						} // end of target probe

						// cdDone.call(result);
						break;
					}

					case TIMEOUT:
					case ERROR: {

						String error = "RTT request to " + req.getTarget().toString(false) + " failed:"
								+ result.toString();
						log.warn(error);
						if (errorBuffer.length() != 0) {
							errorBuffer.append(",");
						}
						errorBuffer.append(error);
						// cdDone.call(result);

						// the target can not be reached, we finish the KNN
						// search process
						CompleteNNRequestMsg msg3 = CompleteNNRequestMsg.makeCopy(req);
						msg3.from = Ninaloader.me;
						// CompleteNNRequestMsg
						// msg3=CompleteNNRequestMsg.makeCopy(req);
						msg3.myCoord = req.targetCoordinates.makeCopy();
						msg3.totalSendingMessages = req.totalSendingMessages;
						msg3.hasRepeated = req.hasRepeated;

						returnKNN2Origin(msg3);

						break;
					}

					}

				}

			});

		}
	}

	/**
	 * backtracking
	 * 
	 * @param req
	 * @param LastHopNode
	 */
	private void backtracking(final ClosestRequestMsg req, final double lat, final StringBuffer errorBuffer) {

		// remove last hop

		AddressIF LastHopNode1 = null;
		int hopLen = req.getHopRecords().size();
		if (hopLen > 0) {
			// the last hop
			LastHopNode1 = req.getHopRecords().get(hopLen - 1);
			req.getHopRecords().remove(hopLen - 1);
		}

		final AddressIF LastHopNode = LastHopNode1;

		// ====================================================
		// not included
		SubSetManager subset = new SubSetManager(req.getForbiddenFilter());
		if (!subset.checkNode(Ninaloader.me)) {

			NodesPair tmp = new NodesPair(Ninaloader.me, req.target, lat, LastHopNode, req.operationsForOneNN);
			req.getNearestNeighborIndex().add(tmp);
			req.operationsForOneNN = 0;
			subset.insertPeers2Subset(Ninaloader.me);
			// OK send originFrom a message
			/*
			 * log.info(Ninaloader.me + " is the closest to: " + req.getTarget()
			 * + " from TargetProbes @: " + Ninaloader.me + " Target: " +
			 * req.getTarget());
			 */
			// -------------------------------------
			// due to "final" constriant
			// register an item to registeredNearestNodes, stop answering the
			// corresponding request

			/*
			 * synchronized (registeredNearestNodes) { if
			 * (registeredNearestNodes.get(req.getTarget()) == null) {
			 * registeredNearestNodes.put(req.getTarget(), new Vector<record>(
			 * 1)); } registeredNearestNodes.get(req.getTarget()).add( new
			 * record(req.getOriginFrom(), req.getVersion(), req .getSeed(), new
			 * NodesPair(Ninaloader.me, req .getTarget(), lat)));
			 * 
			 * }
			 */
		}
		// =======================================================

		/*
		 * if (LastHopNode == null || LastHopNode.equals(Ninaloader.me) ||
		 * (req.getNearestNeighborIndex().size() - 1) >= req.getK()) { //
		 * log.debug("!I am the lastHop"); // finish the querying process:
		 * callback of query CompleteNNRequestMsg msg3 =
		 * CompleteNNRequestMsg.makeCopy(req); msg3.from = Ninaloader.me; //
		 * send the result returnKNN2Origin(msg3); return;
		 * 
		 * } else {
		 */
		/*
		 * log.info("\n----------------------\n KNN hops: " +
		 * req.getHopRecords().size() + "\n----------------------\n");
		 */

		// forward back

		if (LastHopNode != null && !LastHopNode.equals(Ninaloader.me)) {

			final TargetLocatedRequestMsg msg = new TargetLocatedRequestMsg(Ninaloader.me, req.getOriginFrom(),
					req.getTarget(), req.getK(), req.getNearestNeighborIndex(), req.getVersion(), req.getSeed(),
					req.getRoot(), req.getFilter(), req.getForbiddenFilter());

			msg.HopRecords.addAll(req.getHopRecords());

			msg.targetCoordinates = req.targetCoordinates.makeCopy();

			msg.operationsForOneNN = req.operationsForOneNN;
			msg.operationsForOneNN++;
			msg.hasRepeated = req.hasRepeated;

			msg.totalSendingMessages = req.totalSendingMessages;
			msg.increaseMessage(1);

			// different nodes from target
			comm.sendRequestMessage(msg, LastHopNode, new ObjCommRRCB<TargetLocatedResponseMsg>() {

				protected void cb(CBResult result, final TargetLocatedResponseMsg responseMsg, AddressIF node,
						Long ts) {

					switch (result.state) {
					case OK: {
						// acked by original query node
						// cdDone.call(result);
						break;
					}
					case ERROR:
					case TIMEOUT: {
						// cdDone.call(result);
						findAliveAncestor(msg, LastHopNode);
						break;
					}
					}
					if (msg != null) {
						msg.clear();
					}
				}
			});

		} else {
			// myself, if it is the sebacktracking, we finish
			CompleteNNRequestMsg msg3 = CompleteNNRequestMsg.makeCopy(req);
			msg3.from = Ninaloader.me;
			msg3.myCoord = req.targetCoordinates.makeCopy();
			msg3.totalSendingMessages = req.totalSendingMessages;
			msg3.hasRepeated = req.hasRepeated;
			// send the result
			returnKNN2Origin(msg3);
			return;

		}
	}

	/**
	 * ping the ancestors recorded in backtracking lists, and select the alive
	 * ancestor which is nearest to me, as the ancestor
	 * 
	 * @param msg
	 * @param lastHopNode
	 */
	private void findAliveAncestor(final TargetLocatedRequestMsg msg, AddressIF lastHopNode) {
		// TODO Auto-generated method stub
		final List<AddressIF> ancestors = msg.HopRecords;

		boolean ContactAsker = false;

		if (ancestors.size() > 0) {

			log.debug("\n==================\n The total No. of ancestors: " + ancestors.size()
					+ "\n==================\n");

			final int[] bitVector = new int[ancestors.size()];
			for (int i = 0; i < ancestors.size(); i++) {
				bitVector[i] = 0;
			}
			AddressIF ClosestAncestor = null;

			Set<AddressIF> tmpList = new HashSet<AddressIF>(1);
			tmpList.addAll(ancestors);

			collectRTTs(tmpList, new CB2<Set<NodesPair>, String>() {

				protected void cb(CBResult ncResult, Set<NodesPair> nps, String errorString) {
					// send data request message to the core node

					// TODO Auto-generated method stub
					int rank = Integer.MAX_VALUE;

					if (nps != null && nps.size() > 0) {
						log.debug("\n==================\n Alive No. of ancestors: " + nps.size()
								+ "\n==================\n");
						// find the nodes that are closest to the root
						Iterator<NodesPair> ier = nps.iterator();
						while (ier.hasNext()) {
							NodesPair tmp = ier.next();
							int index = ancestors.indexOf(tmp.endNode);

							// failed measurement
							if (tmp.rtt < 0) {
								index = -1;
							}

							if (index < 0) {
								continue;
							} else {
								// found the element, and it is smaller than
								// rank, i.e., it is closer to the target
								bitVector[index] = 1;
							}
						}

					} else {
						// all nodes fail, so there are no alive nodes
						rank = -1;
					}

					// iterate the bitVector
					// ===============================
					int start = -1;
					boolean first = false;
					int ZeroCounter = 0;
					for (int i = 0; i < ancestors.size(); i++) {
						// first failed node
						if (!first && (bitVector[i] == 0)) {
							start = i;
							first = true;
						}
						if (bitVector[i] == 0) {
							ZeroCounter++;
						}
					}

					// all empty, or the root is empty
					if (ZeroCounter == ancestors.size() || start == 0) {
						// we do not have any node to report, no backtracking
						// nodes !

						CompleteNNRequestMsg msg3 = CompleteNNRequestMsg.makeCopy(msg);
						msg3.from = Ninaloader.me;
						msg3.myCoord = msg.targetCoordinates.makeCopy();
						msg3.totalSendingMessages = msg.totalSendingMessages;
						msg3.hasRepeated = msg.hasRepeated;

						returnKNN2Origin(msg3);
						return;

					} else {

						if (start == -1) {
							// it means no ancestors are dead!
							start = ancestors.size() - 1;
						} else {
							// we found an ancestor node
							// remove unnecessary ancestors
							final List<AddressIF> missing = new ArrayList<AddressIF>(1);
							missing.addAll(ancestors.subList(start, ancestors.size()));

							ancestors.removeAll(missing);
						}
						int newLen = ancestors.size();

						// we can not find new cloest nodes
						if (newLen <= 0) {
							//
							// finish the search process, and echo
							CompleteNNRequestMsg msg3 = CompleteNNRequestMsg.makeCopy(msg);
							msg3.from = Ninaloader.me;
							msg3.myCoord = msg.targetCoordinates.makeCopy();
							msg3.totalSendingMessages = msg.totalSendingMessages;
							msg3.hasRepeated = msg.hasRepeated;

							// send the result
							returnKNN2Origin(msg3);
							return;
						}
						final AddressIF LastHopNode = ancestors.get(newLen - 1);
						ancestors.remove(newLen - 1);

						// set up hop vectors
						msg.HopRecords = ancestors;

						// last hop is not me
						if (!LastHopNode.equals(Ninaloader.me)) {
							// different nodes from target
							comm.sendRequestMessage(msg, LastHopNode, new ObjCommRRCB<TargetLocatedResponseMsg>() {

								protected void cb(CBResult result, final TargetLocatedResponseMsg responseMsg,
										AddressIF node, Long ts) {

									switch (result.state) {
									case OK: {
										// acked by original query node
										// cdDone.call(result);
										log.debug("We find new lastHop node: " + LastHopNode);

										break;
									}
									case ERROR:
									case TIMEOUT: {
										// cdDone.call(result);
										findAliveAncestor(msg, LastHopNode);
										break;
									}
									}
								}
							});
							return;
						} else {
							// last hop is me
							log.debug("The last hop can not be me, in findAliveAncestor");
							assert (false);
							return;
						}

					}

					// ===============================
				}

			});

			tmpList.clear();

		} else {
			// we do not have any node to report, no backtracking nodes !
			ContactAsker = true;
			// contact the origin node
			if (ContactAsker) {
				CompleteNNRequestMsg msg3 = CompleteNNRequestMsg.makeCopy(msg);
				msg3.from = Ninaloader.me;
				msg3.myCoord = msg.targetCoordinates.makeCopy();
				msg3.totalSendingMessages = msg.totalSendingMessages;
				msg3.hasRepeated = msg.hasRepeated;

				this.returnKNN2Origin(msg3);
				return;
			}
		}

	}

	/**
	 * If next-hop node fails, we find new candidate from the sortedList
	 * 
	 * @param sortedL
	 * @param msg
	 * @param lastHopNode
	 */
	private void findNewClosestNxtNode(final SortedList<NodesPair> sortedL, final ClosestRequestMsg msg,
			final double lat, final StringBuffer errorBuffer) {
		// TODO Auto-generated method stub
		if (sortedL != null && sortedL.size() > 0) {

			final List<AddressIF> candidates = new ArrayList<AddressIF>(1);
			Iterator<NodesPair> ier = sortedL.iterator();
			while (ier.hasNext()) {
				candidates.add((AddressIF) ier.next().startNode);
			}

			final int[] bitVector = new int[candidates.size()];
			for (int i = 0; i < candidates.size(); i++) {
				bitVector[i] = 0;
			}

			Set<AddressIF> tmpList = new HashSet<AddressIF>(1);
			tmpList.addAll(candidates);

			collectRTTs(tmpList, new CB2<Set<NodesPair>, String>() {

				protected void cb(CBResult ncResult, Set<NodesPair> nps, String errorString) {

					if (nps != null && nps.size() > 0) {
						log.debug("\n==================\n Alive No. of next-hop node: " + nps.size()
								+ "\n==================\n");
						// find the nodes that are closest to the root
						Iterator<NodesPair> ier = nps.iterator();
						while (ier.hasNext()) {
							NodesPair tmp = ier.next();
							int index = candidates.indexOf(tmp.endNode);

							// failed measurement
							if (tmp.rtt < 0) {
								index = -1;
							}
							if (index < 0) {
								continue;
							} else {
								// found the element, and it is smaller than
								// rank, i.e., it is closer to the target
								bitVector[index] = 1;
							}
						}

					} else {
						// all nodes fail, so there are no alive nodes
					}

					// iterate the bitVector
					// ===============================
					int start = -1;
					boolean first = false;
					int ZeroCounter = 0;
					for (int i = 0; i < candidates.size(); i++) {
						// first alive node
						if (!first && (bitVector[i] == 1)) {
							start = i;
							first = true;
						}
						if (bitVector[i] == 0) {
							ZeroCounter++;
						}
					}

					// all empty
					if (ZeroCounter == candidates.size() || start == -1) {
						// we do not have any node to report, we backtrack !

						backtracking(msg, lat, errorBuffer);

						return;

					} else {

						// send to new node
						AddressIF returnID = candidates.get(start);
						comm.sendRequestMessage(msg, returnID, new ObjCommRRCB<ClosestResponseMsg>() {

							protected void cb(CBResult result, final ClosestResponseMsg responseMsg, AddressIF node,
									Long ts) {
								switch (result.state) {
								case OK: {
									log.debug("Request Acked by new query node: ");
									// cdDone.call(result);
									break;
								}
								case ERROR:
								case TIMEOUT: {
									// cdDone.call(result);
									findNewClosestNxtNode(sortedL, msg, lat, errorBuffer);
									break;
								}
								}
								// ----------------------
							}
							// --------------------------

						});

					}

				}
			});

		} else {
			// we do not have next-hop node, so we finish the searching process,
			// I am the nearest node, and backtrack
			backtracking(msg, lat, errorBuffer);
		}
	}

	// ----------------------------------------------
	public class RepeatedNNReqHandler extends ResponseObjCommCB<RepeatedNNRequestMsg> {

		protected void cb(CBResult arg0, final RepeatedNNRequestMsg arg1, AddressIF arg2, Long arg3,
				final CB1<Boolean> arg4) {
			// Auto-generated method stub

			/*
			 * null; Vector<backtracking> vList =
			 * cachedBackTrack.get(arg1.target); if (vList != null) {
			 * Iterator<backtracking> ier = vList.iterator(); if (ier != null) {
			 * while (ier.hasNext()) { backtracking tmp = ier.next(); if
			 * ((arg1.OriginFrom.equals(tmp.OriginFrom)) &&
			 * (arg1.target.equals(tmp.target)) && (arg1.version == tmp.version)
			 * && (arg1.seed == tmp.seed)) { from = tmp.lastHop; break; } } } }
			 */
			final AddressIF answerNod = arg1.from;
			log.debug("repeated from: " + answerNod);
			final long msgID = arg1.getMsgId();
			RepeatedNNResponseMsg msg2 = new RepeatedNNResponseMsg(Ninaloader.me);
			sendResponseMessage("repeated", answerNod, msg2, msgID, null, arg4);

			execKNN.execute(new Runnable() {

				public void run() {

					// backtracking process
					// backtracking process
					// from=lastHop
					int len = arg1.HopRecords.size();
					AddressIF from1 = null;
					if (len > 0) {
						from1 = arg1.HopRecords.get(len - 1);
						// arg1.HopRecords.remove(len-1);
					}
					final AddressIF from = from1;

					ClosestRequestMsg msg1 = new ClosestRequestMsg(arg1.OriginFrom, from, arg1.target, arg1.K,
							arg1.NearestNeighborIndex, arg1.version, arg1.seed, arg1.root, arg1.filter,
							arg1.ForbiddenFilter);
					// hop
					if (!arg1.HopRecords.isEmpty()) {
						msg1.getHopRecords().addAll(arg1.HopRecords);
					}
					final StringBuffer errorBuffer = new StringBuffer();
					msg1.targetCoordinates = arg1.targetCoordinates.makeCopy();

					msg1.operationsForOneNN = arg1.operationsForOneNN;
					msg1.totalSendingMessages = arg1.totalSendingMessages;

					msg1.hasRepeated = arg1.hasRepeated;

					receiveClosestQuery(msg1.targetCoordinates, msg1, errorBuffer);

				}
			});

		}

	}

	/**
	 * answer the callback of intermediate nodes
	 * 
	 * @author ericfu
	 * 
	 */
	class TargetLockReqHandler extends ResponseObjCommCB<TargetLocatedRequestMsg> {

		protected void cb(CBResult arg0, final TargetLocatedRequestMsg arg1, AddressIF arg2, Long arg3,
				final CB1<Boolean> arg4) {
			//

			// Set<NodesPair> NNs = arg1.NearestNeighborIndex;

			/*
			 * Vector<backtracking> vList = cachedBackTrack.get(arg1.target); if
			 * (vList != null) { Iterator<backtracking> ier = vList.iterator();
			 * if (ier != null) { while (ier.hasNext()) { backtracking tmp =
			 * ier.next(); if (arg1.target.equals(tmp.target) && (arg1.version
			 * == tmp.version) && (arg1.seed == tmp.seed)) { from = tmp.lastHop;
			 * break; } } } }
			 */

			final AddressIF answerNod = arg1.from; // original from
			// backtracking process
			// from=lastHop
			int len = arg1.HopRecords.size();
			final AddressIF[] from = { null };
			if (len > 0) {
				from[0] = arg1.HopRecords.get(len - 1);
				// arg1.HopRecords.remove(len-1);
			}

			final long msgID = arg1.getMsgId();

			TargetLocatedResponseMsg msg22 = new TargetLocatedResponseMsg(Ninaloader.me);
			sendResponseMessage("TargetLock", answerNod, msg22, msgID, null, arg4);

			// independent thread
			execKNN.execute(new Runnable() {

				public void run() {
					// TODO Auto-generated method stub
					// reach the K limit, or there is no backtrack node

					// TODO: error
					// from maybe null
					ClosestRequestMsg msg1 = new ClosestRequestMsg(arg1.OriginFrom, from[0], arg1.target, arg1.K,
							arg1.getNearestNeighborIndex(), arg1.version, arg1.seed, arg1.root, arg1.filter,
							arg1.ForbiddenFilter);

					msg1.getHopRecords().addAll(arg1.HopRecords);

					final StringBuffer errorBuffer = new StringBuffer();

					msg1.targetCoordinates = arg1.targetCoordinates.makeCopy();

					msg1.totalSendingMessages = arg1.totalSendingMessages;
					msg1.operationsForOneNN = arg1.operationsForOneNN;
					msg1.hasRepeated = arg1.hasRepeated;

					receiveClosestQuery(msg1.targetCoordinates, msg1, errorBuffer);

				}
			});

		}

	}

	void printHops(String header, List<AddressIF> Hops) {
		boolean open = false;
		if (open) {
			StringBuffer buf = new StringBuffer();
			buf.append(header + " print Hops: ");
			Iterator<AddressIF> ier = Hops.iterator();
			while (ier.hasNext()) {
				AddressIF tmp = ier.next();
				buf.append(tmp.toString() + " ");
			}
			log.info(buf.toString());
		}
	}

	/**
	 * I am the orginFrom node, which send the KNN query
	 */
	void postProcessKNN(CompleteNNRequestMsg arg1) {

		log.debug("=======================\n postProcessing" + "\n=======================");

		final Set<NodesPair> NNs = arg1.getNearestNeighborIndex();
		// callback invoked

		CBcached curCB = cachedCB.get(Long.valueOf(arg1.seed));
		if (curCB != null) {

			// match the seed, if it does not match, it means it is
			// another KNN search
			log.debug("\n@@:\n KNN branch is reported from " + arg1.from);
			/**
			 * total message
			 */
			curCB.totalSendingMessage = arg1.totalSendingMessages;

			curCB.nps.addAll(NNs);

			// arg1.clear();
			// done, wake up the barrier
			curCB.wake.setNumForks(1);
			// curCB.wake.activate();
			curCB.wake.join();
		}

		// notify nodes that cache the query records to withdraw
		/*
		 * if (NNs != null && !NNs.isEmpty()) { log.debug(NNs.size() +
		 * " KNN results are reported!"); final Barrier barrierCollect = new
		 * Barrier(true); // Collect
		 * 
		 * Iterator<NodesPair> ier = NNs.iterator(); while (ier.hasNext()) {
		 * NodesPair tmp = ier.next(); final AddressIF to = tmp.startNode; //
		 * not me if (!to.equals(Ninaloader.me)) { continue; }
		 * WithdrawRequestMsg msg33 = new WithdrawRequestMsg( Ninaloader.me,
		 * arg1.OriginFrom, arg1.target, arg1.version, arg1.seed);
		 * 
		 * barrierCollect.fork(); comm.sendRequestMessage(msg33, to, new
		 * ObjCommRRCB<WithdrawResponseMsg>() {
		 * 
		 * protected void cb(CBResult result, final WithdrawResponseMsg
		 * responseMsg, AddressIF node, Long ts) { switch (result.state) { case
		 * OK: { log.debug("@@: Withdraw accepted by node " + to); // acked by
		 * original query node // cdDone.call(result); break; } case ERROR: case
		 * TIMEOUT: { // cdDone.call(result); break; } } barrierCollect.join();
		 * } }); } long interval=2*1000;
		 * EL.get().registerTimerCB(barrierCollect,interval, new CB0() {
		 * 
		 * protected void cb(CBResult result) {
		 * 
		 * // log.debug("$@ @ Completed"); NNs.clear();
		 * 
		 * } }); }
		 */

	}

	/**
	 * completed the query process
	 * 
	 * @author ericfu
	 * 
	 */
	class CompleteNNReqHandler extends ResponseObjCommCB<CompleteNNRequestMsg> {

		@SuppressWarnings("unchecked")

		protected void cb(CBResult arg0, final CompleteNNRequestMsg arg1, AddressIF arg2, Long arg3,
				final CB1<Boolean> arg4) {

			// final AddressIF answerNod=arg1.from; //original from

			// acked
			// ------------------------------------------
			CompleteNNResponseMsg msg22 = new CompleteNNResponseMsg(Ninaloader.me);
			msg22.setResponse(true);
			msg22.setMsgId(arg1.getMsgId());
			// ------------------------------------------
			sendResponseMessage("CompleteNN", arg1.from, msg22, arg1.getMsgId(), null, arg4);

			// independent thread
			execKNN.execute(new Runnable() {

				public void run() {

					// postProcessing
					postProcessKNN(arg1);

				}
			});

		}

	}

	class WithdrawReqHandler extends ResponseObjCommCB<WithdrawRequestMsg> {

		protected void cb(CBResult arg0, WithdrawRequestMsg arg1, AddressIF arg2, Long arg3, CB1<Boolean> arg4) {
			// TODO Auto-generated method stub
			// remove records

			/*
			 * synchronized (registeredNearestNodes) {
			 * 
			 * Vector<record> RegisteredNNVec = registeredNearestNodes
			 * .get(arg1.target); if (RegisteredNNVec != null &&
			 * RegisteredNNVec.size() > 0) { Iterator<record> ier =
			 * RegisteredNNVec.iterator(); while (ier.hasNext()) { record tmp =
			 * ier.next(); if (tmp.OriginFrom.equals(arg1.OriginFrom) &&
			 * tmp.version == arg1.version && tmp.seed == arg1.seed) {
			 * ier.remove(); break; } } } }
			 * 
			 * synchronized (cachedBackTrack) { backtracking tmp = null;
			 * Vector<backtracking> vList = cachedBackTrack.get(arg1.target); if
			 * (vList != null) { Iterator<backtracking> ier = vList.iterator();
			 * while (ier.hasNext()) { tmp = ier.next(); if
			 * (tmp.target.equals(arg1.target) &&
			 * tmp.OriginFrom.equals(arg1.OriginFrom) && (tmp.version ==
			 * arg1.version) && (tmp.seed == arg1.seed)) {
			 * 
			 * // cachedBackTrack.get(arg1.target).remove(tmp); ier.remove();
			 * break; } } }
			 * 
			 * }
			 */
			// -------------------------------------
			WithdrawResponseMsg msg33 = new WithdrawResponseMsg(Ninaloader.me);
			sendResponseMessage("Withdraw", arg1.from, msg33, arg1.getMsgId(), null, arg4);

		}

	}

	class FarthestReqHandler extends ResponseObjCommCB<FarthestRequestMsg> {

		protected void cb(CBResult arg0, final FarthestRequestMsg arg1, AddressIF arg2, Long arg3, CB1<Boolean> arg4) {
			// TODO Auto-generated method stub
			//
			final AddressIF remNod = arg1.target;
			final AddressIF answerNod = arg1.from;
			final long msgID = arg1.getMsgId();
			final StringBuffer errorBuffer = new StringBuffer();
			sendResponseMessage("Farthest", answerNod, new FarthestResponseMsg(Ninaloader.me), msgID, null, arg4);

			execKNN.execute(new Runnable() {

				public void run() {
					if (arg1.target != null) {
						receiveFathestReq(arg1, errorBuffer);
					} else if (arg1.targets != null) {
						log.warn("can not proceed farthest node search!");

					}
				}
			});

		}
	}

	/**
	 * k farthest nodes search
	 * 
	 * @param target
	 * @param candidates
	 * @param k
	 * @param cbDone
	 */
	public void queryKFarthestNeighbors(final AddressIF target, final int k, final CB1<List<NodesPair>> cbDone) {
		// farthest search
		int direction = FarthestSearch;

		int branches = MultipleRestart;
		final Set<AddressIF> randNodes = new HashSet<AddressIF>(10);

		// rings
		double maxAvgUS = Double.MAX_VALUE;
		// instance.g_rings.getRandomNodes(randNodes, branches);

		// TODO: K farthest node search
		// instance.g_rings.fillVectorBasedOnKCloestNode(branches ,
		// Ninaloader.me, instance.g_rings.myCoordinate.coords, maxAvgUS,
		// randNodes,offsetLatency );

		if (randNodes.isEmpty()) {
			MClient.instance.g_rings.getNodesWithMaximumNonEmptyRings(randNodes, branches);
		}

		if (randNodes.contains(target)) {
			log.debug("$: we remove the target, then we start the KNN search process! ");
			randNodes.remove(target);
		}

		int realNodeNo = randNodes.size();

		log.debug("\nK farthest node search\n!!!!!!!!!!!!!!!!!!!!!!!\nTotal branches: " + realNodeNo
				+ "\n!!!!!!!!!!!!!!!!!!!!!!!");

		final long timeStamp = System.currentTimeMillis();

		if (!randNodes.isEmpty()) {
			final Barrier barrierUpdate = new Barrier(true);
			// generate the KNN's seed
			final int seed = Ninaloader.random.nextInt();

			// save the CB
			// ========================================
			long timer = System.currentTimeMillis();
			if (!cachedCB.containsKey(Long.valueOf(seed))) {
				// Vector<CBcached> cached = new Vector<CBcached>(
				// 5);
				CBcached cb = new CBcached(cbDone, barrierUpdate, seed, timer);
				// set the direction
				cb.setDirection(direction);

				cachedCB.put(Long.valueOf(seed), cb);
			}

			// =========================================
			Iterator<AddressIF> ier = randNodes.iterator();
			for (int index = 0; index < realNodeNo; index++) {

				final AddressIF dest = ier.next();
				// test dest
				if (dest.equals(target)) {
					continue;
				}

				// version number
				final int version = Ninaloader.random.nextInt();
				// added K neighbors
				Set<NodesPair> NearestNeighborIndex = new HashSet<NodesPair>(10);
				// save the target into cache, to avoid target being select as
				// candidate KNN node
				// Note: NearestNeighborIndex-1=k means there are k nodes

				NearestNeighborIndex.add(new NodesPair(target, target, 0));

				// record forbidden nodes
				Filter filter = null;

				Filter forbiddenFilter = SubSetManager.getEmptyFilter();
				SubSetManager subset = new SubSetManager(forbiddenFilter);
				// add the target
				subset.insertPeers2Subset(target);

				// log.debug("Bloom filter: " + filter.getEntryCount());
				// =====================================
				// filter
				// =====================================
				final SubSetClosestRequestMsg msg22 = new SubSetClosestRequestMsg(Ninaloader.me, Ninaloader.me, target,
						k, NearestNeighborIndex, version, seed, null, filter, forbiddenFilter);

				// set the direction
				msg22.setDirection(direction);

				// set the root
				msg22.setRoot(dest);

				// log.info("###############\n$: send out" + index
				// + "th farthestKNN query of " + Ninaloader.me + " to Neighbor
				// "
				// + dest + "\n###############");

				barrierUpdate.fork();

				if (target.equals(Ninaloader.me)) {

					msg22.targetCoordinates = MClient.primaryNC.getSystemCoords();
					log.debug(msg22.targetCoordinates.toString());

					comm.sendRequestMessage(msg22, dest, new ObjCommRRCB<SubSetClosestResponseMsg>() {

						protected void cb(CBResult result, final SubSetClosestResponseMsg responseMsg, AddressIF node,
								Long ts) {
							switch (result.state) {
							case OK: {
								log.debug("K Nearest Request received by new query node: ");
								// cdDone.call(result);
								// save the callbacks
								// add into a list

								break;
							}
							case ERROR:
							case TIMEOUT: {
								cbDone.call(result, null); // the returned
								// map is null
								break;
							}
							}
							// ----------------------
						}
						// --------------------------
					});

				} else {
					// get the relative coordinate of the remote node
					RelativeCoordinateRequestMsg msg = new RelativeCoordinateRequestMsg(Ninaloader.me);

					comm.sendRequestMessage(msg, target, new ObjCommRRCB<RelativeCoordinateResponseMsg>() {
						protected void cb(CBResult result, final RelativeCoordinateResponseMsg responseMsg,
								AddressIF node, Long ts) {
							switch (result.state) {
							case OK: {
								// System.out.println("$ ! We have received ACK
								// from"+responseMsg.from);
								// send the farthest query

								msg22.targetCoordinates = responseMsg.myRelativeCoordinate;

								comm.sendRequestMessage(msg22, dest, new ObjCommRRCB<SubSetClosestResponseMsg>() {

									protected void cb(CBResult result, final SubSetClosestResponseMsg responseMsg,
											AddressIF node, Long ts) {
										switch (result.state) {
										case OK: {
											// log.info("K Nearest Request
											// received by new query node: ");
											// cdDone.call(result);
											// save the callbacks
											// add into a list

											break;
										}
										case ERROR:
										case TIMEOUT: {
											cbDone.call(result, new ArrayList<NodesPair>()); // the
																								// returned
											// map is null
											break;
										}
										}
										// ----------------------
									}
									// --------------------------
								});

								break;
							}
							case ERROR:
							case TIMEOUT: {
								String error = target.toString(false) + "has not received  relative coordinate, as: "
										+ result.toString();
								log.warn(error);

								break;
							}
							}
							// barrier.join();
						}
					});
				}

			}
			randNodes.clear();
			long interval = 8 * 1000;
			EL.get().registerTimerCB(barrierUpdate, new CB0() {

				@SuppressWarnings("unchecked")
				protected void cb(CBResult result) {
					String errorString;
					// merge the results

					// if it does not appear, add
					// long curTime = System.currentTimeMillis();
					// double time=(curTime-)/1000;
					log.info("\n@@:\n All K farthest node search branches are reported \n");

					CBcached np = cachedCB.get(Long.valueOf(seed));

					if (np != null) {

						long curTimer = System.currentTimeMillis();
						long time = curTimer - np.timeStamp;
						// ElapsedKNNSearchTime.add(time);

						List<NodesPair> nps = new ArrayList<NodesPair>(5);
						List<AddressIF> tmpKNN = new ArrayList<AddressIF>(5);

						// List rtts=new ArrayList(5);
						Map<AddressIF, RemoteState<AddressIF>> maps = new HashMap<AddressIF, RemoteState<AddressIF>>(5);

						Set<NodesPair> nodePairs = np.nps;
						// iterate, we use the latency as the hash key
						if (nodePairs != null) {
							Iterator<NodesPair> ierNp = nodePairs.iterator();
							while (ierNp.hasNext()) {
								NodesPair cachedNP = ierNp.next();

								// skip myself
								if (cachedNP.startNode.equals(target)) {
									continue;
								}
								if (cachedNP.startNode.equals(target)) {
									continue;
								}

								if (cachedNP.rtt < 0) {
									continue;
								}

								if (!maps.containsKey(cachedNP.startNode)) {
									RemoteState<AddressIF> cached = new RemoteState<AddressIF>(
											(AddressIF) cachedNP.startNode);
									cached.addSample(cachedNP.rtt);
									maps.put((AddressIF) cachedNP.startNode, cached);

								} else {
									// averaged
									RemoteState<AddressIF> cached = maps.get(cachedNP.startNode);
									cached.addSample(cachedNP.rtt);
									maps.put((AddressIF) cachedNP.startNode, cached);
								}
							}
						}
						// }

						SortedList<Double> sortedL = new SortedList<Double>(new DoubleComp());
						HashMap<Double, AddressIF> unSorted = new HashMap<Double, AddressIF>(2);

						Iterator<Entry<AddressIF, RemoteState<AddressIF>>> ierMap = maps.entrySet().iterator();
						while (ierMap.hasNext()) {
							Entry<AddressIF, RemoteState<AddressIF>> tmp = ierMap.next();
							unSorted.put(Double.valueOf(tmp.getValue().getSample()), tmp.getKey());
							sortedL.add(Double.valueOf(tmp.getValue().getSample()));
						}

						if (sortedL.size() > 0) {
							// yes the results

							// remove unnecessary nodes
							while (sortedL.size() > k) {
								sortedL.remove(0);
							}
							Iterator<Double> ierRTT = sortedL.iterator();

							while (ierRTT.hasNext()) {
								Double item = ierRTT.next();

								AddressIF tmpNode = unSorted.get(item);
								// target
								if (tmpNode.equals(target)) {
									continue;
								}

								// full
								if (tmpKNN.size() == k) {
									break;
								} else {

									// add
									if (tmpKNN.contains(tmpNode)) {
										continue;
									} else {
										tmpKNN.add(tmpNode);
										NodesPair nodePair = new NodesPair(tmpNode, null, item.doubleValue());
										nodePair.elapsedTime = time;
										nps.add(nodePair);
									}
								}
							}

							//
							log.debug("K farthest nodes Number: " + nps.size());

							cbDone.call(CBResult.OK(), nps);

							// remove the callbacks
							cachedCB.remove(Long.valueOf(seed));

							// cachedCB.get(target).clear();
							// cachedCB.remove(target); //remove the callback
							// registered for arg1.target
							nps.clear();
							sortedL.clear();
							unSorted.clear();
							maps.clear();
							tmpKNN.clear();

						} else {
							log.debug("No RTT measurements!");
							randNodes.clear();
							cbDone.call(CBResult.ERROR(), null);
						}
					} else {
						log.debug("NO cachedCB!");
						randNodes.clear();
						cbDone.call(CBResult.ERROR(), null);
					}

				}
			});

		} // index
		else {
			randNodes.clear();
			cbDone.call(CBResult.ERROR(), null);
		}

	}

	/**
	 * based on constrained KNN search
	 */

	public void queryKNearestNeighbors(final AddressIF target, List<AddressIF> candidates, final int k,
			final CB1<List<NodesPair>> cbDone) {
		// TODO Auto-generated method stub
		// subManager.queryKNearestNeighbors(target, candidates, k, cbDone);

		int branches = MultipleRestart;
		final Set<AddressIF> randNodes = new HashSet<AddressIF>(10);

		// rings
		MClient.instance.g_rings.getRandomNodes(randNodes, branches);

		if (randNodes.contains(target)) {
			log.debug("$: we remove the target, then we start the KNN search process! ");
			randNodes.remove(target);
		}

		int realNodeNo = randNodes.size();

		log.debug("\nconstrained KNN\n!!!!!!!!!!!!!!!!!!!!!!!\nTotal branches: " + realNodeNo
				+ "\n!!!!!!!!!!!!!!!!!!!!!!!");

		final long timeStamp = System.currentTimeMillis();

		if (!randNodes.isEmpty()) {
			final Barrier barrierUpdate = new Barrier(true);
			// generate the KNN's seed
			final int seed = Ninaloader.random.nextInt();

			// save the CB
			// ========================================
			long timer = System.currentTimeMillis();
			if (!cachedCB.containsKey(Long.valueOf(seed))) {
				// Vector<CBcached> cached = new Vector<CBcached>(
				// 5);
				CBcached cb = new CBcached(cbDone, barrierUpdate, seed, timer);
				cachedCB.put(Long.valueOf(seed), cb);
			}

			// =========================================
			Iterator<AddressIF> ier = randNodes.iterator();
			for (int index = 0; index < realNodeNo; index++) {

				final AddressIF dest = ier.next();
				// test dest
				if (dest.equals(target)) {
					continue;
				}

				// version number
				final int version = Ninaloader.random.nextInt();
				// added K neighbors
				Set<NodesPair> NearestNeighborIndex = new HashSet<NodesPair>(10);
				// save the target into cache, to avoid target being select as
				// candidate KNN node
				// Note: NearestNeighborIndex-1=k means there are k nodes

				NearestNeighborIndex.add(new NodesPair(target, target, 0));

				// record forbidden nodes
				Filter filter = null;

				if (candidates == null || candidates.isEmpty()) {
					filter = null;
				} else {
					filter = SubSetManager.getEmptyFilter();
					SubSetManager subset = new SubSetManager(filter);
					subset.constructSubsets4KNN(candidates);
				}
				Filter forbiddenFilter = SubSetManager.getEmptyFilter();
				SubSetManager subset = new SubSetManager(forbiddenFilter);
				// add the target
				subset.insertPeers2Subset(target);

				// log.info("Bloom filter: " + filter.getEntryCount());
				// =====================================
				// filter
				// =====================================
				SubSetClosestRequestMsg msg22 = new SubSetClosestRequestMsg(Ninaloader.me, Ninaloader.me, target, k,
						NearestNeighborIndex, version, seed, null, filter, forbiddenFilter);

				// first find the farthest node
				final FarthestRequestMsg FMsg = new FarthestRequestMsg(Ninaloader.me, target, Ninaloader.me, msg22);

				// send to a random neighbor in Set upNeighbors
				// select a node in the ring

				// log.info("###############\n$: send out" + index
				// + "th KNN query of " + Ninaloader.me + " to Neighbor "
				// + dest + "\n###############");

				barrierUpdate.fork();

				if (target.equals(Ninaloader.me)) {

					FMsg.targetCoordinates = MClient.primaryNC.getSystemCoords();
					log.info(FMsg.targetCoordinates.toString());

					comm.sendRequestMessage(FMsg, dest, new ObjCommRRCB<FarthestResponseMsg>() {

						protected void cb(CBResult result, final FarthestResponseMsg responseMsg, AddressIF node,
								Long ts) {
							switch (result.state) {
							case OK: {
								log.debug("K Nearest Request received by new query node: ");
								// cdDone.call(result);
								// save the callbacks
								// add into a list

								break;
							}
							case ERROR:
							case TIMEOUT: {
								cbDone.call(result, null); // the returned
								// map is null
								break;
							}
							}
							// ----------------------
						}
						// --------------------------
					});

				} else {
					// get the relative coordinate of the remote node
					RelativeCoordinateRequestMsg msg = new RelativeCoordinateRequestMsg(Ninaloader.me);

					comm.sendRequestMessage(msg, target, new ObjCommRRCB<RelativeCoordinateResponseMsg>() {
						protected void cb(CBResult result, final RelativeCoordinateResponseMsg responseMsg,
								AddressIF node, Long ts) {
							switch (result.state) {
							case OK: {
								// System.out.println("$ ! We have received ACK
								// from"+responseMsg.from);
								// send the farthest query

								FMsg.targetCoordinates = responseMsg.myRelativeCoordinate;

								comm.sendRequestMessage(FMsg, dest, new ObjCommRRCB<FarthestResponseMsg>() {

									protected void cb(CBResult result, final FarthestResponseMsg responseMsg,
											AddressIF node, Long ts) {
										switch (result.state) {
										case OK: {
											// log.info("K Nearest Request
											// received by new query node: ");
											// cdDone.call(result);
											// save the callbacks
											// add into a list

											break;
										}
										case ERROR:
										case TIMEOUT: {
											cbDone.call(result, new ArrayList<NodesPair>()); // the
																								// returned
											// map is null
											break;
										}
										}
										// ----------------------
									}
									// --------------------------
								});

								break;
							}
							case ERROR:
							case TIMEOUT: {
								String error = target.toString(false) + "has not received  relative coordinate, as: "
										+ result.toString();
								log.warn(error);

								break;
							}
							}
							// barrier.join();
						}
					});
				}

			}
			randNodes.clear();
			long interval = 8 * 1000;
			EL.get().registerTimerCB(barrierUpdate, new CB0() {

				@SuppressWarnings("unchecked")
				protected void cb(CBResult result) {
					String errorString;
					// merge the results

					// if it does not appear, add
					// long curTime = System.currentTimeMillis();
					// double time=(curTime-)/1000;
					log.info("\n@@:\n All KNN branches are reported \n");

					CBcached np = cachedCB.get(Long.valueOf(seed));

					if (np != null) {

						long curTimer = System.currentTimeMillis();
						long time = curTimer - np.timeStamp;
						// ElapsedKNNSearchTime.add(time);

						List<NodesPair> nps = new ArrayList<NodesPair>(5);
						List<AddressIF> tmpKNN = new ArrayList<AddressIF>(5);

						// List rtts=new ArrayList(5);
						Map<AddressIF, RemoteState<AddressIF>> maps = new HashMap<AddressIF, RemoteState<AddressIF>>(5);

						Set<NodesPair> nodePairs = np.nps;
						// iterate, we use the latency as the hash key
						if (nodePairs != null) {
							Iterator<NodesPair> ierNp = nodePairs.iterator();
							while (ierNp.hasNext()) {
								NodesPair cachedNP = ierNp.next();

								// skip myself
								if (cachedNP.startNode.equals(target)) {
									continue;
								}
								if (cachedNP.startNode.equals(target)) {
									continue;
								}

								if (cachedNP.rtt < 0) {
									continue;
								}

								if (!maps.containsKey(cachedNP.startNode)) {
									RemoteState<AddressIF> cached = new RemoteState<AddressIF>(
											(AddressIF) cachedNP.startNode);
									cached.addSample(cachedNP.rtt);
									maps.put((AddressIF) cachedNP.startNode, cached);

								} else {
									// averaged
									RemoteState<AddressIF> cached = maps.get(cachedNP.startNode);
									cached.addSample(cachedNP.rtt);
									maps.put((AddressIF) cachedNP.startNode, cached);
								}
							}
						}
						// }

						SortedList<Double> sortedL = new SortedList<Double>(new DoubleComp());
						HashMap<Double, AddressIF> unSorted = new HashMap<Double, AddressIF>(2);

						Iterator<Entry<AddressIF, RemoteState<AddressIF>>> ierMap = maps.entrySet().iterator();
						while (ierMap.hasNext()) {
							Entry<AddressIF, RemoteState<AddressIF>> tmp = ierMap.next();
							unSorted.put(Double.valueOf(tmp.getValue().getSample()), tmp.getKey());
							sortedL.add(Double.valueOf(tmp.getValue().getSample()));
						}

						if (sortedL.size() > 0) {
							// yes the results

							Iterator<Double> ierRTT = sortedL.iterator();

							while (ierRTT.hasNext()) {
								Double item = ierRTT.next();

								AddressIF tmpNode = unSorted.get(item);
								// target
								if (tmpNode.equals(target)) {
									continue;
								}

								// full
								if (tmpKNN.size() == k) {
									break;
								} else {

									// add
									if (tmpKNN.contains(tmpNode)) {
										continue;
									} else {
										tmpKNN.add(tmpNode);
										NodesPair nodePair = new NodesPair(tmpNode, null, item.doubleValue());
										nodePair.elapsedTime = time;
										nps.add(nodePair);
									}
								}
							}

							//
							log.debug("KNN Number: " + nps.size());

							cbDone.call(CBResult.OK(), nps);

							// remove the callbacks
							cachedCB.remove(Long.valueOf(seed));

							// cachedCB.get(target).clear();
							// cachedCB.remove(target); //remove the callback
							// registered for arg1.target
							nps.clear();
							sortedL.clear();
							unSorted.clear();
							maps.clear();
							tmpKNN.clear();

						} else {
							log.debug("No RTT measurements!");
							randNodes.clear();
							cbDone.call(CBResult.ERROR(), null);
						}
					} else {
						log.debug("NO cachedCB!");
						randNodes.clear();
						cbDone.call(CBResult.ERROR(), null);
					}

				}
			});

		} // index
		else {
			randNodes.clear();
			cbDone.call(CBResult.ERROR(), null);
		}

	}

	/**
	 * answer the subset based KNN
	 */
	public void receiveClosestQuery(final Coordinate targetCoordinate, final SubSetClosestRequestMsg req,
			final StringBuffer errorBuffer) {

		log.debug("~~~~~~~~~~~~~~\n$: Receive constrained KNN search from: " + req.from + " to " + req.target
				+ " Root: " + req.root + "\n~~~~~~~~~~~~~~");

		if (req != null) {

			// ===========================================================
			// last hop node
			AddressIF from = null;
			if (req.getHopRecords() != null && !req.getHopRecords().isEmpty()) {
				from = req.getHopRecords().get(req.getHopRecords().size() - 1);
			}

			// not cached yet, and this node is not the request node
			// if the root,
			if (Ninaloader.me.equals(req.getRoot())) {
				from = null;
			}

			final AddressIF LastHopNode = from;
			// log.info("$: message is from: "+LastHopNode );
			// ===========================================================

			// repeated KNN request, since I have been included
			// TODO: need to be changed to a list of vectors

			SubSetManager subset = new SubSetManager(req.getForbiddenFilter());
			// found, repeated KNN
			if (subset.checkNode(Ninaloader.me)) {

				// double[] rtt=new double[1];
				// ncManager.isCached(req.target, rtt, -1);
				// NodesPair np=new NodesPair(Ninaloader.me,req.target,rtt[0]);

				// if (!req.getNearestNeighborIndex().contains(np)) {
				// log.info("FALSE assert in the forbidden filter!, we do not
				// found a match!");

				// not null, repeated query msg
				// I have registered the address for target req.target,
				// since I am a KNN of req.target
				// not null, not myself
				System.err
						.println("+++++++++++++++++++\n Repeated KNN from " + req.getFrom() + "\n++++++++++++++++++++");

				if (req.getFrom() != null && !req.getFrom().equals(Ninaloader.me)) {
					// not in

					// in, but not saved

					RepeatedSubSetRequestMsg rmsg = new RepeatedSubSetRequestMsg(req.getOriginFrom(), Ninaloader.me,
							req.getTarget(), req.getK(), req.getNearestNeighborIndex(), req.getVersion(), req.getSeed(),
							req.getRoot(), req.getFilter(), req.getForbiddenFilter());

					int len = req.getHopRecords().size();
					// remove last element in hops
					if (len > 0) {
						req.getHopRecords().remove(len - 1);
					}
					rmsg.HopRecords.addAll(req.getHopRecords());
					// relative coordinate
					rmsg.targetCoordinates = targetCoordinate;

					// direction
					rmsg.setDirection(req.getDirection());

					// ====================================================
					comm.sendRequestMessage(rmsg, req.getFrom(), new ObjCommRRCB<RepeatedSubSetResponseMsg>() {

						protected void cb(CBResult result, final RepeatedSubSetResponseMsg responseMsg, AddressIF node,
								Long ts) {
							switch (result.state) {
							case OK: {
								log.debug("repeated request acked ");
								// cdDone.call(result);
								break;
							}
							case ERROR:
							case TIMEOUT: {
								// cdDone.call(result);
								break;
							}
							}
							// ----------------------
						}
						// --------------------------
					});
					return;
				} else {
					// yes, myself, backtracking
					CompleteSubsetSearchRequestMsg msg3 = CompleteSubsetSearchRequestMsg.makeCopy(req);
					msg3.from = Ninaloader.me;

					returnKNN2Origin(msg3);

					/*
					 * double []lat=new double[1];
					 * ncManager.isCached(req.target, lat, -1);
					 * backtracking(req,lat[0], errorBuffer);
					 */
					return;

				}
			}

			// not the target itself, not repeated KNN node, here is the main
			// logic

			// if the nearest nodes reach requirements, return
			if (req.getNearestNeighborIndex() != null && (req.getNearestNeighborIndex().size() - 1) >= req.getK()) {
				log.debug(" $: completed! ");
				CompleteSubsetSearchRequestMsg msg3 = CompleteSubsetSearchRequestMsg.makeCopy(req);
				msg3.from = Ninaloader.me;
				this.returnKNN2Origin(msg3);
				return;
			}

			// else continue

			// TODO: if last hop fails

			// log.info("$: last hop: " + LastHopNode);
			// to confirm whether I am a new nearest node for target

			// send the target a gossip msg
			// LOWTODO could bias which nodes are sent based on his coord
			final long sendStamp = System.nanoTime();

			ncManager.doPing(req.getTarget(), new CB2<Double, Long>() {

				protected void cb(CBResult result, Double latency, Long timer) {
					// TODO Auto-generated method stub
					final double[] rtt = { (System.nanoTime() - sendStamp) / 1000000d };
					switch (result.state) {
					case OK: {

						if (latency.doubleValue() >= 0) {
							rtt[0] = latency.doubleValue();
						}
						//
						// TODO: smoothed RTT
						// save the latency measurements

						double[] latencyNC = new double[1];
						// instance.processSample(req.target, latency, can_add,
						// curr_time, pendingNeighbors, offset)
						// add the coordinate of remote node
						// instance.g_rings.NodesCoords.put(req.getTarget(),
						// targetCoordinate);

						Coordinate new_targetCoordinate = NCClient.updateProxyTargetCoordinate(req.getTarget(),
								req.targetCoordinates, req.targetCoordinates.r_error, MClient.primaryNC.sys_coord,
								MClient.primaryNC.getSystemError(), rtt[0]);
						req.targetCoordinates = new_targetCoordinate.makeCopy();
						new_targetCoordinate.clear();

						// update my coordinate
						MClient.primaryNC.simpleCoordinateUpdate(req.getTarget(), req.targetCoordinates,
								req.targetCoordinates.r_error, rtt[0], System.currentTimeMillis());

						/*
						 * MClient.instance.processSample(req.getTarget(),rtt[0]
						 * , true, System.currentTimeMillis(),
						 * pendingNeighbors,AbstractNNSearchManager.
						 * offsetLatency);
						 */

						// cache history
						// cachedHistoricalNeighbors.addElement(new
						// NodesPair<AddressIF>(req.getTarget(),Ninaloader.me,rtt[0]));

						// if(isCached(req.getTarget(), rtt)){
						// rtt[0]=latencyNC[0];
						// }
						// use the offset to select nodes
						// final double lat
						// =rtt[0]+AbstractNNSearchManager.offsetLatency;
						final double lat = rtt[0];

						// request nodes to probe to target nodes
						final Vector<AddressIF> ringMembers = new Vector<AddressIF>(10);

						/*
						 * log.info("@@@@@@@@@@@@@@@@@@@@@@@@\n Node: " +
						 * Ninaloader.me + " is: " + lat + " from Node: " +
						 * req.getTarget() + "\n@@@@@@@@@@@@@@@@@@@@@@@@");
						 */

						if (!AbstractNNSearchManager.useNormalRingRange) {
							MClient.instance.g_rings.fillVector(req.getNearestNeighborIndex(), req.getTarget(), lat,
									lat, betaRatio, ringMembers, AbstractNNSearchManager.offsetLatency);

							// add perturbation
							Set<AddressIF> randomSeeds = new HashSet<AddressIF>(
									KNNManager.defaultNodesForRandomContact);
							// randomSeeds.addAll(ringMembers);

							MClient.instance.g_rings.getNodesWithMaximumNonEmptyRings(req.getNearestNeighborIndex(),
									randomSeeds, KNNManager.defaultNodesForRandomContact);
							// ringMembers.clear();
							ringMembers.addAll(randomSeeds);
							randomSeeds.clear();

						} else {

							Coordinate targetRelCoord = targetCoordinate;

							// TODO: knn search
							// find k nearest or k farthest nodes
							// 0 closest; 1; farthest
							/*
							 * if(req.getDirection()==KNNManager.ClosestSearch){
							 * //let ring members to ping the target
							 * instance.g_rings.fillVectorBasedOnKClosestNode(
							 * Ninaloader.me,req.getNearestNeighborIndex()
							 * ,targetRelCoord, lat, ringMembers,
							 * AbstractNNSearchManager.offsetLatency); }else
							 * if(req.getDirection()==KNNManager.FarthestSearch)
							 * {
							 * instance.g_rings.fillVectorBasedOnKFarthestNode(
							 * Ninaloader.me,req.getNearestNeighborIndex()
							 * ,targetRelCoord, lat, ringMembers,
							 * AbstractNNSearchManager.offsetLatency); }
							 */
						}

						// not target from rings
						if (ringMembers.contains(req.getTarget())) {
							ringMembers.remove(req.getTarget());
						}

						// log.info("The size of returned members: "
						// + ringMembers.size());

						SubSetManager subset2 = new SubSetManager(req.getForbiddenFilter());
						// postprocess the ring members
						Iterator<AddressIF> ierRing = ringMembers.iterator();
						while (ierRing.hasNext()) {
							AddressIF tmpNode = ierRing.next();
							if (subset2.checkNode(tmpNode)) {
								// found forbidden nodes
								ierRing.remove();
							}
						}

						// also remove the backtracking node
						Iterator<AddressIF> ierBack = req.getHopRecords().iterator();
						while (ierBack.hasNext()) {

							AddressIF tmp = ierBack.next();
							if (ringMembers.contains(tmp)) {
								log.debug("Remove forbidden backtracking node: " + tmp);
								ringMembers.remove(tmp);
							}
						}

						// ---------------------------------

						// boolean for current nodes
						boolean isNearest = false;
						boolean another = false;
						// returned ID is null
						// Note: if the ringMembers contain the target
						// node, current node may not be the nearest
						// nodes
						if (ringMembers == null || (ringMembers.size() == 0)) {

							// the originNode is the only one node
							// no nearer nodes, so return me
							// log.info(Ninaloader.me
							// + " is the closest to: "
							// + req.target
							// + " from g_rings.fillVector @: "
							// + Ninaloader.me + " Target: "
							// + req.target);

							// TODO: add the clustering based 1-step
							// search, the ring must be balanced

							// backtrack
							// find last hop
							// TODO: assume that K<=N
							// AddressIF lastHop=null;

							// I am the lastHop
							// TODO change the beta according to
							// current rings, 9-24
							// remove condition:
							// ||req.K<=req.NearestNeighborIndex.size()-1,
							// 9-24
							if (LastHopNode == null || LastHopNode.equals(Ninaloader.me)) {
								log.info("Finish! ");

								SubSetManager subset = new SubSetManager(req.getFilter());
								if (req.getFilter() == null || subset.checkNode(Ninaloader.me)) {

									req.getNearestNeighborIndex()
											.add(new NodesPair(Ninaloader.me, req.getTarget(), lat, LastHopNode));

								}
								subset = new SubSetManager(req.getForbiddenFilter());
								subset.insertPeers2Subset(Ninaloader.me);

								CompleteSubsetSearchRequestMsg msg3 = CompleteSubsetSearchRequestMsg.makeCopy(req);
								msg3.from = Ninaloader.me;
								returnKNN2Origin(msg3);
								return;

							} else {

								// log.info(" find a new last hop!!!");

								backtracking(req, lat, errorBuffer);
							}

						} else {

							// final double refRTT=lat;
							// TODO: ask a set of nodes to ping a set of
							// nodes

							TargetProbes(ringMembers, req.getTarget(), new CB1<String>() {

								protected void cb(CBResult ncResult, String errorString) {
									switch (ncResult.state) {
									case OK: {
										// log.debug("$: Target Probes are
										// issued for KNN");
									}
									case ERROR:
									case TIMEOUT: {
										break;
									}
									}
								}
							}, new CB2<Set<NodesPair>, String>() {

								protected void cb(CBResult ncResult, Set<NodesPair> nps2, String errorString) {

									switch (ncResult.state) {
									case OK: {
										ringMembers.clear();
										final Set<NodesPair> nps1 = new HashSet<NodesPair>(1);
										nps1.addAll(nps2);

										// +++++++++++++++++++++++++++++++++++++
										// use the rule to select next hop nodes
										AddressIF target = req.target;
										Set<NodesPair> closestNodes = new HashSet<NodesPair>(1);
										SubSetManager subset = new SubSetManager(req.getForbiddenFilter());

										SubSetManager subsetFilter = new SubSetManager(req.getFilter());

										// select the closest or farthest node
										// based on the direction;
										// Note: 0- closest; 1- farthest
										AddressIF returnID = null;
										// ====================================
										// select based rules
										if (req.getDirection() == KNNManager.ClosestSearch) {
											returnID = ClosestNodeRule(lat, target, nps1, closestNodes, subset,
													LastHopNode);
										} else if (req.getDirection() == KNNManager.FarthestSearch) {
											returnID = farthestNodeRule(lat, target, nps1, closestNodes, subset,
													LastHopNode);

										}
										// ====================================

										// record new nearest nodes
										if (closestNodes != null && !closestNodes.isEmpty()) {
											Iterator<NodesPair> ierNode = closestNodes.iterator();

											while (ierNode.hasNext()) {
												NodesPair tmp = ierNode.next();
												AddressIF from = (AddressIF) tmp.startNode;
												// add, only when it is closer
												// than me, and does not reside
												// on the backtrack path

												if (!subset.checkNode(from)) {
													subset.insertPeers2Subset(from);
													// checked in candidates
													if (req.getFilter() == null || subsetFilter.checkNode(from)) {
														req.getNearestNeighborIndex().add(tmp);
													}
												}

											}
										}

										// +++++++++++++++++++++++++++++++++++++
										if (returnID == null) {
											// found nearest, we backtrack
											// I am the lastHop
											// remove 9-24
											// remove last hop

											backtracking(req, lat, errorBuffer);

										} else {
											// returnID is not
											// Ninaloader.me
											//
											final SubSetClosestRequestMsg msg = new SubSetClosestRequestMsg(
													req.getOriginFrom(), Ninaloader.me, req.getTarget(), req.getK(),
													req.getNearestNeighborIndex(), req.getVersion(), req.getSeed(),
													req.getRoot(), req.getFilter(), req.getForbiddenFilter());

											// current hop
											msg.getHopRecords().addAll(req.getHopRecords());
											msg.addCurrentHop(Ninaloader.me);
											msg.targetCoordinates = targetCoordinate;
											// copy the direction
											msg.setDirection(req.getDirection());

											// me
											/*
											 * if(returnID.equals(Ninaloader.me)
											 * ){ receiveClosestQuery(msg,
											 * errorBuffer); }else{
											 */

											comm.sendRequestMessage(msg, returnID,
													new ObjCommRRCB<SubSetClosestResponseMsg>() {

												protected void cb(CBResult result,
														final SubSetClosestResponseMsg responseMsg, AddressIF node,
														Long ts) {
													switch (result.state) {
													case OK: {
														log.debug("Request Acked by new query node: ");
														// cdDone.call(result);
														break;
													}
													case ERROR:
													case TIMEOUT: {
														// cdDone.call(result);
														/*
														 * findNewClosestNxtNode(
														 * sortedL, msg,
														 * LastHopNode, lat);
														 */
														Iterator ier2 = nps1.iterator();
														SortedList<NodesPair> sortedL = new SortedList<NodesPair>(
																new NodesPairComp());
														while (ier2.hasNext()) {

															NodesPair tmp = (NodesPair) ier2.next();
															// invalid
															// measurements
															if (tmp.rtt < 0) {
																continue;
															}
															// the same with
															// target
															if (tmp.startNode.equals(req.getTarget())) {
																continue;
															}
															// if tmp.lat <
															// current
															// latency from
															// me

															// 10-18- add the
															// beta constriant,
															// to reduce the
															// elapsed time
															// if < beta, return
															// current node
															if (tmp.rtt <= lat) {
																sortedL.add(tmp);
															}
														}
														findNewClosestNxtNode(sortedL, msg, lat, errorBuffer);
														break;
													}
													}
													// ----------------------
												}
												// --------------------------

											});// }
										}
										break;
									}
									case ERROR:
									case TIMEOUT: {
										System.err.println(
												"KNN Search: Target probes to " + req.getTarget() + " FAILED!"); // the
										// returned
										// map
										// is
										// null
										// we move backward
										backtracking(req, lat, errorBuffer);
										break;
									}
									}

								}//
							}

							);
						} // end of target probe

						// cdDone.call(result);
						break;
					}

					case TIMEOUT:
					case ERROR: {

						String error = "RTT request to " + req.getTarget().toString(false) + " failed:"
								+ result.toString();
						log.warn(error);
						if (errorBuffer.length() != 0) {
							errorBuffer.append(",");
						}
						errorBuffer.append(error);
						// cdDone.call(result);

						// the target can not be reached, we finish the KNN
						// search process
						CompleteSubsetSearchRequestMsg msg3 = CompleteSubsetSearchRequestMsg.makeCopy(req);

						// direction
						msg3.setDirection(req.getDirection());

						msg3.from = Ninaloader.me;
						// CompleteNNRequestMsg
						// msg3=CompleteNNRequestMsg.makeCopy(req);
						returnKNN2Origin(msg3);

						break;
					}

					}

				}

			});

		}
	}

	class SubSetClosestReqHandler extends ResponseObjCommCB<SubSetClosestRequestMsg> {

		protected void cb(CBResult arg0, final SubSetClosestRequestMsg arg1, AddressIF arg2, Long arg3,
				final CB1<Boolean> arg4) {

			// TODO Auto-generated method stub
			final AddressIF answerNod = arg1.getFrom();
			final AddressIF original = arg1.getOriginFrom();
			final AddressIF to = arg1.getTarget();
			// System.out.println("kNN Request from: "+answerNod);

			final long msgID = arg1.getMsgId();

			SubSetClosestResponseMsg msg2 = new SubSetClosestResponseMsg(original, Ninaloader.me);

			final StringBuffer errorBuffer = new StringBuffer();
			sendResponseMessage("Closest", answerNod, msg2, msgID, null, arg4);

			execKNN.execute(new Runnable() {

				public void run() {

					receiveClosestQuery(arg1.targetCoordinates, arg1, errorBuffer);
					/*
					 * } });
					 */
					//
					/*
					 * receiveClosestQuery(original, to,errorBuffer, new CB0(){
					 * protected void cb(CBResult result) {
					 * 
					 * switch (result.state) { case OK: { // Initialise the
					 * external APIs
					 * 
					 * break; } default: { String error = "fail to find closest"
					 * ; log.warn(error); break; } } } });
					 */

				}
			});

		}

	}

	// ----------------------------------------------
	public class SubSetRepeatedNNReqHandler extends ResponseObjCommCB<RepeatedSubSetRequestMsg> {

		protected void cb(CBResult arg0, final RepeatedSubSetRequestMsg arg1, AddressIF arg2, Long arg3,
				final CB1<Boolean> arg4) {

			// independent thread
			/*
			 * execKNN.execute(new Runnable(){
			 * 
			 * 
			 * public void run() {
			 */

			// Auto-generated method stub
			final AddressIF answerNod = arg1.from;
			log.info("repeated from: " + answerNod);
			final long msgID = arg1.getMsgId();

			RepeatedSubSetResponseMsg msg2 = new RepeatedSubSetResponseMsg(Ninaloader.me);
			sendResponseMessage("repeated", answerNod, msg2, msgID, null, arg4);

			// backtracking process
			// backtracking process
			// from=lastHop
			int len = arg1.HopRecords.size();
			AddressIF from1 = null;
			if (len > 0) {
				from1 = arg1.HopRecords.get(len - 1);
				// arg1.HopRecords.remove(len-1);
			}

			final AddressIF from = from1;
			/*
			 * null; Vector<backtracking> vList =
			 * cachedBackTrack.get(arg1.target); if (vList != null) {
			 * Iterator<backtracking> ier = vList.iterator(); if (ier != null) {
			 * while (ier.hasNext()) { backtracking tmp = ier.next(); if
			 * ((arg1.OriginFrom.equals(tmp.OriginFrom)) &&
			 * (arg1.target.equals(tmp.target)) && (arg1.version == tmp.version)
			 * && (arg1.seed == tmp.seed)) { from = tmp.lastHop; break; } } } }
			 */

			execKNN.execute(new Runnable() {

				public void run() {

					SubSetClosestRequestMsg msg1 = new SubSetClosestRequestMsg(arg1.OriginFrom, from, arg1.target,
							arg1.K, arg1.NearestNeighborIndex, arg1.version, arg1.seed, arg1.root, arg1.filter,
							arg1.ForbiddenFilter);
					// hop
					if (!arg1.HopRecords.isEmpty()) {
						msg1.getHopRecords().addAll(arg1.HopRecords);
					}
					final StringBuffer errorBuffer = new StringBuffer();
					// relative coordinate
					msg1.targetCoordinates = arg1.targetCoordinates;
					// direction
					msg1.setDirection(arg1.getDirection());

					receiveClosestQuery(arg1.targetCoordinates, msg1, errorBuffer);

				}
			});

			/*
			 * } });
			 */
		}

	}

	/**
	 * answer the callback of intermediate nodes
	 * 
	 * @author ericfu
	 * 
	 */
	class SubSetTargetLockReqHandler extends ResponseObjCommCB<SubSetTargetLocatedRequestMsg> {

		protected void cb(CBResult arg0, final SubSetTargetLocatedRequestMsg arg1, AddressIF arg2, Long arg3,
				final CB1<Boolean> arg4) {

			// independent thread
			/*
			 * execKNN.execute(new Runnable(){
			 * 
			 * 
			 * public void run() {
			 */
			//
			final AddressIF answerNod = arg1.from; // original from
			// Set<NodesPair> NNs = arg1.NearestNeighborIndex;

			// backtracking process
			// from=lastHop
			int len = arg1.HopRecords.size();
			AddressIF from1 = null;
			if (len > 0) {
				from1 = arg1.HopRecords.get(len - 1);
				// arg1.HopRecords.remove(len-1);
			}
			final AddressIF from = from1;

			final long msgID = arg1.getMsgId();

			SubSetTargetLocatedResponseMsg msg22 = new SubSetTargetLocatedResponseMsg(Ninaloader.me);
			sendResponseMessage("TargetLock", answerNod, msg22, msgID, null, arg4);

			// reach the K limit, or there is no backtrack node

			// TODO: error
			// from maybe null

			execKNN.execute(new Runnable() {

				public void run() {

					SubSetClosestRequestMsg msg1 = new SubSetClosestRequestMsg(arg1.OriginFrom, from, arg1.target,
							arg1.K, arg1.getNearestNeighborIndex(), arg1.version, arg1.seed, arg1.root, arg1.filter,
							arg1.ForbiddenFilter);

					// hop records
					msg1.getHopRecords().addAll(arg1.HopRecords);
					// relative coordinate
					msg1.targetCoordinates = arg1.targetCoordinates;
					// direction
					msg1.setDirection(arg1.getDirection());

					final StringBuffer errorBuffer = new StringBuffer();

					receiveClosestQuery(arg1.targetCoordinates, msg1, errorBuffer);

				}
			});

			/*
			 * } });
			 */
		}

	}

	/**
	 * completed the query process
	 * 
	 * @author ericfu
	 * 
	 */
	class SubSetCompleteNNReqHandler extends ResponseObjCommCB<CompleteSubsetSearchRequestMsg> {

		@SuppressWarnings("unchecked")

		protected void cb(CBResult arg0, final CompleteSubsetSearchRequestMsg arg1, AddressIF arg2, Long arg3,
				final CB1<Boolean> arg4) {

			// final AddressIF answerNod=arg1.from; //original from

			// acked
			// ------------------------------------------
			CompleteSubSubsetSearchResponseMsg msg22 = new CompleteSubSubsetSearchResponseMsg(Ninaloader.me);
			msg22.setResponse(true);
			msg22.setMsgId(arg1.getMsgId());
			// ------------------------------------------
			sendResponseMessage("CompleteNN", arg1.from, msg22, arg1.getMsgId(), null, arg4);
			// independent thread
			execKNN.execute(new Runnable() {

				public void run() {

					// postProcessing
					postProcessKNN(arg1);

				}
			});

		}

	}

	public void returnKNN2Origin(CompleteSubsetSearchRequestMsg msg3) {

		// I should not work, since I have been a KNNer
		if (!Ninaloader.me.equals(msg3.OriginFrom)) {

			log.debug("!!!!!!!!!!!!!!!!!!!!!!\nComplete @ TargetLockReqHandler\n!!!!!!!!!!!!!!!!!!!!!!");
			// TODO: after the normal search process, we add the extra search
			// steps here

			// query completed, send msg to query nodes
			// send the result
			comm.sendRequestMessage(msg3, msg3.OriginFrom, new ObjCommRRCB<CompleteSubSubsetSearchResponseMsg>() {

				protected void cb(CBResult result, final CompleteSubSubsetSearchResponseMsg responseMsg, AddressIF node,
						Long ts) {
					switch (result.state) {
					case OK: {

						// acked by original query node
						// cdDone.call(result);
						break;
					}
					case ERROR:
					case TIMEOUT: {
						// cdDone.call(result);
						break;
					}
					}
				}
			});

		} else {
			// yes I am the originFrom
			this.postProcessKNN(msg3);
		}

	}

	/**
	 * I am the orginFrom node, which send the KNN query
	 */
	void postProcessKNN(CompleteSubsetSearchRequestMsg arg1) {

		final Set<NodesPair> NNs = arg1.getNearestNeighborIndex();
		// callback invoked

		CBcached curCB = cachedCB.get(Long.valueOf(arg1.seed));
		if (curCB != null) {

			// match the seed, if it does not match, it means it is
			// another KNN search
			log.info("\n@@:\n constrained KNN branch is reported from " + arg1.from);
			curCB.nps.addAll(NNs);
			// done, wake up the barrier
			// curCB.wake.setNumForks(1);

			curCB.wake.join();
		}

		// notify nodes that cache the query records to withdraw

	}

	/**
	 * backtracking
	 * 
	 * @param req
	 * @param LastHopNode
	 */
	private void backtracking(final SubSetClosestRequestMsg req, final double lat, final StringBuffer errorBuffer) {

		// remove last hop

		AddressIF LastHopNode1 = null;
		int hopLen = req.getHopRecords().size();
		if (hopLen > 0) {
			// the last hop
			LastHopNode1 = req.getHopRecords().get(hopLen - 1);
			req.getHopRecords().remove(hopLen - 1);
		}

		final AddressIF LastHopNode = LastHopNode1;

		// ====================================================
		// not included
		SubSetManager subset = new SubSetManager(req.getForbiddenFilter());
		if (!subset.checkNode(Ninaloader.me)) {

			SubSetManager subsetFilter = new SubSetManager(req.getFilter());
			// candidates
			if (req.getFilter() == null || subsetFilter.checkNode(Ninaloader.me)) {
				NodesPair tmp = new NodesPair(Ninaloader.me, null, lat, LastHopNode);
				req.getNearestNeighborIndex().add(tmp);
			}

			subset.insertPeers2Subset(Ninaloader.me);
			// OK send originFrom a message
			/*
			 * log.info(Ninaloader.me + " is the closest to: " + req.getTarget()
			 * + " from TargetProbes @: " + Ninaloader.me + " Target: " +
			 * req.getTarget());
			 */

		}
		// -------------------------------------
		// due to "final" constriant
		// register an item to registeredNearestNodes, stop answering the
		// corresponding request
		/*
		 * log.info("\n----------------------\n KNN hops: " +
		 * req.getHopRecords().size() + "\n----------------------\n");
		 */

		// forward back

		if (LastHopNode != null && !LastHopNode.equals(Ninaloader.me)) {

			final SubSetTargetLocatedRequestMsg msg = new SubSetTargetLocatedRequestMsg(Ninaloader.me,
					req.getOriginFrom(), req.getTarget(), req.getK(), req.getNearestNeighborIndex(), req.getVersion(),
					req.getSeed(), req.getRoot(), req.getFilter(), req.getForbiddenFilter());

			msg.HopRecords.addAll(req.getHopRecords());
			// relative coordinate
			msg.targetCoordinates = req.targetCoordinates;

			// set the search direction
			msg.setDirection(req.getDirection());

			// different nodes from target
			comm.sendRequestMessage(msg, LastHopNode, new ObjCommRRCB<SubSetTargetLocatedResponseMsg>() {

				protected void cb(CBResult result, final SubSetTargetLocatedResponseMsg responseMsg, AddressIF node,
						Long ts) {

					switch (result.state) {
					case OK: {
						// acked by original query node
						// cdDone.call(result);
						break;
					}
					case ERROR:
					case TIMEOUT: {
						// cdDone.call(result);
						findAliveAncestor(msg, LastHopNode);
						break;
					}
					}
				}
			});

		} else {
			// myself, if it is the sebacktracking, we finish
			CompleteNNRequestMsg msg3 = CompleteNNRequestMsg.makeCopy(req);
			msg3.from = Ninaloader.me;
			// send the result
			returnKNN2Origin(msg3);
			return;

		}
	}

	private void findAliveAncestor(final SubSetTargetLocatedRequestMsg msg, AddressIF lastHopNode) {
		// TODO Auto-generated method stub
		final List<AddressIF> ancestors = msg.HopRecords;

		boolean ContactAsker = false;

		if (ancestors.size() > 0) {

			System.out.println("\n==================\n The total No. of ancestors: " + ancestors.size()
					+ "\n==================\n");

			final int[] bitVector = new int[ancestors.size()];
			for (int i = 0; i < ancestors.size(); i++) {
				bitVector[i] = 0;
			}
			AddressIF ClosestAncestor = null;

			Set<AddressIF> tmpList = new HashSet<AddressIF>(1);
			tmpList.addAll(ancestors);

			collectRTTs(tmpList, new CB2<Set<NodesPair>, String>() {

				protected void cb(CBResult ncResult, Set<NodesPair> nps, String errorString) {
					// send data request message to the core node

					// TODO Auto-generated method stub
					int rank = Integer.MAX_VALUE;

					if (nps != null && nps.size() > 0) {
						System.out.println("\n==================\n Alive No. of ancestors: " + nps.size()
								+ "\n==================\n");
						// find the nodes that are closest to the root
						Iterator<NodesPair> ier = nps.iterator();
						while (ier.hasNext()) {
							NodesPair tmp = ier.next();
							int index = ancestors.indexOf(tmp.endNode);

							// failed measurement
							if (tmp.rtt < 0) {
								index = -1;
							}

							if (index < 0) {
								continue;
							} else {
								// found the element, and it is smaller than
								// rank, i.e., it is closer to the target
								bitVector[index] = 1;
							}
						}

					} else {
						// all nodes fail, so there are no alive nodes
						rank = -1;
					}

					// iterate the bitVector
					// ===============================
					int start = -1;
					boolean first = false;
					int ZeroCounter = 0;
					for (int i = 0; i < ancestors.size(); i++) {
						// first failed node
						if (!first && (bitVector[i] == 0)) {
							start = i;
							first = true;
						}
						if (bitVector[i] == 0) {
							ZeroCounter++;
						}
					}

					// all empty, or the root is empty
					if (ZeroCounter == ancestors.size() || start == 0) {
						// we do not have any node to report, no backtracking
						// nodes !

						CompleteSubsetSearchRequestMsg msg3 = CompleteSubsetSearchRequestMsg.makeCopy(msg);
						msg3.from = Ninaloader.me;
						returnKNN2Origin(msg3);
						return;

					} else {

						if (start == -1) {
							// it means no ancestors are dead!
							start = ancestors.size() - 1;
						} else {
							// we found an ancestor node
							// remove unnecessary ancestors
							final List<AddressIF> missing = new ArrayList<AddressIF>(1);
							missing.addAll(ancestors.subList(start, ancestors.size()));

							ancestors.removeAll(missing);
						}
						int newLen = ancestors.size();
						// total fail
						if (newLen == 0) {
							CompleteSubsetSearchRequestMsg msg3 = CompleteSubsetSearchRequestMsg.makeCopy(msg);
							msg3.from = Ninaloader.me;
							returnKNN2Origin(msg3);
							return;
						}

						final AddressIF LastHopNode = ancestors.get(newLen - 1);
						ancestors.remove(newLen - 1);

						// different nodes from target
						comm.sendRequestMessage(msg, LastHopNode, new ObjCommRRCB<SubSetTargetLocatedResponseMsg>() {

							protected void cb(CBResult result, final SubSetTargetLocatedResponseMsg responseMsg,
									AddressIF node, Long ts) {

								switch (result.state) {
								case OK: {
									// acked by original query node
									// cdDone.call(result);
									System.out.println("We find new lastHop node: " + LastHopNode);

									break;
								}
								case ERROR:
								case TIMEOUT: {
									// cdDone.call(result);
									findAliveAncestor(msg, LastHopNode);
									break;
								}
								}
							}
						});
					}

					// ===============================
				}

			});

			tmpList.clear();

		} else {
			// we do not have any node to report, no backtracking nodes !
			ContactAsker = true;
			// contact the origin node
			if (ContactAsker) {
				CompleteSubsetSearchRequestMsg msg3 = CompleteSubsetSearchRequestMsg.makeCopy(msg);
				msg3.from = Ninaloader.me;
				returnKNN2Origin(msg3);
				return;
			}
		}

	}

	/**
	 * If next-hop node fails, we find new candidate from the sortedList
	 * 
	 * @param sortedL
	 * @param msg
	 * @param lastHopNode
	 */
	private void findNewClosestNxtNode(final SortedList<NodesPair> sortedL, final SubSetClosestRequestMsg msg,
			final double lat, final StringBuffer errorBuffer) {

		// TODO Auto-generated method stub
		if (sortedL != null && sortedL.size() > 0) {

			final List<AddressIF> candidates = new ArrayList<AddressIF>(1);
			Iterator<NodesPair> ier = sortedL.iterator();
			while (ier.hasNext()) {
				candidates.add((AddressIF) ier.next().startNode);
			}

			final int[] bitVector = new int[candidates.size()];
			for (int i = 0; i < candidates.size(); i++) {
				bitVector[i] = 0;
			}

			Set<AddressIF> tmpList = new HashSet<AddressIF>(1);
			tmpList.addAll(candidates);

			collectRTTs(tmpList, new CB2<Set<NodesPair>, String>() {

				protected void cb(CBResult ncResult, Set<NodesPair> nps, String errorString) {

					if (nps != null && nps.size() > 0) {
						System.out.println("\n==================\n Alive No. of next-hop node: " + nps.size()
								+ "\n==================\n");
						// find the nodes that are closest to the root
						Iterator<NodesPair> ier = nps.iterator();
						while (ier.hasNext()) {
							NodesPair tmp = ier.next();
							int index = candidates.indexOf(tmp.endNode);

							// failed measurement
							if (tmp.rtt < 0) {
								index = -1;
							}
							if (index < 0) {
								continue;
							} else {
								// found the element, and it is smaller than
								// rank, i.e., it is closer to the target
								bitVector[index] = 1;
							}
						}

					} else {
						// all nodes fail, so there are no alive nodes
					}

					// iterate the bitVector
					// ===============================
					int start = -1;
					boolean first = false;
					int ZeroCounter = 0;
					for (int i = 0; i < candidates.size(); i++) {
						// first alive node
						if (!first && (bitVector[i] == 1)) {
							start = i;
							first = true;
						}
						if (bitVector[i] == 0) {
							ZeroCounter++;
						}
					}

					// all empty
					if (ZeroCounter == candidates.size() || start == -1) {
						// we do not have any node to report, we backtrack !

						backtracking(msg, lat, errorBuffer);

						return;

					} else {

						// send to new node
						AddressIF returnID = candidates.get(start);
						comm.sendRequestMessage(msg, returnID, new ObjCommRRCB<SubSetClosestResponseMsg>() {

							protected void cb(CBResult result, final SubSetClosestResponseMsg responseMsg,
									AddressIF node, Long ts) {
								switch (result.state) {
								case OK: {
									log.debug("Request Acked by new query node: ");
									// cdDone.call(result);
									break;
								}
								case ERROR:
								case TIMEOUT: {
									// cdDone.call(result);
									findNewClosestNxtNode(sortedL, msg, lat, errorBuffer);
									break;
								}
								}
								// ----------------------
							}
							// --------------------------

						});

					}

				}
			});

		} else {
			// we do not have next-hop node, so we finish the searching process,
			// I am the nearest node, and backtrack
			backtracking(msg, lat, errorBuffer);
		}
	}

	@Override
	public void receiveClosestQuery_MultipleTargets(Coordinate targetCoordinate, ClosestRequestMsg req,
			StringBuffer errorBuffer) {
		// TODO Auto-generated method stub

	}

	@Override
	public void queryKNearestNeighbors(ArrayList<AddressIF> targets, int expectedKNN, CB1<List<NodesPair>> cbDone) {
		// TODO Auto-generated method stub

	}

	@Override
	public void queryNearestNeighbor(AddressIF target, int NumOfKNN, CB1<NodesPair> cbDone) {
		// TODO Auto-generated method stub

	}

}
