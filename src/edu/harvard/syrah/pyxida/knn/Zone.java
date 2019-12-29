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
 * @version $Revision: 1.6 $ on $Date: 2008/03/27 18:33:01 $
 * @since Mar 13, 2007
 */
package edu.harvard.syrah.pyxida.knn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;

/*
 * Implements circular list of collections of nodes.
 * We move to the next "interval" when either
 * (a) we are storing too many nodes in the current one
 * (b) enough time has passed.
 * 
 * This way each zone will only take up a limited amount of storage,
 * and will only return fresh results.
 */

public class Zone {

	protected static edu.harvard.syrah.prp.Log slog = new edu.harvard.syrah.prp.Log(
			Zone.class);

	final long id;

	// rotate at once per minute
	public static final long rotateSetInterval = 60 * 1000;

	// max guys per interval
	// TODO Set to 10
	public static final int maxNodesPerInterval = 1000;

	// remember this many slots of nodes
	// Note: if you make this large, you may want to change the memory
	// allocation
	// below so to be a little more judicious
	public static final int intervalCount = 10;

	// total storage per zone is:
	// maxNodesPerInterval x intervalCount pointers to NodeDescs

	final Vector<List<NodeDesc>> intervalSet;
	// final NodeDesc[][] intervalSet;

	int currentInterval = 0;
	long currentIntervalFloor = 0;

	int nodeCount = 0;

	public Zone(long _id) {
		id = _id;

		intervalSet = new Vector<List<NodeDesc>>(intervalCount);
		for (int i = 0; i < intervalCount; i++) {
			intervalSet.add(new ArrayList<NodeDesc>(0));
		}
	}

	public void add(long stamp, NodeDesc node) {
		if (node == null)
			return;
		long intervalFloor = stamp / rotateSetInterval;

		if (intervalFloor > currentIntervalFloor
				|| intervalSet.get(currentInterval).size() >= maxNodesPerInterval) {
			// rotate to next interval
			currentInterval++;
			if (currentInterval == intervalCount)
				currentInterval = 0;

			ArrayList<NodeDesc> current = (ArrayList<NodeDesc>) intervalSet
					.get(currentInterval);
			nodeCount -= current.size();
			current.clear();
			current.trimToSize();
			currentIntervalFloor = intervalFloor;
		}

		intervalSet.get(currentInterval).add(node);

		nodeCount++;
	}

	public void probe(NodeDesc probeSrc,
			SortedMap<Double, NodeDesc> distance2nearbyNodes, int maxNearbyNodes) {
		if (distance2nearbyNodes == null)
			return;
		int interval = currentInterval;
		int intervalsPassed = 0;

		while ((maxNearbyNodes == 0 || distance2nearbyNodes.size() < maxNearbyNodes)
				&& intervalsPassed < intervalCount) {
			for (NodeDesc nearbyNode : intervalSet.get(interval)) {
				if (!probeSrc.equals(nearbyNode) && probeSrc.coord != null
						&& nearbyNode.coord != null) {
					// TODO might want to use planar distance
					// so that we are perhaps more likely to land in same ISP
					double distance = probeSrc.coord
							.distanceTo(nearbyNode.coord);
					if (distance > 0) {
						distance2nearbyNodes.put(distance, nearbyNode);
						// slog.info ("probe adding "+nearbyNode.id+
						// " dist="+distance);
					}
				}
			}
			interval--;
			if (interval == -1)
				interval = intervalCount - 1;
			intervalsPassed++;
		}

	}

	public long getId() {
		return id;
	}

	public int getNodeCount() {
		return nodeCount;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer("id=" + id + " size=" + nodeCount);
		for (int i = 0; i < intervalCount; i++) {
			sb.append(" ");
			if (i == currentInterval)
				sb.append("*");
			sb.append(i + "->[ ");
			for (NodeDesc node : intervalSet.get(i)) {
				sb.append(node.id + " ");
			}
			sb.append("]");
		}
		return new String(sb);
	}

	public static void main(String argv[]) {

		NodeDesc n0 = new NodeDesc(0, null);
		NodeDesc n1 = new NodeDesc(1, null);
		NodeDesc n2 = new NodeDesc(2, null);
		NodeDesc n3 = new NodeDesc(3, null);

		Zone z = new Zone(0);
		z.add(0, n0);
		System.out.println(z);
		z.add(1, n0);
		z.add(1, n1);
		z.add(1, n2);
		z.add(2, n1);
		z.add(Zone.rotateSetInterval * 2, n0);
		System.out.println(z);

		SortedMap<Double, NodeDesc> distance2nearbyNodes = new TreeMap<Double, NodeDesc>();
		z.probe(n0, distance2nearbyNodes, 2);

		System.out.println("nearbyNodes: ");
		for (Map.Entry<Double, NodeDesc> distance2node : distance2nearbyNodes
				.entrySet()) {
			System.out.println(distance2node.getKey() + " "
					+ distance2node.getValue());
		}

		for (int i = 0; i < Zone.intervalCount * 2; i++) {
			z.add(i * Zone.rotateSetInterval, n3);
		}
		System.out.println(z);

		distance2nearbyNodes.clear();
		z.probe(n1, distance2nearbyNodes, 2);
		System.out.println("nearbyNodes: ");
		for (Map.Entry<Double, NodeDesc> distance2node : distance2nearbyNodes
				.entrySet()) {
			System.out.println(distance2node.getKey() + " "
					+ distance2node.getValue());
		}

	}

}
