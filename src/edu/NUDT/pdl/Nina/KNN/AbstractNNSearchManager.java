package edu.NUDT.pdl.Nina.KNN;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import edu.NUDT.pdl.Nina.MeasureComm;
import edu.NUDT.pdl.Nina.Ninaloader;
import edu.NUDT.pdl.Nina.Clustering.ClusteringSet;
import edu.NUDT.pdl.Nina.Clustering.HSH_Manager;
import edu.NUDT.pdl.Nina.Clustering.Matrix_4_HSH;
import edu.NUDT.pdl.Nina.Clustering.RecurRelaBasedHSH_Manager;
import edu.NUDT.pdl.Nina.Clustering.clusteringRecord;
import edu.NUDT.pdl.Nina.Distance.NodesPairComp;
import edu.NUDT.pdl.Nina.KNN.askcachedHistoricalNeighbors.askcachedHistoricalNeighborsRequestMsg;
import edu.NUDT.pdl.Nina.KNN.askcachedHistoricalNeighbors.askcachedHistoricalNeighborsResponseMsg;
import edu.NUDT.pdl.Nina.KNN.multiObjKNN.MultiTargetKNNManager;

import edu.NUDT.pdl.Nina.StableNC.lib.Coordinate;
import edu.NUDT.pdl.Nina.StableNC.lib.EWMAStatistic;
import edu.NUDT.pdl.Nina.StableNC.lib.NCClient;
import edu.NUDT.pdl.Nina.StableNC.lib.RemoteState;
import edu.NUDT.pdl.Nina.StableNC.lib.WindowStatistic;
import edu.NUDT.pdl.Nina.StableNC.nc.CoordGossipRequestMsg;
import edu.NUDT.pdl.Nina.StableNC.nc.CoordGossipResponseMsg;
import edu.NUDT.pdl.Nina.StableNC.nc.StableManager;
import edu.NUDT.pdl.Nina.util.HashSetCache;
import edu.NUDT.pdl.Nina.util.HistoryNearestNeighbors;
import edu.NUDT.pdl.Nina.util.MainGeneric;
import edu.NUDT.pdl.Nina.util.Matrix;
import edu.NUDT.pdl.pyxida.ping.PingManager;
import edu.harvard.syrah.prp.Log;
import edu.harvard.syrah.prp.NetUtil;
import edu.harvard.syrah.prp.POut;
import edu.harvard.syrah.prp.PUtil;
import edu.harvard.syrah.prp.RateCalc;
import edu.harvard.syrah.prp.SortedList;
import edu.harvard.syrah.sbon.async.Barrier;
import edu.harvard.syrah.sbon.async.CBResult;
import edu.harvard.syrah.sbon.async.Config;
import edu.harvard.syrah.sbon.async.EL;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB0;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB1;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB2;
import edu.harvard.syrah.sbon.async.EL.Priority;
import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.NetAddress;
import edu.harvard.syrah.sbon.async.comm.obj.ObjCommCB;
import edu.harvard.syrah.sbon.async.comm.obj.ObjCommIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjCommRRCB;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessageIF;

public abstract class AbstractNNSearchManager {

	public static final Log log = new Log(AbstractNNSearchManager.class);

	public static boolean reverseMeasurement = false;
	/**
	 * Time between gossip messages to coordinate neighbors. Default is 60
	 * seconds.
	 */
	// public static final long UPDATE_DELAY = 5*60*1000;
	public static final long RingUPDATE_DELAY = Integer
			.parseInt(Config.getConfigProps().getProperty("RingUPDATE_DELAY", "300000"));

	// public static final long veryLongUPDATE_DELAY =40*60*1000;
	public static final long veryLongUPDATE_DELAY = Integer
			.parseInt(Config.getConfigProps().getProperty("veryLongUPDATE_DELAY", "300000"));

	public static final long randomPing_DELAY = Integer
			.parseInt(Config.getConfigProps().getProperty("randomPing_DELAY", "300000"));
	// loss measurement
	public static final int PING_DELAY = Integer.parseInt(Config.getConfigProps().getProperty("PING_DELAY", "100"));

	// measurement period
	public static final int doOneWayLossMeasurementPeriod = Integer
			.parseInt(Config.getConfigProps().getProperty("doOneWayLossMeasurementPeriod", "60000"));

	// total pings
	public static final int PINGCounter = Integer.parseInt(Config.getConfigProps().getProperty("PINGCounter", "100"));
	// packetsize
	public static final int packetsize = Integer.parseInt(Config.getConfigProps().getProperty("packetsize", "100"));

	public static ObjCommIF comm;
	public static ObjCommIF measureComm;
	public static ObjCommIF gossipComm;

	public final PingManager pingManager;

	// protected final ReentrantLock lock = new ReentrantLock();
	// beta ratio
	public double betaRatio = Double.parseDouble(Config.getConfigProps().getProperty("betaRatio", "0.5"));

	public double betaRatioFarthest = Double
			.parseDouble(Config.getConfigProps().getProperty("betaRatioFarthest", "0.5"));

	// pending closest queries
	// Map<AddressIF, RemoteState<AddressIF>> pendingClosestQuery;
	// Map<AddressIF, AddressIF> closestMappings;

	// bootstrap
	String bootstrapList[] = Config.getProperty("bootstraplist", "r1d15.pyxida.pdl").split("[\\s]");
	Set<AddressIF> bootstrapAddrs;

	String RelativeClusteringLandmarksList[] = Config.getProperty("RelativeClusteringLandmarksList", "r1d15.pyxida.pdl")
			.split("[\\s]");
	Set<AddressIF> RelativeClusteringLandmarksAddrs;

	public static final boolean WATCH_NEIGHBORS = Boolean
			.parseBoolean(Config.getConfigProps().getProperty("watch_neighbors", "false"));

	public static final int NumberOfK = Integer.parseInt(Config.getConfigProps().getProperty("NumberOfK", "5"));

	public static final int RingMaintainSelection = Integer
			.parseInt(Config.getConfigProps().getProperty("RingMaintainSelection", "1"));

	public static final int ClusteringSelection = Integer
			.parseInt(Config.getConfigProps().getProperty("ClusteringSelection", "0"));

	public static final boolean UseRAWPings = Boolean
			.parseBoolean(Config.getConfigProps().getProperty("UseRAWPings", "false"));

	public static final boolean IsNonRingNode = Boolean
			.parseBoolean(Config.getConfigProps().getProperty("IsNonRingNode", "false"));

	/**
	 * single target KNN
	 */
	public static final boolean singleTargetKNN = Boolean
			.parseBoolean(Config.getConfigProps().getProperty("singleTargetKNN", "false"));

	public static final boolean useOneWayLoss = Boolean
			.parseBoolean(Config.getConfigProps().getProperty("useOneWayLoss", "false"));

	public static int defaultNumOfDetourTargets = Integer
			.parseInt(Config.getConfigProps().getProperty("defaultNumOfDetourTargets", "2"));

	public static int defaultGossip = Integer.parseInt(Config.getConfigProps().getProperty("defaultGossip", "10"));

	// add the offset
	public static double offsetLatency = Double.parseDouble(Config.getConfigProps().getProperty("offsetLatency", "0")); // act
																														// as
																														// the
																														// item
																														// that
																														// is
																														// added
																														// to
																														// each
																														// measurements

	public static int nodesPerRing = Integer.parseInt(Config.getConfigProps().getProperty("nodesPerRing", "30"));

	// all nodes
	public static final String AllNodes = Config.getProperty("AllNodes", "nodeList4Nina.src");

	// ===============================================================================
	// public List<AddressIF> AllAliveNodes=new ArrayList<AddressIF>(2);
	public List<String> AllAlivecachedNodeStrings = new ArrayList<String>(2);

	// Map<AddressIF, Vector<CBcached>> cachedCB = new HashMap<AddressIF,
	// Vector<CBcached>>(10);
	protected Map<Long, CBcached> cachedCB = new ConcurrentHashMap<Long, CBcached>(10);

	boolean ClusteringSupport = false;

	// to record the callbacks of a target

	static Map<AddressIF, Vector<record>> cachedTargetProbes = new ConcurrentHashMap<AddressIF, Vector<record>>(10);

	// Set<AddressIF> upNeighbors;
	// Set<AddressIF> downNeighbors;
	public static Map<AddressIF, RemoteState<AddressIF>> pendingNeighbors = new ConcurrentHashMap<AddressIF, RemoteState<AddressIF>>(
			10);

	// for target
	public Set<AddressIF> targetCandidates = new HashSet<AddressIF>(2);

	// public Map<AddressIF, RemoteState<AddressIF>> pendingTargets;
	// to cache the pending probes to the target
	//
	public static boolean useRandomSeedsInSearching = Boolean
			.parseBoolean(Config.getConfigProps().getProperty("useRandomSeedsInSearching", "true"));

	/**
	 * the the coordinate
	 */
	public static boolean useNormalRingRange = Boolean
			.parseBoolean(Config.getConfigProps().getProperty("useNormalRingRange", "true"));

	public static boolean useAdaptiveSearch = Boolean
			.parseBoolean(Config.getConfigProps().getProperty("useAdaptiveSearch", "false"));

	public static float betaCutoff = Float.parseFloat(Config.getConfigProps().getProperty("betaCutoff", "0.8"));

	public static float FarthestBetaCutoff = Float
			.parseFloat(Config.getConfigProps().getProperty("FarthestBetaCutoff", "1.1"));

	public static int maxPendingNeighbors = Integer
			.parseInt(Config.getConfigProps().getProperty("maxPendingNeighbors", "50"));

	public static Map<Long, PendingRecord<AddressIF>> PendingPingRecordNodes = new ConcurrentHashMap<Long, PendingRecord<AddressIF>>(
			10);

	public static boolean startMeridian = Boolean
			.parseBoolean(Config.getConfigProps().getProperty("startMeridian", "false"));

	/**
	 * nodes in each ring
	 */
	public static int MeridianNodesPerRing = Integer
			.parseInt(Config.getConfigProps().getProperty("MeridianNodesPerRing", "30"));

	public static int allowedRepeated = Integer.parseInt(Config.getConfigProps().getProperty("allowedRepeated", "8"));

	/**
	 * beta threshold of Meridian
	 */
	public double betaRatioMeridian = Double
			.parseDouble(Config.getConfigProps().getProperty("betaRatioMeridian", "0.5"));

	public static int _binDim = Integer.parseInt(Config.getConfigProps().getProperty("binDim", "10"));

	public static int _cutoff = Integer.parseInt(Config.getConfigProps().getProperty("cutoff", "2"));
	public static int _listThreshold = Integer.parseInt(Config.getConfigProps().getProperty("listThreshold", "1"));
	public static int _choiceOfNextHop = Integer.parseInt(Config.getConfigProps().getProperty("choiceOfNextHop", "3"));
	public static float _RHO_INFRAMETRIC = Float
			.parseFloat(Config.getConfigProps().getProperty("RHO_INFRAMETRIC", "2.5"));

	public static int defaultNodesForRandomContact = Integer
			.parseInt(Config.getConfigProps().getProperty("defaultNodesForRandomContact", "5"));

	public static final int defaultDirectPing = Integer
			.parseInt(Config.getConfigProps().getProperty("defaultDirectPing", "52"));

	public static int defaultNonEmptyRingsForKNN = Integer
			.parseInt(Config.getConfigProps().getProperty("defaultNonEmptyRingsForKNN", "3"));

	/**
	 * scale down the ring set
	 */
	public static final int scalarFactor = Integer.parseInt(Config.getConfigProps().getProperty("scalarFactor", "5"));

	// in millisecond
	public static float timeoutTargetProbe = Float
			.parseFloat(Config.getConfigProps().getProperty("timeoutTargetProbe", "20000"));

	/**
	 * returned for next hop
	 */
	public static int returnedNodesForNextHop = Integer
			.parseInt(Config.getConfigProps().getProperty("returnedNodesForNextHop", "5"));

	public static int myNCDimension = Integer.parseInt(Config.getConfigProps().getProperty("myNCDimension", "6"));

	public static int warmPeriod = Integer.parseInt(Config.getConfigProps().getProperty("warmPeriod", "500"));

	public volatile int warmCounter = 0;

	public static int cachedHistoryKNN = Integer
			.parseInt(Config.getConfigProps().getProperty("cachedHistoryKNN", "200"));

	// record timestamp records
	public static int MAX_SAMPLE_SIZE = 16;
	protected static double SAMPLE_PERCENTILE = 0.5;
	// protected EWMAStatistic ElapsedKNNSearchTime;

	public static int logControl_DELAY = 120000;

	boolean isQueryNode = Boolean.parseBoolean(Config.getConfigProps().getProperty("isQueryNode", "false"));;

	/**
	 * record the success rate
	 */
	protected EWMAStatistic SuccessRateForSingleTargetKNN;
	protected EWMAStatistic SuccessRateForMultiTargetKNN;
	protected WindowStatistic recent1NearestNeighbors;
	protected WindowStatistic recent5NearestNeighbors;
	protected WindowStatistic recent10NearestNeighbors;
	protected WindowStatistic recent15NearestNeighbors;
	protected WindowStatistic recent20NearestNeighbors;
	protected WindowStatistic recent30NearestNeighbors;

	protected EWMAStatistic KNNAbsoluteError;
	protected EWMAStatistic KNNRelativeError;
	protected EWMAStatistic KNNcoverage;
	EWMAStatistic KNNClosestNodes;
	protected EWMAStatistic KNNGains;

	public static int hasResendCouter = Integer.parseInt(Config.getConfigProps().getProperty("hasResendCouter", "3"));

	int WindowRange = 30;

	protected EWMAStatistic nonEmptyRingNumber;

	// completely ignore any RTT larger than ten seconds
	public static double OUTRAGEOUSLY_LARGE_RTT = 20000.0;

	// to record the registered Nearest Nodes
	public class record {
		public AddressIF OriginFrom;
		public int version;
		public int seed;
		public NodesPair np;
		public AddressIF from;
		public AddressIF target;
		public long timeStamp;
		public double rtt;

		public record(AddressIF _from, AddressIF _target, double _rtt, long _timeStamp) {

			from = _from;
			target = _target;
			rtt = _rtt;
			timeStamp = _timeStamp;

		}

		//
		public record(AddressIF _OriginFrom, int _version, int _seed, NodesPair _np) {
			OriginFrom = _OriginFrom;
			version = _version;
			seed = _seed;
			np = _np;

		}

		protected Object clone() throws CloneNotSupportedException {
			// TODO Auto-generated method stub
			return new record(from, target, rtt, timeStamp);
		}

		public boolean equals(Object obj) {
			// TODO Auto-generated method stub
			record r = (record) obj;
			if (this.from.equals(r.from) && this.target.equals(r.target) && this.timeStamp == r.timeStamp
					&& Math.abs(this.rtt - r.rtt) < 0.002) {
				return true;
			} else {
				return false;
			}
		}

	}

	// maximum pending periods for pending target probes
	public static long maxPendingMs = 1000 * 10;

	/*
	 * protected Map<AddressIF, Vector<backtracking>> cachedBackTrack = new
	 * HashMap<AddressIF, Vector<backtracking>>( 10);
	 */

	public static StableManager ncManager; // interface to NC Manager

	// public MeridianSearchManager meridianManager;

	// for backtrack process
	public class backtracking {
		public AddressIF OriginFrom; // original node
		public AddressIF target;
		public int version;
		public final int seed;
		public AddressIF lastHop;
		// public Vector<NodesPair> registeredNN;
		public final AddressIF root;
		public final long timer;

		public backtracking(AddressIF _OriginFrom, AddressIF _target, int _version, AddressIF _lastHop, int _seed,
				AddressIF _root, long _timer) {
			OriginFrom = _OriginFrom;
			target = _target;
			version = _version;
			lastHop = _lastHop;
			seed = _seed;
			// registeredNN = new Vector<NodesPair>(10);
			timer = _timer;
			root = _root;
			/*
			 * if (_registeredNN != null) { Iterator<NodesPair> ier =
			 * _registeredNN.iterator(); while (ier != null && ier.hasNext()) {
			 * NodesPair tmp = ier.next(); registeredNN.add(tmp.makeCopy()); } }
			 */
		}

	}

	public HSH_Manager<AddressIF> clusteringManager;

	public AbstractMap<AddressIF, Vector<cachedKNNs4NonRingNodes>> pendingKNNs4NonRings = new ConcurrentHashMap<AddressIF, Vector<cachedKNNs4NonRingNodes>>(
			1);

	// ================================================
	// threadpool
	public final ExecutorService execKNN = Executors.newFixedThreadPool(5);

	/**
	 * Nina client
	 */
	public KNN_NinaClient MClient;
	/**
	 * Meridian client
	 */
	public MeridianClient Meridian_MClient;

	/**
	 * Muilti-target knn
	 */

	public MultiTargetKNNManager MuClient;

	public clusteringClient CClient;
	// ================================================

	static boolean useCachedTargetProbes = false;

	// tick control
	final CB0 tickControl;

	// 10-10
	// for non-ring nodes, it caches the callbacks,
	class cachedKNNs4NonRingNodes {
		int version;
		AddressIF target;
		CB1<List<NodesPair>> cbDone;
		final long registeredTime;

		cachedKNNs4NonRingNodes(int _version, AddressIF _target, CB1<List<NodesPair>> _cbDone, long _registeredTime) {
			version = _version;

			target = _target;
			cbDone = _cbDone;
			registeredTime = _registeredTime;
		}

	}

	public AbstractNNSearchManager(ObjCommIF _GossipComm, ObjCommIF _comm, ObjCommIF _MeasureComm,
			PingManager pingManager, StableManager ncManager) {
		comm = _comm;
		measureComm = _MeasureComm;
		gossipComm = _GossipComm;
		/**
		 * success rate
		 */
		SuccessRateForSingleTargetKNN = new EWMAStatistic();
		SuccessRateForMultiTargetKNN = new EWMAStatistic();

		// cachedHistoricalNeighbors=new
		// HistoryNearestNeighbors<AddressIF>(Ninaloader.me,fixedSize);

		tickControl = new CB0() {
			@Override
			protected void cb(CBResult result) {
				// TODO Auto-generated method stub
				logControlTraffic();
			}
		};

		/*
		 * KNNAbsoluteError=new EWMAStatistic(); KNNRelativeError=new
		 * EWMAStatistic(); KNNcoverage=new EWMAStatistic(); KNNClosestNodes=new
		 * EWMAStatistic(); KNNGains=new EWMAStatistic();
		 */

		nonEmptyRingNumber = new EWMAStatistic();

		this.pingManager = pingManager;
		this.ncManager = ncManager;
		// ElapsedKNNSearchTime=new EWMAStatistic();
		// pendingClosestQuery=new HashMap<AddressIF,
		// RemoteState<AddressIF>>(10);
		// closestMappings=new HashMap<AddressIF, AddressIF>(10);

		/**
		 * use the Nina method or Meridian method
		 */
		if (!this.startMeridian) {
			MClient = new KNN_NinaClient();
			// execKNN.execute(MClient);
		} else {
			Meridian_MClient = new MeridianClient();
		}

		/*
		 * //use cluster if(ClusteringSelection>0){ //Ericfu CClient=new
		 * clusteringClient(); execKNN.execute(CClient); }
		 */
		// clusteringManager=new RecurRelaBasedHSH_Manager<AddressIF>();

		// execKNN.execute(clusteringManager);

		// ================================

		/*
		 * if(startMeridian){ meridianManager=new MeridianSearchManager( _comm,
		 * pingManager, ncManager); meridianManager.MClient.init(); }
		 */

		// parse the string
		AllAlivecachedNodeStrings = MainGeneric.parseAllNodes(AllNodes, Ninaloader.COMM_PORT);

	}

	/**
	 * parse all nodes
	 */
	void parseAllNodes(int port, final CB1<ArrayList<AddressIF>> cbDone) {
		final ArrayList<AddressIF> AllAliveNodes = new ArrayList<AddressIF>(100);
		final List<String> nodeAddrs = MainGeneric.parseAllNodes(AllNodes, port);

		AddressFactory.createResolved(nodeAddrs, port, new CB1<Map<String, AddressIF>>() {

			protected void cb(CBResult result, Map<String, AddressIF> addrMap) {
				switch (result.state) {
				case OK: {
					for (String remoteNode : addrMap.keySet()) {
						// System.out.println("remoteNode='" +
						// remoteNode + "'");
						AddressIF remoteAddr = addrMap.get(remoteNode);
						// we keep these around in case we run out of
						// neighbors in the future
						// not me
						// if(!remoteAddr.equals(Ninaloader.me)){
						// bootstrapAddrs.add(remoteAddr);
						// addPendingNeighbor(remoteAddr);
						// }
						AllAliveNodes.add(remoteAddr);
					}

					cbDone.call(result, AllAliveNodes);
					break;
				}
				case TIMEOUT:
				case ERROR: {
					log.error("Could not resolve bootstrap list: " + result.what);
					cbDone.call(result, AllAliveNodes);
					break;
				}
				}
				nodeAddrs.clear();
			}
		});
	}

	public String getParams() {
		return "beta: " + this.betaRatio + "bindim: " + this._binDim + ", betaCutoff: " + this.betaCutoff
				+ ",RingMaintainSelection: " + this.RingMaintainSelection + ", nodesPerRing: " + this.nodesPerRing
				+ ", UPDATE_DELAY: " + this.RingUPDATE_DELAY + "\n";
	}

	public void init(final CB0 cbDone) {

		// upNeighbors = new HashSet<AddressIF>(10);
		// downNeighbors = new HashSet<AddressIF>(10);

		// pendingTargets = new HashMap<AddressIF, RemoteState<AddressIF>>(10);

		bootstrapAddrs = new HashSet<AddressIF>(10);
		RelativeClusteringLandmarksAddrs = new HashSet<AddressIF>(10);
		log.debug("Resolving bootstrap list");

		// resolve the bootstrap list
		AddressFactory.createResolved(Arrays.asList(bootstrapList), Ninaloader.COMM_PORT,
				new CB1<Map<String, AddressIF>>() {

					protected void cb(CBResult result, Map<String, AddressIF> addrMap) {
						switch (result.state) {
						case OK: {
							for (String remoteNode : addrMap.keySet()) {
								// System.out.println("remoteNode='" +
								// remoteNode + "'");
								AddressIF remoteAddr = addrMap.get(remoteNode);
								// we keep these around in case we run out of
								// neighbors in the future
								// not me
								// if(!remoteAddr.equals(Ninaloader.me)){
								bootstrapAddrs.add(remoteAddr);
								addPendingNeighbor(remoteAddr);
								// add the target
								if (isQueryNode && !targetCandidates.contains(remoteAddr)) {
									targetCandidates.add(remoteAddr);
								}
								// }
							}

							/*
							 * parseAllNodes(Ninaloader.COMM_PORT,new CB0(){
							 * 
							 * @Override protected void cb(CBResult result) { //
							 * TODO Auto-generated method stub
							 * switch(result.state){ case OK:{
							 * 
							 * cbDone.call(result); break; } default:{ log.warn(
							 * "can not parse all nodes"); cbDone.call(result);
							 * break; } } }});
							 */
							cbDone.callOK();
							break;
						}
						case TIMEOUT:
						case ERROR: {
							log.error("Could not resolve bootstrap list: " + result.what);
							cbDone.call(result);
							break;
						}
						}
					}
				});

		// meridianManager.init();
	}

	/*	*//**
			 * refresh my relative coordinate
			 *//*
			 * void refreshRelativeCoordinate(){ this.execKNN.execute(new
			 * Runnable(){
			 * 
			 * public void run() { List<NodesPair> pingedResult=
			 * MainGeneric.pingTargetedNodes(Ninaloader.me,
			 * instance.g_rings.binningProc.BeaconNodes);
			 * instance.g_rings.myCoordinate.setCoordinate(pingedResult); } });
			 * }
			 */

	// /**
	// * ask the remote node's relative coordinate
	// * @param remoteNode
	// */
	// void askRemoteRelativeCoordinateIntoRing(final AddressIF remoteNode){
	//
	// RelativeCoordinateRequestMsg msg=new
	// RelativeCoordinateRequestMsg(Ninaloader.me);
	//
	// comm.sendRequestMessage(msg, remoteNode,
	// new ObjCommRRCB<RelativeCoordinateResponseMsg>() {
	// protected void cb(CBResult result,
	// final RelativeCoordinateResponseMsg responseMsg,
	// AddressIF node, Long ts) {
	// switch (result.state) {
	// case OK: {
	// // System.out.println("$ ! We have received ACK from"+responseMsg.from);
	// instance.g_rings.addRelativeCoordinate(remoteNode,
	// responseMsg.myRelativeCoordinate.coords);
	//
	// break;
	// }
	// case ERROR:
	// case TIMEOUT: {
	// String error =remoteNode.toString(false)
	// + "has not received requirement, as: "
	// + result.toString();
	// log.warn(error);
	//
	// break;
	// }
	// }
	//
	// /* if(msg!=null){
	// msg.clear();
	// }
	// if(responseMsg!=null){
	// responseMsg.clear();
	// }*/
	// //responseMsg.from=null;
	// //responseMsg.myRelativeCoordinate.clear();
	//// barrier.join();
	// }
	// });
	//
	// /* if(msg!=null){
	// msg.clear();
	// }*/
	// }
	//
	// ask ringMembers to ping the target nodes

	public void receiveFathestReq(final FarthestRequestMsg fmsg, final StringBuffer errorBuffer) {

		final long sendStamp = System.nanoTime();

		// fmsg.totalSendingMessages+=1;

		ncManager.doPing(fmsg.target, new CB2<Double, Long>() {

			protected void cb(CBResult result, Double lat, Long timer) {
				// TODO Auto-generated method stub
				double rtt_snap = (System.nanoTime() - sendStamp) / 1000000d;

				switch (result.state) {
				case OK: {
					if (lat.doubleValue() >= 0) {
						rtt_snap = lat.doubleValue();
					}

					AddressIF remoteAddr = fmsg.target;

					/*
					 * if (!remoteAddr.equals(Ninaloader.me) && rtt_snap > 0) {
					 * 
					 * if (!pendingTargets.containsKey(remoteAddr)) {
					 * pendingTargets.put(AddressFactory .create(remoteAddr),
					 * new RemoteState<AddressIF>(remoteAddr)); } long curr_time
					 * = System.currentTimeMillis();
					 * pendingTargets.get(remoteAddr).addSample(rtt_snap,
					 * curr_time); }
					 * 
					 * final double rtt = pendingTargets.get(remoteAddr)
					 * .getSample();
					 */
					// final double rtt
					// =rtt_snap+AbstractNNSearchManager.offsetLatency;
					final double rtt = rtt_snap;

					log.debug("RTT of " + rtt + " from " + Ninaloader.me + " to " + fmsg.target);

					// update the coordinate
					Coordinate new_targetCoordinate = NCClient.updateProxyTargetCoordinate(fmsg.target,
							fmsg.targetCoordinates, fmsg.targetCoordinates.r_error, MClient.primaryNC.getSystemCoords(),
							MClient.primaryNC.getSystemError(), rtt);

					fmsg.targetCoordinates = new_targetCoordinate.makeCopy();
					new_targetCoordinate.clear();

					// update my coordinate
					MClient.primaryNC.simpleCoordinateUpdate(fmsg.target, fmsg.targetCoordinates,
							fmsg.targetCoordinates.r_error, rtt, System.currentTimeMillis());
					// =====================================

					final Vector<AddressIF> ringMembers = new Vector<AddressIF>(10);

					if (!useNormalRingRange) {

						// find maximum RTT, (rtt, maxRTT)
						// 2010-3-7
						double maxRTT = 2 * rtt;

						if (fmsg.CMsg != null && fmsg.CMsg.getNearestNeighborIndex() != null
								&& !fmsg.CMsg.getNearestNeighborIndex().isEmpty()) {
							MClient.instance.g_rings.fillVector(fmsg.CMsg.getNearestNeighborIndex(), fmsg.target, rtt,
									maxRTT, betaRatioFarthest, ringMembers, AbstractNNSearchManager.offsetLatency);
						} else {
							MClient.instance.g_rings.fillVector(fmsg.target, rtt, maxRTT, betaRatioFarthest,
									ringMembers, AbstractNNSearchManager.offsetLatency);
						}
						// add perturbation
						if (useRandomSeedsInSearching) {
							Set<AddressIF> randomSeeds = new HashSet<AddressIF>(defaultNodesForRandomContact);
							MClient.instance.g_rings.getNodesWithMaximumNonEmptyRings(randomSeeds,
									defaultNodesForRandomContact);
							ringMembers.addAll(randomSeeds);
						}

					} else {

						// let ring members to ping the target
						// curNode.cRing.g_rings.fillVector(me, RTT, RTT,
						// betaRatio, ringMembers, offset);
						// List<NodesPair>
						// targetRelCoord=fmsg.targetCoordinates.coords;

						/*
						 * log.debug("Farthest Search: dim: "
						 * +targetRelCoord.size());
						 * 
						 * //3-10 // //double MAXRTT=1000;
						 * instance.g_rings.fillVectorBasedOnKFarthestNode(
						 * _choiceOfNextHop, Ninaloader.me, targetRelCoord,rtt,
						 * ringMembers, AbstractNNSearchManager.offsetLatency);
						 */

						Set<AddressIF> ForbiddenNodes = new HashSet<AddressIF>();
						ForbiddenNodes.addAll(fmsg.HopRecords);

						// in default
						double maxVal = 2 * rtt;
						MClient.instance.g_rings.fillVector(Ninaloader.me, ForbiddenNodes, maxVal, betaRatioFarthest,
								ringMembers, offsetLatency);

						/*
						 * //farthest node to target Integer farthest =
						 * this.getFarthestPeerFromListWithCoord(req.getTarget()
						 * , ringMembers); ringMembers.clear();
						 * if(farthest!=null){ ringMembers.add(farthest); }
						 */

						Set<AddressIF> nodes = MClient.instance.g_rings.getKFarthestPeerFromListWithCoord(fmsg.target,
								fmsg.targetCoordinates, ringMembers, returnedNodesForNextHop);
						ringMembers.clear();
						ringMembers.addAll(nodes);
						nodes.clear();

					}

					log.debug("==================\n$: total nodes: " + ringMembers.size() + "\n==================");

					if (!ringMembers.isEmpty() && ringMembers.size() > 0) {

						// target node

						if (ringMembers.contains(fmsg.target)) {
							ringMembers.remove(fmsg.target);
						}

						/*
						 * if (Ninaloader.me.equals(fmsg.target)) {
						 * log.warn("~~~~~~~~~~~~~~~\n" +
						 * "I am the target\n~~~~~~~~~~~~~~~"); assert (false);
						 * // logic error }
						 */

						// also remove the backtracking node
						Iterator<AddressIF> ierBack = fmsg.HopRecords.iterator();
						while (ierBack.hasNext()) {

							AddressIF tmp = ierBack.next();
							if (ringMembers.contains(tmp)) {
								log.debug("Remove forbidden backtracking node: " + tmp);
								ringMembers.remove(tmp);
							}
						}

					}

					final long timer1 = System.currentTimeMillis();
					// returned ID is null, or is the target itself;
					if (ringMembers == null || ((ringMembers != null) && (ringMembers.size() == 0))) {

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
							fmsg.CMsg.targetCoordinates = fmsg.targetCoordinates.makeCopy();
							fmsg.CMsg.operationsForOneNN = fmsg.HopRecords.size();
							fmsg.CMsg.totalSendingMessages = fmsg.totalSendingMessages;
							/*
							 * System.out .println(
							 * "\n##################\n$: NULL Bloom filter in Farthest KNN search !\n##################\n"
							 * );
							 */
							receiveClosestQuery(fmsg.CMsg.targetCoordinates, fmsg.CMsg, errorBuffer);
							fmsg.clear();

						} else {

							fmsg.SCMsg.setFrom(Ninaloader.me);
							fmsg.SCMsg.setRoot(Ninaloader.me);
							// hop records
							// fmsg.SCMsg.getHopRecords().add(Ninaloader.me);
							fmsg.SCMsg.targetCoordinates = fmsg.targetCoordinates.makeCopy();

							fmsg.SCMsg.totalSendingMessages = fmsg.totalSendingMessages;
							/*
							 * System.out .println(
							 * "\n##################\n$: NULL Bloom filter in Farthest KNN search !\n##################\n"
							 * );
							 */ receiveClosestQuery(fmsg.SCMsg.targetCoordinates, fmsg.SCMsg, errorBuffer);

						}

					} else {

						TargetProbes(ringMembers, fmsg.target, new CB1<String>() {
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

								// clear the storage space
								fmsg.totalSendingMessages += nps.size();
								ringMembers.clear();

								switch (ncResult.state) {
								case OK: {
									AddressIF returnID = Ninaloader.me;

									// update target's coordinate
									Iterator<NodesPair> ier = nps.iterator();
									while (ier.hasNext()) {
										NodesPair rec = ier.next();
										AddressIF remoteNode = (AddressIF) rec.startNode;
										if (MClient.instance.g_rings.NodesCoords.containsKey(remoteNode)) {

											Coordinate refCoord = MClient.instance.g_rings.NodesCoords.get(remoteNode);
											if (refCoord != null && fmsg.targetCoordinates != null) {
												fmsg.targetCoordinates = NCClient.updateProxyTargetCoordinate(
														fmsg.target, fmsg.targetCoordinates,
														fmsg.targetCoordinates.r_error, refCoord, refCoord.r_error,
														rec.rtt);
											}

										}
									}

									if (nps != null) {
										if (!Ninaloader.me.equals(fmsg.target)) {
											nps.add(new NodesPair(Ninaloader.me, fmsg.target, rtt));
										}

										double total_value = 0.;
										int count = 0;
										double maximum = Double.MIN_VALUE;
										ier = nps.iterator();
										// bigger !
										while (ier.hasNext()) {
											NodesPair tmp = (NodesPair) ier.next();
											if (tmp.startNode.equals(fmsg.target)) {
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
										if (RTT_Threshold > (1 + betaRatioFarthest) * rtt) {
											RTT_Threshold = (1 + betaRatioFarthest) * rtt;
										}

										// adaptive threshold

										// stop the search
										if (maximum < RTT_Threshold) {

											// adpative threshold
											if (maximum >= (1 + FarthestBetaCutoff) * RTT_Threshold
													/ (1 + betaRatioFarthest)) {
												RTT_Threshold = (1 + FarthestBetaCutoff) * rtt;
											}

											// not me , send stop message
											if (!returnID.equals(Ninaloader.me)) {
												FinFarthestRequestMsg msg22 = FinFarthestRequestMsg.makeCopy(fmsg);
												msg22.from = Ninaloader.me;
												// relative coordinate
												msg22.targetCoordinates = fmsg.targetCoordinates.makeCopy();
												msg22.totalSendingMessages = fmsg.totalSendingMessages;
												msg22.totalSendingMessages += 1;
												comm.sendRequestMessage(msg22, returnID,
														new ObjCommRRCB<FinFarthestResponseMsg>() {
													protected void cb(CBResult result,
															final FinFarthestResponseMsg responseMsg, AddressIF node,
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
															break;
														}
														}
														// ----------------------
														// msg22.clear();
													}
													// --------------------------
												});

												if (msg22 != null) {
													msg22.clear();
												}

												return;
											} else {
												// its me
											}

										}

										//
										nps.clear();
									}

									// -----------------------------------------
									if (returnID.equals(Ninaloader.me)) {
										// OK send originFrom a
										// message
										/*
										 * log.info("Farthest Search: " +
										 * Ninaloader.me +
										 * " is the farthest to: " + fmsg.target
										 * + " @: " +
										 * (rtt-AbstractNNSearchManager.
										 * offsetLatency)); log.debug(
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
											fmsg.CMsg.targetCoordinates = fmsg.targetCoordinates.makeCopy();

											fmsg.CMsg.totalSendingMessages = fmsg.totalSendingMessages;
											fmsg.CMsg.operationsForOneNN = fmsg.HopRecords.size();
											// the backtrack node is null
											if (fmsg.CMsg.getForbiddenFilter() == null) {
												log.debug(
														"\n##################\n$: NULL Bloom filter in Farthest KNN search !\n##################\n");
												receiveClosestQuery(fmsg.CMsg.targetCoordinates, fmsg.CMsg,
														errorBuffer);
											} else {
												// ========================
												// filter based
												receiveClosestQuery(fmsg.CMsg.targetCoordinates, fmsg.CMsg,
														errorBuffer);
											}

										} else {

											fmsg.SCMsg.setFrom(Ninaloader.me);
											fmsg.SCMsg.setRoot(Ninaloader.me);
											// hop records
											// fmsg.SCMsg.getHopRecords().add(Ninaloader.me);
											fmsg.SCMsg.targetCoordinates = fmsg.targetCoordinates.makeCopy();

											fmsg.SCMsg.totalSendingMessages = fmsg.totalSendingMessages;
											// the backtrack node is null
											if (fmsg.SCMsg.getForbiddenFilter() == null) {
												System.out.println(
														"\n##################\n$: NULL Bloom filter in Farthest KNN search !\n##################\n");
												receiveClosestQuery(fmsg.SCMsg.targetCoordinates, fmsg.SCMsg,
														errorBuffer);
											} else {
												// ========================
												// filter based
												receiveClosestQuery(fmsg.SCMsg.targetCoordinates, fmsg.SCMsg,
														errorBuffer);
											}
										}

									}

									else {

										//
										// directly use current Farthest search
										// message
										FarthestRequestMsg msg = null;
										if (fmsg.CMsg != null) {
											msg = new FarthestRequestMsg(Ninaloader.me, fmsg.target, fmsg.OriginFrom,
													fmsg.CMsg);
											msg.CMsg.increaseMessage(1);
										}

										if (fmsg.SCMsg != null) {
											msg = new FarthestRequestMsg(Ninaloader.me, fmsg.target, fmsg.OriginFrom,
													fmsg.SCMsg);

										}

										/*
										 * if(msg!=null&&fmsg!=null&&fmsg.
										 * targetCoordinates!=null){
										 * msg.targetCoordinates=fmsg.
										 * targetCoordinates; }
										 */
										msg.targetCoordinates = fmsg.targetCoordinates.makeCopy();

										if (fmsg.HopRecords != null && !fmsg.HopRecords.isEmpty()) {
											msg.addCurrentHop(fmsg.HopRecords, Ninaloader.me);
										}
										// overhead
										msg.totalSendingMessages = fmsg.totalSendingMessages;

										comm.sendRequestMessage(msg, returnID, new ObjCommRRCB<FarthestResponseMsg>() {
											protected void cb(CBResult result, final FarthestResponseMsg responseMsg,
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
													break;
												}
												}
												// ----------------------

											}
											// --------------------------
										});
										// clear the records
										/*
										 * if(fmsg!=null){ fmsg.clear(); }
										 * if(msg!=null){ msg.clear(); }
										 */
									}

									break;
								}
								case ERROR:
								case TIMEOUT: {
									System.err.println("Farthest Search: Target probes to " + fmsg.target + " FAILED!"); // the
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

					break;
				}
				}
			}

		});

	}

	// If this guy is in an unknown state add him to pending.
	void addPendingNeighbor(AddressIF node) {
		// assert node != null : "Pending neighbour is null?";

		if (pendingNeighbors.containsKey(node)) {
			return;
		}

		if (pendingNeighbors.size() > maxPendingNeighbors) {

			List<AddressIF> nodes = new ArrayList<AddressIF>(2);
			nodes.addAll(pendingNeighbors.keySet());
			Collections.shuffle(nodes);
			AddressIF randNode = nodes.get(0);
			pendingNeighbors.remove(randNode);
			pendingNeighbors.put(node, new RemoteState<AddressIF>(node));
			nodes.clear();

			/*
			 * int count=pendingNeighbors.size()-maxPendingNeighbors;
			 * Iterator<Entry<AddressIF, RemoteState<AddressIF>>> ier =
			 * pendingNeighbors.entrySet().iterator();
			 * while(count>0&&ier.hasNext()){ ier.remove(); count--; }
			 * pendingNeighbors.put(node, new RemoteState<AddressIF>( node));
			 */

		} else {
			pendingNeighbors.put(node, new RemoteState<AddressIF>(node));
		}

	}

	/*
	 * // If this guy is in an unknown state add him to pending. void
	 * addPendingNeighbor(AddressIF node) { assert node != null :
	 * "Pending neighbour is null?";
	 * 
	 * 
	 * if(pendingNeighbors.size()>maxPendingNeighbors){ List nodes=new
	 * ArrayList(2); nodes.addAll(pendingNeighbors.keySet());
	 * Iterator<AddressIF> ier = nodes.iterator(); int
	 * counter=pendingNeighbors.size()-maxPendingNeighbors+5;
	 * while(ier.hasNext()&&counter>0){ AddressIF tmp = ier.next();
	 * pendingNeighbors.remove(tmp); counter--; } nodes.clear(); }
	 * 
	 * 
	 * 
	 * if (node.equals(comm.getLocalAddress())) return;
	 * 
	 * if (!pendingNeighbors.containsKey(node)) { addPendingNeighbor(node);
	 * 
	 * pendingNeighbors.put(node, new RemoteState<AddressIF>(node)); log.debug(
	 * "addPendingNeighbor: " + node); } else { log.debug(
	 * "!addPendingNeighbor: " + node); } if (WATCH_NEIGHBORS) dumpNeighbors();
	 * }
	 */

	void dumpNeighbors() {
		log.debug(listNeighbors());
	}

	String listNeighbors() {
		StringBuffer sb = new StringBuffer();
		sb.append("pending:");
		for (AddressIF node : pendingNeighbors.keySet()) {
			sb.append(" " + node);
		}

		return new String(sb);
	}

	/**
	 * //main entry
	 */

	/**
	 * find who is nearest to src
	 */
	/*
	 * void QueryNearestNeighbor(final AddressIF src,int K ,final
	 * CB1<Map<AddressIF, NodesPair>> cdDone){
	 * 
	 * // //ClosestRequestMsg msg=new ClosestRequestMsg(Ninaloader.me,
	 * Ninaloader.me,src);
	 * 
	 * //added K neighbors ClosestRequestMsg msg=new
	 * ClosestRequestMsg(Ninaloader.me, Ninaloader.me,src,K);
	 * 
	 * 
	 * //send to a random neighbor in Set upNeighbors
	 * comm.sendRequestMessage(msg, upNeighbors.iterator().next(), new
	 * ObjCommRRCB<ClosestResponseMsg>(){ protected void cb(CBResult result,
	 * final ClosestResponseMsg responseMsg, AddressIF node, Long ts){ switch
	 * (result.state) { case OK: { System.out.println(
	 * "Nearest Request Acked by new query node: "); //cdDone.call(result);
	 * cachedCB.put(src, cdDone); break; } case ERROR: case TIMEOUT: {
	 * cdDone.call(result,null); //the returned map is null break; } }
	 * //---------------------- } //-------------------------- }); }
	 */
	public abstract void queryKNearestNeighbors(AddressIF target, int k, CB1<List<NodesPair>> cbDone);

	public abstract void sortNodesByDistances(Vector<AddressIF> addrs, int N, CB1<Set<NodesPair>> cbDone);

	public abstract void receiveClosestQuery(Coordinate target, final ClosestRequestMsg req,
			final StringBuffer errorBuffer);

	public abstract void receiveClosestQuery(Coordinate target, final SubSetClosestRequestMsg req,
			final StringBuffer errorBuffer);

	public abstract void queryKNN(final String peeringNeighbor, final String target, final int K,
			final CB1<List<NodesPair>> cbDone);

	public abstract void queryKNearestNeighbors(final AddressIF target, List<AddressIF> candidates, final int k,
			final CB1<List<NodesPair>> cbDone);

	public abstract void queryKFarthestNeighbors(final AddressIF target, final int k,
			final CB1<List<NodesPair>> cbDone);

	public abstract void receiveClosestQuery_MultipleTargets(final Coordinate targetCoordinate,
			final ClosestRequestMsg req, final StringBuffer errorBuffer);

	public abstract void queryKNearestNeighbors(final ArrayList<AddressIF> targets, final int expectedKNN,
			final CB1<List<NodesPair>> cbDone);

	public abstract void queryNearestNeighbor(final AddressIF target, int NumOfKNN, final CB1<NodesPair> cbDone);

	/**
	 * KNN based on ring-support KNN search
	 * 
	 * @param target
	 * @param peers
	 * @param cbDone
	 */
	public void queryKNearestNeighbors(final String target, final String[] peers, final int K,
			final CB1<Map<String, Double>> cbDone) {

		final Map<String, Double> map = new Hashtable<String, Double>(5);

		Vector<String> total = new Vector<String>(2);
		total.add(target);
		total.addAll(Arrays.asList(peers));

		AddressFactory.createResolved(total, Ninaloader.COMM_PORT, new CB1<Map<String, AddressIF>>() {
			protected void cb(CBResult result, Map<String, AddressIF> addrMap) {
				switch (result.state) {
				case OK:

					// TODO jonathan: why do you handle OK and TIMEOUT together
					// and why is there an ERROR clause?
					// The way createResolved works is that it always returns OK
					// but only includes the hostnames
					// that resolved correctly in the map. Let me know if you
					// don't like this behaviour and I can change it.
				case TIMEOUT: {

					// StringBuffer dnsErrors = new StringBuffer();
					final List<AddressIF> remoteNodes = new ArrayList<AddressIF>(2);
					AddressIF TargetAddr = null;

					for (String node : addrMap.keySet()) {
						if (node.equalsIgnoreCase(target)) {
							TargetAddr = addrMap.get(node);
						} else {
							remoteNodes.add(addrMap.get(node));
						}

						// else {
						// dnsErrors.append("Could not resolve "+node);
						// }
					}
					if (TargetAddr == null) {
						cbDone.call(result, map);
						return;
					} else {

						// =============================================
						// search process
						int realK = Math.min(K, remoteNodes.size());
						MClient.ConstrainedKNN(TargetAddr, remoteNodes, realK,
								new CB2<Vector<String>, Vector<Double>>() {

							protected void cb(CBResult result, Vector<String> addr, Vector<Double> RTT) {
								if (addr != null && RTT != null && addr.size() == RTT.size()) {
									for (int i = 0; i < addr.size(); i++) {
										map.put(addr.get(i), RTT.get(i));

									}
									cbDone.call(CBResult.OK(), map);
								} else {
									cbDone.call(CBResult.ERROR(), map);
								}

							}

						});

						// =====================================================================

					}

					break;
				}

				case ERROR: {
					// problem resolving one or more of the addresses
					log.warn(result.toString());
					cbDone.call(result, map);
				}
				}
			}
		});

	}

	/**
	 * receive a Nearest Request msg
	 * 
	 * @param originFrom
	 * @param to
	 * @param errorBuffer
	 */
	/*
	 * public void receiveClosestQuery(final AddressIF originFrom, final int
	 * K,final AddressIF to, final StringBuffer errorBuffer){
	 * 
	 * 
	 * // send the target a gossip msg // LOWTODO could bias which nodes are
	 * sent based on his coord GossipRequestMsg msg = new
	 * GossipRequestMsg(null,Ninaloader.me,false); final long
	 * sendStamp=System.nanoTime(); comm.sendRequestMessage(msg, to, new
	 * ObjCommRRCB<GossipResponseMsg>(){ protected void cb(CBResult result,
	 * final GossipResponseMsg responseMsg, AddressIF node, Long ts) { final
	 * double rtt = (System.nanoTime() - sendStamp) / 1000000d; switch
	 * (result.state) { case OK: { System.out.println("RTT of " + rtt +" from "
	 * +Ninaloader.me+" to " + to); //save the latency measurements
	 * pendingClosestQuery.put(to, Double.valueOf(rtt));
	 * 
	 * //request nodes to probe to target nodes Vector<AddressIF>
	 * ringMembers=new Vector<AddressIF>(10);
	 * MClient.instance.g_rings.fillVector(to,rtt, rtt, betaRatio, ringMembers);
	 * if(!ringMembers.isEmpty()&&ringMembers.size()>0){
	 * if(ringMembers.contains(to)){ ringMembers.remove(to); } } //returned ID
	 * is null, or is the target itself;
	 * if(ringMembers==null||((ringMembers!=null
	 * )&&(ringMembers.size()==0))||ringMembers.contains(to)){ //no nearer
	 * nodes, so return me System.out.println(Ninaloader.me+
	 * " is the closest to: "+to);
	 * 
	 * //cached result Set<NodesPair> ReturnedID=new HashSet<NodesPair>();
	 * ReturnedID.add(new NodesPair(to, Ninaloader.me,rtt));
	 * TargetLocatedRequestMsg msg=new TargetLocatedRequestMsg(originFrom,
	 * Ninaloader.me,to,rtt,K,ReturnedID); comm.sendRequestMessage(msg,
	 * originFrom, new ObjCommRRCB<TargetLocatedResponseMsg>(){ protected void
	 * cb(CBResult result, final TargetLocatedResponseMsg responseMsg, AddressIF
	 * node, Long ts) { switch (result.state) { case OK: { //acked by original
	 * query node //cdDone.callOK(); break;
	 * 
	 * } case ERROR: case TIMEOUT: { // cdDone.call(result); break; } } } });
	 * }else { TargetProbes(ringMembers,to ,new CB2<Set<NodesPair>, String>(){
	 * protected void cb(CBResult ncResult, Set<NodesPair> nps, String
	 * errorString){
	 * 
	 * AddressIF returnID=null;
	 * 
	 * double minimum=Double.MAX_VALUE; Iterator ier=nps.iterator();
	 * while(ier.hasNext()){ NodesPair tmp=(NodesPair)ier.next();
	 * if(tmp.rtt<minimum){ returnID=tmp.startNode; minimum=tmp.rtt; } }
	 * //-----------------------------------------
	 * if(returnID.equals(Ninaloader.me)||returnID.equals(to)){ //OK send
	 * originFrom a message System.out.println(Ninaloader.me+
	 * " is the closest to: "+to); Set<NodesPair> ReturnedID=new
	 * HashSet<NodesPair>(); ReturnedID.add(new NodesPair(to, returnID,rtt));
	 * 
	 * TargetLocatedRequestMsg msg=new TargetLocatedRequestMsg(originFrom,
	 * returnID,to,rtt,K,ReturnedID); comm.sendRequestMessage(msg, originFrom,
	 * new ObjCommRRCB<TargetLocatedResponseMsg>(){ protected void cb(CBResult
	 * result, final TargetLocatedResponseMsg responseMsg, AddressIF node, Long
	 * ts) { switch (result.state) { case OK: { //acked by original query node
	 * // cdDone.call(result); break; } case ERROR: case TIMEOUT: { //
	 * cdDone.call(result); break; } } } }); }
	 * 
	 * else{
	 * 
	 * // ClosestRequestMsg msg=new ClosestRequestMsg(originFrom,
	 * Ninaloader.me,to,K);
	 * 
	 * comm.sendRequestMessage(msg, returnID, new
	 * ObjCommRRCB<ClosestResponseMsg>(){ protected void cb(CBResult result,
	 * final ClosestResponseMsg responseMsg, AddressIF node, Long ts){ switch
	 * (result.state) { case OK: { System.out.println(
	 * "Request Acked by new query node: "); // cdDone.call(result); break; }
	 * case ERROR: case TIMEOUT: { // cdDone.call(result); break; } }
	 * //---------------------- } //-------------------------- }); } }// }
	 * 
	 * );}// end of target probe
	 * 
	 * //cdDone.call(result); break; } case ERROR: case TIMEOUT: { String error
	 * = "RTT request to " + to.toString(false) + " failed:"+ result.toString();
	 * log.warn(error); if (errorBuffer.length() != 0) {
	 * errorBuffer.append(","); } errorBuffer.append(error);
	 * //cdDone.call(result); break; } }
	 * 
	 * } });
	 * 
	 * }
	 */

	// according to the thresholds, we decide,
	// > threshold, we stop, and return null, to indicate the backtracking
	// <threshold, we continue
	// TODO: 10.18
	// add the
	// hierarchical
	// searching process

	/**
	 * find closest node, and find the node that is backtracked to
	 */

	public AddressIF ClosestNodeRule(double RTT_O, AddressIF target, Set<NodesPair> nps, Set<NodesPair> closestNodes,
			SubSetManager forbidden, AddressIF lastHop) {

		double RTT = RTT_O + AbstractNNSearchManager.offsetLatency;
		double lowestLatency = Double.MAX_VALUE;

		// empty records
		if (nps.isEmpty()) {
			closestNodes.add(new NodesPair(Ninaloader.me, null, RTT - AbstractNNSearchManager.offsetLatency, lastHop));
			return lastHop;
		}

		// yes. has child node
		AddressIF closestNode = Ninaloader.me;
		NodesPair closestRec = null;

		Iterator<NodesPair> ier = nps.iterator();

		double total_value = 0.0;
		int count = 0;
		// ============================================
		while (ier.hasNext()) {
			NodesPair tmp = ier.next();
			AddressIF start = (AddressIF) tmp.startNode;
			if (tmp.rtt < 0) {
				continue;
			}
			if (forbidden.checkNode(start)) {
				continue;
			}
			/*
			 * if(!target.equals(tmp.endNode)){ System.err.println(
			 * "\n================\nERROR, ClosestNodeRule is broken! !target.equals(tmp.endNode)!\n================\n "
			 * ); continue; }
			 */

			total_value += tmp.rtt + AbstractNNSearchManager.offsetLatency;
			count++;

			if (tmp.rtt + AbstractNNSearchManager.offsetLatency < lowestLatency) {
				lowestLatency = tmp.rtt + AbstractNNSearchManager.offsetLatency;
				closestNode = start;
				closestRec = tmp;
			}
		}
		// //=============================================
		// double avgRTT=total_value/count;
		// double RTT_Threshold=betaRatio*avgRTT;
		//
		// if(RTT_Threshold<betaRatio*RTT){
		// System.err.println("We can not find closer ring members!");
		// RTT_Threshold=betaRatio*RTT;
		// }

		// second
		// double secondDist=Double.MAX_VALUE;
		// AddressIF secondClosestNode=null;
		//
		// ier= nps.iterator();
		// while(ier.hasNext()){
		// NodesPair tmp = ier.next();
		// AddressIF addr=(AddressIF)tmp.startNode;
		//
		// if((!addr.equals(closestNode))&&((tmp.rtt+AbstractNNSearchManager.offsetLatency)<secondDist)){
		// secondClosestNode=addr;
		// secondDist=tmp.rtt+AbstractNNSearchManager.offsetLatency;
		// }
		// }
		//
		// if(lowestLatency>RTT_Threshold){

		// yes, smaller than the cutoff
		if ((lowestLatency <= RTT && !closestNode.equals(Ninaloader.me))) {
			return closestNode;
		} else {
			// >RTT
			closestNodes.add(new NodesPair(Ninaloader.me, null, RTT - AbstractNNSearchManager.offsetLatency, lastHop));
			return lastHop;
		}

		// =======================================
		// }else{
		// //continue the search process
		// return closestNode;
		//
		// }

		// //log.info(msg);
		// if(lowestLatency>RTT_Threshold){
		//
		// //still can search
		// if(useAdaptiveSearch){
		// //yes, smaller than the cutoff
		// if((lowestLatency<RTT)&&(lowestLatency<(betaCutoff*RTT_Threshold/betaRatio))){
		// return closestNode;
		// }
		// }
		//
		// //can not search find node that has minimum latency
		//
		// //I am the closest
		// if(lowestLatency>RTT){
		// closestNodes.add(new
		// NodesPair(Ninaloader.me,null,RTT-AbstractNNSearchManager.offsetLatency,lastHop));
		// return lastHop;
		// }else{
		// //child is the closest
		// closestNodes.add(new
		// NodesPair(closestNode,null,lowestLatency-AbstractNNSearchManager.offsetLatency,Ninaloader.me));
		//
		// if(secondClosestNode==null||secondDist>RTT){
		// closestNodes.add(new
		// NodesPair(Ninaloader.me,null,RTT-AbstractNNSearchManager.offsetLatency,lastHop));
		// return lastHop;
		// }else{
		// return secondClosestNode;
		// }
		// }
		//
		// //=======================================
		// }else{
		// //continue the search process
		// return closestNode;
		//
		// }

	}

	public AddressIF ClosestNodeRule2(double RTT, AddressIF target, Set<NodesPair> nps, Set<NodesPair> closestNodes,
			SubSetManager forbidden, AddressIF lastHop) {

		// ===================================
		// TODO Auto-generated method stub
		double lowestLatency = Double.MAX_VALUE;

		/*
		 * double []RTT=new double[1];
		 * 
		 * ncManager.isCached(target, RTT, -1); //not cached if(RTT[0]<=0){
		 * assert(false); }
		 */
		// use the offset to the raw measurements
		RTT += offsetLatency;

		AddressIF closestNode = Ninaloader.me;

		if (nps != null && !nps.isEmpty()) {

			Iterator<NodesPair> ier = nps.iterator();

			double total_value = 0.0;
			int count = 0;
			// ============================================
			while (ier.hasNext()) {
				NodesPair<AddressIF> tmp = ier.next();
				AddressIF start = tmp.startNode;
				if (tmp.rtt < 0) {
					continue;
				}
				if (forbidden.checkNode(start)) {
					continue;
				}
				/*
				 * if(!target.equals(tmp.endNode)){ System.err.println(
				 * "\n================\nERROR, ClosestNodeRule is broken! !target.equals(tmp.endNode)!\n================\n "
				 * ); continue; }
				 */

				total_value += tmp.rtt + offsetLatency;
				count++;

				if (tmp.rtt + offsetLatency < lowestLatency) {
					lowestLatency = tmp.rtt + offsetLatency;
					closestNode = start;
				}
			}
			// =============================================
			// double avgRTT=total_value/count;
			double RTT_Threshold = betaRatio * RTT;

			if (RTT_Threshold > RTT) {
				// log.warn("RTT threshold is bigger than RTT!");
				RTT_Threshold = betaRatio * RTT;
			}

			// log.info(msg);
			if (lowestLatency > RTT_Threshold) {

				if (useAdaptiveSearch) {
					// yes, smaller than the cutoff
					if ((lowestLatency < RTT) && (lowestLatency < (betaCutoff * RTT_Threshold / betaRatio))) {
						return closestNode;
					}
				}

				// add all nodes that are lower than avgRTT
				// compare the minimum node with me
				if (lowestLatency > RTT) {
					closestNodes.add(new NodesPair(Ninaloader.me, target, RTT - offsetLatency));
					return null;
				} else {
					closestNodes.add(new NodesPair(closestNode, target, lowestLatency - offsetLatency));
					return Ninaloader.me;
				}

			} else {
				// find a node
				//
				return closestNode;

			}

		} else {
			closestNodes.add(new NodesPair(Ninaloader.me, target, RTT - offsetLatency));
			return null;
		}

		// ===================================

		/**
		 * original method AddressIF returnID = Ninaloader.me; final SortedList
		 * <NodesPair> sortedL = new SortedList<NodesPair>( new
		 * NodesPairComp());
		 * 
		 * if (nps != null) {
		 * 
		 * // obtaned in the // beginning
		 * 
		 * // If I am not the // target, then add // me also if (!Ninaloader.me
		 * .equals(req .getTarget())) { nps .add(new NodesPair( Ninaloader.me,
		 * req .getTarget(), lat)); }
		 * 
		 * // the greedy search // process- adapted // from OASIS in // NSDI
		 * double minimum = Double.MAX_VALUE; Iterator ier2 = nps .iterator();
		 * while (ier2.hasNext()) {
		 * 
		 * NodesPair tmp = (NodesPair) ier2 .next(); // invalid // measurements
		 * if (tmp.rtt < 0) { continue; } // the same with // target if
		 * (tmp.startNode .equals(req .getTarget())) { continue; } // if tmp.lat
		 * < // current // latency from // me
		 * 
		 * //10-18- add the beta constriant, to reduce the elapsed time // if <
		 * beta, return current node if (tmp.rtt <= lat) { sortedL.add(tmp); }
		 * 
		 * if (tmp.rtt < minimum) { returnID = tmp.startNode; minimum = tmp.rtt;
		 * } } }
		 */
	}

	/**
	 * find the farthest node
	 * 
	 * @param RTT_O
	 * @param target
	 * @param nps
	 * @param closestNodes
	 * @param forbidden
	 * @param lastHop
	 * @return
	 */
	public AddressIF farthestNodeRule(double RTT_O, AddressIF target, Set<NodesPair> nps, Set<NodesPair> closestNodes,
			SubSetManager forbidden, AddressIF lastHop) {

		double RTT = RTT_O + AbstractNNSearchManager.offsetLatency;

		double LargestLatency = Double.MIN_VALUE;

		/*
		 * double []RTT=new double[1];
		 * 
		 * ncManager.isCached(target, RTT, -1); //not cached if(RTT[0]<=0){
		 * assert(false); }
		 */

		// use the offset to the raw measurements
		// RTT+=AbstractNNSearchManager.offsetLatency;

		AddressIF farthestNode = Ninaloader.me;

		if (nps != null && !nps.isEmpty()) {

			Iterator<NodesPair> ier = nps.iterator();

			double total_value = 0.0;
			int count = 0;
			// ============================================
			while (ier.hasNext()) {
				NodesPair tmp = ier.next();
				AddressIF start = (AddressIF) tmp.startNode;
				if (tmp.rtt < 0) {
					continue;
				}
				if (forbidden.checkNode(start)) {
					continue;
				}
				/*
				 * if(!target.equals(tmp.endNode)){ System.err.println(
				 * "\n================\nERROR, ClosestNodeRule is broken! !target.equals(tmp.endNode)!\n================\n "
				 * ); continue; }
				 */

				total_value += tmp.rtt + AbstractNNSearchManager.offsetLatency;
				count++;

				if (tmp.rtt + AbstractNNSearchManager.offsetLatency > LargestLatency) {
					LargestLatency = tmp.rtt + AbstractNNSearchManager.offsetLatency;
					farthestNode = start;
				}
			}
			// =============================================
			double avgRTT = total_value / count;

			double RTT_Threshold = (1 + betaRatioFarthest) * avgRTT;

			if (RTT_Threshold < RTT) {
				System.err.println("We can not find farther ring members!");
				RTT_Threshold = (1 + betaRatioFarthest) * RTT;
			}

			// log.info(msg);
			if (LargestLatency < RTT_Threshold) {

				if (useAdaptiveSearch) {
					// yes, smaller than the cutoff
					if ((LargestLatency > RTT)
							&& (LargestLatency > (FarthestBetaCutoff * RTT_Threshold / (1 + betaRatioFarthest)))) {
						return farthestNode;
					}
				}

				// add all nodes that are lower than avgRTT
				ier = nps.iterator();
				while (ier.hasNext()) {
					NodesPair tmp = ier.next();
					if ((tmp.rtt + AbstractNNSearchManager.offsetLatency) > RTT) {
						// yes, we found a target node, that is closer than us
						AddressIF addr = (AddressIF) tmp.startNode;
						closestNodes.add(new NodesPair(addr, null, tmp.rtt, Ninaloader.me));
					}
				}
				// =======================================
				// I am the closest node, no other nodes
				closestNodes
						.add(new NodesPair(Ninaloader.me, null, RTT - AbstractNNSearchManager.offsetLatency, lastHop));
				return null;

			} else {
				// find a node
				//
				return farthestNode;

			}

		} else {
			closestNodes.add(new NodesPair(Ninaloader.me, null, RTT - AbstractNNSearchManager.offsetLatency, lastHop));
			return null;
		}
	}

	/**
	 * perform probing target in the reverse direction, we use the target to
	 * probe the querying nodes, in this way, we wish to void the TIMEOUT of
	 * AnswerUpdateRequestMsg
	 * 
	 * @param ringMembers
	 * @param target
	 * @param cbCoords
	 */
	public void reverseTargetProbe(Vector<AddressIF> ringMembers, AddressIF target,
			final CB2<Set<NodesPair>, String> cbCoords) {

	}

	/**
	 * get basic parameters
	 * 
	 * @param ringMembers
	 * @param minL
	 * @param avgL
	 * @param maxL
	 */
	public void getMaxAndMinAndAverage(Vector<AddressIF> ringMembers, double[] minL, double[] avgL, double[] maxL) {
		if (ringMembers == null || ringMembers.isEmpty()) {
			minL[0] = -1;
			avgL[0] = -1;
			maxL[0] = -1;
			return;
		}
		maxL[0] = 0;
		minL[0] = Double.MAX_VALUE;
		avgL[0] = 0;
		int count = 0;
		double[] tmp = new double[1];
		Iterator<AddressIF> ier = ringMembers.iterator();
		while (ier.hasNext()) {
			AddressIF addr = ier.next();
			if (isCached(addr, tmp)) {
				count++;
				if (tmp[0] > maxL[0]) {
					maxL[0] = tmp[0];
				}
				if (tmp[0] < minL[0]) {
					minL[0] = tmp[0];
				}
				avgL[0] += tmp[0];

			} else {
				// assert(false);
			}
		}
		//
		if (count > 0) {
			avgL[0] /= count;
		}
	}

	/**
	 * probe the target, if the target is cached, and some (or all) ring
	 * memebers are cached, then we reuse the measurements, we use the timer to
	 * remove old records, by default, 30 secs are used, 30*60*1000 TODO: fix
	 * upper bound for the probing process, to avoid the overflowing problem
	 * 
	 * @param ringMembers
	 * @param target
	 * @param cbCoords
	 * @param cbCoords_Global
	 */
	public static void TargetProbes(Collection<AddressIF> ringMember, AddressIF target, final CB1<String> cbCoords,
			final CB2<Set<NodesPair>, String> cbCoords_Global) {

		final Set<NodesPair> nodesPairSet = new HashSet<NodesPair>(5);
		final Vector<AddressIF> nodes = new Vector<AddressIF>(1);
		nodes.addAll(ringMember);

		// failure
		if (nodes == null || nodes.size() == 0) {
			String errorString = "getRemoteCoords: no valid nodes";
			log.warn(errorString);
			nodesPairSet.clear();
			cbCoords.call(CBResult.ERROR(), errorString);
			// callback function also returned
			cbCoords_Global.call(CBResult.ERROR(), nodesPairSet, errorString);
			return;
		}

		// use cached
		final long measuredResultsPeriod = 60 * 1000;
		long timeStamp = System.currentTimeMillis();
		final long timerCode = System.currentTimeMillis();
		// ask the target to measure distances to ring Members
		if (reverseMeasurement) {

		} else {

			if (useCachedTargetProbes) {

				// reuse the probes towards a target, use the cached
				// measurements
				if (cachedTargetProbes.containsKey(target)) {
					Vector<record> cachedResults = cachedTargetProbes.get(target);
					Iterator<record> ier = cachedResults.iterator();
					while (ier.hasNext()) {
						record r = ier.next();
						if (timeStamp - r.timeStamp < measuredResultsPeriod) {
							nodesPairSet.add(new NodesPair(r.from, r.target, r.rtt));
							// from node
							nodes.remove(r.from);
						} else {
							ier.remove();
						}
					}
					if (cachedTargetProbes.get(target) == null || cachedTargetProbes.get(target).isEmpty()) {
						cachedTargetProbes.remove(target);
					}
				}

				// if full, we return immediately
				if (nodes.isEmpty()) {
					String errorString = "";
					cbCoords.call(CBResult.OK(), errorString);
					// callback function also returned
					cbCoords_Global.call(CBResult.OK(), nodesPairSet, errorString);
					return;
				}

			}

			// indirect ping
			// register the CB2 function to global variables
			// uniqueCode, Timer

			PendingRecord pr1 = new PendingRecord(cbCoords_Global, nodes.size());
			pr1.timeStamp = System.currentTimeMillis();
			PendingPingRecordNodes.put(Long.valueOf(timerCode), pr1);
			// pending
			PendingPingRecordNodes.get(Long.valueOf(timerCode)).addAllNodes(nodes);

			log.debug("TargetProbe: " + nodes.size());

			// send requests
			// Barrier barrierUpdate = new Barrier(true);
			// long interval=2*1000;

			// if multiple nodes are requested, may be overflow!
			final StringBuffer errorBuffer = new StringBuffer();

			for (AddressIF addr : nodes) {
				int[] repeated = { 1 };
				updateRTT(timerCode, addr, target, errorBuffer, repeated);
			}

			nodes.clear();
			/*
			 * EL.get().registerTimerCB(barrierUpdate, new CB0() { protected
			 * void cb(CBResult result) { nodes.clear(); String errorString; if
			 * (errorBuffer.length() == 0) { errorString = new
			 * String("Success"); } else { errorString = new
			 * String(errorBuffer); } // only return cbCoords
			 * cbCoords.call(CBResult.OK(), errorString); } });
			 */

		}
		// timeout the target probe
		// long timeoutTargetProbe=10*1000;

		// no timeout
		// EL.get().registerTimerCB(timeout,new CB0(){
		// timeout setUp
		EL.get().registerTimerCB((long) timeoutTargetProbe, new CB0() {

			protected void cb(CBResult result) {
				// TODO Auto-generated method stub

				// contains the element, and has not been called
				if (PendingPingRecordNodes.containsKey(Long.valueOf(timerCode))
						&& !PendingPingRecordNodes.get(Long.valueOf(timerCode)).hasCalled) {
					// update the pending list
					log.warn(" timeout, we start the callbackfunction");
					Set<NodesPair> cachedNPS = new HashSet<NodesPair>(5);

					if (PendingPingRecordNodes.containsKey(Long.valueOf(timerCode))
							&& PendingPingRecordNodes.get(Long.valueOf(timerCode)).nps != null) {

						synchronized (PendingPingRecordNodes.get(Long.valueOf(timerCode)).nps) {

							if (PendingPingRecordNodes.containsKey(Long.valueOf(timerCode))
									&& PendingPingRecordNodes.get(Long.valueOf(timerCode)).nps != null) {
								cachedNPS.addAll(PendingPingRecordNodes.get(Long.valueOf(timerCode)).nps);
								PendingPingRecordNodes.get(Long.valueOf(timerCode)).cbFunction.call(CBResult.OK(),
										cachedNPS, new String("Success"));
								if (PendingPingRecordNodes.containsKey(Long.valueOf(timerCode))) {
									PendingPingRecordNodes.get(Long.valueOf(timerCode)).hasCalled = true;
								}

							}
						}
						// remove ca;;back function
						PendingPingRecordNodes.remove(Long.valueOf(timerCode));
						cachedNPS.clear();
					}

				} // -----------------------------

			}
		});

	}

	/**
	 * probe the targets
	 * 
	 * @param ringMember
	 * @param targets
	 * @param cbCoords
	 * @param cbCoords_Global
	 */
	public static void TargetProbes(Collection<AddressIF> ringMember, ArrayList<AddressIF> targets,
			final CB1<String> cbCoords, final CB2<Set<NodesPair>, String> cbCoords_Global) {

		final Set<NodesPair> nodesPairSet = new HashSet<NodesPair>(5);
		final Vector<AddressIF> nodes = new Vector<AddressIF>(1);
		nodes.addAll(ringMember);

		// failure
		if (nodes == null || nodes.size() == 0) {
			String errorString = "getRemoteCoords: no valid nodes";
			log.warn(errorString);
			nodesPairSet.clear();
			cbCoords.call(CBResult.ERROR(), errorString);
			// callback function also returned
			cbCoords_Global.call(CBResult.ERROR(), nodesPairSet, errorString);
			return;
		}

		// use cached
		final long measuredResultsPeriod = 60 * 1000;
		long timeStamp = System.currentTimeMillis();
		final long timerCode = System.currentTimeMillis();
		// ask the target to measure distances to ring Members
		if (reverseMeasurement) {

		} else {

			// indirect ping
			// register the CB2 function to global variables
			// uniqueCode, Timer

			PendingRecord pr1 = new PendingRecord(cbCoords_Global, nodes.size());
			pr1.timeStamp = System.currentTimeMillis();
			PendingPingRecordNodes.put(Long.valueOf(timerCode), pr1);

			log.debug("TargetProbe: " + nodes.size());

			// send requests
			final Barrier barrierUpdate = new Barrier(true);
			// long interval=2*1000;

			final StringBuffer errorBuffer = new StringBuffer();

			// for each target, ask nodes to ping them

			/*
			 * for(AddressIF target : targets){ for (AddressIF addr : nodes) {
			 * updateRTT(timerCode, addr, target, errorBuffer); } }
			 */
			// barrierUpdate.setNumForks(nodes.size());
			for (AddressIF addr : nodes) {

				// barrierUpdate.fork();
				AnswerUpdateRequestMsg msg = new AnswerUpdateRequestMsg(timerCode, Ninaloader.me, targets);
				measureComm.sendRequestMessage(msg, AddressFactory.create(addr, measureComm.getLocalPort()),
						new ObjCommRRCB<AnswerUpdateResponseMsg>() {
							protected void cb(CBResult result, final AnswerUpdateResponseMsg responseMsg,
									AddressIF node, Long ts) {
								switch (result.state) {
								case OK: {
									log.debug(
											"$ ! We have received AnswerUpdateRequestMsg ACK from" + responseMsg.from);
									break;
								}
								case ERROR:
								case TIMEOUT: {
									String error = "#: AnswerUpdateRequestMsg " + "has not received requirement, as: "
											+ result.toString();
									log.warn(error);

									break;
								}
								}
								// barrierUpdate.join();
							}
						});
				// msg.clear();
			}

			nodes.clear();
			/*
			 * EL.get().registerTimerCB(barrierUpdate, new CB0() { protected
			 * void cb(CBResult result) {
			 */

			// timeout the target probe
			// long timeoutTargetProbe=10*1000;

			// no timeout
			// EL.get().registerTimerCB(timeout,new CB0(){
			// timeout setUp
			EL.get().registerTimerCB(Math.round(timeoutTargetProbe), new CB0() {

				protected void cb(CBResult result) {
					// TODO Auto-generated method stub
					// nodes.clear();
					log.warn("Timeout, timeoutTargetProbe!");
					// contains the element, and has not been called
					if (PendingPingRecordNodes.containsKey(Long.valueOf(timerCode))
							&& !PendingPingRecordNodes.get(Long.valueOf(timerCode)).hasCalled) {
						// update the pending list
						log.info(" timeout, we start the callbackfunction");

						Set<NodesPair> cachedNPS = new HashSet<NodesPair>(5);

						// contsins the record
						if (PendingPingRecordNodes.containsKey(Long.valueOf(timerCode))) {

							if (PendingPingRecordNodes.get(Long.valueOf(timerCode)).nps == null
									|| PendingPingRecordNodes.get(Long.valueOf(timerCode)).nps.isEmpty()) {
								log.warn("PendingPingRecordNodes does not contain any records"
										+ Long.valueOf(timerCode));
								PendingPingRecordNodes.get(Long.valueOf(timerCode)).cbFunction.call(CBResult.ERROR(),
										cachedNPS, new String("failed"));
								PendingPingRecordNodes.remove(Long.valueOf(timerCode));

							} else {

								if (PendingPingRecordNodes.get(Long.valueOf(timerCode)).nps != null
										&& !PendingPingRecordNodes.get(Long.valueOf(timerCode)).nps.isEmpty()) {
									synchronized (PendingPingRecordNodes.get(Long.valueOf(timerCode)).nps) {
										cachedNPS.addAll(PendingPingRecordNodes.get(Long.valueOf(timerCode)).nps);
									}

									PendingPingRecordNodes.get(Long.valueOf(timerCode)).cbFunction.call(CBResult.OK(),
											cachedNPS, new String("Success"));
									PendingPingRecordNodes.get(Long.valueOf(timerCode)).hasCalled = true;
									//
									if (cachedNPS != null) {
										cachedNPS.clear();
									}

								}
								// remove callback function
								PendingPingRecordNodes.remove(Long.valueOf(timerCode));
							}

						}

					} // -----------------------------

				}
			});

			/*
			 * String errorString; if (errorBuffer.length() == 0) { errorString
			 * = new String("Success"); } else { errorString = new
			 * String(errorBuffer); }
			 * 
			 * 
			 * // only return cbCoords cbCoords.call(CBResult.OK(),
			 * errorString); } });
			 */

		}

	}

	/*
	 * 
	 * 
	 * //add the timeout interval double[] minL=new double[1]; double[] avgL=new
	 * double[1]; double[]maxL=new double[1];
	 * getMaxAndMinAndAverage(ringMembers,minL,avgL,maxL); double
	 * interval=3*1000; if(maxL[0]>0){ interval= }
	 * 
	 * 
	 * 
	 */

	/**
	 * ask node addr2 to measure target
	 * 
	 * @param addr2
	 * @param target
	 * @param barrierUpdate
	 * @param errorBuffer2
	 */
	public static void updateRTT(final long timerCode, final AddressIF remNode, final AddressIF target,
			final StringBuffer errorBuffer2, final int[] repeated) {
		// TODO Auto-generated method stub

		// repeated 3 times
		if (Ninaloader.me.equals(remNode) || remNode.equals(target)) {
			// we can not ask me to ping (already), or self ping of target
			// (selfloop)
			PendingPingRecordNodes.get(Long.valueOf(timerCode)).deleteNode(remNode);
			PendingPingRecordNodes.get(Long.valueOf(timerCode)).PendingProbes--;
			return;
		}

		UpdateRequestMsg msg = new UpdateRequestMsg(Ninaloader.me, target, timerCode);
		// System.out.println("ask " + remNode + " to update rtt");

		// todo, change the communication channel

		measureComm.sendRequestMessage(msg, AddressFactory.create(remNode, measureComm.getLocalPort()),
				new ObjCommRRCB<UpdateResponseMsg>() {
					protected void cb(CBResult result, final UpdateResponseMsg responseMsg, AddressIF node, Long ts) {
						switch (result.state) {
						case OK: {
							log.debug("$ ! We have received ACK from" + responseMsg.from);
							break;
						}
						case ERROR:
						case TIMEOUT: {
							String error = "#: updateRTT " + remNode.toString(false) + "has not been received , as: "
									+ result.toString();
							log.warn(error);
							/*
							 * repeated[0]++; updateRTT(timerCode, remNode,
							 * target, errorBuffer2,repeated);
							 */

							if (PendingPingRecordNodes.containsKey(Long.valueOf(timerCode))) {
								synchronized (PendingPingRecordNodes.get(Long.valueOf(timerCode))) {
									if (remNode != null) {
										PendingPingRecordNodes.get(Long.valueOf(timerCode)).deleteNode(remNode);
										PendingPingRecordNodes.get(Long.valueOf(timerCode)).PendingProbes--;
									}
								}
							}
							break;
						}
						}

						// barrier.join();
					}
				});
		msg.clear();
	}

	/**
	 * use the cache to speed up measurement
	 * 
	 * @param target
	 * @param lat
	 * @param curLatency
	 * @return
	 */
	public boolean isCached(AddressIF target, double[] lat) {
		if (!this.startMeridian && MClient.instance.g_rings.nodesCache.containsKey(target)) {
			lat[0] = MClient.instance.g_rings.nodesCache.get(target).getSample();
			return true;
		} else if (this.startMeridian && this.Meridian_MClient.instance.g_rings.nodesCache.containsKey(target)) {
			lat[0] = Meridian_MClient.instance.g_rings.nodesCache.get(target).getSample();
			return true;
		} else if (this.pendingNeighbors.containsKey(target)) {

			lat[0] = this.pendingNeighbors.get(target).getSample();
			return true;
		} else {
			lat[0] = -1;
			return false;
		}

	}

	/**
	 * collect RTTs from a set of nodes
	 * 
	 * @param nodes
	 * @param cbCoords
	 */
	public void collectRTTs(final Collection<AddressIF> oriLandmarks, final CB2<Set<NodesPair>, String> cbCoords) {

		final Set<NodesPair> nodesPairSet = new HashSet<NodesPair>(1);

		/**
		 * empty list
		 */
		if (oriLandmarks.size() == 0) {
			String errorString = "collectRTTs: no valid nodes";
			log.debug(errorString);
			cbCoords.call(CBResult.OK(), nodesPairSet, errorString);
			return;
		}

		final Barrier barrier = new Barrier(true); // Collect

		final StringBuffer errorBuffer = new StringBuffer();
		// System.out.println("$@ @ Collect from: "+pendingLandmarks.size()+"
		// nodes "+pendingLandmarks.toString());
		// when "for" is run over, the local node's nodesPairSet will get the
		// rtt informations from itself to the other nodes

		double[] latency = new double[1];
		latency[0] = -1;
		Collection<AddressIF> landmarks = new ArrayList<AddressIF>(2);
		landmarks.addAll(oriLandmarks);

		/**
		 * remove cached measurements
		 */
		for (AddressIF remNod : oriLandmarks) {

			if (remNod == null) {
				continue;
			}
			// me
			if (Ninaloader.me.equals(remNod)) {
				landmarks.remove(remNod);
				continue;
			} else
			// cached,
			if (isCached(remNod, latency)) {
				if (Double.isNaN(latency[0]) || latency[0] < 0) {
					continue;
				} else {
					nodesPairSet.add(new NodesPair(Ninaloader.me, remNod, latency[0]));

					landmarks.remove(remNod);
				}
				continue;
			}
		}

		/**
		 * remove all
		 */
		if (landmarks.isEmpty()) {
			String errorString = "collectRTTs: all are cached nodes";
			cbCoords.call(CBResult.OK(), nodesPairSet, errorString);
			return;
		} else {

			for (AddressIF addr : landmarks) {
				final AddressIF remNod = addr;
				// not cached
				barrier.fork();
				ncManager.doPing(remNod, new CB2<Double, Long>() {
					protected void cb(CBResult result, Double lat, Long timer) {
						// TODO Auto-generated method stub

						switch (result.state) {
						case OK: {
							if (!lat.isNaN() && lat.doubleValue() >= 0) {
								nodesPairSet.add(new NodesPair(Ninaloader.me, remNod, lat.doubleValue()));

								// addPendingNeighbor(remNod);
								// pendingNeighbors.get(remNod).addSample(lat.doubleValue());
								/*
								 * if(startMeridian){
								 * cachedHistoricalNeighbors.addElement(new
								 * NodesPair(remNod,Ninaloader.me,lat.
								 * doubleValue())); }
								 */
							}
							log.debug("$: Collect: doPing OK!");
							break;
						}
						case ERROR:
						case TIMEOUT: {
							String error = remNod.toString(false) + "has not received responses, as: "
									+ result.toString();
							log.debug(error);
							break;
						}
						}

						barrier.join();
					}
				});
			}
			// ================================

			// ============================================================
			final long interval = 10 * 1000;
			EL.get().registerTimerCB(barrier, new CB0() {
				protected void cb(CBResult result) {
					String errorString;
					if (errorBuffer.length() == 0) {
						errorString = new String("Success");
					} else {
						errorString = new String(errorBuffer);
					}
					log.debug("$@ @ Collect Completed");
					cbCoords.call(result, nodesPairSet, errorString);
				}
			});
		}
	}

	// After receiving the update request message from the core node,
	// the node which has been asked for update begins to collect rtts from
	// other nodes
	/*
	 * public void collectRTT(final AddressIF remNod, final Set<NodesPair>
	 * nodesPairSet, final Barrier barrier, final StringBuffer errorBuffer) {
	 * 
	 * if (Ninaloader.me.equals(remNod)) { return; }
	 * 
	 * KNNCollectRequestMsg msg = new KNNCollectRequestMsg(Ninaloader.me);
	 * msg.timeStamp = System.nanoTime(); // System.out.println(
	 * "try to get RTT from " // +Ninaloader.me+" to "+remNod );
	 * 
	 * barrier.fork(); comm.sendRequestMessage(msg, remNod, new
	 * ObjCommRRCB<KNNCollectResponseMsg>() { protected void cb(CBResult result,
	 * final KNNCollectResponseMsg responseMsg, AddressIF node, Long ts) {
	 * 
	 * switch (result.state) { case OK: { double rtt = (System.nanoTime() -
	 * responseMsg.timestamp) / 1000000d; // System.out.println("RTT of " + rtt
	 * +" from " // +Ninaloader.me+" to " + remNod);
	 * 
	 * nodesPairSet.add(new NodesPair(AddressFactory .create(Ninaloader.me),
	 * AddressFactory .create(remNod), rtt)); break; } case ERROR: case TIMEOUT:
	 * { String error = "RTT request to " + remNod.toString(false) + " failed:"
	 * + result.toString(); log.warn(error); if (errorBuffer.length() != 0) {
	 * errorBuffer.append(","); } errorBuffer.append(error); break; } }
	 * barrier.join(); } }); }
	 */

	// hander of CollectRequestMsg
	public class CollectHandler extends ResponseObjCommCB<KNNCollectRequestMsg> {

		public void cb(CBResult result, KNNCollectRequestMsg msg, AddressIF remoteAddr1, Long ts,
				final CB1<Boolean> cbHandled) {
			// System.out.println("in CollectHandler cb");
			// response
			KNNCollectResponseMsg msg1 = new KNNCollectResponseMsg(Ninaloader.me, msg.timeStamp);
			msg1.setResponse(true);
			msg1.setMsgId(msg.getMsgId());
			// System.out.println("$ @@ Maybe CollectRequest from:
			// "+remoteAddr1+", but from: "+msg.from);

			sendResponseMessage("Collect", msg.from, msg1, msg.getMsgId(), null, cbHandled);

		}
	}

	/**
	 * relative coordinate handler
	 * 
	 * @author ericfu
	 *
	 */
	public class RelativeCoordinateHandler extends ResponseObjCommCB<RelativeCoordinateRequestMsg> {

		public void cb(CBResult result, RelativeCoordinateRequestMsg msg, AddressIF remoteAddr1, Long ts,
				final CB1<Boolean> cbHandled) {
			// System.out.println("in CollectHandler cb");
			// // response
			// RelativeCoordinateResponseMsg msg1 = new
			// RelativeCoordinateResponseMsg(
			// Ninaloader.me,
			// instance.g_rings.myCoordinate,MClient.primaryNC.sys_coord,MClient.primaryNC.getSystemError());
			// msg1.setResponse(true);
			// msg1.setMsgId(msg.getMsgId());
			// // System.out.println("$ @@ Maybe CollectRequest from:
			// "+remoteAddr1+", but from: "+msg.from);
			//
			// sendResponseMessage("RelativeCoordinate", msg.from, msg1,
			// msg.getMsgId(),
			// null, cbHandled);

		}
	}

	/**
	 * 
	 * @author ericfu
	 *
	 */
	public class FinKNNHandler extends ResponseObjCommCB<FinFarthestRequestMsg> {
		public void cb(CBResult result, final FinFarthestRequestMsg fmsg, AddressIF remoteAddr, Long ts,
				final CB1<Boolean> cbHandled) {

			execKNN.execute(new Runnable() {

				public void run() {

					FinFarthestResponseMsg msg2 = new FinFarthestResponseMsg(Ninaloader.me);
					final long id = fmsg.getMsgId();
					sendResponseMessage("FinFarthestRequest", fmsg.from, msg2, id, null, cbHandled);

					final StringBuffer errorBuffer = new StringBuffer();

					if (fmsg.CMsg != null) {
						fmsg.CMsg.setFrom(Ninaloader.me);
						fmsg.CMsg.setRoot(Ninaloader.me);
						fmsg.CMsg.targetCoordinates = fmsg.targetCoordinates.makeCopy();

						fmsg.CMsg.totalSendingMessages = fmsg.totalSendingMessages;
						// hop records
						fmsg.CMsg.getHopRecords().add(Ninaloader.me);

						if (fmsg.CMsg.targets == null) {

							receiveClosestQuery(fmsg.CMsg.targetCoordinates, fmsg.CMsg, errorBuffer);
						} else {
							log.info("start multiObjective KNN search @receiveClosestQuery_MultipleTargets!");
							receiveClosestQuery_MultipleTargets(fmsg.CMsg.targetCoordinates, fmsg.CMsg, errorBuffer);
						}

					} else {

						fmsg.SCMsg.setFrom(Ninaloader.me);
						fmsg.SCMsg.setRoot(Ninaloader.me);
						// hop records
						fmsg.SCMsg.getHopRecords().add(Ninaloader.me);
						fmsg.SCMsg.targetCoordinates = fmsg.targetCoordinates.makeCopy();
						fmsg.SCMsg.totalSendingMessages = fmsg.totalSendingMessages;
						// ========================
						// filter based
						receiveClosestQuery(fmsg.SCMsg.targetCoordinates, fmsg.SCMsg, errorBuffer);

					}

				}
			});

		}

	}

	/*
	 * public abstract class MeasureResponseObjCommCB<T extends ObjMessageIF>
	 * extends ObjCommCB<T> {
	 * 
	 * void sendResponseMessage(final String handler, final AddressIF
	 * remoteAddr, final ObjMessage response, long requestMsgId, final String
	 * errorMessage, final CB1<Boolean> cbHandled) {
	 * 
	 * if (errorMessage != null) { log.warn(handler + " :" + errorMessage); }
	 * 
	 * comm.sendResponseMessage(response, remoteAddr, requestMsgId, new CB0() {
	 * protected void cb(CBResult sendResult) { switch (sendResult.state) { case
	 * TIMEOUT: case ERROR: { log.warn(handler + ": " + sendResult.what);
	 * return; } } } }); cbHandled.call(CBResult.OK(), true); } }
	 */

	// updateHandler
	// update request message received, and then send corresponding response
	// back
	public class updateTargetHandler extends ResponseObjCommCB_Measure<UpdateRequestMsg> {
		public void cb(CBResult result, final UpdateRequestMsg msg22, final AddressIF remoteAddr2, Long ts,
				final CB1<Boolean> cbHandled) {
			log.debug("in UpdateHandler cb");

			final AddressIF origin_remoteAddr = AddressFactory.create(msg22.from, comm.getLocalPort());
			final long msgID22 = msg22.getMsgId();
			// System.out.println("$ @@ Maybe UpdateRequest from:
			// "+remoteAddr2+" but is from "+origin_remoteAddr);

			UpdateResponseMsg msg1 = new UpdateResponseMsg(Ninaloader.me);
			// System.out.println("$ @@ Update acked to: "+origin_remoteAddr+"
			// With ID "+msgID22);

			sendResponseMessage("update", AddressFactory.create(msg22.from, measureComm.getLocalPort()), msg1, msgID22,
					null, cbHandled);

			final long timerCache = msg22.TimerCache;

			double[] lat = { -1 };

			/**
			 * the measurement is cached
			 */
			if (msg22.target != null && isCached(msg22.target, lat)) {

				double rtt = lat[0];

				Set<NodesPair> nps = new HashSet<NodesPair>(1);
				if (rtt >= 0) {
					nps.add(new NodesPair(Ninaloader.me, msg22.target, rtt));
				}
				DatRequestMsg datPayload = new DatRequestMsg(Ninaloader.me, nps, timerCache);
				log.debug("$ @@ Dat is sent to: " + origin_remoteAddr + " With ID " + msgID22);

				sendTargetMeasurement(datPayload, AddressFactory.create(origin_remoteAddr, measureComm.getLocalPort()));

			} else {

				// not cached
				ncManager.doPing(msg22.target, new CB2<Double, Long>() {

					protected void cb(CBResult result, Double lat, Long timer) {
						// TODO Auto-generated method stub
						double rtt = -1;
						final Set<NodesPair> nps = new HashSet<NodesPair>(1);
						switch (result.state) {
						case OK: {
							rtt = lat.doubleValue();

							if (rtt >= 0) {
								nps.add(new NodesPair(Ninaloader.me, msg22.target, rtt));
							}
							// TODO Auto-generated method stub
							DatRequestMsg datPayload = new DatRequestMsg(Ninaloader.me, nps, timerCache);
							log.debug("$ @@ Dat is sent to: " + origin_remoteAddr + " With ID " + msgID22);

							sendTargetMeasurement(datPayload,
									AddressFactory.create(origin_remoteAddr, measureComm.getLocalPort()));
							/*
							 * measureComm.sendRequestMessage(datPayload,
							 * AddressFactory.create(origin_remoteAddr,
							 * measureComm.getLocalPort()), new
							 * ObjCommRRCB<DatResponseMsg>() { protected void
							 * cb(CBResult result, final DatResponseMsg
							 * responseMsg22, AddressIF node, Long ts) { switch
							 * (result.state) { case OK: { log.debug(
							 * "$ ! Dat is ACKed from"+responseMsg22.from+
							 * ", by direct Ping"); break; } case ERROR: case
							 * TIMEOUT: { String error = origin_remoteAddr
							 * .toString(false) +
							 * "has not received datPayload, as: " +
							 * result.toString(); log.warn(error); break; } } }
							 * });
							 */

							break;
						}
						case ERROR:
						case TIMEOUT: {
							String error = origin_remoteAddr.toString(false) + "has not received requirement, as: "
									+ result.toString();
							log.warn(error);
							// TODO Auto-generated method stub
							DatRequestMsg datPayload = new DatRequestMsg(Ninaloader.me, nps, timerCache);
							log.debug("$ @@ Dat is sent to: " + origin_remoteAddr + " With ID " + msgID22);

							sendTargetMeasurement(datPayload,
									AddressFactory.create(origin_remoteAddr, measureComm.getLocalPort()));
							/*
							 * measureComm.sendRequestMessage(datPayload,
							 * AddressFactory.create(origin_remoteAddr,
							 * measureComm.getLocalPort()), new
							 * ObjCommRRCB<DatResponseMsg>() { protected void
							 * cb(CBResult result, final DatResponseMsg
							 * responseMsg22, AddressIF node, Long ts) { switch
							 * (result.state) { case OK: { log.debug(
							 * "$ ! Dat is ACKed from"+responseMsg22.from+
							 * ", by direct Ping"); break; } case ERROR: case
							 * TIMEOUT: { String error = origin_remoteAddr
							 * .toString(false) +
							 * "has not received datPayload, as: " +
							 * result.toString(); log.warn(error); break; } } }
							 * });
							 */
							break;
						}

						}
					}

				});

			}

		}
	}

	/**
	 * 
	 * @param msg
	 * @param origin_remoteAddr
	 */
	void sendTargetMeasurement(final DatRequestMsg datPayload, final AddressIF origin_remoteAddr) {

		/*
		 * if(datPayload.repeated>hasResendCouter){ log.warn(
		 * "sendTargetMeasurement failed"); return;
		 * 
		 * }else{
		 */

		log.debug("$ @@ Dat is sent to: " + origin_remoteAddr);

		measureComm.sendRequestMessage(datPayload, origin_remoteAddr, new ObjCommRRCB<DatResponseMsg>() {
			protected void cb(CBResult result, final DatResponseMsg responseMsg22, AddressIF node, Long ts) {
				switch (result.state) {
				case OK: {
					log.debug("$ ! Dat is ACKed from" + responseMsg22.from + ", by caching");
					break;
				}
				case ERROR:
				case TIMEOUT: {
					String error = origin_remoteAddr.toString(false) + "has not received datPayload, as: "
							+ result.toString();
					log.warn(error);
					// add counter
					/*
					 * datPayload.repeated++;
					 * sendTargetMeasurement(datPayload,origin_remoteAddr);
					 */

					break;
				}
				}
			}
		});
		// }

	}

	// handler of Dat message
	public class DatHandler extends ResponseObjCommCB_Measure<DatRequestMsg> {
		public void cb(CBResult result, DatRequestMsg msg33, AddressIF remoteAddr22, Long ts,
				final CB1<Boolean> cbHandled) {

			log.debug(Ninaloader.me + " has received rtts from " + msg33.from);
			// coreNode.nodesPairSet <- UpdateResponseMsg.latencySet (viz
			// remNode.nodesPairSet);
			final long timerCache = msg33.timerCached;

			if (!PendingPingRecordNodes.containsKey(Long.valueOf(timerCache))) {
				log.warn("no ping record");
				return;
			} else {
				synchronized (PendingPingRecordNodes.get(Long.valueOf(timerCache)).allNodes) {
					if (!PendingPingRecordNodes.get(Long.valueOf(timerCache)).allNodes.contains(msg33.from)) {
						log.warn("no record for remote node");
						return;
					}
				}
			}

			DatResponseMsg msg1 = new DatResponseMsg(Ninaloader.me);
			sendResponseMessage("Dat", AddressFactory.create(msg33.from, measureComm.getLocalPort()), msg1,
					msg33.getMsgId(), null, cbHandled);

			final Set<NodesPair> latencySet = msg33.latencySet;
			StringBuffer errorBuffer = new StringBuffer();

			final AddressIF answNode = AddressFactory.create(msg33.from);

			final long curr_time = System.currentTimeMillis();

			execKNN.execute(new Runnable() {
				@Override
				public void run() {

					if (PendingPingRecordNodes.containsKey(Long.valueOf(timerCache))) {

						synchronized (PendingPingRecordNodes.get(Long.valueOf(timerCache))) {

							// remove remote record
							if (PendingPingRecordNodes.containsKey(Long.valueOf(timerCache)) && answNode != null
									&& PendingPingRecordNodes.get(Long.valueOf(timerCache)).allNodes
											.contains(answNode)) {
								PendingPingRecordNodes.get(Long.valueOf(timerCache)).deleteNode(answNode);
							}
							// TODO Auto-generated method stub
							// contains the element, and has not been called
							if (PendingPingRecordNodes.containsKey(Long.valueOf(timerCache))
									&& !PendingPingRecordNodes.get(Long.valueOf(timerCache)).hasCalled) {

								PendingPingRecordNodes.get(Long.valueOf(timerCache)).PendingProbes--;
								// no records
								if (latencySet.isEmpty()) {
									// call
									// PendingPingRecordNodes.get(Long.valueOf(timerCache)).PendingProbes--;
									log.warn("dat does not contain measurement records");
									return;
								} else {

									if (latencySet.size() > 0) {

										synchronized (PendingPingRecordNodes.get(Long.valueOf(timerCache)).nps) {
											Iterator<NodesPair> ier = latencySet.iterator();
											while (ier.hasNext()) {
												NodesPair nxt = ier.next();

												// not numerical value
												if (Double.isNaN(nxt.rtt) || nxt.rtt < 0) {
													continue;
												}
												if (nxt != null
														&& PendingPingRecordNodes.containsKey(Long.valueOf(timerCache))
														&& PendingPingRecordNodes
																.get(Long.valueOf(timerCache)).nps != null) {
													PendingPingRecordNodes.get(Long.valueOf(timerCache)).nps
															.add(nxt.makeCopy());
												} else {
													continue;
												}

												// save
												// if(!cachedTargetProbes.containsKey(nxt.endNode)){
												// cachedTargetProbes.put(nxt.endNode,
												// new Vector<record>(10));
												// }
												// cachedTargetProbes.get(nxt.endNode).add(new
												// record(nxt.startNode,nxt.endNode,nxt.rtt,curr_time));
												//
											}
										}

									}
								}

								// update the pending list
								// we stop the updating process, when full
								if (PendingPingRecordNodes.get(Long.valueOf(timerCache)).PendingProbes == 0
										|| (System.currentTimeMillis() - PendingPingRecordNodes
												.get(Long.valueOf(timerCache)).timeStamp) >= timeoutTargetProbe) {

									log.debug("we start the callbackfunction, after "
											+ PendingPingRecordNodes.get(Long.valueOf(timerCache)).PendingProbes
											+ " left!");

									Set<NodesPair> cachedNPS = new HashSet<NodesPair>(5);

									// synchronized(PendingPingRecordNodes.get(Long.valueOf(timerCache)).nps){
									if (PendingPingRecordNodes.containsKey(Long.valueOf(timerCache))
											&& PendingPingRecordNodes.get(Long.valueOf(timerCache)).nps != null) {
										cachedNPS.addAll(PendingPingRecordNodes.get(Long.valueOf(timerCache)).nps);
										PendingPingRecordNodes.get(Long.valueOf(timerCache)).cbFunction
												.call(CBResult.OK(), cachedNPS, new String("Success"));
										// PendingPingRecordNodes.get(Long.valueOf(timerCache)).hasCalled
										// = true;
										// remove ca;;back function
										if (PendingPingRecordNodes.containsKey(Long.valueOf(timerCache))) {
											PendingPingRecordNodes.remove(Long.valueOf(timerCache));
										}

										cachedNPS.clear();
									}
									// }

								}

							} // -----------------------------

						}

					}
					// ========================================================

				}

			});

			// ---------------------------------

		}
	}

	// --------------------------------------------

	// ------------------------------------------------
	// probe to a set of nodes
	// update request message received, and then send corresponding response
	// back

	// ----------------------------------------------
	public class ClosestReqHandler extends ResponseObjCommCB<ClosestRequestMsg> {

		protected void cb(CBResult arg0, final ClosestRequestMsg arg1, AddressIF arg2, Long arg3,
				final CB1<Boolean> arg4) {

			/*
			 * execKNN.execute(new Runnable(){
			 * 
			 * 
			 * public void run() {
			 */

			// TODO Auto-generated method stub
			final AddressIF answerNod = arg1.getFrom();
			final AddressIF original = arg1.getOriginFrom();
			final AddressIF to = arg1.getTarget();
			// System.out.println("kNN Request from: "+answerNod);

			final long msgID = arg1.getMsgId();
			ClosestResponseMsg msg2 = new ClosestResponseMsg(original, Ninaloader.me);

			final StringBuffer errorBuffer = new StringBuffer();

			sendResponseMessage("Closest", answerNod, msg2, msgID, null, arg4);

			if (arg1.targets == null) {

				receiveClosestQuery(arg1.targetCoordinates, arg1, errorBuffer);
			} else {
				receiveClosestQuery_MultipleTargets(arg1.targetCoordinates, arg1, errorBuffer);
			}

			/*
			 * } });
			 */
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
	 * request the coordinate
	 * 
	 * @author visitor
	 *
	 */
	public class CoordGossipHandler extends ResponseObjCommCB_Gossip<CoordGossipRequestMsg> {
		protected void cb(CBResult result, CoordGossipRequestMsg msg, AddressIF arg2, Long arg3, CB1<Boolean> arg4) {
			// TODO Auto-generated method stub
			log.debug("in GossipHandler cb");

			// we just heard from him so we know he is up

			// translate the address, use comm's port
			final AddressIF answerNod = AddressFactory.create(msg.from, comm.getLocalPort());
			final long msgID = msg.getMsgId();

			if (!containinRing(answerNod)) {
				addPendingNeighbor(answerNod);
			}
			long curr_time = System.currentTimeMillis();

			sendResponseMessage("Gossip", AddressFactory.create(msg.from, gossipComm.getLocalPort()),
					new CoordGossipResponseMsg(MClient.primaryNC.getSystemCoords(), MClient.primaryNC.getSystemError(),
							MClient.primaryNC.getAge(curr_time)),
					msgID, null, arg4);

		}

	}

	/**
	 * answer farthest request similar with closest request handler procedure,
	 * but select farther nodes to ping the target
	 * 
	 * @author ericfu
	 * 
	 */

	public class GossipHandler extends ResponseObjCommCB_Gossip<KNNGossipRequestMsg> {

		// send a subset of nodes in the rings

		protected void cb(CBResult arg0, final KNNGossipRequestMsg arg1, AddressIF arg2, Long arg3, CB1<Boolean> arg4) {
			// TODO Auto-generated method stub

			final AddressIF answerNod = AddressFactory.create(arg1.from, comm.getLocalPort());
			final long msgID = arg1.getMsgId();

			final boolean cached = arg1.cached;
			Set<AddressIF> _nodes = new HashSet<AddressIF>(10);

			if (!AbstractNNSearchManager.startMeridian) {
				if (cached) {
					MClient.instance.g_rings.getRandomNodes(_nodes, defaultGossip);
				}
				// save remote node's coordinate
				// arg1.myCoordinate.r_error=arg1.myError;
				if (isQueryNode && !targetCandidates.contains(answerNod)) {
					targetCandidates.add(answerNod);
				}
				// ack my coordinate
				KNNGossipResponseMsg msg2 = new KNNGossipResponseMsg(_nodes, Ninaloader.me, arg1.cached,
						MClient.primaryNC.getSystemCoords(), MClient.primaryNC.getSystemError());
				msg2.sendingStamp = arg1.sendingStamp;
				msg2.NumOfNonEmptyRings = MClient.instance.g_rings.getCurrentNonEmptyRings();

				if (IsNonRingNode) {
					msg2.isNonRing = true;
				}

				sendResponseMessage("Gossip", AddressFactory.create(arg1.from, gossipComm.getLocalPort()), msg2, msgID,
						null, arg4);

			} else {
				if (cached) {
					Meridian_MClient.instance.g_rings.getRandomNodes(_nodes, defaultGossip);
				}

				/**
				 * Meridian method
				 */
				KNNGossipResponseMsg msg2 = new KNNGossipResponseMsg(_nodes, Ninaloader.me, arg1.cached, null, -1);
				sendResponseMessage("Gossip", AddressFactory.create(arg1.from, gossipComm.getLocalPort()), msg2, msgID,
						null, arg4);

			}

			execKNN.execute(new Runnable() {
				@Override
				public void run() {
					if (!AbstractNNSearchManager.startMeridian) {
						log.debug("addr: " + answerNod.toString());
						log.debug("coord: " + arg1.myCoordinate.toString());
						// coordinate update
						if (arg1.RTTValue > 0) {

							if (answerNod != null && !containinRing(answerNod)) {
								MClient.instance.processSample(answerNod, arg1.RTTValue, true,
										System.currentTimeMillis(), pendingNeighbors,
										AbstractNNSearchManager.offsetLatency);
								MClient.instance.g_rings.NodesCoords.put(answerNod, arg1.myCoordinate.makeCopy());
								MClient.primaryNC.simpleCoordinateUpdate(answerNod, arg1.myCoordinate,
										arg1.myCoordinate.r_error, arg1.RTTValue, System.currentTimeMillis());
							}

						}
					} else {

						if (!containinRing(answerNod)) {
							if (arg1.RTTValue > 0) {
								Meridian_MClient.instance.processSample(answerNod, arg1.RTTValue, true,
										System.currentTimeMillis(), pendingNeighbors,
										AbstractNNSearchManager.offsetLatency);
							}
						}

					}

					log.debug("has receive gossip from " + arg1.from.toString());

					// final Set<AddressIF> nodes=new HashSet<AddressIF>(10);
					if (arg1.nodes != null && !arg1.nodes.isEmpty()) {
						Iterator<AddressIF> ier = arg1.nodes.iterator();
						AddressIF node = null;
						while (ier.hasNext()) {
							node = ier.next();
							if (node.equals(Ninaloader.me)) {
								continue;
							}
							if (!containinRing(node)) {
								if (!pendingNeighbors.containsKey(node)) {
									addPendingNeighbor(node);
								}
							}
						}

					}

				}

			});

			// nodes.add(AddressFactory.create(answerNod));

			// only nodes that do not reside on rings
			/*
			 * if (!IsNonRingNode) { ping(pendingNeighbors.keySet(), false,
			 * null); }
			 */

			// TODO: MClient.instance.g_rings.insertNode(inNode, latencyUS)

		}

	}

	public class UpdateReqHandler extends ResponseObjCommCB_Measure<AnswerUpdateRequestMsg> {

		protected void cb(CBResult arg0, AnswerUpdateRequestMsg arg1, AddressIF arg2, Long arg3, CB1<Boolean> arg5) {
			// TODO Auto-generated method stub
			//
			// final AddressIF remNod22 = arg1.target;
			final ArrayList<AddressIF> targets = new ArrayList<AddressIF>(5);
			if (arg1.targets != null && !arg1.targets.isEmpty()) {
				targets.addAll(arg1.targets);
			}
			final AddressIF answerNod = AddressFactory.create(arg1.from, measureComm.getLocalPort());
			final long msgID = arg1.getMsgId();
			final CB1<Boolean> arg4 = arg5;

			final long timerCode = arg1.timerCode;

			sendResponseMessage("Update", AddressFactory.create(answerNod, measureComm.getLocalPort()),
					new AnswerUpdateResponseMsg(Ninaloader.me, -1), msgID, null, arg4);

			/*
			 * execKNN.execute(new Runnable(){
			 * 
			 * @Override public void run() {
			 */
			// TODO Auto-generated method stub

			collectRTTs(targets, new CB2<Set<NodesPair>, String>() {
				@Override
				protected void cb(CBResult result, Set<NodesPair> arg1, String arg2) {
					// TODO Auto-generated method stub

					switch (result.state) {
					case OK: {
						Set<NodesPair> records = arg1;

						DatRequestMsg datPayload = new DatRequestMsg(
								AddressFactory.create(Ninaloader.me, measureComm.getLocalPort()), records, timerCode);
						log.debug("$ @@ Dat is sent to: " + answerNod.toString());

						sendTargetMeasurement(datPayload, AddressFactory.create(answerNod, measureComm.getLocalPort()));
						/*
						 * measureComm.sendRequestMessage(datPayload,
						 * AddressFactory.create(answerNod,measureComm.
						 * getLocalPort()), new ObjCommRRCB<DatResponseMsg>() {
						 * protected void cb(CBResult result, final
						 * DatResponseMsg responseMsg22, AddressIF node, Long
						 * ts) { switch (result.state) { case OK: { log.debug(
						 * "$ ! Dat is ACKed from"+responseMsg22.from+
						 * ", by caching"); break; } case ERROR: case TIMEOUT: {
						 * String error = answerNod .toString(false) +
						 * "has not received datPayload, as: " +
						 * result.toString(); log.warn(error); break; } } } });
						 */

						// save the record
						// cachedHistoricalNeighbors.addElement(records);

						break;
					}
					default: {
						log.warn("collect rtts failed!");

						Set<NodesPair> nps = new HashSet<NodesPair>(1);
						DatRequestMsg datPayload = new DatRequestMsg(Ninaloader.me, nps, timerCode);
						// log.debug("$ @@ Dat is sent to: "+origin_remoteAddr+"
						// With ID "+msgID22);

						measureComm.sendRequestMessage(datPayload,
								AddressFactory.create(answerNod, measureComm.getLocalPort()),
								new ObjCommRRCB<DatResponseMsg>() {
							protected void cb(CBResult result, final DatResponseMsg responseMsg22, AddressIF node,
									Long ts) {
								switch (result.state) {
								case OK: {
									log.debug("$ ! Dat is ACKed from" + responseMsg22.from + ", by direct Ping");
									break;
								}
								case ERROR:
								case TIMEOUT: {
									String error = answerNod.toString(false) + "has not received datPayload, as: "
											+ result.toString();
									log.warn(error);
									break;
								}
								}
							}
						});

						break;

					}
					}
				}

			});

			/*
			 * } });
			 */

			/*
			 * if (remNod22.equals(Ninaloader.me)) {
			 * 
			 * sendResponseMessage("Update", answerNod, new
			 * AnswerUpdateResponseMsg(Ninaloader.me, -1), msgID, null, arg4); }
			 * else {
			 * 
			 * final GossipRequestMsg msg = new GossipRequestMsg(null,
			 * Ninaloader.me, false); msg.sendingStamp = System.nanoTime(); //
			 * System.out.println("try to get RTT from " //
			 * +MClient.instance.g_rendvNode+" to "+remNod );
			 * 
			 * comm.sendRequestMessage(msg, remNod22, new
			 * ObjCommRRCB<GossipResponseMsg>() { protected void cb(CBResult
			 * result, final GossipResponseMsg responseMsg, AddressIF node, Long
			 * ts) { double RTT = -1; switch (result.state) { case OK: {
			 * AddressIF remNod = responseMsg.from;
			 * 
			 * double rtt = (System.nanoTime() - responseMsg.sendingStamp) /
			 * 1000000d; long curr_time = System.currentTimeMillis();
			 * 
			 * if (responseMsg.isNonRing) { RTT = rtt; break; }
			 * 
			 * if (!pendingNeighbors.containsKey(remNod)) {
			 * 
			 * addPendingNeighbor(remNod);
			 * 
			 * } pendingNeighbors.get(remNod).addSample(rtt, curr_time); RTT =
			 * pendingNeighbors.get(remNod) .getSample(); break; } case ERROR:
			 * case TIMEOUT: { String error =
			 * "@@@@@@@@@@@@@@@@@@@@@@@@@\nRTT request failed:" +
			 * result.toString() + "\n@@@@@@@@@@@@@@@@@@@@@@@@@";
			 * log.debug(error); break; } } sendResponseMessage("Update",
			 * answerNod, new AnswerUpdateResponseMsg( Ninaloader.me, RTT),
			 * msgID, null, arg4);
			 * 
			 * }
			 * 
			 * });
			 * 
			 * }
			 */
		}
	}

	public abstract class ResponseObjCommCB<T extends ObjMessageIF> extends ObjCommCB<T> {

		protected void sendResponseMessage(final String handler, final AddressIF remoteAddr, final ObjMessage response,
				final long requestMsgId, final String errorMessage, final CB1<Boolean> cbHandled) {

			if (errorMessage != null) {
				log.warn(handler + " :" + errorMessage);
			}
			/*
			 * if(response.hasRepeated>hasResendCouter){ log.warn(
			 * "reponse failed"); return; }
			 */
			comm.sendResponseMessage(response, remoteAddr, requestMsgId, new CB0() {
				protected void cb(CBResult sendResult) {
					switch (sendResult.state) {
					case TIMEOUT:
					case ERROR: {
						log.warn(handler + ": " + sendResult.what);
						/*
						 * response.hasRepeated++; sendResponseMessage(handler,
						 * remoteAddr, response, requestMsgId, errorMessage,
						 * cbHandled);
						 */
						return;
					}
					}
				}
			});
			cbHandled.call(CBResult.OK(), true);
		}
	}

	// for gossip
	public abstract class ResponseObjCommCB_Gossip<T extends ObjMessageIF> extends ObjCommCB<T> {

		protected void sendResponseMessage(final String handler, final AddressIF remoteAddr, final ObjMessage response,
				final long requestMsgId, final String errorMessage, final CB1<Boolean> cbHandled) {

			if (errorMessage != null) {
				log.warn(handler + " :" + errorMessage);
			}

			/*
			 * if(response.hasRepeated>hasResendCouter){ log.warn(
			 * "response failed"); return; }
			 */

			gossipComm.sendResponseMessage(response, remoteAddr, requestMsgId, new CB0() {
				protected void cb(CBResult sendResult) {
					switch (sendResult.state) {
					case TIMEOUT:
					case ERROR: {
						log.warn(handler + ": " + sendResult.what);
						/*
						 * response.hasRepeated++; sendResponseMessage(handler,
						 * remoteAddr, response, requestMsgId, errorMessage,
						 * cbHandled);
						 */
						return;
					}
					}
				}
			});
			cbHandled.call(CBResult.OK(), true);
		}
	}

	public abstract class ResponseObjCommCB_Measure<T extends ObjMessageIF> extends ObjCommCB<T> {

		protected void sendResponseMessage(final String handler, final AddressIF remoteAddr, final ObjMessage response,
				final long requestMsgId, final String errorMessage, final CB1<Boolean> cbHandled) {

			if (errorMessage != null) {
				log.warn(handler + " :" + errorMessage);
			}
			/*
			 * if(response.hasRepeated>hasResendCouter){ log.warn(
			 * "reponse failed"); return; }
			 */
			measureComm.sendResponseMessage(response, remoteAddr, requestMsgId, new CB0() {
				protected void cb(CBResult sendResult) {
					switch (sendResult.state) {
					case TIMEOUT:
					case ERROR: {
						log.warn(handler + ": " + sendResult.what);
						/*
						 * response.hasRepeated++; sendResponseMessage(handler,
						 * remoteAddr, response, requestMsgId, errorMessage,
						 * cbHandled);
						 */
						return;
					}
					}
				}
			});
			cbHandled.call(CBResult.OK(), true);
		}
	}

	class PingRecord {
		AddressIF addr;
		long timeStamp;

		public PingRecord(AddressIF _addr, long _timeStamp) {
			addr = _addr;
			timeStamp = _timeStamp;
		}
	}

	/**
	 * ping without barrier
	 * 
	 * @param Nodes
	 * @param cached
	 * @param cbDone
	 */
	// void ping(Set<AddressIF> Nodes, final boolean cached,final CB0 cbDone){
	/*
	 * public void ping(Set<AddressIF> Nodes, final boolean cached, final
	 * CB1<Map<AddressIF, Double>> cbDone) {
	 */
	public void ping(Set<AddressIF> Nodes, final boolean cached) {

		/*
		 * final Map<AddressIF, Double> cachedPings = new HashMap<AddressIF,
		 * Double>( 2);
		 */
		// cachedPings.put(Ninaloader.me, Double.valueOf(-1));
		// todo ping only, no nodes
		if (Nodes == null || (Nodes != null) && (Nodes.isEmpty())) {
			System.err.println("Empty Ping List!");
			// cbDone.callOK();
			return;
		}
		if (Nodes.contains(Ninaloader.me)) {
			Nodes.remove(Ninaloader.me);
			if (Nodes.isEmpty()) {
				log.warn("After Remove myself, Empty Ping List!");
				// cbDone.callOK();
			}
			return;
		}
		// barrier

		// final Barrier barrier = new Barrier(true);
		final StringBuffer errorBuffer = new StringBuffer();

		for (final AddressIF remoteAddr_1 : Nodes) {
			// myself
			if (remoteAddr_1.equals(Ninaloader.me)) {
				continue;
			}
			// remote access
			// GossipWithNeighbor(barrier, cached, remoteAddr_1,
			// errorBuffer,cachedPings);
			GossipWithNeighbor(cached, remoteAddr_1, errorBuffer);

		}

		/*
		 * long interval=2*1000;
		 * 
		 * EL.get().registerTimerCB(barrier,new CB0() { protected void
		 * cb(CBResult result) {
		 * 
		 * // if not registered, we return if (cbDone == null) { return; } else
		 * { // not null String errorString; if (errorBuffer.length() == 0) {
		 * errorString = new String("Success"); } else { if (errorBuffer ==
		 * null) { System.err.println("NULL errorBuffer"); } // errorString =
		 * new String(errorBuffer); } cbDone.call(result, cachedPings);
		 * 
		 * }
		 * 
		 * } });
		 */

	}

	public void GossipWithNeighbor(final boolean cached, final AddressIF remoteAddr_1, final StringBuffer errorBuffer) {
		final Set<AddressIF> gossips = new HashSet<AddressIF>(10);
		// piggy-back
		if (cached) {

			/*
			 * if(gossips==null||((gossips!=null)&&gossips.size()==0)){ //init
			 * process, fill my address inside
			 * gossips.add(AddressFactory.create(Ninaloader.me)); }
			 */

		}

		// first ping the neighbor
		// =============================================

		final long sendStamp = System.nanoTime();
		ncManager.doPing(remoteAddr_1, new CB2<Double, Long>() {

			@Override
			protected void cb(CBResult result, Double latency, Long time) {
				// TODO Auto-generated method stub
				switch (result.state) {
				case OK: {
					final double[] RoundTripTime = { (System.nanoTime() - sendStamp) / 1000000d };
					if (latency.doubleValue() >= 0) {
						RoundTripTime[0] = latency.doubleValue();

					}

					// second, send gossip message
					// save the timer into records
					/*
					 * if(!PendingPingRecordNodes.containsKey(remoteAddr_1)){
					 * PendingPingRecordNodes.put(remoteAddr_1, new
					 * Vector<Long>(5)); }
					 */
					long timestamp = System.nanoTime();
					// PendingPingRecordNodes.get(remoteAddr_1).add(Long.valueOf(timestamp));
					KNNGossipRequestMsg gMsg = null;
					if (!startMeridian) {
						// select nodes from rings
						//
						MClient.instance.g_rings.getRandomNodes(gossips, defaultGossip);
						gossips.addAll(MClient.pickGossipNodes());

						gMsg = new KNNGossipRequestMsg(gossips,
								AddressFactory.create(Ninaloader.me, comm.getLocalPort()), cached,
								MClient.primaryNC.getSystemCoords(), MClient.primaryNC.getSystemError());
						gMsg.NumOfNonEmptyRings = MClient.instance.g_rings.getCurrentNonEmptyRings();

						gossips.clear();

					} else {
						Meridian_MClient.instance.g_rings.getRandomNodes(gossips, defaultGossip);
						gMsg = new KNNGossipRequestMsg(gossips, Ninaloader.me, cached, null, -1);
					}
					gMsg.sendingStamp = timestamp;
					gMsg.RTTValue = RoundTripTime[0];

					// barrier.fork();
					gossipComm.sendRequestMessage(gMsg, AddressFactory.create(remoteAddr_1, gossipComm.getLocalPort()),
							new ObjCommRRCB<KNNGossipResponseMsg>() {
						protected void cb(CBResult result, final KNNGossipResponseMsg resp, AddressIF remoteAddr22,
								Long ts) {
							switch (result.state) {
							case OK: {

								// final double appRTT = (System.nanoTime() -
								// resp.sendingStamp) / 1000000d;
								double appRTT = RoundTripTime[0];
								/*
								 * if (resp.isNonRing) {
								 * 
								 * cachedPings.put(resp.from,
								 * Double.valueOf(appRTT)); break; }
								 */

								// the node that send the message, not
								// remoteAddr22!!!
								final long curr_time = System.currentTimeMillis();
								final AddressIF remoteAddr = AddressFactory.create(resp.from, comm.getLocalPort());

								// add to the ring
								if (!remoteAddr.equals(Ninaloader.me)) {

									/*
									 * Ninaloader.execNina.execute(new
									 * Runnable(){
									 * 
									 * @Override public void run() {
									 */
									// TODO Auto-generated method stub

									/**
									 * huge
									 */
									if (appRTT > OUTRAGEOUSLY_LARGE_RTT) {
										log.warn("ignore huge RTT measurements");
										return;
									}

									// ====================================================
									// we get the latency
									// rtt=RoundTripTime[0];

									/*
									 * if
									 * (!pendingNeighbors.containsKey(remoteAddr
									 * )) {
									 * 
									 * addPendingNeighbor(remoteAddr); }
									 * 
									 * pendingNeighbors.get(remoteAddr).
									 * addSample(RoundTripTime[0], curr_time);
									 * 
									 * // System.out.println("Latency: "+Double
									 * // .valueOf(pendingNeighbors.get( //
									 * remoteAddr).getSample()));
									 * 
									 * cachedPings.put(remoteAddr, Double
									 * .valueOf(pendingNeighbors.get(
									 * remoteAddr).getSample()));
									 * 
									 * //double
									 * newRTTWithOffset=pendingNeighbors.get(
									 * remoteAddr).getSample()+
									 * AbstractNNSearchManager.offsetLatency;
									 * double
									 * newRTT=pendingNeighbors.get(remoteAddr).
									 * getSample();
									 * 
									 */

									execKNN.execute(new Runnable() {

										public void run() {

											/*
											 * if(!remoteAddr.equals(Ninaloader.
											 * me)){ //cache history
											 * cachedHistoricalNeighbors.
											 * addElement(new
											 * NodesPair<AddressIF>(remoteAddr,
											 * Ninaloader.me,RoundTripTime[0]));
											 * }
											 */

											/*
											 * if(isQueryNode){
											 * targetCandidates.add(remoteAddr);
											 * }
											 */
											/**
											 * Nina method
											 */
											if (!startMeridian) {
												// add the coordinate of remote
												// node
												resp.myCoordinate.r_error = resp.myError;
												MClient.instance.g_rings.NodesCoords.put(remoteAddr,
														resp.myCoordinate.makeCopy());

												// update my coordinate
												MClient.primaryNC.simpleCoordinateUpdate(remoteAddr, resp.myCoordinate,
														resp.myError, RoundTripTime[0], curr_time);

												MClient.instance.processSample(remoteAddr, RoundTripTime[0], true,
														curr_time, pendingNeighbors,
														AbstractNNSearchManager.offsetLatency);

												// instance.g_rings.addRelativeCoordinate(remoteAddr,
												// resp.myCoordinate.coords);
												// addNonEmptyNodes
												MClient.instance.g_rings.addNodesNumOfNonEmptyRing(remoteAddr,
														resp.NumOfNonEmptyRings);

												//

											}
											/**
											 * Meridian
											 */
											if (startMeridian) {
												// meridian
												Meridian_MClient.instance.processSample(remoteAddr, RoundTripTime[0],
														true, curr_time, pendingNeighbors, 0);
											}

											if (resp != null && !resp.nodes.isEmpty()) {

												// for nodes that are not in the
												// ring
												// Set<AddressIF> pending = new
												// HashSet<AddressIF>(10);

												Iterator<AddressIF> ier = resp.nodes.iterator();
												while (ier.hasNext()) {
													AddressIF tmp = ier.next();

													// if the node is not me,
													// nor is inserted
													// into rings before, add
													// into pending

													// -------------------------------------------
													// pending nodes are
													// inserted into temporary
													// lists
													// -----------------------------------

													if (tmp.equals(Ninaloader.me)) {
														continue;
													}
													if (!containinRing(tmp)) {
														addPendingNeighbor(tmp);
													}

												}

												// ping(pending, false, null);

											}

											// remove the pending target node
											if (pendingNeighbors.containsKey(remoteAddr)) {
												pendingNeighbors.remove(remoteAddr);
											}

										}
									});
									// =======================================================
									// run instance is finished
									/*
									 * }
									 * 
									 * });
									 */

								}
								// cbDone.callOK();
								break;
							}
							case TIMEOUT:

							case ERROR: {
								log.warn("Did not receive gossip response from " + remoteAddr_1);
								String error = "failed:" + result.toString();
								log.warn(error + " to measure: " + remoteAddr_1 + " from: " + Ninaloader.me);
								if (errorBuffer.length() != 0) {
									errorBuffer.append(",");
								}
								errorBuffer.append(error);

								final long expirationStamp = System.currentTimeMillis() - ConcentricRing.RS_EXPIRATION;
								if (pendingNeighbors.containsKey(remoteAddr_1)
										&& pendingNeighbors.get(remoteAddr_1).getLastUpdateTime() < expirationStamp) {
									log.warn("Remove from pending lists: " + remoteAddr_1);
									pendingNeighbors.remove(remoteAddr_1);
								} else {
									// add it to the pending nodes
									addPendingNeighbor(remoteAddr_1);
								}

								if (!startMeridian) {
									if (MClient.instance.g_rings.Contains(remoteAddr_1)
											&& MClient.instance.g_rings.nodesCache.get(remoteAddr_1)
													.getLastUpdateTime() < expirationStamp) {
										// log.debug("Remove from rings: " +
										// remoteAddr_1);

										AbstractNNSearchManager.lockRing2.lock();
										// lock
										try {
											MClient.instance.g_rings.eraseNode(remoteAddr_1);
										} finally {
											AbstractNNSearchManager.lockRing2.unlock();
										}
										// remove from the bin
										// instance.g_rings.binningProc.removeFromBins(remoteAddr_1);

									}

								}
								if (startMeridian) {
									AbstractNNSearchManager.lockRing2.lock();
									// lock
									try {
										if (Meridian_MClient.instance.g_rings.Contains(remoteAddr_1)) {
											Meridian_MClient.instance.g_rings.eraseNode(remoteAddr_1);
										}
									} finally {
										AbstractNNSearchManager.lockRing2.unlock();
									}

								}

								// remove nodes, that is not responsive
								/*
								 * int
								 * status=MClient.instance.g_rings.eraseNode(
								 * resp .from); if(status==0){ //successfully
								 * remove from rings
								 * pendingNeighbors.add(AddressFactory.create(
								 * resp .from)); }
								 */
								// MClient.instance.rs_map.remove(remoteAddr);
								// Remove the barrier to clear up state
								// barrier.remove();
								// cbDone.callERROR();
								break;
							}
							}

							if (gossips != null) {
								gossips.clear();
							}

							// barrier.join();
						}
					});

					/*
					 * }catch( ConcurrentModificationException e){
					 * e.printStackTrace(); }
					 */
					if (gMsg != null) {
						gMsg.clear();
					}
					break;
				}
				case ERROR:
				case TIMEOUT: {
					final long expirationStamp = System.currentTimeMillis() - ConcentricRing.RS_EXPIRATION;
					if (pendingNeighbors.containsKey(remoteAddr_1)
							&& pendingNeighbors.get(remoteAddr_1).getLastUpdateTime() < expirationStamp) {
						log.debug("Remove from pending lists: " + remoteAddr_1);
						pendingNeighbors.remove(remoteAddr_1);
					} else {
						// add it to the pending nodes
						addPendingNeighbor(remoteAddr_1);
					}

					if (!startMeridian) {
						if (MClient.instance.g_rings.Contains(remoteAddr_1) && MClient.instance.g_rings.nodesCache
								.get(remoteAddr_1).getLastUpdateTime() < expirationStamp) {
							log.debug("Remove from rings: " + remoteAddr_1);

							AbstractNNSearchManager.lockRing2.lock();
							// lock
							try {
								MClient.instance.g_rings.eraseNode(remoteAddr_1);
							} finally {
								AbstractNNSearchManager.lockRing2.unlock();
							}
							// remove from the bin
							// instance.g_rings.binningProc.removeFromBins(remoteAddr_1);

						}

					}
					if (startMeridian) {
						AbstractNNSearchManager.lockRing2.lock();
						// lock
						try {
							if (Meridian_MClient.instance.g_rings.Contains(remoteAddr_1)) {
								Meridian_MClient.instance.g_rings.eraseNode(remoteAddr_1);
							}
						} finally {
							AbstractNNSearchManager.lockRing2.unlock();
						}
					}
					break;
				}
				}
				// ===========================================================
			}
		});

	}

	void resetBootstrapNeighbors() {
		for (AddressIF remoteAddr : bootstrapAddrs) {
			if (!containinRing(remoteAddr)) {
				addPendingNeighbor(remoteAddr);
			}
		}
	}

	/**
	 * get the IP address
	 * 
	 * @param IPOrName
	 * @param port
	 * @param cbDone
	 */
	public void getAddressWithPort(String IPOrName, int port, final CB1<AddressIF> cbDone) {
		AddressFactory.createResolved(IPOrName, port, new CB1<AddressIF>() {
			@Override
			protected void cb(CBResult result, AddressIF arg1) {
				// TODO Auto-generated method stub
				cbDone.call(result, arg1);
			}
		});
	}

	/**
	 * test whether the ring has fullfilled the requirementsd
	 * 
	 * @return
	 */
	public boolean candoKNN() {
		if (MClient.instance.g_rings.CurrentNonEmptyRings >= defaultNonEmptyRingsForKNN) {
			return true;
		} else {
			// log.info("offset: "+offsetLatency);
			MClient.instance.g_rings.printAllRings();

			return false;
		}
	}

	/**
	 * maintain the rings
	 * 
	 * @param curr_time
	 */
	synchronized public void maintain(final long curr_time,
			final Map<AddressIF, RemoteState<AddressIF>> pendingNeighbors) {

		// maintain the pendingClosestQuery
		log.debug("$: maintain");

		execKNN.execute(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub

				/*
				 * // maintain if (curr_time - ncManager.lastUpdate >
				 * ConcentricRing.MAX_PING_RESPONSE_TIME) {
				 * ncManager.maintainPendingLatencies(curr_time); }
				 */

				final long expirationStamp = curr_time - ConcentricRing.RS_EXPIRATION;

				if (pendingNeighbors.size() > ConcentricRing.MAX_RS_MAP_SIZE) {
					ConcentricRing.RS_EXPIRATION = (long) (.9 * ConcentricRing.RS_EXPIRATION);
					if (ConcentricRing.VERBOSE)
						ConcentricRing.logger.info("lowered RS_EXPIRATION to " + ConcentricRing.RS_EXPIRATION + " size "
								+ pendingNeighbors.size());
				}

				Set<Map.Entry<AddressIF, RemoteState<AddressIF>>> states = pendingNeighbors.entrySet();

				for (Iterator<Map.Entry<AddressIF, RemoteState<AddressIF>>> stateIter = states.iterator(); stateIter
						.hasNext();) {
					Map.Entry<AddressIF, RemoteState<AddressIF>> entry = stateIter.next();
					if (entry.getValue().getLastUpdateTime() < expirationStamp) {
						if (ConcentricRing.VERBOSE)
							ConcentricRing.logger.info("tossing " + entry.getValue().getAddress());
						stateIter.remove();
					}
				}

				// iterate old records
				final Iterator<Long> iers = PendingPingRecordNodes.keySet().iterator();
				List<Long> toBeRemoved = new ArrayList<Long>(5);
				// find removed nodes
				while (iers.hasNext()) {
					Long tmp = iers.next();
					long oldTime = tmp.longValue();
					// has been called, removed directly
					if (PendingPingRecordNodes.get(Long.valueOf(oldTime)).hasCalled) {
						toBeRemoved.add(Long.valueOf(oldTime));

					}
					// or pending timeout
					else if ((oldTime + AbstractNNSearchManager.maxPendingMs) < curr_time) {
						toBeRemoved.add(Long.valueOf(oldTime));

					}
				}
				// remove
				if (toBeRemoved.size() > 0) {
					Iterator<Long> ier = toBeRemoved.iterator();
					while (ier.hasNext()) {
						Long oldTime = ier.next();
						if (PendingPingRecordNodes.get(Long.valueOf(oldTime)).hasCalled) {
							log.debug("####################\n sucessful! to be removed! @" + Ninaloader.me + ", knns: "
									+ PendingPingRecordNodes.get(Long.valueOf(oldTime)).nps.size()
									+ "\n####################");
							PendingPingRecordNodes.remove(Long.valueOf(oldTime));
						} else if ((oldTime + AbstractNNSearchManager.maxPendingMs) < curr_time) {

							synchronized (PendingPingRecordNodes.get(Long.valueOf(oldTime)).nps) {
								log.warn("####################\n timeout! to be removed! @" + Ninaloader.me
										+ ", pingRecords: "
										+ PendingPingRecordNodes.get(Long.valueOf(oldTime)).nps.size()
										+ "\n####################");
								Set<NodesPair> cachedNPS = new HashSet<NodesPair>(5);
								cachedNPS.addAll(PendingPingRecordNodes.get(Long.valueOf(oldTime)).nps);

								PendingPingRecordNodes.get(Long.valueOf(oldTime)).cbFunction.call(CBResult.OK(),
										cachedNPS, new String("failed"));
								// remove ca;;back function
								PendingPingRecordNodes.remove(Long.valueOf(oldTime));
							}
						}
					}

				}

				toBeRemoved.clear();
				toBeRemoved = null;
				// garbage collection
				// PUtil.gc();

				// System.gc();

			}
		});

	}

	public void addWarmExperience() {
		lockWarm.lock();
		try {
			warmCounter++;
			// warm period
		} finally {
			lockWarm.unlock();
		}
	}

	/**
	 * log the knn results
	 * 
	 * @param bestNodes
	 * @param logPerm
	 * @param closestNodes
	 * @param gains
	 * @param absoluteError
	 * @param relativeError
	 * @param totalSendingMessage
	 * @param ruuningTime
	 * @param successRatio
	 * @param ExpectedNumberOfK
	 * @param KNNresults
	 * @param target
	 * @param header
	 * @param coverageRate
	 * @throws IOException
	 */
	public void logKNNPerformance(SortedList<NodesPair<AddressIF>> bestNodes, BufferedWriter logPerm,
			double closestNodes, double gains, double absoluteError, double relativeError, int totalSendingMessage,
			double ruuningTime, double successRatio, int ExpectedNumberOfK, Collection<NodesPair> KNNresults,
			Object target, String header, double coverageRate) throws IOException {

		boolean isWarm = false;

		lockWarm.lock();
		try {
			if (warmCounter < warmPeriod) {
				isWarm = true;
			}
		} finally {
			lockWarm.unlock();
		}

		if (isWarm) {
			log.warn("warm " + warmCounter + ", expected " + warmPeriod);
			return;
		}
		String timeStamp = Ninaloader.getUptimeStr();// (System.currentTimeMillis()
														// -
														// Ninaloader.StartTime)
														// / 1000d;

		// Ninaloader.logKNN=null;
		try {

			// since me is also added
			if (KNNresults != null && !KNNresults.isEmpty()) {
				logPerm.append("#: " + header + "\n");
				logPerm.append("ExpectedNumberOfK: " + ExpectedNumberOfK + "\n");
				logPerm.append("returnedPercentage: " + KNNresults.size() / (ExpectedNumberOfK + 0.0) + "\n");
				if (target != null) {
					logPerm.append("Target: " + target.toString() + "\n");

				}
				logPerm.append("\nTimer: " + Ninaloader.getUptimeStr() + "\n");
				logPerm.append("Elapsed KNN search time: " + ruuningTime + "\n");

				// omit the result
				boolean shown = false;
				if (shown) {
					logPerm.append("closestNodes: " + closestNodes + "\n");
					logPerm.append("gains: " + gains + "\n");
					logPerm.append("absoluteError: " + absoluteError + "\n");
					logPerm.append("relativeError: " + relativeError + "\n");
					logPerm.append("successRatio: " + successRatio + "\n");

					if (!this.startMeridian) {
						logPerm.append("NonEmptyRing: " + MClient.instance.g_rings.getCurrentNonEmptyRings() + "\n");
					} else {
						/**
						 * TODO: log the Meridian non-empty ring
						 */
						logPerm.append(
								"NonEmptyRing: " + Meridian_MClient.instance.g_rings.getCurrentNonEmptyRings() + "\n");
					}

					if (!this.startMeridian) {
						logPerm.append("AllNodes: " + MClient.instance.g_rings.getAllNodes() + "\n");
					} else {
						/**
						 * TODO: log the Meridian nonempty ring
						 */
						logPerm.append("AllNodes: " + Meridian_MClient.instance.g_rings.getAllNodes() + "\n");
					}
				}

				// dual direction
				logPerm.append("totalSendingMessage: " + (totalSendingMessage) + "\n");
				// coverage
				logPerm.append("coverage: " + coverageRate + "\n");

				Iterator<NodesPair> ier = KNNresults.iterator();
				int ind = 0;
				NodesPair nxtNode;
				while (ier.hasNext()) {
					nxtNode = ier.next();
					ind++;
					// myself
					/*
					 * if (nxtNode.startNode.equals(target)) { continue; }
					 */

					logPerm.append(nxtNode.SimtoString() + "\n");
				}

				logPerm.flush();
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {

		}
		// start Meridian

		logCurrentBestAndWorstRecords(bestNodes, logPerm, timeStamp, ExpectedNumberOfK);

	}

	/**
	 * log the best nodes
	 * 
	 * @param bestNodes
	 * @param logPerm
	 * @param timeStamp
	 * @param K
	 */
	private void logCurrentBestAndWorstRecords(SortedList<NodesPair<AddressIF>> bestNodes, BufferedWriter logPerm,
			String timeStamp, int K) {
		// TODO Auto-generated method stub
		if (bestNodes == null || bestNodes.isEmpty()) {
			log.debug("the best Nodes is null");
			return;
		}
		try {

			logPerm.append("cached KNNs\n");
			/*
			 * SortedList<NodesPair<AddressIF>> tmpList = new
			 * SortedList<NodesPair<AddressIF>>(new NodesPairComp());
			 * tmpList.addAll(cachedHistoricalNeighbors.getBestKNN());
			 */
			Iterator<NodesPair<AddressIF>> ier = bestNodes.iterator();
			int counter = 0;
			while ((counter < K) && (ier.hasNext())) {
				NodesPair<AddressIF> rec = ier.next();
				logPerm.append(rec.startNode.toString() + " " + rec.rtt + "\n");

				if (!this.startMeridian) {
					// MClient.instance.processSample(rec.startNode, rec.rtt,
					// true, System.currentTimeMillis(), pendingNeighbors,
					// offsetLatency);
					askCoordinate(rec.startNode, rec.rtt, System.currentTimeMillis(), pendingNeighbors);
				}
				counter++;
			}
			logPerm.flush();
			// tmpList.clear();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * ask the coordinate of neighbors
	 * 
	 * @param pendingNeighbors2
	 * @param curr_time
	 * @param d
	 * @param startNode
	 */
	private void askCoordinate(final AddressIF neighbor, final double rtt, final long curr_time,
			final Map<AddressIF, RemoteState<AddressIF>> pendingNeighbors2) {
		// TODO Auto-generated method stub

		CoordGossipRequestMsg msg = new CoordGossipRequestMsg(Ninaloader.me);

		final long sendStamp = System.nanoTime();

		// log.info("Sending gossip request to " + neighbor);
		log.debug("CoordGossipRequestMsg: " + neighbor);
		gossipComm.sendRequestMessage(msg, AddressFactory.create(neighbor, gossipComm.getLocalPort()),
				new ObjCommRRCB<CoordGossipResponseMsg>() {

					protected void cb(CBResult result, final CoordGossipResponseMsg responseMsg, AddressIF remoteAddr,
							Long ts) {
						switch (result.state) {
						case OK: {
							execKNN.execute(new Runnable() {

								@Override
								public void run() {
									// TODO Auto-generated method stub

									// save the coordinate
									MClient.instance.g_rings.NodesCoords.put(neighbor, responseMsg.getRemoteCoord());

									MClient.instance.processSample(neighbor, rtt, true, curr_time, pendingNeighbors2,
											offsetLatency);
								}

							});

							break;
						}
						case ERROR:
						case TIMEOUT: {

							log.warn("can not find the coordinate for" + neighbor.toString());
							break;
						}

						}

					}

				});

	}

	public void askCoordinate(final AddressIF neighbor, final CB1<Coordinate> cbDone) {
		// TODO Auto-generated method stub

		CoordGossipRequestMsg msg = new CoordGossipRequestMsg(Ninaloader.me);

		final long sendStamp = System.nanoTime();

		// log.info("Sending gossip request to " + neighbor);
		log.debug("CoordGossipRequestMsg: " + neighbor);
		gossipComm.sendRequestMessage(msg, AddressFactory.create(neighbor, gossipComm.getLocalPort()),
				new ObjCommRRCB<CoordGossipResponseMsg>() {

					protected void cb(CBResult result, final CoordGossipResponseMsg responseMsg, AddressIF remoteAddr,
							Long ts) {
						switch (result.state) {
						case OK: {
							// save the coordinate
							cbDone.call(result, responseMsg.getRemoteCoord());
							break;
						}
						case ERROR:
						case TIMEOUT: {

							cbDone.call(result, null);
							log.warn("can not find the coordinate for" + neighbor.toString());
							break;
						}

						}

					}

				});

	}

	public class clusteringClient implements Runnable {

		final CB0 doHSHClustering;

		public clusteringClient() {
			doHSHClustering = new CB0() {
				protected void cb(CBResult result) {

					// log.info("$: Ready to do HSH clustering!");
					doHSHClustering();
				}
			};
		}

		void registerDoHSHClustering() {
			double rnd = Ninaloader.random.nextGaussian();
			long delay2 = RingUPDATE_DELAY + (long) (60 * 1000 * rnd);

			// log.debug("setting timer to " + delay2);
			EL.get().registerTimerCB(delay2, doHSHClustering);

		}

		public void run() {
			// TODO Auto-generated method stub
			init();

		}

		public void init() {
			registerDoHSHClustering();
		}

		// HSH clustering
		synchronized void doHSHClustering() {

			registerDoHSHClustering();

			final Vector<AddressIF> landmarks = new Vector<AddressIF>(10);
			final Vector<AddressIF> AllNodes = new Vector<AddressIF>(10);
			Set<AddressIF> randNodes = new HashSet<AddressIF>(5);

			MClient.instance.g_rings.getRandomNodes(randNodes, RecurRelaBasedHSH_Manager.defaultLandmarks);
			if (randNodes.size() < RecurRelaBasedHSH_Manager.defaultLandmarks) {
				log.info("The landmarks fail to reach " + RecurRelaBasedHSH_Manager.defaultLandmarks);
				return;
			}
			landmarks.addAll(randNodes);
			randNodes.clear();

			if (landmarks.size() <= 3) {
				log.info("The landmarks fail to reach " + 3);
				return;

			} else {
				// yes start
				randNodes.addAll(MClient.instance.g_rings.nodesCache.keySet());

				AllNodes.addAll(randNodes);
				randNodes.clear();
				// landmarks

				// log.info("\n\n$: All: "+AllNodes.size()+" landmarks
				// "+landmarks.size());

				Iterator<AddressIF> ier2 = landmarks.iterator();
				final Barrier barrier = new Barrier(true);
				final StringBuffer errorBuffer = new StringBuffer();
				final Set<NodesPair> cachedM = new HashSet<NodesPair>(10);

				// fork manually
				barrier.setNumForks(landmarks.size());
				barrier.activate();
				long time = 10 * 1000;
				while (ier2.hasNext()) {
					// barrier.join();
					TargetProbes(AllNodes, ier2.next(), new CB1<String>() {
						protected void cb(CBResult ncResult, String errorString) {
							switch (ncResult.state) {
							case OK: {
								// log.info("$: Target Probes are issued");
							}
							case ERROR:
							case TIMEOUT: {
								break;
							}
							}
						}
					}, new CB2<Set<NodesPair>, String>() {

						protected void cb(CBResult result, Set<NodesPair> arg1, String arg2) {
							// TODO Auto-generated method stub
							switch (result.state) {
							case OK: {
								cachedM.addAll(arg1);
								// log.info("Target probe is finished
								// successfully!");
								break;
							}
							case ERROR:
							case TIMEOUT: {

								// remove nodes,
								// MClient.instance.g_rings.eraseNode(remoteNode);
								// MClient.instance.rs_map.remove(remoteNode);
								// log.info("Target probe fails!");
								break;
							}
							}
							barrier.join();
						}

					});
				}

				// (2) reduceSetByN
				EL.get().registerTimerCB(barrier, new CB0() {
					protected void cb(CBResult result) {
						String errorString;
						if (errorBuffer.length() == 0) {
							errorString = new String("Success");
						} else {
							errorString = new String(errorBuffer);
						}

						execKNN.execute(new Runnable() {

							public void run() {

								// log.info("$@ @ ready for computing HSH
								// clustering with result: "+errorString);
								// TODO: assume that all nodes are alive here!
								List<AddressIF> allAliveNodes = new ArrayList<AddressIF>(5);
								List<AddressIF> allAliveLandmarkNodes = new ArrayList<AddressIF>(5);
								Iterator<NodesPair> ier3 = cachedM.iterator();
								Map<AddressIF, Integer> zeros = new HashMap<AddressIF, Integer>(5);
								Map<AddressIF, Integer> NonZeros = new HashMap<AddressIF, Integer>(5);
								// remove offline nodes
								while (ier3.hasNext()) {
									NodesPair np = ier3.next();
									if (Math.abs(np.rtt) < 0.0002) {
										if (!zeros.containsKey(np.endNode)) {
											zeros.put((AddressIF) np.endNode, Integer.valueOf(1));
										} else {
											int counters = zeros.get(np.endNode).intValue();
											counters++;
											zeros.put((AddressIF) np.endNode, Integer.valueOf(counters));
										}

									} else {
										if (!allAliveNodes.contains(np.startNode)) {
											allAliveNodes.add((AddressIF) np.startNode);
										}
										if (!allAliveLandmarkNodes.contains(np.endNode)) {
											allAliveLandmarkNodes.add((AddressIF) np.endNode);
										}

										if (!NonZeros.containsKey(np.endNode)) {
											NonZeros.put((AddressIF) np.endNode, Integer.valueOf(1));
										} else {
											int counters = NonZeros.get(np.endNode).intValue();
											counters++;
											NonZeros.put((AddressIF) np.endNode, Integer.valueOf(counters));
										}
									}

								}

								Iterator<Entry<AddressIF, Integer>> ierZeros = zeros.entrySet().iterator();
								// remove zero elements
								while (ierZeros.hasNext()) {

									Entry<AddressIF, Integer> t = ierZeros.next();
									if (t.getValue().intValue() > 0.3 * allAliveNodes.size()) {
										log.info("remove landmarks!" + t.getKey());
										allAliveLandmarkNodes.remove(t.getKey());
									}
								}

								Iterator<Entry<AddressIF, Integer>> ierNonZeros = NonZeros.entrySet().iterator();
								// remove zero elements
								while (ierNonZeros.hasNext()) {

									Entry<AddressIF, Integer> t = ierNonZeros.next();
									// fewer non-zero elements
									if (t.getValue().intValue() < 0.3 * allAliveNodes.size()) {
										log.info("remove landmarks!" + t.getKey());
										allAliveLandmarkNodes.remove(t.getKey());
									}
								}

								// row, column
								int row = allAliveNodes.size();
								int column = allAliveLandmarkNodes.size();
								log.info("$ after computation, row: " + row + ", column " + column);

								double[] latencyMatrix = new double[row * column];

								for (int i = 0; i < row; i++) {
									for (int j = 0; j < column; j++) {
										latencyMatrix[i * column + j] = 0;
									}
								}
								// -----------------------------------------
								ier3 = cachedM.iterator();

								int from = 0, to = 0;
								while (ier3.hasNext()) {
									NodesPair tmp = ier3.next();

									from = allAliveNodes.indexOf(tmp.startNode);
									if (from < 0) {
										continue;
									}
									// to = AllNodes.indexOf(tmp.endNode);
									to = allAliveLandmarkNodes.indexOf(tmp.endNode);
									if (to < 0) {
										continue;
									}
									// use offset
									latencyMatrix[from * column + to] = tmp.rtt + AbstractNNSearchManager.offsetLatency;
								}

								// do clustering

								if (!clusteringManager.getClusCache().containsKey(Ninaloader.me)) {
									clusteringRecord<AddressIF> tmp = new clusteringRecord<AddressIF>(
											new ClusteringSet(), MClient.instance);
									clusteringManager.getClusCache().put(Ninaloader.me, tmp);
								}
								Matrix_4_HSH tmp = clusteringManager.getRelativeCoordinates(Ninaloader.me,
										allAliveNodes, allAliveLandmarkNodes, latencyMatrix);

								clusteringManager.doInitialClustering(Ninaloader.me, tmp);

							}
						});
						// ======================================================

					}
				});
			}
		}

	}

	/**
	 * ask the history neighbors for me
	 * 
	 * @param target
	 * @param cbDone
	 */
	void askcachedHistoricalNeighborsOther(final AddressIF target, final int K,
			final CB1<HistoryNearestNeighbors<AddressIF>> cbDone) {

		askcachedHistoricalNeighborsRequestMsg req = new askcachedHistoricalNeighborsRequestMsg(Ninaloader.me, target,
				K);

		gossipComm.sendRequestMessage(req, AddressFactory.create(target, gossipComm.getLocalPort()),
				new ObjCommRRCB<askcachedHistoricalNeighborsResponseMsg>() {
					protected void cb(CBResult result, final askcachedHistoricalNeighborsResponseMsg resp,
							AddressIF remoteAddr22, Long ts) {

						switch (result.state) {
						case OK: {
							if (resp.currentBestNearestNeighbors == null
									|| resp.currentBestNearestNeighbors.isEmpty()) {
								log.warn("empty cachedHistoricalNeighbors");
								cbDone.call(CBResult.ERROR(), null);

							} else {
								HistoryNearestNeighbors<AddressIF> tmp = new HistoryNearestNeighbors<AddressIF>(target,
										K);
								Iterator<NodesPair<AddressIF>> ier = resp.currentBestNearestNeighbors.iterator();
								while (ier.hasNext()) {
									AddressIF resolvedAddr = ier.next().startNode;
									tmp.getRawsortedElements().add(resolvedAddr);
								}

								tmp.getCurrentBestNearestNeighbors().addAll(resp.currentBestNearestNeighbors);
								tmp.getCurrentWorstNearestNeighbors().addAll(resp.currentWorstNearestNeighbors);
								cbDone.call(result, tmp);
								resp.clear();

							}
							break;
						}
						case TIMEOUT:
						case ERROR: {
							log.warn("can not askcachedHistoricalNeighborsOther");
							cbDone.call(result, null);
							break;
						}
						}
					}
				});

	}

	public class askcachedHistoricalNeighborsHandler
			extends ResponseObjCommCB_Gossip<askcachedHistoricalNeighborsRequestMsg> {

		// send a subset of nodes in the rings

		protected void cb(CBResult arg0, askcachedHistoricalNeighborsRequestMsg arg1, AddressIF arg2, Long arg3,
				CB1<Boolean> arg4) {
			// TODO Auto-generated method stub

			final AddressIF answerNod = arg1.from;
			final long msgID = arg1.getMsgId();

			askcachedHistoricalNeighborsResponseMsg resp = new askcachedHistoricalNeighborsResponseMsg(Ninaloader.me,
					arg1.K);
			// resp.setcachedHistoricalNeighbors(cachedHistoricalNeighbors);

			sendResponseMessage("Gossip", AddressFactory.create(answerNod, gossipComm.getLocalPort()), resp, msgID,
					null, arg4);

		}

	}

	/**
	 * get one target
	 * 
	 * @param choiceOfTargets
	 * @param cbDone
	 */
	synchronized void getOneTarget(int choiceOfTargets, final CB1<ArrayList<AddressIF>> cbDone) {
		int realNum = Math.min(2, AllAlivecachedNodeStrings.size());
		if (realNum <= 0) {
			log.warn("can not find nodes!");
			cbDone.call(CBResult.ERROR(), null);
			return;
		}

		final ArrayList<AddressIF> nodes = new ArrayList<AddressIF>(choiceOfTargets);
		Collections.shuffle(AllAlivecachedNodeStrings);

		AddressFactory.createResolved(AllAlivecachedNodeStrings.subList(0, realNum), Ninaloader.COMM_PORT,
				new CB1<Map<String, AddressIF>>() {

					protected void cb(CBResult result, Map<String, AddressIF> addrMap) {
						switch (result.state) {
						case OK: {
							for (String remoteNode : addrMap.keySet()) {
								// System.out.println("remoteNode='" +
								// remoteNode + "'");
								AddressIF remoteAddr = addrMap.get(remoteNode);
								// we keep these around in case we run out of
								// neighbors in the future
								// not me
								// if(!remoteAddr.equals(Ninaloader.me)){
								nodes.add(remoteAddr);

								// }
							}

							cbDone.call(result, nodes);
							break;
						}
						case TIMEOUT:
						case ERROR: {
							log.error("Could not resolve target for Communication: " + result.what);
							cbDone.call(result, nodes);
							break;
						}
						}
					}
				});
	}

	/**
	 * use the
	 * 
	 * @return
	 */
	synchronized void findTwoCommunicationNodesFroDetour(int choiceOfTargets, final CB1<ArrayList<AddressIF>> cbDone) {

		final ArrayList<AddressIF> nodes = new ArrayList<AddressIF>(choiceOfTargets);

		Collections.shuffle(AllAlivecachedNodeStrings);
		int realNum = Math.min(choiceOfTargets, AllAlivecachedNodeStrings.size());
		if (realNum <= 0) {
			log.warn("can not find nodes!");
			cbDone.call(CBResult.ERROR(), nodes);
			return;
		}
		AddressFactory.createResolved(AllAlivecachedNodeStrings.subList(0, realNum), Ninaloader.COMM_PORT,
				new CB1<Map<String, AddressIF>>() {

					protected void cb(CBResult result, Map<String, AddressIF> addrMap) {
						switch (result.state) {
						case OK: {
							for (String remoteNode : addrMap.keySet()) {
								// System.out.println("remoteNode='" +
								// remoteNode + "'");
								AddressIF remoteAddr = addrMap.get(remoteNode);
								// we keep these around in case we run out of
								// neighbors in the future
								// not me
								// if(!remoteAddr.equals(Ninaloader.me)){
								nodes.add(remoteAddr);

								// }
							}

							cbDone.call(result, nodes);
							break;
						}
						case TIMEOUT:
						case ERROR: {
							log.error("Could not resolve target for Communication: " + result.what);
							cbDone.call(result, nodes);
							break;
						}
						}
					}
				});

		/*
		 * ArrayList<AddressIF> nodes =new
		 * ArrayList<AddressIF>(choiceOfTargets); List<AddressIF> list1=new
		 * ArrayList<AddressIF>(100);
		 * if(AllAliveNodes==null||AllAliveNodes.isEmpty()){ return nodes; }
		 * list1.addAll(AllAliveNodes); Collections.shuffle(list1);
		 * 
		 * int realNum=Math.min(choiceOfTargets, list1.size());
		 * Iterator<AddressIF> ier = list1.iterator(); while(ier.hasNext()){
		 * if(realNum==0){ break; }else{ AddressIF tmp = ier.next();
		 * nodes.add(tmp); } realNum--; } return nodes;
		 */
	}

	/**
	 * measure the control message
	 */
	private volatile double controlTraffic = 0;

	/**
	 * get the control traffic, and reset the counter
	 * 
	 * @return
	 */
	public double getControlTraffic() {

		double traffic = controlTraffic;
		controlTraffic = 0;
		// dual direction
		return traffic * 2;
	}

	/**
	 * add the control traffic
	 * 
	 * @param val
	 */
	public void addControlTraffic(double val) {
		controlTraffic += val;
	}

	public void logControlTraffic() {

		registerControlTrafficTimer();
		try {
			Ninaloader.logControlTraffic.append(Ninaloader.getUptimeStr() + " " + this.getControlTraffic() + "\n");
			Ninaloader.logControlTraffic.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * register the control traffic timer
	 */
	public void registerControlTrafficTimer() {
		// LOWTODO adaptive delay
		long delay = logControl_DELAY;

		// log.debug("setting timer to " + delay);
		EL.get().registerTimerCB(delay, tickControl);

	}

	/**
	 * whether in the ring set
	 * 
	 * @param target
	 * @return
	 */
	public boolean containinRing(AddressIF target) {
		if (!startMeridian) {
			return this.MClient.instance.g_rings.Contains(target);
		} else {
			return this.Meridian_MClient.instance.g_rings.Contains(target);
		}
	}

	/**
	 * current history
	 */
	// HistoryNearestNeighbors<AddressIF> cachedHistoricalNeighbors;
	/**
	 * maintain the history
	 */
	int fixedSize = Integer.parseInt(Config.getConfigProps().getProperty("fixedSize", "200"));

	static long totalSingleObjKNN = 0;
	static long totalMultObjKNN = 0;
	static long SucesstotalSingleObjKNN = 0;
	static long SucesstotalMultObjKNN = 0;

	// Meridian client
	public class KNN_NinaClient implements Runnable {

		public ConcentricRing<AddressIF> instance = null;

		/**
		 * update the coordinate, for selecting the closest neighbor for
		 * selecting the next hop node
		 */
		public final NCClient<AddressIF> primaryNC;

		final Collection<NodesPair> currentKNN;
		final Collection<NodesPair> currentKFarthestNodes;
		final Collection<NodesPair> currentConstrainedKNN;
		int K = NumberOfK;

		final CB0 nonRingKNNTest;
		double nonRingLastUpdate;

		final CB0 updateGosssipCB1;
		final CB0 updateRingMaintainCB2;
		final CB0 updateKNNTestCB3;
		final CB0 updateTestKNNBasedOnNinaForOtherCB3;
		final CB0 updateMultiObjectiveKNNTestCB3;
		final CB0 updateTestConstrainedMeridianCB;
		final CB0 directPingCB;
		final CB0 UpdateRelativeCoordinateCB2;

		final CB0 findKFarthestNodes;

		// int defaultGossipSize = 5;
		long delayInMs = 60; // for rate calc, 60
		RateCalc rcalc;

		// int[] knns={1,2,3,4,5,6,7,8,9,10};

		// double AvgPkts;

		public KNN_NinaClient() {

			primaryNC = new NCClient<AddressIF>(myNCDimension);

			if (instance == null) {
				instance = new ConcentricRing<AddressIF>(Ninaloader.me, nodesPerRing);
				// init the parameters
				// instance.g_rings.initBinningProcess(_binDim, _cutoff,
				// _listThreshold, _choiceOfNextHop, _RHO_INFRAMETRIC);
			}

			currentKNN = new ArrayList<NodesPair>(50);
			currentKFarthestNodes = new ArrayList<NodesPair>(50);

			currentConstrainedKNN = new ArrayList<NodesPair>(50);
			rcalc = new RateCalc(delayInMs);

			nonRingKNNTest = new CB0() {
				protected void cb(CBResult result) {

					testNonRingKNN();
				}

			};

			directPingCB = new CB0() {
				protected void cb(CBResult result) {
					pingRandomNodes();
				}

			};

			updateMultiObjectiveKNNTestCB3 = new CB0() {
				protected void cb(CBResult result) {
					execKNN.execute(new Runnable() {
						@Override
						public void run() {
							// TODO Auto-generated method stub
							TestMultiObjectiveKNNNina();

						}
					});

				}

			};

			updateGosssipCB1 = new CB0() {
				protected void cb(CBResult result) {

					performGossip(true);
				}

			};

			updateRingMaintainCB2 = new CB0() {
				protected void cb(CBResult result) {

					execKNN.execute(new Runnable() {

						public void run() {
							performRingManagement(RingMaintainSelection); // 0,
																			// random,1
							// meridian, 2, clustering based
						}
					});

				}
			};

			updateKNNTestCB3 = new CB0() {
				protected void cb(CBResult result) {
					log.debug("$: Ready for test Meridian!");

					TestKNNBasedOnNinaForMe();

				}
			};

			updateTestKNNBasedOnNinaForOtherCB3 = new CB0() {
				protected void cb(CBResult result) {
					log.debug("$: Ready for test Meridian!");

					TestKNNBasedOnNinaForOther();
				}
			};

			findKFarthestNodes = new CB0() {
				protected void cb(CBResult result) {
					log.debug("$: Ready for test Meridian!");
					TestKFarthestSearchBasedOnNina();

				}
			};

			UpdateRelativeCoordinateCB2 = new CB0() {
				protected void cb(CBResult result) {
					log.debug("$: Ready for test Meridian!");
					UpdateRelativeCoordinate();

				}
			};

			updateTestConstrainedMeridianCB = new CB0() {
				protected void cb(CBResult result) {
					// log.info("$: Ready for test Meridian!");
					// nodes in lists
					List<AddressIF> candidates = new ArrayList<AddressIF>(1);
					candidates.addAll(pickGossipNodes());
					int num = -1;
					if (candidates.size() > 3) {
						num = (int) Math.round(candidates.size() * 0.7);
					} else {
						num = candidates.size() - 1;
					}
					if (num <= 0) {
						log.info("Too few elements!");
						return;
					}
					/*
					 * log.info("$: Test ConstrainedMeridian" +
					 * POut.toString(candidates) + ", K=" + num);
					 */

					ConstrainedKNN(Ninaloader.me, candidates, num, new CB2<Vector<String>, Vector<Double>>() {

						protected void cb(CBResult result, Vector<String> arg1, Vector<Double> arg2) {
							// TODO Auto-generated method stub

						}
					});

				}
			};

		}

		void registerNonRingKNNTimer() {

			double rnd = Ninaloader.random.nextDouble();
			long delay2 = 5 * RingUPDATE_DELAY + (long) (60 * 1000 * rnd);

			// log.debug("setting timer to " + delay2);
			EL.get().registerTimerCB(delay2, nonRingKNNTest);
		}

		void registerGossipTimer() {
			// LOWTODO adaptive delay
			double rnd = Ninaloader.random.nextDouble();
			long delay = RingUPDATE_DELAY + (long) (5000 * rnd);

			// log.debug("setting timer to " + delay);
			EL.get().registerTimerCB(delay, updateGosssipCB1);

		}

		void registerRingManagerTimer() {
			double rnd = Ninaloader.random.nextDouble();
			long delay2 = 3 * veryLongUPDATE_DELAY + (long) (veryLongUPDATE_DELAY * rnd);

			// log.debug("setting timer to " + delay2);
			EL.get().registerTimerCB(delay2, updateRingMaintainCB2);

		}

		void registerUpdateRelativeCoordinateTimer() {
			double rnd = Ninaloader.random.nextDouble();
			long delay2 = RingUPDATE_DELAY + (long) (RingUPDATE_DELAY * rnd);

			// log.debug("setting timer to " + delay2);
			EL.get().registerTimerCB(delay2, UpdateRelativeCoordinateCB2);

		}

		void registerTestKNNBasdOnNina() {
			double rnd = Ninaloader.random.nextDouble();
			long delay2 = 10 * veryLongUPDATE_DELAY + (long) (5 * veryLongUPDATE_DELAY * rnd);

			// log.debug("setting timer to " + delay2);
			EL.get().registerTimerCB(delay2, updateKNNTestCB3);

		}

		void registerTestKNNBasedOnNinaForOther() {
			double rnd = Ninaloader.random.nextDouble();
			long delay2 = veryLongUPDATE_DELAY + (long) (veryLongUPDATE_DELAY * rnd);

			// log.debug("setting timer to " + delay2);
			EL.get().registerTimerCB(delay2, updateTestKNNBasedOnNinaForOtherCB3);

		}

		void registerTestKNNBasdOnMultiObjectiveNina() {
			double rnd = Ninaloader.random.nextDouble();
			long delay2 = veryLongUPDATE_DELAY + (long) (veryLongUPDATE_DELAY * rnd);

			// log.debug("setting timer to " + delay2);
			EL.get().registerTimerCB(delay2, updateMultiObjectiveKNNTestCB3);

		}

		void registerTestKFarthestNodeSearchBasdOnNina() {
			double rnd = Ninaloader.random.nextDouble();
			long delay2 = veryLongUPDATE_DELAY + (long) (veryLongUPDATE_DELAY * rnd);

			// log.debug("setting timer to " + delay2);
			EL.get().registerTimerCB(delay2, findKFarthestNodes);

		}

		void registerTestConstrainedMeridian() {
			double rnd = Ninaloader.random.nextDouble();
			long delay2 = veryLongUPDATE_DELAY + (long) (veryLongUPDATE_DELAY * rnd);

			// log.debug("setting timer to " + delay2);
			EL.get().registerTimerCB(delay2, updateTestConstrainedMeridianCB);
		}

		void registerDirectPing() {
			double rnd = Ninaloader.random.nextDouble();
			long delay2 = randomPing_DELAY + (long) (randomPing_DELAY * rnd);

			// log.debug("setting timer to " + delay2);
			EL.get().registerTimerCB(delay2, directPingCB);
		}

		// for nodes on the rings
		public void init() {
			// timer
			instance.lastMaintenanceStamp = System.currentTimeMillis();
			instance.g_rendvNode = Ninaloader.me;
			// timer

			// stop the ring management
			// registerRingManagerTimer();
			registerGossipTimer();
			registerTestKNNBasdOnNina();

			if (isQueryNode) {
				// if(singleTargetKNN){
				// registerDoHSHClustering();

				registerTestKNNBasedOnNinaForOther();
				// registerTestConstrainedMeridian();
				/*
				 * }else{ //multiobjective
				 * registerTestKNNBasdOnMultiObjectiveNina();
				 * 
				 * }
				 */
			}
			// registerUpdateRelativeCoordinateTimer();

			// registerTestKFarthestNodeSearchBasdOnNina();

			registerDirectPing();

			registerControlTrafficTimer();
			/*
			 * if(!Ninaloader.useConstrainedKNN) { registerTestMeridian();
			 * }else{ registerTestConstrainedMeridian(); }
			 */

			// registerTestMeridian1NN();

		}

		// for nodes that are not on rings
		public void initNonRing() {
			nonRingLastUpdate = System.currentTimeMillis();
			registerNonRingKNNTimer();

		}

		void UpdateRelativeCoordinate() {
			// update the relative coordinate
			/*
			 * refreshRelativeCoordinate();
			 * registerUpdateRelativeCoordinateTimer();
			 */
		}

		/**
		 * search k farthest nodes
		 */
		public void TestKFarthestSearchBasedOnNina() {

			// repeat
			registerTestKFarthestNodeSearchBasdOnNina();

			final long timer = System.currentTimeMillis();

			// if the size of currentKNN fullfil the number of KNN, return
			// immediately

			// use background to search, <Long,CBcached>
			/*
			 * Set<AddressIF> targets=new HashSet<AddressIF>(1);
			 * instance.g_rings.getRandomNodes(targets, 1);
			 * 
			 * 
			 * if(targets.isEmpty()){ return; }
			 * 
			 * final AddressIF target=targets.iterator().next();
			 */
			final AddressIF target = Ninaloader.me;
			// number of knns
			K = knns[Ninaloader.random.nextInt(knns.length)];

			queryKFarthestNeighbors(target, K, new CB1<List<NodesPair>>() {

				protected void cb(CBResult result, List<NodesPair> arg1) {

					switch (result.state) {
					case OK: {

						if (arg1 != null && arg1.size() > 0) {
							log.debug("$: " + arg1.size() + "farthest neighbors are as follows: ");
							// record
							try {
								String header = "\nTest K Farthest search!\n";
								// logKNNPerformance(K,arg1, target,header);
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							if (target.equals(Ninaloader.me)) {
								// record current KNN
								// currentKNN.clear();
								Iterator<NodesPair> ier = arg1.iterator();
								int ind = 0;
								long curr_time = System.currentTimeMillis();
								NodesPair nxtNode;
								log.info("KFN:\nExpected: " + K + ", real: " + arg1.size());
								while (ier.hasNext()) {
									nxtNode = ier.next();

									// instance.processSample((AddressIF)(nxtNode.startNode),
									// nxtNode.rtt, true, curr_time,
									// pendingNeighbors, offsetLatency);
									// askRemoteRelativeCoordinateIntoRing((AddressIF)(nxtNode.startNode));
									// myself
									ind++;
									// otherwise
									log.info(ind + "th FN: " + nxtNode.SimtoString());
									// currentKFarthestNodes.add(nxtNode.makeCopy());
									if (ind == K) {
										break;
									}
								}
							}
							// only success, we register
							// registerTestMeridian();
						} else {
							log.debug(
									"************************\n FAILED   K Farthest search!\nSet<NodesPair> is empty!\n************************");
						}

						break;
					}
					case TIMEOUT:
					case ERROR: {
						// registerTestMeridian();

						log.debug("************************\n FAILED  K Farthest search!\n************************");
						break;
					}
					}
					// after the result is received, we start a new
					// process

				}
			}

			);

		}

		/**
		 * random node
		 */
		void pingRandomNodes() {

			// TODO Auto-generated method stub
			AddressIF target;
			// number of knns

			// K=knns[Ninaloader.random.nextInt(knns.length)];

			final int num = defaultDirectPing;
			// control traffic
			addControlTraffic(num);

			final Set<AddressIF> nodes = new HashSet<AddressIF>(num);
			findTwoCommunicationNodesFroDetour(num, new CB1<ArrayList<AddressIF>>() {

				@Override
				protected void cb(CBResult result, final ArrayList<AddressIF> arg1) {
					// TODO Auto-generated method stub

					switch (result.state) {
					case OK: {

						MainGeneric.execMain.execute(new Runnable() {

							public void run() {

								if (arg1 != null && !arg1.isEmpty()) {
									int realNum = Math.min(num, arg1.size());
									nodes.addAll(arg1.subList(0, realNum));

									// Ninaloader.logPingHistory=null;
									Iterator<AddressIF> ier = nodes.iterator();
									try {

										// Ninaloader.logPingHistory.append("Timer:
										// "+Ninaloader.getUptimeStr()+"\n");
										while (ier.hasNext()) {
											AddressIF targeter = ier.next();

											// myself
											if (targeter.equals(Ninaloader.me)) {
												continue;
											}

											String ip = targeter.getHostname();
											if (ip == null || ip.isEmpty()) {
												ip = NetUtil
														.byteIPAddrToString(((NetAddress) targeter).getByteIPAddr());
											}

											// log.info("$: current IP: " +ip);
											double rtt = MainGeneric.doPingDirect(ip);
											// rtt is
											if (rtt >= 0) {
												NodesPair lat = new NodesPair(targeter, Ninaloader.me, rtt);

												// cachedHistoricalNeighbors.addElement(lat);

												if (!containinRing(targeter)) {
													// add the ping result into
													// the ring
													/*
													 * if(!pendingNeighbors.
													 * containsKey(targeter)){
													 * addPendingNeighbor(
													 * targeter); }else{
													 * pendingNeighbors.get(
													 * targeter).addSample(rtt);
													 * }
													 */
												} else {
													instance.g_rings.nodesCache.get(targeter).addSample(rtt);
												}
												if (isQueryNode && !targetCandidates.contains(targeter)) {
													targetCandidates.add(targeter);
												}
											}

											Ninaloader.logPingHistory
													.append(Ninaloader.getUptimeStr() + "\t" + ip + "\t" + rtt + "\n");
										}
										Ninaloader.logPingHistory.flush();

									} catch (IOException e) {

										e.printStackTrace();
									} finally {

									}
									nodes.clear();

								}
							}

						});
						break;
					}
					default: {

						log.warn("can not extract nodes for ping");
						return;
					}
					}
					registerDirectPing();

				}

			});

			// List<AddressIF> list1=new ArrayList<AddressIF>(100);
			// list1.addAll(AllAliveNodes);
			// Collections.shuffle(list1);

		}

		/**
		 * Ask a peering node to query the KNN, default from a Bootstrap node
		 */
		void testNonRingKNN() {

			registerNonRingKNNTimer();

			AddressIF peering = PUtil.getRandomObject(bootstrapAddrs);
			if (peering != null) {

				queryKNN(peering.getHostname(), Ninaloader.me.getHostname(), K, new CB1<List<NodesPair>>() {

					protected void cb(CBResult result, List<NodesPair> knns) {
						// TODO Auto-generated method stub

						System.out.println("KNN query from non-ring nodes");
						switch (result.state) {
						case OK: {
							try {
								String header = "\ntestNonRingKNN\n";
								// logKNNPerformance(K,knns,
								// Ninaloader.me,header);
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							break;
						}
						case TIMEOUT:
						case ERROR: {
							// registerTestMeridian();

							System.out.println(
									"************************\n FAILED to query KNN for Non-ring node !\n************************");
							break;
						}

						}

					}
				});
			}
			if (System.currentTimeMillis() - nonRingLastUpdate > ConcentricRing.MAINTENANCE_PERIOD) {
				maintainPendingCBs(System.currentTimeMillis());
				nonRingLastUpdate = System.currentTimeMillis();
			}

		}

		/**
		 * constrained KNN search TODO:
		 */
		void ConstrainedKNN(final AddressIF target, final List<AddressIF> candidates, final int num,
				final CB2<Vector<String>, Vector<Double>> cbDone) {

			registerTestConstrainedMeridian();

			// final long timer=System.currentTimeMillis();

			queryKNearestNeighbors(target, candidates, num, new CB1<List<NodesPair>>() {

				protected void cb(CBResult result, List<NodesPair> cknns) {
					// TODO Auto-generated method stub

					switch (result.state) {
					case OK: {

						if (cknns != null && cknns.size() > 0) {

							Vector<String> addr = new Vector<String>(1);
							Vector<Double> estRTT = new Vector<Double>(1);

							/*
							 * log.info("$: " + cknns.size() +
							 * "nearest neighbors are as follows: ");
							 */
							// record
							try {
								String header = "\nTestConstrainedMeridian\n" + POut.toString(candidates.toArray())
										+ "\n";

								// log.info(header);

								// logKNNPerformance(num,cknns, target,header);
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

							// record current KNN
							currentKNN.clear();
							Iterator<NodesPair> ier = cknns.iterator();
							int ind = 0;
							NodesPair nxtNode;
							while (ier.hasNext()) {
								nxtNode = ier.next();
								addr.add(nxtNode.startNode.toString());
								estRTT.add(Double.valueOf(nxtNode.rtt));
								// myself
								ind++;
								// otherwise
								/*
								 * log.info(ind + "th NN: " +
								 * nxtNode.SimtoString());
								 */
								currentKNN.add(nxtNode.makeCopy());
								if (ind == K) {
									break;
								}
							}
							// only success, we register
							// registerTestMeridian();
							cbDone.call(CBResult.OK(), addr, estRTT);
							break;
						} else {
							log.debug(
									"************************\n FAILED to query constrained KNN!\nSet<NodesPair> is empty!\n************************");
							cbDone.call(CBResult.ERROR(), new Vector<String>(1), new Vector<Double>(1));
							break;
						}

					}
					case TIMEOUT:
					case ERROR: {
						// registerTestMeridian();
						log.debug(
								"************************\n FAILED to query constrained KNN!\n************************");
						cbDone.call(CBResult.ERROR(), new Vector<String>(1), new Vector<Double>(1));
						break;
					}
					}

				}

			});

		}

		/**
		 * Test multiobjective based knn
		 */
		void TestMultiObjectiveKNNNina() {
			addWarmExperience();
			// ================================================================
			// register next update process
			registerTestKNNBasdOnMultiObjectiveNina();

			// ring is too limited
			if (!candoKNN()) {
				log.warn("can not do TestMultiObjectiveKNNNina(), since current number of non-empty rings is:"
						+ instance.g_rings.CurrentNonEmptyRings);
				return;
			}

			final long timer = System.currentTimeMillis();

			findTwoCommunicationNodesFroDetour(defaultNumOfDetourTargets, new CB1<ArrayList<AddressIF>>() {

				@Override
				protected void cb(CBResult result, ArrayList<AddressIF> arg1) {
					// TODO Auto-generated method stub
					switch (result.state) {
					case OK: {
						final ArrayList<AddressIF> targets = arg1;
						if (targets == null || targets.size() < defaultNumOfDetourTargets) {
							log.warn("returned detour targets is smaller!");
							return;
						}
						final String[] targetAddrs = { " " };
						Iterator<AddressIF> ier = targets.iterator();
						while (ier.hasNext()) {
							AddressIF tmp = ier.next();
							// targetAddrs[0]+=",
							// "+NetUtil.byteIPAddrToString(((NetAddress)tmp).getByteIPAddr());
							targetAddrs[0] += ", " + tmp.toString();
						}
						log.info("#: multiObj KNN: " + targetAddrs[0]);

						if (targets.size() > 1) {
							final int len = targets.size();
							if (len <= 1) {
								log.warn("the targets is smaller than 2!");
								break;
							} else {

								// =======
								final ArrayList<AddressIF> targetFrom = new ArrayList<AddressIF>(2);
								targetFrom.addAll(targets.subList(0, len - 1));
								final AddressIF targetTo = targets.get(len - 1);
								TargetProbes(targetFrom, targetTo, new CB1<String>() {

									@Override
									protected void cb(CBResult result, String arg1) {
										// TODO Auto-generated method stub

									}
								}, new CB2<Set<NodesPair>, String>() {

									@Override
									protected void cb(CBResult result, Set<NodesPair> arg1, String arg2) {
										// TODO Auto-generated method stub
										switch (result.state) {
										case OK: {
											Set<NodesPair> lat = arg1;

											// save to the history
											// cachedHistoricalNeighbors.addElement(lat);

											String str = "baseline latency measurement for multiobjective measurement";
											try {
												logKNNPerformance(null, Ninaloader.logmultiKNN, -1, -1, -1, -1, -1, -1,
														-1, len - 1, lat, targetTo, str, -1);
											} catch (IOException e) {
												// TODO Auto-generated catch
												// block
												e.printStackTrace();
											}

											final double[] rttBaseline = { 0 };
											Iterator<NodesPair> ier22 = arg1.iterator();
											while (ier22.hasNext()) {
												rttBaseline[0] += ier22.next().rtt;
											}

											// ===========================================

											// ================================================
											// multiobjective knn
											// start the multiObjective
											// measurement
											// ===========================================
											final int expectedKNN = knns[Ninaloader.random.nextInt(knns.length)];

											queryKNearestNeighbors(targets, expectedKNN, new CB1<List<NodesPair>>() {
												protected void cb(CBResult result, List<NodesPair> arg1) {

													switch (result.state) {
													case OK: {

														if (arg1 != null && arg1.size() > 0) {
															log.info("$: " + arg1.size()
																	+ "nearest neighbors are as follows: ");
															log.info("KNN:\nExpected: " + expectedKNN + ", real: "
																	+ arg1.size());

															// add the elements
															// cachedHistoricalNeighbors.addElement(arg1);

															int totalSendingMessage = arg1.get(0).totalSendingMessage;

															double time = arg1.get(0).elapsedTime;
															// record
															try {
																String header = "\nTestMultiObjective\nbaseLineRTT: "
																		+ rttBaseline[0] + "\n";

																logKNNPerformance(null, Ninaloader.logmultiKNN, -1, -1,
																		-1, -1, totalSendingMessage, time,
																		(arg1.size() + 0.0) / expectedKNN, expectedKNN,
																		arg1, targetAddrs[0], header, -1);

															} catch (IOException e) {
																// TODO
																// Auto-generated
																// catch block
																e.printStackTrace();
															}

															arg1.clear();
															// only success, we
															// register
															// registerTestMeridian();
														} else {
															log.debug(
																	"************************\n FAILED to query KNN!\nSet<NodesPair> is empty!\n************************");
														}

														// TargetProbes(targets,);

														break;
													}
													case TIMEOUT:
													case ERROR: {
														// registerTestMeridian();

														log.debug(
																"************************\n FAILED to query KNN!\n************************");
														break;
													}
													}
													targets.clear();
													// after the result is
													// received, we start a new
													// process

												}
											});

											// ===========================================
											break;
										}
										default: {
											log.warn("can not measure for multiobjective knn");
											break;
										}
										}
									}

								});

							}

						}

						break;
					}
					default: {

						log.warn("can not extract the real address!");
						break;
					}

					}

				}

			});

		}

		/**
		 * Test Nina, find who is nearest to me
		 * 
		 * @param neighbor
		 * @param cdDone
		 */
		void TestKNNBasedOnNinaForMe() {

			registerTestKNNBasdOnNina();

			addWarmExperience();

			final long timer = System.currentTimeMillis();

			// if the size of currentKNN fullfil the number of KNN, return
			// immediately

			// use background to search, <Long,CBcached>
			/*
			 * Set<AddressIF> targets=new HashSet<AddressIF>(1);
			 * instance.g_rings.getRandomNodes(targets, 1);
			 * 
			 * 
			 * if(targets.isEmpty()){ return; }
			 * 
			 * final AddressIF target=targets.iterator().next();
			 */
			// final AddressIF target=Ninaloader.me;

			/*
			 * findTwoCommunicationNodesFroDetour(1, new
			 * CB1<ArrayList<AddressIF> >(){
			 * 
			 * @Override protected void cb(CBResult result, ArrayList<AddressIF>
			 * arg1) { // TODO Auto-generated method stub switch(result.state){
			 * case OK:{ if(arg1!=null&&!arg1.isEmpty()){
			 */

			/*
			 * if(!candoKNN()){ log.warn(
			 * "can not do TestKNNBasedOnNinaForMe(), since current number of non-empty rings is:"
			 * +instance.g_rings.CurrentNonEmptyRings);
			 * registerTestKNNBasdOnNina(); return; }
			 */

			final AddressIF target = Ninaloader.me;
			// number of knns
			final int expectedKNN = knns[Ninaloader.random.nextInt(knns.length)];

			queryKNearestNeighbors(target, expectedKNN, new CB1<List<NodesPair>>() {

				protected void cb(CBResult result, List<NodesPair> arg1) {

					switch (result.state) {
					case OK: {

						if (arg1 != null && arg1.size() > 0) {
							log.debug("$: " + arg1.size() + "nearest neighbors are as follows: ");

							// totalSingleObjKNN++;
							if (target.equals(Ninaloader.me)) {

								// record current KNN
								// currentKNN.clear();
								Iterator<NodesPair> ier = arg1.iterator();
								int ind = 0;

								long curr_time = System.currentTimeMillis();
								NodesPair nxtNode;
								while (ier.hasNext()) {
									nxtNode = ier.next();

									/**
									 * cached knn result
									 */

									if (!containinRing((AddressIF) (nxtNode.startNode))) {
										askCoordinate((AddressIF) (nxtNode.startNode), nxtNode.rtt, curr_time,
												pendingNeighbors);
										/*
										 * addPendingNeighbor((AddressIF)(
										 * nxtNode.startNode));
										 * if(pendingNeighbors.containsKey((
										 * AddressIF)(nxtNode.startNode))){
										 * pendingNeighbors.get((AddressIF)(
										 * nxtNode.startNode)).addSample(nxtNode
										 * .rtt); }
										 */

									} else {
										instance.g_rings.nodesCache.get((AddressIF) (nxtNode.startNode))
												.addSample(nxtNode.rtt);
									}
									// askRemoteRelativeCoordinateIntoRing((AddressIF)(nxtNode.startNode));
									// myself
									ind++;
									// otherwise
									log.info(ind + "th NN: " + nxtNode.SimtoString());
									// currentKNN.add(nxtNode.makeCopy());
									if (ind == expectedKNN) {
										break;
									}
								}

							}

							boolean record = false;
							if (record) {
								log.info("KNN:\nExpected: " + expectedKNN + ", real: " + arg1.size());
								/*
								 * cachedHistoricalNeighbors.addElement(arg1);
								 * //my cached history is too small
								 * if(cachedHistoricalNeighbors==null||
								 * cachedHistoricalNeighbors.isEmpty()){
								 * registerTestKNNBasdOnNina(); return; }
								 */

								// add the elements

								// cachedHistoricalNeighbors.addElement(arg1);

								// SuccessRateForSingleTargetKNN.add((arg1.size()+0.0)/expectedKNN);

								// SortedList<NodesPair<AddressIF>> bestNodes =
								// new SortedList<NodesPair<AddressIF>>(new
								// NodesPairComp());
								// bestNodes.addAll(cachedHistoricalNeighbors.getCurrentBestNearestNeighbors());

								/*
								 * double ClosestNodes=KNNStatistics.
								 * containClosestNodes(arg1); double
								 * KNNGains=-1; double
								 * absoluteError=KNNStatistics.absoluteError(
								 * arg1, bestNodes); double
								 * RelativeError=KNNStatistics.RelativeError(
								 * arg1, bestNodes); //coverage double
								 * coverageRate=KNNStatistics.coverage(arg1,
								 * bestNodes);
								 */
								double ClosestNodes = -1;
								double KNNGains = -1;
								double absoluteError = -1;
								double RelativeError = -1;
								// coverage
								double coverageRate = -1;

								double time = arg1.get(0).elapsedTime;
								int totalSendingMessage = arg1.get(0).totalSendingMessage;

								// record
								try {
									String header = "\nTestNina\n";
									logKNNPerformance(null, Ninaloader.logKNN, ClosestNodes, KNNGains, absoluteError,
											RelativeError, totalSendingMessage, time, (arg1.size() + 0.0) / expectedKNN,
											expectedKNN, arg1, target, header, coverageRate);
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}

							}
							// only success, we register
							// registerTestMeridian();
						} else {
							log.warn(
									"************************\n FAILED to query KNN!\nSet<NodesPair> is empty!\n************************");
						}

						break;
					}
					case TIMEOUT:
					case ERROR: {
						// registerTestMeridian();
						log.warn("************************\n FAILED to query KNN for me!\n************************");
						break;
					}
					}
					// after the result is received, we start a new
					// process
					// registerTestKNNBasdOnNina();

				}
			}

			);

			/*
			 * } break; } default:{
			 * 
			 * log.warn("can not extract the target"); return; }
			 * 
			 * } //start new search registerTestKNNBasdOnNina(); }
			 * 
			 * 
			 * });
			 */

		}

		/**
		 * test
		 */
		void TestKNNBasedOnNinaForOther() {

			// register
			registerTestKNNBasedOnNinaForOther();

			addWarmExperience();

			// one nearest neighbor
			// final int
			// expectedKNN=knns[Ninaloader.random.nextInt(knns.length)];
			final int expectedKNN = 1;

			// registerTestMeridian1NN();

			/*
			 * Set<AddressIF> targets=new HashSet<AddressIF>(1);
			 * instance.g_rings.getRandomNodes(targets, 2);
			 * targets.add(AddressFactory.create(Ninaloader.me));
			 */
			final AddressIF target = PUtil.getRandomObject(targetCandidates);
			// final AddressIF target=

			queryKNearestNeighbors(target, expectedKNN, new CB1<List<NodesPair>>() {
				@Override
				protected void cb(CBResult result, final List<NodesPair> arg1) {
					// TODO Auto-generated method stub
					switch (result.state) {
					case OK: {
						if (arg1 != null && arg1.size() > 0) {

							// totalSingleObjKNN++;
							double ClosestNodes = -1;
							double KNNGains = -1;
							double absoluteError = -1;
							double RelativeError = -1;
							double coverageRate = -1;

							// bestNodes.clear();
							double time = arg1.get(0).elapsedTime;
							int totalSendingMessage = arg1.get(0).totalSendingMessage;
							log.info("KNN_Other:\nExpected: " + expectedKNN + ", real: " + arg1.size() + ", time: "
									+ time);

							// record
							try {
								String header = "\nTestNinaOther\n";
								logKNNPerformance(null, Ninaloader.logKNN, ClosestNodes, KNNGains, absoluteError,
										RelativeError, totalSendingMessage, time, (arg1.size() + 0.0) / expectedKNN,
										expectedKNN, arg1, target, header, coverageRate);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							/*
							 * registerTestMeridian1NN() ; return;
							 */
							break;

						} else {
							log.warn("returned Results is empty");
						}
						break;
					}
					case TIMEOUT:
					case ERROR: {
						log.warn("FAILED, Nina");
						break;
					}
					}
					// registerTestKNNBasedOnNinaForOther();
				}
			});

		}

		/**
		 * find knn for others
		 */
		void TestKNNBasedOnNinaForOther1() {

			addWarmExperience();

			if (!candoKNN()) {
				log.warn("can not do TestKNNBasedOnNinaForother(), since current number of non-empty rings is:"
						+ instance.g_rings.CurrentNonEmptyRings);
				registerTestKNNBasedOnNinaForOther();
				return;
			}

			final long timer = System.currentTimeMillis();

			// if the size of currentKNN fullfil the number of KNN, return
			// immediately
			/*
			 * findTwoCommunicationNodesFroDetour(3, new
			 * CB1<ArrayList<AddressIF> >(){
			 * 
			 * @Override protected void cb(CBResult result, ArrayList<AddressIF>
			 * arg1) { // TODO Auto-generated method stub switch(result.state){
			 * case OK:{ if(arg1==null||arg1.isEmpty()){
			 * registerTestKNNBasedOnNinaForOther(); return; }else{
			 */
			/*
			 * Vector<AddressIF> ringMembers=new Vector<AddressIF>(1);
			 * instance.g_rings.getRandomNodes(ringMembers,1);
			 * 
			 * if(ringMembers.isEmpty()){ registerTestKNNBasedOnNinaForOther();
			 * return; }
			 */
			final AddressIF target = PUtil.getRandomObject(targetCandidates);

			// number of knns
			final int expectedKNN = knns[Ninaloader.random.nextInt(knns.length)];

			queryKNearestNeighbors(target, expectedKNN, new CB1<List<NodesPair>>() {

				protected void cb(CBResult result, final List<NodesPair> arg1) {
					log.info("$test Nina for others: ");
					switch (result.state) {
					case OK: {

						if (arg1 != null && arg1.size() > 0) {

							// totalSingleObjKNN++;

							log.info("other KNN:\nExpected: " + expectedKNN + ", real: " + arg1.size());

							// add the elements
							// cachedHistoricalNeighborsOther.addElement(arg1);
							askcachedHistoricalNeighborsOther(target, expectedKNN,
									new CB1<HistoryNearestNeighbors<AddressIF>>() {

								@Override
								protected void cb(CBResult result,
										HistoryNearestNeighbors<AddressIF> cachedHistoricalNeighborsOther) {
									// TODO Auto-generated method stub
									switch (result.state) {
									case OK: {

										// save the elements
										if (cachedHistoricalNeighborsOther == null
												|| cachedHistoricalNeighborsOther.isEmpty()) {

											/*
											 * cachedHistoricalNeighborsOther=
											 * new
											 * HistoryNearestNeighbors<AddressIF
											 * >(expectedKNN);
											 * cachedHistoricalNeighborsOther.
											 * addElement(arg1);
											 */
											log.warn("no cached KNNs are found,");
											// registerTestKNNBasedOnNinaForOther();
											break;
										}
										log.info("history size: " + cachedHistoricalNeighborsOther.getCurrentSize());
										// SuccessRateForSingleTargetKNN.add((arg1.size()+0.0)/expectedKNN);

										double ClosestNodes = KNNStatistics.containClosestNodes(arg1);
										double KNNGains = KNNStatistics.KNNGains(arg1,
												cachedHistoricalNeighborsOther.getCurrentWorstNearestNeighbors(),
												cachedHistoricalNeighborsOther.getCurrentBestNearestNeighbors());
										double absoluteError = KNNStatistics.absoluteError(arg1,
												cachedHistoricalNeighborsOther.getCurrentBestNearestNeighbors());
										double RelativeError = KNNStatistics.RelativeError(arg1,
												cachedHistoricalNeighborsOther.getCurrentBestNearestNeighbors());
										double coverageRate = KNNStatistics.coverage(arg1,
												cachedHistoricalNeighborsOther.getCurrentBestNearestNeighbors());

										double time = arg1.get(0).elapsedTime;
										int totalSendingMessage = arg1.get(0).totalSendingMessage;

										// record
										try {
											String header = "\nTestNinaOther\n";
											logKNNPerformance(
													cachedHistoricalNeighborsOther.getCurrentBestNearestNeighbors(),
													Ninaloader.logKNN, ClosestNodes, KNNGains, absoluteError,
													RelativeError, totalSendingMessage, time,
													(arg1.size() + 0.0) / expectedKNN, expectedKNN, arg1, target,
													header, coverageRate);
										} catch (IOException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}

										if (cachedHistoricalNeighborsOther != null) {
											cachedHistoricalNeighborsOther.clear();
										}
										log.warn("ask OK");
										break;
									}
									case TIMEOUT:
									case ERROR: {
										log.warn("ask failed");
										break;
									}
									}
									// registerTestKNNBasedOnNinaForOther();
								}

							});

							// only success, we register
							// registerTestMeridian();
						} else {
							log.debug(
									"************************\n FAILED to query KNN other!\nSet<NodesPair> is empty!\n************************");
							// registerTestKNNBasedOnNinaForOther();
						}

						break;
					}
					case TIMEOUT:
					case ERROR: {
						// registerTestMeridian();

						log.warn("************************\n FAILED to query KNN, other!\n************************");
						// registerTestKNNBasedOnNinaForOther();
						break;
					}
					}
					// after the result is received, we start a new
					// process
					registerTestKNNBasedOnNinaForOther();

				}
			}

			);

			/*
			 * //finish break; }
			 * 
			 * 
			 * case TIMEOUT: case ERROR: {
			 * 
			 * log.warn("can not extract the target");
			 * registerTestKNNBasedOnNinaForOther(); return; }
			 * 
			 * } //start new search //registerTestKNNBasdOnNina(); }
			 * 
			 * 
			 * });
			 */

		}

		// Start a gossip session
		void performGossip(boolean useRings) {

			registerGossipTimer();
			// use rings and pending nodes
			final Set<AddressIF> randNodes = pickGossipNodes();
			// pending neighbors that are not inserted into rings yet
			// insert the pending neighbors

			if (randNodes.isEmpty()) {

				Iterator<AddressIF> iers = bootstrapAddrs.iterator();
				while (iers.hasNext()) {
					AddressIF tmps = iers.next();
					if (tmps.equals(Ninaloader.me)) {
						continue;
					}
					randNodes.add(tmps);
					if (!containinRing(tmps)) {
						addPendingNeighbor(tmps);
					}
					// pendingNeighbors.put(tmps, new
					// RemoteState<AddressIF>(tmps));
				}
			}

			if (randNodes.isEmpty()) {
				log.warn("@ No nodes to gossip");

			} else {
				// log.info("#####################\$: Gossip with: "+
				// randNodes.size() + "Nodes\n#####################");
				// Iterator ier=randNodes.iterator();
				/*
				 * while(ier.hasNext()){ log.info("$: Addr: "+ier.next()); }
				 */
				addControlTraffic(randNodes.size());
				// send cached nodes
				ping(randNodes, useRings);
				// todo: gossip with others not ping
			}

			randNodes.clear();

			long curr_time = System.currentTimeMillis();

			if (instance.lastMaintenanceStamp < curr_time - instance.MAINTENANCE_PERIOD) {
				maintain(curr_time, pendingNeighbors);
				instance.lastMaintenanceStamp = curr_time;
			}
		}

		// selection=0: random, 1, maximum hypercolume
		synchronized void performRingManagement(int selection) {

			// the number of each ring, is k, which is proportional to logN

			// Find all full rings

			log.info("Concentric ring: " + instance.g_rings.CurrentNodes);

			// empty rings
			int defaultClustergingLandmarks = 5;

			if (instance.g_rings.getCurrentNonEmptyRings() < defaultNonEmptyRingsForKNN) {
				log.warn("@ fail to manage rings");
				registerRingManagerTimer();
				return;
			}

			int numRings = instance.g_rings.getNumberOfRings();
			List<Integer> eligibleRings = new ArrayList<Integer>();
			for (int i = 0; i < numRings; i++) {
				// Test if the ring is eligible for ring management
				if (instance.g_rings.eligibleForReplacement(i)) {
					eligibleRings.add(i);
				}
			}
			if (eligibleRings.isEmpty()) {
				registerRingManagerTimer();
				return;
			}

			Collections.shuffle(eligibleRings);
			// Pick a random eligible ring
			final int selectedRing = eligibleRings.get(0).intValue();
			eligibleRings.clear();
			eligibleRings = null;

			// TODO: remove old records, like RemoteState - soft-state
			final Vector<AddressIF> primaryRingNodes = instance.g_rings.primaryRing.get(selectedRing);

			int pSize = primaryRingNodes.size();
			final Vector<AddressIF> secondaryRingNodes = instance.g_rings.secondaryRing.get(selectedRing);

			final int sSize = secondaryRingNodes.size();
			int secondSize = sSize;
			final Vector<AddressIF> candidates = new Vector<AddressIF>(10);

			// TODO: remove nodes that do not response in T time slots
			long curr_time = System.currentTimeMillis();
			final long expirationStamp = curr_time - ConcentricRing.RS_EXPIRATION;

			// ====================================
			lockRing2.lock();

			try {
				Iterator<AddressIF> ier = primaryRingNodes.iterator();
				while (ier.hasNext()) {
					AddressIF tmp = ier.next();
					// cached latency
					if (instance.g_rings.nodesCache.containsKey(tmp)) {
						RemoteState<AddressIF> rs = instance.g_rings.nodesCache.get(tmp);

						if (rs.getLastUpdateTime() > expirationStamp) {
							candidates.add(tmp);
						} else {
							// remove nodes
							pSize--;

							instance.g_rings.eraseNode(tmp);
							continue;
						}
					}

				}
				ier = secondaryRingNodes.iterator();

				while (ier.hasNext()) {
					AddressIF tmp = ier.next();
					// cached latency
					if (instance.g_rings.nodesCache.containsKey(tmp)) {
						RemoteState<AddressIF> rs = instance.g_rings.nodesCache.get(tmp);
						if (rs.getLastUpdateTime() > expirationStamp) {
							candidates.add(tmp);
						} else {
							// remove nodes
							secondSize--;
							instance.g_rings.nodesCache.remove(tmp);
							instance.g_rings.eraseNode(tmp);
							continue;
						}
					}

				}
			} finally {
				lockRing2.unlock();
			}

			primaryRingNodes.clear();
			secondaryRingNodes.clear();

			int len;
			// -----------------------------------------
			// TODO:

			if (selection == 0) {
				// (1): randomly select a set of k nodes
				/*
				 * Vector<AddressIF> permuted=new Vector<AddressIF>(10);
				 * while(!candidates.isEmpty()){ len=candidates.size(); int
				 * index=(int)Math.round(Math.random()len); while(index>=len){
				 * index--; } permuted.add(candidates.get(index));
				 * candidates.remove(index); }
				 */
				Collections.shuffle(candidates);
				int total = pSize + secondSize;

				int primarySize = Math.min(total, instance.g_rings.primarySize);
				int secondarySize = Math.max(0, total - primarySize);

				int counter = 0;
				while (counter < primarySize) {
					primaryRingNodes.add(candidates.get(counter));
					counter++;
				}
				// reset
				int counter2 = 0;
				while (counter2 < secondarySize) {
					secondaryRingNodes.add(candidates.get(counter2 + counter));
					counter2++;
				}

				candidates.clear();

				// ==========================================================
				instance.g_rings.setRingMembers(selectedRing, primaryRingNodes, secondaryRingNodes);

				// TODO: gossip with other nodes
				registerRingManagerTimer();

			} else if (selection == 1) {
				// default 1: Meridian, compute maximum hypervolume

				// (1) latency matrix
				Iterator<AddressIF> ier2 = candidates.iterator();
				final Barrier barrier = new Barrier(true);
				final StringBuffer errorBuffer = new StringBuffer();
				final Set<NodesPair> cachedM = new HashSet<NodesPair>(10);

				if (candidates.size() <= 1) {
					return;
				}
				addControlTraffic(candidates.size() * candidates.size());
				while (ier2.hasNext()) {
					barrier.fork();
					TargetProbes(candidates, ier2.next(), new CB1<String>() {
						protected void cb(CBResult ncResult, String errorString) {
							switch (ncResult.state) {
							case OK: {
								// log.info("$: Target Probes are issued");
							}
							case ERROR:
							case TIMEOUT: {
								break;
							}
							}
						}
					}, new CB2<Set<NodesPair>, String>() {

						protected void cb(CBResult result, Set<NodesPair> arg1, String arg2) {
							// TODO Auto-generated method stub
							switch (result.state) {
							case OK: {
								cachedM.addAll(arg1);

							}
							case ERROR:
							case TIMEOUT: {

								// remove nodes,
								// MClient.instance.g_rings.eraseNode(remoteNode);
								// MClient.instance.rs_map.remove(remoteNode);

								break;
							}
							}
							barrier.join();
						}

					});
				}

				// (2) reduceSetByN
				EL.get().registerTimerCB(barrier, new CB0() {
					protected void cb(CBResult result) {
						String errorString;
						if (errorBuffer.length() == 0) {
							errorString = new String("Success");
						} else {
							errorString = new String(errorBuffer);
						}
						log.debug("$@ @ ready for computing geometry operation");
						// TODO: assume that all nodes are alive here!
						int len = candidates.size();
						double[] latencyMatrix = new double[len * len];
						for (int i = 0; i < len; i++) {
							for (int j = 0; j < len; j++) {
								latencyMatrix[i * len + j] = 0;
							}
						}
						// -----------------------------------------
						Iterator<NodesPair> ier3 = cachedM.iterator();
						int from = 0, to = 0;
						while (ier3.hasNext()) {
							NodesPair tmp = ier3.next();
							from = candidates.indexOf(tmp.startNode);
							to = candidates.indexOf(tmp.endNode);
							// use offset
							latencyMatrix[from * len + to] = tmp.rtt + AbstractNNSearchManager.offsetLatency;
						}
						// -----------------------------------------
						Vector<AddressIF> deletedNodes = new Vector<AddressIF>(10);

						instance.g_rings.reduceSetByN(candidates, deletedNodes, sSize, latencyMatrix);
						primaryRingNodes.addAll(candidates);
						secondaryRingNodes.addAll(deletedNodes);

						instance.g_rings.setRingMembers(selectedRing, primaryRingNodes, secondaryRingNodes);

						// TODO: gossip with other nodes
						registerRingManagerTimer();

					}
				});

			} else if (selection == 2) {
				// clustering awareness with HSH
				// maximize the clustering coverage

				// query the H vector of each node

				// append nodes according to round robin fashion

				int total = pSize + secondSize;

				int primarySize = Math.min(total, instance.g_rings.primarySize);
				int secondarySize = Math.max(0, total - primarySize);

				// selection based on bin optimization
				List<AddressIF> nodesBasedonBin; // instance.g_rings.binningProc.findKNodesFromCandidates(primarySize,
													// candidates);

				// primaryRingNodes.addAll(nodesBasedonBin);
				secondaryRingNodes.addAll(candidates);

				primaryRingNodes.clear();
				secondaryRingNodes.clear();

				// candidates.clear();

				// ==========================================================
				lockRing2.lock();
				try {
					instance.g_rings.setRingMembers(selectedRing, primaryRingNodes, secondaryRingNodes);
				} finally {
					lockRing2.unlock();
				}

				/*
				 * final int pSize1=pSize; final int secondSize2=secondSize;
				 * if(clusteringManager!=null){
				 * //=======================================================
				 * 
				 * // do clustering
				 * 
				 * List<AddressIF> landmarks = candidates.subList(0,
				 * defaultClustergingLandmarks); // List<AddressIF> hosts =
				 * candidates.subList(defaultClustergingLandmarks,
				 * candidates.size()); List<AddressIF> hosts = candidates;
				 * List<AddressIF> Fin_Nodes=new ArrayList<AddressIF>(5);
				 * List<AddressIF> ToBeRemovedNodes=new ArrayList<AddressIF>(5);
				 * 
				 * SortedList<NodesPair> sortedL = new SortedList<NodesPair>(
				 * new NodesPairComp());
				 * 
				 * Iterator<AddressIF> ierL; double clusteringSim;
				 * ToBeRemovedNodes.addAll(hosts);
				 * 
				 * //first add all landmarks Fin_Nodes.addAll(landmarks);
				 * 
				 * //remove the landmarks ToBeRemovedNodes.removeAll(landmarks);
				 * 
				 * 
				 * do{ sortedL.clear(); //then add node that minimize the
				 * clustering indicators for(int
				 * i=0;i<ToBeRemovedNodes.size();i++){ ClusteringSet
				 * me=instance.g_rings.getClusteringVec().get(ToBeRemovedNodes.
				 * get(i)); clusteringSim=0; //included
				 * if(Fin_Nodes.contains(ToBeRemovedNodes.get(i))){ continue;
				 * }else{ //compute the total scores for(int
				 * index=0;index<Fin_Nodes.size();index++){ ClusteringSet you =
				 * instance.g_rings.getClusteringVec().get(Fin_Nodes.get(index))
				 * ; clusteringSim+=me.findCommonBits(you); } sortedL.add(new
				 * NodesPair(ToBeRemovedNodes.get(i),null, clusteringSim)); }
				 * 
				 * } //========================== if(sortedL.isEmpty()){ break;
				 * }else{ the most farthest node is added; AddressIF
				 * fNode=(AddressIF)(sortedL.get(0).startNode);
				 * Fin_Nodes.add(fNode); ToBeRemovedNodes.remove(fNode); }
				 * //==========================
				 * }while(!ToBeRemovedNodes.isEmpty());
				 * //==================================
				 * 
				 * 
				 * for(int i=0;i<hosts.size();i++){ ierL= landmarks.iterator();
				 * clusteringSim=0; ClusteringSet
				 * me=instance.g_rings.getClusteringVec().get(hosts.get(i));
				 * if(me==null){ sortedL.add(new NodesPair(hosts.get(i),null,
				 * clusteringSim)); }else{ while(ierL.hasNext()){ AddressIF
				 * LM=ierL.next(); ClusteringSet you =
				 * instance.g_rings.getClusteringVec().get(LM); if(you==null){
				 * continue; }else{ clusteringSim+=me.findCommonBits(you); } }
				 * // the score sortedL.add(new NodesPair(hosts.get(i),null,
				 * clusteringSim)); } }
				 * 
				 * //====================================================
				 * //first K nodes are selected int total = pSize1 +
				 * secondSize2;
				 * 
				 * 
				 * int primarySize = Math.min(total,
				 * instance.g_rings.primarySize);
				 * 
				 * 
				 * int secondarySize = Math.max(0, total - primarySize);
				 * 
				 * //in reverse direction
				 * 
				 * int counter = 0; while (counter < primarySize) {
				 * primaryRingNodes.add(Fin_Nodes.get(counter)); counter++; } //
				 * reset int counter2 = 0; while (counter2 < secondarySize) {
				 * secondaryRingNodes.add(Fin_Nodes.get(counter2 + counter));
				 * counter2++; } candidates.clear(); Fin_Nodes.clear();
				 * 
				 * instance.g_rings.setRingMembers(selectedRing,
				 * primaryRingNodes, secondaryRingNodes);
				 * 
				 * // TODO: gossip with other nodes
				 * 
				 * 
				 * 
				 * }
				 */

				// ===============================
				registerRingManagerTimer();
			}

		}

		public Set<AddressIF> pickGossipNodes() {
			// TODO: add pending nodes from lists
			Set<AddressIF> nodes = new HashSet<AddressIF>(10);

			instance.g_rings.getRandomNodes(nodes, defaultGossip);

			// a random node
			// int size=pendingNeighbors.size();
			int num = 5;
			int realNum = Math.min(num, pendingNeighbors.keySet().size());
			for (int i = 0; i < realNum; i++) {
				AddressIF pending = PUtil.getRandomObject(pendingNeighbors.keySet());

				if (pending != null) {
					if (!pending.equals(Ninaloader.me) && !nodes.contains(pending)) {
						nodes.add(pending);
					}
				}
			}
			nodes.remove(Ninaloader.me);

			// reset
			if (nodes.isEmpty()) {
				resetBootstrapNeighbors();

				realNum = Math.min(num, pendingNeighbors.keySet().size());
				for (int i = 0; i < realNum; i++) {
					AddressIF pending = PUtil.getRandomObject(pendingNeighbors.keySet());

					if (pending != null) {
						if (!pending.equals(Ninaloader.me) && !nodes.contains(pending)) {
							nodes.add(pending);
						}
					}
				}

			}

			// log.info("find gossip nodes: "+nodes.size());
			return nodes;
		}

		public void run() {
			// TODO Auto-generated method stub
			init();

		}
	}

	// =======================================================
	int[] knns = { 1, 10, 25, 30 };

	public class MeridianClient implements Runnable {

		public ConcentricRing<AddressIF> instance = null;

		final CB0 updateGosssipCB1;
		final CB0 updateRingMaintainCB2;

		final CB0 directPingCB;

		int RingMaintainSelection = 1;

		int defaultGossipSize = 5;
		long delayInMs = 60; // for rate calc, 60
		final CB0 MeridianTest;

		public MeridianClient() {
			if (instance == null) {
				instance = new ConcentricRing<AddressIF>(Ninaloader.me, MeridianNodesPerRing);
			}

			updateRingMaintainCB2 = new CB0() {
				protected void cb(CBResult result) {

					performRingManagement(RingMaintainSelection); // 0, random,1
																	// meridian

				}
			};
			MeridianTest = new CB0() {
				protected void cb(CBResult result) {
					log.debug("$: Ready for test Meridian!");

					TestKNNBasedOnMeridian();

				}
			};

			updateGosssipCB1 = new CB0() {
				protected void cb(CBResult result) {

					performGossip(true);
				}

			};

			directPingCB = new CB0() {
				protected void cb(CBResult result) {
					pingRandomNodes();
				}

			};
		}

		public void init() {

			instance.lastMaintenanceStamp = System.currentTimeMillis();
			instance.g_rendvNode = Ninaloader.me;

			registerGossipTimer();
			registerRingManagerTimer();

			if (isQueryNode) {
				registerTestMeridian1NN();
			}

			registerDirectPing();
			registerControlTrafficTimer();
		}
		// register the control traffic timer

		void registerGossipTimer() {
			// LOWTODO adaptive delay
			double rnd = Ninaloader.random.nextDouble();
			long delay = RingUPDATE_DELAY + (long) (30 * 1000 * rnd);

			// log.debug("setting timer to " + delay);
			EL.get().registerTimerCB(delay, updateGosssipCB1);

		}

		// Start a gossip session
		void performGossip(boolean useRings) {

			registerGossipTimer();
			// use rings and pending nodes
			final Set<AddressIF> randNodes = pickGossipNodes();
			// pending neighbors that are not inserted into rings yet
			// insert the pending neighbors

			if (randNodes.isEmpty()) {

				Iterator<AddressIF> iers = bootstrapAddrs.iterator();
				while (iers.hasNext()) {
					AddressIF tmps = iers.next();
					if (tmps.equals(Ninaloader.me)) {
						continue;
					}
					randNodes.add(tmps);
					if (!containinRing(tmps)) {
						addPendingNeighbor(tmps);
					}
					// pendingNeighbors.put(tmps, new
					// RemoteState<AddressIF>(tmps));
				}
			}

			if (randNodes.isEmpty()) {
				log.warn("@ No nodes to gossip");

			} else {
				// log.info("#####################\$: Gossip with: "+
				// randNodes.size() + "Nodes\n#####################");
				// Iterator ier=randNodes.iterator();
				/*
				 * while(ier.hasNext()){ log.info("$: Addr: "+ier.next()); }
				 */

				// control traffic
				addControlTraffic(randNodes.size());
				// send cached nodes
				ping(randNodes, useRings);
				// todo: gossip with others not ping
			}

			randNodes.clear();

			long curr_time = System.currentTimeMillis();

			if (instance.lastMaintenanceStamp < curr_time - instance.MAINTENANCE_PERIOD) {
				maintain(curr_time, pendingNeighbors);
				instance.lastMaintenanceStamp = curr_time;
			}
		}

		void registerRingManagerTimer() {
			double rnd = Ninaloader.random.nextDouble();
			long delay2 = RingUPDATE_DELAY + (long) (5 * 60 * 1000 * rnd);

			// log.debug("setting timer to " + delay2);
			EL.get().registerTimerCB(delay2, updateRingMaintainCB2);

		}

		void registerTestMeridian1NN() {
			double rnd = Ninaloader.random.nextDouble();
			;
			long delay2 = AbstractNNSearchManager.veryLongUPDATE_DELAY + (long) (veryLongUPDATE_DELAY * rnd);

			// log.debug("setting timer to " + delay2);
			EL.get().registerTimerCB(delay2, MeridianTest);

		}

		void registerDirectPing() {
			double rnd = Ninaloader.random.nextDouble();
			long delay2 = randomPing_DELAY + (long) (randomPing_DELAY * rnd);

			// log.debug("setting timer to " + delay2);
			EL.get().registerTimerCB(delay2, directPingCB);
		}

		/**
		 * random node
		 */
		void pingRandomNodes() {
			registerDirectPing();
			// TODO Auto-generated method stub
			AddressIF target;
			// number of knns

			// K=knns[Ninaloader.random.nextInt(knns.length)];

			final int num = defaultDirectPing;

			addControlTraffic(num);

			final Set<AddressIF> nodes = new HashSet<AddressIF>(num);
			findTwoCommunicationNodesFroDetour(num, new CB1<ArrayList<AddressIF>>() {

				@Override
				protected void cb(CBResult result, final ArrayList<AddressIF> arg1) {
					// TODO Auto-generated method stub
					switch (result.state) {
					case OK: {

						MainGeneric.execMain.execute(new Runnable() {

							public void run() {

								if (arg1 != null && !arg1.isEmpty()) {
									int realNum = Math.min(num, arg1.size());
									nodes.addAll(arg1.subList(0, realNum));

									// Ninaloader.logPingHistory=null;
									Iterator<AddressIF> ier = nodes.iterator();
									try {

										Ninaloader.logPingHistory.append("DirectPing: \n Expetected K: " + realNum
												+ "\n" + "Timer: " + Ninaloader.getUptimeStr() + "\n");
										while (ier.hasNext()) {
											AddressIF targeter = ier.next();

											// myself
											if (targeter.equals(Ninaloader.me)) {
												continue;
											}

											String ip = targeter.getHostname();
											if (ip == null || ip.isEmpty()) {
												ip = NetUtil
														.byteIPAddrToString(((NetAddress) targeter).getByteIPAddr());
											}
											// log.info("$: current IP: " +ip);
											double rtt = MainGeneric.doPingDirect(ip);
											// rtt is
											if (rtt >= 0) {
												NodesPair lat = new NodesPair(targeter, Ninaloader.me, rtt);

												// cachedHistoricalNeighbors.addElement(lat);

												if (!instance.g_rings.Contains(targeter)) {
													// add the ping result into
													// the ring
													/*
													 * if(!pendingNeighbors.
													 * containsKey(targeter)){
													 * addPendingNeighbor(
													 * targeter);
													 * pendingNeighbors.get(
													 * targeter).addSample(rtt);
													 * }else{
													 * pendingNeighbors.get(
													 * targeter).addSample(rtt);
													 * 
													 * }
													 */
												} else {
													instance.g_rings.nodesCache.get(targeter).addSample(rtt);
												}
												targetCandidates.add(targeter);
											}

											Ninaloader.logPingHistory.append(ip + " " + rtt + "\n");
										}
										Ninaloader.logPingHistory.flush();

									} catch (IOException e) {

										e.printStackTrace();
									} finally {

									}
									nodes.clear();

								}

							}

						});

						break;
					}
					default: {

						log.warn("can not extract nodes for ping");
						return;
					}
					}

				}

			});

			// List<AddressIF> list1=new ArrayList<AddressIF>(100);
			// list1.addAll(AllAliveNodes);
			// Collections.shuffle(list1);

		}

		/**
		 * only record the result, do not analyze the performance
		 */
		void TestKNNBasedOnMeridian() {

			// register
			registerTestMeridian1NN();
			//
			addWarmExperience();

			// final int
			// expectedKNN=knns[Ninaloader.random.nextInt(knns.length)];
			final int expectedKNN = 1;

			// registerTestMeridian1NN();

			/*
			 * Set<AddressIF> targets=new HashSet<AddressIF>(1);
			 * instance.g_rings.getRandomNodes(targets, 2);
			 * targets.add(AddressFactory.create(Ninaloader.me));
			 */
			final AddressIF target = PUtil.getRandomObject(targetCandidates);
			// final AddressIF
			// target=PUtil.getRandomObject(AllAlivecachedNodeStrings);
			// final AddressIF target=

			if (target == null) {
				registerTestMeridian1NN();
				return;
			}

			queryKNearestNeighbors(target, expectedKNN, new CB1<List<NodesPair>>() {
				@Override
				protected void cb(CBResult result, final List<NodesPair> arg1) {
					// TODO Auto-generated method stub
					switch (result.state) {
					case OK: {
						if (arg1 != null && arg1.size() > 0) {

							// totalSingleObjKNN++;
							log.info("KNN:\nExpected: " + expectedKNN + ", real: " + arg1.size());

							double ClosestNodes = -1;
							double KNNGains = -1;
							double absoluteError = -1;
							double RelativeError = -1;
							double coverageRate = -1;

							// bestNodes.clear();
							double time = arg1.get(0).elapsedTime;
							int totalSendingMessage = arg1.get(0).totalSendingMessage * 2;

							// record
							try {
								String header = "\nTestMeridian\n";
								logKNNPerformance(null, Ninaloader.logKNN, ClosestNodes, KNNGains, absoluteError,
										RelativeError, totalSendingMessage, time, (arg1.size() + 0.0) / expectedKNN,
										expectedKNN, arg1, target, header, coverageRate);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							/*
							 * registerTestMeridian1NN() ; return;
							 */
							break;

						} else {
							log.warn("empty returned KNNs");
						}
						break;
					}
					case TIMEOUT:
					case ERROR: {
						log.warn("FAILED to measure! ");
						break;
					}
					}
					registerTestMeridian1NN();
				}
			});
		}

		/**
		 * test the Meridian
		 */
		void TestKNNBasedOnMeridian1() {
			//
			addWarmExperience();

			final int expectedKNN = knns[Ninaloader.random.nextInt(knns.length)];
			if (Ninaloader.me == null) {
				registerTestMeridian1NN();
				return;
			}
			// registerTestMeridian1NN();

			/*
			 * Set<AddressIF> targets=new HashSet<AddressIF>(1);
			 * instance.g_rings.getRandomNodes(targets, 2);
			 * targets.add(AddressFactory.create(Ninaloader.me));
			 */
			final AddressIF target = PUtil.getRandomObject(targetCandidates);
			// final AddressIF target=

			queryKNearestNeighbors(target, expectedKNN, new CB1<List<NodesPair>>() {
				@Override
				protected void cb(CBResult result, final List<NodesPair> arg1) {
					// TODO Auto-generated method stub
					switch (result.state) {
					case OK: {
						if (arg1 != null && arg1.size() > 0) {

							// totalSingleObjKNN++;

							log.info("KNN:\nExpected: " + expectedKNN + ", real: " + arg1.size());

							if (target.equals(Ninaloader.me)) {

								/*
								 * //save current elements
								 * cachedHistoricalNeighbors.addElement(arg1);
								 * 
								 * if(cachedHistoricalNeighbors==null||
								 * cachedHistoricalNeighbors.isEmpty()){
								 * //registerTestMeridian1NN() ; //return;
								 * log.warn("empty cached KNNs"); break; } else{
								 * SortedList<NodesPair<AddressIF>> bestNodes =
								 * new SortedList<NodesPair<AddressIF>>(new
								 * NodesPairComp());
								 * bestNodes.addAll(cachedHistoricalNeighbors.
								 * getCurrentBestNearestNeighbors());
								 * 
								 * double ClosestNodes=KNNStatistics.
								 * containClosestNodes(arg1); double
								 * KNNGains=-1; double
								 * absoluteError=KNNStatistics.absoluteError(
								 * arg1,bestNodes); double
								 * RelativeError=KNNStatistics.RelativeError(
								 * arg1, bestNodes); double
								 * coverageRate=KNNStatistics.coverage(arg1,
								 * bestNodes);
								 * 
								 * //bestNodes.clear(); double
								 * time=arg1.get(0).elapsedTime; int
								 * totalSendingMessage=arg1.get(0).
								 * totalSendingMessage*2;
								 * 
								 * // record try { String header =
								 * "\nTestMeridian\n";
								 * logKNNPerformance(bestNodes,Ninaloader.logKNN
								 * ,ClosestNodes,KNNGains,absoluteError,
								 * RelativeError,totalSendingMessage,time,(arg1.
								 * size()+0.0)/expectedKNN, expectedKNN,arg1,
								 * target,header,coverageRate); } catch
								 * (IOException e) { // TODO Auto-generated
								 * catch block e.printStackTrace(); }
								 * bestNodes.clear(); registerTestMeridian1NN()
								 * ; return; break; }
								 */

							} else {

								// otherwise
								// add the elements
								// cachedHistoricalNeighborsOther.addElement(arg1);
								askcachedHistoricalNeighborsOther(target, expectedKNN,
										new CB1<HistoryNearestNeighbors<AddressIF>>() {

									@Override
									protected void cb(CBResult result,
											HistoryNearestNeighbors<AddressIF> cachedHistoricalNeighborsOther) {
										// TODO Auto-generated method stub
										switch (result.state) {
										case OK: {

											// save the elements
											if (cachedHistoricalNeighborsOther == null
													|| cachedHistoricalNeighborsOther.isEmpty()) {

												/*
												 * cachedHistoricalNeighborsOther
												 * =new HistoryNearestNeighbors<
												 * AddressIF>(expectedKNN);
												 * cachedHistoricalNeighborsOther
												 * .addElement(arg1);
												 */
												log.warn("no cached KNNs are found,");
												// registerTestMeridian1NN() ;
												break;
											} else {

												log.info("history size: "
														+ cachedHistoricalNeighborsOther.getCurrentSize());
												// SuccessRateForSingleTargetKNN.add((arg1.size()+0.0)/expectedKNN);

												double ClosestNodes = KNNStatistics.containClosestNodes(arg1);
												double KNNGains = -1;
												double absoluteError = KNNStatistics.absoluteError(arg1,
														cachedHistoricalNeighborsOther
																.getCurrentBestNearestNeighbors());
												double RelativeError = KNNStatistics.RelativeError(arg1,
														cachedHistoricalNeighborsOther
																.getCurrentBestNearestNeighbors());
												double coverageRate = KNNStatistics.coverage(arg1,
														cachedHistoricalNeighborsOther
																.getCurrentBestNearestNeighbors());

												double time = arg1.get(0).elapsedTime;
												int totalSendingMessage = arg1.get(0).totalSendingMessage;

												// record
												try {
													String header = "\nTestMeridianOther\n";
													logKNNPerformance(
															cachedHistoricalNeighborsOther
																	.getCurrentBestNearestNeighbors(),
															Ninaloader.logKNN, ClosestNodes, KNNGains, absoluteError,
															RelativeError, totalSendingMessage, time,
															(arg1.size() + 0.0) / expectedKNN, expectedKNN, arg1,
															target, header, coverageRate);
												} catch (IOException e) {
													// TODO Auto-generated catch
													// block
													e.printStackTrace();
												}

												if (cachedHistoricalNeighborsOther != null) {
													cachedHistoricalNeighborsOther.clear();
												}
												log.warn("ask OK");
												break;
											}

										}
										case TIMEOUT:
										case ERROR: {
											log.warn("ask failed");
											break;
										}

										}

										// registerTestMeridian1NN() ;
										// return;
									}

								});

							}
							// only success, we register
							// registerTestMeridian1NN() ;

						} else {
							log.info(
									"************************\n FAILED to query KNN!\nSet<NodesPair> is empty!\n************************");
							// registerTestMeridian1NN() ;
							break;
						}

						break;
					}
					case ERROR:
					case TIMEOUT: {
						log.warn("failed to query!");
						// registerTestMeridian1NN() ;
						break;
					}

					}
					// ====================================
					registerTestMeridian1NN();
				}
			});
		}

		void performRingManagement(int selection) {

			// the number of each ring, is k, which is proportional to logN

			// Find all full rings

			// empty rings
			int defaultClustergingLandmarks = 5;

			if (instance.g_rings.CurrentNodes < defaultClustergingLandmarks) {
				log.warn("@ fail to manage rings");
				registerRingManagerTimer();
				return;
			}

			int numRings = instance.g_rings.getNumberOfRings();
			List<Integer> eligibleRings = new ArrayList<Integer>();
			for (int i = 0; i < numRings; i++) {
				// Test if the ring is eligible for ring management
				if (instance.g_rings.eligibleForReplacement(i)) {
					eligibleRings.add(i);
				}
			}
			if (eligibleRings.isEmpty()) {
				registerRingManagerTimer();
				return;
			}

			Collections.shuffle(eligibleRings);
			// Pick a random eligible ring
			final int selectedRing = eligibleRings.get(0).intValue();
			eligibleRings.clear();
			eligibleRings = null;

			lockRing2.lock();

			try {

				// TODO: remove old records, like RemoteState - soft-state
				final Vector<AddressIF> primaryRingNodes = new Vector<AddressIF>(2);
				primaryRingNodes.addAll(instance.g_rings.primaryRing.get(selectedRing));

				int pSize = primaryRingNodes.size();
				final Vector<AddressIF> secondaryRingNodes = new Vector<AddressIF>(2);
				secondaryRingNodes.addAll(instance.g_rings.secondaryRing.get(selectedRing));

				final int sSize = secondaryRingNodes.size();
				int secondSize = sSize;
				final Vector<AddressIF> candidates = new Vector<AddressIF>(10);
				Iterator<AddressIF> ier = primaryRingNodes.iterator();

				// TODO: remove nodes that do not response in T time slots
				long curr_time = System.currentTimeMillis();
				final long expirationStamp = curr_time - ConcentricRing.RS_EXPIRATION;

				while (ier.hasNext()) {
					AddressIF tmp = ier.next();
					// cached latency
					if (instance.g_rings.nodesCache.containsKey(tmp)) {
						RemoteState<AddressIF> rs = instance.g_rings.nodesCache.get(tmp);

						if (rs.getLastUpdateTime() > expirationStamp) {
							candidates.add(tmp);
						} else {
							// remove nodes
							pSize--;
							instance.g_rings.nodesCache.remove(tmp);
							instance.g_rings.eraseNode(tmp);
							continue;
						}
					}

				}
				ier = secondaryRingNodes.iterator();
				while (ier.hasNext()) {
					AddressIF tmp = ier.next();
					// cached latency
					if (instance.g_rings.nodesCache.containsKey(tmp)) {
						RemoteState<AddressIF> rs = instance.g_rings.nodesCache.get(tmp);
						if (rs.getLastUpdateTime() > expirationStamp) {
							candidates.add(tmp);
						} else {
							// remove nodes
							secondSize--;
							instance.g_rings.nodesCache.remove(tmp);
							instance.g_rings.eraseNode(tmp);
							continue;
						}
					}

				}
				if (candidates.size() <= 1) {
					registerRingManagerTimer();
					return;
				}

				// clear the rings
				primaryRingNodes.clear();
				secondaryRingNodes.clear();

				int len;
				// -----------------------------------------
				// TODO:

				// default 1: Meridian, compute maximum hypervolume

				// (1) latency matrix
				Iterator<AddressIF> ier2 = candidates.iterator();
				// final Barrier barrier = new Barrier(true);
				final StringBuffer errorBuffer = new StringBuffer();
				final Set<NodesPair> cachedM = new HashSet<NodesPair>(10);

				addControlTraffic(candidates.size() * candidates.size());

				Collections.shuffle(candidates);
				int total = candidates.size();

				int primarySize = Math.min(total, instance.g_rings.primarySize);
				int secondarySize = Math.max(0, total - primarySize);

				int counter = 0;
				while (counter < primarySize) {
					primaryRingNodes.add(candidates.get(counter));
					counter++;
				}
				// reset
				if (secondarySize > 0) {
					int counter2 = 0;
					while (counter2 < secondarySize) {
						secondaryRingNodes.add(candidates.get(counter2 + counter));
						counter2++;
					}
				}

				candidates.clear();
				// ==========================================================
				instance.g_rings.setRingMembers(selectedRing, primaryRingNodes, secondaryRingNodes);
				secondaryRingNodes.clear();
				primaryRingNodes.clear();

			} finally {
				lockRing2.unlock();
			}

			// TODO: gossip with other nodes
			registerRingManagerTimer();
			/*
			 * while (ier2.hasNext()) { barrier.fork();
			 * AbstractNNSearchManager.TargetProbes(candidates, ier2.next(), new
			 * CB1<String>() { protected void cb(CBResult ncResult, String
			 * errorString) { switch (ncResult.state) { case OK: { // log.info(
			 * "$: Target Probes are issued"); } case ERROR: case TIMEOUT: {
			 * break; } } } }, new CB2<Set<NodesPair>, String>() {
			 * 
			 * 
			 * protected void cb(CBResult result, Set<NodesPair> arg1, String
			 * arg2) { // TODO Auto-generated method stub switch (result.state)
			 * { case OK: { cachedM.addAll(arg1);
			 * 
			 * } case ERROR: case TIMEOUT: {
			 * 
			 * // remove nodes, //
			 * Meridian_MClient.instance.g_rings.eraseNode(remoteNode); //
			 * Meridian_MClient.instance.rs_map.remove(remoteNode);
			 * 
			 * break; } } barrier.join(); }
			 * 
			 * }); }
			 * 
			 * // (2) reduceSetByN EL.get().registerTimerCB(barrier, new CB0() {
			 * protected void cb(CBResult result) { String errorString; if
			 * (errorBuffer.length() == 0) { errorString = new
			 * String("Success"); } else { errorString = new
			 * String(errorBuffer); }
			 * 
			 * if(cachedM==null||cachedM.isEmpty()){ registerRingManagerTimer();
			 * return; } log .debug(
			 * "$@ @ ready for computing geometry operation"); // TODO: assume
			 * that all nodes are alive here! int len = candidates.size();
			 * double[] latencyMatrix = new double[len * len]; for (int i = 0; i
			 * < len; i++) { for (int j = 0; j < len; j++) { latencyMatrix[i *
			 * len + j] = 0; } } // -----------------------------------------
			 * Iterator<NodesPair> ier3 = cachedM.iterator(); int from = 0, to =
			 * 0; while (ier3.hasNext()) { NodesPair tmp = ier3.next(); from =
			 * candidates.indexOf(tmp.startNode); to =
			 * candidates.indexOf(tmp.endNode); //use offset latencyMatrix[from
			 * * len + to] = tmp.rtt; } //
			 * ----------------------------------------- Vector<AddressIF>
			 * deletedNodes = new Vector<AddressIF>( 10);
			 * 
			 * instance.g_rings.reduceSetByN(candidates, deletedNodes, sSize,
			 * latencyMatrix); primaryRingNodes.addAll(candidates);
			 * secondaryRingNodes.addAll(deletedNodes);
			 * 
			 * instance.g_rings.setRingMembers(selectedRing, primaryRingNodes,
			 * secondaryRingNodes); // TODO: gossip with other nodes
			 * registerRingManagerTimer();
			 * 
			 * } });
			 */

		}

		public Set<AddressIF> pickGossipNodes() {
			// TODO: add pending nodes from lists
			Set<AddressIF> nodes = new HashSet<AddressIF>(10);

			instance.g_rings.getRandomNodes(nodes, defaultGossip);

			// a random node
			// int size=pendingNeighbors.size();
			int num = 5;
			int realNum = Math.min(num, pendingNeighbors.keySet().size());
			for (int i = 0; i < realNum; i++) {
				AddressIF pending = PUtil.getRandomObject(pendingNeighbors.keySet());

				if (pending != null) {
					if (!pending.equals(Ninaloader.me) && !nodes.contains(pending)) {
						nodes.add(pending);
					}
				}
			}
			nodes.remove(Ninaloader.me);

			// reset
			if (nodes.isEmpty()) {
				resetBootstrapNeighbors();

				realNum = Math.min(num, pendingNeighbors.keySet().size());
				for (int i = 0; i < realNum; i++) {
					AddressIF pending = PUtil.getRandomObject(pendingNeighbors.keySet());

					if (pending != null) {
						if (!pending.equals(Ninaloader.me) && !nodes.contains(pending)) {
							nodes.add(pending);
						}
					}
				}

			}

			return nodes;
		}

		public void run() {
			// TODO Auto-generated method stub
			init();
		}
	}

	// ======================================================
	public void maintainPendingCBs(long currentTimeMillis) {
		// TODO Auto-generated method stub
		Iterator<Vector<cachedKNNs4NonRingNodes>> values = pendingKNNs4NonRings.values().iterator();
		while (values.hasNext()) {
			Vector<cachedKNNs4NonRingNodes> tmp = values.next();
			Iterator<cachedKNNs4NonRingNodes> ier = tmp.iterator();
			while (ier.hasNext()) {
				cachedKNNs4NonRingNodes tmpCB = ier.next();

				// timeouted
				if (currentTimeMillis - tmpCB.registeredTime > ConcentricRing.RS_EXPIRATION) {
					ier.remove();
				}
			}
		}
		PUtil.gc();
	}

	public static Lock lockRing2 = new ReentrantLock();

	public static Lock lockWarm = new ReentrantLock();
}
