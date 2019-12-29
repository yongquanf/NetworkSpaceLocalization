package edu.NUDT.pdl.Nina.Distance;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import edu.NUDT.pdl.Nina.KNN.NodesPair;
import edu.NUDT.pdl.Nina.util.MathUtil;
import edu.NUDT.pdl.Nina.util.Matrix;
import edu.harvard.syrah.prp.Log;
import edu.harvard.syrah.prp.SortedList;

public class ScalableBinning<T> {

	Log log=new Log(ScalableBinning.class);
	/**
	 * <Integer, nodes>, index
	 */
	public Map<Integer,ArrayList<T>> cachedBinNodes;
	
	
	/**
	 *  <Integer, nodes> ,index, store the additional nodes
	 */
	public Map<Integer,ArrayList<T>> secondryCachedBinNodes;
	/**
	 * reverse mapping
	 * node->bin
	 */
	public   Map<T,Integer> BinnedInfo;
	
	/**
	 * cache the outlier bin
	 */
	public ArrayList<T> specialBinning;
	
	/**
	 * <id, relative coordinates>
	 */
	public  Map<T,List<NodesPair>> cachedRelativeCoordinate;
	
	/**
	 * map table
	 */
	DistanceHandler<T> distHandler;
	
	/**
	 * landmarks
	 */
	public List BeaconNodes; //for relative coordinate
	
	int id=0;
	
	MathUtil math=new MathUtil(10);
	
	public ScalableBinning(){
		
		secondryCachedBinNodes=new ConcurrentHashMap<Integer,ArrayList<T>>(5);
		cachedBinNodes	=new ConcurrentHashMap<Integer,ArrayList<T>>(5);
		cachedRelativeCoordinate = new ConcurrentHashMap<T,List<NodesPair>>(5);
		distHandler=new DistanceHandler<T>();
		BeaconNodes=new ArrayList();
		BinnedInfo=new ConcurrentHashMap<T,Integer> (2);
		specialBinning=new ArrayList<T> ();
		id=0;
	}
	
	/**
	 * init the mapping
	 * @return 
	 */
	public void initLandmarkMapping(List<T> landmarks){
		distHandler.mappedLandmark.clear();
		Iterator<T> ier = landmarks.iterator();
		while(ier.hasNext()){
			 T tmp = ier.next();
			 distHandler.addMap(tmp);
		}
		
	}
	/**
	 * init the mapping
	 */
	public void initLandmarkMapping(){
		if(BeaconNodes.isEmpty()){
			log.warn("empty landmarks!");
			return;
		}
		distHandler.mappedLandmark.clear();
	
		Iterator<T> ier = BeaconNodes.iterator();
		while(ier.hasNext()){
			 T tmp = ier.next();
			 distHandler.addMap(tmp);
		}
		
	}
	
	/**
	 * add the relative coordinate
	 * @param node
	 * @param relativeCoort
	 */
	public void addRelativeCoordinates(T node ,List<NodesPair> relativeCoort){
		if(node!=null&&relativeCoort!=null&&!relativeCoort.isEmpty()){
			List<NodesPair>  copied=new ArrayList<NodesPair>(2);
			copied.addAll(relativeCoort);
		this.cachedRelativeCoordinate.put(node, copied);
		}
	}
	
	
	
	/**
	 * TODO: unstable version of merging
	 * @param dim
	 * @param cutoff
	 * @param dist, the edit/Ulam distance
	 */	
	Integer incrementAddBinning( int cutoff, T node){
		int len=cachedBinNodes.size();
		Integer index;
		if(len==0){
				ArrayList<T> list=new ArrayList<T>(2);
				list.add(node);
				//set current id, and increment it
				index=Integer.valueOf(id++);
				cachedBinNodes.put(index, list);
				this.BinnedInfo.put(node, index);
		}else{
			index=findClosestBinning(cutoff, node);
			if(index!=null){
			//found	
				ArrayList<T> list = cachedBinNodes.get(index);
				list.add(node);
				cachedBinNodes.put(index, list);
				this.BinnedInfo.put(node, index);
			}else{
				//not found
				ArrayList<T> list=new ArrayList<T>(2);
				list.add(node);
				
				index=Integer.valueOf(id++);
				while(cachedBinNodes.containsKey(index)){
					index=Integer.valueOf(id++);
				}
				
				cachedBinNodes.put(index, list);
				this.BinnedInfo.put(node, index);
			}
		}	
		return index;
	}
	
	/**
	 * initialize the bin for each node, then merge all
	 * @param node
	 */
	public void initBinAndMerge(List<T> nodes,int cutoff,int listThreshold){
		//initialize the bin
		Iterator<T> ier = nodes.iterator();
		while(ier.hasNext()){
			T tmp = ier.next();	
			ArrayList<T> list = new ArrayList<T>(1);
			list.add(tmp);
			log.debug("bin ID: "+id);
			Integer idBin=Integer.valueOf(id++);
			this.cachedBinNodes.put(idBin, list);
			this.BinnedInfo.put(tmp, idBin);
		}
		//merge the bin
		this.mergeBinning(cutoff);
		//post
		this.postProcess(listThreshold);
	}
	
	/**
	 * add one node into the bins
	 * @param cutoff
	 * @param node
	 * @return
	 */
	public Integer AddOneNode( int cutoff, T node){
		
		int len=cachedBinNodes.size();
		Integer index;
		if(len==0){
				ArrayList<T> list=new ArrayList<T>(2);
				list.add(node);
				//set current id, and increment it
				index=Integer.valueOf(id++);
				cachedBinNodes.put(index, list);
				this.BinnedInfo.put(node, index);
		}else{
			
			index=findClosestBinning(cutoff, node);
			if(index!=null){
			//found									
				ArrayList<T> list = cachedBinNodes.get(index);
				//already in
				if(list.contains(node)){
					return null;
				}
				else{
				list.add(node);
				cachedBinNodes.put(index, list);
				this.BinnedInfo.put(node, index);
				}
				
			}else{
				//not found
				ArrayList<T> list=new ArrayList<T>(2);
				list.add(node);
				
				index=Integer.valueOf(id++);
				while(cachedBinNodes.containsKey(index)){
					index=Integer.valueOf(id++);
				}
				
				cachedBinNodes.put(index, list);
				this.BinnedInfo.put(node, index);
			}
		}	
		return index;
	}
	
	/**
	 * add nodes to secondary nodes
	 * @param cutoff
	 * @param node
	 */
	public void addSecondryNode( int cutoff, T node){
		
		Integer index = findClosestBinning(cutoff, node);
	    if(index==null){
	    	log.warn("can not find closest bin");
	    	return;
	    }else{
	    	if(!secondryCachedBinNodes.containsKey(index)){
	    		ArrayList<T> list=new ArrayList<T>(2);
				list.add(node); 
				secondryCachedBinNodes.put(index, list);
	    	}else{
	    		secondryCachedBinNodes.get(index).add(node);
	    	}	    	
	    }
		
	}
	/**
	 * merge cached nodes
	 * @param cutoff
	 * @param listThreshold
	 */
	public void initBinAndMerge(int cutoff,int listThreshold){
		//initialize the bin
		Iterator<T> ier = this.cachedRelativeCoordinate.keySet().iterator();
		while(ier.hasNext()){
			T tmp = ier.next();	
			ArrayList<T> list = new ArrayList<T>(1);
			list.add(tmp);
			log.debug("bin ID: "+id);
			Integer idBin=Integer.valueOf(id++);
			this.cachedBinNodes.put(idBin, list);
			this.BinnedInfo.put(tmp, idBin);
		}
		//merge the bin
		this.mergeBinning(cutoff);
		//post
		//this.postProcess(listThreshold);
	}
	
	/**
	 * stable version of merge
	 * merge the nodes by their relative coordinates
	 */
	void mergeBinning( int cutoff){
		
		ArrayList<Integer> listInteger = new ArrayList<Integer>(2);
		for(int level=0;level<=cutoff;level++){
			
			//iterate the cached bins
			listInteger.clear();
			Iterator<Integer> ier = this.cachedBinNodes.keySet().iterator();
			while(ier.hasNext()){
				Integer ind = ier.next();
				listInteger.add(ind);
			}
			//===========================
			//iterate the inter-bin distance
			//
			for(int i=0;i<listInteger.size();i++){
				for(int j=(i+1);j<listInteger.size();j++){
					float dist=this.averageDistance(listInteger.get(i), listInteger.get(j));
					//invalid distance
					if(dist<0){
						continue;
					}
					//valid distance
					log.debug("dist: "+dist);
					if(dist<=level){
						merge(listInteger.get(i),listInteger.get(j));
						
					}
				}
				
			}
			//===========================
			
		}		
	}
	
	/**
	 * merge two bins
	 * @param binA
	 * @param binB
	 */
	private void merge(Integer binA, Integer binB) {
		// TODO Auto-generated method stub
		ArrayList<T> listA=this.cachedBinNodes.get(binA);
		ArrayList<T> listB=this.cachedBinNodes.get(binB);
		
		if(listA==null||listB==null||listA.isEmpty()||listB.isEmpty()){
			return;
		}
		else{
			//merge nodes
			listA.addAll(listB);
			this.cachedBinNodes.put(binA, listA);
		
			//change bin number
			Iterator<T> ier = listB.iterator();
			while(ier.hasNext()){
				T tmp = ier.next();
				this.BinnedInfo.put(tmp, binA);			
			}
			this.cachedBinNodes.remove(binB);
		}
				
	}

	/**
	 * 
	 * @param cutoff
	 * @param node
	 * @return
	 */
	Integer findClosestBinning(int cutoff, T node){
		
		Iterator<Integer> ier = this.cachedBinNodes.keySet().iterator();
		float minDist=Float.MAX_VALUE;
		Integer indexMin=null;
		while(ier.hasNext()){
			Integer tmp = ier.next();
			float avgDist=this.averageDistance( node, tmp);
			if(avgDist<minDist){
				minDist=avgDist;
				indexMin=tmp;
			}
		}
		//found a bin
		if(minDist<=cutoff){
			return indexMin;
		}else{
		//not found
			return null;
		}
	}
	
	/**
	 * median distance
	 * @param cutoff
	 * @param node
	 * @param index
	 * @return
	 */
	float averageDistance(T node, Integer index){
		ArrayList<T> nodes = cachedBinNodes.get(index);
		float sum=0;
		double percentile=.5;
		Vector<Double> vec=new Vector<Double>(2);
		for(int i=0;i<nodes.size();i++){
			List<NodesPair> A = this.cachedRelativeCoordinate.get(node);
			List<NodesPair> B=null;
			//change the coordinate by other thread
			//FIX: reset the binnng
			if(nodes.size()>i){
			 B= this.cachedRelativeCoordinate.get(nodes.get(i));
			}
			if(A==null||B==null){
				
				continue;
			}
			//sum+=distHandler.UlamBasedcomputeEditDistanceFromTwoNodes(A, B);
			vec.add((double)distHandler.UlamBasedcomputeEditDistanceFromTwoNodes(A, B));
		}
		return (float)math.percentile(vec, percentile);
		
	}
	
	/**
	 * compute the average distance between a node and all nodes in a bin
	 * @param A
	 * @param index
	 * @return
	 */
	float averageDistance(List<NodesPair> A, Integer index){
		ArrayList<T> nodes = cachedBinNodes.get(index);
		float sum=0;
		double percentile=.5;
		Vector<Double> vec=new Vector<Double>(2);
		for(int i=0;i<nodes.size();i++){
			
			List<NodesPair> B = this.cachedRelativeCoordinate.get(nodes.get(i));
			if(A==null||B==null){
				
				continue;
			}
			//sum+=distHandler.UlamBasedcomputeEditDistanceFromTwoNodes(A, B);
			vec.add((double)distHandler.UlamBasedcomputeEditDistanceFromTwoNodes(A, B));
		}
		return (float)math.percentile(vec, percentile);
		
	}
	
	/**
	 * averaged inter-cluster distance
	 * @param indexA
	 * @param indexB
	 * @return
	 */
	float averageDistance(Integer indexA, Integer indexB){
		ArrayList<T> nodesA = cachedBinNodes.get(indexA);
		ArrayList<T> nodesB = cachedBinNodes.get(indexB);
		if(nodesA==null||nodesB==null||nodesB.isEmpty()||nodesA.isEmpty()){
			return -1;
		}
		
		float sum=0;
		double percentile=.5;
		Vector<Double> vec=new Vector<Double>(2);
		for(int i=0;i<nodesA.size();i++){
			for(int j=0;j<nodesB.size();j++){
			List<NodesPair> A = null;
			List<NodesPair> B = null;
			if(nodesB.size()>j){
				this.cachedRelativeCoordinate.get(nodesB.get(j));
			}
			
			if(nodesA.size()>i){
				this.cachedRelativeCoordinate.get(nodesA.get(i));
			}
			if(A==null||B==null){				
				continue;
			}
			//sum+=distHandler.UlamBasedcomputeEditDistanceFromTwoNodes(A, B);
			vec.add((double)distHandler.UlamBasedcomputeEditDistanceFromTwoNodes(A, B));
			}
		}
		float dist=(float)math.percentile(vec, percentile);
		vec.clear();
		vec=null;
		return dist;		
	}
	
	//==================================================
	// remove a node from the binning
	//==================================================
	public boolean removeFromBins(T node){
		if(this.BinnedInfo.containsKey(node)){
			Integer binNo=this.BinnedInfo.get(node);
			this.cachedBinNodes.get(binNo).remove(node);
			this.BinnedInfo.remove(node);
			this.cachedRelativeCoordinate.remove(node);
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * query the size of the bin
	 * @return
	 */
	public int getBinSizeForCandidateNodes(int binNo){
		if(this.cachedBinNodes.containsKey(Integer.valueOf(binNo))){
			return this.cachedBinNodes.size();
		}else{
			return -1;
		}
		
	}
	
	/**
	 * return the closest bin for specific node
	 * @param relCoord
	 * @return
	 */
	public Integer getBinNoForNode(int cutoff, List<NodesPair> relCoord){
		
		Iterator<Integer> ier = this.cachedBinNodes.keySet().iterator();
		float minDist=Float.MAX_VALUE;
		Integer indexMin=null;
		while(ier.hasNext()){
			Integer tmp = ier.next();
			float avgDist=this.averageDistance( relCoord, tmp);
			if(avgDist<minDist){
				minDist=avgDist;
				indexMin=tmp;
			}
		}
		//found a bin
		if(minDist<=cutoff){
			return indexMin;
		}else{
		//not found
			return null;
		}
	}
	
	//==================================================
	// search closest nodes towards target from candidates
	//==================================================
	/**
	 * find k closest nodes based on the edit distance
	 * @param k
	 * @param candidates
	 * @return
	 */
	public List<T> findKCloestNodesForKNN(int k, T fromNode, List<T> candidates){
		
		List<T> kclosestNodes=new ArrayList<T>(5);
		
		SortedList<NodesPair> sortedL = new SortedList<NodesPair>(
				new NodesPairComp());
		int realK=Math.min(k, candidates.size());
		Iterator<T> ier = candidates.iterator();
		while(ier.hasNext()){
			T target = ier.next();
			List<NodesPair> A = this.cachedRelativeCoordinate.get(fromNode);
			//TODO: A==null
			List<NodesPair> B = this.cachedRelativeCoordinate.get(target);
			
			int dist = this.distHandler.UlamBasedcomputeEditDistanceFromTwoNodes(A, B);
			sortedL.add(new NodesPair(fromNode,target,dist));
		}
		
		Iterator<NodesPair> ier2 = sortedL.iterator();
		while(ier2.hasNext()){
		NodesPair rec = ier2.next();	
		 kclosestNodes.add((T)rec.endNode);
		}
		sortedL.clear();
		return  kclosestNodes;
	}
	
	
	
	/**
	 * find k farthest nodes
	 * @param k
	 * @param ANode
	 * @param candidates
	 * @return
	 */
	public List<T> findKFarthestNodesForKNN(int k, List<NodesPair> ANode, List<T> candidates){
		
		
		List<T> kFarthestNodes=new ArrayList<T>(5);
		
		SortedList<NodesPair> sortedL = new SortedList<NodesPair>(
				new NodesPairComp());
		int realK=Math.min(k, candidates.size());
		Iterator<T> ier = candidates.iterator();
		while(ier.hasNext()){
			T target = ier.next();
			//List<NodesPair> A = this.cachedRelativeCoordinate.get(fromNode);
			//TODO: A==null
			List<NodesPair> B = this.cachedRelativeCoordinate.get(target);
			
			int dist = this.distHandler.UlamBasedcomputeEditDistanceFromTwoNodes(ANode, B);
			
			sortedL.add(new NodesPair(null,target,dist));
		}
		
		//2010-3-8
		//distance threshold
		double dist_threshold=k;
		
		Iterator<NodesPair> ier2 = sortedL.iterator();
		while(ier2.hasNext()){
			
		NodesPair rec = ier2.next();	
		if(rec.rtt>dist_threshold)
			{		
			kFarthestNodes.add((T)rec.endNode);
			}else{
				break;
			}
		}
		

		sortedL.clear();
		
		return  kFarthestNodes;
	}
	
	/**
	 * find all nodes that are below cutoff, as well as sampled nodes from each bin
	 * @param nodesPerBin
	 * @param cutoff
	 * @param ANode
	 * @param candidates
	 * @return
	 */
	public List<T> findOptimisedClosestNodesForKNN(int nodesPerBin,int cutoff ,List<NodesPair> ANode, List<T> candidates){
		
		//nodes per bin
		List<T> randomNodes = findKNodesFromAllBinsForGossip(nodesPerBin);
		
		//nodes from nearest bin
		List<T> nearestNodes = findKCloestNodesForKNN(cutoff, ANode, candidates);
	
		List<T> allNodes=new ArrayList<T>(5);
		if(randomNodes!=null&&!randomNodes.isEmpty()){
			allNodes.addAll(randomNodes);
		}
		if(nearestNodes!=null&&!nearestNodes.isEmpty()){
			allNodes.addAll(nearestNodes);
		}
		randomNodes.clear();
		nearestNodes.clear();
		return allNodes;
	}
	

	
	/**
	 * measure the edit distance between target and me
	 * @param k, the threshold of the edit distance,
	 * @param ANode
	 * @param candidates
	 * @return
	 */
	public List<T> findKCloestNodesForKNN(int k, List<NodesPair> ANode, List<T> candidates){
		
		List<T> kclosestNodes=new ArrayList<T>(5);
		
		SortedList<NodesPair> sortedL = new SortedList<NodesPair>(
				new NodesPairComp());
		
		//int realK=Math.min(k, candidates.size());
		
		Iterator<T> ier = candidates.iterator();
		while(ier.hasNext()){
			T target = ier.next();
			//List<NodesPair> A = this.cachedRelativeCoordinate.get(fromNode);
			//TODO: A==null
			List<NodesPair> B = this.cachedRelativeCoordinate.get(target);
			
			int dist = this.distHandler.UlamBasedcomputeEditDistanceFromTwoNodes(ANode, B);
			sortedL.add(new NodesPair(null,target,dist));
		}
		
		//edit distance <k
		double dist_threshold=k;
						
		Iterator<NodesPair> ier2 = sortedL.iterator();
		while(ier2.hasNext()){
			
		NodesPair rec = ier2.next();	
		if(rec.rtt<=dist_threshold)
			{		
		 kclosestNodes.add((T)rec.endNode);
			}else{
				break;
			}
		}
		
		
		
		sortedL.clear();
		return  kclosestNodes;
	}
	
	//=============================================================
	// search k nodes that maximize the difference from the bins
	//=============================================================
	public List<T> findKNodesFromAllBinsForGossip(int k){
		
		List<T> nodes=new ArrayList<T>(2);		
		int totalBins=specialBinning.isEmpty()?this.BinnedInfo.size():this.BinnedInfo.size()+this.specialBinning.size();
		//weighted
		 double nodesPerBin = (k+0.1)/ totalBins;
		Iterator<Entry<Integer, ArrayList<T>>> ier = this.cachedBinNodes.entrySet().iterator();
		while(ier.hasNext()){
			Entry<Integer, ArrayList<T>> tmp = ier.next();
			int selected = (int) Math.round(nodesPerBin*tmp.getValue().size());
			Collections.shuffle(tmp.getValue());
			for(int i=0;i<selected;i++){
				nodes.add(tmp.getValue().get(i));
			}
			
		}
		
		if(!specialBinning.isEmpty()){
			int selected = (int) Math.round(nodesPerBin*specialBinning.size());
			Collections.shuffle(specialBinning);
			for(int i=0;i<selected;i++){
				nodes.add(specialBinning.get(i));
			}
		}
		return nodes;
	}
	
	/**
	 * select k nodes that maximize the difference from the bins from candidates
	 * @param k
	 * @param candidates, contain remained nodes after selection
	 * @return selected nodes
	 */
	public List<T> findKNodesFromCandidates(int k, List<T> candidates){
		
		if(k>=candidates.size()){
			return candidates;
		}
		
		List<T> nodes=new ArrayList<T>(2);		
		//use a temporary bins		
		Hashtable<Integer,ArrayList<T>> cachedBinsForCandidates=new Hashtable<Integer,ArrayList<T>> (5);		
		Iterator<T> ier2 = candidates.iterator();
		while(ier2.hasNext()){
			T A = ier2.next();
			Integer indBin=this.BinnedInfo.get(A);
			if(cachedBinsForCandidates.containsKey(indBin)){
				ArrayList<T> list = cachedBinsForCandidates.get(indBin);
				list.add(A);
				cachedBinsForCandidates.put(indBin, list);
			}else{
				ArrayList<T> list = new ArrayList<T> (5);
				list.add(A);
				cachedBinsForCandidates.put(indBin, list);				
			}			
		}			
		
		int totalBins=cachedBinsForCandidates.size();
		//weighted
		 double nodesPerBin = (k+0.1)/ totalBins;
		Iterator<Entry<Integer, ArrayList<T>>> ier = cachedBinsForCandidates.entrySet().iterator();
		while(ier.hasNext()){
			Entry<Integer, ArrayList<T>> tmp = ier.next();
			int selected = (int) Math.round(nodesPerBin*tmp.getValue().size());
			if(selected>tmp.getValue().size()){
				selected=tmp.getValue().size();
			}
			Collections.shuffle(tmp.getValue());
			for(int i=0;i<selected;i++){
				nodes.add(tmp.getValue().get(i));
			}
			
		}
		candidates.removeAll(nodes);
		cachedBinsForCandidates.clear();
		return nodes;
	}
	
	
	
	/**
	 * TODO: post-process the binning, any bin that has <t nodes are merged,as special bins
	 */
	public void postProcess(int threshold){
		
		List<Integer> nodes=new ArrayList<Integer>(2);
		Iterator<Integer> ier = this.cachedBinNodes.keySet().iterator();
		while(ier.hasNext()){
			Integer binNo = ier.next();	
			if(this.cachedBinNodes.get(binNo).size()<threshold){
				nodes.add(binNo);
		}		
		}
		//============================
		Integer finalBinNo=null;
		ArrayList<T> candidates=new ArrayList<T>(2);
		Iterator<Integer> ier2 = nodes.iterator();
		while(ier2.hasNext()){
			Integer toRemoved = ier2.next();
			candidates.addAll(this.cachedBinNodes.get(toRemoved));
			this.cachedBinNodes.remove(toRemoved);
			finalBinNo=toRemoved;
		}
		if(!candidates.isEmpty()|| finalBinNo==null){
			return;
		}else{
			//this.cachedBinNodes.put(finalBinNo, candidates);
			this.specialBinning.addAll(candidates);
			
			Iterator<T> ier3 = candidates.iterator();
			while(ier3.hasNext()){
				T node = ier3.next();
				this.BinnedInfo.put(node, finalBinNo);
			}
			
		}
		
	}
	
	/**
	 * averaged inter-bin distance
	 * @param indexOfBinA
	 * @param indexOfBinB
	 * @return
	 */
	float interBin(Integer indexOfBinA,Integer indexOfBinB,Matrix mat){
		
		ArrayList<T> nodesA = cachedBinNodes.get(indexOfBinA);
		ArrayList<T> nodesB = cachedBinNodes.get(indexOfBinB);
		//exception
		if(nodesA==null||nodesB==null||nodesA.isEmpty()||nodesB.isEmpty()){
			return -1;
		}
		
		else{
			float sum=0;
			int count=0;
			
			Iterator<T> ierA = nodesA.iterator();
			Iterator<T> ier2 = nodesB.iterator();

			
			Vector<Double> vec=new Vector<Double>(5);
			
			while(ierA.hasNext()){
				T A = ierA.next();
				while(ier2.hasNext()){
					T B = ier2.next();
					if(A.equals(B)){
						continue;
					}else{
						
						//sum+=distHandler.UlamBasedcomputeEditDistanceFromTwoNodes(A1, B1);
						float dist=mat.get((Integer)A,(Integer)B);
						if(dist<=0){
							continue;
						}else{
							//sum+=dist;
							//count++;
							vec.add((double)dist);
						}
					}
				}
				
				
				
				count++;
			}
			if(count==0){
				return -1;
			}
			return (float)this.math.percentile(vec, .5);
		}
		
	}
	
	
	/**
	 * averaged  intra-bin distance
	 * @param indexOfBin
	 * @return
	 */
	float intraBin(Integer indexOfBin,Matrix mat){
		ArrayList<T> nodes = cachedBinNodes.get(indexOfBin);
		//exception
		if(nodes==null||nodes.isEmpty()){
			return -1;
		}
		
		Vector<Double> vec=new Vector<Double>(5);
		//binning
		float sum=0;
		int count=0;
		Iterator<T> ier1 = nodes.iterator();
		Iterator<T> ier2 = nodes.iterator();
		while(ier1.hasNext()){
			T A = ier1.next();
			while(ier2.hasNext()){
				T B = ier2.next();
				if(A.equals(B)){
					continue;
				}else{
				
					//sum+=distHandler.UlamBasedcomputeEditDistanceFromTwoNodes(A1, B1);
					float dist=mat.get((Integer)A,(Integer)B);
					if(dist<=0){
						log.warn("distance <=0");
						continue;
					}else{
						
					//sum+=dist;
					//count++;
						vec.add((double)dist);	
						count++;
					}
				}
			}
			
		}
		if(count==0){
			return -1;
		}
		return  (float)this.math.percentile(vec, .5);
	}
	
	/**
	 * statistics
	 * @param testCaseStream 
	 */
	public void outStatistics(PrintWriter testCaseStream, Matrix mat){
		testCaseStream.println("Number of bin: "+this.cachedBinNodes.size());
		Iterator<Integer> ier111 = this.cachedBinNodes.keySet().iterator();
		testCaseStream.println("$: intra-Bin");
		while(ier111.hasNext()){
			Integer ind=ier111.next();
			testCaseStream.println(this.cachedBinNodes.get(ind).size()+", "+this.intraBin(ind,mat));			
		}
		
		Iterator<Integer> ier = this.cachedBinNodes.keySet().iterator();		
		ArrayList<Integer> list = new ArrayList<Integer>();
		while(ier.hasNext()){
			list.add(ier.next());
		}
		int listSize=list.size();
		testCaseStream.println("$: inter-Bin");	
		for(int i=0;i<listSize;i++){
			for(int j=i+1;j<listSize;j++){
				Integer A = list.get(i);
				Integer B = list.get(j);
				testCaseStream.println(A+", "+B+": "+this.interBin(A, B, mat));
			}
		}

		testCaseStream.println("$: outlier: "+this.specialBinning.size());	
		testCaseStream.flush();
	}
}
