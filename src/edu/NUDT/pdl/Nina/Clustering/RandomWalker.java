package edu.NUDT.pdl.Nina.Clustering;

import java.util.List;

public abstract class RandomWalker<T> {
	
	
	/**
	 * find next hop from current list
	 * @param neighbors
	 * @return
	 */
	public abstract T NextHop(List<T> neighbors,RandomWalkRequestMsg<T> msg);
}
