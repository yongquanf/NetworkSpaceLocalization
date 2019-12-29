package edu.NUDT.pdl.Nina.StableNC.lib;

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

import edu.NUDT.pdl.Nina.Sim.SimCoord.HVivaldi_Test_StochasticGradientDescent;
import edu.harvard.syrah.prp.Log;

public class NCClient_StochasticGradientDescent<T> implements NCClientIF<T>{

	
	static Log logger=new Log(NCClient_StochasticGradientDescent.class);
	
	//for stochastic gradient descent
	
	public static float alpha=0.5f;
	
	public static float lamda=0.2f;
	
	
	

	// include even good events in output, not just problems
	public static boolean debugGood = false;
	public static final boolean SIMULATION = true;
	public static boolean debug = false;

	public static boolean KEEP_STATS = true;
	public static boolean DEBUG = false;
	public static boolean VERBOSE = false;
	
	
	//is simulator
	public static boolean SIM = true;

	public static HVivaldi_Test_StochasticGradientDescent testStochasticGradientDescent=null;
	
	
	public static final byte VERSION_04 = 0x04;
	public static final byte CURRENT_VERSION = VERSION_04;

	public static double COORD_ERROR = 0.20; // c_e parameter
	public static double COORD_CONTROL = 0.2; // c_c parameter

	// The last element of the coordinate is a "height" away from the Euclidean
	// space
	public static boolean USE_HEIGHT = false;

	
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
	// public static double GRAVITY_DIAMETER = 1024.;
	public static double GRAVITY_DIAMETER = 0; // no gravity
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

	// Note: for larger installations, use e.g. 512
	// Try to minimize our error between up to MAX_NEIGHBORS guys at once
	public static int MAX_NEIGHBORS = 32;

	
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

	protected int updateCounter=1;
	
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

	//use stable process or not
	public boolean stableProcess = true; // stable or not

	protected List<Double> _avg_avgerr = new ArrayList<Double>(10);
	protected int count = 0;


	private Map<T, Integer> addr2id = new HashMap<T, Integer>();
	private int idCounter = 0;

	
	public NCClient_StochasticGradientDescent(int _num_dims){
		num_dims = _num_dims;

		sys_coord = new Coordinate(num_dims);

		//init position
		
		sys_coord.bump();

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
	
	
	@Override
	public boolean addHost(T addr) {
		// TODO Auto-generated method stub
		if (rs_map.containsKey(addr)) {
			return false;
		}

		RemoteState<T> rs = new RemoteState<T>(addr);
		rs_map.put(addr, rs);
		return true;
	}

	@Override
	public boolean addHost(T addr, Coordinate _r_coord, double r_error,
			long curr_time, boolean can_update) {
		// TODO Auto-generated method stub
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

	@Override
	public void check_state() {
		// TODO Auto-generated method stub

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

	@Override
	public boolean containsHost(T addr) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int detect_bad_neighbor() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	synchronized public long getAge(long curr_time) {
		if (curr_time < time_of_last_sys_update)
			return 0;
		return curr_time - time_of_last_sys_update;
	}

	@Override
	public Set<T> getHosts() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public T getNeighborToPing(long curr_time) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getNumDimensions() {
		// TODO Auto-generated method stub
		return this.sys_coord.num_dims;
	}

	@Override
	public ObserverList getObserverList() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Hashtable<String, Double> getStatistics() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Coordinate getSystemCoords() {
		// TODO Auto-generated method stub
		return this.sys_coord;
	}

	@Override
	public double getSystemError() {
		// TODO Auto-generated method stub
		return error;
	}

	@Override
	public boolean hasAllNeighbors() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int is_stable() {
		// TODO Auto-generated method stub
		return _stable;
	}

	@Override
	public void not_stable() {
		// TODO Auto-generated method stub
		_state = CONV_STATE;
		_conv_steps = 0;
		_stable = 0;
		_stab_err = 0;
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
	
	@Override
	public String printNeighbors() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void print_coord() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean processSample(T addr, Coordinate __r_coord, double r_error,
			double sample_rtt, long sample_age, long curr_time, boolean can_add) {
		
		//System.out.println("time: "+curr_time);
		
		
		int id = getIdFromAddr(addr);
		if (VERBOSE)
			logger.info(id + " START");

		assert (__r_coord != sys_coord);
		assert (__r_coord != null);
		assert (sys_coord != null);

		if (!sys_coord.isCompatible(__r_coord)) {
			if (DEBUG)
				logger.info("INVALID " + id + " s " + sample_rtt
						+ " NOT_COMPAT " + __r_coord.getVersion());
			return false;
		}

		// There is a major problem with the coord.
		// However, if this is happening, it will probably
		// happen again and again.
		// Note that error is checked and fixed in updateError()
		if (!sys_coord.isValid() ) {
			System.err.println("Warning: resetting Vivaldi coordinate");
			if (DEBUG)
				logger.info(id + " RESET, USE_HEIGHT=" + USE_HEIGHT);
			
			//System.exit(-1);
			reset();
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
		
		
		Coordinate r_coord = __r_coord.makeCopy();

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

		//===============================
			addNeighbor(addr_rs);
		
		
		// first update our error
		updateError(addr, r_coord, r_error, smoothed_rtt, sample_rtt,
				sample_age, sample_size, curr_time);
		
		
		Coordinate newRemote=updateSystemCoordinate(r_error, r_coord,smoothed_rtt,curr_time);
		//Coordinate newRemote=incrementalUpdate(r_error, r_coord,smoothed_rtt,curr_time);
		
		
		
        sendNewCoordinateToRemote(addr,newRemote); 
		
        
        updateStepParam();
        
		//incrementUpdateWithNeighbors(curr_time);
        
         System.out.println("\nTime: "+curr_time+", error: "+error);
		
         updateCounter++;
		// TODO Auto-generated method stub
		return true;
	}

	/**
	 * update the step size
	 */
	private void updateStepParam() {
		// TODO Auto-generated method stub
		lamda/=Math.sqrt(updateCounter);
		alpha/=Math.sqrt(updateCounter);
	}


	/**
	 * update the coordinate
	 * @param newRemote
	 */
	public void setCoordinate(Coordinate newRemote){
		this.sys_coord.clear();
		this.sys_coord=newRemote.makeCopy();
		newRemote.clear();
	}
	
	
	private void sendNewCoordinateToRemote(T addr, Coordinate newRemote) {
		// TODO Auto-generated method stub
	if(SIM){
		
		testStochasticGradientDescent.nodes.get((Integer)addr).nc.setCoordinate(newRemote);
		
	}else{
		
		logger.warn("need to define the coordinate piggyback module!");
	}
	}


	/**
	 * regularized , with one node, 
	 * @param r_error
	 * @param _r_coord
	 * @param smoothed_rtt
	 * @param curr_time
	 * @return
	 */
	private Coordinate updateSystemCoordinate(double r_error, Coordinate _r_coord, double smoothed_rtt, long curr_time) {
		// TODO Auto-generated method stub
		//in both directions
		
		Coordinate r_coord=_r_coord.makeCopy();
		//Coordinate my_coord=this.sys_coord.makeCopy();
		
		Vec remote_gradient = r_coord.asVectorFromZero(true);
		
 		
		double product1=sys_coord.innerProduct(r_coord);
		
		//update my coordinate
		Vec my_gradient = sys_coord.asVectorFromZero(true);
		//sign vector
		//my_gradient=my_gradient.sign();
		
		my_gradient.scale(-lamda);
		
		 remote_gradient.scale(smoothed_rtt-product1-(this.sys_coord.global_mean+r_coord.global_mean)/2-this.sys_coord.bias-r_coord.bias);			
		 remote_gradient.scale(alpha);	 
		 
		 remote_gradient.add(my_gradient);

		 
/*		 double sample_weight = 0; 
		 
	       if(r_error + error!=0){
	    	    sample_weight = error / (r_error + error); 
	         }
		 
		 
		 if(Double.isNaN(sample_weight)||Double.isInfinite(sample_weight)||sample_weight ==0){
			logger.warn("skip weight"); 
		 }else{			 
			 remote_gradient.scale(sample_weight);  
		 }*/
   
         //===================================
         
		 //updated gradient
	 	Vec my_gradient_r = sys_coord.asVectorFromZero(true);
	 	
         //update remote
         Vec remote_gradient_r = r_coord.asVectorFromZero(true);
         //remote_gradient_r=remote_gradient_r.sign();
        
         remote_gradient_r.scale(-lamda);
         

 		double product2=sys_coord.innerProduct(r_coord);
 		
 		
         my_gradient_r.scale(smoothed_rtt-product2 -(this.sys_coord.global_mean+r_coord.global_mean)/2-this.sys_coord.bias-r_coord.bias);         
         my_gradient_r.scale(alpha);
         
         
         my_gradient_r.add(remote_gradient_r);

         
    /*     double sample_weight_r =0;         
         if(r_error + error!=0){
        	 sample_weight_r = r_error / (r_error + error); 
         }
        
         
         if(Double.isNaN(sample_weight_r)||Double.isInfinite(sample_weight_r)||sample_weight_r==0){
 			logger.warn("skip weight"); 
 		 }else{			 
 			   my_gradient_r.scale(sample_weight_r);  
 		 }*/

         //update the bias
         sys_coord.bias=this.sys_coord.bias+alpha*(smoothed_rtt-sys_coord.innerProduct(r_coord) -(this.sys_coord.global_mean+r_coord.global_mean)/2-this.sys_coord.bias-r_coord.bias)-lamda*this.sys_coord.bias;
         
         r_coord.bias= r_coord.bias+alpha*(smoothed_rtt-sys_coord.innerProduct(r_coord) -(this.sys_coord.global_mean+r_coord.global_mean)/2-this.sys_coord.bias-r_coord.bias)-lamda* r_coord.bias;
         
         
         
		 sys_coord.add(remote_gradient);
         r_coord.add(my_gradient_r);
         
        
         
         time_of_last_sys_update=curr_time;
         
         return r_coord;
	}

	
	/**
	 *  incremental, with one node
	 * @param r_error
	 * @param _r_coord
	 * @param smoothed_rtt
	 * @param curr_time
	 * @return
	 */
	public Coordinate incrementalUpdate(double r_error, Coordinate _r_coord, double smoothed_rtt, long curr_time) {
		// TODO Auto-generated method stub
		//in both directions
		Coordinate r_coord=_r_coord.makeCopy();
		
		// whether old records are better?
		//	copy old record
		//Coordinate sysCopy=this.sys_coord.makeCopy();	
			
		
		double sample_weight =1;         
		  if(r_error + error!=0){
		        	 sample_weight = error / (r_error + error); 
		  }
		    
			//belief in me
		  double sample_weight_r =1;         
	         if(r_error + error!=0){
	        	 sample_weight_r = r_error / (r_error + error); 
	         }
		
		double R= smoothed_rtt;
		
		
		for(int i=0;i<this.num_dims;i++){
		
		//for each dimension, we update the component						   
		this.sys_coord.coords[i]=this.sys_coord.coords[i] +	sample_weight*(alpha*(R-this.sys_coord.coords[i]*r_coord.coords[i])*r_coord.coords[i]-lamda*this.sys_coord.coords[i]);
			        	         	         
		r_coord.coords[i]=	r_coord.coords[i] + sample_weight_r*(alpha*(R-this.sys_coord.coords[i]*r_coord.coords[i])*this.sys_coord.coords[i] - lamda *	r_coord.coords[i]);
		R=R-this.sys_coord.coords[i]*r_coord.coords[i];		
		}

        time_of_last_sys_update=curr_time;
        
        return r_coord;	
	}
	
	
	/**
	 * increment with current neighbors
	 * @param curr_time
	 */
	public void incrementUpdateWithNeighbors(long curr_time){
		
		//logger.info("neighbor: "+neighbors.size());
		
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
		
		
		//total vector
		Vec total=new Vec(this.num_dims,false);
		
		for(RemoteState<T> neighbor : neighbors){
			
			double r_error=neighbor.getLastError();
			Coordinate r_coord=neighbor.last_coords;
			double smoothed_rtt=neighbor.getSample();
					
			double sampleWeightDecay = 1.;
			if (sampleWeightSum > 0) {
				sampleWeightDecay = (neighbor.getLastUpdateTime() - oldestSample)
						/ sampleWeightSum;
			}
			
			Vec remote_gradient = r_coord.asVectorFromZero(true);
			

	 		
			double product1=sys_coord.innerProduct(r_coord);
			

			
			 remote_gradient.scale(smoothed_rtt-product1);
				 
			remote_gradient.scale(sampleWeightDecay);  
			 			 
			 //add the gradient
			total.add(remote_gradient);
		}
		
		total.scale(1.0/neighbors.size());
		
		total.scale(alpha);
		
		//update my coordinate
		Vec my_gradient = sys_coord.asVectorFromZero(true);
		//sign vector
		//my_gradient=my_gradient.sign();
		
		my_gradient.scale(-lamda);
		 
		total.add(my_gradient);
		 
		 
		 sys_coord.add(total);
		
		
        time_of_last_sys_update=curr_time;
		
	}
	
	

	protected void updateError(T addr, Coordinate r_coord, double r_error,
			double smoothed_rtt, double sample_rtt, long sample_age,
			int sample_size, long curr_time) {
		// get the coordinate distance
		
		double sys_distance = sys_coord.distanceToMatrixNorm(r_coord);

		double str = -1;
		double actual_str = -1;
		double expect = -1;
		// scale

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
	
			sys_sample_error = Math.abs(sys_distance - smoothed_rtt)
					/ smoothed_rtt;
		
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
	
	
	@Override
	public boolean removeHost(T addr) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		sys_coord.reset();
		error = MAX_ERROR;
		rs_map.clear();
	}

	@Override
	public void setLocalID(T _local_addr) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void set_stable() {
		// TODO Auto-generated method stub
		_stable = 1;
	}

	@Override
	public void shutDown(DataOutputStream os) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int significant_coordinates_change() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean stabilized() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void startUp(DataInputStream is) throws IOException {
		// TODO Auto-generated method stub
		
	}

	void save_prev_coord(){
		this.prev_coord = this.sys_coord.makeCopy();
	}
	
	@Override
	public void update_neigherr() {
		// TODO Auto-generated method stub
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

}
