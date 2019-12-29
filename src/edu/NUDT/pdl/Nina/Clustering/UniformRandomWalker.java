package edu.NUDT.pdl.Nina.Clustering;

import java.util.List;
import java.util.Random;

public class UniformRandomWalker<T> extends RandomWalker<T> {

	
	Random r=new Random(System.currentTimeMillis());
	
	
	
	/**
	 * @param r
	 */
	public UniformRandomWalker() {
		
	}



	/**
	 * uniformly at random
	 */
	public  T NextHop(List<T> neighbors,RandomWalkRequestMsg<T> msg) {
		// TODO Auto-generated method stub
		if(neighbors==null||neighbors.isEmpty()||msg.cachedLandmarks.size()==msg.NumOfLandmarks){
			return null;
		}else{
		    
			int nxt=r.nextInt(neighbors.size());
			msg.hops--;
			if(msg.hops<=0){
				msg.cachedLandmarks.add(neighbors.get(nxt));
				msg.hops=0;
			}
			return neighbors.get(nxt);
		}
	}

}
