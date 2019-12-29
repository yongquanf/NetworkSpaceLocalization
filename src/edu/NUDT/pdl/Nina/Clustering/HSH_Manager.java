package edu.NUDT.pdl.Nina.Clustering;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import edu.harvard.syrah.sbon.async.comm.AddressIF;

public abstract class HSH_Manager<T>{


	//cache the clustering results 
	private Map<T,clusteringRecord<T>> clusCache; 
	
	//---------------------------------------
	public static long SystemStartTime = -1;
	
	
	
	
	static boolean sim=false;
	public static double[][]SimDist;              //the distance matrix
	
	double[][]RelativeCoords;              //the relative coordinates
	Matrix_4_HSH  refCoordinates;
	
	List<T> AllNodes; //for indexing nodes
	List<T> BeaconNodes; //for landmarks
	
	// static int NODES_NUM = -1;
	public  abstract Matrix_4_HSH getRelativeCoordinates(T currentNode,Collection<T> allNodes,
			Collection<T> landmarks, double[] lat);

	public abstract void doInitialClustering(T me, Matrix_4_HSH tmp);

	public void setClusCache(Map<T,clusteringRecord<T>> clusCache) {
		this.clusCache = clusCache;
	}

	public Map<T,clusteringRecord<T>> getClusCache() {
		return clusCache;
	}
	/**
	 * init the ring set 
	 */
	public void init(boolean isSim){		
		sim=isSim;		
	}
	
	
}
