package edu.NUDT.pdl.Nina.Distance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import edu.NUDT.pdl.Nina.KNN.NodesPair;
import edu.NUDT.pdl.Nina.util.Matrix;
import edu.harvard.syrah.prp.Log;
import edu.harvard.syrah.prp.SortedList;

public class DistanceHandler<T> {

	Log log=new Log(DistanceHandler.class);
	/**
	 * map the landmark to a char, A-Z
	 */
	
	Hashtable<T,Character> mappedLandmark=null;
	Random rand;
	String Alphabet="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	int totalSize;
	public DistanceHandler(){
		mappedLandmark=new Hashtable<T,Character>(5);
		rand=new Random(System.currentTimeMillis());
		 totalSize=Alphabet.length();
	}
	
	/**
	 * add a map pair incrementally
	 * @param landmark
	 * @return
	 */
	public boolean addMap(T landmark){
		if(mappedLandmark!=null){
			if(mappedLandmark.containsKey(landmark)){
				return false;
			}
			//else
			int curLen = mappedLandmark.size();
			if( curLen >= totalSize){
				return false;
			}else{
				char name = this.Alphabet.charAt(curLen);
				//log.debug(landmark+", "+name);
				mappedLandmark.put(landmark, Character.valueOf(name));
				return true;
			}
			
		}
		return false;
	}
	
	/**
	 * from the sorted landmarks, a mapped string is generated
	 * @return
	 */
	public String getMap(List<T> landmarks){
		if(landmarks!=null){
			StringBuffer buf=new StringBuffer();
			Iterator<T> ier = landmarks.iterator();
			while(ier.hasNext()){
				T tmp = ier.next();
				buf.append(this.mappedLandmark.get(tmp).charValue());				
			}
			
			return buf.toString();
			
		}else{
		return null;
		}
		}
	
	
	/**
	 * get the edit distance for two sorted pair
	 * @param ALandmarks
	 * @param BLandmarks
	 * @return
	 */
	public int getEditDistance(List<T> ALandmarks, List<T> BLandmarks){
		String A=this.getMap(ALandmarks);
		String B=this.getMap(BLandmarks);
		log.debug(A);
		log.debug(B);
		if(A!=null&&B!=null){
			return EditDistance.getLevenshteinDistance(A, B);
		}else{
			return -1;
		}
	}
	
	/**
	 * generate the sortedlandmarks, from the list of <from, landmark, rtt>
	 * @param distanceMeasurements, 
	 * @return
	 */
	public List<T>  generateSortedLandmarks(List<NodesPair> distanceMeasurements){
		
		SortedList<NodesPair<T>> sortedL = new SortedList<NodesPair<T>>(
				new NodesPairComp());
		Iterator<NodesPair> ier = distanceMeasurements.iterator();
		while(ier.hasNext()){
			sortedL.add(ier.next());			
		}
		//sorted
		List<T>  lists=new ArrayList<T>(sortedL.size());
		Iterator<NodesPair<T>> ier2 = sortedL.iterator();
		while(ier2.hasNext()){
			//landmark
			NodesPair<T> tmp = ier2.next();
			lists.add(tmp.endNode);
			log.debug(tmp.toString());
		}
		return lists;
	}
	
	/**
	 * remove landmarks that are only on one side, the resulting output forms an Ulam distance
	 */
	int UlamBasedcomputeEditDistanceFromTwoNodes(List<NodesPair> distanceMeasurementsA, List<NodesPair> distanceMeasurementsB){
		List LandmarksA=new ArrayList(2);
		List LandmarksB=new ArrayList(2);
		
		Iterator<NodesPair> ier1 = distanceMeasurementsA.iterator();
		Iterator<NodesPair> ier2 = distanceMeasurementsB.iterator();
		while(ier1.hasNext()){
			NodesPair A = ier1.next();
			LandmarksA.add(A.endNode);			
		}
		while(ier2.hasNext()){
			NodesPair B = ier2.next();
			LandmarksB.add(B.endNode);
		}
		
		List commonLandmarks=commonNodes(LandmarksA,LandmarksB);
		
		ier1 = distanceMeasurementsA.iterator();
		while(ier1.hasNext()){
			NodesPair A = ier1.next();
			if(!commonLandmarks.contains(A.endNode)){
				ier1.remove();
			}
		}
		ier2 = distanceMeasurementsB.iterator();
		while(ier2.hasNext()){
			NodesPair B = ier2.next();
			if(!commonLandmarks.contains(B.endNode)){
				ier2.remove();
			}			
		}
		LandmarksA.clear();
		LandmarksB.clear();
		//found common landmarks
		return computeEditDistanceFromTwoNodes(distanceMeasurementsA,distanceMeasurementsB );
	}
	
	List commonNodes(List LandmarksA,List LandmarksB){
		List commons=new ArrayList();
		if(LandmarksA!=null&&LandmarksB!=null){
			Iterator ierA = LandmarksA.iterator();
			while(ierA.hasNext()){
			Object tmp = ierA.next();	
			if(LandmarksB.contains(tmp)){
				commons.add(tmp);
			}
			}
		}
		return commons;
		
	}
	/**
	 * compute the edit distance from a set of samples to the landmarks
	 * assume: each node have same set of landmarks
	 * @param distanceMeasurementsA
	 * @param distanceMeasurementsB
	 * @return
	 */
	public int computeEditDistanceFromTwoNodes(List<NodesPair> distanceMeasurementsA, List<NodesPair> distanceMeasurementsB){
	
		List<T>  A=generateSortedLandmarks(distanceMeasurementsA);
		List<T>  B=generateSortedLandmarks(distanceMeasurementsB);

		return getEditDistance(A,B);

	}
	
	
	
	public static void main(String[ ]args){
		
		int[] landmarks={5, 7, 8,9,10};
		DistanceHandler<Integer> test=new DistanceHandler<Integer> ();
		for(int i=0;i<landmarks.length;i++){
			test.addMap(Integer.valueOf(landmarks[i]));			
		}
		//mapped
		NodesPair<Integer> s1=new NodesPair<Integer>(12,5,12.0);
		NodesPair<Integer> s2=new NodesPair<Integer>(12,7,23.0);
		NodesPair<Integer> s3=new NodesPair<Integer>(12,8,54.0);
		NodesPair<Integer> s4=new NodesPair<Integer>(12,9,32.0);
		NodesPair<Integer> s5=new NodesPair<Integer>(12,10,8.0);
		
		List<NodesPair> distanceMeasurementsA=new ArrayList<NodesPair>(10);
		distanceMeasurementsA.add(s1);
		distanceMeasurementsA.add(s2);
		distanceMeasurementsA.add(s3);
		distanceMeasurementsA.add(s4);
		distanceMeasurementsA.add(s5);
		
		NodesPair<Integer> as1=new NodesPair<Integer>(12,5,17.0);
		NodesPair<Integer> as2=new NodesPair<Integer>(12,7,8.0);
		NodesPair<Integer> as3=new NodesPair<Integer>(12,8,5.0);
		
		List<NodesPair> distanceMeasurementsB=new ArrayList<NodesPair>(10);
		 distanceMeasurementsB.add(as1);
		 distanceMeasurementsB.add(as2);
		 distanceMeasurementsB.add(as3);
		 
		 System.out.println("edit distance: "+test.computeEditDistanceFromTwoNodes(distanceMeasurementsA,distanceMeasurementsB));
		 System.out.println("Ulam distance: "+test.UlamBasedcomputeEditDistanceFromTwoNodes(distanceMeasurementsA, distanceMeasurementsB));
	}
}
