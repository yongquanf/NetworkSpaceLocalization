/*
 * Pyxida - a network coordinate library
 * 
 * Copyright 2008 Jonathan Ledlie and Peter Pietzuch
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
package edu.NUDT.pdl.Nina.Sim.SimCoord;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Vector;

import edu.NUDT.pdl.Nina.StableNC.lib.NCClient;
import edu.NUDT.pdl.Nina.StableNC.lib.RemoteState;
import edu.NUDT.pdl.Nina.util.MathUtil;
import gnu.getopt.Getopt;

/**
 * @author ledlie
 * 
 */
public class Converge_Nina_Sim {

	int startFromOne = 0;
	int landmarks = 25;
	float rttMedian[][];
	// --------------------
	final int DIMENSIONS;
	final List<Node> nodes;
	final int nodeCount;

	final boolean DEBUG;
	final long FRAME_INTERVAL;
	final boolean FULL_LATENCY_MATRIX;
	final String outputPrefix;

	final float rtts[][];
	final float dists[][];
	final float errors[][];

	final static protected NumberFormat nf = NumberFormat.getInstance();
	final static protected int NFDigits = 3;

	final long ROUNDS;
	final BufferedWriter log;
	final BufferedWriter update;
	final Random random;

	class Node {
		final public int id;
		final public NCClient<Integer> nc;

		public Node(int _id, NCClient<Integer> _nc) {
			id = _id;
			nc = _nc;
		}
	}

	/**
	 * 
	 * @throws Exception
	 */
	public void run() throws Exception {

		int tau = 200;
		final long sample_age = 0;
		rttMedian = rtts;
		// -------------------------
		MathUtil math = new MathUtil(10);
		int iers = 0, i; // pointers
		// -----------------------------
		// init
		// (1) random select landmarks
		Hashtable<Integer, Integer> test = new Hashtable<Integer, Integer>();
		Vector<Integer> nonLandmarks = new Vector<Integer>();
		for (i = 0; i < nodeCount; i++) {
			nonLandmarks.add(Integer.valueOf(i));
		}

		int[] index = new int[landmarks];
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

		// (2) init landmarks
		// construct landmarks latency

		// each landmark update its coordinate with other landmarks
		// in tau rounds
		for (i = 0; i < landmarks * tau; i++) {
			int srcId = random.nextInt(landmarks);
			int dstId = random.nextInt(landmarks);

			float rtt = 0;
			// use the median matrix to initialize the coordinates of landmarks
			rtt = rttMedian[index[srcId]][index[dstId]];
			while (srcId == dstId || rtt <= 0) {

				dstId = random.nextInt(landmarks);
				System.err.println("src: " + index[srcId] + ", dest: "
						+ index[dstId]);
				rtt = rttMedian[index[srcId]][index[dstId]];
				if (rtt <= 0)
					dstId = srcId;
			}
			Node src = nodes.get(index[srcId]);
			Node dst = nodes.get(index[dstId]);
			final boolean can_add = true;

			src.nc.processSample_noStable(dst.id, dst.nc.getSystemCoords(),
					dst.nc.getSystemError(), rtt, sample_age, 2, can_add);

		}

		System.out.println("Complete Landmark inilization!");
		// (3) non-landmarks

		// each non-landmark initialize its coordinate with landmarks
		// int tau rounds
		Iterator<Integer> ier_nonLandmarks = nonLandmarks.iterator();
		int src_index;
		while (ier_nonLandmarks.hasNext()) {
			src_index = ier_nonLandmarks.next().intValue();
			Node src = nodes.get(src_index);

			for (i = 0; i < landmarks * tau; i++) {
				int dstId = random.nextInt(landmarks);
				Node dst = nodes.get(index[dstId]);
				final boolean can_add = true;

				float rtt = rttMedian[src_index][index[dstId]];
				src.nc.processSample_noStable(dst.id, dst.nc.getSystemCoords(),
						dst.nc.getSystemError(), rtt, sample_age, 2, can_add);

			}

		}

		System.out.println("Complete NON-Landmark inilization!");
		// ---------------------------------------------------------

		// ------------------------------
		for (long round = 0; round < ROUNDS; round++) {
			// long round = 0;
			// while (true) {
			// round++;

			// TODO This needs to restrict the number of neighbors!

			int srcId = random.nextInt(nodes.size());
			int dstId = random.nextInt(nodes.size());
			float rtt = 0;
			rtt = rtts[srcId][dstId];
			// int counter=0;
			// int maxZero=(int)Math.round(0.6*nodeCount);
			while (srcId == dstId || rtt <= 0) {
				/*
				 * counter++; if(counter>=maxZero){ //change src counter=0;
				 * srcId = random.nextInt(nodes.size()); }
				 */
				dstId = random.nextInt(nodes.size());
				rtt = rtts[srcId][dstId];
				if (rtt <= 0)
					dstId = srcId;
			}

			Node src = nodes.get(srcId);
			Node dst = nodes.get(dstId);

			final boolean can_add = true;

			// System.out.println("From: "+srcId+", Dest: "+dstId+" RTT: "+rtt);

			/*
			 * if (DEBUG) { float dist =
			 * (float)(src.nc.getSystemCoords().distanceTo
			 * (dst.nc.getSystemCoords())); float abs_error =
			 * Math.abs(rtt-dist); float rel_error = Math.abs(rtt-dist)/rtt;
			 * 
			 * String up = round+" src "+srcId+" dst "+dstId+" rtt "+rtt+
			 * " dist "+dist+" rE "+rel_error+
			 * " sC "+src.nc.getSystemCoords()+" sE "
			 * +nf.format(src.nc.getSystemError())+
			 * " dC "+dst.nc.getSystemCoords
			 * ()+" dE "+nf.format(dst.nc.getSystemError()); update.write
			 * (up+"\n"); //System.out.println (up); }
			 */
			// src.nc.simpleCoordinateUpdate(dst.id, dst.nc.getSystemCoords(),
			// dst.nc.getSystemError(),
			// rtt, round);
			src.nc.processSample_noStable(dst.id, dst.nc.getSystemCoords(),
					dst.nc.getSystemError(), rtt, sample_age, 2, can_add);

			if (round > 0 && round % FRAME_INTERVAL == 0) {
				System.err.println("$: " + round);
				logGlobalStatus(round);
			}
		}

		// TODO move to caught output

	}

	void logGlobalStatus(long round) throws Exception {
		Statistic<Float> rel_errors = new Statistic<Float>();
		for (Node src : nodes) {
			for (Node dst : nodes) {
				float rtt = rtts[src.id][dst.id];
				if (src == dst || rtt <= 0)
					continue;

				float dist = (float) (src.nc.getSystemCoords()
						.distanceTo(dst.nc.getSystemCoords()));
				float abs_error = Math.abs(rtt - dist);
				// float rel_error = Math.abs(rtt-dist)/Math.max(rtt, dist);
				float rel_error = Math.abs(rtt - dist) / rtt;
				dists[src.id][dst.id] = dist;
				errors[src.id][dst.id] = rel_error;
				rel_errors.add(rel_error);
			}
		}
		log.write(round + " me50 " + nf.format(rel_errors.getPercentile(.5))
				+ " me80 " + nf.format(rel_errors.getPercentile(.8)) + " me95 "
				+ nf.format(rel_errors.getPercentile(.95)) + "\n");
		log.flush();
	}

	void logLocalStatus(long round) throws Exception {
		Statistic<Double> rel_errors = new Statistic<Double>();
		Statistic<Double> distance_deltas = new Statistic<Double>();
		for (Node node : nodes) {
			rel_errors.add(node.nc.getSystemError());
			distance_deltas.add(node.nc.getDistanceDelta());
		}
		log.write(round + " re50 " + nf.format(rel_errors.getPercentile(.5))
				+ " re80 " + nf.format(rel_errors.getPercentile(.8)) + " re95 "
				+ nf.format(rel_errors.getPercentile(.95)) + " dd50 "
				+ nf.format(distance_deltas.getPercentile(.5)) + " dd80 "
				+ nf.format(distance_deltas.getPercentile(.8)) + " dd95 "
				+ nf.format(distance_deltas.getPercentile(.95)) + "\n");
		log.flush();
	}

	public Converge_Nina_Sim(String[] args) throws Exception {
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
		BufferedReader rttReader = null;
		int c;
		Getopt g = new Getopt("ConvergeSim", args, "n:l:dA:bx:zO:r:ZI:s:");

		boolean _FULL_LATENCY_MATRIX = false;
		long _FRAME_INTERVAL = 100000;
		long _ROUNDS = 0;
		boolean _DEBUG = false;

		NCClient.DEBUG = true;
		//NCClient.USE_HEIGHT = true;
		String _outputPrefix = "cs";
		int _nodeCount = 0;
		int _DIMENSIONS = 3;

		// Coordinate.MIN_COORD = 0.000001;

		while ((c = g.getopt()) != -1) {
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

		if (nf.getMaximumFractionDigits() > NFDigits) {
			nf.setMaximumFractionDigits(NFDigits);
		}
		if (nf.getMinimumFractionDigits() > NFDigits) {
			nf.setMinimumFractionDigits(NFDigits);
		}
		random = new Random(seed);
		NCClient.setRandomSeed(seed);

		log = new BufferedWriter(
				new FileWriter(new File(outputPrefix + ".log")));
		update = new BufferedWriter(new FileWriter(new File(outputPrefix
				+ ".up")));

		log.write("# NODECOUNT " + nodeCount + "\n" + "# RTTFILE " + rttFile
				+ "\n" + "# DIMENSIONS " + DIMENSIONS + "\n" + "\n" + "# GRAVITY "
				+ NCClient.GRAVITY_DIAMETER + "\n" + "# COORD_CONTROL "
				+ NCClient.COORD_CONTROL + "\n" + "# COORD_ERROR "
				+ NCClient.COORD_ERROR + "\n");

		// ////////////////////////////////////////////
		// Initialize nodes and matrices

		nodes = new ArrayList<Node>();
		for (int i = 0; i < nodeCount; i++) {
			NCClient<Integer> nc = new NCClient<Integer>(DIMENSIONS);
			nc.setLocalID(i);
			Node node = new Node(i, nc);
			nodes.add(node);
		}
		rtts = new float[nodeCount][nodeCount];
		dists = new float[nodeCount][nodeCount];
		errors = new float[nodeCount][nodeCount];
		for (int i = 0; i < nodeCount; i++) {
			for (int j = 0; j < nodeCount; j++) {
				rtts[i][j] = 0;
				dists[i][j] = 0;
				errors[i][j] = 0;
			}
		}
		// ////////////////////////////////////////////
		// Read RTT matrix

		if (rttReader == null)
			printUsage("RTT file not open");

		// Handles long list of rtts case
		String rttLine = rttReader.readLine();
		// System.out.println("$: "+rttLine);

		while (rttLine != null) {

			StringTokenizer adjTokenizer = new StringTokenizer(rttLine);
			int myId = Integer.parseInt((String) (adjTokenizer.nextElement()));
			int yourId = Integer
					.parseInt((String) (adjTokenizer.nextElement()));
			float rtt = Float.parseFloat((String) (adjTokenizer.nextElement()));

			if (rtt > 0 && myId != yourId) {
				rtts[myId - startFromOne][yourId - startFromOne] = rtt;
				// assume symmetric
				// rtts[myId][yourId] = rtt;
				if (!FULL_LATENCY_MATRIX) {
					rtts[yourId - startFromOne][myId - startFromOne] = rtt;
				}
			}

			rttLine = rttReader.readLine();
		}

		// Assumes square matrix file
		/*
		 * for (int i = 0; i < nodeCount; i++) { String rttLine =
		 * rttReader.readLine(); System.out.println("$: "+rttLine);
		 * StringTokenizer tokenizer = new StringTokenizer (rttLine);
		 * 
		 * for (int j = 0; j < nodeCount; j++) { float rtt =
		 * Float.parseFloat(tokenizer.nextToken()); rtts[i][j] = rtt; } }
		 */

		verifyRttMatrix();

		// ////////////////////////////////////////////
		// Clear dists and errors matrices
	}

	void finish() throws Exception {
		log.flush();
		log.close();
		update.flush();
		update.close();
	}

	void verifyRttMatrix() {

		for (Node me : nodes) {
			int validCount = 0;
			for (Node you : nodes) {
				float rtt = rtts[me.id][you.id];
				assert (rtt >= 0);
				String problem = null;
				if (me.id == you.id) {
					if (rtt != 0) {
						problem = "me is you " + me.id + " and rtt not 0";
					}
				} else {
					if (rtt >= NCClient.OUTRAGEOUSLY_LARGE_RTT) {
						problem = "Too large an RTT: " + rtt + " me " + me.id
								+ " you " + you.id;
					}
					if (rtt < 0.001) {
						// problem =
						// "Too small an RTT: "+rtt+" me "+me.id+" you "+you.id;
						rtt = -1;
					} else {
						validCount++;
					}
				}
				if (problem != null) {
					System.out.println(problem);
					System.exit(-1);
				}

			}

			if (validCount < nodes.size() / 2.) {
				System.out.println(me.id + " only has " + validCount
						+ " valid links");
				// System.exit (-1);
			}

		}
	}

	public static void main(String[] args) {
		try {
			Converge_Nina_Sim cs = new Converge_Nina_Sim(args);
			cs.run();
			cs.finish();
		} catch (Exception ex) {
			System.err.println("Exception: " + ex);
			ex.printStackTrace();
		}
	}

	public void printUsage(String problem) {
		System.err.println(problem);
		System.err.println("ConvergeSim Usage\n" + " -r rounds\n"
				+ " -I log output interval\n" + " -O output prefix\n"
				+ " -z use height\n" + " -x dimensions\n" + " -n node count\n"
				+ " -b latency matrix is not symmetric\n"
				+ " -l latency matrix file\n" + " -d debug\n" + " -s seed\n"
				+ " -A Vivaldi EWMA parameters\n");
		System.exit(-1);
	}

}
