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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

public class HyperCubeZoneManager implements ZoneManager {

	protected static edu.harvard.syrah.prp.Log slog = new edu.harvard.syrah.prp.Log(
			HyperCubeZoneManager.class);

	final int dimensions;
	final double zoneEdgeLength;
	final double dimensionSideLength;
	final double halfDimensionSideLength;
	final int zonesPerDimension;
	final boolean useAdjacentZones;
	final Map<Long, Zone> zoneMap;

	// note that a particular adjacent zone may not have been materialized,
	// so we just store its id
	final Map<Long, List<Long>> zone2adjacentZones;

	public HyperCubeZoneManager(int _dimensions, double _dimensionSideLength,
			double _zoneEdgeLength, boolean _useAdjacentZones) {
		dimensions = _dimensions;
		dimensionSideLength = _dimensionSideLength;
		halfDimensionSideLength = dimensionSideLength / 2.;
		zoneEdgeLength = _zoneEdgeLength;
		zonesPerDimension = (int) Math.floor(_dimensionSideLength
				/ _zoneEdgeLength);

		useAdjacentZones = _useAdjacentZones;
		zoneMap = new HashMap<Long, Zone>();
		zone2adjacentZones = new HashMap<Long, List<Long>>();
	}

	public void add(long stamp, NodeDesc node) {
		double vec[] = node.coord.asVectorFromZero(false).getComponents();

		// determine his zone
		long zoneId = findZone(vec);

		Zone z = zoneMap.get(zoneId);
		if (z == null) {
			z = new Zone(zoneId);
			zoneMap.put(zoneId, z);

			if (useAdjacentZones) {
				if (!zone2adjacentZones.containsKey(zoneId)) {
					zone2adjacentZones.put(zoneId, findAdjacentZones(vec));
				}
			}
		}
		z.add(stamp, node);

		// slog.info ("added "+node.id+" to zone "+zoneId);

	}

	public void query(NodeDesc probeSrc,
			SortedMap<Double, NodeDesc> distance2nearbyNodes, int maxReturnSize) {
		double vec[] = probeSrc.coord.asVectorFromZero(false).getComponents();

		// determine his zone
		long zoneId = findZone(vec);

		Zone z = zoneMap.get(zoneId);
		if (z != null) {
			z.probe(probeSrc, distance2nearbyNodes, maxReturnSize);
		}

		// slog.info
		// ("added "+distance2nearbyNodes.size()+" from our zone "+zoneId);

		if (useAdjacentZones
				&& (maxReturnSize == 0 || distance2nearbyNodes.size() < maxReturnSize)) {
			List<Long> adjacentZones = zone2adjacentZones.get(zoneId);
			// compute our adjacent zones in case ours is empty
			if (adjacentZones == null) {
				adjacentZones = findAdjacentZones(vec);
				zone2adjacentZones.put(zoneId, adjacentZones);
			}
			for (Long adjacentZoneId : adjacentZones) {
				Zone adjacentZone = zoneMap.get(adjacentZoneId);
				if (adjacentZone != null) {
					adjacentZone.probe(probeSrc, distance2nearbyNodes,
							maxReturnSize);
				}
				// slog.info
				// ("total "+distance2nearbyNodes.size()+" from zone "+adjacentZoneId);

				if (maxReturnSize == 0
						|| distance2nearbyNodes.size() >= maxReturnSize) {
					return;
				}
			}
		}

	}

	long findZone(double vec[]) {
		long zoneId = 0;

		for (int i = 0; i < dimensions; i++) {
			// normalize coordinate s.t. all coords are >0
			int zoneIndex = (int) (Math
					.floor((vec[i] + halfDimensionSideLength) / zoneEdgeLength));
			zoneId += Math.pow(zonesPerDimension, i) * zoneIndex;
			// slog.info (i+" c"+vec[i]+" -> "+zoneIndex+ " id "+zoneId);
		}
		return zoneId;
	}

	List<Long> findAdjacentZones(double vec[]) {
		List<Long> adjacentZones = new ArrayList<Long>();

		int zoneIndex[] = new int[dimensions];
		for (int i = 0; i < dimensions; i++) {
			zoneIndex[i] = (int) (Math.floor((vec[i] + halfDimensionSideLength)
					/ zoneEdgeLength));
		}

		for (int i = 0; i < dimensions; i++) {

			if (zoneIndex[i] > 0) {
				zoneIndex[i]--;
				adjacentZones.add(findZone(zoneIndex));
				zoneIndex[i]++;
			}
			if (zoneIndex[i] < zonesPerDimension - 1) {
				zoneIndex[i]++;
				adjacentZones.add(findZone(zoneIndex));
				zoneIndex[i]--;
			}
		}

		return adjacentZones;
	}

	long findZone(int zoneIndex[]) {
		long zoneId = 0;

		for (int i = 0; i < dimensions; i++) {
			zoneId += Math.pow(zonesPerDimension, i) * zoneIndex[i];
		}
		return zoneId;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (Map.Entry<Long, Zone> zoneEntry : zoneMap.entrySet()) {
			sb.append("\n" + zoneEntry.getValue());
		}
		return new String(sb);
	}

}
