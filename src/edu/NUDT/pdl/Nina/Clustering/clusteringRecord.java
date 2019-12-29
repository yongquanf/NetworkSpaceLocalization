package edu.NUDT.pdl.Nina.Clustering;

import edu.NUDT.pdl.Nina.KNN.ConcentricRing;

public class clusteringRecord<T>{
	public ClusteringSet myClustering; //my clustering vector
	public ConcentricRing<T>   refMeridian;  //the ring set of mine
	public boolean joined;  //joined indicator
	/**
	 * @param myClustering
	 * @param refMeridian
	 */
	public clusteringRecord(ClusteringSet myClustering,
			ConcentricRing<T> refMeridian) {
		this.myClustering = myClustering;
		this.refMeridian = refMeridian;
		joined=false;
	}
	
}