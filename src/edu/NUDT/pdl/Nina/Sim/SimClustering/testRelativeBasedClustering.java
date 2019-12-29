package edu.NUDT.pdl.Nina.Sim.SimClustering;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.NUDT.pdl.Nina.Clustering.ClusteringSet;
import edu.NUDT.pdl.Nina.Clustering.RecurRelaBasedHSH_Manager;
import edu.NUDT.pdl.Nina.Clustering.clusteringRecord;
import edu.NUDT.pdl.Nina.KNN.AbstractNNSearchManager;
import edu.NUDT.pdl.Nina.KNN.ConcentricRing;
import edu.harvard.syrah.prp.Log;

public class testRelativeBasedClustering {

	
	RecurRelaBasedHSH_Manager<Integer> instance=null;
	Map<Integer,Node> nodes=new HashMap<Integer,Node>(10);
	
	int  nodesPerRing=30;
	class Node{
		int id;
		ConcentricRing<Integer> meridian;
		Node(int _id, ConcentricRing<Integer> _meridian){
			id=_id;
			meridian=_meridian;
		}
	}
	int initialSize=100;
	int gossipRounds=100; //to stablize the meridian
	private Log log=new Log(testRelativeBasedClustering .class);
	
	public testRelativeBasedClustering(){
		instance.init(true);
	}
	
	
	/*
	 * format: 
	 *  row
	 *  column
	 *   line of latency matrix
	 */
	public void ReadLatFile(String file){
		
				if (file != null) {
					try {
						RandomAccessFile raf = new RandomAccessFile(file, "r");
						if (raf == null) {
							System.err.println("empty file");
							return ;
						} else {
							int row = 0;
							int column = 0;
							int line = -1;

							String curLine;
							curLine = raf.readLine();

							String[] s = curLine.split("[ \\s\t ]");
							row = Integer.parseInt(s[0]);
							curLine = raf.readLine();

							s = curLine.split("[ \\s\t ]");
							column=Integer.parseInt(s[0]);
							
							instance.SimDist = new double[row][column];
							for(int i=0;i<row;i++){
								for(int j=0;j<column;j++){
									instance.SimDist[i][j]=0;
								}
								nodes.put(Integer.valueOf(i), new Node(i, new ConcentricRing<Integer>(Integer.valueOf(i), nodesPerRing)));
							}
							while (true) {
								curLine = raf.readLine();
								line++;

								if (curLine == null)
									break;

								s = curLine.split("[ \\s\t ]");

								for (int j = 0; j < column; j++){
									instance.SimDist[line][j] = Double
											.parseDouble(s[j]);
									instance.SimDist[j][line]=instance.SimDist[line][j];
								}
							}
							raf.close();
							return;
						}
					} catch (Exception e) {
					}
				}
				System.err.println(" empty file");
				return ;
			
	}
	
	/**
	 * init
	 */
	public void constructNodes(){
		
		
		if(instance.SimDist==null||initialSize>instance.SimDist.length){
			return;
		}else{
			List<Integer> ids=new ArrayList<Integer>(10);
			for(int i=0;i<instance.SimDist.length;i++){
				ids.add(i);
			}
			int curID;
			for(int i=0;i<instance.SimDist.length;i++){
				curID=i;				
				Collections.shuffle(ids);
				for(int m=0;m<initialSize;m++){
					int you=ids.get(m);
					if(you==curID){
						continue;
					}
					double latency=instance.SimDist[curID][you];
					//log.info("from "+curID+", to "+you+" = "+latency);
					nodes.get(Integer.valueOf(curID)).meridian.processSample(Integer.valueOf(you), latency, true, 
							1, null,AbstractNNSearchManager.offsetLatency);
				}
				}
		}
		//==================================
		for(int i=0;i<instance.SimDist.length;i++){
			clusteringRecord<Integer> tmp=new clusteringRecord<Integer>(new ClusteringSet(),nodes.get(Integer.valueOf(i)).meridian);
			instance.getClusCache().put(Integer.valueOf(i), tmp);
		}
	}
	/**
	 * the test entry
	 */
	public void startClustering(){
		
		for(int i=0;i<instance.SimDist.length;i++){
		Integer currentNode=Integer.valueOf(i);	
		instance.RelativeCoordBasedClustering(currentNode);	
		if(i>0){
			break;
		}
		}
	}
	
	/**
	 * a gossip round
	 * @param from
	 * @param to
	 */
	public void gossip(int from,int to){
		
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		testRelativeBasedClustering test=new testRelativeBasedClustering ();
		test.ReadLatFile(args[0]);
		
		test.constructNodes();
		test.startClustering();
		
	}

}
