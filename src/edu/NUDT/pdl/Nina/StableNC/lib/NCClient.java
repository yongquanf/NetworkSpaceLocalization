package edu.NUDT.pdl.Nina.StableNC.lib;

/*
 * Pyxida - a network coordinate library
 * 
 * Copyright 2008 Jonathan Ledlie and Peter Pietzuch, modified by Yongquan Fu
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import edu.NUDT.pdl.Nina.Ninaloader;
import edu.NUDT.pdl.Nina.KNN.AbstractNNSearchManager;
import edu.NUDT.pdl.Nina.Sim.SimCoord.HVivaldiTest_sim;
import edu.NUDT.pdl.Nina.Sim.SimCoord.HVivaldiTest_sim.Node;
import edu.NUDT.pdl.Nina.StableNC.nc.StableManager;
import edu.harvard.syrah.prp.Log;
import edu.harvard.syrah.sbon.async.comm.AddressIF;

/**
 * 
 * A class that is responsible for updating the local Vivaldi coordinates, both
 * at the system and application level, and also maintaining the state of remote
 * hosts that support Vivaldi.
 * 
 * @author Ericfu
 * 
 * @param <T>
 *            the type of the unique identifier of a host
 * 
 *            A note about time: Try to not use System.currentTimeMillis or
 *            similar references to the system's time in this code. This code
 *            should be able to run in simulation, where arbitrary times are
 *            passed in as parameters.
 */
public class NCClient<T> implements NCClientIF<T> {

	public boolean UseFitFunction = false;

	// varying curvature
	public boolean UseVaryingCurvature = false;

	public boolean UseSwitching = false;
	// group based on PSO
	public boolean UseGroupEnhancement = false;

	//icdcs version
	public boolean UseICDCS08Hyperbolic = false;
	
	
	// whether we use only one neighbor
	public boolean oneNeighborPerUpdate = false;

	protected boolean symmetricUpdate = true;
	public boolean stableProcess = false; // stable or not

	protected static edu.harvard.syrah.prp.Log logger = new edu.harvard.syrah.prp.Log(
			NCClient.class);
	// protected static Logger crawler_log =
	// Logger.getLogger(ThinNCClient.class.getName());

	protected static Logger crawler_log = Logger.getLogger(NCClient.class
			.getName());
	public static boolean debugCrawler = true;

	// include even good events in output, not just problems
	public static boolean debugGood = false;
	public static final boolean SIMULATION = true;
	public static boolean debug = false;

	public static boolean KEEP_STATS = true;
	public static boolean DEBUG = false;
	public static boolean VERBOSE = false;
	public static boolean SIM = false;

	public static final byte VERSION_04 = 0x04;
	public static final byte CURRENT_VERSION = VERSION_04;

	public static double COORD_ERROR = 0.20; // c_e parameter
	public static double COORD_CONTROL = 0.2; // c_c parameter

	// The last element of the coordinate is a "height" away from the Euclidean
	// space
	//public static boolean USE_HEIGHT = StableManager.useHeight;

	// Note: for larger installations, use e.g. 512
	// Try to minimize our error between up to MAX_NEIGHBORS guys at once
	public static int MAX_NEIGHBORS = 32;

	// Toss remote state of nodes if we haven't heard from them for thirty
	// minutes
	// This allows us to keep around a list of RTTs for the node even if
	// we currently aren't using its coordinate for update
	public static long RS_EXPIRATION = 30 * 60 * 1000;

	final public static long MAINTENANCE_PERIOD = 5* 60 * 1000; // ten minutes

	final public static long MAX_PING_RESPONSE_TIME = 10 * 60 * 1000; // ten
	// minutes

	// target max number of remote states kept
	// set to be larger than MAX_NEIGHBORS
	public final static int MAX_RS_MAP_SIZE = 128;

	private long lastMaintenanceStamp = 0;

	public static Random random = new Random();

	// completely ignore any RTT larger than ten seconds
	public static double OUTRAGEOUSLY_LARGE_RTT = 20000.0;

	// range from origin where pull of gravity is 1
	 public static double GRAVITY_DIAMETER = 1024.;
	//public static double GRAVITY_DIAMETER = 0; // no gravity
	// We reject remote coords with any component larger than this value
	// This is to prevent serialization problems from leaking into coords
	// we're actually going to use
	// Gravity should keep everybody within this ball
	public static double MAX_DIST_FROM_ORIGIN = 600000.;

	/*
	 * The minimum time to wait before kicking a neighbor off the list if that
	 * neighbor has not been pinged yet.
	 */
	public static long MIN_UPDATE_TIME_TO_PING = 2 * MAINTENANCE_PERIOD;

	/*
	 * The weight to be used in calculating the probability
	 */

	final static protected NumberFormat nf = NumberFormat.getInstance();

	final static protected int NFDigits = 2;

	// to indicate whether is likely to be high-err node
	int tick = 0;

	static {
		if (nf.getMaximumFractionDigits() > NFDigits) {
			nf.setMaximumFractionDigits(NFDigits);
		}
		if (nf.getMinimumFractionDigits() > NFDigits) {
			nf.setMinimumFractionDigits(NFDigits);
		}
		nf.setGroupingUsed(false);
	}

	protected final int num_dims;

	public volatile Coordinate sys_coord;
	
	
	
	protected Coordinate prev_coord; // history coordinate

	// error should always be less than or equal to MAX_ERROR
	// and greater than 0
	protected double error;
	
	public volatile double TooLargeMovement=0;

	public static final double MAX_ERROR = 2.;

	// keeping an EWMA of some of these things gives skewed results
	// so we use a filter
	public static int RUNNING_STAT_HISTORY = 1024;

	protected WindowStatistic running_sys_error;
	protected WindowStatistic running_Nearest_error;

	// protected WindowStatistic running_app_error;
	protected EWMAStatistic running_sys_dd;
	// protected EWMAStatistic running_app_dd;
	// protected EWMAStatistic running_neighbors_used;
	// protected EWMAStatistic running_relative_diff;
	protected EWMAStatistic running_sys_update_frequency;

	// icdcs 08
	// static value, identical for all nodes
	public EWMAStatistic averageLatency;

	// protected EWMAStatistic running_app_update_frequency;
	// protected EWMAStatistic running_age;
	// protected EWMAStatistic running_gravity;

	// keep the list of neighbors around for computing statistics
	protected final List<RemoteState<T>> neighbors;

	// this is returned to querier of our coords so he knows how stale they are
	protected long time_of_last_sys_update = -1;

	protected final ObserverList obs_list;

	protected final Map<T, RemoteState<T>> rs_map;

	public Map<T, RemoteState<T>> getRs_map() {
		return rs_map;
	}

	protected final Set<T> hosts;

	protected T local_addr;

	// ------------------------------------------------
	// constant parameters
	double C_SIG_COORD_CHANGE_THRESH_MS = 10;
	int CONV_STATE = -1;
	int STAB_STATE = 1;
	int WINDOW_SIZE = 10;
	double TIME_STEP = 0.25;
	// -------------------------------------------------
	// stability test
	int _space;
	int _state = CONV_STATE;

	int _stable;
	double _pred_err = -1;

	int _round;

	int _window_size = WINDOW_SIZE;
	double _stab_err;
	int _conv_steps;
	double _min_err = 10000000;

	public boolean initialized = false;
	public boolean isAlive = false;

	// ----------------------------
	// 0 original Vivaldi, 1 in icdcs 08, the Magnitude of Euclidean, the
	// Magnitude of 2 Hyperbilic

	// int choiceOfUpdate=2;

	public static void setRealLargeNetworkParameters() {
		MAX_NEIGHBORS = 512;

		// three days
		RS_EXPIRATION = 3 * 24 * 60 * 60 * 1000;
	}

	/**
	 * Creates a new instance. Typically an application should only have one
	 * instance of this class, as it only needs one set of Vivaldi coordinates.
	 * 
	 * @param _num_dims
	 *            the number of Euclidian dimensions coordinates should have
	 */
	public NCClient(int _num_dims) {
		num_dims = _num_dims;

		sys_coord = new Coordinate(num_dims);

		// sys_coord.bump();

		error = MAX_ERROR;
		neighbors = new ArrayList<RemoteState<T>>();

		obs_list = new ObserverList();
		rs_map = new ConcurrentHashMap<T, RemoteState<T>>();
		hosts = Collections.unmodifiableSet(rs_map.keySet());

		running_sys_dd = new EWMAStatistic();
		averageLatency = new EWMAStatistic();

		if (KEEP_STATS) {
			running_sys_update_frequency = new EWMAStatistic();
			running_sys_error = new WindowStatistic(RUNNING_STAT_HISTORY);
			running_Nearest_error = new WindowStatistic(RUNNING_STAT_HISTORY);
			// running_neighbors_used = new EWMAStatistic();
			// running_relative_diff = new EWMAStatistic();
			// running_age = new EWMAStatistic();
			// running_gravity = new EWMAStatistic();
		}

		not_stable();
		_avg_avgerr.clear();

	}

	// for debugging simulations
	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.harvard.syrah.pyxida.nc.lib.NCClientIF#setLocalID(T)
	 */
	public void setLocalID(T _local_addr) {
		local_addr = _local_addr;
	}

	// See Lua IMC 2005, Pietzuch WORLDS 2005, Ledlie ICDCS 2006
	// for description of these statistics
	// protected ApplicationStatistics computeApplicationStatistics() {
	// }

	// ApplicationStatistics appStats = new ApplicationStatistics();

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.harvard.syrah.pyxida.nc.lib.NCClientIF#toString()
	 */
	synchronized public String toString() {
		return new String("[sc=" + sys_coord + ",dd="
				+ nf.format(running_sys_dd.get()) + ",er=" + nf.format(error)
				+ "]");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.harvard.syrah.pyxida.nc.lib.NCClientIF#getStatistics()
	 */
	synchronized public Hashtable<String, Double> getStatistics() {
		Hashtable<String, Double> stats = new Hashtable<String, Double>();
		for (int i = 0; i < num_dims; i++) {
			stats.put("sys_coord_" + i, sys_coord.coords[i]);
			stats.put("app_coord_" + i, sys_coord.coords[i]);
		}
		stats.put("er", error);
		stats.put("dd", running_sys_dd.get());
		return stats;
	}

	synchronized public double getDistanceDelta() {
		return running_sys_dd.get();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.harvard.syrah.pyxida.nc.lib.NCClientIF#reset()
	 */
	synchronized public void reset() {
		sys_coord.reset();
		error = MAX_ERROR;
		rs_map.clear();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.harvard.syrah.pyxida.nc.lib.NCClientIF#getNumDimensions()
	 */
	synchronized public int getNumDimensions() {
		return num_dims;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.harvard.syrah.pyxida.nc.lib.NCClientIF#getSystemCoords()
	 */
	synchronized public Coordinate getSystemCoords() {
/*		if (!sys_coord.isHyperbolic) {
			return new Coordinate(sys_coord);
		} else {*/
		  Coordinate tmp = sys_coord.makeCopy();
		  tmp.r_error=this.getSystemError();
			return tmp;
		//}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.harvard.syrah.pyxida.nc.lib.NCClientIF#getSystemError()
	 */
	synchronized public double getSystemError() {
		return error;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.harvard.syrah.pyxida.nc.lib.NCClientIF#getAge(long)
	 */
	synchronized public long getAge(long curr_time) {
		if (curr_time < time_of_last_sys_update)
			return 0;
		return curr_time - time_of_last_sys_update;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.harvard.syrah.pyxida.nc.lib.NCClientIF#getObserverList()
	 */
	synchronized public ObserverList getObserverList() {
		return obs_list;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.harvard.syrah.pyxida.nc.lib.NCClientIF#addHost(T)
	 */
	synchronized public boolean addHost(T addr) {
		if (rs_map.containsKey(addr)) {
			return false;
		}

		RemoteState<T> rs = new RemoteState<T>(addr);
		rs_map.put(addr, rs);
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.harvard.syrah.pyxida.nc.lib.NCClientIF#addHost(T,
	 * edu.harvard.syrah.pyxida.nc.lib.Coordinate, double, long, boolean)
	 */

	synchronized public boolean addHost(T addr, Coordinate _r_coord,
			double r_error, long curr_time, boolean can_update) {
		RemoteState<T> rs = null;
		if (rs_map.containsKey(addr)) {
			if (!can_update) {
				return false;
			}
			rs = rs_map.get(addr);
		} else {
			rs = new RemoteState<T>(addr);
			rs_map.put(addr, rs);
		}

		Coordinate r_coord = _r_coord.makeCopy();
		rs.assign(r_coord, r_error, curr_time);
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.harvard.syrah.pyxida.nc.lib.NCClientIF#removeHost(T)
	 */

	synchronized public boolean removeHost(T addr) {
		if (rs_map.containsKey(addr)) {
			return true;
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.harvard.syrah.pyxida.nc.lib.NCClientIF#containsHost(T)
	 */
	synchronized public boolean containsHost(T addr) {
		return rs_map.containsKey(addr);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.harvard.syrah.pyxida.nc.lib.NCClientIF#getHosts()
	 */
	synchronized public Set<T> getHosts() {
		return hosts;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.harvard.syrah.pyxida.nc.lib.NCClientIF#processSample(T,
	 * edu.harvard.syrah.pyxida.nc.lib.Coordinate, double, double, long, long,
	 * boolean)
	 */

	synchronized public boolean processSample(T addr, Coordinate _r_coord,
			double r_error, double sample_rtt, long sample_age, long curr_time,
			boolean can_add) {

		this.isAlive = true;

		int id = getIdFromAddr(addr);
		if (VERBOSE)
			logger.info(id + " START");

		assert (_r_coord != sys_coord);
		assert (_r_coord != null);
		assert (sys_coord != null);

		if (!sys_coord.isCompatible(_r_coord)) {
			if (DEBUG)
				logger.info("INVALID " + id + " s " + sample_rtt
						+ " NOT_COMPAT " + _r_coord.getVersion());
			return false;
		}

		// There is a major problem with the coord.
		// However, if this is happening, it will probably
		// happen again and again.
		// Note that error is checked and fixed in updateError()
		if (!sys_coord.isValid() || Double.isNaN(error)) {
			System.err.println("Warning: resetting Vivaldi coordinate");
			if (DEBUG)
				logger.info(id + " RESET, USE_HEIGHT=" + _r_coord.USE_HEIGHT);
			reset();
		}

		if (r_error <= 0. || r_error > MAX_ERROR || Double.isNaN(r_error)
				|| !_r_coord.isValid()) {
			if (DEBUG)
				logger.info(id + " BUSTED his coord is busted: r_error "
						+ r_error + " r_coord " + _r_coord);
			return false;
		}

		if (sample_rtt > OUTRAGEOUSLY_LARGE_RTT) {
			if (DEBUG)
				System.err.println("Warning: skipping huge RTT of "
						+ nf.format(sample_rtt) + " from " + addr);
			if (DEBUG)
				logger.info(id + " HUGE " + sample_rtt);
			return false;
		}

		RemoteState<T> addr_rs = rs_map.get(addr);
		if (addr_rs == null) {
			if (!can_add) {
				if (DEBUG)
					logger.info(id + " NO_ADD");
				return false;
			}
			addHost(addr);
			addr_rs = rs_map.get(addr);
		}
		Coordinate r_coord = _r_coord.makeCopy();

		// add sample to history, then get smoothed rtt based on percentile
		addr_rs.addSample(sample_rtt, sample_age, r_coord, r_error, curr_time);
		addr_rs.addMeasurementErrors(sample_rtt, r_coord, this.sys_coord);
		// even if we aren't going to use him this time around, we remember this
		// RTT

		/* update the error estimation and measurement for each neighbor */
		update_neigherr();

		/* store the previous coordinates */
		save_prev_coord();

		/*
		 * check the excution state of SVivaldi, switch to stablizing state if
		 * necessary
		 */
		// uncoment the following three lines of code for Stabilized Vivaldi
		check_state();

		// coordinate

		if (sys_coord.atOrigin()) {
			sys_coord.bump();
		}

		boolean didUpdate = false;
		int sample_size = addr_rs.getSampleSize();

		// smoothed
		double smoothed_rtt = addr_rs.getSample();
		// System.out.println("before stable: smoothed_rtt: "+smoothed_rtt);
		// or error elimination
		if (this.stableProcess) {
			if (_state == STAB_STATE) {
				smoothed_rtt = addr_rs.err_eliminate();
			}
		}
		this.averageLatency.add(smoothed_rtt);
		// System.out.println("after stable: smoothed_rtt: "+smoothed_rtt);
		if (addr_rs.isValid(curr_time)) {
			if (!stabilized() || neighbors.size() < MAX_NEIGHBORS)
				addNeighbor(addr_rs);
			// simpleCoordinateUpdate(addr, r_coord, r_error, smoothed_rtt,
			// sample_rtt, sample_age, sample_size, curr_time);
			// first update our error
			updateError(addr, r_coord, r_error, smoothed_rtt, sample_rtt,
					sample_age, sample_size, curr_time);

			// System.out.println("smoothed_rtt: "+smoothed_rtt+", sample_rtt: "+sample_rtt);
			// next, update our system-level coordinate

			// updateSystemCoordinate(curr_time);
			// updateCoordinateWithOneNeighbor(addr_rs,curr_time);
			if (!this.UseSwitching && oneNeighborPerUpdate) {
				// symmetric updated
				if (UseICDCS08Hyperbolic) {
					// TODO update: error
					updateCoordinateWithOneNeighbor_ICDCS08(addr_rs, curr_time);
				} else {
					if (!this.UseGroupEnhancement) {
						// updateError(addr, r_coord, r_error, smoothed_rtt,
						// sample_rtt, sample_age, sample_size, curr_time);
						updateCoordinateWithOneNeighbor(addr_rs, curr_time);
					} else {
						// this.updateWithGroupEnhancement(addr_rs, curr_time);
						localGlobalBestFromNeighbors(curr_time);
					}
				}

			} else if (!this.UseSwitching) {
				// updateSystemCoordinate(curr_time);
				updateSystemCoordinate_Vivaldi_Fit(curr_time);
			} else {
				// switching
				this.updateBySwitching(addr_rs, curr_time);
			}

			didUpdate = true;

		} else {
			if (DEBUG) {
				String reason;
				if (addr_rs.getSampleSize() < RemoteState.MIN_SAMPLE_SIZE) {
					reason = "TOO_FEW";
				} else if (addr_rs.getSample() <= 0) {
					reason = "sample is " + addr_rs.getSample();
				} else if (addr_rs.getLastError() <= 0.) {
					reason = "error is " + addr_rs.getLastError();
				} else if (addr_rs.getLastUpdateTime() <= 0) {
					reason = "last update " + addr_rs.getLastUpdateTime();
				} else if (addr_rs.getLastCoordinate().atOrigin()) {
					reason = "AT_ORIGIN";
				} else {
					reason = "UNKNOWN";
				}
				logger.info("INVALID " + id + " s " + sample_rtt + " ss "
						+ smoothed_rtt + " c " + sample_size + " " + reason);

			}
		}

		if (lastMaintenanceStamp < curr_time - MAINTENANCE_PERIOD) {
			performMaintenance(curr_time);
			lastMaintenanceStamp = curr_time;
		}

		return didUpdate;
	}

	synchronized public boolean processSample_noStable(T addr,
			Coordinate _r_coord, double r_error, double sample_rtt,
			long sample_age, long curr_time, boolean can_add) {

		this.isAlive = true;

		int id = getIdFromAddr(addr);
		if (debugCrawler && debugGood)
			crawler_log.info(id + " START");

		assert (_r_coord != sys_coord);
		assert (_r_coord != null);
		assert (sys_coord != null);

		if (!sys_coord.isCompatible(_r_coord)) {
			if (debugCrawler)
				crawler_log.info("INVALID " + id + " s " + sample_rtt
						+ " NOT_COMPAT " + _r_coord.getVersion());
			return false;
		}

		// There is a major problem with the coord.
		// However, if this is happening, it will probably
		// happen again and again.
		// Note that error is checked and fixed in updateError()
		if (!sys_coord.isValid() || Double.isNaN(error)) {
			System.err.println("Warning: resetting Vivaldi coordinate");
			if (debugCrawler || SIMULATION)
				//crawler_log.info(id + " RESET, USE_HEIGHT=" + USE_HEIGHT);
			reset();
		}

		if (r_error <= 0. || r_error > MAX_ERROR || Double.isNaN(r_error)
				|| !_r_coord.isValid()) {
			if (debugCrawler)
				crawler_log.info(id + " BUSTED his coord is busted: r_error "
						+ r_error + " r_coord " + _r_coord);
			return false;
		}

		if (sample_rtt > OUTRAGEOUSLY_LARGE_RTT) {
			if (debug)
				System.err.println("Warning: skipping huge RTT of "
						+ nf.format(sample_rtt) + " from " + addr);
			if (debugCrawler)
				crawler_log.info(id + " HUGE " + sample_rtt);
			return false;
		}

		RemoteState<T> addr_rs = rs_map.get(addr);
		if (addr_rs == null) {
			if (!can_add) {
				if (debugCrawler)
					crawler_log.info(id + " NO_ADD");
				return false;
			}
			addHost(addr);
			addr_rs = rs_map.get(addr);
		}
		Coordinate r_coord = _r_coord.makeCopy();

		// add sample to history, then get smoothed rtt based on percentile
		addr_rs.addSample(sample_rtt, sample_age, r_coord, r_error, curr_time);
		addr_rs.addMeasurementErrors(sample_rtt, r_coord, this.sys_coord);
		// even if we aren't going to use him this time around, we remember this
		// RTT

		/* update the error estimation and measurement for each neighbor */
		update_neigherr();

		/* store the previous coordinates */
		save_prev_coord();

		/*
		 * check the excution state of SVivaldi, switch to stablizing state if
		 * necessary
		 */
		// uncoment the following three lines of code for Stabilized Vivaldi
		check_state();

		// even if we aren't going to use him this time around, we remember this
		// RTT

		if (sys_coord.atOrigin()) {
			sys_coord.bump();
		}

		boolean didUpdate = false;
		int sample_size = addr_rs.getSampleSize();
		double smoothed_rtt = addr_rs.getSample();

		this.averageLatency.add(smoothed_rtt);

		if (addr_rs.isValid(curr_time)) {
			addNeighbor(addr_rs);
			// first update our error
			updateError(addr, r_coord, r_error, smoothed_rtt, sample_rtt,
					sample_age, sample_size, curr_time);

			// next, update our system-level coordinate

			if (!this.UseSwitching && oneNeighborPerUpdate) {
				// symmetric updated
				if (UseICDCS08Hyperbolic) {
					// TODO update: error
					updateCoordinateWithOneNeighbor_ICDCS08(addr_rs, curr_time);
				} else {
					if (!this.UseGroupEnhancement) {
						// updateError(addr, r_coord, r_error, smoothed_rtt,
						// sample_rtt, sample_age, sample_size, curr_time);
						updateCoordinateWithOneNeighbor(addr_rs, curr_time);
					} else {
						// this.updateWithGroupEnhancement(addr_rs, curr_time);
						localGlobalBestFromNeighbors(curr_time);
					}
				}

			} else if (!this.UseSwitching) {
				// updateSystemCoordinate(curr_time);
				updateSystemCoordinate_Vivaldi_Fit(curr_time);
			} else {
				// switching
				this.updateBySwitching(addr_rs, curr_time);
			}

			/*
			 * if(oneNeighborPerUpdate){
			 * 
			 * //symmetric updated if(UseICDCS08Hyperbolic){ //TODO update:
			 * error updateCoordinateWithOneNeighbor_ICDCS08(addr_rs,curr_time);
			 * } else{ if(!this.UseGroupEnhancement){ //updateError(addr,
			 * r_coord, r_error, smoothed_rtt, sample_rtt, sample_age,
			 * sample_size, curr_time);
			 * updateCoordinateWithOneNeighbor(addr_rs,curr_time); }else{
			 * this.updateWithGroupEnhancement(addr_rs, curr_time); } }
			 * 
			 * }else{
			 * 
			 * //updateSystemCoordinate(curr_time);
			 * updateSystemCoordinate_Vivaldi_Fit(curr_time); }
			 */
			didUpdate = true;

		} else {
			if (debugCrawler) {
				String reason;
				if (addr_rs.getSampleSize() < RemoteState.MIN_SAMPLE_SIZE) {
					reason = "TOO_FEW";
				} else if (addr_rs.getSample() <= 0) {
					reason = "sample is " + addr_rs.getSample();
				} else if (addr_rs.getLastError() <= 0.) {
					reason = "error is " + addr_rs.getLastError();
				} else if (addr_rs.getLastUpdateTime() <= 0) {
					reason = "last update " + addr_rs.getLastUpdateTime();
				} else if (addr_rs.getLastCoordinate().atOrigin()) {
					reason = "AT_ORIGIN";
				} else {
					reason = "UNKNOWN";
				}
				crawler_log.info("INVALID " + id + " s " + sample_rtt + " ss "
						+ smoothed_rtt + " c " + sample_size + " " + reason);

			}
		}

		// System.out.println ("maint?");
		if (lastMaintenanceStamp < curr_time - MAINTENANCE_PERIOD) {
			performMaintenance(curr_time);
			lastMaintenanceStamp = curr_time;
		}
		// vary the curvature
		/*
		 * if(this.UseVaryingCurvature){ //if(this.stabilized()){
		 * if(this._state==this.STAB_STATE&&this.error>0.5){
		 * updateCurvature(curr_time); //} } }
		 */
		return didUpdate;

	}

	private Map<T, Integer> addr2id = new HashMap<T, Integer>();

	private int idCounter = 0;

	private static Log log=new Log(NCClient.class);

	//threshold of tiv alert
	//tiv alert
 static double thresoldDistance=100;

	/**
	 * init the coordinate based on optimization process
	 * 
	 * @param remoteLandmarks
	 * @param r_errors
	 * @param RTTs
	 */
	public void initFirstCoord(Set<T> addrs, Set<Coordinate> remoteLandmarks,
			Set r_errors, Set RTTs) {
		int repeats = 100;
		int len = addrs.size();

		for (long round = 0; round < repeats; round++) {
			Iterator<T> ier = addrs.iterator();
			Iterator<Coordinate> ier_Coord = remoteLandmarks.iterator();
			Iterator ier_r_errors = r_errors.iterator();
			Iterator ierRTTs = RTTs.iterator();
			while (ier.hasNext()) {
				T dst = ier.next();
				Coordinate sysCoord = ier_Coord.next();
				simpleCoordinateUpdate(dst, sysCoord, ((Double) ier_r_errors
						.next()).doubleValue(), ((Double) ierRTTs.next())
						.doubleValue(), round);
				// processSample(dst, sysCoord,
				// ((Double)ier_r_errors.next()).doubleValue(),
				// ((Double)ierRTTs.next()).doubleValue(), );
			}
		}
		initialized = true;
	}

	// If remote nodes are already represented by ints, just use them
	// otherwise, translate into more easily read-able ID
	protected int getIdFromAddr(T addr) {
		if (DEBUG) {

			if (addr instanceof Integer) {
				return ((Integer) addr).intValue();
			}

			if (!addr2id.containsKey(addr)) {
				addr2id.put(addr, idCounter);
				idCounter++;
			}
			return addr2id.get(addr);
		}

		return (0);
	}

	/**
	 * best performance, in terms of Vivaldi
	 * @param addr
	 * @param r_coord
	 * @param r_error
	 * @param rtt
	 * @param curr_time
	 */
	public void simpleCoordinateUpdate(T addr, Coordinate r_coord,
			double r_error, double rtt, long curr_time) {

		if(this.sys_coord==null){
			log.warn("sys_coord is null");
		}else if(r_coord==null){
			log.warn("r_coord is null");
		}
		double distance = sys_coord.distanceTo(r_coord);


		// scale
		if (sys_coord.isHyperbolic) {
			distance = distance * sys_coord.num_curvs;
		}
		
		if (sys_coord.USE_HEIGHT && distance > 0) {
			// scale by curvature
			distance += sys_coord.coords[sys_coord.coords.length - 1]
					+ r_coord.coords[sys_coord.coords.length - 1];
		}
		
		
		
		while (distance == 0. || Double.isNaN(distance)) {
			sys_coord.bump();
			distance = sys_coord.distanceTo(r_coord);
		}

		
		// line 1 in NSDI paper
		// w_s = w_i / (w_i + w_j)
		double sample_weight = error / (r_error + error);
		if (sample_weight == 0.) {
			logger.warn("weight of zero");
			return;
		}

		//tiv alert
		if(TIVAlert(error,r_error ,rtt,distance)){
			//too large error
			return;
		}
		//assert (rtt > 0.);

		// line 2 in NSDI paper
		// compute epsilon aka sys_sample_error
		// epsilon = | |x_i - x_j| - l(i,j) | / l(i,j)

		// error of sample
		double sample_error = distance - rtt;
		double epsilon = Math.abs(sample_error) / rtt;

		
		
		/*
		 * if(!sys_coord.isHyperbolic){ //icdcs 08 vivaldi
		 * sample_error=sample_error/distance;
		 * 
		 * } if(sys_coord.isHyperbolic){ //icdcs08 Hyperbolic double sum = 0.0;
		 * sum=Math.pow(Math.cosh(distance),2)-1; sum = Math.sqrt(sum);
		 * sample_error=sample_error/sum;
		 * 
		 * 
		 * }
		 */

		if (epsilon < 0) {
			epsilon = 0;
		}
		if (epsilon > MAX_ERROR) {
			epsilon = MAX_ERROR;
		}

		if (KEEP_STATS) {
			running_sys_error.add(epsilon);
		}

		Vec force = new Vec(sys_coord.getNumDimensions(),
				sys_coord.isHyperbolic);
		// EWMA on error
		// alpha = c_e * w_s
		double alpha = sample_weight * COORD_ERROR;

		// line 4 in NSDI paper
		// w_i = (alpha * epsilon) + ( (1-alpha) * w_i )
		error = (epsilon * alpha) + ((1 - alpha) * error);

		// cannot return null b/c distance is not 0
		Vec unitVector = sys_coord.getDirection(r_coord);
		unitVector.scale(sample_error * sample_weight);
		force.add(unitVector);
		
		if (sys_coord.USE_HEIGHT) {
			force.direction[force.direction.length - 1] = -1.
					* force.direction[force.direction.length - 1];
		}
		force.scale(COORD_CONTROL);

		DEBUG = false;
		if (DEBUG) {
			int remote_id = getIdFromAddr(addr);
			String info = "UPDATE " + remote_id + " re " + nf.format(epsilon)
					+ " rtt " + nf.format(rtt) + " raw " + " dist "
					+ nf.format(distance) + " lE " + nf.format(error) + " rE "
					+ nf.format(r_error) + " rV " + r_coord.getVersion()
					+ " lc " + sys_coord + " rc " + r_coord;

			logger.info(info);
		}

		/*
		 * if (VERBOSE) { int remote_id = getIdFromAddr(addr); logger.info("f "
		 * + id + " age " + Math.round((curr_time -
		 * neighbor.getLastUpdateTime()) / 1000.) + " er " + sampleError +
		 * " sw " + sampleWeight + " comb " + (sampleError sampleWeight));
		 * logger.info("t " + force.getLength() + " " + force); }
		 */

		// include "gravity" to keep coordinates centered on the origin
		// include "gravity" to keep coordinates centered on the origin 
		if (GRAVITY_DIAMETER > 0) { 
			Vec gravity = sys_coord.asVectorFromZero(true); 
			if (gravity.getLength() > 0) { 
				// scale gravity s.t. it increases polynomially with distance
				double force_of_gravity =
					Math.pow(gravity.getLength() / GRAVITY_DIAMETER, 2.); 
				gravity.makeUnit();
				gravity.scale(force_of_gravity); // add to total force
				force.subtract(gravity);
				
				//if (keepStatistics) { running_gravity.add(force_of_gravity); }
			}
		}

		sys_coord.add(force);
		sys_coord.checkHeight();
		double distance_delta = force.getLength(sys_coord.num_curvs);

		running_sys_dd.add(distance_delta);
		time_of_last_sys_update = curr_time;
			
	}
	
	
	/**
	 * update the target's coordinate
	 * @param target
	 * @param targetCoordinate
	 * @param r_error
	 * @return
	 */
	public static Coordinate updateProxyTargetCoordinate(
			AddressIF target, Coordinate targetCoordinate_1,
			double r_error,Coordinate refCoordinate,double ref_error,double rtt) {
		
		//illegal measurement
		if(rtt <= 0 || rtt > 5*60*1000 || r_error+ref_error==0||refCoordinate==null) return targetCoordinate_1.makeCopy();
		
		//reset the coordinate
		if(targetCoordinate_1==null){
			log.warn("target coordinate is null");
			return new Coordinate(AbstractNNSearchManager.myNCDimension);
		}
		
		
		Coordinate targetCoordinate=targetCoordinate_1.makeCopy();
		double distance = targetCoordinate.distanceTo(refCoordinate);
		/**
		 * invalid update
		 */
		if(distance<0){
			return targetCoordinate.makeCopy();
		}
		
		if (targetCoordinate.USE_HEIGHT && distance > 0) {
			// scale by curvature
			distance += targetCoordinate.coords[targetCoordinate.coords.length - 1]
					+ refCoordinate.coords[targetCoordinate.coords.length - 1];
		}
		
		
		
		while (distance == 0. || Double.isNaN(distance)) {
			targetCoordinate.bump();
			distance = targetCoordinate.distanceTo(refCoordinate);
		}

		
		// line 1 in NSDI paper
		// w_s = w_i / (w_i + w_j)
		double sample_weight = r_error / (r_error + ref_error);
		if (sample_weight == 0.) {
			logger.warn("weight of zero");			
			return targetCoordinate.makeCopy();
		}

		//tiv alert
		if(TIVAlert(r_error,ref_error,rtt,distance)){
			log.warn("TIV alert, my Error: "+r_error+", remote error: "+ref_error);
			//too large error
			return targetCoordinate.makeCopy();
		}
		//assert (rtt > 0.);

		// line 2 in NSDI paper
		// compute epsilon aka sys_sample_error
		// epsilon = | |x_i - x_j| - l(i,j) | / l(i,j)

		// error of sample
		double sample_error = distance - rtt;
		double epsilon = Math.abs(sample_error) / rtt;

		
		
		/*
		 * if(!sys_coord.isHyperbolic){ //icdcs 08 vivaldi
		 * sample_error=sample_error/distance;
		 * 
		 * } if(sys_coord.isHyperbolic){ //icdcs08 Hyperbolic double sum = 0.0;
		 * sum=Math.pow(Math.cosh(distance),2)-1; sum = Math.sqrt(sum);
		 * sample_error=sample_error/sum;
		 * 
		 * 
		 * }
		 */

		if (epsilon < 0) {
			epsilon = 0;
		}
		if (epsilon > MAX_ERROR) {
			epsilon = MAX_ERROR;
		}


		Vec force = new Vec(targetCoordinate.getNumDimensions(),
				targetCoordinate.isHyperbolic);
		// EWMA on error
		// alpha = c_e * w_s
		double alpha = sample_weight * COORD_ERROR;

		// line 4 in NSDI paper
		// w_i = (alpha * epsilon) + ( (1-alpha) * w_i )
		r_error = (epsilon * alpha) + ((1 - alpha) * r_error);

		targetCoordinate.r_error=r_error;
		
		// cannot return null b/c distance is not 0
		Vec unitVector = targetCoordinate.getDirection(refCoordinate);
		unitVector.scale(sample_error * sample_weight);
		force.add(unitVector);
		//height
		if (targetCoordinate.USE_HEIGHT) {
			force.direction[force.direction.length - 1] = -1.
					* force.direction[force.direction.length - 1];
		}
		force.scale(COORD_CONTROL);

		//gravity
		if (GRAVITY_DIAMETER > 0) { 
			Vec gravity = targetCoordinate.asVectorFromZero(true); 
			if (gravity.getLength() > 0) { 
				// scale gravity s.t. it increases polynomially with distance
				double force_of_gravity =
					Math.pow(gravity.getLength() / GRAVITY_DIAMETER, 2.); 
				gravity.makeUnit();
				gravity.scale(force_of_gravity); // add to total force
				force.subtract(gravity);
				
				//if (keepStatistics) { running_gravity.add(force_of_gravity); }
			}
		}
		
		
		targetCoordinate.add(force);
		targetCoordinate.checkHeight();
		
		return targetCoordinate.makeCopy();
	}

	//TIV alert
	/**
	 * alert when |d-d1|>t, iff error < threshold
	 * error: uncertainty
	 * imc06
	 * @param distance 
	 */
	protected static boolean TIVAlert(double myError,double yourError, double rtt, double coordDist){
		
		double threshold=0.32;
		if(myError<threshold&&yourError<threshold){
			if(Math.abs(rtt-coordDist)>thresoldDistance){
				return true;
			}
		}
		return false;
	}
	
	
	protected void updateError(T addr, Coordinate r_coord, double r_error,
			double smoothed_rtt, double sample_rtt, long sample_age,
			int sample_size, long curr_time) {
		// get the coordinate distance
		double sys_distance = sys_coord.distanceTo(r_coord);

		double str = -1;
		double actual_str = -1;
		double expect = -1;
		// scale
		if (sys_coord.isHyperbolic) {

			if (this.UseICDCS08Hyperbolic) {
				str = sys_coord.num_curvs / this.averageLatency.get();
				actual_str = smoothed_rtt * str;
				expect = sys_distance * this.averageLatency.get()
						/ sys_coord.num_curvs;

			} else {
				// our method
				sys_distance = sys_distance * sys_coord.num_curvs;
				if (sys_coord.USE_HEIGHT && sys_distance > 0) {
					// scale by curvature
					sys_distance += sys_coord.coords[sys_coord.coords.length - 1]
							+ r_coord.coords[sys_coord.coords.length - 1];
				}

			}
		}

		if (Double.isNaN(sys_distance) || Double.isInfinite(sys_distance)) {
			if (DEBUG)
				logger.info("bad distance " + sys_distance);
			return;
		}

		if (sys_distance == 0.) {
			if (DEBUG)
				logger.info("bad distance " + sys_distance);
			return;
		}

		// get sample error in terms of coordinate distance and sample rtt
		// Note that smoothed_rtt must be greater than zero
		// or we wouldn't have entered this function

		// Note that app_sample_error is only giving us a limited amount of info
		// because his app coord is not going over the wire

		assert (smoothed_rtt > 0.);

		// line 2 in NSDI paper
		// compute epsilon aka sys_sample_error
		// epsilon = | |x_i - x_j| - l(i,j) | / l(i,j)
		double sys_sample_error;
		if (this.UseICDCS08Hyperbolic) {
			sys_sample_error = Math.abs(expect * str - actual_str) / actual_str;
		} else {
			sys_sample_error = Math.abs(sys_distance - smoothed_rtt)/ smoothed_rtt;
		}
		VERBOSE = false;
		if (VERBOSE) {
			int remote_id = getIdFromAddr(addr);
			String info =
			// "lID "+local_addr+" rID "+
			"UPDATE " + remote_id + " re " + nf.format(sys_sample_error)
					+ " rtt " + nf.format(smoothed_rtt) + " raw "
					+ nf.format(sample_rtt) + " age " + sample_age + " dist "
					+ nf.format(sys_distance) + " ssize " + sample_size
					+ " lE " + nf.format(error) + " rE " + nf.format(r_error)
					+ " rV " + r_coord.getVersion() + " lc " + sys_coord
					+ " rc " + r_coord;

			logger.info(info);
		}

		if (sys_sample_error < 0) {
			sys_sample_error = 0;
		}
		if (sys_sample_error > MAX_ERROR) {
			sys_sample_error = MAX_ERROR;
		}

		// EWMA on error
		// lines 1 and 3 in NSDI paper
		// (1) w_s = w_i / (w_i + w_j)
		// (3) alpha = c_e * w_s
		double alpha = error / (error + r_error) * COORD_ERROR;

		// line 4 in NSDI paper
		// w_i = (alpha * epsilon) + ( (1-alpha) * w_i )
		error = (sys_sample_error * alpha) + ((1 - alpha) * error);

		if (_pred_err < 0) {
			this._pred_err = sys_sample_error;
		} else {
			_pred_err = (19 * _pred_err + error) / 20.0;
			if (_pred_err > 1.0)
				_pred_err = 1.0;
		}

		if (KEEP_STATS) {
			running_sys_error.add(sys_sample_error);
		}
	}

	
	protected boolean addNeighbor(RemoteState<T> guy) {
		boolean added = false;
		if (!neighbors.contains(guy)) {
			neighbors.add(guy);
			if (VERBOSE)
				logger.info("addNeighbor: adding " + guy);
			added = true;
		}
		if (neighbors.size() > MAX_NEIGHBORS) {
			RemoteState<T> neighbor = neighbors.remove(0);
			if (VERBOSE)
				logger.info("addNeighbor: removing " + neighbor);

		}
		return added;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.harvard.syrah.pyxida.nc.lib.NCClientIF#hasAllNeighbors()
	 */
	public boolean hasAllNeighbors() {
		if (neighbors.size() < MAX_NEIGHBORS) {
			return false;
		} else {
			return true;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.harvard.syrah.pyxida.nc.lib.NCClientIF#stabilized()
	 */
	public boolean stabilized() {
		// Could also do something more sophisticated with the app level
		// coordinates
		// but this should be a rough approximation.
		// May want to be on the lookout for transitional behavior.
		// That is, we don't want nodes to suddenly act differently based on
		// this value.
		// 0ld 0.3;
		// 0.5 stablized
		double threshold = 0.5;
		if (error < threshold) {
			return true;
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.harvard.syrah.pyxida.nc.lib.NCClientIF#printNeighbors()
	 */
	public String printNeighbors() {
		String toPrint = "";
		for (Iterator<RemoteState<T>> i = neighbors.iterator(); i.hasNext();) {
			RemoteState<T> A_rs = i.next();
			toPrint = toPrint + "," + A_rs.getAddress();
		}
		return toPrint;
	}

	protected boolean removeNeighbor(RemoteState<T> guy) {
		if (neighbors.contains(guy)) {
			neighbors.remove(guy);
			return true;
		}
		return false;
	}

	/*
	 * Pick a "random" neighbor from the neighbors list to send a ping request
	 * For each neighbor, calculate the weight (probability that it will be sent
	 * a ping) If, on the off chance, that no neighbor was picked, then randomly
	 * pick one.
	 */

	/*
	 * JL Note: Eric used a weight here which might be nicer...
	 */

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.harvard.syrah.pyxida.nc.lib.NCClientIF#getNeighborToPing(long)
	 */
	synchronized public T getNeighborToPing(long curr_time) {
		List<RemoteState<T>> grayingNeighbors = new ArrayList<RemoteState<T>>();

		double avgTimeSincePing = 0.;
		int pingedNeighborCount = 0, nonPingedNeighborCount = 0;
		for (RemoteState<T> neighbor : neighbors) {

			if (curr_time - neighbor.getLastPingTime() > MIN_UPDATE_TIME_TO_PING) {
				grayingNeighbors.add(neighbor);
			}

			if (neighbor.getLastPingTime() > 0) {
				avgTimeSincePing += (curr_time - neighbor.getLastPingTime());
				pingedNeighborCount++;
			} else {
				nonPingedNeighborCount++;
			}
		}
		logger.debug("getNeighborToPing: pinged/non " + pingedNeighborCount
				+ "/" + nonPingedNeighborCount + " avgTimeSincePing "
				+ (avgTimeSincePing / pingedNeighborCount) + " minTime "
				+ MIN_UPDATE_TIME_TO_PING);

		if (grayingNeighbors.size() > 0) {
			Collections.shuffle(grayingNeighbors);
			RemoteState<T> neighbor = grayingNeighbors.get(0);
			neighbor.setLastPingTime(curr_time);

			// reduce the likely size of graying neighbors
			// if it is (relatively) too big, but only if it is (absolutely) too
			// big
			if (grayingNeighbors.size() > neighbors.size() / 8
					&& grayingNeighbors.size() > 3) {
				MIN_UPDATE_TIME_TO_PING /= 2;
				if (MIN_UPDATE_TIME_TO_PING <= 1)
					MIN_UPDATE_TIME_TO_PING = 1;
				logger
						.info("getNeighborToPing: lowered MIN_UPDATE_TIME_TO_PING "
								+ MIN_UPDATE_TIME_TO_PING);
			}

			logger.info("getNeighborToPing: picking from grayingNeighbors "
					+ grayingNeighbors.size() + "/" + neighbors.size() + " "
					+ neighbor.getAddress());

			return neighbor.getAddress();
		}

		// we do want to get some nodes in grayingNeighbors
		MIN_UPDATE_TIME_TO_PING *= 2;
		logger
				.info("getNeighborToPing: returning null, increased MIN_UPDATE_TIME_TO_PING "
						+ MIN_UPDATE_TIME_TO_PING);
		return null;
	}

	/**
	 * get a random neighbor
	 * @return
	 */
	public synchronized T getRandomNeighbor(){
		
		if(neighbors.isEmpty()){
			return null;
		}else{
			int index=Ninaloader.random.nextInt(neighbors.size());
			return neighbors.get(index).addr;
		}
		
		
	}
	/*
	 * protected void updateSystemCoordinate(long curr_time) {
	 * 
	 * // figure out the oldest sample we are going to use // and recalculate
	 * the nearest neighbor // as a side effect long oldestSample = curr_time;
	 * 
	 * double nearest_neighbor_distance = Double.MAX_VALUE; double
	 * nearest_neighbor_RTT=Double.MAX_VALUE; double
	 * nearest_neighbor_eRTT=Double.MAX_VALUE; Collections.shuffle(neighbors);
	 * 
	 * for (RemoteState<T> neighbor : neighbors) {
	 * 
	 * //estimated
	 * 
	 * double distance = sys_coord.distanceTo(neighbor.getLastCoordinate()); if
	 * (distance < nearest_neighbor_distance) { nearest_neighbor_distance =
	 * distance; nearest_neighbor_eRTT=neighbor.getSample(); //nearest_neighbor
	 * = neighbor.getLastCoordinate(); } //real double RTT=neighbor.getSample();
	 * if(RTT<nearest_neighbor_RTT){ nearest_neighbor_RTT=RTT; } if
	 * (oldestSample > neighbor.getLastUpdateTime()) { oldestSample =
	 * neighbor.getLastUpdateTime(); } } //nearest neighbor error double
	 * nearestNeighbor_re
	 * =Math.abs(nearest_neighbor_RTT-nearest_neighbor_eRTT)/nearest_neighbor_RTT
	 * ; this.running_Nearest_error.add(nearestNeighbor_re);
	 * 
	 * 
	 * double sampleWeightSum = 0.; for (RemoteState<T> neighbor : neighbors) {
	 * // double distance = sys_coord.distanceTo(neighbor.getLastCoordinate());
	 * sampleWeightSum += neighbor.getLastUpdateTime() - oldestSample; } assert
	 * (sampleWeightSum >= 0.);
	 * 
	 * int tau=1; //System.out.println("Neighbor Size:"+neighbors.size());
	 * 
	 * for(int i=0;i<tau;i++){
	 * 
	 * Vec force = new Vec(sys_coord.getNumDimensions());
	 * 
	 * for (RemoteState<T> neighbor : neighbors) {
	 * 
	 * //TODO: if a neighbor has high TIV with landmarks //skip this neighbor
	 * 
	 * double distance = sys_coord.distanceTo(neighbor.getLastCoordinate());
	 * while (distance == 0.) { sys_coord.bump(); distance =
	 * sys_coord.distanceTo(neighbor.getLastCoordinate()); }
	 * //System.out.println
	 * ("A: "+sys_coord.toString()+", B: "+neighbor.getLastCoordinate
	 * ().toString()); //scale if(sys_coord.isHyperbolic){
	 * distance=distancesys_coord.num_curvs; if (NCClient.USE_HEIGHT && distance
	 * > 0) { //scale by curvature
	 * distance+=sys_coord.coords[sys_coord.coords.length
	 * -1]+neighbor.getLastCoordinate().coords[sys_coord.coords.length-1]; } }
	 * 
	 * if(Double.isNaN(distance)||Double.isInfinite(distance)){ if (DEBUG)
	 * logger.info("bad distance " +
	 * distance+", A: "+sys_coord.toString()+", B: "
	 * +neighbor.getLastCoordinate().toString()); continue; }
	 * 
	 * // cannot return null b/c distance is not 0 Vec unitVector =
	 * sys_coord.getDirection(neighbor.getLastCoordinate());
	 * 
	 * //stable RTT double latency = neighbor.getSample(); //stable
	 * if(NCClient.stableProcess){ if(_state == STAB_STATE) {
	 * latency=neighbor.err_eliminate(); } }
	 * 
	 * 
	 * // ~line 1 in NSDI paper (except we are using our current confidence)
	 * double sample_weight = error / (neighbor.getLastError() + error); if
	 * (sample_weight == 0.) continue;
	 * 
	 * // error of sample double sample_error = distance-latency;
	 * 
	 * //normalized if(!sys_coord.isHyperbolic){ //icdcs 08 vivaldi Coordinate
	 * x=sys_coord.makeCopy(); Coordinate
	 * y=neighbor.getLastCoordinate().makeCopy();
	 * 
	 * double sum = 0.0; for (int i = 0; i < num_dims; ++i) { final double
	 * abs_dist = x.coords[i]-y.coords[i]; sum += (abs_dist abs_dist); } sum =
	 * Math.sqrt(sum); sample_error=sample_error/sum;
	 * 
	 * } if(sys_coord.isHyperbolic){ //icdcs08 Hyperbolic Coordinate
	 * x=sys_coord.makeCopy(); Coordinate
	 * y=neighbor.getLastCoordinate().makeCopy();
	 * 
	 * double sum = 0.0; sum=Math.pow(Math.cosh(x.distanceTo(y)),2)-1; sum =
	 * Math.sqrt(sum); sample_error=sample_error/sum;
	 * 
	 * 
	 * }
	 * 
	 * 
	 * //error! if(Double.isNaN(sample_error)){
	 * System.err.println("sample_error is NaN!"); continue; }
	 * 
	 * double decay = 1.; if (sampleWeightSum > 0) { decay =
	 * (neighbor.getLastUpdateTime() - oldestSample) / sampleWeightSum; }
	 * 
	 * if (VERBOSE) { int id = getIdFromAddr(neighbor.getAddress());
	 * logger.info("f " + id + " age " + Math.round((curr_time -
	 * neighbor.getLastUpdateTime()) / 1000.) + " er " + sample_error +
	 * " decay " + decay); }
	 * 
	 * //no decay unitVector.scale(COORD_CONTROLsample_weight sample_error);
	 * //unitVector.scale(COORD_CONTROL sample_weight sample_error decay);
	 * force.add(unitVector); }
	 * 
	 * if (USE_HEIGHT) { force.direction[force.direction.length - 1] = -1.
	 * force.direction[force.direction.length - 1]; }
	 * 
	 * if (VERBOSE) { logger.info("t " + force.getLength(sys_coord.num_curvs) +
	 * " " + force); }
	 * 
	 * // include "gravity" to keep coordinates centered on the origin if
	 * (GRAVITY_DIAMETER > 0) { Vec gravity = sys_coord.asVectorFromZero(true);
	 * if (gravity.getLength(sys_coord.num_curvs) > 0) { // scale gravity s.t.
	 * it increases polynomially with distance double force_of_gravity =
	 * Math.pow(gravity.getLength(sys_coord.num_curvs) / GRAVITY_DIAMETER, 2.);
	 * gravity.makeUnit(sys_coord.num_curvs); gravity.scale(force_of_gravity);
	 * // add to total force force.subtract(gravity);
	 * 
	 * //if (keepStatistics) { running_gravity.add(force_of_gravity); } } }
	 * 
	 * sys_coord.add(force); sys_coord.checkHeight();
	 * 
	 * double distance_delta = force.getLength(sys_coord.num_curvs);
	 * 
	 * running_sys_dd.add(distance_delta); }
	 * 
	 * 
	 * if (keepStatistics) {
	 * 
	 * if (neighbors != null) { running_neighbors_used.add(neighbors.size()); }
	 * 
	 * if (time_of_last_sys_update > 0) { long since_last_sys_update = curr_time
	 * - time_of_last_sys_update;
	 * running_sys_update_frequency.add(since_last_sys_update); } }
	 * 
	 * time_of_last_sys_update = curr_time;
	 * 
	 * }
	 */

	/**
	 * update coordinates based on switching
	 * 
	 * @param addr_rs
	 * @param curr_time
	 */
	public void updateBySwitching(RemoteState<T> addr_rs, long curr_time) {

		// if stablized or is accurate, use one neighbor
		// ||this.error<0.3, this._state==this.STAB_STATE
		// after 30 steps
		if (this.error < 0.5) {
			this.updateCoordinateWithOneNeighbor(addr_rs, curr_time);
		} else {
			this.updateSystemCoordinate_Vivaldi_Fit(curr_time);
		}

	}

	/**
	 * when updating the coordinates, we fetch the latest coordinates of
	 * neighbors remove the obsolete coordinates, immediately
	 */
	protected void updateNeighborCoordinates() {
		// in simulation mode
		if (NCClient.SIM) {
			for (RemoteState<T> neighbor : neighbors) {
				// fetch neighbors' new coordinates

				// simulator module
				Coordinate latestCoord = HVivaldiTest_sim.getInstance().nodes
						.get(((Integer) neighbor.getAddress()).intValue()).nc.sys_coord;
				neighbor.last_coords = latestCoord.makeCopy();
				neighbor.last_error = HVivaldiTest_sim.getInstance().nodes
						.get(((Integer) neighbor.getAddress()).intValue()).nc.error;
			}
		}
	}

	protected Coordinate nearest_neighbor;

	protected Vec updateSystemCoordinate_Vivaldi_Fit(long curr_time) {

		// figure out the oldest sample we are going to use
		// and recalculate the nearest neighbor
		// as a side effect
		long oldestSample = curr_time;
		double nearest_neighbor_distance = Double.MAX_VALUE;

		Collections.shuffle(neighbors);

		// history-agnostic
		// update neighbors' coordinates
		// updateNeighborCoordinates();

		// -------------------------------------------
		for (RemoteState<T> neighbor : neighbors) {
			double distance = sys_coord
					.distanceTo(neighbor.getLastCoordinate());

			if (sys_coord.isHyperbolic) {
				distance = distance * sys_coord.num_curvs;
				if (sys_coord.USE_HEIGHT && distance > 0) {
					// scale by curvature
					distance += sys_coord.coords[sys_coord.coords.length - 1]
							+ neighbor.getLastCoordinate().coords[sys_coord.coords.length - 1];
				}
			}
			if (distance < nearest_neighbor_distance) {
				nearest_neighbor_distance = distance;
				nearest_neighbor = neighbor.getLastCoordinate();
			}

			if (oldestSample > neighbor.getLastUpdateTime()) {
				oldestSample = neighbor.getLastUpdateTime();
			}
		}

		double sampleWeightSum = 0.;
		for (RemoteState<T> neighbor : neighbors) {
			// double distance =
			// sys_coord.distanceTo(neighbor.getLastCoordinate());
			sampleWeightSum += neighbor.getLastUpdateTime() - oldestSample;
		}
		assert (sampleWeightSum >= 0.);

		//
		Vec force = new Vec(sys_coord.getNumDimensions(),
				sys_coord.isHyperbolic);
		// force.isHyperbolic=sys_coord.isHyperbolic;

		for (RemoteState<T> neighbor : neighbors) {
			double distance = sys_coord
					.distanceTo(neighbor.getLastCoordinate());
			while (distance == 0.) {
				sys_coord.bump();
				distance = sys_coord.distanceTo(neighbor.getLastCoordinate());
			}
			// cannot return null b/c distance is not 0
			Vec unitVector = sys_coord.getDirection(neighbor
					.getLastCoordinate());
			double latency = neighbor.getSample();
			// stable process
			if (stableProcess) {
				if (this._state == this.STAB_STATE) {
					latency = neighbor.err_eliminate();
				}
			}

			double weight = error / (neighbor.getLastError() + error);
			if (weight == 0.)
				continue;

			// error of sample
			double sampleError = distance - latency;
			double sampleWeight = 1.;
			if (sampleWeightSum > 0) {
				sampleWeight = (neighbor.getLastUpdateTime() - oldestSample)
						/ sampleWeightSum;
			}

			if (weight > 0) {
				sampleWeight = sampleWeight * weight;
			}
			if (debugCrawler && debugGood) {
				int id = getIdFromAddr(neighbor.getAddress());
				crawler_log.info("f "
						+ id
						+ " age "
						+ Math
								.round((curr_time - neighbor
										.getLastUpdateTime()) / 1000.) + " er "
						+ sampleError + " sw " + sampleWeight + " comb "
						+ (sampleError * sampleWeight));
			}

			unitVector.scale(sampleError * sampleWeight);
			force.add(unitVector);
		}

		if (sys_coord.USE_HEIGHT) {
			force.direction[force.direction.length - 1] = -1.
					* force.direction[force.direction.length - 1];
		}
		force.scale(COORD_CONTROL);

		if (debugCrawler && debugGood) {
			crawler_log.info("t " + force.getLength(sys_coord.num_curvs) + " "
					+ force);
		}

		// include "gravity" to keep coordinates centered on the origin
		if (GRAVITY_DIAMETER > 0) {
			Vec gravity = sys_coord.asVectorFromZero(true);
			// gravity.isHyperbolic=sys_coord.isHyperbolic;

			if (gravity.getLength(sys_coord.num_curvs) > 0) {
				// scale gravity s.t. it increases polynomially with distance
				double force_of_gravity = Math.pow(gravity
						.getLength(sys_coord.num_curvs)
						/ GRAVITY_DIAMETER, 2.);
				gravity.makeUnit(sys_coord.num_curvs);
				gravity.scale(force_of_gravity); // add to total force
				force.subtract(gravity);

				// if (keepStatistics) { running_gravity.add(force_of_gravity);
				// }
			}
		}

		Vec returnedVec = null;
		if (UseFitFunction) {
			// global best

			Coordinate tmp = sys_coord.makeCopy();
			Coordinate repliCoord = tmp.makeCopy();
			tmp.add(force);
			// whether tmp is better than current place
			// fit to new position
			int fit = testError(sys_coord, tmp);
			if (fit == 0) {
				sys_coord.add(force);
				sys_coord.checkHeight();
				double distance_delta = force.getLength(sys_coord.num_curvs);
				running_sys_dd.add(distance_delta);
				//
				returnedVec = new Vec(force, sys_coord.isHyperbolic);
			} else {
				// System.out.println("not fit to new position!");
				// vary the curvature
				if (this.UseVaryingCurvature) {
					// if(this.stabilized()){
					// stablized but high-error
					if (this._state == this.STAB_STATE && this.error > 0.5) {
						updateCurvature(curr_time);
						// }
					}
				}
			}
			/*
			 * if (keepStatistics) { running_sys_dd.add(distance_delta); if
			 * (neighbors != null) {
			 * running_neighbors_used.add(neighbors.size()); }
			 * 
			 * if (time_of_last_sys_update > 0) { long since_last_sys_update =
			 * curr_time - time_of_last_sys_update;
			 * running_sys_update_frequency.add(since_last_sys_update); } }
			 */
			// if not better than current position, return itself;
			if (returnedVec == null) {
				returnedVec = repliCoord.asVectorFromZero(true);
			}
		} else {
			// no fit function
			sys_coord.add(force);
			sys_coord.checkHeight();
			double distance_delta = force.getLength(sys_coord.num_curvs);
			// System.out.println("@@: "+distance_delta);
			running_sys_dd.add(distance_delta);
		}

		time_of_last_sys_update = curr_time;
		return returnedVec;

	}

	// test the fitness of the coordinate, with the neighbors
	public double fitness(Coordinate test) {
		// remove the noise effect
		// Statistic<Double> rel_errors = new Statistic<Double>();
		double averagedErr = 0.;
		if (neighbors.size() > 0) {
			for (RemoteState<T> neighbor : neighbors) {
				Coordinate you = neighbor.getLastCoordinate();
				double dist = test.distanceTo(you);
				if (sys_coord.isHyperbolic) {
					dist *= sys_coord.num_curvs;
				}
				double re_error = Math.abs(dist - neighbor.getSample())
						/ neighbor.getSample();
				// rel_errors.add(re_error);
				averagedErr += re_error;
			}
			// return rel_errors.getPercentile(.5);
			return averagedErr / neighbors.size();
		}
		return -1; // false
	}

	public double fitness(Coordinate test, double curv) {
		// remove the noise effect
		// Statistic<Double> rel_errors = new Statistic<Double>();
		double averagedErr = 0.;
		if (neighbors.size() > 0) {
			for (RemoteState<T> neighbor : neighbors) {
				Coordinate you = neighbor.getLastCoordinate();
				double dist = test.distanceTo(you);
				if (sys_coord.isHyperbolic) {
					dist *= curv;
				}
				double re_error = Math.abs(dist - neighbor.getSample())
						/ neighbor.getSample();
				// rel_errors.add(re_error);
				averagedErr += re_error;
			}
			// return rel_errors.getPercentile(.5);
			return averagedErr / neighbors.size();
		}
		return -1; // false
	}

	// evaluate whether refined is better than origin
	// 1 origin better thaN refined
	// 0 not
	// -1 not comparable
	public int testError(Coordinate origin, Coordinate refined) {
		double originErr = fitness(origin);
		double refinedErr = fitness(refined);

		// System.out.println("origin Error: "+originErr+", refined Error: "+refinedErr);
		// soft threshold, allow temporal movement to bad position,
		// ericfu
		boolean soft = true;
		if (soft) {
			double threshold = 0.15;
			if (originErr != -1 && refinedErr != -1) {

				if (Math.abs(refinedErr - originErr) < threshold) {
					return 0;
				} else {
					return 1;
				}
			}
		} else {
			if (originErr != -1 && refinedErr != -1) {
				if (originErr < refinedErr) {
					return 1;
				} else {
					return 0;
				}
			}
		}
		return -1;
	}

	/*
	 * Periodically walk the entire rs_map and toss anybody who has expired. If
	 * the map has grown beyond the preferred size (MAX_RS_MAP_SIZE), shrink the
	 * max age that a guy can be before he gets kicked out.
	 */

	protected void performMaintenance(long curr_time) {
		if (VERBOSE)
			logger.info("performing maintenance");

		if (rs_map.size() > MAX_RS_MAP_SIZE) {
			RS_EXPIRATION = (long) (.9 * RS_EXPIRATION);
			if (VERBOSE)
				logger.info("lowered RS_EXPIRATION to " + RS_EXPIRATION
						+ " size " + rs_map.size());
		}

		final long expirationStamp = curr_time - RS_EXPIRATION;
		Set<Map.Entry<T, RemoteState<T>>> states = rs_map.entrySet();

		for (Iterator<Map.Entry<T, RemoteState<T>>> stateIter = states
				.iterator(); stateIter.hasNext();) {
			Map.Entry<T, RemoteState<T>> entry = stateIter.next();
			if (entry.getValue().getLastUpdateTime() < expirationStamp) {
				if (VERBOSE)
					logger.info("tossing " + entry.getValue().getAddress());
				removeNeighbor(entry.getValue());
				stateIter.remove();
			}
		}

		// remove neighbors that are "dead" -- the last update time is more than
		// 10 minutes (MAINTENANCE_PERIOD) ago
		// Also check that the lastPingTime is not greater than the
		// MIN_UPDATE_TIME_TO_PING

		// Toss guys we've pinged but who haven't responded.

		for (Iterator<RemoteState<T>> i = neighbors.iterator(); i.hasNext();) {
			RemoteState<T> rs = i.next();
			if (rs.getLastPingTime() - rs.getLastUpdateTime() > MAX_PING_RESPONSE_TIME) {
				logger.info("performMaintenance: removed " + rs.getAddress()
						+ " lastPing " + rs.getLastPingTime() + " lastUpdate "
						+ rs.getLastUpdateTime() + " diff "
						+ (rs.getLastPingTime() - rs.getLastUpdateTime())
						+ " > MAX " + MAX_PING_RESPONSE_TIME);

				i.remove();
				// removeNeighbor(A_rs);
			}
		}

	}

	double sum_square(Coordinate c) {
		double sum = 0.0;
		for (int i = 0; i < c.num_dims; i++) {
			sum += Math.pow(c.coords[i], 2);
		}
		return sum;
	}

	double product_coord(Coordinate c, Coordinate _c) {
		double sum = 0.0;
		assert (c.num_dims == _c.num_dims);
		for (int i = 0; i < c.num_dims; i++) {
			sum += c.coords[i] * _c.coords[i];
		}
		return sum;
	}

	synchronized public boolean Vivaldi_ProcessSample(T addr,
			Coordinate _r_coord, double r_error, double sample_rtt,
			long sample_age, long curr_time, boolean can_add) {

		int id = getIdFromAddr(addr);
		if (debugCrawler && debugGood)
			crawler_log.info(id + " START");

		assert (_r_coord != sys_coord);
		assert (_r_coord != null);
		assert (sys_coord != null);

		if (!sys_coord.isCompatible(_r_coord)) {
			if (debugCrawler)
				crawler_log.info("INVALID " + id + " s " + sample_rtt
						+ " NOT_COMPAT " + _r_coord.getVersion());
			return false;
		}

		// There is a major problem with the coord.
		// However, if this is happening, it will probably
		// happen again and again.
		// Note that error is checked and fixed in updateError()
		if (!sys_coord.isValid() || Double.isNaN(error)) {
			System.err.println("Warning: resetting Vivaldi coordinate");
			if (debugCrawler || SIMULATION)
				crawler_log.info(id + " RESET, USE_HEIGHT=" + sys_coord.USE_HEIGHT);
			reset();
		}

		if (r_error <= 0. || r_error > MAX_ERROR || Double.isNaN(r_error)
				|| !_r_coord.isValid()) {
			if (debugCrawler)
				crawler_log.info(id + " BUSTED his coord is busted: r_error "
						+ r_error + " r_coord " + _r_coord);
			return false;
		}

		if (sample_rtt > OUTRAGEOUSLY_LARGE_RTT) {
			if (debug)
				System.err.println("Warning: skipping huge RTT of "
						+ nf.format(sample_rtt) + " from " + addr);
			if (debugCrawler)
				crawler_log.info(id + " HUGE " + sample_rtt);
			return false;
		}

		RemoteState<T> addr_rs = rs_map.get(addr);
		if (addr_rs == null) {
			if (!can_add) {
				if (debugCrawler)
					crawler_log.info(id + " NO_ADD");
				return false;
			}
			addHost(addr);
			addr_rs = rs_map.get(addr);
		}
		Coordinate r_coord = _r_coord.makeCopy();

		// add sample to history, then get smoothed rtt based on percentile
		addr_rs.addSample(sample_rtt, sample_age, r_coord, r_error, curr_time);

		// even if we aren't going to use him this time around, we remember this
		// RTT

		if (sys_coord.atOrigin()) {
			sys_coord.bump();
		}

		boolean didUpdate = false;
		int sample_size = addr_rs.getSampleSize();
		double smoothed_rtt = addr_rs.getSample();

		if (addr_rs.isValid(curr_time)) {
			addNeighbor(addr_rs);
			// first update our error
			updateError(addr, r_coord, r_error, smoothed_rtt, sample_rtt,
					sample_age, sample_size, curr_time);
			// next, update our system-level coordinate
			updateSystemCoordinate(curr_time);
			// last, try to update our application-level coordinate
			// tryUpdateAppCoordinate(curr_time);
			didUpdate = true;

		} else {
			if (debugCrawler) {
				String reason;
				if (addr_rs.getSampleSize() < RemoteState.MIN_SAMPLE_SIZE) {
					reason = "TOO_FEW";
				} else if (addr_rs.getSample() <= 0) {
					reason = "sample is " + addr_rs.getSample();
				} else if (addr_rs.getLastError() <= 0.) {
					reason = "error is " + addr_rs.getLastError();
				} else if (addr_rs.getLastUpdateTime() <= 0) {
					reason = "last update " + addr_rs.getLastUpdateTime();
				} else if (addr_rs.getLastCoordinate().atOrigin()) {
					reason = "AT_ORIGIN";
				} else {
					reason = "UNKNOWN";
				}
				crawler_log.info("INVALID " + id + " s " + sample_rtt + " ss "
						+ smoothed_rtt + " c " + sample_size + " " + reason);

			}
		}

		// System.out.println ("maint?");
		if (lastMaintenanceStamp < curr_time - MAINTENANCE_PERIOD) {
			performMaintenance(curr_time);
			lastMaintenanceStamp = curr_time;
		}

		return didUpdate;
	}

	/**
	 * update according to the Hyperbolic coordinate method in ICDCS 08
	 * 
	 * @param addr_rs
	 * @param curr_time
	 */
	protected void updateCoordinateWithOneNeighbor_ICDCS08(
			RemoteState<T> addr_rs, long curr_time) {

		// figure out the oldest sample we are going to use
		// and recalculate the nearest neighbor
		// as a side effect
		long oldestSample = curr_time;

		double nearest_neighbor_distance = Double.MAX_VALUE;
		double nearest_neighbor_RTT = Double.MAX_VALUE;
		double nearest_neighbor_eRTT = Double.MAX_VALUE;
		// Collections.shuffle(neighbors);

		for (RemoteState<T> neighbor : neighbors) {

			// estimated

			double distance = sys_coord
					.distanceTo(neighbor.getLastCoordinate());

			if (distance < nearest_neighbor_distance) {
				nearest_neighbor_distance = distance;
				nearest_neighbor_eRTT = neighbor.getSample();
				// nearest_neighbor = neighbor.getLastCoordinate();
			}
			// real
			double RTT = neighbor.getSample();
			if (RTT < nearest_neighbor_RTT) {
				nearest_neighbor_RTT = RTT;
			}
			if (oldestSample > neighbor.getLastUpdateTime()) {
				oldestSample = neighbor.getLastUpdateTime();
			}
		}
		// nearest neighbor error
		// double
		// nearestNeighbor_re=Math.abs(nearest_neighbor_RTT-nearest_neighbor_eRTT)/nearest_neighbor_RTT;
		// this.running_Nearest_error.add(nearestNeighbor_re);

		double weightedAvg = 0;
		double sampleWeightSum = 0.;
		for (RemoteState<T> neighbor : neighbors) {
			// double distance =
			// sys_coord.distanceTo(neighbor.getLastCoordinate());
			sampleWeightSum += neighbor.getLastUpdateTime() - oldestSample;
			weightedAvg += neighbor.getSample();
		}
		// averageLatency
		// weightedAvg/=neighbors.size();
		double w = 0.75;

		/*
		 * if(averageLatency==0){ averageLatency=weightedAvg; }else{
		 * averageLatency=waverageLatency+(1-w)weightedAvg; }
		 */
		// real stretch
		double str = sys_coord.num_curvs / averageLatency.get();

		assert (sampleWeightSum >= 0.);

		// System.out.println("Neighbor Size:"+neighbors.size());

		Vec force = new Vec(sys_coord.getNumDimensions(),
				sys_coord.isHyperbolic);

		for (RemoteState<T> neighbor : neighbors) {

			// TODO: if a neighbor has high TIV with landmarks
			// skip this neighbor

			// TODO:add >avglatency()

			double distance = sys_coord
					.distanceTo(neighbor.getLastCoordinate());
			while (distance == 0.) {
				sys_coord.bump();
				distance = sys_coord.distanceTo(neighbor.getLastCoordinate());
			}

			if (Double.isNaN(distance) || Double.isInfinite(distance)) {
				if (DEBUG)
					logger.info("bad distance " + distance + ", A: "
							+ sys_coord.toString() + ", B: "
							+ neighbor.getLastCoordinate().toString());
				continue;
			}

			// stablization proces, by compensating the latency
			// double latency = neighbor.getSample();
			/*
			 * if(_state == STAB_STATE) { latency=addr_rs.err_eliminate(); }
			 */

			double actual_str = neighbor.getSample() * str;
			double expect = distance * averageLatency.get()
					/ sys_coord.num_curvs;

			if (neighbor.getSample() >= 0) {

				double grad = expect * str - actual_str;
				double d1 = Math.sqrt((1 + sum_square(this.sys_coord))
						* (1 + sum_square(neighbor.getLastCoordinate())))
						- product_coord(this.sys_coord, neighbor
								.getLastCoordinate());
				// if d1==1, the derivative will be infinite
				if (d1 == 1) {

					d1 = d1 + 0.1;
				}
				double d2 = Math.sqrt((1 + sum_square(neighbor
						.getLastCoordinate()))
						/ (1 + sum_square(this.sys_coord)));
				// the deriaitive
				double d = 1 / Math.sqrt(d1 * d1 - 1);
				double d2h = d2;
				double unit;
				double err;
				Vec me = new Vec(this.sys_coord.asVectorFromZero(true),
						sys_coord.isHyperbolic);
				me.scale(d2h);
				Vec dir = new Vec(neighbor.getLastCoordinate()
						.asVectorFromZero(true), sys_coord.isHyperbolic);
				dir.subtract(me);

				Random rand = new Random(10);
				while (dir.plane_length_hyp() < 1) {
					for (int j = 0; j < dir.getNumDimensions(); j++) {
						dir.direction[j] += (rand.nextInt() % 10 - 5) / 10.0;
					}
				}
				unit = d;

				// update error
				double sample_weight = error
						/ (neighbor.getLastError() + error);

				if (sample_weight == 0.) {
					continue;
				}
				// no decay
				// dir.scale(COORD_CONTROL*((unit*sample_weight) *grad));
				dir.scale(((unit * sample_weight) * grad));

				// unitVector.scale(COORD_CONTROL * sample_weight * sample_error
				// * decay);
				force.add(dir);

			}
		}

		if (sys_coord.USE_HEIGHT) {
			force.direction[force.direction.length - 1] = -1.
					* force.direction[force.direction.length - 1];
		}

		if (VERBOSE) {
			logger.info("t " + force.getLength(sys_coord.num_curvs) + " "
					+ force);
		}

		sys_coord.add(force);
		sys_coord.checkHeight();
		double distance_delta = force.getLength(sys_coord.num_curvs);
		// System.out.println("@@: "+distance_delta);
		running_sys_dd.add(distance_delta);

		time_of_last_sys_update = curr_time;

	}

	/**
	 * update coordinate with only one neighbor, symmetrica
	 * 
	 * @param addr_rs
	 * @param curr_time
	 * @return
	 */
	protected Vec updateCoordinateWithOneNeighbor(RemoteState<T> addr_rs,
			long curr_time) {

		// figure out the oldest sample we are going to use
		// and recalculate the nearest neighbor
		// as a side effect
		long oldestSample = curr_time;

		for (RemoteState<T> neighbor : neighbors) {

			if (oldestSample > neighbor.getLastUpdateTime()) {
				oldestSample = neighbor.getLastUpdateTime();
			}
		}

		double sampleWeightSum = 0.;
		for (RemoteState<T> neighbor : neighbors) {
			// double distance =
			// sys_coord.distanceTo(neighbor.getLastCoordinate());
			sampleWeightSum += neighbor.getLastUpdateTime() - oldestSample;

		}

		// remote neighbor
		RemoteState<T> neighbor = addr_rs;
		assert (sampleWeightSum >= 0.);

		Vec force = new Vec(sys_coord.getNumDimensions(),
				sys_coord.isHyperbolic);

		double distance = sys_coord.distanceTo(neighbor.getLastCoordinate());
		while (distance == 0.) {
			sys_coord.bump();
			distance = sys_coord.distanceTo(neighbor.getLastCoordinate());
		}
		if (sys_coord.isHyperbolic) {
			distance *= sys_coord.num_curvs;
		}

		// cannot return null b/c distance is not 0
		Vec unitVector = sys_coord.getDirection(neighbor.getLastCoordinate());
		double latency = neighbor.getSample();
		// stable process
		if (stableProcess) {
			if (this._state == this.STAB_STATE) {
				latency = neighbor.err_eliminate();
			}
		}
		double sampleWeight = 1.;
		sampleWeight = error / (neighbor.getLastError() + error);
		if (sampleWeight == 0.)
			return null;

		// error of sample
		double sampleError = distance - latency;

		double decay = 1.;
		if (sampleWeightSum > 0) {
			decay = (neighbor.getLastUpdateTime() - oldestSample)
					/ sampleWeightSum;
		}

		if (debugCrawler && debugGood) {
			int id = getIdFromAddr(neighbor.getAddress());
			crawler_log
					.info("f "
							+ id
							+ " age "
							+ Math.round((curr_time - neighbor
									.getLastUpdateTime()) / 1000.) + " er "
							+ sampleError + " sw " + sampleWeight + " comb "
							+ (sampleError * sampleWeight));
		}

		unitVector.scale(COORD_CONTROL * sampleError * sampleWeight * decay);
		force.add(unitVector);
		//
		if (sys_coord.USE_HEIGHT) {
			force.direction[force.direction.length - 1] = -1.
					* force.direction[force.direction.length - 1];
		}

		if (debugCrawler && debugGood) {
			crawler_log.info("t " + force.getLength(sys_coord.num_curvs) + " "
					+ force);
		}

		// include "gravity" to keep coordinates centered on the origin
		if (GRAVITY_DIAMETER > 0) {
			Vec gravity = sys_coord.asVectorFromZero(true);
			if (gravity.getLength(sys_coord.num_curvs) > 0) {
				// scale gravity s.t. it increases polynomially with distance
				double force_of_gravity = Math.pow(gravity
						.getLength(sys_coord.num_curvs)
						/ GRAVITY_DIAMETER, 2.);
				gravity.makeUnit(sys_coord.num_curvs);
				gravity.scale(force_of_gravity); // add to total force
				force.subtract(gravity);

			}
		}

		Vec returnedVec = null;
		if (UseFitFunction) {
			// personal movement
			Coordinate tmp = sys_coord.makeCopy();
			tmp.add(force);
			// whether tmp is better than current place
			// fit to new position
			int fit = testError(sys_coord, tmp);
			if (fit == 0) {
				sys_coord.add(force);
				sys_coord.checkHeight();
				double distance_delta = force.getLength(sys_coord.num_curvs);
				// System.out.println("@@: "+distance_delta);
				running_sys_dd.add(distance_delta);
				returnedVec = new Vec(force, sys_coord.isHyperbolic);
			} else {
				// System.out.println("not fit to new position!");

				// try to update the curvature
				// vary the curvature
				if (this.UseVaryingCurvature) {
					// if(this.stabilized()){
					// stablized but high-error
					if (this._state == this.STAB_STATE && this.error > 0.5) {
						updateCurvature(curr_time);
						// }
					}
				}
			}
		} else {
			// no fit function
			sys_coord.add(force);
			sys_coord.checkHeight();
			double distance_delta = force.getLength(sys_coord.num_curvs);
			// System.out.println("@@: "+distance_delta);
			running_sys_dd.add(distance_delta);
		}
		time_of_last_sys_update = curr_time;
		return returnedVec;
	}

	// find global best
	public void localGlobalBestFromNeighbors(long curr_time) {
		boolean selectGlobalFromNeighbor = true;
		// save temporal coordinate
		Vec oldCoord = this.sys_coord.asVectorFromZero(true);
		Vec globalForce = null;
		long nanoTime = System.nanoTime();
		// ---------------------------------------------------------
		// PERSONAL coordinate
		// ---------------------------------------------------------

		Vec force_pi = updateSystemCoordinate_Vivaldi_Fit(curr_time);

		long curTime = System.nanoTime();
		double piTime = (curTime - nanoTime) / 1000000d;
		// System.out.println("$: personal coordinate overhead: " + piTime);
		// ---------------------------------------------------------
		// GROUP coordinate
		// ---------------------------------------------------------

		// find neighbor with lowest error
		double max_error = Double.MIN_VALUE;
		RemoteState<T> neighbor_best = null;
		// from neighbor
		if (selectGlobalFromNeighbor) {
			for (RemoteState<T> neighbor : neighbors) {
				if (neighbor.getLastError() < max_error) {
					max_error = neighbor.getLastError();
					neighbor_best = neighbor;
				}
			}
		} else {
			// from global list, in simulation mode
			if (NCClient.SIM) {

				// boolean can_add=true;
				Iterator<Node> ier = HVivaldiTest_sim.getInstance().nodes
						.iterator();
				int yourID = -1;
				double sample_rtt = 0.;
				long sample_age = 0;
				double r_error = 0.;
				while (ier.hasNext()) {
					yourID++;
					Node tmp = ier.next();
					sample_rtt = HVivaldiTest_sim.getInstance().rttMedian[((Integer) this.local_addr)
							.intValue()][yourID];
					sample_age = tmp.nc.getAge(curr_time);
					r_error = tmp.nc.getSystemError();
					// latency>0 and lower error
					if (tmp.nc.error < max_error && sample_rtt > 0) {
						max_error = tmp.nc.error;
						RemoteState<T> addr_rs = rs_map.get(tmp.nc.local_addr);
						if (addr_rs == null) {

							addHost((T) tmp.nc.local_addr);
							addr_rs = rs_map.get(tmp.nc.local_addr);
						}
						Coordinate r_coord = tmp.nc.sys_coord.makeCopy();

						// add sample to history, then get smoothed rtt based on
						// percentile
						addr_rs.addSample(sample_rtt, sample_age, r_coord,
								r_error, curr_time);
						addr_rs.addMeasurementErrors(sample_rtt, r_coord,
								this.sys_coord);
						neighbor_best = addr_rs;
					}
				}

				/*
				 * for (RemoteState<T> neighbor : neighbors){ //fetch neighbors'
				 * new coordinates
				 * 
				 * //simulator module Coordinate
				 * latestCoord=HVivaldiTest_sim.getInstance
				 * ().nodes.get(((Integer
				 * )neighbor.getAddress()).intValue()).nc.sys_coord;
				 * neighbor.last_coords=latestCoord.makeCopy();
				 * neighbor.last_error
				 * =HVivaldiTest_sim.getInstance().nodes.get((
				 * (Integer)neighbor.getAddress()).intValue()).nc.error; }
				 */
			}
		}

		// find the neighbor
		if (neighbor_best != null) {
			// think low error as stablized
			if (neighbor_best.getLastError() < 0.5) {
				// return new
				// Vec(neighbor_best.getLastCoordinate().asVectorFromZero(true));
				// get global best position
				globalForce = updateCoordinateWithOneNeighbor(neighbor_best,
						curr_time);

			}
		}
		long NowTime = System.nanoTime();
		double glTime = (NowTime - curTime) / 1000000d;
		// System.out.println("$: global best position overhead: " + glTime);

		// get position by combining from personal and global best position;
		// Gaussian PSO
		Vec force_total = new Vec(sys_coord.coords.length,
				sys_coord.isHyperbolic);
		// ------------------------------
		// TOTAL force
		// ------------------------------
		if (force_pi != null && globalForce != null) {
			Vec tmp = new Vec(sys_coord.coords.length, sys_coord.isHyperbolic);
			Random r = new Random(10);
			for (int i = 0; i < sys_coord.coords.length; i++) {
				tmp.direction[i] = force_pi.direction[i]
						- globalForce.direction[i];
			}

			for (int i = 0; i < sys_coord.coords.length; i++) {
				double miu = (force_pi.direction[i] + globalForce.direction[i]) / 2;
				double delta = tmp.getPlanarLength();

				force_total.direction[i] = miu + delta * r.nextGaussian();
			}
		} else if (force_pi != null) {
			force_total = force_pi;
		} else if (globalForce != null) {
			force_total = globalForce;
		}

		Coordinate tmp = oldCoord.asCoordinateFromZero(true);
		tmp.add(force_total);
		// whether tmp is better than current place
		// fit to new position
		int fit = testError(oldCoord.asCoordinateFromZero(true), tmp);
		if (fit == 0) {
			oldCoord.add(force_total);
			sys_coord.assign(oldCoord.asCoordinateFromZero(true));
			sys_coord.checkHeight();
			double distance_delta = force_total.getLength(sys_coord.num_curvs);
			// System.out.println("@@: "+distance_delta);
			running_sys_dd.add(distance_delta);
		} else {
			// System.out.println("not fit to new position!");
		}

		time_of_last_sys_update = curr_time;

		// standard PSO

	}

	/*
	 * robust statistical imrpovement 2009-9-17- canceled energy function
	 * improvement s(i,j)= (1) alphar(i,j)^2, if |r(i,j)|<tau; (2)
	 * ln(|r(i,j)|-u)-v, otherwise
	 */
	// double tau=20; //parameters for shaping width of the energy function
	// double alpha=1.0/Math.pow(tau, 2); //parameters for shaping the height of
	// the energy function
	// double alpha=1.0/Math.pow(tau, 2);
	public void updateWithGroupEnhancement(RemoteState<T> addr_rs,
			long curr_time) {

		// save temporal coordinate
		Vec oldCoord = this.sys_coord.asVectorFromZero(true);
		// ---------------------------------------------------------
		// PERSONAL coordinate
		// ---------------------------------------------------------
		Vec force_pi = updateSystemCoordinate_Vivaldi_Fit(curr_time);

		// ---------------------------------------------------------
		// GROUP coordinate
		// ---------------------------------------------------------
		Vec force = updateCoordinateWithOneNeighbor(addr_rs, curr_time);

		Vec force_total = new Vec(sys_coord.coords.length,
				sys_coord.isHyperbolic);
		// ------------------------------
		// TOTAL force
		// ------------------------------
		if (force_pi != null && force != null) {
			Vec tmp = new Vec(sys_coord.coords.length, sys_coord.isHyperbolic);
			Random r = new Random(10);
			for (int i = 0; i < sys_coord.coords.length; i++) {
				tmp.direction[i] = force_pi.direction[i] - force.direction[i];
			}

			for (int i = 0; i < sys_coord.coords.length; i++) {
				double miu = (force_pi.direction[i] + force.direction[i]) / 2;
				double delta = tmp.getPlanarLength();

				force_total.direction[i] = miu + delta * r.nextGaussian();
			}
		} else if (force_pi != null) {
			force_total = force_pi;
		} else if (force != null) {
			force_total = force;
		}
		// force_total.scale(COORD_CONTROL);

		// force_total=force;

		// ////////////////////////////////////
		//
		// ////////////////////////////////////
		if (sys_coord.USE_HEIGHT) {
			force_total.direction[force_total.direction.length - 1] = -1.
					* force_total.direction[force_total.direction.length - 1];
		}

		if (VERBOSE) {
			logger.info("t " + force_total.getLength(sys_coord.num_curvs) + " "
					+ force_total);
		}

		Coordinate tmp = oldCoord.asCoordinateFromZero(true);
		tmp.add(force_total);
		// whether tmp is better than current place
		// fit to new position
		int fit = testError(oldCoord.asCoordinateFromZero(true), tmp);
		if (fit == 0) {
			oldCoord.add(force_total);
			sys_coord.assign(oldCoord.asCoordinateFromZero(true));
			sys_coord.checkHeight();
			double distance_delta = force_total.getLength(sys_coord.num_curvs);
			// System.out.println("@@: "+distance_delta);
			running_sys_dd.add(distance_delta);
		} else {
			// System.out.println("not fit to new position!");
		}

		time_of_last_sys_update = curr_time;
	}

	/**
	 * update the curvature 9-21
	 */
	public void updateCurvature(long curr_time) {
		// figure out the oldest sample we are going to use
		// and recalculate the nearest neighbor
		// as a side effect

		// update neighbors' coordinates, and errors
		updateNeighborCoordinates();

		long oldestSample = curr_time;
		for (RemoteState<T> neighbor : neighbors) {
			if (oldestSample > neighbor.getLastUpdateTime()) {
				oldestSample = neighbor.getLastUpdateTime();
			}
		}

		// Collections.shuffle(neighbors);
		double sampleWeightSum = 0.;
		for (RemoteState<T> neighbor : neighbors) {
			// double distance =
			// sys_coord.distanceTo(neighbor.getLastCoordinate());
			sampleWeightSum += neighbor.getLastUpdateTime() - oldestSample;

		}
		// System.out.println("$: sampleWeightSum: "+sampleWeightSum);
		assert (sampleWeightSum >= 0.);

		double opt_Curvature = 0.;
		double upperValue = 0.;
		double lowerValue = 0.;
		double delta = 0.;
		int counter = 0;
		// -------------------------------------------
		for (RemoteState<T> neighbor : neighbors) {

			if (sys_coord.isHyperbolic) {
				// weighted least square
				double weight = error / (neighbor.getLastError() + error);
				if (weight == 0.)
					continue;

				double distance = sys_coord.distanceTo(neighbor
						.getLastCoordinate());
				while (distance == 0.) {
					sys_coord.bump();
					distance = sys_coord.distanceTo(neighbor
							.getLastCoordinate());
				}
				// total curvature
				double latency = neighbor.getSample();
				// stable enforced
				if (this.stableProcess) {
					if (this._state == STAB_STATE) {
						latency = neighbor.err_eliminate();
					}
				}
				// error
				double sampleWeight = 1.;
				if (sampleWeightSum > 0) {
					sampleWeight = (neighbor.getLastUpdateTime() - oldestSample)
							/ sampleWeightSum;
				}

				upperValue += sampleWeight * weight * latency * distance;
				lowerValue += sampleWeight * weight * Math.pow(distance, 2);

				// magnitude
				// opt_Curvature+=weight*(latency/distance-sys_coord.num_curvs);

				delta += sampleWeight;
				// delta+=weight;
				//	
				counter++;
			}

		}
		// least square curvature
		opt_Curvature = upperValue / lowerValue;
		// delta
		delta = delta / counter;
		delta = 1.;
		// delta=delta*this.COORD_CONTROL;
		// delta=this.COORD_CONTROL;
		double diff = opt_Curvature - sys_coord.num_curvs;
		// diff*=(delta*COORD_CONTROL);

		System.out.println("$: delta: " + delta + ", diff: " + diff);
		// history effects
		double omega = 0.729;
		double c1 = 2.041;
		// double seed=random.nextDouble();
		double seed = 1;
		// double diff=opt_Curvature/neighbors.size();
		// change the curvature
		double tmp_curv = sys_coord.num_curvs;
		double old = sys_coord.num_curvs;

		// incremental update
		tmp_curv = sys_coord.num_curvs + diff * delta;
		// tmp_curv=omega*sys_coord.num_curvs+(1-omega)*diff*seed;
		// EWMA curvature
		// tmp_curv=omega*sys_coord.num_curvs+(1-omega)*opt_Curvature;

		int fit = testError(sys_coord, tmp_curv);

		if (fit == 0) {
			sys_coord.num_curvs = tmp_curv;
			System.out.println("##################\n" + "$: old curvature "
					+ old + ", new: " + sys_coord.num_curvs + ", diff: " + diff
					+ "\n#######################");

		} else {
			return;
		}

	}

	private int testError(Coordinate sys_coord2, double tmp_curv) {
		// TODO Auto-generated method stub

		double originErr = fitness(sys_coord2);
		double refinedErr = fitness(sys_coord2, tmp_curv);

		// System.out.println("origin Error: "+originErr+", refined Error: "+refinedErr);
		// soft threshold, allow temporal movement to bad position,
		// ericfu
		boolean soft = true;
		// larger than margin
		double margin = 0.05;

		if (soft) {
			double threshold = 0.15;
			if (originErr != -1 && refinedErr != -1) {

				if (Math.abs(refinedErr - originErr) < threshold) {
					return 0;
				} else {
					return 1;
				}
			}
		} else {
			if (originErr != -1 && refinedErr != -1) {
				if ((originErr - margin) < refinedErr) {
					return 1;
				} else {
					return 0;
				}
			}
		}
		return -1;

	}

	public static void setRandomSeed(long seed) {
		random = new Random(seed);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.harvard.syrah.pyxida.nc.lib.NCClientIF#startUp(java.io.DataInputStream
	 * )
	 */
	public void startUp(DataInputStream is) throws IOException {

		// System.err.println("Attempting to read coords during startup");

		/*
		 * boolean valid = true; try { sys_coord = new Coordinate (num_dims,
		 * is); if (!sys_coord.isValid()) valid = false; } catch (IOException
		 * ex) { valid = false; }
		 * 
		 * if (!valid) { //System.err.println("Error deserializing coordinate.
		 * Starting afresh."); sys_coord = new Coordinate (num_dims); }
		 */
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.harvard.syrah.pyxida.nc.lib.NCClientIF#shutDown(java.io.DataOutputStream
	 * )
	 */
	synchronized public void shutDown(DataOutputStream os) throws IOException {
		// could also save a number of neighbors
		// but then when we come back into the system,
		// it would be tricky to know how to treat them (they'd be old
		// so they would get much weight either).

		// System.err.println("Attempting to save coords during shut down");

		// sys_coord.toSerialized(os);

		// save confidence?

		// save app_coord also?

	}

	public int detect_bad_neighbor() {
		// TODO Auto-generated method stub
		return 0;
	}

	public int is_stable() {
		// TODO Auto-generated method stub
		return _stable;
	}

	public void not_stable() {
		// TODO Auto-generated method stub
		_state = CONV_STATE;
		_conv_steps = 0;
		_stable = 0;
		_stab_err = 0;

	}

	public void print_coord() {
		// TODO Auto-generated method stub

	}

	public void set_stable() {
		// TODO Auto-generated method stub
		_stable = 1;
	}

	public int significant_coordinates_change() {
		// TODO Auto-generated method stub
		double step;

		step = this.sys_coord.distanceTo(this.prev_coord);
		if (step > C_SIG_COORD_CHANGE_THRESH_MS)
			return 1;
		else
			return 0;

	}

	public void check_state() {
		// TODO Auto-generated method stub

		double min, max, median;

		if (_avg_avgerr.size() < _window_size) {
			return;
		}

		double[] a = new double[_avg_avgerr.size()];
		for (int i = 0; i < a.length; i++) {
			a[i] = _avg_avgerr.get(i).doubleValue();
			// System.out.print(a[i]+", ");
		}
		// System.out.print("\n");
		Arrays.sort(a);
		min = a[0];
		max = a[a.length - 1];
		median = a[Math.round(a.length / 2)];
		// System.out.println("State: "+_state);
		// System.out.println("$: Check- min: "+min+", max: "+max+", median: "+median+", _min_err"+_min_err);

		if (_state == CONV_STATE) {
			_conv_steps++;

			// if A finds ̄ cannot be decreased for L rounds, it decides the
			// coordinates have converged and enters the stabilizing state to
			// stabi-
			// lize the coordinates, //optional

			if (min > _min_err && _conv_steps > 30) {
				// System.out.println("$: Stable "+min);
				_state = STAB_STATE;
				_stab_err = 0;
				_conv_steps = 0;
				return;
			}
		}

		if (_state == STAB_STATE) {
			if (_stab_err == 0) {
				if (min / max > 0.95 && min / max < 1.0) {
					_stab_err = median;
					set_stable();
				}
				return;
			}
			// Vivaldi coordinates have been stabilized.
			/*
			 * add your strategy for triggering recomputation to adapt network
			 * changes
			 */
			/*
			 * e.g. if(fabs(median-_stab_err) > 1.0) { not_stable(); return; }
			 */
			if (Math.abs(median - _stab_err) > 1.0) {
				// System.out.println("$: not Stable"+min);
				not_stable();
				return;
			}
		}

	}

	void save_prev_coord() {

		this.prev_coord = this.sys_coord.makeCopy();
	}

	public void update_neigherr() {
		// TODO Auto-generated method stub
		Iterator ier = neighbors.iterator();
		double avg = 0.0;
		count = 0;
		while (ier.hasNext()) {
			RemoteState<T> neighbor = (RemoteState<T>) ier.next();

			avg += neighbor._measured_AbsErr;
			count++;
		}
		if (count == 0) {
			// currently no neighbor
			return;
		}
		avg = avg / count;
		// System.out.println("@@: avg: "+avg+", count: "+count);

		if (_avg_avgerr.size() == _window_size) {
			_avg_avgerr.remove(0);
		}
		// System.out.println("_avg_avgerr: "+_avg_avgerr.toString());
		double new_avg = 0;
		if (_avg_avgerr.size() == 0) {
			new_avg = avg;
		} else {

			new_avg = _avg_avgerr.get(_avg_avgerr.size() - 1).doubleValue()
					* RemoteState.ALPHA + (1 - RemoteState.ALPHA) * avg;
			// System.out.println("##: New _avg_avgerr: "+new_avg);
		}
		// System.out.println("@@: New _avg_avgerr: "+new_avg+", avg: "+avg);

		_avg_avgerr.add(Double.valueOf(new_avg));

		if (new_avg < _min_err)
			_min_err = new_avg;
	}

	protected List<Double> _avg_avgerr = new ArrayList<Double>(10);
	protected int count = 0;

	/**
	 * For Vivaldi
	 * 
	 * @param curr_time
	 */
	protected void updateSystemCoordinate(long curr_time) {

		// figure out the oldest sample we are going to use
		// and recalculate the nearest neighbor
		// as a side effect
		long oldestSample = curr_time;
		double nearest_neighbor_distance = Double.MAX_VALUE;

		Collections.shuffle(neighbors);

		for (RemoteState<T> neighbor : neighbors) {
			double distance = sys_coord
					.distanceTo(neighbor.getLastCoordinate());
			if (distance < nearest_neighbor_distance) {
				nearest_neighbor_distance = distance;
				nearest_neighbor = neighbor.getLastCoordinate();
			}
			if (oldestSample > neighbor.getLastUpdateTime()) {
				oldestSample = neighbor.getLastUpdateTime();
			}
		}

		double sampleWeightSum = 0.;
		for (RemoteState<T> neighbor : neighbors) {
			// double distance =
			// sys_coord.distanceTo(neighbor.getLastCoordinate());
			sampleWeightSum += neighbor.getLastUpdateTime() - oldestSample;
		}
		assert (sampleWeightSum >= 0.);

		Vec force = new Vec(sys_coord.getNumDimensions(),
				sys_coord.isHyperbolic);

		for (RemoteState<T> neighbor : neighbors) {
			double distance = sys_coord
					.distanceTo(neighbor.getLastCoordinate());
			while (distance == 0.) {
				sys_coord.bump();
				distance = sys_coord.distanceTo(neighbor.getLastCoordinate());
			}
			// cannot return null b/c distance is not 0
			Vec unitVector = sys_coord.getDirection(neighbor
					.getLastCoordinate());
			double latency = neighbor.getSample();
			double weight = error / (neighbor.getLastError() + error);
			if (weight == 0.)
				continue;

			// error of sample
			double sampleError = distance - latency;
			double sampleWeight = 1.;
			if (sampleWeightSum > 0) {
				sampleWeight = (neighbor.getLastUpdateTime() - oldestSample)
						/ sampleWeightSum;
			}

			if (debugCrawler && debugGood) {
				int id = getIdFromAddr(neighbor.getAddress());
				crawler_log.info("f "
						+ id
						+ " age "
						+ Math
								.round((curr_time - neighbor
										.getLastUpdateTime()) / 1000.) + " er "
						+ sampleError + " sw " + sampleWeight + " comb "
						+ (sampleError * sampleWeight));
			}

			//unitVector.scale(sampleError * sampleWeight);
			unitVector.scale(sampleError * weight* sampleWeight);
			force.add(unitVector);
		}

		if (sys_coord.USE_HEIGHT) {
			force.direction[force.direction.length - 1] = -1.
					* force.direction[force.direction.length - 1];
		}
		force.scale(COORD_CONTROL);

		if (debugCrawler && debugGood) {
			crawler_log.info("t " + force.getLength(-1) + " " + force);
		}

		// include "gravity" to keep coordinates centered on the origin
		if (GRAVITY_DIAMETER > 0) {
			Vec gravity = sys_coord.asVectorFromZero(true);
			if (gravity.getLength(-1) > 0) {
				// scale gravity s.t. it increases polynomially with distance
				double force_of_gravity = Math.pow(gravity.getLength(-1)
						/ GRAVITY_DIAMETER, 2.);
				gravity.makeUnit(-1);
				gravity.scale(force_of_gravity); // add to total force
				force.subtract(gravity);

				// if (keepStatistics) { running_gravity.add(force_of_gravity);
				// }
			}
		}

		sys_coord.add(force);
		sys_coord.checkHeight();
		double distance_delta = force.getLength(-1);
		running_sys_dd.add(distance_delta);
		if (time_of_last_sys_update > 0) {
			long since_last_sys_update = curr_time - time_of_last_sys_update;
			running_sys_update_frequency.add(since_last_sys_update);
		}

		time_of_last_sys_update = curr_time;

	}
}
