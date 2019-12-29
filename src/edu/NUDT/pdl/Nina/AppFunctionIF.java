/*
 * Copyright 2009 Yongquan Fu
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

package edu.NUDT.pdl.Nina;

import java.util.Map;

import edu.harvard.syrah.sbon.async.CallbacksIF.CB1;

public interface AppFunctionIF {

	/**
	 * return real latency between me and remoteNode
	 * 
	 * @param remoteNodeStr
	 */
	public void queryRealRTT(final String remoteNodeStr, final CB1<Double> cbRTT);

	/**
	 * return loss rate between me and remoteNode
	 * 
	 * @param remoteNodeStr
	 * @param cbLoss
	 */
	public void GetLoss(final String remoteNodeStr, final CB1<Double> cbLoss);

	/**
	 * return coordinate distance
	 * 
	 * @param nodeA
	 * @param nodeB
	 * @param cbDistance
	 */
	public void estimateRTT(final String nodeA, final String nodeB,
			final CB1<Double> cbDistance);

	/**
	 * return local or remote coordinate
	 * 
	 * @param remoteNodeStr
	 * @param cbRemoteCoord
	 */
	public void GetCoordinate(final String remoteNodeStr,
			final CB1<Map<String, Object>> cbRemoteCoord);

	/**
	 * coordinate error
	 * 
	 * @param cbLocalError
	 */
	public void getLocalError(CB1<Double> cbLocalError);

	/**
	 * Nearest nodes,
	 * 
	 * @param cbNearestNodes
	 */
	public void getNearestNeighbors(
			final CB1<Map<String, Object>> cbNearestNodes);

	/**
	 * clustering indicator vectors
	 * 
	 * @param remoteNodeStr
	 * @param cbRemoteClusters
	 */
	public void QueryProximityCluster(final String remoteNodeStr,
			final CB1<Map<String, Object>> cbRemoteClusters);

	/**
	 * remote bandwidth
	 * 
	 * @param remoteNodeStr
	 * @param cbRemoteBandwidth
	 */
	public void GetBandwidth(final String remoteNodeStr,
			final CB1<Map<String, Object>> cbRemoteBandwidth);
}
