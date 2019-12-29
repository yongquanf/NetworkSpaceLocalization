package edu.NUDT.pdl.Nina.Sim.SimCoord;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import edu.NUDT.pdl.Nina.Distance.NodesPairComp;
import edu.NUDT.pdl.Nina.KNN.NodesPair;
import edu.NUDT.pdl.Nina.Sim.SimCoord.HVivaldiTest_sim.Node;
import edu.NUDT.pdl.Nina.StableNC.lib.GNP;
import edu.NUDT.pdl.Nina.StableNC.lib.NCClient;
import edu.NUDT.pdl.Nina.StableNC.lib.NCClientIF;
import edu.NUDT.pdl.Nina.StableNC.lib.NCClient_StochasticGradientDescent;
import edu.NUDT.pdl.Nina.StableNC.lib.RemoteState;
import edu.NUDT.pdl.Nina.util.MathUtil;
import edu.harvard.syrah.prp.SortedList;
import gnu.getopt.Getopt;

public class HVivaldi_Test_StochasticGradientDescent {

	boolean stable = false; // stable
	boolean symmetric = false; // symmetric
	static boolean init = false; // init

	final int SymRepeats;
	// update 15, landmarks 20,
	int startFromOne = 0;
	float[][] rttMedian;
	Vector coveredLM;
	int landmarks = 25;
	BufferedReader rttReader = null;

	final int DIMENSIONS;
	public List<Node> nodes;
	final int nodeCount;

	final boolean DEBUG;
	final long FRAME_INTERVAL;
	final boolean FULL_LATENCY_MATRIX;
	final String outputPrefix;

	final static protected NumberFormat nf = NumberFormat.getInstance();
	final static protected int NFDigits = 3;

	final long ROUNDS;
	final BufferedWriter log;
	final BufferedWriter update;
	final Random random;

	final float dists[][];
	final float errors[][];

	public class Node {
		final public int id;
		final public NCClient_StochasticGradientDescent<Integer> nc;
		int coveredNodes = 0;

		public Node(int _id, NCClient_StochasticGradientDescent<Integer> _nc) {
			id = _id;
			nc = _nc;
		}
	}

	public HVivaldi_Test_StochasticGradientDescent(String[] args) throws Exception {

		long seed = new Date().getTime();
		RemoteState.MIN_SAMPLE_SIZE = 0;
		RemoteState.MAX_SAMPLE_SIZE = 1;
		NCClient.GRAVITY_DIAMETER = 0;
		NCClient.KEEP_STATS = true;

		// Note: remember that too large of an axis will cause some pairs of
		// nodes to be
		// too far apart from each other. I.e., the RTT will exceed the HUGE
		// value

		String rttFile = null;
		BufferedReader MedRTTReader = null;
		int c;
		Getopt g = new Getopt("HVivaldi_Test", args,
				"n:l:dA:bx:zO:r:I:Zs:L:S:R:");

		boolean _FULL_LATENCY_MATRIX = false;
		long _FRAME_INTERVAL = 100000;
		long _ROUNDS = 0;
		boolean _DEBUG = false;

		NCClient.DEBUG = false;
		//NCClient.USE_HEIGHT = false;

		String _outputPrefix = "cs";
		int _nodeCount = 0;
		int _DIMENSIONS = 5;

		// default
		int _landmarks = 20;
		int _symRepeats = 5;

		// Coordinate.MIN_COORD = 0.000001;

		while ((c = g.getopt()) != -1) {
			String rttFile1;
			switch (c) {
			case 'r':
				_ROUNDS = Long.parseLong(g.getOptarg());
				break;
			case 'I':
				_FRAME_INTERVAL = Long.parseLong(g.getOptarg());
				break;
			case 'O':
				_outputPrefix = g.getOptarg();
				break;
			case 'z':
				/*if (NCClient.USE_HEIGHT)
					NCClient.USE_HEIGHT = false;
				else
					NCClient.USE_HEIGHT = true;*/
				break;
			case 'x':
				_DIMENSIONS = Integer.parseInt(g.getOptarg());
				break;
			case 'n':
				_nodeCount = Integer.parseInt(g.getOptarg());
				break;
			case 's':
				seed = Long.parseLong(g.getOptarg());
				break;
			case 'S':
				_landmarks = Integer.parseInt(g.getOptarg());
				break;
			case 'R':
				_symRepeats = Integer.parseInt(g.getOptarg());
				break;
			case 'b':
				_FULL_LATENCY_MATRIX = true;
				break;
			case 'l':
				rttFile = g.getOptarg();
				try {
					rttReader = new BufferedReader(new FileReader(new File(
							rttFile)));
				} catch (FileNotFoundException ex) {
					System.err.println("Cannot find file " + rttFile);
					System.exit(-1);
				}
				break;
			case 'L': // median latency matrix MedRTTReader
				rttFile1 = g.getOptarg();
				try {
					MedRTTReader = new BufferedReader(new FileReader(new File(
							rttFile1)));
				} catch (FileNotFoundException ex) {
					System.err.println("Cannot find file " + rttFile);
					System.exit(-1);
				}
				break;
			case 'd':
				_DEBUG = true;
				NCClient.DEBUG = true;
				break;
			case 'Z': // from one
				startFromOne = 1;
				break;
			case 'A':
				double alpha = Double.parseDouble(g.getOptarg());
				NCClient.COORD_CONTROL = alpha;
				NCClient.COORD_ERROR = alpha;
				break;

			default:
				System.out.println("Bad input");
				System.exit(-1);
			}
		}

		DEBUG = _DEBUG;
		outputPrefix = _outputPrefix;
		ROUNDS = _ROUNDS;
		nodeCount = _nodeCount;
		DIMENSIONS = _DIMENSIONS;
		FULL_LATENCY_MATRIX = _FULL_LATENCY_MATRIX;
		FRAME_INTERVAL = _FRAME_INTERVAL;
		landmarks = _landmarks;
		SymRepeats = _symRepeats;

		if (nf.getMaximumFractionDigits() > NFDigits) {
			nf.setMaximumFractionDigits(NFDigits);
		}
		if (nf.getMinimumFractionDigits() > NFDigits) {
			nf.setMinimumFractionDigits(NFDigits);
		}
		random = new Random(seed);
		NCClient.setRandomSeed(seed);

		log = new BufferedWriter(
				new FileWriter(new File(outputPrefix + ".log"),true));
		update = new BufferedWriter(new FileWriter(new File(outputPrefix
				+ ".up"),true));

		log.write("# NODECOUNT " + nodeCount + "\n" + "# RTTFILE " + rttFile
				+ "\n" + "# DIMENSIONS " + DIMENSIONS + "\n"  + "\n" + "# GRAVITY "
				+ NCClient.GRAVITY_DIAMETER + "\n" + "# COORD_CONTROL "
				+ NCClient.COORD_CONTROL + "\n" + "# COORD_ERROR "
				+ NCClient.COORD_ERROR + "\n");

		// ////////////////////////////////////////////
		// Initialize nodes and matrices

		nodes = new ArrayList<Node>();
		for (int i = 0; i < nodeCount; i++) {
			NCClient_StochasticGradientDescent<Integer> nc = new NCClient_StochasticGradientDescent<Integer>(DIMENSIONS);
			nc.setLocalID(i);
			Node node = new Node(i, nc);
			nodes.add(node);
		}

		rttMedian = new float[nodeCount][nodeCount];
		dists = new float[nodeCount][nodeCount];
		errors = new float[nodeCount][nodeCount];
		for (int i = 0; i < nodeCount; i++) {
			for (int j = 0; j < nodeCount; j++) {
				rttMedian[i][j] = 0;
				dists[i][j] = 0;
				errors[i][j] = 0;
			}
		}

		// ------------------------------------------
		if (MedRTTReader == null)
			printUsage("RTT file not open");

		// Handles long list of rtts case
		String rttLine = MedRTTReader.readLine();
		// System.out.println("$: "+rttLine);

		while (rttLine != null) {

			StringTokenizer adjTokenizer = new StringTokenizer(rttLine);
			int myId = Integer.parseInt((String) (adjTokenizer.nextElement()));
			int yourId = Integer
					.parseInt((String) (adjTokenizer.nextElement()));
			float rtt = Float.parseFloat((String) (adjTokenizer.nextElement()));

			//System.out.println("Len: " + rttMedian.length);
			if (rtt > 0 && myId != yourId) {
				
				 //System.out.println("from: "+(myId-startFromOne)+", to: "+(yourId-startFromOne)+" rtt "+rtt);
				
				rttMedian[myId - startFromOne][yourId - startFromOne] = rtt;
				// assume symmetric
				// rtts[myId][yourId] = rtt;
				if (!FULL_LATENCY_MATRIX) {
					rttMedian[yourId - startFromOne][myId - startFromOne] = rtt;
				}
			}

			rttLine = MedRTTReader.readLine();
		}

		for (int i = 0; i < nodeCount; i++) {
			int count = 0;
			for (int ind = 0; ind < nodeCount; ind++) {
				if (rttMedian[i][ind] > 0) {
					count++;
				}
			}
			// ----------------------------
			nodes.get(i).coveredNodes = count;
		}
		// ------------------------------------------
	}

	/**
	 * 
	 */
	public void run() {
		double average = 0.0;
		for (int i = 0; i < nodeCount; i++) {
			for (int j = 0; j < nodeCount; j++) {
				if (i == j) {
					continue;
				}
				average += this.rttMedian[i][j];
			}
		}
		average /= (2 * nodeCount);
		// if(average>0){
		// NCClient.averageLatency=average;
		// }
		// System.out.println("$: average: "+NCClient.averageLatency);
		// ------------------------------
		boolean can_add = true;
		
		//static reference
		NCClient_StochasticGradientDescent.testStochasticGradientDescent=this;

		// NCClient.UseGroupEnhancement=false;

		try {
			String sampleLine = rttReader.readLine();
			//System.err.println(sampleLine);
			int counter = 0;

			while (sampleLine != null) {
				// reads in timestamp in ms and raw rtt
				StringTokenizer sampleTokenizer = new StringTokenizer(
						sampleLine);
				long curr_time = Long.parseLong((String) (sampleTokenizer
						.nextElement()));
				int from = Integer.parseInt((String) (sampleTokenizer
						.nextElement()));
				int to = Integer.parseInt((String) (sampleTokenizer
						.nextElement()));
				double rawRTT = Double.parseDouble((String) (sampleTokenizer
						.nextElement()));

				Node src = nodes.get(from);
				Node dst = nodes.get(to);

				//System.out.println("\n==========================\n"+curr_time+", "+from+", "+to+", "+rawRTT);
				// update RTT
				// rttMedian[from][to]=(float)rawRTT;
				// rttMedian[to][from]= rttMedian[from][to];
				// symmetric update
				int count = 0;

						src.nc.processSample(dst.id, dst.nc.sys_coord,
								dst.nc.getSystemError(), rawRTT, dst.nc
										.getAge(curr_time), curr_time, can_add);
					
				

				sampleLine = rttReader.readLine();

				//
				if (curr_time > 0
						&& Math.round(curr_time / FRAME_INTERVAL - 0.5) > counter) {
					System.err.println("$: " + curr_time);
					logGlobalStatus(curr_time);
					//logLocalStatus(curr_time);
					System.out.println("finish global!");
					logRankingError(curr_time);
					counter++;
				}
			}
		} catch (Exception ex) {
			System.err.println("Problem parsing " + ex);
			System.exit(-1);
		}
	}

	
	void runFromMatrix(){
		
		
		double average = 0.0;
		int count=0;
		for (int i = 0; i < nodeCount; i++) {
			for (int j = 0; j < nodeCount; j++) {
				
				if (i == j||this.rttMedian[i][j]<=0) {
					continue;
				}
				average += this.rttMedian[i][j];
				count++;
			}
		}
		average /= (count);
		
		
		int repeat=(int) ROUNDS;
		
		int counter=0;
		
		NCClient_StochasticGradientDescent.testStochasticGradientDescent=this;
		
		for (int i = 0; i < this.nodeCount*this.nodeCount * repeat; i++) {
			
			
			
			int srcId = random.nextInt(nodeCount);
			int dstId = random.nextInt(nodeCount);
			
			float rtt = 0;
			// use the median matrix to initialize the coordinates of landmarks
			rtt = rttMedian[srcId][dstId];
					
			if(srcId==dstId || rtt<=0){
				continue;
			}
			
		
			// System.err.println("src: "+index[srcId]+", dest: "+index[dstId]+": "+rtt);

			Node src = nodes.get(srcId);
			//src.nc.sys_coord.global_mean=average;
			
			Node dst = nodes.get(dstId);
			//dst.nc.sys_coord.global_mean=average;
			// update RTT
			// rttMedian[from][to]=(float)rawRTT;
			// rttMedian[to][from]= rttMedian[from][to];
			// symmetric update
			count = i;

					src.nc.processSample(dst.id, dst.nc.sys_coord,
							dst.nc.getSystemError(), rtt, dst.nc
									.getAge(count), count, true);
				
			//long curr_time=i;		

		}

				long curr_time=10;
				try {
					
					logGlobalStatus(curr_time);
					//logLocalStatus(curr_time);
					logRankingError(curr_time);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
			
	}
	
	
	void logGlobalStatus(long curr_time) throws Exception {
		Statistic<Float> rel_errors = new Statistic<Float>();

		for (Node src : nodes) {
			for (Node dst : nodes) {

				// ----------------------------------------
				float rtt = rttMedian[src.id][dst.id];

				if (src == dst || rtt <= 0) {
					continue;
				}
				if (src.nc.sys_coord.atOrigin() || dst.nc.sys_coord.atOrigin()) {
					continue;
				}

				
				double dist = (src.nc.getSystemCoords().distanceToMatrixNorm(dst.nc
						.getSystemCoords()));
				// scale

				float abs_error =(float) Math.abs(rtt - dist);
				//System.out.println(rtt+", "+dist);
				// float rel_error = Math.abs(rtt-dist)/Math.max(rtt, dist);
				float rel_error = (float) Math.abs(rtt - dist) / rtt;
				//dists[src.id][dst.id] = (float) dist;
				//errors[src.id][dst.id] = rel_error;

				rel_errors.add(abs_error);
			}
		}
		
		log.append(curr_time + " me50 "
				+ nf.format(rel_errors.getPercentile(.5)) + " me80 "
				+ nf.format(rel_errors.getPercentile(.8)) + " me95 "
				+ nf.format(rel_errors.getPercentile(.95)) + "\n");

		log.flush();
	}

	void logLocalStatus(long curr_time) throws Exception {
		Statistic<Double> rel_errors = new Statistic<Double>();
		Statistic<Double> distance_deltas = new Statistic<Double>();
		Statistic<Float> Movements = new Statistic<Float>();
/*
		for (Node node : nodes) {
			// log.write(node.nc.toString()+"\n");
			rel_errors.add(node.nc.getSystemError());
			distance_deltas.add(node.nc.getDistanceDelta());
		}
*/
		log.append(curr_time + " re50 "
				+ nf.format(rel_errors.getPercentile(.5)) + " re80 "
				+ nf.format(rel_errors.getPercentile(.8)) + " re95 "
				+ nf.format(rel_errors.getPercentile(.95)) + " dd50 "
				+ nf.format(distance_deltas.getPercentile(.5)) + " dd80 "
				+ nf.format(distance_deltas.getPercentile(.8)) + " dd95 "
				+ nf.format(distance_deltas.getPercentile(.95)) + "\n");
		log.flush();
	}

	
	/**
	 * 
	 * @param src
	 * @param k
	 * @param sampledRings
	 * @return k real KNN
	 */
	public Vector<NodesPair> getRealKNN(Node src, int k, Set<Integer> sampledRings,Set<Integer> forbidden){
		
		SortedList<NodesPair<Integer>> sorted=new SortedList(new NodesPairComp<Integer>());
		//=new HashSet<Integer>(2);
		
		 Iterator<Integer> ier = sampledRings.iterator();
		while(ier.hasNext()){
			Integer rem = ier.next();
			int to=rem.intValue();
			int from=src.id;
			
			if(rttMedian[from][to]<=0){
				forbidden.add(Integer.valueOf(to));
				continue;
			}
			sorted.add(new NodesPair<Integer>(from,to,rttMedian[from][to]));			
		}
		
		int realK=Math.min(k, sampledRings.size());
		realK=Math.min(realK, sorted.size());
		//System.out.println("K: "+realK);
		//====================================================
		Vector<NodesPair> tmp=new Vector<NodesPair>();
		
		Iterator<NodesPair<Integer>> ier2 = sorted.iterator();
		int counter=0;
		while(counter<realK){
			
			tmp.add(sorted.get(counter));			
			counter++;
		}
		
		sorted.clear();
		return tmp;
	}
	
	/**
	 * @param src
	 * @param k
	 * @return
	 */
	public Vector<NodesPair> getEstimatedKNN(Node src, int k,Set<Integer> sampledRings,Set<Integer> forbidden){
		
			
		SortedList<NodesPair<Integer>> sorted=new SortedList(new NodesPairComp<Integer>());
		
		 Iterator<Integer> ier = sampledRings.iterator();
		while(ier.hasNext()){
			Integer rem = ier.next();
			int to=rem.intValue();
			int from=src.id;
			
			if(forbidden.contains(Integer.valueOf(to))){
				continue;
			}
			//save the node
			Node dst=nodes.get(to);
						
			float dist = (float) (src.nc.getSystemCoords()
					.distanceToMatrixNorm(dst.nc.getSystemCoords()));

					
			sorted.add(new NodesPair<Integer>(from,to,dist));			
		}
		
		
		int realK=Math.min(k, sampledRings.size());
		realK=Math.min(realK, sorted.size());
		
		//System.out.println("K: "+realK);
		//====================================================
		Vector<NodesPair> tmp=new Vector<NodesPair>();
		
		Iterator<NodesPair<Integer>> ier2 = sorted.iterator();
		int counter=0;
		while(counter<realK){
			
			tmp.add(sorted.get(counter));			
			counter++;
		}
		
		sorted.clear();
		return tmp;
	}
	
	
	/**
	 * log the knn error
	 * @param curr_time
	 * @throws IOException 
	 */
	public void logRankingError(long curr_time) throws IOException{
		
		int[] knns={1,3,5,10,15,20};
				
		Statistic<Float> coverage = new Statistic<Float>();
		
		int sampledRingNum=30;
		Random rand=new Random(System.currentTimeMillis());
		

		
		for(int ind=0;ind<knns.length;ind++){
		
			
		//=======================================
		int kk=knns[ind];	
		
		for (Node src : nodes) {
		
			//the subset of nodes
			Set<Integer> sampledRings=new HashSet<Integer>();
			int counter=0;
			while(counter<sampledRingNum){
				int id=rand.nextInt(nodes.size());
				if(!sampledRings.contains(id)){
					sampledRings.add(id);
				}
				counter++;
			}
			
		
			Set<Integer> forbidden=new HashSet<Integer>(2);
			
			//real knns, also find the forbidden nodes
			Vector<NodesPair> realKNN= getRealKNN(src,kk,sampledRings,forbidden);
			
						
			//estimated knns
			Vector<NodesPair> estimatedKNN= getEstimatedKNN(src,kk,sampledRings,forbidden);
			
			forbidden.clear();
			double slack=0;
			
		    double covered=coverage(src.id,estimatedKNN,realKNN,slack);
		    coverage.add((float)covered);
		}
		
/*		log.write(curr_time +"k "+kk+" covered10 "
				+ nf.format( coverage.getPercentile(.1))+ " covered50 "
				+ nf.format( coverage.getPercentile(.5)) + " covered80 "
				+ nf.format( coverage.getPercentile(.8)) + " covered95 "
				+ nf.format( coverage.getPercentile(.95))  + "\n");*/
		
		String str=nf.format( coverage.getPercentile(.1))+"\t"
		+nf.format( coverage.getPercentile(.2))+"\t"
		+nf.format( coverage.getPercentile(.3))+"\t"
		+nf.format( coverage.getPercentile(.4))+"\t"
		+nf.format( coverage.getPercentile(.5))+"\t"
		+nf.format( coverage.getPercentile(.6))+"\t"
		+nf.format( coverage.getPercentile(.7))+"\t"
		+nf.format( coverage.getPercentile(.8))+"\t"
		+nf.format( coverage.getPercentile(.9))+"\t"
		+nf.format( coverage.getPercentile(.95))+"\n"
		;
		
		update.append(curr_time +"k "+kk+" "+str);
		
		update.flush();
		}
		
		coverage.clear();
	}
	
	/**
	 * compute the coverage of coordinates
	 * @param target
	 * @param EstRecords
	 * @param RealRecords
	 * @param slack
	 * @return
	 */
	public double coverage(int target, Vector<NodesPair> EstRecords,
			Vector<NodesPair> RealRecords, double slack) {
		// TODO Auto-generated method stub
		
		int len1=EstRecords.size();
		int len2=RealRecords.size();
		int len=Math.min(len1, len2);
		int total=0;
	
		Vector<Integer> vecE=new Vector<Integer>(2);
		Vector<Integer> vecR=new Vector<Integer>(2);
		
		for(int i=0;i<len;i++){
		vecE.add((Integer)EstRecords.get(i).endNode);	
		vecR.add((Integer)RealRecords.get(i).endNode);
		}
				
			for(int i=0;i<len;i++){
				if(vecR.contains(vecE.get(i))){
					total++;
				}
			}
			
		return (float)(0.0+total)/len;
		
	}
	/**
	 * run according to the event sequence
	 */
	public void initCoord() {

		int tau = 100;
		final long sample_age = 0;
		final boolean can_add = true;
		// -------------------------
		MathUtil math = new MathUtil(10);
		int iers = 0, i; // pointers
		// -----------------------------
		// init
		// no PSO
		// NCClient.UseGroupEnhancement=false;

		Hashtable<Integer, Integer> test = new Hashtable<Integer, Integer>();
		Vector<Integer> nonLandmarks = new Vector<Integer>();
		for (i = 0; i < nodeCount; i++) {
			nonLandmarks.add(Integer.valueOf(i));
		}
		int[] index = new int[landmarks];

		boolean maxCover = true;
		if (maxCover) {
			// (0) select nodes has largest cover
			int[] covers = new int[nodeCount];
			int[] tmp = new int[nodeCount];
			for (i = 0; i < nodeCount; i++) {
				covers[i] = nodes.get(i).coveredNodes;
				tmp[i] = nodes.get(i).coveredNodes;

			}
			Arrays.sort(covers);
			for (i = 0; i < landmarks; i++) {
				// select landmarks
				int max = covers[nodeCount - 1 - i];

				int ind = 0;
				while (tmp[ind] != max) {
					ind++;
				}
				index[i] = ind;
				tmp[ind] = -1;
				System.err.println("Landmark cover: " + max + "@: " + ind);
				nonLandmarks.remove(Integer.valueOf(ind));
			}
		} else {
			// (1) random select landmarks
			while (true) {
				if (iers == landmarks) {
					break;
				}
				i = (int) math.uniform(0, nodeCount - 1);
				if (test.containsKey(i)) {
					continue;
				}

				index[iers] = i; // Note: maybe repeated, but for large matrix
				// ignored is resonable
				nonLandmarks.remove(Integer.valueOf(i));
				test.put(i, iers);
				iers++;
			}
		}
		// (2) init landmarks
		// construct landmarks latency

		// each landmark update its coordinate with other landmarks
		// in tau rounds
		for (i = 0; i < landmarks * landmarks * tau; i++) {
			int srcId = random.nextInt(landmarks);
			int dstId = random.nextInt(landmarks);

			float rtt = 0;
			// use the median matrix to initialize the coordinates of landmarks
			rtt = rttMedian[index[srcId]][index[dstId]];

			while (srcId == dstId || rtt <= 0) {

				dstId = random.nextInt(landmarks);

				rtt = rttMedian[index[srcId]][index[dstId]];
				if (rtt <= 0)
					dstId = srcId;
			}
			// System.err.println("src: "+index[srcId]+", dest: "+index[dstId]+": "+rtt);

			Node src = nodes.get(index[srcId]);
			Node dst = nodes.get(index[dstId]);

	/*		src.nc.simpleCoordinateUpdate(dst.id, dst.nc.getSystemCoords(),
					dst.nc.getSystemError(), rtt, 2);*/

			// src.nc.processSample
			// (dst.id, dst.nc.getSystemCoords(), dst.nc.getSystemError(),
			// rtt, sample_age, 2, can_add);

		}
		// init nonlinear opt
		/*
		 * gnp.dim=DIMENSIONS; gnp.mp=landmarks; gnp.CLandmarks.clear(); //same
		 * index for(i=0;i<landmarks;i++){
		 * gnp.CLandmarks.add(nodes.get(index[i]).nc.sys_coord); }
		 */
		System.out.println("Complete Landmark inilization!");
		// (3) non-landmarks

		// each non-landmark initialize its coordinate with landmarks
		// int tau rounds

		// with PSO
		// NCClient.UseGroupEnhancement=false;

		boolean opByNonlinear = false;
		Iterator<Integer> ier_nonLandmarks = nonLandmarks.iterator();
		int src_index;
		while (ier_nonLandmarks.hasNext()) {
			src_index = ier_nonLandmarks.next().intValue();

			// if landmark skip

			Node src = nodes.get(src_index);

			if (opByNonlinear) {

				// nonlinear

			} else {
				// iterative
				for (i = 0; i < landmarks * tau; i++) {
					int dstId = random.nextInt(landmarks);
					Node dst = nodes.get(index[dstId]);

					float rtt = rttMedian[src_index][index[dstId]];
					if (rtt <= 0) {
						System.out.println(" RTT ZERO!" + "[" + src_index
								+ ", " + index[dstId] + "]");
						continue;
					}
					// src.nc.simpleCoordinateUpdate
					// (dst.id, dst.nc.getSystemCoords(),
					// dst.nc.getSystemError(),
					// rtt, 2);
				/*	src.nc.processSample_noStable(dst.id, dst.nc
							.getSystemCoords(), dst.nc.getSystemError(), rtt,
							sample_age, 2, can_add);*/

				}
			}

		}

		System.out.println("Complete NON-Landmark inilization!");

		try {
			logGlobalStatus(3);
			logLocalStatus(3);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// ----------------------------------
	}

	void finish() throws Exception {
		log.flush();
		log.close();
		update.flush();
		update.close();
	}

	static GNP gnp = null;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("Testing based on traces");
		// String sampleFile = args[0];
		// RemoteState<String> rs = new RemoteState<String>(sampleFile);

		HVivaldi_Test_StochasticGradientDescent test;
		try {
			
			test = new HVivaldi_Test_StochasticGradientDescent(args);
			if (init) {
				test.initCoord();
			}

			//test.run();
			test.runFromMatrix();
			
			test.finish();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void printUsage(String problem) {
		System.err.println(problem);
		System.err.println("ConvergeSim Usage\n" + " -r rounds\n"
				+ " -I log output interval\n" + " -O output prefix\n"
				+ " -z use height\n" + " -L median latency matrix\n"
				+ " -Z startFromOne\n" + " -x dimensions\n"
				+ " -n node count\n" + " -b latency matrix is not symmetric\n"
				+ " -l latency matrix file\n" + " -d debug\n" + " -s seed\n"
				+ " -A Vivaldi EWMA parameters\n");
		System.exit(-1);
	}
}
