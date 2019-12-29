package edu.NUDT.pdl.Nina.Sim.SimClustering;

import java.util.List;

import edu.NUDT.pdl.Nina.Clustering.ClusteringSet;
import edu.NUDT.pdl.Nina.Clustering.joinRecords;

public interface HierarchicalClustering<T> {

	
	
	/**
	 * find the layer node resides 
	 * @param root
	 * @param HostSet
	 * @param layerCounter record current layer
	 */
	public joinRecords<T> findHierarchy(T root, ClusteringSet hostSet,List<T> landmarks, Object layerCounter);
	
	/**
	 * The bootstrap node find the bootstrap nodes for each layer of the clusters
	 * @param root
	 */
	//public void assignClusterBootstrap(T root);
	
	/**
	 * do clustering for the host
	 * @param landmarks
	 * @param hostNode
	 * @param pos current layer, layer starts from 1
	 */
	public void doLandmarkClustering(List<T> landmarks, T hostNode, int pos);
	
	/**
	 * start random walk from boot, in the layer where the prefix matches the hostClusteringVector
	 * @param bootStrap
	 * @param bootNode
	 * @param layer, in the specific layer
	 */
	public List<T> startRandWalk4FindingLandmarks(T bootStrap, T bootNode,int layer);
	
	/**
	 * Test the cluster validity of the layer where bootNode acts as the bootstrap node
	 * @param root
	 * @param bootNode
	 */
	public void testClusteringValidity(T root, T bootNode);
	
	/**
	 * find the candidates in the same clusters 
	 * @param bootNode
	 * @param indicator
	 * @return
	 */
	public List<T> findSameClusteringNodes(T Node,ClusteringSet indicator,int bits);
}
