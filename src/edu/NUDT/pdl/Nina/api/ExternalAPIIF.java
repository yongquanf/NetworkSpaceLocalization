package edu.NUDT.pdl.Nina.api;

import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import edu.harvard.syrah.sbon.async.CallbacksIF.CB1;

public interface ExternalAPIIF {

	/**
	 * Returns a coordinate of either a substrate node or a local proxy node
	 */
	public void getLocalCoord(CB1<Vector<Double>> cbLocalCoord);

	public void getRemoteCoord(String remoteNodeStr,
			final CB1<Vector> cbRemoteCoord);

	public void getRemoteCoords(Hashtable<String, Object> remoteNodes,
			CB1<Map<String, Object>> cbRemoteCoords);

	public void getLocalError(CB1<Double> cbLocalError);

	// Estimate the RTT
	public void estimateRTT(String nodeA, String nodeB, CB1<Double> cbLatency);

	// Create a new proxy coord with a lease
	// lease is given in ms
	// lease of 0 will expire in one hour
	// public void createProxyCoord(String remoteNode, long lease, CB1<Boolean>
	// cbResult);
	// public void createProxyCoord(String remoteNode, int lease,
	// CB1<String> cbResult);
	//
	// public void getProxyCoord(String remoteNode,
	// CB2<Vector<Double>, String> cbProxyCoord);
	//
	// // Renew proxy coord lease
	// public void renewProxyCoord(String remoteNode, int lease,
	// CB1<String> cbResult);
	//
	// public void destroyProxyCoord(String remoteNode, CB1<String> cbResult);

	// TODO Add routing methods

	// TODO add startup and shutdown either here or to pyxida main

}
