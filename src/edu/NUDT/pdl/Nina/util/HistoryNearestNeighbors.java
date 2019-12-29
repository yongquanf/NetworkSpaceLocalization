package edu.NUDT.pdl.Nina.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import edu.NUDT.pdl.Nina.Distance.NodesPairComp;
import edu.NUDT.pdl.Nina.KNN.AbstractNNSearchManager;
import edu.NUDT.pdl.Nina.KNN.NodesPair;
import edu.harvard.syrah.prp.Log;
import edu.harvard.syrah.prp.SortedList;
import edu.harvard.syrah.sbon.async.comm.AddressIF;


/**
 * 
 * keep the current best and worst neighbors
 * 
 * @author ericfu
 *
 * @param <T>
 */
public class HistoryNearestNeighbors<T> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7878965307247116017L;

	Log log=new Log(HistoryNearestNeighbors.class);
	
	int fixedSize;
	//cached elements
	private SortedList<NodesPair<T>> currentBestNearestNeighbors;
	private SortedList<NodesPair<T>> currentWorstNearestNeighbors;
	
	private Queue<T> rawsortedElements;
	
	final public T MyAddress;
	
	public HistoryNearestNeighbors(T _MyAddress, int _fixedSize){
		
		currentBestNearestNeighbors=new SortedList<NodesPair<T>>(new NodesPairComp());
		currentWorstNearestNeighbors=new SortedList<NodesPair<T>>(new NodesPairComp());
		
		rawsortedElements= new ConcurrentLinkedQueue<T>();
		fixedSize=_fixedSize;
		MyAddress= _MyAddress;
	}
	
	
	
	
	public synchronized SortedList<NodesPair<T>> getCurrentBestNearestNeighbors() {
		return currentBestNearestNeighbors;
	}



	public synchronized void setCurrentBestNearestNeighbors(
			SortedList<NodesPair<T>> currentBestNearestNeighbors) {
		this.currentBestNearestNeighbors = currentBestNearestNeighbors;
	}




	public synchronized SortedList<NodesPair<T>> getCurrentWorstNearestNeighbors() {
		return currentWorstNearestNeighbors;
	}




	public synchronized void setCurrentWorstNearestNeighbors(
			SortedList<NodesPair<T>> currentWorstNearestNeighbors) {
		this.currentWorstNearestNeighbors = currentWorstNearestNeighbors;
	}




	public synchronized Queue<T> getRawsortedElements() {
		return rawsortedElements;
	}




	public synchronized void setRawsortedElements(Queue<T> rawsortedElements) {
		this.rawsortedElements = rawsortedElements;
	}




	public synchronized SortedList<NodesPair<T>>  getBestKNN(){		
		return currentBestNearestNeighbors;
	}
	/**
	 * current size
	 * @return
	 */
	public  synchronized int getCurrentSize(){
		return  rawsortedElements.size();
	}
	
	/**
	 * add the elements
	 * @param lat
	 */
	public synchronized void addElement(final Collection<NodesPair> lat){
		if(lat==null||lat.isEmpty()){
			return;
		}else{
			
			Iterator<NodesPair> ier = lat.iterator();
			while(ier.hasNext()){
				NodesPair<T> tmp = (NodesPair<T>)ier.next();
				addElement(tmp);
			}
			

		}
		
		
	}
	
	public synchronized void addElement(final Set<NodesPair> elements){
		
		if(elements==null||elements.isEmpty()){
			return;
		}else{
					
			Iterator<NodesPair> ier = elements.iterator();
			while(ier.hasNext()){
				NodesPair<T> tmp = (NodesPair<T>)ier.next();
				addElement(tmp);
			}
			
		}
		
		
	}
	
	
	/**
	 * add an element
	 * @param element
	 */
	public synchronized void addElement(NodesPair<T> element){
				
		
		if(element==null||element.rtt<0||element.rtt>AbstractNNSearchManager.OUTRAGEOUSLY_LARGE_RTT){
			log.warn("add operation is failed");
			return;
		}
		T node =element.startNode;
		if(node==null||node.equals(MyAddress)){
			log.warn("add element is not allowed!");
			return;
		}
	/*	if(node==null){
			node=element.endNode;
		}*/
		
		if(rawsortedElements.contains(node)){
			
			double rtt2=Double.MIN_VALUE;
			//first remove
			boolean found=false;
			Iterator<NodesPair<T>> ier = currentBestNearestNeighbors.iterator();
			while(ier.hasNext()){
				NodesPair<T> tmp = ier.next();
				if(tmp.startNode.equals(node)){
					if(rtt2<tmp.rtt){
						rtt2=tmp.rtt;
					}
					ier.remove();
				}
			}	
			if(!AbstractNNSearchManager.startMeridian){
			if(rtt2>Double.MIN_VALUE){
				element.rtt=rtt2;
			}
			}
			//second add new records
			currentBestNearestNeighbors.add(element);
			//============================================	
			rtt2=Double.MIN_VALUE;
			ier=currentWorstNearestNeighbors.iterator();
			while(ier.hasNext()){
				NodesPair<T> tmp = ier.next();
				if(tmp.startNode.equals(node)){
					if(rtt2<tmp.rtt){
						rtt2=tmp.rtt;
					}
					ier.remove();
				}
			}	
			
			if(!AbstractNNSearchManager.startMeridian){
				if(rtt2>Double.MIN_VALUE){
					element.rtt=rtt2;
				}
			}
			currentWorstNearestNeighbors.add(element);
									
			if(getCurrentSize()>=fixedSize){
				MainGeneric.execMain.execute(new Runnable(){
					public void run() {
						maintain();
					}
				});
			}
			
			return;
		}else{
			//record the node
			rawsortedElements.offer(node);
			currentBestNearestNeighbors.add(element);
			currentWorstNearestNeighbors.add(element);
						
			if(getCurrentSize()>=fixedSize){
				
				MainGeneric.execMain.execute(new Runnable(){

					public void run() {
				maintain();
				
					}
				});
			}
		}

	}


	/**
	 * maintain the table
	 */
	private synchronized void maintain() {
		
		//remove the earlier records
		
		int toBeRemoved=rawsortedElements.size()-fixedSize;
		
		if(toBeRemoved<=0){
			return;
		}
		
		
		int count=0;
		//Iterator<T> ier = rawsortedElements.iterator();
		
		while(count<toBeRemoved){
			T node =  rawsortedElements.poll();
			if(node==null){
				break;
			}
			
			//remove the corresponding node
			Iterator<NodesPair<T>> ier1 = currentBestNearestNeighbors.iterator();
			while(ier1.hasNext()){
				NodesPair<T> rec1 = ier1.next();
				if((rec1.startNode!=null&&rec1.startNode.equals(node))){
					ier1.remove();
				}
			}
			//
			ier1 = currentWorstNearestNeighbors.iterator();
			while(ier1.hasNext()){
				NodesPair<T> rec1 = ier1.next();
				if((rec1.startNode!=null&&rec1.startNode.equals(node))){
					ier1.remove();
				}
			}	
			//remove from node list			
			count++;
		}
		
		
	}
	
private synchronized void maintain1() {
		
		//remove the earlier records				
			//remove the corresponding node
			while(currentBestNearestNeighbors.size()>this.fixedSize){
				NodesPair<T> elem = currentBestNearestNeighbors.get(this.currentBestNearestNeighbors.size()-1);
				this.rawsortedElements.remove(elem.startNode);
				currentBestNearestNeighbors.remove(currentBestNearestNeighbors.size()-1);
			}
			
			//
			while(currentWorstNearestNeighbors.size()>this.fixedSize){
				NodesPair<T> elem = this.currentWorstNearestNeighbors.get(0);
				this.rawsortedElements.remove(elem.startNode);
				this.currentWorstNearestNeighbors.remove(0);
			}
			
		
		
	}
	
	/**
	 * get the subset of the history nearestNeighbors
	 * the rawsortedElements is empty!
	 * @param K
	 * @return
	 */
	public synchronized HistoryNearestNeighbors<T> getHistoryNearestNeighbors(int K){
		HistoryNearestNeighbors<T> tmp=new HistoryNearestNeighbors<T>(MyAddress,K);
		
		
		int totalSize=rawsortedElements.size();
		if(totalSize!=this.currentBestNearestNeighbors.size()||totalSize!=this.currentWorstNearestNeighbors.size()){
			log.warn("Odd, totalSize!=this.currentBestNearestNeighbors.size()||totalSize!=this.currentWorstNearestNeighbors.size()");
		}
		int realSize=Math.min(totalSize, K);
		int trueSize=Math.min(realSize, this.currentBestNearestNeighbors.size());
		trueSize=Math.min(realSize, this.currentWorstNearestNeighbors.size());
		if(trueSize<=0){
			return null;
		}

		//empty
		if(trueSize==0){
			return null;
		}else{

		//raw is empty
		for(int i=0;i<trueSize;i++){
			tmp.currentBestNearestNeighbors.add(currentBestNearestNeighbors.get(i));			
		}
		for(int i=0;i<trueSize;i++){
			tmp.currentWorstNearestNeighbors.add(currentWorstNearestNeighbors.get(trueSize-i-1));		
		}
		}
		
		return tmp;
	}
	
	/**
	 * copy to the tmp data structure
	 * @param tmp
	 * @param K
	 */
	public synchronized void getHistoryNearestNeighbors(HistoryNearestNeighbors<T> tmp, int K){
		
		int totalSize=rawsortedElements.size();
		if(totalSize<=0||(this.currentBestNearestNeighbors.isEmpty())||(this.currentWorstNearestNeighbors.isEmpty())){
			log.warn("empty!");
			return;
		}
				
		if(totalSize!=this.currentBestNearestNeighbors.size()||totalSize!=this.currentWorstNearestNeighbors.size()){
			log.warn("Odd, totalSize!=this.currentBestNearestNeighbors.size()||totalSize!=this.currentWorstNearestNeighbors.size()");
		
		}
				
		int realSize=Math.min(totalSize, K);
		int trueSize=Math.min(realSize, this.currentBestNearestNeighbors.size());
		trueSize=Math.min(realSize, this.currentWorstNearestNeighbors.size());
		if(trueSize<=0){
			return;
		}

		//raw is empty
		for(int i=0;i<trueSize;i++){
			tmp.currentBestNearestNeighbors.add(currentBestNearestNeighbors.get(i));			
		}
		int worstSize=currentWorstNearestNeighbors.size();
		for(int i=0;i<trueSize;i++){
			tmp.currentWorstNearestNeighbors.add(currentWorstNearestNeighbors.get(worstSize-i-1));		
		}
		
	}
	
	/**
	 * add to sortedList For serialize
	 * @param best
	 * @param worst
	 * @param K
	 */
	public synchronized void getHistoryNearestNeighbors(SortedList<NodesPair<T>> best,SortedList<NodesPair<T>>  worst, int K){
		
		int totalSize=rawsortedElements.size();
		if(totalSize<=0||(this.currentBestNearestNeighbors.isEmpty())||(this.currentWorstNearestNeighbors.isEmpty())){
			
			return;
		}
		/*if(totalSize<AbstractNNSearchManager.cachedHistoryKNN){
			log.warn("not full to "+AbstractNNSearchManager.cachedHistoryKNN);
		}*/		
/*		if(totalSize!=this.currentBestNearestNeighbors.size()||totalSize!=this.currentWorstNearestNeighbors.size()){
			log.warn("Odd, totalSize!=this.currentBestNearestNeighbors.size()||totalSize!=this.currentWorstNearestNeighbors.size()");
		
		}*/
			
	/*	SortedList<NodesPair<T>> tmpBest = new SortedList<NodesPair<T>>(new NodesPairComp());
		SortedList<NodesPair<T>> tmpWorst = new SortedList<NodesPair<T>>(new NodesPairComp());
		tmpBest.addAll(currentBestNearestNeighbors);
		tmpWorst.addAll(currentWorstNearestNeighbors);
		
		int counter=0;
		ier=*/
		
		int trueSize=Math.min(K, this.currentBestNearestNeighbors.size());
		
		//raw is empty
		for(int i=0;i<trueSize;i++){
			if(MyAddress.equals(currentBestNearestNeighbors.get(i).startNode)){
				if(trueSize<currentBestNearestNeighbors.size()){
					trueSize++;
				}
				continue;
			}
			best.add(currentBestNearestNeighbors.get(i));			
		}
		int worstSize=currentWorstNearestNeighbors.size();
		trueSize=Math.min(K, worstSize);
		for(int i=0;i<trueSize;i++){
			if(MyAddress.equals(currentWorstNearestNeighbors.get(worstSize-i-1).startNode)){
				if(trueSize<worstSize-1){
					trueSize++;
				}
				continue;
			}
			
			worst.add(currentWorstNearestNeighbors.get(worstSize-i-1));		
		}
		
	}
	/**
	 * is empty
	 * @return
	 */
	public synchronized boolean isEmpty(){
		if(this.rawsortedElements.isEmpty()&&(this.currentBestNearestNeighbors.isEmpty()||this.currentWorstNearestNeighbors.isEmpty())){
			return true;
		}else{
			return false;
		}
		
	}

	public  synchronized void clear() {
		// TODO Auto-generated method stub
		this.rawsortedElements.clear();
		this.currentBestNearestNeighbors.clear();
		this.currentWorstNearestNeighbors.clear();
	}
	
}
