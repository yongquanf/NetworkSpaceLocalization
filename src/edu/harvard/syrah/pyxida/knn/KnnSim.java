/*
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
/*
 * @author Last modified by $Author: ledlie $
 * @version $Revision: 1.7 $ on $Date: 2008/03/27 18:33:01 $
 * @since Mar 13, 2007
 */
package edu.harvard.syrah.pyxida.knn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

import edu.NUDT.pdl.Nina.StableNC.lib.Coordinate;
import edu.NUDT.pdl.Nina.StableNC.lib.NCClient;
import gnu.getopt.Getopt;

/*
 * Driver for testing Zone assignment algorithms.
 * 
 * Make sure that input coordinates are not spaced at regular intervals.
 */

public class KnnSim {
	protected static edu.harvard.syrah.prp.Log slog = new edu.harvard.syrah.prp.Log(
			KnnSim.class);

	Set<NodeDesc> nodes = new HashSet<NodeDesc>();
	Map<NodeDesc, SortedMap<Double, NodeDesc>> node2distance2node = new HashMap<NodeDesc, SortedMap<Double, NodeDesc>>();

	int dimensions = 4;
	int dimsPlusHeight;

	ZoneManager zm = null;

	// Number of guys we would be returning to client.
	// We will see how many of the true NN of the same size we find.
	int returnSetSize = 0;
	String coordFilename = null;
	boolean useAdjacentZones = false;
	double dimensionSideLength = 10000;
	double zoneEdgeLength = 25.;
	double zoneTargetRTT = 100.;

	final static protected int NFDigits = 3;

	final static protected NumberFormat nf = NumberFormat.getInstance();
	static {
		if (nf.getMaximumFractionDigits() > NFDigits) {
			nf.setMaximumFractionDigits(NFDigits);
		}
		if (nf.getMinimumFractionDigits() > NFDigits) {
			nf.setMinimumFractionDigits(NFDigits);
		}
		nf.setGroupingUsed(false);
	}

	public static void main(String[] args) {
		KnnSim sim = new KnnSim(args);
	}

	public void printUsage(String problem) {
		System.err.println(problem);
		System.err.println("java KnnSim\n" + " -d dimensions (" + dimensions
				+ ")\n" + " -h toggle height use (" 
				+ ")\n" + " -s dimension side length (" + dimensionSideLength
				+ ")\n" + " -e zone edge length (" + zoneEdgeLength + ")\n"
				+ " -t zone target RTT (" + zoneTargetRTT + ")\n"
				+ " -a toggle adjacent zone use (" + useAdjacentZones + ")\n"
				+ " -r return set size (" + returnSetSize + ")\n"
				+ " -c coordinate file name (" + coordFilename + ")\n");
		System.exit(-1);
	}

	public KnnSim(String[] args) {
		int c;
		Getopt g = new Getopt("KnnSim", args, "d:he:s:ar:c:");

		while ((c = g.getopt()) != -1) {
			switch (c) {
			case 'd':
				dimensions = Integer.parseInt(g.getOptarg());
				break;
			case 'h':
				//NCClient.USE_HEIGHT = !NCClient.USE_HEIGHT;
				break;
			case 'e':
				zoneEdgeLength = Double.parseDouble(g.getOptarg());
				break;
			case 's':
				dimensionSideLength = Double.parseDouble(g.getOptarg());
				break;
			case 'a':
				useAdjacentZones = !useAdjacentZones;
				break;
			case 'r':
				returnSetSize = Integer.parseInt(g.getOptarg());
				break;
			case 'c':
				coordFilename = g.getOptarg();
				break;
			default:
				printUsage("Invalid argument");
			}
		}

		if (dimensionSideLength % zoneEdgeLength != 0.)
			printUsage("zone edge length must divide dimension side length evenly.");

		int zonesPerDimension = (int) (Math.floor(dimensionSideLength
				/ zoneEdgeLength));

		if (coordFilename == null)
			printUsage("coordinate file required");
		if (dimensions <= 0)
			printUsage("invalid number of dimensions");
		if (zonesPerDimension <= 1)
			printUsage("zones per dimension invalid");
		if (dimensionSideLength < 1.)
			printUsage("dimension side length invalid");
		if (returnSetSize < 0)
			printUsage("return set size invalid");

		//dimsPlusHeight = NCClient.USE_HEIGHT ? (dimensions + 1) : dimensions;

		String params = "PARAMS dims=" + dimensions + " height="
				 + " dimensionSideLength="
				+ dimensionSideLength + " zonesPerDimension="
				+ zonesPerDimension + " zoneEdgeLength=" + zoneEdgeLength
				+ " useAdjacentZones=" + useAdjacentZones + " returnSetSize="
				+ returnSetSize + " coordFile=" + coordFilename;
		System.out.println("# " + params);

		slog.info("reading coordinate file " + coordFilename);
		readCoordFile(coordFilename);
		slog.info("assigning true NN");
		assignTrueNN();

		zm = new HyperCubeZoneManager(dimensions, dimensionSideLength,
				zoneEdgeLength, useAdjacentZones);

		slog.info("adding nodes to zones");
		addNodesToZones();

		slog.info("eval zone assignment");
		evaluateZoneAssignment();
	}

	void assignTrueNN() {
		for (NodeDesc me : nodes) {
			SortedMap<Double, NodeDesc> distance2node = new TreeMap<Double, NodeDesc>();
			double nearestDistance = Double.MAX_VALUE;
			NodeDesc nearestNode = null;
			for (NodeDesc node : nodes) {
				if (me != node) {
					double distance = me.coord.distanceTo(node.coord);
					if (distance < nearestDistance) {
						nearestDistance = distance;
						nearestNode = node;
					}
				}
			}

			distance2node.put(nearestDistance, nearestNode);

			/*
			 * SortedMap<Double,NodeDesc> distance2node = new
			 * TreeMap<Double,NodeDesc>(); int count = 0; final int
			 * MAX_NEARBY_TNN = 1; for (Map.Entry<Double,NodeDesc> entry :
			 * allDistance2node.entrySet()) { distance2node.put(entry.getKey(),
			 * entry.getValue()); if (count > MAX_NEARBY_TNN) break; }
			 */
			node2distance2node.put(me, distance2node);
		}
	}

	public void addNodesToZones() {
		// for now, assume time is not a factor
		final long stamp = 0;
		for (NodeDesc node : nodes) {
			zm.add(stamp, node);
		}
		// slog.info (zm.toString());
		// System.out.println (zm.toString());
	}

	public void evaluateZoneAssignment() {
		for (NodeDesc node : nodes) {
			// slog.info ("Finding nodes nearby "+node.id);

			SortedMap<Double, NodeDesc> distance2nearbyNodes = new TreeMap<Double, NodeDesc>();

			zm.query(node, distance2nearbyNodes, 0);

			// foundSet is who we would be returning to the client
			// Set<NodeDesc> foundSet = getSetHead (distance2node,
			// returnSetSize);
			SortedMap<Double, NodeDesc> trueDistance2node = node2distance2node
					.get(node);
			double trueNearestDistance = trueDistance2node.firstKey();

			double rttSum = 0.;
			int countLtTargetRtt = 0;
			int countGtTargetRtt = 0;

			/*
			 * slog.info (node.id+" true nearest neighbors:"); for (NodeDesc tnn
			 * : tnnSet) { slog.info (""+tnn.id); }
			 */

			for (Map.Entry<Double, NodeDesc> entry : distance2nearbyNodes
					.entrySet()) {
				// slog.info (node.id+" union "+foundNode.id);
				double rtt = entry.getKey();
				rttSum += rtt;
				if (rtt < zoneTargetRTT) {
					countLtTargetRtt++;
				} else {
					countGtTargetRtt++;
				}
			}

			double avgRtt = (distance2nearbyNodes.size() > 0) ? rttSum
					/ distance2nearbyNodes.size() : 0.;

			// slog.info (node.id+" unionSize "+unionSize +
			// " pct="+nf.format(pctOverlap));
			String res = node.id
					+ " avgRtt= "
					+ nf.format(avgRtt)
					+ " lt= "
					+ countLtTargetRtt
					+ " gt= "
					+ countGtTargetRtt
					+ " pctLt= "
					+ nf
							.format((distance2nearbyNodes.size() > 0) ? (double) countLtTargetRtt
									/ (double) distance2nearbyNodes.size()
									: 0.) + " tN= "
					+ nf.format(trueNearestDistance) + " c= " + node.coord;

			System.out.println(res);

			// Could also eval mapping with abs(rank difference)
			// exponentially weighted by tnn rank

			// Or something that actually has to do with distance
			// as opposed to just rank
			// System.exit(0);
		}
	}

	Set<NodeDesc> getSetHead(SortedMap<Double, NodeDesc> theMap, int maxSize) {
		Set<NodeDesc> foundSet = new HashSet<NodeDesc>();
		for (Map.Entry<Double, NodeDesc> entry : theMap.entrySet()) {
			foundSet.add(entry.getValue());
			if (foundSet.size() == returnSetSize)
				break;
		}
		return foundSet;
	}

	void readCoordFile(String coordFilename) {
		BufferedReader coordReader = null;
		try {
			coordReader = new BufferedReader(new FileReader(new File(
					coordFilename)));
		} catch (FileNotFoundException ex) {
			System.err.println("Cannot find coord file " + coordFilename);
			System.exit(-1);
		}

		int idCounter = 0;
		try {
			String coordLine = coordReader.readLine();
			while (coordLine != null) {
				StringTokenizer coordTokenizer = new StringTokenizer(coordLine);
				float v[] = new float[dimsPlusHeight];

				for (int i = 0; i < dimsPlusHeight; i++) {
					v[i] = Float.parseFloat((String) (coordTokenizer
							.nextElement()));
				}
				NodeDesc node = new NodeDesc(idCounter++, new Coordinate(v));
				nodes.add(node);
				coordLine = coordReader.readLine();
			}

		} catch (IOException ex) {
			System.err.println("Error reading coord file: " + ex);
			System.exit(-1);
		}
	}
}
