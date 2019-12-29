package edu.NUDT.pdl.Nina.KNN;

import java.io.IOException;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.NUDT.pdl.Nina.MeasureComm;
import edu.NUDT.pdl.Nina.Ninaloader;
import edu.NUDT.pdl.Nina.KNN.AbstractNNSearchManager.ClosestReqHandler;
import edu.NUDT.pdl.Nina.KNN.AbstractNNSearchManager.CollectHandler;
import edu.NUDT.pdl.Nina.KNN.AbstractNNSearchManager.DatHandler;
import edu.NUDT.pdl.Nina.KNN.AbstractNNSearchManager.GossipHandler;
import edu.NUDT.pdl.Nina.KNN.AbstractNNSearchManager.UpdateReqHandler;
import edu.NUDT.pdl.Nina.KNN.AbstractNNSearchManager.askcachedHistoricalNeighborsHandler;
import edu.NUDT.pdl.Nina.KNN.AbstractNNSearchManager.updateTargetHandler;
import edu.NUDT.pdl.Nina.KNN.KNNManager.CompleteNNReqHandler;
import edu.NUDT.pdl.Nina.KNN.KNNManager.FarthestReqHandler;
import edu.NUDT.pdl.Nina.KNN.KNNManager.FinKNNRequestMsgHandler;
import edu.NUDT.pdl.Nina.KNN.KNNManager.QueryKNNReqHandler;
import edu.NUDT.pdl.Nina.KNN.KNNManager.RepeatedNNReqHandler;
import edu.NUDT.pdl.Nina.KNN.KNNManager.SubSetClosestReqHandler;
import edu.NUDT.pdl.Nina.KNN.KNNManager.SubSetCompleteNNReqHandler;
import edu.NUDT.pdl.Nina.KNN.KNNManager.SubSetRepeatedNNReqHandler;
import edu.NUDT.pdl.Nina.KNN.KNNManager.SubSetTargetLockReqHandler;
import edu.NUDT.pdl.Nina.KNN.KNNManager.TargetLockReqHandler;
import edu.NUDT.pdl.Nina.KNN.KNNManager.WithdrawReqHandler;
import edu.NUDT.pdl.Nina.KNN.askcachedHistoricalNeighbors.askcachedHistoricalNeighborsRequestMsg;
import edu.NUDT.pdl.Nina.StableNC.lib.Coordinate;
import edu.NUDT.pdl.Nina.StableNC.lib.RemoteState;
import edu.NUDT.pdl.Nina.StableNC.nc.StableManager;
import edu.NUDT.pdl.pyxida.ping.PingManager;
import edu.harvard.syrah.prp.Log;
import edu.harvard.syrah.prp.POut;
import edu.harvard.syrah.prp.PUtil;
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
import edu.harvard.syrah.sbon.async.comm.obj.ObjCommCB;
import edu.harvard.syrah.sbon.async.comm.obj.ObjCommIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjCommRRCB;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessageIF;

public class MeridianSearchManager extends AbstractNNSearchManager {
	private Log log = new Log(MeridianSearchManager.class);

	// Set<AddressIF> bootstrapAddrs;
	// public Map<AddressIF, RemoteState<AddressIF>> pendingNeighbors;

	// final ExecutorService execMeridian =Executors.newFixedThreadPool(5);

	/*
	 * public final ObjCommIF comm; public final PingManager pingManager; public
	 * final StableManager ncManager;
	 */

	Map<Long, CBcached> cachedCB = new ConcurrentHashMap<Long, CBcached>(10);

	public MeridianSearchManager(ObjCommIF _GossipComm, ObjCommIF _comm, ObjCommIF _MeasureComm,
			PingManager pingManager, StableManager ncManager) {
		super(_GossipComm, _comm, _MeasureComm, pingManager, ncManager);
		/*
		 * comm=_comm; pingManager=_pingManager; ncManager=_ncManager;
		 */

		// execMeridian.execute(MClient);
		// Ninaloader.execNina.execute(MClient);
	}

	public void init(final CB0 cbDone) {

		super.init(new CB0() {
			protected void cb(CBResult result) {

				switch (result.state) {
				case OK: {

					// use the measurement com
					measureComm.registerMessageCB(UpdateRequestMsg.class, new updateTargetHandler());
					// ==================================================

					measureComm.registerMessageCB(DatRequestMsg.class, new DatHandler());

					measureComm.registerMessageCB(AnswerUpdateRequestMsg.class, new UpdateReqHandler());

					comm.registerMessageCB(ClosestRequestMsg.class, new ClosestReqHandler());

					// gossip comm
					gossipComm.registerMessageCB(KNNGossipRequestMsg.class, new GossipHandler());
					gossipComm.registerMessageCB(askcachedHistoricalNeighborsRequestMsg.class,
							new askcachedHistoricalNeighborsHandler());

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

					comm.registerMessageCB(KNNCollectRequestMsg.class, new CollectHandler());

					// ==================================================
					comm.registerMessageCB(MeridClosestRequestMsg.class, new MeridClosestReqHandler());
					comm.registerMessageCB(CompleteMeridianRequestMsg.class, new MeridianCompleteReqHandler());

					/**
					 * execute the Meridian method
					 */
					execKNN.execute(Meridian_MClient);

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

	}

	/**
	 * The Meridian process
	 * 
	 * @param req
	 * @param errorBuffer
	 */
	public void receiveClosestQuery(final MeridClosestRequestMsg req, final StringBuffer errorBuffer) {
		// TODO Auto-generated method stub

		final long sendStamp = System.nanoTime();

		req.totalSendingMessage += 1;

		ncManager.doPing(req.target, new CB2<Double, Long>() {

			protected void cb(CBResult result, Double lat, Long timer) {

				// TODO Auto-generated method stub
				double rtt_snap = (System.nanoTime() - sendStamp) / 1000000d;

				switch (result.state) {
				case OK: {
					if (lat.doubleValue() >= 0) {
						rtt_snap = lat.doubleValue();
					}

					AddressIF remoteAddr = req.target;

					final double rtt = rtt_snap;// ;
					log.debug("RTT of " + rtt + " from " + Ninaloader.me + " to " + req.target);

					// cachedHistoricalNeighbors.addElement(new
					// NodesPair(req.target,Ninaloader.me,rtt));

					Vector<AddressIF> ringMembers = new Vector<AddressIF>(10);

					// remove forbidden nodes
					Set<AddressIF> ForbiddenNodes = new HashSet<AddressIF>();
					Iterator<NodesPair> ier = req.getNearestNeighborIndex().iterator();
					while (ier.hasNext()) {
						AddressIF nextTarget = (AddressIF) ier.next().startNode;
						ForbiddenNodes.add(nextTarget);

					}
					ForbiddenNodes.addAll(req.getHopRecords());
					// no offset
					Meridian_MClient.instance.g_rings.fillVector(req.target, ForbiddenNodes, rtt_snap, rtt_snap,
							betaRatioMeridian, ringMembers, 0);

					ForbiddenNodes.clear();
					/*
					 * // add perturbation
					 * 
					 * Set<AddressIF> randomSeeds = new HashSet<AddressIF>(
					 * AbstractNNSearchManager.defaultNodesForRandomContact);
					 * randomSeeds.addAll(ringMembers);
					 * MClient.instance.g_rings.getRandomNodes(randomSeeds,
					 * AbstractNNSearchManager.defaultNodesForRandomContact);
					 * ringMembers.clear(); ringMembers.addAll(randomSeeds);
					 */

					log.debug("==================\n$: total nodes: " + ringMembers.size() + "\n==================");
					if (!ringMembers.isEmpty() && ringMembers.size() > 0) {

						// target node

						if (ringMembers.contains(req.target)) {
							ringMembers.remove(req.target);
						}

						/*
						 * if (Ninaloader.me.equals(req.target)) {
						 * System.err.println("~~~~~~~~~~~~~~~\n" +
						 * "I am the target\n~~~~~~~~~~~~~~~"); assert (false);
						 * // logic error }
						 */

					}

					// returned ID is null, or is the target itself;
					if (ringMembers == null || ((ringMembers != null) && (ringMembers.size() == 0))) {

						// no nearer nodes, so return me
						// System.out.println("Ring Empty! " + Ninaloader.me
						// + " is the farthest to: " + fmsg.target
						// + " @: " + rtt);

						CompleteMeridianRequestMsg msg3 = CompleteMeridianRequestMsg.makeCopy(req);
						msg3.from = Ninaloader.me;

						SubSetManager subset = new SubSetManager(req.getForbiddenFilter());
						if (!subset.checkNode(Ninaloader.me)) {
							msg3.getNearestNeighborIndex().add(new NodesPair(Ninaloader.me, req.target, rtt));
							subset.insertPeers2Subset(Ninaloader.me);
						}

						msg3.totalSendingMessage = req.totalSendingMessage;
						msg3.totalSendingMessage += 1;

						msg3.hasRepeated = req.hasRepeated;
						returnKNN2Origin(msg3);

						return;

					} else {
						/**
						 * ask them to ping
						 */
						req.totalSendingMessage += 3 * ringMembers.size();

						TargetProbes(ringMembers, req.target, new CB1<String>() {
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
							protected void cb(CBResult ncResult, Set<NodesPair> nps, String errorString) {
								switch (ncResult.state) {
								case OK: {
									AddressIF returnID = null;
									AddressIF minID = null;
									if (nps != null) {

										if (!req.getNearestNeighborIndex()
												.contains(new NodesPair(Ninaloader.me, req.target, rtt))) {
											nps.add(new NodesPair(Ninaloader.me, req.target, rtt));
										}

										double minimum = Double.MAX_VALUE;
										Iterator ier = nps.iterator();
										// bigger !
										while (ier.hasNext()) {
											NodesPair tmp = (NodesPair) ier.next();
											if (tmp.startNode.equals(req.target)) {
												continue;
											}
											// skip target
											if ((tmp.rtt) < minimum) {
												minID = (AddressIF) tmp.startNode;
												minimum = tmp.rtt;
											}

										}
										// ============================
										// -----------------------------------------
										if (minimum >= betaRatioMeridian * rtt) {

											if (minimum < rtt) {
												CompleteMeridianRequestMsg msg3 = CompleteMeridianRequestMsg
														.makeCopy(req);
												msg3.from = Ninaloader.me;

												SubSetManager subset = new SubSetManager(req.getForbiddenFilter());
												if (!subset.checkNode(minID)) {
													msg3.getNearestNeighborIndex()
															.add(new NodesPair(minID, req.target, minimum));
													subset.insertPeers2Subset(Ninaloader.me);
												}

												msg3.totalSendingMessage = req.totalSendingMessage;
												msg3.totalSendingMessage += 1;

												msg3.hasRepeated = req.hasRepeated;
												returnKNN2Origin(msg3);
												return;
											} else {
												// larger than current
												returnID = Ninaloader.me;
											}
										} else {
											returnID = minID;
										}
									} else {
										// nps is null
										returnID = Ninaloader.me;
									}

									if (returnID.equals(Ninaloader.me) || returnID == null) {
										// OK send originFrom a
										// message
										// Set<NodesPair>
										// ReturnedID=new
										// HashSet<NodesPair>();
										// ReturnedID.add(new
										// NodesPair(fmsg.target,
										// returnID,rtt));
										// closest query

										CompleteMeridianRequestMsg msg3 = CompleteMeridianRequestMsg.makeCopy(req);
										msg3.from = Ninaloader.me;

										if (!msg3.getNearestNeighborIndex()
												.contains(new NodesPair(Ninaloader.me, req.target, rtt))) {
											msg3.getNearestNeighborIndex()
													.add(new NodesPair(Ninaloader.me, req.target, rtt));
										}
										msg3.totalSendingMessage = req.totalSendingMessage;
										msg3.totalSendingMessage += 1;

										msg3.hasRepeated = req.hasRepeated;
										returnKNN2Origin(msg3);

										return;

									} else {

										//
										final MeridClosestRequestMsg msg = new MeridClosestRequestMsg(
												req.getOriginFrom(), Ninaloader.me, req.getTarget(), req.getK(),
												req.getNearestNeighborIndex(), req.getVersion(), req.getSeed(),
												req.getRoot(), req.getFilter(), req.getForbiddenFilter());

										msg.totalSendingMessage = req.totalSendingMessage;
										msg.totalSendingMessage += 1;
										msg.addCurrentHop(Ninaloader.me);

										msg.hasRepeated = req.hasRepeated;

										comm.sendRequestMessage(msg, returnID,
												new ObjCommRRCB<MeridClosestResponseMsg>() {
											protected void cb(CBResult result,
													final MeridClosestResponseMsg responseMsg, AddressIF node,
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
													log.warn("does not receive ack!");

													CompleteMeridianRequestMsg msg3 = CompleteMeridianRequestMsg
															.makeCopy(req);

													msg3.from = Ninaloader.me;

													SubSetManager subset = new SubSetManager(req.getForbiddenFilter());
													if (!subset.checkNode(Ninaloader.me)) {
														msg3.getNearestNeighborIndex()
																.add(new NodesPair(Ninaloader.me, req.target, rtt));
														subset.insertPeers2Subset(Ninaloader.me);
													}
													msg3.totalSendingMessage = req.totalSendingMessage;
													msg3.totalSendingMessage += 1;

													msg3.hasRepeated = req.hasRepeated;
													returnKNN2Origin(msg3);

													break;
												}
												}
												// ----------------------
											}
											// --------------------------
										});
									}

									break;
								}
								case ERROR:
								case TIMEOUT: {
									log.warn("closest Search: Target probes to " + req.target + " FAILED!"); // the
									// returned
									// map
									// is
									// null

									CompleteMeridianRequestMsg msg3 = CompleteMeridianRequestMsg.makeCopy(req);
									msg3.from = Ninaloader.me;
									if (!msg3.getNearestNeighborIndex()
											.contains(new NodesPair(Ninaloader.me, req.target, rtt))) {
										msg3.getNearestNeighborIndex()
												.add(new NodesPair(Ninaloader.me, req.target, rtt));
									}
									msg3.totalSendingMessage = req.totalSendingMessage;
									msg3.totalSendingMessage += 1;

									msg3.hasRepeated = req.hasRepeated;
									returnKNN2Origin(msg3);

									break;
								}
								}

							}//
						}

						);
					} // end of target probe
					ringMembers.clear();

					break;
				}
				case TIMEOUT:
				case ERROR: {

					String error = "RTT request to " + req.target.toString(false) + " failed:" + result.toString();
					log.warn(error);
					if (errorBuffer.length() != 0) {
						errorBuffer.append(",");
					}
					errorBuffer.append(error);

					CompleteMeridianRequestMsg msg3 = CompleteMeridianRequestMsg.makeCopy(req);
					msg3.from = Ninaloader.me;
					msg3.totalSendingMessage = req.totalSendingMessage;
					msg3.totalSendingMessage += 1;

					msg3.hasRepeated = req.hasRepeated;
					returnKNN2Origin(msg3);

					break;
				}
				}
			}

		});

	}

	public void returnKNN2Origin(CompleteMeridianRequestMsg msg3) {

		// test whether we finish
		/*
		 * if(msg3.K>=(msg3.getNearestNeighborIndex().size())&&msg3.hasRepeated<
		 * allowedRepeated){ restartKNNSearch(msg3); return; }else{
		 */

		// log.info("has restarted "+msg3.hasRepeated);
		// I should not work, since I have been a KNNer
		if (!Ninaloader.me.equals(msg3.OriginFrom)) {

			log.debug("!!!!!!!!!!!!!!!!!!!!!!\nComplete @ TargetLockReqHandler\n!!!!!!!!!!!!!!!!!!!!!!");
			// TODO: after the normal search process, we add the extra search
			// steps here

			msg3.totalSendingMessage += 1;

			// query completed, send msg to query nodes
			// send the result
			comm.sendRequestMessage(msg3, msg3.OriginFrom, new ObjCommRRCB<CompleteMeridianResponseMsg>() {

				protected void cb(CBResult result, final CompleteMeridianResponseMsg responseMsg, AddressIF node,
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
			postProcessKNN(msg3);
		}

		// }

	}

	/**
	 * restart the knn search
	 * 
	 * @param msg3
	 */
	private void restartKNNSearch(CompleteMeridianRequestMsg msg3) {
		// TODO Auto-generated method stub

		Set<AddressIF> forbidden = new HashSet<AddressIF>(2);
		Iterator<NodesPair> ier = msg3.getNearestNeighborIndex().iterator();
		while (ier.hasNext()) {
			NodesPair rec = ier.next();
			forbidden.add((AddressIF) rec.startNode);
		}
		final Set<AddressIF> randNodes = new HashSet<AddressIF>(10);
		Meridian_MClient.instance.g_rings.getRandomNodes(randNodes, forbidden, 4);
		randNodes.remove(Ninaloader.me);

		/**
		 * can not continue;
		 */
		if (randNodes.isEmpty()) {
			msg3.hasRepeated = allowedRepeated + 1;
			this.returnKNN2Origin(msg3);
			return;
		}

		AddressIF neighbor = PUtil.getRandomObject(randNodes);

		Set<NodesPair> NearestNeighborIndex = new HashSet<NodesPair>(10);
		NearestNeighborIndex.addAll(msg3.getNearestNeighborIndex());

		// int k=msg3.K-msg3.getNearestNeighborIndex().size();
		int k = msg3.K;
		int version = msg3.version;
		// long seed = msg3.seed;
		/**
		 * preserve the original KNN requestor's address
		 */
		MeridClosestRequestMsg msg = new MeridClosestRequestMsg(msg3.OriginFrom, Ninaloader.me, msg3.target, k,
				NearestNeighborIndex, version, msg3.seed, null, null, SubSetManager.getEmptyFilter());

		/**
		 * message cost
		 */
		msg.totalSendingMessage = msg3.totalSendingMessage;
		msg.totalSendingMessage += 1;
		// repeated
		msg.hasRepeated = msg3.hasRepeated;
		msg.hasRepeated++;

		// barrierUpdate.fork();
		comm.sendRequestMessage(msg, neighbor, new ObjCommRRCB<MeridClosestResponseMsg>() {

			protected void cb(CBResult result, final MeridClosestResponseMsg responseMsg, AddressIF node, Long ts) {
				switch (result.state) {
				case OK: {
					// log.debug("K Nearest Request received by new query node:
					// ");
					// cdDone.call(result);
					// save the callbacks
					// add into a list

					break;
				}
				case ERROR:
				case TIMEOUT: {
					// cbDone.call(result, null); // the returned
					// map is null
					break;
				}
				}
				// ----------------------
			}
			// --------------------------
		});

	}

	void postProcessKNN(CompleteMeridianRequestMsg arg1) {

		// log.info("=======================\n postProcessing"
		// + "\n=======================");

		final Set<NodesPair> NNs = arg1.getNearestNeighborIndex();
		// callback invoked

		CBcached curCB = cachedCB.get(Long.valueOf(arg1.seed));
		if (curCB != null) {

			curCB.totalSendingMessage = arg1.totalSendingMessage;
			// match the seed, if it does not match, it means it is
			// another KNN search
			log.info("\n@@:\n KNN branch is reported from " + arg1.from);
			curCB.nps.addAll(NNs);
			// done, wake up the barrier
			curCB.wake.setNumForks(1);
			curCB.wake.join();

		} else {
			log.warn("does not contain the KNN searched record!");
			log.warn("now seeding: " + arg1.seed);
			log.warn("total seed: " + POut.toString(cachedCB.keySet()));
			return;
		}

	}

	/**
	 * save the record
	 * 
	 * @param arg1
	 */
	/*
	 * public void addPostProcessRecord(List<NodesPair> arg1,int
	 * expectedKNN,AddressIF target){
	 * 
	 * double ClosestNodes=KNNStatistics.containClosestNodes(arg1); double
	 * KNNGains=KNNStatistics.KNNGains(arg1,
	 * cachedHistoricalNeighbors.currentWorstNearestNeighbors,
	 * cachedHistoricalNeighbors.currentBestNearestNeighbors); double
	 * absoluteError=KNNStatistics.absoluteError(arg1,
	 * cachedHistoricalNeighbors.currentBestNearestNeighbors); double
	 * RelativeError=KNNStatistics.RelativeError(arg1,
	 * cachedHistoricalNeighbors.currentBestNearestNeighbors); double
	 * coverageRate=KNNStatistics.coverage(arg1,
	 * cachedHistoricalNeighbors.currentBestNearestNeighbors);
	 * 
	 * 
	 * double time=arg1.get(0).elapsedTime; int
	 * totalSendingMessage=arg1.get(0).totalSendingMessage;
	 * 
	 * // record try { String header = "\nTestMeridian\n";
	 * logKNNPerformance(Ninaloader.logKNN,ClosestNodes,KNNGains,absoluteError,
	 * RelativeError,totalSendingMessage,time,(arg1.size()+0.0)/expectedKNN,
	 * expectedKNN,arg1, target,header,coverageRate); } catch (IOException e) {
	 * // TODO Auto-generated catch block e.printStackTrace(); }
	 * 
	 * }
	 */

	public abstract class ResponseObjCommCB<T extends ObjMessageIF> extends ObjCommCB<T> {

		void sendResponseMessage(final String handler, final AddressIF remoteAddr, final ObjMessage response,
				long requestMsgId, final String errorMessage, final CB1<Boolean> cbHandled) {

			if (errorMessage != null) {
				log.warn(handler + " :" + errorMessage);
			}

			comm.sendResponseMessage(response, remoteAddr, requestMsgId, new CB0() {
				protected void cb(CBResult sendResult) {
					switch (sendResult.state) {
					case TIMEOUT:
					case ERROR: {
						log.warn(handler + ": " + sendResult.what);
						return;
					}
					}
				}
			});
			cbHandled.call(CBResult.OK(), true);
		}
	}

	public class MeridianCompleteReqHandler extends ResponseObjCommCB<CompleteMeridianRequestMsg> {

		protected void cb(CBResult arg0, CompleteMeridianRequestMsg arg1, AddressIF arg2, Long arg3,
				CB1<Boolean> arg4) {
			// TODO Auto-generated method stub
			final AddressIF answerNod = arg1.from;
			final AddressIF original = arg1.OriginFrom;
			final AddressIF to = arg1.target;
			// System.out.println("kNN Request from: "+answerNod);

			final long msgID = arg1.getMsgId();
			CompleteMeridianResponseMsg msg2 = new CompleteMeridianResponseMsg(Ninaloader.me);

			final StringBuffer errorBuffer = new StringBuffer();

			log.info("Meridian Search received, from " + arg1.from);
			sendResponseMessage("MeridianComplete", answerNod, msg2, msgID, null, arg4);

			postProcessKNN(arg1);

		}

	}

	public class MeridClosestReqHandler extends ResponseObjCommCB<MeridClosestRequestMsg> {

		protected void cb(CBResult arg0, MeridClosestRequestMsg arg1, AddressIF arg2, Long arg3, CB1<Boolean> arg4) {
			// TODO Auto-generated method stub
			final AddressIF answerNod = arg1.getFrom();
			final AddressIF original = arg1.getOriginFrom();
			final AddressIF to = arg1.getTarget();
			// System.out.println("kNN Request from: "+answerNod);

			final long msgID = arg1.getMsgId();
			MeridClosestResponseMsg msg2 = new MeridClosestResponseMsg(original, Ninaloader.me);

			final StringBuffer errorBuffer = new StringBuffer();

			log.debug("Meridian Search received, from " + arg1.from);
			sendResponseMessage("Closest", answerNod, msg2, msgID, null, arg4);

			receiveClosestQuery(arg1, errorBuffer);
			//
			/*
			 * receiveClosestQuery(original, to,errorBuffer, new CB0(){
			 * protected void cb(CBResult result) {
			 * 
			 * switch (result.state) { case OK: { // Initialise the external
			 * APIs
			 * 
			 * break; } default: { String error = "fail to find closest";
			 * log.warn(error); break; } } } });
			 */

		}

	}

	/**
	 * Meridian entry, 1 nearest neighbor
	 * 
	 * @param target
	 * @param cbDone
	 */
	public void queryKNearestNeighbors(final AddressIF target, final int expectedKNN,
			final CB1<List<NodesPair>> cbDone) {
		// TODO Auto-generated method stub

		final Set<AddressIF> randNodes = new HashSet<AddressIF>(10);

		// rings
		Meridian_MClient.instance.g_rings.getRandomNodes(randNodes, 4);

		log.info("\n\nThe Ring is: " + Meridian_MClient.instance.g_rings.getAllNodes());
		if (randNodes.isEmpty()) {
			log.info("\n\nEmpty rings\n\n");
			cbDone.call(CBResult.ERROR(), null);
			return;
		}
		List<AddressIF> Inode = new ArrayList<AddressIF>(4);
		Inode.addAll(randNodes);

		int counter = 4;
		while (Inode.contains(target)) {
			Inode.remove(target);
		}
		if (Inode.isEmpty()) {
			log.info("\n\nEmpty rings\n\n");
			cbDone.call(CBResult.ERROR(), null);
			return;
		}
		final AddressIF dest = Inode.get(0);

		final long seed = Ninaloader.random.nextLong();
		final Barrier barrierUpdate = new Barrier(true);
		// save the CB
		// ========================================
		long timer = System.currentTimeMillis();
		final int version = Ninaloader.random.nextInt();
		if (!cachedCB.containsKey(Long.valueOf(seed))) {
			// Vector<CBcached> cached = new Vector<CBcached>(
			// 5);

			CBcached cb = new CBcached(cbDone, barrierUpdate, seed, timer);
			cb.ExpectedNumber = expectedKNN;
			cb.version = version;
			cachedCB.put(Long.valueOf(seed), cb);
		} else {
			log.warn("repeated seed!");
			cbDone.call(CBResult.ERROR(), null);
			return;
		}

		Set<NodesPair> NearestNeighborIndex = new HashSet<NodesPair>(10);

		int k = expectedKNN;
		MeridClosestRequestMsg msg = new MeridClosestRequestMsg(Ninaloader.me, Ninaloader.me, target, k,
				NearestNeighborIndex, version, seed, null, null, SubSetManager.getEmptyFilter());

		msg.totalSendingMessage += 1;

		msg.hasRepeated = 0;
		log.info("\n\n Send Meridian request to " + dest.toString() + "\n\n");

		barrierUpdate.fork();

		comm.sendRequestMessage(msg, dest, new ObjCommRRCB<MeridClosestResponseMsg>() {

			protected void cb(CBResult result, final MeridClosestResponseMsg responseMsg, AddressIF node, Long ts) {
				switch (result.state) {
				case OK: {
					// log.debug("K Nearest Request received by new query node:
					// ");
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

		randNodes.clear();
		// long interval=4*1000;
		EL.get().registerTimerCB(barrierUpdate, new CB0() {

			protected void cb(CBResult result) {

				// merge the results

				// if it does not appear, add
				// long curTime = System.currentTimeMillis();

				CBcached curCB = cachedCB.get(Long.valueOf(seed));

				// odd, we can not find the record
				if (curCB == null) {
					log.warn("can not find the record!");
					cbDone.call(CBResult.ERROR(), null);
					return;
				}

				int k = curCB.ExpectedNumber;

				int totalSendingMessage = curCB.totalSendingMessage;

				if (curCB != null) {

					long curTimer = System.currentTimeMillis();
					long elapsedTime = curTimer - curCB.timeStamp;
					// ElapsedKNNSearchTime.add(elapsedTime);

					List<NodesPair> nps = new ArrayList<NodesPair>(5);
					// List<AddressIF> tmpKNN = new ArrayList<AddressIF>(5);

					// List rtts=new ArrayList(5);
					Map<AddressIF, RemoteState<AddressIF>> maps = new HashMap<AddressIF, RemoteState<AddressIF>>(5);

					Set<NodesPair> nodePairs = new HashSet<NodesPair>(2);
					nodePairs.addAll(curCB.nps);

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
						log.debug("KNN Number: " + nps.size() + " required: " + k);

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
	public void queryKNearestNeighbors(AddressIF target, List<AddressIF> candidates, int k,
			CB1<List<NodesPair>> cbDone) {
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

	@Override
	public void receiveClosestQuery(Coordinate target, ClosestRequestMsg req, StringBuffer errorBuffer) {
		// TODO Auto-generated method stub

	}

	@Override
	public void receiveClosestQuery(Coordinate target, SubSetClosestRequestMsg req, StringBuffer errorBuffer) {
		// TODO Auto-generated method stub

	}

	@Override
	public void receiveClosestQuery_MultipleTargets(Coordinate targetCoordinate, ClosestRequestMsg req,
			StringBuffer errorBuffer) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sortNodesByDistances(Vector<AddressIF> addrs, int N, CB1<Set<NodesPair>> cbDone) {
		// TODO Auto-generated method stub

	}

}
