package edu.NUDT.pdl.Nina.Sim.SimClustering;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.NUDT.pdl.Nina.Clustering.ClusteringSet;
import edu.NUDT.pdl.Nina.Clustering.Matrix_4_HSH;
import edu.NUDT.pdl.Nina.Clustering.RandomWalkRequestMsg;
import edu.NUDT.pdl.Nina.Clustering.UniformRandomWalker;
import edu.NUDT.pdl.Nina.Clustering.bootCache;
import edu.NUDT.pdl.Nina.Clustering.clusteringRecord;
import edu.NUDT.pdl.Nina.Clustering.joinRecords;
import edu.NUDT.pdl.Nina.KNN.ConcentricRing;

public class RecurDisBasedHSH<T> implements HierarchicalClustering<T>{

	//---------------------------------------
	Map<T,bootCache<T>> pendingBootStraps;
	Map<T,clusteringRecord<T>> clusCache; 
	
	int defaultLandmarks=10;
	int clusteringThresholds=10;
	double clusteringValidityThreshold=0.5; //only >0.5 nodes have distinct clustering validity
	double clusteringSetValidityThreshold=0.6; //the ratio between the clustering possibility between two dimensions
	
	
	
	
/*	//---------------------------------------
	class clusteringRecord<T>{
		ClusteringSet myClustering; //my clustering vector
		Meridian<T>   refMeridian;  //the ring set of mine
		boolean joined;  //joined indicator
		*//**
		 * @param myClustering
		 * @param refMeridian
		 *//*
		public clusteringRecord(ClusteringSet myClustering,
				Meridian<T> refMeridian) {
			this.myClustering = myClustering;
			this.refMeridian = refMeridian;
			joined=false;
		}
		
	}*/
	

	
	static boolean isSim=true;
	static double[][]SimDist;
	
	/**
	 * a host joins the clustering structure
	 * @param root
	 * @param HostNode
	 */
    public void join(T root, T HostNode,ConcentricRing<T> hostMeridian){
    	//save 
    	if(!clusCache.containsKey(HostNode)){
    		ClusteringSet myClustering=new ClusteringSet();
    		clusCache.put(HostNode, new clusteringRecord(myClustering, hostMeridian));
    	}
    	//find bootNode
    	List<T> landmark=pendingBootStraps.get(root).getCachedLandmarks();
    	int[] layerCounter={0};
    	joinRecords<T> boot=findHierarchy(root,clusCache.get(HostNode).myClustering,landmark,layerCounter);
    	
    	// landmark walks   
    	 List<T> landmarks=boot.getLandmarks();
    	if(landmarks==null||landmarks.isEmpty()){
    		landmarks=startRandWalk4FindingLandmarks(root,boot.getBootNode(),boot.getLayer());
    	}
    	//
    	if(landmarks==null||landmarks.size()<defaultLandmarks){
    		//failed
    		
    		return;
    	}else{
    		//join correctly;
    		
    		doLandmarkClustering(landmarks,HostNode,boot.getLayer());
    		clusCache.get(HostNode).joined=true;
    	}
    }
	
    
	public joinRecords<T> findHierarchy(T root, ClusteringSet hostSet,List<T> landmarks, Object layerCounter) {
		// TODO Auto-generated method stub
    	
    	List<T> landmark=pendingBootStraps.get(root).getCachedLandmarks();	
		if( hostSet==null){
			//the fist layer, not joined			
			landmarks.addAll(landmark);
			joinRecords<T> joined=new joinRecords<T>(root,landmarks);
			((int[])layerCounter)[0]++;
			joined.setLayer(((int[])layerCounter)[0]);
			return joined;
		}else{
			//the layer is initialized
			List<T> nextBootstraps=pendingBootStraps.get(root).getNextLayerBootstraps();
			
			if(nextBootstraps!=null&&nextBootstraps.size()==2){
				//yes, find, and we are ready to go to next layer
				T ALandmark=pendingBootStraps.get(root).getNextLayerBootstraps().get(0);
				T BLandmark=pendingBootStraps.get(root).getNextLayerBootstraps().get(1);
				
				if(!clusCache.containsKey(ALandmark)||!clusCache.containsKey(BLandmark)|| clusCache.get(ALandmark).myClustering==null||	
						clusCache.get(BLandmark).myClustering==null){
					landmarks.addAll(landmark);
					joinRecords<T> joined=new joinRecords<T>(root,landmarks);
					((int[])layerCounter)[0]++;
					return joined;
				}else{
					//find the matched side
					int matchedA=hostSet.findCommonBits(clusCache.get(ALandmark).myClustering);
					int matchedB=hostSet.findCommonBits(clusCache.get(BLandmark).myClustering);
					T newRoot;
					if(matchedA>matchedB){
						newRoot=ALandmark;
					}else{
					    newRoot=BLandmark;
					}
					return findHierarchy(newRoot,hostSet,landmarks,layerCounter);
				}
				
			}else{
				landmarks.addAll(landmark);
				joinRecords<T> joined=new joinRecords<T>(root,landmarks);
				((int[])layerCounter)[0]++;
				return joined;
			}
			
		}
	}
	
    
	
	/**
	 * TODO: compute the clustering vector 
	 * 10.23
	 */
	public void doLandmarkClustering(List<T> landmarks, T hostNode, int pos) {
		
		//empty
	   if(landmarks==null||landmarks.isEmpty()){
		   return;
	   }
	   int N=landmarks.size();
	   int dim=2;
	   double[][] _H=new double[N][dim];
	   double[][] _S=new double[dim][dim];
		//(1) if landmarks do not have clustering vectors, cluster them first
		int len=landmarks.size();
		double[][]dist=new double[len][len];
		for(int i=0;i<len;i++){
			for(int j=i;j<len;j++){
				if(i==j){
					dist[i][j]=0;
				}
				dist[i][j]=GetDitance(landmarks.get(i),landmarks.get(j));
				dist[j][i]=dist[i][j];
			}
		}
		//update the clustering indicator for landmarks
		Matrix_4_HSH m=new Matrix_4_HSH();
		m.symmetric_NMF(N, dim, dist, _H, _S);
		
		boolean bits[]=m.transformed2BinaryVectors(_H);
		for(int i=0;i<bits.length;i++){
			clusCache.get(landmarks.get(i)).myClustering.setLayer(pos, bits[i]);
		}
	
		dist=null;
		bits=null;
		//(2) use the clustering vector of landmarks, we compute the clustering indicator of hostNode
		double[] host2Landmark=new double[N];
		for(int i=0;i<N;i++){
			 host2Landmark[i]=GetDitance(hostNode,landmarks.get(i));
		}
		Matrix_4_HSH host2Landmarks=new Matrix_4_HSH(1,N);
		Matrix_4_HSH LM_H=new Matrix_4_HSH(N,dim);
		Matrix_4_HSH LM_S=new Matrix_4_HSH(dim,dim);
		for(int i=0;i<N;i++){
			host2Landmarks.mat[0][i]=host2Landmark[i];
			for(int j=0;j<dim;j++){
				LM_H.mat[i][j]=_H[i][j];	
			}
		}
		for(int i=0;i<dim;i++){
			for( int j=0;j<dim;j++){
				LM_S.mat[i][j]=_S[i][j];
			}
		}
		Matrix_4_HSH hostClustering=m.D_N_S_NMF(host2Landmarks, LM_H, LM_S);
		
		bits=m.transformed2BinaryVectors(hostClustering.mat);
		clusCache.get(hostNode).myClustering.setLayer(pos, bits[0]);
		
		
		host2Landmarks.clear();
		LM_H.clear();
		LM_S.clear();
		_H=null;
		_S=null;
	}

	/**
	 * get the network distance, 
	 * @param from
	 * @param to
	 * @return
	 */
	double GetDitance(T from,T to){
		//simulation model
		if(RecurDisBasedHSH.isSim){
		return RecurDisBasedHSH.SimDist[((Integer)from).intValue()][((Integer)to).intValue()];
		}
		else{
			int[] latencyUS={-1};
			clusCache.get(from).refMeridian.g_rings.getNodeLatency(to, latencyUS);
			return  latencyUS[0];
		}
	}
	
	
	/**
	 * we use random walks to sample landmarks from bootNode
	 */
	public List<T> startRandWalk4FindingLandmarks(T bootStrap, T bootNode, int pos) {
		// TODO Auto-generated method stub
		//the random walk process
		RandomWalkRequestMsg<T> msg=new RandomWalkRequestMsg<T>(null, defaultLandmarks,new ArrayList<T>(1));
		
		//find the set of nodes in the current layer, then construct the random walk message, and sends it to 
		//one of them uniformly at random

		UniformRandomWalker<T> walker=new UniformRandomWalker<T>();
		
		T start=bootNode;
		//identical bits of the layers
		int bits=clusCache.get(bootNode).myClustering.getLayer();	

				
		while(start!=null){
			
			//find neighbors
			List<T> neighbors=findSameClusteringNodes(start,clusCache.get(start).myClustering,bits);
			start=walker.NextHop(neighbors, msg);
		}
		return msg.cachedLandmarks;
	}

	/*
	 * find the neighbors from current clusters
	 * @see edu.NUDT.pdl.Nina.Clustering.SIM.HierarchicalClustering#findSameClusteringNodes(java.lang.Object, edu.NUDT.pdl.Nina.Clustering.ClusteringSet)
	 */
	public List<T> findSameClusteringNodes(T Node,ClusteringSet indicator,int bits){
		List<T> nodes=new ArrayList<T>(1);
		Iterator<Entry<T, ClusteringSet>> ier = clusCache.get(Node).refMeridian.g_rings.getClusteringVec().entrySet().iterator();
		while(ier.hasNext()){
			Entry<T, ClusteringSet> tmp = ier.next();
			int bit=tmp.getValue().findCommonBits(indicator);
			if(bit>=bits){
				nodes.add(tmp.getKey());
			}
		}
	return nodes;
	}
	
	
	
	
	
	public void testClusteringValidity(T root, T bootNode) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * remove offline nodes
	 * @param root
	 */
	public void updateLandmarks(T root){
		
	}
	
	
}
