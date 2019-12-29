package edu.NUDT.pdl.Nina.Clustering;

import java.util.List;


public class bootCache<T> {

	List<T> cachedLandmarks;     //current layers
	List<T> nextLayerBootstraps; //the first two are selected as bootstraps, which are on different clusters

	
	public List<T> getNextLayerBootstraps() {
		return nextLayerBootstraps;
	}


	public List<T> getCachedLandmarks() {
		return cachedLandmarks;
	}	
	
}
