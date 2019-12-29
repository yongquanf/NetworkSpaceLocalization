package edu.NUDT.pdl.Nina.KNN.multiObjKNN;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
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
import edu.NUDT.pdl.Nina.KNN.AbstractNNSearchManager;
import edu.NUDT.pdl.Nina.KNN.AnswerUpdateRequestMsg;
import edu.NUDT.pdl.Nina.KNN.CBcached;
import edu.NUDT.pdl.Nina.KNN.ClosestRequestMsg;
import edu.NUDT.pdl.Nina.KNN.ClosestResponseMsg;
import edu.NUDT.pdl.Nina.KNN.CompleteNNRequestMsg;
import edu.NUDT.pdl.Nina.KNN.CompleteNNResponseMsg;
import edu.NUDT.pdl.Nina.KNN.CompleteSubsetSearchRequestMsg;
import edu.NUDT.pdl.Nina.KNN.DatRequestMsg;
import edu.NUDT.pdl.Nina.KNN.DoubleComp;
import edu.NUDT.pdl.Nina.KNN.FarthestRequestMsg;
import edu.NUDT.pdl.Nina.KNN.FarthestResponseMsg;
import edu.NUDT.pdl.Nina.KNN.FinFarthestRequestMsg;
import edu.NUDT.pdl.Nina.KNN.FinFarthestResponseMsg;
import edu.NUDT.pdl.Nina.KNN.FinKNNRequestMsg;
import edu.NUDT.pdl.Nina.KNN.FinKNNResponseMsg;
import edu.NUDT.pdl.Nina.KNN.KNNGossipRequestMsg;
import edu.NUDT.pdl.Nina.KNN.KNNCollectRequestMsg;
import edu.NUDT.pdl.Nina.KNN.KNNManager;
import edu.NUDT.pdl.Nina.KNN.NodesPair;
import edu.NUDT.pdl.Nina.KNN.QueryKNNRequestMsg;
import edu.NUDT.pdl.Nina.KNN.RelativeCoordinate;
import edu.NUDT.pdl.Nina.KNN.RelativeCoordinateRequestMsg;
import edu.NUDT.pdl.Nina.KNN.RepeatedNNRequestMsg;
import edu.NUDT.pdl.Nina.KNN.RepeatedNNResponseMsg;
import edu.NUDT.pdl.Nina.KNN.RepeatedSubSetRequestMsg;
import edu.NUDT.pdl.Nina.KNN.SubSetClosestRequestMsg;
import edu.NUDT.pdl.Nina.KNN.SubSetManager;
import edu.NUDT.pdl.Nina.KNN.SubSetTargetLocatedRequestMsg;
import edu.NUDT.pdl.Nina.KNN.TargetLocatedRequestMsg;
import edu.NUDT.pdl.Nina.KNN.TargetLocatedResponseMsg;
import edu.NUDT.pdl.Nina.KNN.UpdateRequestMsg;
import edu.NUDT.pdl.Nina.KNN.WithdrawRequestMsg;
import edu.NUDT.pdl.Nina.KNN.WithdrawResponseMsg;
import edu.NUDT.pdl.Nina.KNN.AbstractNNSearchManager.CoordGossipHandler;
import edu.NUDT.pdl.Nina.KNN.AbstractNNSearchManager.DatHandler;
import edu.NUDT.pdl.Nina.KNN.AbstractNNSearchManager.FinKNNHandler;
import edu.NUDT.pdl.Nina.KNN.AbstractNNSearchManager.GossipHandler;
import edu.NUDT.pdl.Nina.KNN.AbstractNNSearchManager.RelativeCoordinateHandler;
import edu.NUDT.pdl.Nina.KNN.AbstractNNSearchManager.ResponseObjCommCB;
import edu.NUDT.pdl.Nina.KNN.AbstractNNSearchManager.UpdateReqHandler;
import edu.NUDT.pdl.Nina.KNN.AbstractNNSearchManager.updateTargetHandler;
import edu.NUDT.pdl.Nina.StableNC.lib.Coordinate;
import edu.NUDT.pdl.Nina.StableNC.lib.EWMAStatistic;
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

public class MultiTargetKNNManager extends AbstractNNSearchManager {

	// braches of the KNN search
	public static int MultipleRestart = Integer.parseInt(Config.getProperty("MultipleRestart", "1"));

	public static long MsxMSToRemove = 10 * 60 * 1000;

	// search direction
	public static int FarthestSearch = 1;
	public static int ClosestSearch = 0;

	// SubSetSearchManager subManager;

	// for KNN query CB
	// Map<AddressIF,CB1<Set<NodesPair>>> cachedKNNCB=new
	// HashMap<AddressIF,CB1<Set<NodesPair>>>(10);
	// for forbidden nodes in querying process
	// Map<AddressIF, Vector> forbiddenSet =new HashMap<AddressIF, Vector>(10);
	// for already registered NNs, target Original
	// for query cache

	// -----------------------------------------------------------------

	public MultiTargetKNNManager(ObjCommIF _GossipComm, ObjCommIF _comm, ObjCommIF _MeasureComm,
			PingManager pingManager, StableManager ncManager) {
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
					log.info("$:multiple objective knn Initialized");

					// non-ring node
					if (IsNonRingNode) {
						MClient.initNonRing();
					}

					comm.registerMessageCB(RelativeCoordinateRequestMsg.class, new RelativeCoordinateHandler());
					comm.registerMessageCB(FinFarthestRequestMsg.class, new FinKNNHandler());

					// on ring nodes
					if (!IsNonRingNode) {

						MClient.init();

						comm.registerMessageCB(RepeatedNNRequestMsg.class, new RepeatedNNReqHandler());
						comm.registerMessageCB(TargetLocatedRequestMsg.class, new TargetLockReqHandler());
						comm.registerMessageCB(CompleteNNRequestMsg.class, new CompleteNNReqHandler());
						comm.registerMessageCB(WithdrawRequestMsg.class, new WithdrawReqHandler());

						comm.registerMessageCB(ClosestRequestMsg.class, new ClosestReqHandler());

						// get the coordinate of neighbors
						gossipComm.registerMessageCB(CoordGossipRequestMsg.class, new CoordGossipHandler());
						gossipComm.registerMessageCB(KNNGossipRequestMsg.class, new GossipHandler());

						// use the measurement com
						measureComm.registerMessageCB(UpdateRequestMsg.class, new updateTargetHandler());
						// ==================================================
						// data request
						measureComm.registerMessageCB(DatRequestMsg.class, new DatHandler());

						measureComm.registerMessageCB(AnswerUpdateRequestMsg.class, new UpdateReqHandler());

						comm.registerMessageCB(FarthestRequestMsg.class, new FarthestReqHandler());

						comm.registerMessageCB(FinKNNRequestMsg.class, new FinKNNRequestMsgHandler());
						// =====================================
						/*
						 * //constrained KNN query
						 * comm.registerMessageCB(SubSetClosestRequestMsg.class,
						 * new SubSetClosestReqHandler());
						 * comm.registerMessageCB(CompleteSubsetSearchRequestMsg
						 * .class, new SubSetCompleteNNReqHandler() );
						 * comm.registerMessageCB(SubSetTargetLocatedRequestMsg.
						 * class, new SubSetTargetLockReqHandler() );
						 * comm.registerMessageCB(RepeatedSubSetRequestMsg.
						 * class, new SubSetRepeatedNNReqHandler() );
						 */
						/*
						 * comm.registerMessageCB(QueryKNNRequestMsg.class, new
						 * QueryKNNReqHandler());
						 * comm.registerMessageCB(FinKNNRequestMsg.class, new
						 * FinKNNRequestMsgHandler());
						 */

					}

					comm.registerMessageCB(KNNCollectRequestMsg.class, new CollectHandler());

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
	 * main entrance
	 */
	public void queryKNearestNeighbors(final ArrayList<AddressIF> targets, final int expectedKNN,
			final CB1<List<NodesPair>> cbDone) {

		// forbidden nodes
		Set<NodesPair> KNNsIndex = new HashSet<NodesPair>(2);
		Iterator<AddressIF> ier1 = targets.iterator();
		while (ier1.hasNext()) {
			AddressIF tmp = ier1.next();
			KNNsIndex.add(new NodesPair<AddressIF>(tmp, null, -2));
		}

		int branches = MultiTargetKNNManager.MultipleRestart;
		final Set<AddressIF> randNodes = new HashSet<AddressIF>(10);

		// rings
		MClient.instance.g_rings.getNodesWithMaximumNonEmptyRings(KNNsIndex, randNodes, branches);

		int realNodeNo = randNodes.size();

		queryKNearestNeighbors(randNodes, KNNsIndex, targets, expectedKNN, cbDone);
	}

	/**
	 * query the nearest nodes for targets
	 * 
	 * @param targets
	 * @param expectedKNN
	 * @param cbDone
	 */
	public void queryKNearestNeighbors(final Set<AddressIF> randNodes, Set<NodesPair> KNNsIndex,
			final ArrayList<AddressIF> targets, final int expectedKNN, final CB1<List<NodesPair>> cbDone) {

		int realNodeNo = randNodes.size();

		log.info("!!!!!!!!!!!!!!!!!!!!!!!\nTotal branches: " + realNodeNo + "\n!!!!!!!!!!!!!!!!!!!!!!!");

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
			Iterator<AddressIF> ier33 = randNodes.iterator();
			for (int index = 0; index < realNodeNo; index++) {

				final AddressIF dest = ier33.next();

				// version number
				final int version = Ninaloader.random.nextInt();
				// added K neighbors
				Set<NodesPair> NearestNeighborIndex = new HashSet<NodesPair>(10);
				// save the target into cache, to avoid target being select as
				// candidate KNN node
				// Note: NearestNeighborIndex-1=k means there are k nodes

				// NearestNeighborIndex.add(new NodesPair(target,target, -1));
				NearestNeighborIndex.addAll(KNNsIndex);

				// SubSetManager forbiddenFilter = new SubSetManager();
				// add the target
				// forbiddenFilter.insertPeers2Subset(target);

				ClosestRequestMsg msg22 = new ClosestRequestMsg(Ninaloader.me, Ninaloader.me, null, expectedKNN,
						NearestNeighborIndex, version, seed, null, SubSetManager.getEmptyFilter(),
						SubSetManager.getEmptyFilter());

				msg22.setTargetsForMultiObjKNN(targets);

				// first find the farthest node
				final FarthestRequestMsg FMsg = new FarthestRequestMsg(Ninaloader.me, null, Ninaloader.me, msg22);

				FMsg.targets = targets;
				// add current hop
				FMsg.HopRecords.add(Ninaloader.me);

				// send to a random neighbor in Set upNeighbors
				// select a node in the ring

				/*
				 * log.info("###############\n$: send out" + index +
				 * "th KNN query of " + Ninaloader.me + " to Neighbor " + dest +
				 * "\n###############");
				 */

				// the barrier
				barrierUpdate.fork();
				// barrierUpdate.fork();

				comm.sendRequestMessage(FMsg, dest, new ObjCommRRCB<FarthestResponseMsg>() {

					protected void cb(CBResult result, final FarthestResponseMsg responseMsg, AddressIF node, Long ts) {
						switch (result.state) {
						case OK: {
							log.info("K Nearest Request received by new query node: ");
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
					// log.info("\n@@:\n All KNN branches are reported after
					// "+time+" seconds\n\n");

					CBcached np = cachedCB.get(Long.valueOf(seed));
					int k = np.ExpectedNumber;

					if (np != null) {

						long curTimer = System.currentTimeMillis();
						long elapsedTime = curTimer - np.timeStamp;
						// ElapsedKNNSearchTime.add(elapsedTime);

						List<NodesPair> nps = new ArrayList<NodesPair>(5);
						// List<AddressIF> tmpKNN = new ArrayList<AddressIF>(5);

						int totalSendingMessage = np.totalSendingMessage;

						// List rtts=new ArrayList(5);
						Map<AddressIF, RemoteState<AddressIF>> maps = new HashMap<AddressIF, RemoteState<AddressIF>>(5);

						Set<NodesPair> nodePairs = np.nps;
						// remove the cached nearestNodes

						// iterate, we use the latency as the hash key
						if (nodePairs != null) {
							Iterator<NodesPair> ierNp = nodePairs.iterator();
							while (ierNp.hasNext()) {
								NodesPair cachedNP = ierNp.next();

								// skip myself
								if (targets.contains(cachedNP.startNode)) {
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
								if (targets.contains(tmpNode)) {
									continue;
								}

								// add the node
								nps.add(new NodesPair(tmpNode, null, item.doubleValue(), elapsedTime,
										maps.get(tmpNode).elapsedHops, totalSendingMessage));

								counter++;
							}

							//
							log.info("KNN Number: " + nps.size() + " required: " + k);

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
							log.info("No RTT measurements!");
							randNodes.clear();
							cbDone.call(CBResult.ERROR(), null);
						}
					} else {
						log.info("NO cachedCB!");
						randNodes.clear();
						cbDone.call(CBResult.ERROR(), null);
					}

				}
			});

		} // index
		else {
			randNodes.clear();
			log.warn("can not du query!");
			cbDone.call(CBResult.ERROR(), null);
		}
	}

	/**
	 * extract the targets
	 * 
	 * @param NearestNeighborIndex
	 * @return
	 */
	ArrayList<AddressIF> extractTargets(Set<NodesPair> NearestNeighborIndex, AddressIF forbiddenNode) {
		ArrayList<AddressIF> cachedTargets = new ArrayList<AddressIF>(2);
		Iterator<NodesPair> ier = NearestNeighborIndex.iterator();
		while (ier.hasNext()) {
			AddressIF tmp = (AddressIF) (ier.next().startNode);
			if (forbiddenNode == null) {
				cachedTargets.add(tmp);
				continue;
			}
			if (tmp.equals(forbiddenNode)) {
				continue;
			} else {
				cachedTargets.add(tmp);
			}
		}
		return cachedTargets;
	}

	/**
	 * extract the rtt records
	 * 
	 * @param NearestNeighborIndex
	 * @return
	 */
	ArrayList extractRTT(Set<NodesPair> NearestNeighborIndex) {
		ArrayList cachedRTT = new ArrayList(2);
		Iterator<NodesPair> ier = NearestNeighborIndex.iterator();
		while (ier.hasNext()) {
			NodesPair tmp = ier.next();
			if (tmp.rtt < 0) {
				continue;
			}
			cachedRTT.add(tmp.rtt);
		}
		return cachedRTT;
	}

	/**
	 * TODO:
	 */
	void removeLastHopNodes() {

	}

	/**
	 * forward request towards multiple targets
	 * 
	 * @param fmsg
	 * @param errorBuffer
	 */
	public void receiveFathestReq_MultipleTargets(final FarthestRequestMsg fmsg, final StringBuffer errorBuffer) {

		final long sendStamp = System.nanoTime();

		// ask me to ping them
		ArrayList<AddressIF> cachedTargets = fmsg.CMsg.targets;
		log.debug("Received from, " + fmsg.from.toString() + ", targets: " + cachedTargets.size() + ", FMsg's targets: "
				+ fmsg.targets);

		collectRTTs(cachedTargets, new CB2<Set<NodesPair>, String>() {

			protected void cb(CBResult result, Set<NodesPair> records, String arg2) {
				// TODO Auto-generated method stub
				switch (result.state) {
				case OK: {

					// all alive targets
					// final ArrayList<AddressIF> aliveTargets =
					// extractTargets(records,null);
					final ArrayList<AddressIF> aliveTargets = fmsg.targets;
					ArrayList RTTs = extractRTT(records);
					double totalRTT = 0;
					Iterator ier = RTTs.iterator();
					while (ier.hasNext()) {
						double tmp = (Double) ier.next();
						totalRTT += tmp;
					}

					double rttValue = -1;
					if (RTTs.isEmpty()) {
						log.warn("no target measurement is received!");
					} else {
						rttValue = totalRTT / RTTs.size();
					}

					// release
					RTTs.clear();

					// baseline rtt for me
					final double baseRTT = rttValue;
					// TODO: we do not receive measurements
					if (baseRTT < 0) {
						log.warn("farthest measurement is forced to be stopped!");
						return;
					}

					log.info("averaged RTT: " + baseRTT);

					Vector<AddressIF> ringMembers = new Vector<AddressIF>(10);

					if (!useNormalRingRange) {

						// find maximum RTT, (rtt, maxRTT)
						// 2010-3-7
						double maxRTT = 2 * baseRTT;
						// not the target
						MClient.instance.g_rings.fillVector(fmsg.CMsg.getNearestNeighborIndex(), fmsg.target, baseRTT,
								maxRTT, betaRatioFarthest, ringMembers, AbstractNNSearchManager.offsetLatency);

						/*
						 * if(fmsg.CMsg!=null&&fmsg.CMsg.getNearestNeighborIndex
						 * ()!=null&&!fmsg.CMsg.getNearestNeighborIndex().
						 * isEmpty()){ instance.g_rings.fillVector(fmsg.CMsg.
						 * getNearestNeighborIndex(),fmsg.target, baseRTT,
						 * maxRTT, betaRatio_farthest,
						 * ringMembers,AbstractNNSearchManager.offsetLatency);
						 * }else{ instance.g_rings.fillVector(fmsg.target,
						 * baseRTT, maxRTT, betaRatio_farthest,
						 * ringMembers,AbstractNNSearchManager.offsetLatency); }
						 */

						// add perturbation
						if (useRandomSeedsInSearching) {
							Set<AddressIF> randomSeeds = new HashSet<AddressIF>(defaultNodesForRandomContact);
							MClient.instance.g_rings.getNodesWithMaximumNonEmptyRings(randomSeeds,
									defaultNodesForRandomContact);
							ringMembers.addAll(randomSeeds);
						}

					} else {

						// TODO: select for multi-knn
						/*
						 * //let ring members to ping the target
						 * //curNode.cRing.g_rings.fillVector(me, RTT, RTT,
						 * betaRatio, ringMembers, offset); List<NodesPair>
						 * targetRelCoord=fmsg.targetCoordinates.coords;
						 * 
						 * log.info("Farthest Search: dim: "
						 * +targetRelCoord.size());
						 * 
						 * //3-10 // //double MAXRTT=1000;
						 * instance.g_rings.fillVectorBasedOnKFarthestNode(
						 * _choiceOfNextHop, Ninaloader.me,
						 * targetRelCoord,baseRTT, ringMembers,
						 * AbstractNNSearchManager.offsetLatency);
						 */

					}

					// also remove the backtracking node

					Iterator<AddressIF> ierRing = ringMembers.iterator();
					while (ierRing.hasNext()) {
						AddressIF addrRing = ierRing.next();
						if (fmsg.HopRecords.contains(addrRing)) {
							log.info("remove repeated hops!" + addrRing.toString());
							ierRing.remove();
						}

					}

					log.info("$: total nodes: " + ringMembers.size());

					final long timer1 = System.currentTimeMillis();
					// returned ID is null, or is the target itself;
					if (ringMembers == null || ((ringMembers != null) && (ringMembers.size() == 0))) {
						log.info("returned ring is empty, start multiobjective knn search");
						// no nearer nodes, so return me
						// System.out.println("Ring Empty! " + Ninaloader.me
						// + " is the farthest to: " + fmsg.target
						// + " @: " + rtt);

						/*
						 * System.out.println(
						 * "\n==================\nTotal hops: " +
						 * fmsg.HopRecords.size() + "\n==================");
						 */

						// myself
						final StringBuffer errorBuffer = new StringBuffer();
						if (fmsg.CMsg != null) {
							fmsg.CMsg.setFrom(Ninaloader.me);
							fmsg.CMsg.setRoot(Ninaloader.me);
							// hop records
							// fmsg.CMsg.getHopRecords().add(Ninaloader.me);
							// null
							fmsg.CMsg.targetCoordinates = fmsg.targetCoordinates;

							/*
							 * System.out .println(
							 * "\n##################\n$: NULL Bloom filter in Farthest KNN search !\n##################\n"
							 * );
							 */
							receiveClosestQuery_MultipleTargets(fmsg.targetCoordinates, fmsg.CMsg, errorBuffer);

						} else {

							fmsg.SCMsg.setFrom(Ninaloader.me);
							fmsg.SCMsg.setRoot(Ninaloader.me);
							// hop records
							// fmsg.SCMsg.getHopRecords().add(Ninaloader.me);
							fmsg.SCMsg.targetCoordinates = fmsg.targetCoordinates;

							/*
							 * System.out .println(
							 * "\n##################\n$: NULL Bloom filter in Farthest KNN search !\n##################\n"
							 * );
							 * receiveClosestQuery(fmsg.targetCoordinates,fmsg.
							 * SCMsg, errorBuffer);
							 */
						}

					} else {

						log.info("ringMembers for farthest search!");
						// printHops("Ring: ",ringMembers);
						// ping all alive targets

						if (fmsg.CMsg != null) {
							// add the message
							fmsg.CMsg.increaseMessage(ringMembers.size());
						}

						TargetProbes(ringMembers, aliveTargets, new CB1<String>() {
							protected void cb(CBResult ncResult, String errorString) {
								switch (ncResult.state) {
								case OK: {
									// System.out.println("$: Target Probes are
									// issued");
								}
								case ERROR:
								case TIMEOUT: {
									break;
								}
								}
							}
						}, new CB2<Set<NodesPair>, String>() {
							protected void cb(CBResult ncResult, Set<NodesPair> nps11, String errorString) {
								switch (ncResult.state) {
								case OK: {

									log.info("returned results: " + nps11.size());

									AddressIF returnID = Ninaloader.me;

									// compute the averaged RTT for each node
									Hashtable<AddressIF, ArrayList<Double>> cachedRTTs = new Hashtable<AddressIF, ArrayList<Double>>(
											2);

									Iterator<NodesPair> ier33 = nps11.iterator();
									// TODO: parse all nodes's averaged
									// distances towards the target
									while (ier33.hasNext()) {
										NodesPair tmp = ier33.next();
										// print
										log.info(tmp.SimtoString());

										if (tmp.rtt < 0) {
											continue;
										} else {
											AddressIF addr = (AddressIF) tmp.startNode;
											if (!cachedRTTs.containsKey(addr)) {
												cachedRTTs.put(addr, new ArrayList());
												cachedRTTs.get(addr).add(tmp.rtt);
											} else {
												cachedRTTs.get(addr).add(tmp.rtt);
											}
										}
										// =================================
									}
									// save the averaged RTT
									Set<NodesPair> nps = new HashSet<NodesPair>(2);
									Iterator<AddressIF> ier22 = cachedRTTs.keySet().iterator();

									double rttM = 0.;
									int count = 0;
									while (ier22.hasNext()) {

										double avgRTT = 0.;
										AddressIF tmp = ier22.next();
										ArrayList listRTT = cachedRTTs.get(tmp);
										avgRTT = 0.;
										Iterator ier44 = listRTT.iterator();
										count = 0;
										while (ier44.hasNext()) {
											rttM = ((Double) ier44.next()).doubleValue();
											if (rttM < 0) {
												continue;
											}
											avgRTT += rttM;
											count++;
										}
										/**
										 * has more than one measurement
										 */
										if (count > 0) {
											avgRTT /= count;
											log.main("averaged RTT: " + tmp.toString() + ", " + avgRTT);
											nps.add(new NodesPair(tmp, null, avgRTT));
										}
									}

									cachedRTTs.clear();
									// ==============================
									if (nps != null) {

										if (!aliveTargets.contains(Ninaloader.me)) {
											nps.add(new NodesPair(Ninaloader.me, null, baseRTT));
										}

										double total_value = 0.;
										count = 0;
										double maximum = Double.MIN_VALUE;
										Iterator ier = nps.iterator();
										// bigger !
										while (ier.hasNext()) {
											NodesPair tmp = (NodesPair) ier.next();
											if (aliveTargets.contains(tmp.startNode)) {
												continue;
											}
											// skip target
											if ((tmp.rtt + AbstractNNSearchManager.offsetLatency) > maximum) {
												returnID = (AddressIF) tmp.startNode;
												maximum = tmp.rtt + AbstractNNSearchManager.offsetLatency;
											}
											count++;
											total_value += (tmp.rtt + AbstractNNSearchManager.offsetLatency);
										}
										double avgRTT = total_value / count;
										double RTT_Threshold = (1 + betaRatioFarthest) * avgRTT;

										// base rtt
										if (RTT_Threshold < baseRTT) {
											RTT_Threshold = (1 + betaRatioFarthest) * baseRTT;
										}

										// stop the search
										if (maximum < RTT_Threshold) {

											log.info("maximum<RTT_Threshold");
											// not me , send stop message
											if (!returnID.equals(Ninaloader.me)) {
												final FinFarthestRequestMsg msg22 = FinFarthestRequestMsg
														.makeCopy(fmsg);
												msg22.from = Ninaloader.me;
												// relative coordinate
												msg22.targetCoordinates = fmsg.targetCoordinates;

												msg22.setTargetsForMultiObjKNN(fmsg.targets);

												log.info("stop farthest search, and call: " + returnID.toString()
														+ " to start multiobjective KNN");

												comm.sendRequestMessage(msg22, returnID,
														new ObjCommRRCB<FinFarthestResponseMsg>() {
													protected void cb(CBResult result,
															final FinFarthestResponseMsg responseMsg, AddressIF node,
															Long ts) {
														switch (result.state) {
														case OK: {
															log.info("Request Acked by new query node: ");
															// cdDone.call(result);
															break;
														}
														case ERROR:
														case TIMEOUT: {
															// cdDone.call(result);
															break;
														}
														}
														msg22.clear();
														// ----------------------
													}
													// --------------------------
												});
												return;
											} else {
												// its me
												log.info("Congratulate! start closest node search!");
												// start search!
												if (fmsg.CMsg != null) {
													fmsg.CMsg.setFrom(Ninaloader.me);
													fmsg.CMsg.setRoot(Ninaloader.me);
													// hop records
													// fmsg.CMsg.getHopRecords().add(Ninaloader.me);
													fmsg.CMsg.targetCoordinates = fmsg.targetCoordinates;

													// the backtrack node is
													// null

													// ========================
													// filter based

													receiveClosestQuery_MultipleTargets(fmsg.targetCoordinates,
															fmsg.CMsg, errorBuffer);

												}
												return;
											}

										}
									} else {
										log.warn(" no measurements are returned!");

									}
									log.info("returned ID is: " + returnID.toString());
									// -----------------------------------------
									if (returnID.equals(Ninaloader.me)) {
										// OK send originFrom a
										// message
										log.info("Farthest Search: " + Ninaloader.me + " is the farthest");
										/*
										 * log.info(
										 * "\n==================\nTotal hops: "
										 * + fmsg.HopRecords .size() +
										 * "\n==================");
										 */
										// Set<NodesPair>
										// ReturnedID=new
										// HashSet<NodesPair>();
										// ReturnedID.add(new
										// NodesPair(fmsg.target,
										// returnID,rtt));
										// closest query
										final StringBuffer errorBuffer = new StringBuffer();

										// change the from field
										// of the closest query
										// message, since I am
										// the real original
										// node
										if (fmsg.CMsg != null) {
											fmsg.CMsg.setFrom(Ninaloader.me);
											fmsg.CMsg.setRoot(Ninaloader.me);
											// hop records
											// fmsg.CMsg.getHopRecords().add(Ninaloader.me);
											fmsg.CMsg.targetCoordinates = fmsg.targetCoordinates;

											// the backtrack node is null
											if (fmsg.CMsg.getForbiddenFilter() == null) {
												System.out.println(
														"\n##################\n$: NULL Bloom filter in Farthest KNN search !\n##################\n");
												receiveClosestQuery_MultipleTargets(fmsg.targetCoordinates, fmsg.CMsg,
														errorBuffer);
											} else {
												// ========================
												// filter based
												log.info("Congratulate! start closest node search!");
												receiveClosestQuery_MultipleTargets(fmsg.targetCoordinates, fmsg.CMsg,
														errorBuffer);
											}

										} else {

											log.warn("fmsg.CMsg is NULL, this should not happen!");
											fmsg.SCMsg.setFrom(Ninaloader.me);
											fmsg.SCMsg.setRoot(Ninaloader.me);
											// hop records
											// fmsg.SCMsg.getHopRecords().add(Ninaloader.me);
											fmsg.SCMsg.targetCoordinates = fmsg.targetCoordinates;

											// the backtrack node is null
											if (fmsg.SCMsg.getForbiddenFilter() == null) {
												System.out.println(
														"\n##################\n$: NULL Bloom filter in Farthest KNN search !\n##################\n");
												// receiveClosestQuery_MultipleTargets(fmsg.targetCoordinates,fmsg.SCMsg,
												// errorBuffer);
											} else {
												// ========================
												// filter based
												/*
												 * receiveClosestQuery_MultipleTargets
												 * (fmsg.targetCoordinates,fmsg.
												 * SCMsg, errorBuffer);
												 */
											}
										}

										return;
									}

									else {

										//
										// directly use current Farthest search
										// message
										log.info("farthest search, next hop is: " + returnID.toString());
										// FarthestRequestMsg msg =null;
										// if(fmsg.CMsg!=null){
										final FarthestRequestMsg msg = new FarthestRequestMsg(Ninaloader.me,
												fmsg.target, fmsg.OriginFrom, fmsg.CMsg);

										// cloest neighbor query
										msg.CMsg.increaseMessage(1);
										// }

										/*
										 * if(fmsg.SCMsg!=null){ msg = new
										 * FarthestRequestMsg( Ninaloader.me,
										 * fmsg.target, fmsg.OriginFrom,
										 * fmsg.SCMsg); }
										 */

										msg.targetCoordinates = fmsg.targetCoordinates;

										msg.addCurrentHop(fmsg.HopRecords, Ninaloader.me);

										// info
										// printHops("start multiobjective
										// knn",msg.HopRecords);

										// targets
										msg.setTargetsForMultiObjKNN(fmsg.CMsg.targets);

										comm.sendRequestMessage(msg, returnID, new ObjCommRRCB<FarthestResponseMsg>() {
											protected void cb(CBResult result, final FarthestResponseMsg responseMsg,
													AddressIF node, Long ts) {
												switch (result.state) {
												case OK: {
													log.info("Request Acked by new query node: ");
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

										if (fmsg != null) {
											fmsg.clear();
										}
										if (msg != null) {
											msg.clear();
										}
									}

									break;
								}
								case ERROR:
								case TIMEOUT: {
									log.warn("Farthest Search: Target probes to " + fmsg.target + " FAILED!"); // the
									// returned
									// map
									// is
									// null
									break;
								}
								}

							}//
						}

						);

					} // end of target probe
						// ringMembers.clear();

					break;
				}
				case TIMEOUT:
				case ERROR: {

					String error = "RTT request to " + fmsg.target.toString(false) + " failed:" + result.toString();
					log.warn(error);
					if (errorBuffer.length() != 0) {
						errorBuffer.append(",");
					}

					errorBuffer.append(error);

					if (fmsg.CMsg != null) {
						fmsg.CMsg.setFrom(Ninaloader.me);
						fmsg.CMsg.setRoot(Ninaloader.me);
						// hop records
						// fmsg.CMsg.getHopRecords().add(Ninaloader.me);
						fmsg.CMsg.targetCoordinates = fmsg.targetCoordinates;

						// ========================
						// filter based
						log.info("Congratulate! start closest node search!");
						receiveClosestQuery_MultipleTargets(fmsg.targetCoordinates, fmsg.CMsg, errorBuffer);

					}
					break;
				}
				}
			}

		});

	}

	/**
	 * print out the hops
	 * 
	 * @param Hops
	 */
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
	 * I may not be the originFrom, but I have to return now
	 * 
	 * @param msg3
	 */
	public void returnKNN2Origin_MultipleTargets(final CompleteNNRequestMsg msg3) {

		//

		boolean stop = false;
		// end the knn search process

		float thresholdOfKNN = 0.8f;
		log.info("found " + msg3.getNearestNeighborIndex().size() + ", expected " + msg3.K);
		// TODO: avoid the loop process ??
		if (msg3.K > thresholdOfKNN * msg3.getNearestNeighborIndex().size()) {
			// restart the search process
			// forbidden nodes
			Set<NodesPair> KNNsIndex = msg3.getNearestNeighborIndex();

			int branches = MultiTargetKNNManager.MultipleRestart;
			final List<AddressIF> randNodes = new ArrayList<AddressIF>(10);

			// rings
			MClient.instance.g_rings.getNodesWithMaximumNonEmptyRings(KNNsIndex, randNodes, branches);

			int realNodeNo = randNodes.size();

			if (realNodeNo > 0) {

				Collections.shuffle(randNodes);

				AddressIF dest = randNodes.get(0);

				ClosestRequestMsg _CMsg = CompleteNNRequestMsg.makeCopy(msg3);

				// first find the farthest node
				FarthestRequestMsg FMsg = new FarthestRequestMsg(Ninaloader.me, msg3.target, Ninaloader.me, _CMsg);

				FMsg.setTargetsForMultiObjKNN(msg3.targets);
				// add current hop
				FMsg.HopRecords.add(Ninaloader.me);

				// send to a random neighbor in Set upNeighbors
				// select a node in the ring

				// barrierUpdate.fork();

				comm.sendRequestMessage(FMsg, dest, new ObjCommRRCB<FarthestResponseMsg>() {

					protected void cb(CBResult result, final FarthestResponseMsg responseMsg, AddressIF node, Long ts) {
						switch (result.state) {
						case OK: {
							log.info("K Nearest Request received by new query node: ");
							// cdDone.call(result);
							// save the callbacks
							// add into a list

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
					}
					// --------------------------
				});

				if (FMsg != null) {
					FMsg = null;

				}
				log.warn("we repeat the knn search process to find more nearest nodes");
				stop = true;
			} else {
				log.warn("we have no repeated neighbors to continue the search process");
				normalEndKNNprocess(msg3);
				return;
			}
		}

		if (stop) {
			// we can stop the answering process, since we have to reset the
			// search process
			return;
		} else {
			// end it
			normalEndKNNprocess(msg3);
		}

	}

	void normalEndKNNprocess(CompleteNNRequestMsg msg3) {

		// I should not work, since I have been a KNNer
		if (!Ninaloader.me.equals(msg3.OriginFrom)) {

			log.info("!!!!!!!!!!!!!!!!!!!!!!\nComplete @ TargetLockReqHandler\n!!!!!!!!!!!!!!!!!!!!!!");
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
					// msg3.clear();
				}
			});

			if (msg3 != null) {
				msg3 = null;
			}

		} else {
			// yes I am the originFrom
			postProcessKNN_MultipleTargets(msg3);
		}

	}

	void postProcessKNN1(CompleteNNRequestMsg arg1) {

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

	}

	/**
	 * answer KNN request form lastHop
	 * 
	 * @param ClosestRequestMsg
	 * @param errorBuffer
	 */

	public void receiveClosestQuery_MultipleTargets(final Coordinate targetCoordinate, final ClosestRequestMsg req,
			final StringBuffer errorBuffer) {

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

			// the targets
			final ArrayList<AddressIF> realtargets = req.targets;

			// log.info("$: message is from: "+LastHopNode );
			// ===========================================================

			// repeated KNN request, since I have been included
			// TODO: need to be changed to a list of vectors

			SubSetManager subset = new SubSetManager(req.getForbiddenFilter());
			// found, repeated KNN
			if (subset.checkNode(Ninaloader.me) && !realtargets.contains(Ninaloader.me)) {

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

					final RepeatedNNRequestMsg rmsg = new RepeatedNNRequestMsg(req.getOriginFrom(), Ninaloader.me,
							req.getTarget(), req.getK(), req.getNearestNeighborIndex(), req.getVersion(), req.getSeed(),
							req.getRoot(), req.getFilter(), req.getForbiddenFilter());

					// cache the relative coordinate
					rmsg.targetCoordinates = targetCoordinate;

					// add the targets
					rmsg.setTargetsForMultiObjKNN(req.targets);

					// record the message cost
					rmsg.totalSendingMessages = req.totalSendingMessages;
					rmsg.increaseMessage(1);

					int len = req.getHopRecords().size();
					// remove last element in hops
					if (len > 0) {
						req.getHopRecords().remove(len - 1);
					}
					rmsg.HopRecords.addAll(req.getHopRecords());
					// req.clear();
					// ====================================================
					comm.sendRequestMessage(rmsg, req.getFrom(), new ObjCommRRCB<RepeatedNNResponseMsg>() {

						protected void cb(CBResult result, final RepeatedNNResponseMsg responseMsg, AddressIF node,
								Long ts) {
							switch (result.state) {
							case OK: {
								log.info("repeated request acked ");
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

					if (rmsg != null) {
						rmsg.clear();
					}

					return;
				} else {
					// yes, myself, backtracking
					CompleteNNRequestMsg msg3 = CompleteNNRequestMsg.makeCopy(req);
					msg3.from = Ninaloader.me;
					// release req;
					// req=null;
					req.clear();
					returnKNN2Origin_MultipleTargets(msg3);

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
			if (req != null && req.targets != null && (req.getNearestNeighborIndex() != null)
					&& ((req.getNearestNeighborIndex().size() - req.targets.size()) >= req.getK())) {
				log.info(" $: completed! ");
				CompleteNNRequestMsg msg3 = CompleteNNRequestMsg.makeCopy(req);
				msg3.from = Ninaloader.me;
				// req.clear();
				returnKNN2Origin_MultipleTargets(msg3);
				return;
			}

			// else continue

			// TODO: if last hop fails

			// log.info("$: last hop: " + LastHopNode+", me:
			// "+Ninaloader.me.toString());
			// to confirm whether I am a new nearest node for target

			// send the target a gossip msg
			// LOWTODO could bias which nodes are sent based on his coord
			final long sendStamp = System.nanoTime();

			final ArrayList<AddressIF> cachedTargets = req.targets;

			// this.printHops("targets for KNN: ",cachedTargets);

			collectRTTs(cachedTargets, new CB2<Set<NodesPair>, String>() {

				@Override
				protected void cb(CBResult result, Set<NodesPair> records, String arg2) {
					// TODO Auto-generated method stub

					switch (result.state) {
					case OK: {

						// all alive targets
						// final ArrayList<AddressIF> aliveTargets =
						// extractTargets(records,null);

						ArrayList RTTs = extractRTT(records);
						double totalRTT = 0;
						Iterator ier = RTTs.iterator();
						while (ier.hasNext()) {
							double tmp = (Double) ier.next();
							totalRTT += tmp;
						}
						// recieve no measurements, we need to go back
						if (RTTs.isEmpty()) {

							if (req.getFrom() != null && !req.getFrom().equals(Ninaloader.me)) {
								// not in

								// in, but not saved

								final RepeatedNNRequestMsg rmsg = new RepeatedNNRequestMsg(req.getOriginFrom(),
										Ninaloader.me, req.getTarget(), req.getK(), req.getNearestNeighborIndex(),
										req.getVersion(), req.getSeed(), req.getRoot(), req.getFilter(),
										req.getForbiddenFilter());

								// I can not be contacted in near future
								rmsg.NearestNeighborIndex.add(new NodesPair(Ninaloader.me, null, -1));

								// cache the relative coordinate
								rmsg.targetCoordinates = targetCoordinate;

								// add the targets
								rmsg.setTargetsForMultiObjKNN(req.targets);

								int len = req.getHopRecords().size();
								// remove last element in hops
								if (len > 0) {
									req.getHopRecords().remove(len - 1);
								}
								rmsg.HopRecords.addAll(req.getHopRecords());
								// req.clear();
								// ====================================================
								comm.sendRequestMessage(rmsg, req.getFrom(), new ObjCommRRCB<RepeatedNNResponseMsg>() {

									protected void cb(CBResult result, final RepeatedNNResponseMsg responseMsg,
											AddressIF node, Long ts) {
										switch (result.state) {
										case OK: {
											log.info("repeated request acked ");
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

								if (rmsg != null) {
									rmsg.clear();
								}

								return;
							} else {
								// yes, myself, backtracking
								CompleteNNRequestMsg msg3 = CompleteNNRequestMsg.makeCopy(req);
								msg3.from = Ninaloader.me;
								// req.clear();
								returnKNN2Origin_MultipleTargets(msg3);

								/*
								 * double []lat=new double[1];
								 * ncManager.isCached(req.target, lat, -1);
								 * backtracking(req,lat[0], errorBuffer);
								 */
								return;

							}

						}
						// baseline rtt for me
						final double baseRTT = totalRTT / RTTs.size();

						RTTs.clear();

						// request nodes to probe to target nodes
						final Vector<AddressIF> ringMembers = new Vector<AddressIF>(10);

						log.info("@@@@@@@@@@@@@@@@@@@@@@@@\n Node: " + Ninaloader.me + " is: " + (baseRTT)
								+ " from Node: " + req.getTarget() + "\n@@@@@@@@@@@@@@@@@@@@@@@@");

						if (!AbstractNNSearchManager.useNormalRingRange) {

							// find
							MClient.instance.g_rings.fillVector(req.getNearestNeighborIndex(), null, baseRTT, baseRTT,
									betaRatio, ringMembers, AbstractNNSearchManager.offsetLatency);

							// add perturbation
							Set<AddressIF> randomSeeds = new HashSet<AddressIF>();
							// randomSeeds.addAll(ringMembers);
							if (useRandomSeedsInSearching) {
								MClient.instance.g_rings.getNodesWithMaximumNonEmptyRings(req.getNearestNeighborIndex(),
										randomSeeds, defaultNodesForRandomContact);

								ringMembers.addAll(randomSeeds);
								randomSeeds.clear();
							}

						} else {

							/*
							 * List<NodesPair>
							 * targetRelCoord=targetCoordinate.coords;
							 * 
							 * //let ring members to ping the target
							 * instance.g_rings.fillVectorBasedOnKClosestNode(
							 * Ninaloader.me,req.getNearestNeighborIndex()
							 * ,targetRelCoord,baseRTT, ringMembers,
							 * AbstractNNSearchManager.offsetLatency);
							 */

							// TODO: select for multi-knn
						}

						/**
						 * remove me
						 */
						if (ringMembers.contains(Ninaloader.me)) {
							log.warn("ring members contain myself!");
							ringMembers.remove(Ninaloader.me);
						}
						// found nearest nodes
						SubSetManager subset = new SubSetManager(req.getForbiddenFilter());
						// postprocess the ring members
						Iterator<AddressIF> ierRing11 = ringMembers.iterator();
						while (ierRing11.hasNext()) {
							AddressIF tmpNode = ierRing11.next();
							if (subset.checkNode(tmpNode)) {
								// found forbidden nodes
								ierRing11.remove();
							}
						}

						// also remove the backtracking node

						Iterator<AddressIF> ierRing = ringMembers.iterator();
						while (ierRing.hasNext()) {
							AddressIF addrRing = ierRing.next();
							if (req.getHopRecords().contains(addrRing)) {
								log.warn("Remove forbidden backtracking node: " + addrRing.toString());
								ierRing.remove();
							}

						}

						// ---------------------------------

						log.info("receiveClosestQuery_MultipleTargets: me: " + Ninaloader.me.toString() + ", last: "
								+ LastHopNode + ", baseRTT: " + baseRTT + ", ringMembers: " + ringMembers.size());
						// printHops("ringMembers: ",ringMembers);
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

							log.warn("no ring members are found!");

							if (LastHopNode == null || LastHopNode.equals(Ninaloader.me)) {
								log.info("Finish! ");

								req.getNearestNeighborIndex().add(new NodesPair(Ninaloader.me, null, baseRTT,
										LastHopNode, req.operationsForOneNN));
								subset = new SubSetManager(req.getForbiddenFilter());
								subset.insertPeers2Subset(Ninaloader.me);

								CompleteNNRequestMsg msg3 = CompleteNNRequestMsg.makeCopy(req);
								msg3.from = Ninaloader.me;
								// CompleteNNRequestMsg
								// msg3=CompleteNNRequestMsg.makeCopy(req);
								// req.clear();
								returnKNN2Origin_MultipleTargets(msg3);
								return;

							} else {

								log.info(" find a new last hop!!!");

								backtracking(req, baseRTT, errorBuffer);
								return;
							}

						} else {

							// final double refRTT=lat;
							// TODO: ask a set of nodes to ping a set of
							// nodes

							req.increaseMessage(ringMembers.size());

							TargetProbes(ringMembers, cachedTargets, new CB1<String>() {

								protected void cb(CBResult ncResult, String errorString) {
									switch (ncResult.state) {
									case OK: {
										// log.info("$: Target Probes are issued
										// for KNN");
									}
									case ERROR:
									case TIMEOUT: {
										break;
									}
									}
								}
							}, new CB2<Set<NodesPair>, String>() {

								protected void cb(CBResult ncResult, Set<NodesPair> nps11, String errorString) {

									switch (ncResult.state) {
									case OK: {

										// AddressIF returnID = Ninaloader.me;

										// compute the averaged RTT for each
										// node
										Hashtable<AddressIF, ArrayList> cachedRTTs = new Hashtable<AddressIF, ArrayList>(
												2);

										Iterator<NodesPair> ier33 = nps11.iterator();
										// TODO: parse all nodes's averaged
										// distances towards the target
										while (ier33.hasNext()) {
											NodesPair tmp = ier33.next();
											if (tmp.rtt < 0) {
												continue;
											} else {
												AddressIF addr = (AddressIF) tmp.startNode;
												if (!cachedRTTs.containsKey(addr)) {
													cachedRTTs.put(addr, new ArrayList(2));
													cachedRTTs.get(addr).add(tmp.rtt);
												} else {
													cachedRTTs.get(addr).add(tmp.rtt);
												}
											}
											// =================================
										}
										// save the averaged RTT
										// StringBuffer buf=new StringBuffer();
										final Set<NodesPair> nps = new HashSet<NodesPair>(2);
										Iterator<AddressIF> ier22 = cachedRTTs.keySet().iterator();
										int count = 0;
										while (ier22.hasNext()) {
											double avgRTT = 0.;
											AddressIF tmp = ier22.next();
											ArrayList listRTT = cachedRTTs.get(tmp);
											avgRTT = 0.;
											count = 0;
											Iterator ier44 = listRTT.iterator();
											while (ier44.hasNext()) {

												double rttM = ((Double) ier44.next()).doubleValue();
												if (rttM < 0) {
													continue;
												}
												avgRTT += rttM;
												count++;
											}
											if (count > 0) {

												avgRTT /= count;
												nps.add(new NodesPair(tmp, null, avgRTT));
												// buf.append(avgRTT+",
												// "+tmp.toString()+"; ");
											}
										}

										// buf.append("\n");

										// ringMembers.clear();
										// final Set<NodesPair> nps1=new
										// HashSet<NodesPair>(1);
										// nps1.addAll(nps2);

										ringMembers.clear();
										cachedRTTs.clear();
										// +++++++++++++++++++++++++++++++++++++
										// use the rule to select next hop nodes
										// AddressIF target=req.target;

										Set<NodesPair> closestNodes = new HashSet<NodesPair>(1);
										SubSetManager subset = new SubSetManager(req.getForbiddenFilter());
										// select based rules
										AddressIF returnID = ClosestNodeRule(baseRTT, null, nps, closestNodes, subset,
												LastHopNode);
												// log.info("baseRTT:
												// "+baseRTT+", distances of
												// ring members are as follows:
												// \n"+buf.toString());

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
													tmp.ElapsedSearchHops = req.operationsForOneNN;
													subset.insertPeers2Subset(from);
													req.getNearestNeighborIndex().add(tmp);
												}

											}
											closestNodes.clear();
										}
										req.operationsForOneNN = 0;

										// +++++++++++++++++++++++++++++++++++++
										// returned node is null, or returned
										// node is last hop
										if (returnID == null
												|| (LastHopNode != null) && (returnID.equals(LastHopNode))) {
											// found nearest, we backtrack
											// I am the lastHop
											// remove 9-24
											// remove last hop
											log.info("backtrack, due to returnID is null or is the last hop node!");
											backtracking(req, baseRTT, errorBuffer);

										} else {
											// returnID is not
											// Ninaloader.me
											//
											log.info("send to next hop, for KNN");

											final ClosestRequestMsg msg = new ClosestRequestMsg(req.getOriginFrom(),
													Ninaloader.me, req.getTarget(), req.getK(),
													req.getNearestNeighborIndex(), req.getVersion(), req.getSeed(),
													req.getRoot(), req.getFilter(), req.getForbiddenFilter());

											// current hop
											req.getHopRecords().add(Ninaloader.me);
											msg.getHopRecords().addAll(req.getHopRecords());

											// printHops("hop records for KNN:
											// ",msg.getHopRecords());
											// ===============
											// target

											msg.setTargetsForMultiObjKNN(req.targets);

											// relative coordinate
											msg.targetCoordinates = targetCoordinate;

											// record the hops for finding a nn
											msg.operationsForOneNN = req.operationsForOneNN;
											msg.operationsForOneNN++;

											// increase the message cost
											msg.increaseMessage(1);

											ringMembers.clear();
											// req.clear();

											// me
											/*
											 * if(returnID.equals(Ninaloader.me)
											 * ){ receiveClosestQuery(msg,
											 * errorBuffer); }else{
											 */

											log.info("receiveClosestQuery_MultipleTargets: next hop: "
													+ returnID.toString() + ", from: " + Ninaloader.me.toString()
													+ ", found " + msg.getNearestNeighborIndex().size() + "KNNs");

											comm.sendRequestMessage(msg, returnID,
													new ObjCommRRCB<ClosestResponseMsg>() {

												protected void cb(CBResult result, final ClosestResponseMsg responseMsg,
														AddressIF node, Long ts) {
													switch (result.state) {
													case OK: {
														log.info("Request Acked by new query node: ");
														// cdDone.call(result);
														// req.clear();
														// msg.clear();
														// nps.clear();
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
														Iterator ier2 = nps.iterator();
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
															if (msg.targets.contains(tmp.startNode)) {
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
															if (tmp.rtt <= baseRTT) {
																sortedL.add(tmp);
															}
														}
														findNewClosestNxtNode(sortedL, msg, baseRTT, errorBuffer);
														break;
													}
													}

													// msg.clear();
													// ----------------------
												}
												// --------------------------

											});// }

											/*
											 * if(msg!=null){ msg.clear(); }
											 * if(req!=null){ req.clear(); }
											 */

										}
										break;
									}
									case ERROR:
									case TIMEOUT: {
										log.warn("KNN Search: Target probes to " + req.getTarget() + " FAILED!"); // the
										// returned
										// map
										// is
										// null
										// we move backward
										backtracking(req, baseRTT, errorBuffer);
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
						returnKNN2Origin_MultipleTargets(msg3);

						break;
					}
					}

				}
			}

			);
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

			NodesPair tmp = new NodesPair(Ninaloader.me, null, lat, LastHopNode, req.operationsForOneNN);
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
		 * log.info("!I am the lastHop"); // finish the querying process:
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

			msg.setTargetsForMultiObjKNN(req.targets);
			/*
			 * //targets msg.setTargetsForMultiObjKNN(req.targets);
			 * 
			 * 
			 * //relative coordinate
			 * msg.targetCoordinates=req.targetCoordinates;
			 * 
			 * msg.operationsForOneNN=req.operationsForOneNN;
			 * msg.operationsForOneNN++;
			 * 
			 * msg.totalSendingMessages=req.totalSendingMessages;
			 * msg.increaseMessage(1);
			 */

			if (useNormalRingRange) {
				// relative coordinate
				msg.targetCoordinates = req.targetCoordinates;
			}
			msg.operationsForOneNN = req.operationsForOneNN;
			msg.operationsForOneNN++;

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

			/*
			 * if(req!=null){ req.clear(); }
			 */

		} else {
			// myself, if it is the sebacktracking, we finish
			CompleteNNRequestMsg msg3 = CompleteNNRequestMsg.makeCopy(req);
			// req.clear();
			msg3.from = Ninaloader.me;
			// send the result
			returnKNN2Origin_MultipleTargets(msg3);

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

		log.info("findAliveAncestor!");
		final List<AddressIF> ancestors = msg.HopRecords;

		boolean ContactAsker = false;

		if (ancestors.size() > 0) {

			log.info("\n==================\n The total No. of ancestors: " + ancestors.size()
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
						log.info("\n==================\n Alive No. of ancestors: " + nps.size()
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
						returnKNN2Origin_MultipleTargets(msg3);
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
							// send the result
							returnKNN2Origin_MultipleTargets(msg3);
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
										log.info("We find new lastHop node: " + LastHopNode);

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
							log.info("The last hop can not be me, in findAliveAncestor");
							// assert(false);
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
				returnKNN2Origin_MultipleTargets(msg3);
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
						log.info("\n==================\n Alive No. of next-hop node: " + nps.size()
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
									log.info("Request Acked by new query node: ");
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

	public class FinKNNRequestMsgHandler extends ResponseObjCommCB<FinKNNRequestMsg> {

		protected void cb(CBResult result, FinKNNRequestMsg arg1, AddressIF arg2, Long arg3, CB1<Boolean> arg4) {
			// TODO Auto-generated method stub

			switch (result.state) {
			case OK: {
				AddressIF target = arg1.target;
				int version = arg1.version;

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
			log.info("repeated from: " + answerNod);
			final long msgID = arg1.getMsgId();
			RepeatedNNResponseMsg msg2 = new RepeatedNNResponseMsg(Ninaloader.me);
			sendResponseMessage("repeated", answerNod, msg2, msgID, null, arg4);

			execKNN.execute(new Runnable() {

				public void run() {

					// backtracking process
					// backtracking process
					// from=lastHop
					/*
					 * int len=arg1.HopRecords.size(); AddressIF from1=null;
					 * if(len>0){ arg1.HopRecords.remove(len-1); }
					 * if(!arg1.HopRecords.isEmpty()){
					 * from1=arg1.HopRecords.get(arg1.HopRecords.size()-1); }
					 * final AddressIF from=from1;
					 */

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
					if (arg1.HopRecords != null && !arg1.HopRecords.isEmpty()) {
						msg1.getHopRecords().addAll(arg1.HopRecords);
					}

					// targets
					msg1.setTargetsForMultiObjKNN(arg1.targets);

					final StringBuffer errorBuffer = new StringBuffer();
					msg1.targetCoordinates = arg1.targetCoordinates;
					// ===========
					receiveClosestQuery_MultipleTargets(arg1.targetCoordinates, msg1, errorBuffer);
					// arg1.clear();

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

			final long msgID = arg1.getMsgId();

			TargetLocatedResponseMsg msg22 = new TargetLocatedResponseMsg(Ninaloader.me);
			sendResponseMessage("TargetLock", answerNod, msg22, msgID, null, arg4);

			int len = arg1.HopRecords.size();
			final AddressIF[] from = { null };
			if (len > 0) {
				from[0] = arg1.HopRecords.get(len - 1);
				// arg1.HopRecords.remove(len-1);
			}

			// independent thread
			execKNN.execute(new Runnable() {

				public void run() {
					// TODO Auto-generated method stub
					// reach the K limit, or there is no backtrack node

					/*
					 * int len=arg1.HopRecords.size();
					 * 
					 * AddressIF from1=null; if(len>0){
					 * arg1.HopRecords.remove(len-1); }
					 * if(!arg1.HopRecords.isEmpty()){
					 * from1=arg1.HopRecords.get(arg1.HopRecords.size()-1); }
					 */

					// TODO: error
					// from maybe null
					ClosestRequestMsg msg1 = new ClosestRequestMsg(arg1.OriginFrom, from[0], arg1.target, arg1.K,
							arg1.getNearestNeighborIndex(), arg1.version, arg1.seed, arg1.root, arg1.filter,
							arg1.ForbiddenFilter);

					msg1.getHopRecords().addAll(arg1.HopRecords);

					final StringBuffer errorBuffer = new StringBuffer();

					if (KNNManager.useNormalRingRange) {
						msg1.targetCoordinates = arg1.targetCoordinates;
					}
					msg1.operationsForOneNN = arg1.operationsForOneNN;

					// targets
					msg1.setTargetsForMultiObjKNN(arg1.targets);

					// arg1.clear();
					// if(arg1.targets!=null){
					receiveClosestQuery_MultipleTargets(arg1.targetCoordinates, msg1, errorBuffer);
					// }
				}
			});

		}

	}

	/**
	 * I am the orginFrom node, which send the KNN query
	 */
	void postProcessKNN_MultipleTargets(CompleteNNRequestMsg arg1) {

		log.info("=======================\n postProcessing" + "\n=======================");

		final Set<NodesPair> NNs = arg1.getNearestNeighborIndex();
		// callback invoked

		CBcached curCB = cachedCB.get(Long.valueOf(arg1.seed));
		if (curCB != null) {

			// match the seed, if it does not match, it means it is
			// another KNN search
			log.info("\n@@:\n KNN branch is reported from " + arg1.from);

			curCB.totalSendingMessage = arg1.totalSendingMessages;

			curCB.nps.addAll(NNs);
			// done, wake up the barrier
			curCB.wake.setNumForks(1);
			// curCB.wake.activate();
			curCB.wake.join();
		}

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
					postProcessKNN_MultipleTargets(arg1);
					// arg1.clear();

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

					receiveFathestReq_MultipleTargets(arg1, errorBuffer);

				}
			});

		}
	}

	@Override
	public void queryKFarthestNeighbors(AddressIF target, int k, CB1<List<NodesPair>> cbDone) {
		// TODO Auto-generated method stub

	}

	@Override
	public void queryKNN(String peeringNeighbor, String target, int K, CB1<List<NodesPair>> cbDone) {
		// TODO Auto-generated method stub

	}

	@Override
	public void queryKNearestNeighbors(AddressIF target, int k, CB1<List<NodesPair>> cbDone) {
		// TODO Auto-generated method stub

	}

	@Override
	public void queryKNearestNeighbors(AddressIF target, List<AddressIF> candidates, int k,
			CB1<List<NodesPair>> cbDone) {
		// TODO Auto-generated method stub

	}

	@Override
	public void receiveClosestQuery(Coordinate target, ClosestRequestMsg req, StringBuffer errorBuffer) {
		// TODO Auto-generated method stub

	}

	@Override
	public void receiveClosestQuery(Coordinate target, SubSetClosestRequestMsg req, StringBuffer errorBuffer) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sortNodesByDistances(Vector<AddressIF> addrs, int N, CB1<Set<NodesPair>> cbDone) {
		// TODO Auto-generated method stub

	}

	@Override
	public void queryNearestNeighbor(AddressIF target, int NumOfKNN, CB1<NodesPair> cbDone) {
		// TODO Auto-generated method stub

	}

}
