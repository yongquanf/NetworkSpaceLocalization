package edu.NUDT.pdl.Nina.KNN;

import java.util.Map;

import edu.NUDT.pdl.Nina.StableNC.lib.RemoteState;
import edu.harvard.syrah.sbon.async.comm.AddressIF;

//TODO: measure the IP prefix

public class ConcentricRing<T> {

	// Toss remote state of nodes if we haven't heard from them for thirty
	// minutes
	// This allows us to keep around a list of RTTs for the node even if
	// we currently aren't using its coordinate for update
	public static long RS_EXPIRATION = 30 * 60 * 1000;
	// target max number of remote states kept
	// set to be larger than MAX_NEIGHBORS
	public final static int MAX_RS_MAP_SIZE = 128;

	final public static long MAX_PING_RESPONSE_TIME = 10 * 60 * 1000; // ten
	// minutes
	public static boolean VERBOSE = false;

	protected static edu.harvard.syrah.prp.Log logger = new edu.harvard.syrah.prp.Log(ConcentricRing.class);

	int g_initGossipInterval_s; // Initial gossip period
	int g_numInitIntervalRemain; // Number of initial gossip periods
	int g_ssGossipInterval_s; // Steady state gossip period
	int g_replaceInterval_s; // Ring replacement period

	public T g_rendvNode; // Rendavous node for this node. {0,0} means
	// this node does not need one
	public RingSet<T> g_rings; // Rings for this node

	// public AddressIF g_localAddr; // IP address of this node

	// public HashMap<T, RemoteState<T>> rs_map;

	boolean PLANET_LAB_SUPPORT = false;
	// public LatencyCache g_icmpCache;

	// public List<NodeIdentRendv> g_seedNodes;

	public ConcentricRing(T me, int nodesPerRing) {
		// rs_map=new HashMap<T, RemoteState<T>>(10);

		// primary 100, secondary 100, base 2
		g_rings = new RingSet<T>(me, nodesPerRing, nodesPerRing, 2);
		// measurement cache, 1000 items, 10 seconds
		// g_icmpCache = new LatencyCache(1000, 10);
		g_rendvNode = me;
		// g_seedNodes=new ArrayList<NodeIdentRendv>(10);
	}

	// Sets the gossip interval
	void setGossipInterval(int initial_s, int initial_length, int steady_state_s) {
		g_initGossipInterval_s = initial_s;
		g_numInitIntervalRemain = initial_length;
		g_ssGossipInterval_s = steady_state_s;
	}

	// Sets the ring replacement interval
	void setReplaceInterval(int seconds) {
		g_replaceInterval_s = seconds;
	}

	// Sets the rendavous node for this node. {0,0} means no rendavous node
	void setRendavousNode(T addr) {
		g_rendvNode = addr;

	}

	// Add seed nodes
	void addSeedNode(AddressIF addr) {
		NodeIdentRendv tmp = new NodeIdentRendv();
		tmp.copy(addr, null);
		// g_seedNodes.add(tmp);
	}

	// Return the rendavous node for this node
	T returnRendv() {
		return g_rendvNode;
	}

	public static long lastMaintenanceStamp = 0;

	public static long MAINTENANCE_PERIOD = 5 * 60 * 1000; // 5 minutes

	/**
	 * maintain the ring,
	 * 
	 */
	public synchronized boolean processSample(T remoteAddr, double latency, boolean can_add, long curr_time,
			Map<AddressIF, RemoteState<AddressIF>> pendingNeighbors, double offset) {

		// RemoteState<T> addr_rs =rs_map.get(remoteAddr);
		// if (addr_rs == null) {
		// if (!can_add) {

		// return false;
		// }
		// addHost(remoteAddr);
		// addr_rs = rs_map.get(remoteAddr);
		// }
		// add sample to history, then get smoothed rtt based on percentile
		// addr_rs.addSample(latency,curr_time);
		if (g_rendvNode != null && g_rendvNode.equals(remoteAddr)) {
			System.err.println("Can not add yourself into Ring!!");
			return false;
		}

		// latency
		g_rings.insertNode(remoteAddr, (int) Math.round(latency), curr_time, offset);

		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.harvard.syrah.pyxida.nc.lib.NCClientIF#addHost(T)
	 */
	/*
	 * synchronized public boolean addHost(T addr) { if
	 * (rs_map.containsKey(addr)) { return false; }
	 * 
	 * RemoteState<T> rs = new RemoteState<T>(addr); rs_map.put(addr, rs);
	 * return true; }
	 */

}
