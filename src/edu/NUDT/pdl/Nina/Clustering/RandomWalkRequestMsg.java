package edu.NUDT.pdl.Nina.Clustering;

import java.util.List;

import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class RandomWalkRequestMsg<T> extends ObjMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 67L;
	public AddressIF from;
	public final int NumOfLandmarks;
	public List<T> cachedLandmarks;
	public int hops;
	/**
	 * @param from
	 * @param numOfLandmarks
	 * @param cachedLandmarks
	 */
	public RandomWalkRequestMsg(AddressIF from, int numOfLandmarks,
			List<T> cachedLandmarks) {
		this.from = from;
		NumOfLandmarks = numOfLandmarks;
		this.cachedLandmarks = cachedLandmarks;
		hops=0;
	}
	
	
		
}
