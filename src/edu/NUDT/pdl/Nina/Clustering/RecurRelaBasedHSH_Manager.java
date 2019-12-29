package edu.NUDT.pdl.Nina.Clustering;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.NUDT.pdl.Nina.Clustering.ClusteringSet;
import edu.NUDT.pdl.Nina.Clustering.HSH_Manager;
import edu.NUDT.pdl.Nina.Clustering.Matrix_4_HSH;
import edu.NUDT.pdl.Nina.Clustering.clusteringRecord;
import edu.NUDT.pdl.Nina.Clustering.joinRecords;
import edu.NUDT.pdl.Nina.KNN.ConcentricRing;
import edu.NUDT.pdl.Nina.KNN.AbstractNNSearchManager;
import edu.NUDT.pdl.Nina.KNN.RingSet;
import edu.NUDT.pdl.Nina.StableNC.nc.StableManager;
import edu.harvard.syrah.prp.Log;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjCommIF;

public class RecurRelaBasedHSH_Manager<T> extends HSH_Manager<T>{
	private Log log=new Log(RecurRelaBasedHSH_Manager.class);
	
	public static int defaultLandmarks=7;
	public static int clusteringThresholds=5;
	public static double clusteringValidityThreshold=0.5; //only >0.5 nodes have distinct clustering validity
	public static double clusteringSetValidityThreshold=1.5; //the ratio between the clustering possibility between two dimensions

	
	
	
	public RecurRelaBasedHSH_Manager() {
		setClusCache(new HashMap<T,clusteringRecord<T>>(5));
		SimDist=null;
		AllNodes=new ArrayList<T>(10);
		BeaconNodes=new ArrayList<T>(5); 
		refCoordinates=null;
	}
			
	/**
	 * main entry
	 * @param currentNode
	 */
	public void RelativeCoordBasedClustering(T currentNode){
		
		Matrix_4_HSH mat=getRelativeCoordinates(currentNode);
		doInitialClustering(currentNode, mat);
	}
	/**
	 * do relative coordinate based clustering for one layer
	 * @param currentNode
	 * @return
	 */
	public  Matrix_4_HSH getRelativeCoordinates(T currentNode){
		clusteringRecord<T> curClust= getClusCache().get(currentNode);
		//landmarks
		Set<T> all=new HashSet<T>(5);
		int N=curClust.refMeridian.g_rings.CurrentNodes;
		curClust.refMeridian.g_rings.getRandomNodes(all, N);
		AllNodes.addAll(all);
		all.clear();
		Set<T> randNodes=new HashSet<T>(5);
		curClust.refMeridian.g_rings.getRandomNodes(randNodes,clusteringThresholds);
		if(randNodes.size()<clusteringThresholds){
			return null;
		}	
		BeaconNodes.addAll(randNodes);
		if(sim){
			
			int R= BeaconNodes.size();
		//	log.info("N "+N+"R "+R);
			
			RelativeCoords=new double[N][R];
			for(int i=0;i<N;i++){
				for(int j=0;j<R;j++){
					T from=AllNodes.get(i);
					T to=BeaconNodes.get(j);
					RelativeCoords[i][j]=GetDitance(from,to);
				}
			}
			//
			Matrix_4_HSH m=new Matrix_4_HSH(N,R);
			m.CopyMatrix(RelativeCoords);
			return m;
			
		}else{
			return null;
		}

	}
	
	
	
	/**
	 * do relative coordinate based clustering for one layer
	 * @param currentNode
	 * @return
	 */
	public  Matrix_4_HSH getRelativeCoordinates(T currentNode,Collection<T> allNodes, Collection<T> landmarks, double[] lat){
		clusteringRecord<T> curClust= getClusCache().get(currentNode);
		//landmarks
		
		if(allNodes==null||allNodes.isEmpty()||landmarks==null||landmarks.isEmpty()){
			log.warn("empty data structure");
			return null;
		}
		int N=allNodes.size();
		if(N<clusteringThresholds){
			log.warn("too few hosts");
			return null;
		}
		
		if(!AllNodes.isEmpty()){
			AllNodes.clear();
		}
		if(!BeaconNodes.isEmpty()){
			BeaconNodes.clear();
		}
		
		
		//curClust.refMeridian.g_rings.getRandomNodes(all, N);
		 AllNodes.addAll( allNodes);					
		 BeaconNodes.addAll(landmarks);
         		
		 
			int R= BeaconNodes.size();
		//	log.info("N "+N+"R "+R);
			
			RelativeCoords=new double[N][R];
			if(N*R!=lat.length){
				log.warn("the matrix does not match");
				return null;
			}
			
			//System.out.print("\n###############################\n");
			for(int i=0;i<N;i++){
				for(int j=0;j<R;j++){
					//T from=AllNodes.get(i);
					//T to=BeaconNodes.get(j);					
					//RelativeCoords[i][j]=lat[i*R+AllNodes.indexOf(to)];
					RelativeCoords[i][j]=lat[i*R+j];
					//System.out.print(RelativeCoords[i][j]+", ");
				}
				//System.out.print("\n");
			}
			//System.out.print("\n###############################\n");
			reSize();
			
			//
			Matrix_4_HSH m=new Matrix_4_HSH(AllNodes.size(),BeaconNodes.size());
			m.CopyMatrix(RelativeCoords);
			return m;
					
	}
	/**
	 * find zero items, and remove the corresponding columns when too many zeros are met
	 */
	private void reSize() {
		// TODO Auto-generated method stub
		if(RelativeCoords==null){
			return;
		}
		int N1=AllNodes.size();
		int R1=BeaconNodes.size();
		if(N1!=RelativeCoords.length||R1!=RelativeCoords[0].length){
			log.warn("in RelativeCoords, the length does not match! ");
			return;
		}
		double[][] cpyRC=new double[N1][R1];
		for(int i=0;i<N1;i++){
			for(int j=0;j<R1;j++){
				 cpyRC[i][j]=RelativeCoords[i][j];
			}
		}
		
		Map<T,Integer> map=new HashMap<T,Integer>(5); 
		
		double threshold=0.4;
		int zero=0;	
		for(int j=0;j<R1;j++){
			zero=0;
		//================================	
			for(int i=0;i<N1;i++){	
					if(Math.abs(RelativeCoords[i][j])<0.000002){
						zero++;
					}			
		}
		//================================
			if((zero/(N1+0.0d))>threshold){
				map.put(BeaconNodes.get(j), Integer.valueOf(j));
			}
		}
		List<Integer> ind=new ArrayList<Integer>(5);
		for(int i=0;i<R1;i++){
			ind.add(Integer.valueOf(i));
		}
		
		Iterator<T> ier = map.keySet().iterator();
		while(ier.hasNext()){
			T tmp=ier.next();
			BeaconNodes.remove(tmp);
			ind.remove(map.get(tmp));
		}
		
		RelativeCoords=new double[N1][BeaconNodes.size()];
		for(int i=0;i<N1;i++){
			for(int j=0;j<BeaconNodes.size();j++){
				RelativeCoords[i][j]=cpyRC[i][ind.get(j).intValue()];
			}
		}
		map.clear();
		cpyRC=null;
		ind.clear();
	}

	/**
	 * get the network distance, 
	 * @param from
	 * @param to
	 * @return
	 */
	double GetDitance(T from,T to){
		//simulation model
		if(sim){
		return SimDist[((Integer)from).intValue()][((Integer)to).intValue()];
		}
		else{
			return -1;
		}
	}
	
	/**
	 * test the refCoordinates
	 * @param rc 
	 * @return
	 */
	public boolean hasTooManyZeros(double[][] rc){
		if(rc==null||rc.length==0||rc[0].length==0){
			return true;
		}
		int zeroItems=0;
		int row=rc.length;
		int column=rc[0].length;
		
		for(int i=0;i<row;i++){
			for(int j=0;j<column;j++){
				if(Math.abs(rc[i][j])<0.0000002){
					zeroItems++;
				}
			}
		}
		
		double threshold=0.3;
		double total=row*column+0.0;
		if((zeroItems/total)>=threshold){
			return true;
		}else{
			return false;
		}
		
	}
	/**
	 * 
	 * @param currentNode
	 * @param dist
	 */
	public void doInitialClustering(T currentNode, Matrix_4_HSH dist){
		
		if(RelativeCoords==null||dist==null){
			return;
		}
		int r=2;
		int row=dist.mat_row;
		int column=dist.mat_column;
		//log.info("$ row "+row+" column "+column);
		
		refCoordinates=new Matrix_4_HSH (row,column);
		refCoordinates.CopyMatrix(dist.mat);
		if(hasTooManyZeros(refCoordinates.mat)){
			log.warn("Has too many zeros in refCoordinates");
			return;
		}
		
		 double[][] _H=new double[row][r];
		 double[][] _S=new double[column][r];
		
		
		Matrix_4_HSH _F=new Matrix_4_HSH(row,r);
		Matrix_4_HSH _G=new Matrix_4_HSH(column,r);
		
		Matrix_4_HSH m=new Matrix_4_HSH(row,column);
		
		m.FG_NMF(row, column, r, dist.mat, _H, _S);
		
		for(int i=0;i<row;i++){
			
			for(int j=0;j<r;j++){
				_F.mat[i][j]=_H[i][j];	
			}
		}
		for(int i=0;i<column;i++){
			for( int j=0;j<r;j++){
				_G.mat[i][j]=_S[i][j];
			}
		}
		//===============================
		List<T> ASubset=new ArrayList<T>(5);
		List<T> BSubset=new ArrayList<T>(5);
		//F acts as the clustering indicator matrix
		int pos=0;
		boolean[] bits=m.transformed2BinaryVectors(_F.mat);
		log.info("Bit vector: "+_F.mat_row);
		for(int i=0;i<bits.length;i++){
			if(sim){
			getClusCache().get(AllNodes.get(i)).myClustering.setLayer(pos, bits[i]);
			}else{
			//	log.info("@ "+i+" is: "+bits[i]);
				Map<T, ClusteringSet> tmp = getClusCache().get(currentNode).refMeridian.g_rings.getClusteringVec();
				if(tmp==null){
					log.warn("empty clustering sets in rings!");
					break;
				}
				ClusteringSet t2 = tmp.get(AllNodes.get(i));
				if(t2==null){
					log.warn("the clustering set has been removed!");
					continue;
				}else{
					t2.setLayer(pos, bits[i]);
				}
			}
			if(bits[i]){
				ASubset.add(AllNodes.get(i));				
			}else{
				BSubset.add(AllNodes.get(i));	
			}
		}
		
		//System.out.println("ASubset: "+ASubset.size()+" \tBSubset,"+BSubset.size());
		
		pos++;
		if(ASubset.size()>clusteringThresholds){
		continueCluster4Subsets(currentNode,ASubset,ASubset.size() ,column,r,pos);
		}
		if(BSubset.size()> clusteringThresholds){
		continueCluster4Subsets(currentNode,BSubset, BSubset.size() ,column,r,pos);
		}
		
		//=================================================================
		
		
		
		if(sim){
		Iterator<clusteringRecord<T>> ier;
		ier= getClusCache().values().iterator();
		while(ier.hasNext()){
			clusteringRecord<T> tmp = ier.next();
			log.info("\n clustering vector  "+tmp.myClustering.toString());
			
		}
		}else{
			 Iterator<ClusteringSet> ier = getClusCache().get(currentNode).refMeridian.g_rings.getClusteringVec().values().iterator();
			/*while(ier.hasNext()){
				log.info("\n clustering vector  "+ier.next().toString());
			}*/
			
		}
		
		
		
		
	}
	
	/**
	 * recursively do clustering on each subset of nodes
	 * @param subsets
	 * @param row
	 * @param column
	 * @param r
	 * @param pos
	 */
	public void continueCluster4Subsets(T currentNode, List<T> subsets, int row, int column, int r, int pos){
		
		//return condition
		if(row<clusteringThresholds||column<clusteringThresholds){
			return;
		}
		
		double[][] _H=new double[row][r];
		 double[][] _S=new double[column][r];
		
		
		Matrix_4_HSH _F=new Matrix_4_HSH(row,r);
		Matrix_4_HSH _G=new Matrix_4_HSH(column,r);
		
		//System.out.print("\n==============================\n");
		
		if(refCoordinates==null||refCoordinates.mat==null){
			System.err.println("error in mat");
			return;
		}
		
		//System.out.println("refRow: "+refCoordinates.mat_row+" refColumn: "+refCoordinates.mat_column);
		double[][]RC=new double[row][column];
		for(int i=0;i<row;i++){	
			for(int j=0;j<column;j++){						
				RC[i][j]=0;				
			}
		}

	//log.info("\n==============================\n");		
		for(int i=0;i<row;i++){
			
			int index=AllNodes.indexOf(subsets.get(i));						
			for(int j=0;j<column;j++){
							
				RC[i][j]= refCoordinates.mat[index][j];	
			//	System.out.print("(i,j)="+RC[i][j]+", index "+index);					
			}
		//	System.out.print("\n");
		}
		//log.info("\n==============================\n");
		if(hasTooManyZeros(RC)){
			log.warn("Has too many zeros in refCoordinates");
			return;
		}
		
		Matrix_4_HSH m=new Matrix_4_HSH(row,column);
		//log.info("row "+row+"column "+column);         
		boolean result=m.FG_NMF(row, column, r, RC, _H, _S);
		if(!result){
			return;
		}
		for(int i=0;i<row;i++){
			
			for(int j=0;j<r;j++){
				_F.mat[i][j]=_H[i][j];	
			}
		}
		
		boolean[] bits=m.transformed2BinaryVectors(_F.mat);
		
		for(int i=0;i<row;i++){
			
			for(int j=0;j<r;j++){
				_F.mat[i][j]=_H[i][j];	
			}
		}
		for(int i=0;i<column;i++){
			for( int j=0;j<r;j++){
				_G.mat[i][j]=_S[i][j];
			}
		}
		//can not do clustering
		if(!hasClusterValidity(_F,row,r)){
			return;
		}
		
		
		//===============================
		List<T> ASubset=new ArrayList<T>(5);
		List<T> BSubset=new ArrayList<T>(5);
		//F acts as the clustering indicator matrix
	
		for(int i=0;i<bits.length;i++){
			if(sim){
			getClusCache().get(subsets.get(i)).myClustering.setLayer(pos, bits[i]);
			}else{
				Map<T, ClusteringSet> VC = getClusCache().get(currentNode).refMeridian.g_rings.getClusteringVec();
				if(VC==null){
					log.warn("empty clustering set in subset!");
					break;
				}
				ClusteringSet item = VC.get(subsets.get(i));
				if(item==null){
					log.warn("item has been removed @: "+subsets.get(i));
					continue;
				}else{
				item.setLayer(pos, bits[i]);
				}
			}
			if(bits[i]){
				ASubset.add(subsets.get(i));				
			}else{
				BSubset.add(subsets.get(i));	
			}
		}
	//	log.info("ASubset: "+ASubset.size()+" \tBSubset,"+BSubset.size());
		
		pos++;		
		if(ASubset.size()<=1||BSubset.size()<=1){
			return;
		}
		if(ASubset.size()> clusteringThresholds){
		continueCluster4Subsets(currentNode,ASubset,ASubset.size() ,column,r,pos);
		}
		if(BSubset.size()> clusteringThresholds){
		continueCluster4Subsets(currentNode,BSubset, BSubset.size() ,column,r,pos);	
		}
	}
	
	
	/**
	 * test the clustering validity of the clustering
	 * @param _F
	 * @param row
	 * @param r
	 * @return
	 */
	public boolean hasClusterValidity(Matrix_4_HSH _F,int row,int r){
		double count=0.0;
		//two cluster only
		if(r!=2){
			assert(false);
		}
		for(int i=0;i<row;i++){
			double v=-1;
			if(_F.mat[i][0]!=0&&_F.mat[i][1]!=0){
			v=Math.max(Math.abs(_F.mat[i][0]/_F.mat[i][1]),Math.abs(_F.mat[i][1]/ _F.mat[i][0]));
			}else{
				v=2;
			}
			if(v>=clusteringSetValidityThreshold){
				count++;
			}
		}
	
		
		if((count/row)<clusteringValidityThreshold){
			return false;
		}else{
			return true;
		}
		
	}

	
	
	
/*	@Override
	public void run() {
		// TODO Auto-generated method stub
		init(false);
	}*/
	

}
