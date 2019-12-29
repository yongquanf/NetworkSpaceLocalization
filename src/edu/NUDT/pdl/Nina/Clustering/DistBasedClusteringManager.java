package edu.NUDT.pdl.Nina.Clustering;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;

import edu.NUDT.pdl.Nina.Ninaloader;
import edu.NUDT.pdl.Nina.KNN.AbstractNNSearchManager;
import edu.NUDT.pdl.Nina.KNN.NodesPair;
import edu.NUDT.pdl.Nina.StableNC.lib.RemoteState;
import edu.NUDT.pdl.Nina.StableNC.nc.StableManager;
import edu.harvard.syrah.prp.ANSI;
import edu.harvard.syrah.prp.Log;
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

public class DistBasedClusteringManager extends  HSH_Manager<AddressIF> {

	final int LandmarkThresholds = 15;

	static long[] sendStamp;

	final ObjCommIF comm;
	HSHClient localNode = null;
	// if the node is host, then new a hostClient objective
	HostClient hostNode = null;

	static AddressIF UpdateSource;

	// Number of dimensions, in version 1, we set the dimension manually,
	// TODO: in Version 2, we extend the adaptive dimension estimation process
	// public static int NC_NUM_DIMS = Integer.parseInt(Config.getProperty(
	// "ClusteringDimension", "2"));

	public static final String[] bootstrapList = Config.getProperty(
			"bootstraplist", "r1d15.pyxida.pdl").split("[\\s]");

	public static final String[] UpdateSrc = Config.getProperty("updateSource",
			"").split("[\\s]");

	public static final boolean WATCH_NEIGHBORS = Boolean
			.parseBoolean(Config.getConfigProps().getProperty(
					"ncmanager.watch_neighbors", "false"));

	// public static final boolean IS_HEADER=
	// Boolean.parseBoolean(Config.getConfigProps().getProperty(
	// "isHead", "false"));

	public boolean IS_HEADER = false;

	// number of landmarks
	public static final int NumberOfLandmarks = Integer.parseInt(Config
			.getConfigProps().getProperty("NumberOfLandmarks4HSH", "15"));

	// cache hosts that are seen so far, time out in t secs, if no cotaction is
	// received from that node
	Set<AddressIF> bootstrapAddrs;
	Set<AddressIF> pendingNeighbors;
	Set<AddressIF> upNeighbors = new HashSet<AddressIF>();
	Set<AddressIF> downNeighbors = new HashSet<AddressIF>();

	// cache the alive landmarks
	public final static Map<Long, HSHRecord> pendingHSHLandmarks = new HashMap<Long, HSHRecord>(
			1);

	long timeStamp4Clustering;
	// H & S are belong to core node. Core node sends them to host nodes
	double[][] H, S;

	// Coord record all the node's coordinate in the network
	double[] Coord;

	ClusteringSet clusteringVector;
	
	//
	Matrix_4_HSH MatrixOps;

	long versionNumber = -1;

	// for HSH clustering
	class HSHRecord {
		final long timeStamp;

		List<AddressIF> IndexOfLandmarks;

		// completed the clustering process
		boolean alreadyComputedClustering = false;

		boolean readyForUpdate = false;
		// H & S are belong to core node. Core node sends them to host nodes
		double[][] H, S;
		int UnReceivedNodes;
		long lastUpdateTime;
		// ============================================
		// //for update source node
		HashMap<AddressIF, ArrayList<RemoteState<AddressIF>>> pendingLandmarkLatency = new HashMap<AddressIF, ArrayList<RemoteState<AddressIF>>>(
				1);

		// latency matrix
		double[][] DistanceMatrix;
		long version;

		/*
		 * public HSHRecord(double[][] _H, double[][] _S, double[] _Coord, long
		 * _timeStamp){ H=_H; S=_S; Coord=_Coord; timeStamp=_timeStamp;
		 * 
		 * }
		 */
		public HSHRecord(long _timeStamp) {
			timeStamp = _timeStamp;
			IndexOfLandmarks = new ArrayList<AddressIF>(1);
		}

		public void addLandmarks(AddressIF landmark) {
			if (!IndexOfLandmarks.contains(landmark)) {
				IndexOfLandmarks.add(landmark);
			}
		}

		void clear() {
			IndexOfLandmarks.clear();
			IndexOfLandmarks = null;
			pendingLandmarkLatency.clear();
			DistanceMatrix = null;

		}
	}

	public ClusteringSet getClusteringVector() {
		return clusteringVector;
	}
	
	// ============================================

	// head: r1d14.pyxida.pdl
	
	/**
	 * Time between gossip messages to coordinate neighbors. Default is 10
	 * seconds.
	 */
	public static final long UPDATE_DELAY = 10 * 1000;

	static final Log log = new Log(HSH_Manager.class);

	protected static StableManager ncManager; // interface to NC Manager
	protected AbstractNNSearchManager NNManager;

	public Map<AddressIF,clusteringRecord<AddressIF>> clusCache;

	public DistBasedClusteringManager(ObjCommIF _comm, StableManager _ncManager,
			AbstractNNSearchManager NN_manager) {
		SystemStartTime = System.currentTimeMillis();
		comm = _comm;
		ncManager = _ncManager;
		NNManager = NN_manager;
		// MyHSHRecord=new HSHRecord();
		MatrixOps = new Matrix_4_HSH();
	}

	public void init(final CB0 cbDone) {
		comm.registerMessageCB(ClustCollectRequestMsg.class,
				new CollectHandler());
		comm
				.registerMessageCB(ClustUpdateRequestMsg.class,
						new updateHandler());
		comm.registerMessageCB(CluDatRequestMsg.class, new DatHandler());
		// When a host node send its request for H & S, core node do the job in
		// ReleaseHandler()
		comm.registerMessageCB(ClustReleaseRequestMsg.class,
				new ReleaseHandler());
		// Core node send its H & S to all landmarks after updating the distance
		// matrix
		comm.registerMessageCB(ClustIssueRequestMsg.class, new IssueHandler());

		comm.registerMessageCB(LandmarkRequestMsg.class,
				new LandmarkRequestHandler());

		bootstrapAddrs = new HashSet<AddressIF>();
		pendingNeighbors = new HashSet<AddressIF>();

		// =====================================================
		AddressFactory.createResolved(Arrays.asList(UpdateSrc),
				Ninaloader.COMM_PORT, new CB1<Map<String, AddressIF>>() {
					protected void cb(CBResult result,
							Map<String, AddressIF> addrMap) {
						switch (result.state) {
						case OK: {

							for (String node : addrMap.keySet()) {

								AddressIF remoteAddr = addrMap.get(node);
								UpdateSource = remoteAddr;
								System.out.println("Update Source Addr='"
										+ UpdateSource + "'");
								break;
							}
							if (Ninaloader.me.equals(UpdateSource)) {
								IS_HEADER = true;
							}
							// ====================================
							if (IS_HEADER) {
								System.out
										.println(" We are ready to register the Update procedure");
								localNode = new HSHClient();
								hostNode = null;
							} else {
								localNode = null;
								// at the beginning, nodesList is empty. So we
								// couldn't make sure which node is host node
								hostNode = new HostClient();

							}
							// ====================================
							// Starts local coordinate timer
							if (localNode != null) {
								localNode.init();
							}
							// if the node is host, start the timer for release
							if (hostNode != null) {

								hostNode.init();
							}

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

	// define what to do once the different types of request messages have been
	// received
	abstract class ResponseObjCommCB<T extends ObjMessageIF> extends
			ObjCommCB<T> {
		void sendResponseMessage(final String handler,
				final AddressIF remoteAddr11, final ObjMessage response,
				long requestMsgId, final String errorMessage,
				final CB1<Boolean> cbHandled) {
			if (errorMessage != null) {
				log.warn(handler + " : " + errorMessage);
			}

			comm.sendResponseMessage(response, remoteAddr11, requestMsgId,
					new CB0() {
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

	class LandmarkRequestHandler extends ResponseObjCommCB<LandmarkRequestMsg> {

		public void cb(CBResult result, LandmarkRequestMsg msg,
				AddressIF remoteAddr1, Long ts, CB1<Boolean> cbHandled) {
			System.out.println("in CollectHandler cb");

			addUpNeighbor(msg.from);
			// response
			LandmarkResponseMsg msg1 = new LandmarkResponseMsg();
			msg1.setResponse(true);
			msg1.setMsgId(msg.getMsgId());
			// System.out.println("$ @@ Maybe landmarkRequest from: "+remoteAddr1+", but from: "+msg.from);

			sendResponseMessage("LandmarkRequest", msg.from, msg1, msg
					.getMsgId(), null, cbHandled);

		}
	}

	class CollectHandler extends ResponseObjCommCB<ClustCollectRequestMsg> {

		public void cb(CBResult result, ClustCollectRequestMsg msg,
				AddressIF remoteAddr1, Long ts, CB1<Boolean> cbHandled) {
			System.out.println("in CollectHandler cb");
			// response
			ClustCollectResponseMsg msg1 = new ClustCollectResponseMsg();
			msg1.setResponse(true);
			msg1.setMsgId(msg.getMsgId());
			System.out.println("$ @@ Maybe CollectRequest from: " + remoteAddr1
					+ ", but from: " + msg.from);

			sendResponseMessage("Collect", msg.from, msg1, msg.getMsgId(),
					null, cbHandled);

		}
	}

	class DatHandler extends ResponseObjCommCB<CluDatRequestMsg> {
		public void cb(CBResult result, CluDatRequestMsg msg33,
				AddressIF remoteAddr, Long ts, CB1<Boolean> cbHandled) {

			System.out.println(Ninaloader.me + " has received rtts from "
					+ msg33.from);
			// coreNode.nodesPairSet <- UpdateResponseMsg.latencySet (viz
			// remNode.nodesPairSet);
			Iterator ier = msg33.latencySet.iterator();
			CluDatResponseMsg msg1 = new CluDatResponseMsg(Ninaloader.me);
			sendResponseMessage("Dat", msg33.from, msg1, msg33.getMsgId(),
					null, cbHandled);

			if (pendingHSHLandmarks.containsKey(Long.valueOf(msg33.timeStamp))) {
				// HSHRecord list =
				// pendingHSHLandmarks.get(Long.valueOf(msg33.timeStamp));
				HashMap<AddressIF, ArrayList<RemoteState<AddressIF>>> LatList = pendingHSHLandmarks
						.get(Long.valueOf(msg33.timeStamp)).pendingLandmarkLatency;
				System.out.println("\n\n TimeStamp: " + msg33.timeStamp
						+ "\n\n");
				AddressIF srcNode = msg33.from;
				while (ier.hasNext()) {

					NodesPair nxt = (NodesPair) ier.next();

					// System.out.println("$: "+nxt.toString());

					if (nxt.rtt < 0) {
						continue;
					}

					// endNode
					if (!LatList.containsKey(srcNode)) {
						ArrayList<RemoteState<AddressIF>> tmp = new ArrayList<RemoteState<AddressIF>>(
								1);
						RemoteState<AddressIF> state = new RemoteState<AddressIF>(
								(AddressIF)nxt.endNode);
						state.addSample(nxt.rtt);
						tmp.add(state);
						LatList.put(srcNode, tmp);

					} else {

						ArrayList<RemoteState<AddressIF>> tmp = LatList
								.get(srcNode);
						Iterator<RemoteState<AddressIF>> IER = tmp.iterator();

						// ========================================
						boolean found = false;
						RemoteState<AddressIF> S = null;
						while (IER.hasNext()) {
							RemoteState<AddressIF> state = IER.next();
							if (state.getAddress().equals(nxt.endNode)) {
								found = true;
								S = state;
								break;
							}
						}
						// ========================================
						if (found) {
							S.addSample(nxt.rtt);
							LatList.put(srcNode, tmp);
						} else {
							S = new RemoteState<AddressIF>((AddressIF)nxt.endNode);
							S.addSample(nxt.rtt);
							tmp.add(S);
							LatList.put(srcNode, tmp);
						}
					}
				}

				// test if we can start clustering computation
				HashMap<AddressIF, ArrayList<RemoteState<AddressIF>>> latRecords = pendingHSHLandmarks
						.get(Long.valueOf(msg33.timeStamp)).pendingLandmarkLatency;
				HSHRecord curLandmarks = pendingHSHLandmarks.get(Long
						.valueOf(msg33.timeStamp));

				if (latRecords != null
						&& !latRecords.isEmpty()
						&& curLandmarks != null
						&& curLandmarks.IndexOfLandmarks != null
						&& !curLandmarks.IndexOfLandmarks.isEmpty()
						&& latRecords.keySet().size() > 0.7 * curLandmarks.IndexOfLandmarks
								.size()) {
					localNode.updateDistMatrix(msg33.timeStamp);
				}

			}

		}
	}

	class IssueHandler extends ResponseObjCommCB<ClustIssueRequestMsg> {

		public void cb(CBResult result, ClustIssueRequestMsg msg,
				AddressIF remoteAddr1, Long ts, CB1<Boolean> cbHandled) {
			System.out.println("in IssueHandler cb");

			int NODES_NUM = msg.H.length;
			int NC_NUM_DIMS = msg.S.length;
			timeStamp4Clustering = msg.timeStamp;

			if (H == null || H.length != NODES_NUM || S.length != NC_NUM_DIMS) {
				H = null;
				S = null;
				H = new double[NODES_NUM][NC_NUM_DIMS];
				S = new double[NC_NUM_DIMS][NC_NUM_DIMS];
				Coord = new double[NC_NUM_DIMS];
			}
			// landmark.H <- coreNode.H
			for (int i = 0; i < NODES_NUM; i++)
				for (int j = 0; j < NC_NUM_DIMS; j++)
					H[i][j] = msg.H[i][j];
			// landmark.S <- coreNode.S
			for (int i = 0; i < NC_NUM_DIMS; i++)
				for (int j = 0; j < NC_NUM_DIMS; j++)
					S[i][j] = msg.S[i][j];

			int ind = msg.IndexOfFrom;

			// fill the landmark's Coord
			for (int j = 0; j < NC_NUM_DIMS; j++)
				Coord[j] = H[ind][j];

			// new version of clustering results
			versionNumber = msg.version;
			try {
				logHSHClusteringResults();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println(Ninaloader.me + "has acquired its coordinate");
			ClustIssueResponseMsg msg1 = new ClustIssueResponseMsg(
					Ninaloader.me);
			msg1.setResponse(true);
			msg1.setMsgId(msg.getMsgId());

			sendResponseMessage("Issue", msg.from, msg1, msg.getMsgId(), null,
					cbHandled);
		}
	}

	void logHSHClusteringResults() throws IOException {

		Ninaloader.logCluster=null;
		try{
			Ninaloader.logCluster = new BufferedWriter(new FileWriter(new File(
					Ninaloader.ClusteringLogName + ".log")));
		double timer = (System.currentTimeMillis() - Ninaloader.StartTime) / 1000d;
		Ninaloader.logCluster.write("Timer: " + Ninaloader.getUptimeStr() + "\n");
		int len = Coord.length;
		for (int i = 0; i < len; i++) {
			Ninaloader.logCluster.write(Coord[i] + " ");
		}
		Ninaloader.logCluster.write("\n");
		Ninaloader.logCluster.flush();
		
		}catch(IOException e){
			e.printStackTrace();
		}finally{
			try{
				if(Ninaloader.logCluster!=null){
				Ninaloader.logCluster.close();
				}
			}catch(IOException e){
				e.printStackTrace();
			}
		}
	}

	// after the centralized computation, the core node issue H & S to landmarks
	// forwardly
	void issueCoords(final long timeStamp, final List<AddressIF> nodes,
			final CB1<String> cbCoords) {

		if (nodes.size() == 0) {
			String errorString = "getRemoteCoords: no valid nodes";
			log.warn(errorString);
			cbCoords.call(CBResult.OK(), errorString);
			return;
		}

		Barrier barrierIssue = new Barrier(true);
		final StringBuffer errorBuffer = new StringBuffer();

		for (AddressIF addr : nodes) {
			issueCoord(timeStamp, addr, barrierIssue, errorBuffer);
		}

		EL.get().registerTimerCB(barrierIssue, new CB0() {
			protected void cb(CBResult result) {
				String errorString;
				if (errorBuffer.length() == 0) {
					errorString = new String("Success");
				} else {
					errorString = new String(errorBuffer);
				}
				cbCoords.call(CBResult.OK(), errorString);
			}
		});
	}

	void issueCoord(long timeStamp, final AddressIF remNode,
			final Barrier barrier, final StringBuffer errorBuffer) {
		if (Ninaloader.me.equals(remNode))
			return;

		HSHRecord curRecord = pendingHSHLandmarks.get(Long.valueOf(timeStamp));
		if (curRecord.H == null) {
			return;
		}
		int NODES_NUM = curRecord.H.length;
		int NC_NUM_DIMS = curRecord.S.length;

		double _H[][] = new double[NODES_NUM][NC_NUM_DIMS];
		double _S[][] = new double[NC_NUM_DIMS][NC_NUM_DIMS];
		_H = makeCopy(curRecord.H, NODES_NUM, NC_NUM_DIMS);
		_S = makeCopy(curRecord.S, NC_NUM_DIMS, NC_NUM_DIMS);

		int ind = curRecord.IndexOfLandmarks.indexOf(remNode);

		ClustIssueRequestMsg msg = new ClustIssueRequestMsg(Ninaloader.me, _H,
				_S, ind, timeStamp, curRecord.version);
		System.out.println("Core node issues H & S to " + remNode);

		barrier.fork();
		comm.sendRequestMessage(msg, remNode,
				new ObjCommRRCB<ClustIssueResponseMsg>() {
					protected void cb(CBResult result,
							final ClustIssueResponseMsg responseMsg,
							AddressIF node, Long ts) {
						switch (result.state) {
						case OK: {
							System.out
									.println("$ ! H & S have been received from"
											+ responseMsg.from);
							break;
						}
						case ERROR:
						case TIMEOUT: {
							String error = remNode.toString(false)
									+ "has not received requirement, as: "
									+ result.toString();
							log.warn(error);
							break;
						}
						}
						barrier.join();
					}
				});
	}

	// core node send its H & S to the node which launches release request
	class ReleaseHandler extends ResponseObjCommCB<ClustReleaseRequestMsg> {
		public void cb(CBResult result, ClustReleaseRequestMsg msg,
				AddressIF remoteAddr1, Long ts, CB1<Boolean> cbHandled) {

			// addPendingNeighbor(msg.from);

			// TODO: test the initialization process
			ClustReleaseResponseMsg msg1 = null;

			System.out.println("in ReleaseHandler cb");
			if (pendingHSHLandmarks.isEmpty()) {
				msg1 = new ClustReleaseResponseMsg(UpdateSource, null, null,
						null, -1);
				sendResponseMessage("Release", msg.from, msg1, msg.getMsgId(),
						null, cbHandled);
				return;
			}
			// not empty
			long timeStamp = Long.MIN_VALUE;
			Iterator<Long> ierTimer = pendingHSHLandmarks.keySet().iterator();
			while (ierTimer.hasNext()) {
				long tmp = ierTimer.next().longValue();
				if (timeStamp < tmp
						&& pendingHSHLandmarks.get(Long.valueOf(tmp)).alreadyComputedClustering) {
					timeStamp = tmp;
					break;
				}
			}

			HSHRecord record = pendingHSHLandmarks.get(Long.valueOf(timeStamp));

			if (record != null && record.alreadyComputedClustering) {

				int NODES_NUM = record.H.length;
				int NC_NUM_DIMS = record.S.length;

				double _H[][] = new double[NODES_NUM][NC_NUM_DIMS];
				double _S[][] = new double[NC_NUM_DIMS][NC_NUM_DIMS];
				_H = makeCopy(record.H, NODES_NUM, NC_NUM_DIMS);
				_S = makeCopy(record.S, NC_NUM_DIMS, NC_NUM_DIMS);

				msg1 = new ClustReleaseResponseMsg(UpdateSource, _H, _S,
						record.IndexOfLandmarks, versionNumber);
				msg1.setResponse(true);
				msg1.setMsgId(msg.getMsgId());
			} else {
				msg1 = new ClustReleaseResponseMsg(UpdateSource, null, null,
						null, -1);
			}
			sendResponseMessage("Release", msg.from, msg1, msg.getMsgId(),
					null, cbHandled);
		}
	}

	// make a copy of double[][]
	double[][] makeCopy(double[][] M, int row, int column) {
		double _M[][] = new double[row][column];
		for (int i = 0; i < row; i++)
			for (int j = 0; j < column; j++)
				_M[i][j] = M[i][j];
		return _M;
	}

	// update request message received, and then send corresponding response
	// back
	class updateHandler extends ResponseObjCommCB<ClustUpdateRequestMsg> {
		public void cb(CBResult result, ClustUpdateRequestMsg msg22,
				AddressIF remoteAddr2, Long ts, CB1<Boolean> cbHandled) {
			log.debug("in UpdateHandler cb");

			final AddressIF origin_remoteAddr = msg22.from;
			final long msgID22 = msg22.getMsgId();
			final long timer = msg22.timeStamp;

			// System.out.println("$ @@ Maybe UpdateRequest from: "+remoteAddr2+" but is from "+origin_remoteAddr);

			ClustUpdateResponseMsg msg1 = new ClustUpdateResponseMsg(
					Ninaloader.me);
			System.out.println("$ @@ Update acked to: " + origin_remoteAddr
					+ " With ID " + msgID22);

			sendResponseMessage("update", origin_remoteAddr, msg1, msgID22,
					null, cbHandled);

			// send collect request message to other landmarks
			if (msg22.landmarks == null || msg22.landmarks.size() == 0) {
				System.out.println("$ empty landmarks from  "
						+ origin_remoteAddr.toString());
				return;
			} else {
				NNManager.collectRTTs(msg22.landmarks, new CB2<Set<NodesPair>, String>() {
					protected void cb(CBResult ncResult, Set<NodesPair> nps,
							String errorString) {
						// send data request message to the core node
						CluDatRequestMsg datPayload = new CluDatRequestMsg(
								Ninaloader.me, nps, timer);
						System.out.println("$ @@ Dat is sent to: "
								+ origin_remoteAddr + " With ID " + msgID22);

						comm.sendRequestMessage(datPayload, origin_remoteAddr,
								new ObjCommRRCB<CluDatResponseMsg>() {
									protected void cb(
											CBResult result,
											final CluDatResponseMsg responseMsg22,
											AddressIF node, Long ts) {
										switch (result.state) {
										case OK: {
											System.out
													.println("$ ! Dat is ACKed from"
															+ responseMsg22.from);
											break;
										}
										case ERROR:
										case TIMEOUT: {
											String error = origin_remoteAddr
													.toString(false)
													+ "has not received responses from"
													+ origin_remoteAddr
															.toString()
													+ ", as: "
													+ result.toString();
											log.warn(error);
											break;
										}
										}
									}
								});
					}
				});

			}
		}
	}

	/*public static void collectRTTs(final Set<AddressIF> landmarks,
			final CB2<Set<NodesPair>, String> cbCoords) {

		final Set<NodesPair> nodesPairSet = new HashSet<NodesPair>(1);

		if (landmarks.size() == 0) {
			String errorString = "getRemoteCoords: no valid nodes";
			System.out.println(errorString);
			cbCoords.call(CBResult.OK(), nodesPairSet, errorString);
			return;
		}

		final Barrier barrier = new Barrier(true); // Collect

		final StringBuffer errorBuffer = new StringBuffer();
		// System.out.println("$@ @ Collect from: "+pendingLandmarks.size()+" nodes "+pendingLandmarks.toString());
		// when "for" is run over, the local node's nodesPairSet will get the
		// rtt informations from itself to the other nodes
		
		double []latency =new double[1];
		latency[0]=-1;
		
		for (AddressIF addr : landmarks) {
			
			final AddressIF remNod=addr;
			//me
			if (Ninaloader.me.equals(remNod)) {
				continue;
			} else
			//cached, 
			if(ncManager.isCached(remNod, latency,-1)){
				nodesPairSet.add(new NodesPair(Ninaloader.me, remNod, latency[0]
						));	
				continue;
			}else{
			//not cached
			barrier.fork();
			ncManager.doPing(remNod, new CB2<Double, Long>() {

				@Override
				protected void cb(CBResult result, Double lat, Long arg2) {
					// TODO Auto-generated method stub

					switch (result.state) {
					case OK: {
						nodesPairSet.add(new NodesPair(Ninaloader.me, remNod, lat
								.doubleValue()));
						System.out.println("$: Collect: doPing OK!");
						break;
					}
					case ERROR:
					case TIMEOUT: {
						String error = remNod.toString(false)
								+ "has not received responses, as: "
								+ result.toString();
						System.out.println(error);
						break;
					}
					}

					barrier.join();
				}
			});		
			}
		}

		//============================================================
		final long interval=3*1000;
		EL.get().registerTimerCB(barrier, interval,new CB0() {
			protected void cb(CBResult result) {
				String errorString;
				if (errorBuffer.length() == 0) {
					errorString = new String("Success");
				} else {
					errorString = new String(errorBuffer);
				}
				System.out.println("$@ @ Collect Completed");
				cbCoords.call(result, nodesPairSet, errorString);
			}
		});
	}
*/
	// After receiving the update request message from the core node,
	// the node which has been asked for update begins to collect rtts from
	// other nodes
	/*static void collectRTT(final AddressIF remNod,
			final Set<NodesPair> nodesPairSet, final Barrier barrier1,
			final StringBuffer errorBuffer) {



		
		 * ClustCollectRequestMsg msg = new
		 * ClustCollectRequestMsg(Ninaloader.me);
		 * 
		 * final int ind=nodesList.indexOf(remNod); sendStamp[ind] =
		 * System.nanoTime(); System.out.println("try to get RTT from "
		 * +Ninaloader.me+" to "+remNod );
		 * 
		 * barrier.fork(); comm.sendRequestMessage(msg, remNod, new
		 * ObjCommRRCB<ClustCollectResponseMsg>(){ protected void cb(CBResult
		 * result, final ClustCollectResponseMsg responseMsg, AddressIF node,
		 * Long ts) { double rtt = (System.nanoTime() - sendStamp[ind]) /
		 * 1000000d; switch (result.state) { case OK: {
		 * System.out.println("RTT of " + rtt +" from " +Ninaloader.me+" to " +
		 * remNod);
		 * 
		 * nodesPairSet.add(new NodesPair(AddressFactory.create(Ninaloader.me),
		 * AddressFactory.create(remNod), rtt)); break; } case ERROR: case
		 * TIMEOUT: { String error = "RTT request to " + remNod.toString(false)
		 * + " failed:"+ result.toString(); log.warn(error); if
		 * (errorBuffer.length() != 0) { errorBuffer.append(","); }
		 * errorBuffer.append(error); break; } } barrier.join(); } });
		 */

	
	

	// --------------------------------------------
	class HSHClient {

		final CB0 updateLandmarks;
		final CB0 updateCB;

		public HSHClient() {

			// update landmarks
			updateLandmarks = new CB0() {

				@Override
				protected void cb(CBResult result) {
					// TODO Auto-generated method stub
					updateLandmarks();
				}
			};

			// update RTTs of landmarks
			updateCB = new CB0() {
				protected void cb(CBResult result) {

					if (pendingHSHLandmarks.isEmpty()) {
						System.err.println("$: Update failed, empty landmarks");
						return;
					}

					Iterator<Long> ier = pendingHSHLandmarks.keySet()
							.iterator();

					Long curRecord = null;

					// get a non-empty list of landmarks
					while (ier.hasNext()) {
						Long nxt = ier.next();
						if (pendingHSHLandmarks.get(nxt).readyForUpdate
								&& pendingHSHLandmarks.get(nxt).IndexOfLandmarks != null
								&& pendingHSHLandmarks.get(nxt).IndexOfLandmarks
										.size() > 0) {
							curRecord = nxt;
							break;
						}
					}
					if (curRecord == null) {
						return;
					}

					System.out.println("$: HSH: The update process starts");
					final Set<AddressIF> nodes = new HashSet<AddressIF>(1);
					nodes
							.addAll(pendingHSHLandmarks.get(curRecord).IndexOfLandmarks);
					updateRTTs(curRecord.longValue(), nodes, new CB1<String>() {
						protected void cb(CBResult ncResult, String errorString) {
							switch (ncResult.state) {
							case OK: {
								System.out.println("$: Update completed");
								System.out.println();
							}
							case ERROR:
							case TIMEOUT: {
								break;
							}
							}
							nodes.clear();
						}
					});
				}
			};
		}

		// call updateRTTs() periodically
		void registerUpdateTimer() {
			double rnd = Ninaloader.random.nextGaussian();
			long delay = 3 * UPDATE_DELAY + (long) (50000 * rnd);
			log.debug("setting timer to " + delay);

			EL.get().registerTimerCB(delay, updateCB);
		}

		void registerUpdateLandmarksTimer() {
			double rnd = Ninaloader.random.nextGaussian();
			long delay = 5 * UPDATE_DELAY + (long) (50000 * rnd);
			log.debug("setting timer to " + delay);

			EL.get().registerTimerCB(delay, updateLandmarks);
		}

		public void init() {

			//registerUpdateTimer();
			registerUpdateLandmarksTimer();
		}

		/**
		 * we update the set of landmarks, after each time epoch
		 */
		void updateLandmarks() {

			registerUpdateLandmarksTimer();

			int m = 2;
			final int expectedLandmarks = NumberOfLandmarks + m;
			Iterator<Long> ier = pendingHSHLandmarks.keySet().iterator();

			Set<AddressIF> tmpList = new HashSet<AddressIF>(1);
			Long curRecord = null;

			// get a non-empty list of landmarks
			while (ier.hasNext()) {
				Long nxt = ier.next();
				if (pendingHSHLandmarks.get(nxt).IndexOfLandmarks != null
						&& pendingHSHLandmarks.get(nxt).IndexOfLandmarks.size() > 0) {
					tmpList
							.addAll(pendingHSHLandmarks.get(nxt).IndexOfLandmarks);
					curRecord = nxt;
					break;
				}
			}

			// if empty, we need to update landmarks immediately
			if (tmpList.size() == 0) {
				// remove the record
				if (curRecord != null) {
					pendingHSHLandmarks.get(curRecord).clear();
					pendingHSHLandmarks.remove(curRecord);
				} else {
					// null curRecord
				}

			} else {
				final Long Timer = curRecord;
				// ping landmarks, if several of them fails, p percentage p=0.2
				// , we remove the records, and restart the landmark process
				// =================================================================
				final List<AddressIF> aliveLandmarks = new ArrayList<AddressIF>(
						1);

				NNManager.collectRTTs(tmpList, new CB2<Set<NodesPair>, String>() {
					protected void cb(CBResult ncResult, Set<NodesPair> nps,
							String errorString) {
						// send data request message to the core node
						long timer=System.currentTimeMillis();
						
						if (nps != null && nps.size() > 0) {
							System.out
									.println("\n==================\n Alive No. of landmarks: "
											+ nps.size()
											+ "\n==================\n");

							Iterator<NodesPair> NP = nps.iterator();
							while (NP.hasNext()) {
								NodesPair tmp = NP.next();
								if (tmp != null && tmp.rtt >= 0) {

									AddressIF peer = (AddressIF)tmp.endNode;
									
									//====================================================
									if (!ncManager.pendingLatency.containsKey(peer)) {
										ncManager.pendingLatency.put(peer,
												new RemoteState<AddressIF>(peer));
									}
									ncManager.pendingLatency.get(peer).addSample(tmp.rtt, timer);
									//=====================================================
									
									if (!pendingHSHLandmarks.containsKey(Timer)
											|| pendingHSHLandmarks.get(Timer).IndexOfLandmarks == null) {
										continue;
									}

									int index = pendingHSHLandmarks.get(Timer).IndexOfLandmarks
											.indexOf(peer);

									if (index < 0) {

										continue;
									} else {

										// found the element, and it is smaller
										// than
										// rank, i.e., it is closer to the
										// target
										aliveLandmarks.add(peer);

										continue;
									}
								} else {
									// wrong measurements
								}
							}
						} else {
							// empty
							// all nodes fail, so there are no alive nodes
							if (pendingHSHLandmarks.containsKey(Timer)) {
								pendingHSHLandmarks.get(Timer).clear();
								pendingHSHLandmarks.remove(Timer);
							}
						}
						// some landmarks are offline, we clear records and
						// start
						if (pendingHSHLandmarks.containsKey(Timer)) {

							if (aliveLandmarks.size() < 0.8 * expectedLandmarks) {
								pendingHSHLandmarks.get(Timer).clear();
								pendingHSHLandmarks.remove(Timer);

							} else {
								// the landmarks are healthy, so we can sleep
								// awhile
								// TODO: remove dead landmarks, and resize the
								// landmarks
								pendingHSHLandmarks.get(Timer).IndexOfLandmarks
										.clear();
								pendingHSHLandmarks.get(Timer).IndexOfLandmarks
										.addAll(aliveLandmarks);

								// pendingHSHLandmarks.get(Timer).readyForUpdate=false;
								final Set<AddressIF> nodes = new HashSet<AddressIF>(
										1);

								nodes
										.addAll(pendingHSHLandmarks.get(Timer).IndexOfLandmarks);
								pendingHSHLandmarks.get(Timer).readyForUpdate = true;

								//update the rtts
								 updateRTTs(Timer.longValue(),nodes, new
								  CB1<String>(){ protected void cb(CBResult
								 ncResult, String errorString){ switch
								 (ncResult.state) { case OK: {
								 System.out.println("$: Update completed");
								 System.out.println();
								 if(errorString.length()<=0){
								 pendingHSHLandmarks
								 .get(Timer).readyForUpdate=true; }
								 
								 break; } case ERROR: case TIMEOUT: { break; }
								 } nodes.clear(); } });
								 

								return;
							}
						}
					}
				});

			}

			// ==================================================================

			// expected landmarks, K+m

			final Set<AddressIF> pendingNodes = new HashSet<AddressIF>(1);

			getRandomNodes(pendingNodes, expectedLandmarks);

			// remove myself
			if (pendingNodes.contains(Ninaloader.me)) {
				pendingNodes.remove(Ninaloader.me);
			}

			System.out.println("$: HSH: Total number of landmarks are: "
					+ pendingNodes.size());

			if (pendingNodes.size() == 0) {
				String errorString = "$: HSH no valid nodes";
				System.out.println(errorString);
				return;
			}
			Barrier barrierUpdate = new Barrier(true);
			final StringBuffer errorBuffer = new StringBuffer();

			final long TimeStamp = System.currentTimeMillis();

			for (AddressIF addr : pendingNodes) {
				seekLandmarks(TimeStamp, addr, barrierUpdate,
						pendingHSHLandmarks, errorBuffer);
			}

			EL.get().registerTimerCB(barrierUpdate, new CB0() {
				protected void cb(CBResult result) {
					String errorString;
					if (errorBuffer.length() == 0) {
						errorString = new String("Success");
					} else {
						errorString = new String(errorBuffer);
					}

					// finish the landmark seeking process

					if (!pendingHSHLandmarks.containsKey(Long
							.valueOf(TimeStamp))
							|| pendingHSHLandmarks.get(Long.valueOf(TimeStamp)).IndexOfLandmarks == null) {
						System.out.println("$: NULL elements! ");
						return;
					}
					if (pendingHSHLandmarks.get(Long.valueOf(TimeStamp)).IndexOfLandmarks
							.size() < (0.7 * expectedLandmarks)) {
						pendingHSHLandmarks.get(Long.valueOf(TimeStamp))
								.clear();
						pendingHSHLandmarks.remove(Long.valueOf(TimeStamp));
						System.out.println("$: not enough landmark nodes");
					} else {

						//
						pendingHSHLandmarks.get(Long.valueOf(TimeStamp)).readyForUpdate = true;
						System.out.println("$: enough landmark nodes");
						
						final Set<AddressIF> nodes = new HashSet<AddressIF>(1);
						nodes
								.addAll(pendingHSHLandmarks.get(Long.valueOf(TimeStamp)).IndexOfLandmarks);
						updateRTTs(Long.valueOf(TimeStamp), nodes, new CB1<String>() {
							protected void cb(CBResult ncResult, String errorString) {
								switch (ncResult.state) {
								case OK: {
									System.out.println("$: Update completed");
									System.out.println();
								}
								case ERROR:
								case TIMEOUT: {
									break;
								}
								}
								nodes.clear();
							}
						});
					}

				}
			});
		}

		/**
		 * seek landmarks from pending neighbors
		 * 
		 * @param timeStamp
		 * @param addr
		 * @param barrier
		 * @param pendingHSHLandmarks
		 * @param errorBuffer
		 */
		private void seekLandmarks(final long timeStamp, final AddressIF addr,
				final Barrier barrier,
				final Map<Long, HSHRecord> pendingHSHLandmarks,
				StringBuffer errorBuffer) {
			// TODO Auto-generated method stub
			if (Ninaloader.me.equals(addr)) {
				return;
			}

			LandmarkRequestMsg msg = new LandmarkRequestMsg(Ninaloader.me,
					timeStamp);
			barrier.fork();
			comm.sendRequestMessage(msg, addr,
					new ObjCommRRCB<LandmarkResponseMsg>() {
						protected void cb(CBResult result,
								final LandmarkResponseMsg responseMsg,
								AddressIF node, Long ts) {
							switch (result.state) {
							case OK: {
								// System.out.println("$Landmark Request is acked! We have received ACK from"+addr);

								// we override previous landmarks
								if (!pendingHSHLandmarks.containsKey(Long
										.valueOf(timeStamp))) {
									pendingHSHLandmarks.put(Long
											.valueOf(timeStamp), new HSHRecord(
											Long.valueOf(timeStamp)));
								}
								pendingHSHLandmarks
										.get(Long.valueOf(timeStamp))
										.addLandmarks(addr);
								pendingHSHLandmarks
										.get(Long.valueOf(timeStamp)).alreadyComputedClustering = false;
								break;
							}
							case ERROR:
							case TIMEOUT: {
								String error = addr.toString(false)
										+ "has not received requirement, as: "
										+ result.toString();
								log.warn(error);

								// remove pending neighbor
								// addDownNeighbor(addr);
								break;
							}
							}
							barrier.join();
						}
					});

		}

		void updateRTTs(final long timeStamp,
				final Set<AddressIF> LandmarkList, final CB1<String> cbCoords) {

			registerUpdateTimer();

			if (LandmarkList.size() == 0) {
				String errorString = "getRemoteCoords: no valid nodes";
				System.out.println(errorString);
				cbCoords.call(CBResult.OK(), errorString);
				return;
			}

			Barrier barrierUpdate = new Barrier(true);
			final StringBuffer errorBuffer = new StringBuffer();

			for (AddressIF addr : LandmarkList) {
				updateRTT(timeStamp, addr, LandmarkList, barrierUpdate,
						errorBuffer);
			}

			EL.get().registerTimerCB(barrierUpdate, new CB0() {
				protected void cb(CBResult result) {
					String errorString;
					if (errorBuffer.length() == 0) {
						errorString = new String("Success");
					} else {
						errorString = new String(errorBuffer);
					}

					cbCoords.call(CBResult.OK(), errorString);
				}
			});
		}

		// send the update request
		void updateRTT(long timeStamp, final AddressIF remNode,
				Set<AddressIF> landmarkList, final Barrier barrier,
				final StringBuffer errorBuffer) {

			if (Ninaloader.me.equals(remNode))
				return;

			ClustUpdateRequestMsg msg = new ClustUpdateRequestMsg(
					Ninaloader.me, landmarkList, timeStamp);
			// System.out.println("ask " + remNode + " to update rtt");

			barrier.fork();
			comm.sendRequestMessage(msg, remNode,
					new ObjCommRRCB<ClustUpdateResponseMsg>() {
						protected void cb(CBResult result,
								final ClustUpdateResponseMsg responseMsg,
								AddressIF node, Long ts) {
							switch (result.state) {
							case OK: {
								System.out
										.println("$ ! We have received ACK from"
												+ responseMsg.from);
								break;
							}
							case ERROR:
							case TIMEOUT: {
								String error = remNode.toString(false)
										+ "has not received requirement, as: "
										+ result.toString();
								System.out.println(error);
								errorBuffer.append(error);
								break;
							}
							}
							barrier.join();
						}
					});
		}

		// latency information from all other nodes has been received by core
		// node and recorded in its Set<NodesPair>
		// TODO: update latency
		void updateDistMatrix(long timeStamp) {

			// for each alive landmark, when its ping results are received by
			// the source node,
			// we start the clustering computation process
			// TODO: remove landmarks that return a subset of results 10-12
			Long timer = Long.valueOf(timeStamp);

			if (!pendingHSHLandmarks.containsKey(timer)
					|| pendingHSHLandmarks.get(timer).IndexOfLandmarks == null) {
				System.err.println("$: Null elements!");
				return;
			}

			HSHRecord CurList = pendingHSHLandmarks.get(timer);
			int NoOfLandmarks = CurList.IndexOfLandmarks.size();
			if(NoOfLandmarks<=0){
				System.err.println("$: Empty elements!");
				return;
			}

			HashMap<AddressIF, ArrayList<RemoteState<AddressIF>>> LatRecords = CurList.pendingLandmarkLatency;
			if (LatRecords == null) {
				System.err
						.println("$: Null HashMap<AddressIF, ArrayList<RemoteState<AddressIF>>> LatRecords!");
				return;
			}

			int receivedLandmarks = LatRecords.keySet().size();
			// empty or incomplete records
			if (LatRecords.isEmpty() || receivedLandmarks < 0.8 * NoOfLandmarks) {
				System.err.println("$: Null LatRecords!");
				return;
			}

			System.out.println("$: HSH: total landmarks: " + NoOfLandmarks
					+ ", received from " + receivedLandmarks);
			// use the IndexOfLandmarks as the index of nodes

			if (CurList.DistanceMatrix == null
					|| CurList.DistanceMatrix.length != NoOfLandmarks) {
				CurList.DistanceMatrix = null;
				CurList.DistanceMatrix = new double[NoOfLandmarks][NoOfLandmarks];
				for (int i = 0; i < NoOfLandmarks; i++) {
					for (int j = 0; j < NoOfLandmarks; j++) {
						CurList.DistanceMatrix[i][j] = 0;
					}
				}
			}

			// fill elements
			Iterator<Entry<AddressIF, ArrayList<RemoteState<AddressIF>>>> entrySet = LatRecords
					.entrySet().iterator();

			while (entrySet.hasNext()) {
				Entry<AddressIF, ArrayList<RemoteState<AddressIF>>> tmp = entrySet
						.next();
				// empty lists
				if (tmp.getValue() == null || tmp.getValue().size() == 0) {
					continue;
				}
				// ===============================================================
				AddressIF srcNode = tmp.getKey();
				// find the index of landmarks
				int from = CurList.IndexOfLandmarks.indexOf(srcNode);
				Iterator<RemoteState<AddressIF>> ier = tmp.getValue()
						.iterator();
				if (from < 0) {
					// already removed landmarks!
					System.out.println("already removed landmarks!");
					continue;
				}
				while (ier.hasNext()) {
					RemoteState<AddressIF> NP = ier.next();

					if (!CurList.IndexOfLandmarks.contains(NP.getAddress())) {
						// not landmarks
						continue;
					}
					int to = CurList.IndexOfLandmarks.indexOf(NP.getAddress());

					double rtt = NP.getSample();
					if (to < 0 || rtt < 0) {
						System.err.println("$: failed to find the landmarks!");
						continue;
					} else {
						// System.out.print(" <"+to+", "+from+", "+rtt+"> ");
						// Note: symmetric RTT
						if (CurList.DistanceMatrix[to][from] > 0) {
							double avgRTT;
							if (rtt > 0) {
								avgRTT = (rtt + CurList.DistanceMatrix[to][from]) / 2;

							} else {
								avgRTT = CurList.DistanceMatrix[to][from];
							}
							CurList.DistanceMatrix[from][to] = avgRTT;
							CurList.DistanceMatrix[to][from] = avgRTT;
						} else {
							// TODO: missing elements
							CurList.DistanceMatrix[from][to] = rtt;
							CurList.DistanceMatrix[to][from] = rtt;
						}

					}
				}
			}

			// ======================================================

			boolean markZero = false;
			for (int i = 0; i < NoOfLandmarks; i++) {
				int sum = 0;
				for (int column = 0; column < NoOfLandmarks; column++) {
					if (i == column) {
						continue;
					}
					if (CurList.DistanceMatrix[i][column] > 0) {
						sum++;
					}
				}

				if (sum < 0.7 * NoOfLandmarks) {
					markZero = true;
					break;
				}
			}
			if (markZero) {
				// CurList.DistanceMatrix=null;
				System.err.println("$: incomplete CurList.DistanceMatrix!");
				return;
			}

			System.out
					.println("\n\n$: HSH: Start HSH clustering computation process!");

			// TODO: find accurate clustering number
			int cluNum = MatrixOps.findClusteringNum(CurList.DistanceMatrix);

			if(cluNum<=0){
				System.err.println("the clustering number is <=0");
				return;
			}
				
				
			// version control for dimensions
			if (CurList.S != null && CurList.S.length == cluNum) {

			} else {
				CurList.version = System.currentTimeMillis();
			}

			// initialize the matrixes for computation
			CurList.H = new double[NoOfLandmarks][cluNum];
			CurList.S = new double[cluNum][cluNum];
			// CurList.Coord = new double[cluNum];
			for (int i = 0; i < NoOfLandmarks; i++) {
				for (int j = 0; j < cluNum; j++) {
					CurList.H[i][j] = 0;
				}
			}
			for (int i = 0; i < cluNum; i++) {
				for (int j = 0; j < cluNum; j++) {
					CurList.S[i][j] = 0;
				}
			}

			// update coordinate vectors
			MatrixOps.symmetric_NMF(NoOfLandmarks, cluNum,
					CurList.DistanceMatrix, CurList.H, CurList.S);
			CurList.alreadyComputedClustering = true;

			// TODO: H,S is Null
			if (CurList.H == null || CurList.S == null) {
				System.err.println("$: after HSH computation, NULL results!");
				return;
			}

			// after centralized computation, the core node should send its H &
			// S to all the landmark nodes
			issueCoords(timeStamp, CurList.IndexOfLandmarks, new CB1<String>() {
				protected void cb(CBResult ncResult, String errorString) {
					System.out
							.println("Core node has issued its H & S to all landmarks.");
				}
			});
		}
		//
	}

	// --------------------------------------------
	class HostClient {
		final CB0 releaseCB;

		public HostClient() {
			releaseCB = new CB0() {
				protected void cb(CBResult result) {
					askRelease(UpdateSource, new CB1<String>() {
						protected void cb(CBResult cbResult, String errorString) {
							switch (cbResult.state) {
							case OK: {
								System.out
										.println("$: ask for release completed");
								break;
							}
							case ERROR:
							case TIMEOUT: {
								System.err.println("$: ask for release failed");
								break;
							}
							}
						}
					});
				}
			};
		}

		// call askRelease() periodically
		void registerTimer() {
			double rnd = Ninaloader.random.nextGaussian();// What is the type of
															// Ninaloader?
			long delay = 10 * UPDATE_DELAY + (long) (50000 * rnd);
			log.debug("setting timer to " + delay);

			EL.get().registerTimerCB(delay, releaseCB);
		}

		public void init() {
			registerTimer();
		}

		void askRelease(final AddressIF coreNode, final CB1<String> cbCoords) {
			registerTimer();

			ClustReleaseRequestMsg msg = new ClustReleaseRequestMsg(
					Ninaloader.me);
			System.out.println("ask " + coreNode + " to release H & S");

			comm.sendRequestMessage(msg, coreNode,
					new ObjCommRRCB<ClustReleaseResponseMsg>() {
						protected void cb(CBResult result,
								final ClustReleaseResponseMsg responseMsg,
								AddressIF node, Long ts) {
							switch (result.state) {
							case OK: {
								// the host node records the release response
								// message
								// hostNode._H <- coreNode.responseMsg.H
								// hostNode._S <- coreNode.responseMsg.S

								if (responseMsg.H == null
										|| responseMsg.S == null) {
									System.out
											.println("Failed due to empty H or S");
									cbCoords.call(CBResult.ERROR(),
											"Failed due to empty H or S");
									break;
								} else {
									// else
									final int NODES_NUM = responseMsg.H.length;
									final int NC_NUM_DIMS = responseMsg.S.length;

									final Matrix_4_HSH _H = new Matrix_4_HSH(NODES_NUM,
											NC_NUM_DIMS);
									final Matrix_4_HSH _S = new Matrix_4_HSH(NC_NUM_DIMS,
											NC_NUM_DIMS);
									H = new double[NODES_NUM][NC_NUM_DIMS];
									S = new double[NC_NUM_DIMS][NC_NUM_DIMS];
									// Use IsReady to judge if the H & S have
									// already been computed.
									boolean IsReady = true;
									if (responseMsg.H == null
											|| responseMsg.S == null) {
										//
										IsReady = false;
									}

									if (IsReady) {
										for (int i = 0; i < NODES_NUM; i++) {
											double sum = 0;
											for (int j = 0; j < NC_NUM_DIMS; j++) {
												sum += responseMsg.H[i][j];
												_H.mat[i][j] = responseMsg.H[i][j];
												H[i][j] = responseMsg.H[i][j];
											}
											if (sum < 0.0002) {
												IsReady = false;
												break;
											}
										}

										if (IsReady) {

											for (int i = 0; i < NC_NUM_DIMS; i++) {
												double sum = 0;
												for (int j = 0; j < NC_NUM_DIMS; j++) {
													sum += responseMsg.S[i][j];
													_S.mat[i][j] = responseMsg.S[i][j];
													S[i][j] = responseMsg.S[i][j];
												}
												if (sum < 0.0002) {
													IsReady = false;
													break;
												}
											}
										}
									}
									if (!IsReady) {
										System.out
												.println("The core node's H & S are not ready!");
										cbCoords
												.call(CBResult.ERROR(), "error");
										return;
									}

									System.out
											.println("$ ! ReleaseResponse from"
													+ responseMsg.from);

									// Send collect message to all the landmark
									// nodes.
									// Record the rtts and call the D_N_S_NMF()
									// to compute the Coord
									final Set<AddressIF> nodes = new HashSet<AddressIF>(
											1);
									nodes.addAll(responseMsg.landmarks);

									NNManager.collectRTTs(nodes,
											new CB2<Set<NodesPair>, String>() {
												protected void cb(
														CBResult ncResult,
														Set<NodesPair> nps,
														String errorString) {

													// A host to all the
													// landmarks, it's a row
													// vector
													Matrix_4_HSH host2Landmark = new Matrix_4_HSH(
															1, NODES_NUM);
													// After call
													// D_N_S_NMF(host2Landmark),
													// the result returned as
													// coord
													Matrix_4_HSH coord = new Matrix_4_HSH(
															1, NC_NUM_DIMS);
													Iterator ier = nps
															.iterator();
													while (ier.hasNext()) {
														NodesPair tmp = (NodesPair) ier
																.next();
														int to = responseMsg.landmarks
																.indexOf(tmp.endNode);
														if (to < 0) {
															continue;
														} else {
															host2Landmark.mat[0][to] = tmp.rtt;
														}
													}
													coord = MatrixOps
															.D_N_S_NMF(
																	host2Landmark,
																	_H, _S);

													// initialize my coordinate
													Coord = new double[NC_NUM_DIMS];
													// record the coordinate in
													// Coord
													for (int i = 0; i < NC_NUM_DIMS; i++)
														Coord[i] = coord.mat[0][i];

													versionNumber = responseMsg.version;
													try {
														logHSHClusteringResults();
													} catch (IOException e) {
														// TODO Auto-generated
														// catch block
														e.printStackTrace();
													}

													nodes.clear();
												}
											});
									cbCoords.call(CBResult.OK(), "Success");
								}

								break;
							}
							case ERROR:
							case TIMEOUT: {
								String error = coreNode.toString(false)
										+ "has not received requirement, as: "
										+ result.toString();
								log.warn(error);
								cbCoords.call(CBResult.OK(), error);
							}
							}
						}
					});
		}

	}

	// If this guy is in an unknown state add him to pending.
	/*
	 * void addPendingNeighbor(AddressIF node) {
	 * ncManager.addPendingNeighbor(node);
	 * 
	 * }
	 */

	// down nodes
	/*
	 * void addDownNeighbor(AddressIF node) { ncManager.addDownNeighbor(node);
	 * 
	 * }
	 */

	// up neighbors
	void addUpNeighbor(AddressIF node) {
		ncManager.addUpNeighbor(node);
	}

	public int getRandomNodes(Set<AddressIF> randNodes, int size) {

		return NNManager.MClient.instance.g_rings.getRandomNodes(randNodes,
				size);

	}

	static AddressIF II;

	public static void main(String[] args) {

		EL.set(new EL(0, false));

		ANSI.use(true);

		EL.get().registerTimerCB(new CB0() {
			protected void cb(CBResult result) {

				log.debug("Resolving bootstrap list");
				final Set<AddressIF> bootst = new HashSet<AddressIF>();

				int port = 55504;

				AddressFactory.createResolved("Ericfu.pyxida.pdl",
						new CB1<AddressIF>() {
							protected void cb(CBResult result, AddressIF addrMap) {
								switch (result.state) {
								case OK: {
									II = addrMap;
									System.out.println("me='" + II);
									break;

								}
								case TIMEOUT:
								case ERROR: {
									log.error("Could not resolve me "
											+ result.what);
									break;
								}
								}
							}

						});

				final ArrayList<AddressIF> nodesList = new ArrayList<AddressIF>(
						10);
				nodesList.ensureCapacity(4);

				final List<String> addrList = new ArrayList<String>();

				String[] oneAddr = { "Ericfu.pyxida.pdl", "r1d15.pyxida.pdl",
						"r1d14.pyxida.pdl", "r2d16a.pyxida.pdl" };
				for (String a : oneAddr)
					addrList.add(a);

				final int totalNum = addrList.size();

				log.main("addrList.size=" + totalNum);

				AddressFactory.createResolved(addrList, port,
						new CB1<Map<String, AddressIF>>() {
							protected void cb(CBResult result,
									Map<String, AddressIF> addrMap) {
								switch (result.state) {
								case OK: {
									for (String node33 : addrMap.keySet()) {

										AddressIF remoteAddr = addrMap
												.get(node33);
										System.out.println("remoteNode='"
												+ node33 + "'" + remoteAddr);
										// we keep these around in case we run
										// out of neighbors in the future
										bootst.add(remoteAddr);
										if (!remoteAddr.equals(II)) {
											nodesList.add(remoteAddr); // add to
																		// potential
																		// nodes
																		// for
																		// measurement
										}

									}
									System.out.println("#################");
									for (AddressIF ier : nodesList) {
										System.out.println(" " + ier);
									}
								}
								}
							}

						});
			}

		});
		EL.get().main();
	}

	public  Matrix_4_HSH getRelativeCoordinates(AddressIF currentNode,Collection<AddressIF > allNodes,
			Collection<AddressIF > landmarks, double[] lat) {
		// TODO Auto-generated method stub
		return null;
	}

	public void doInitialClustering(AddressIF me, Matrix_4_HSH tmp) {
		// TODO Auto-generated method stub
		
	}
	

}
