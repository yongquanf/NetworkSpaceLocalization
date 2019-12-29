package edu.NUDT.pdl.Nina.Clustering;

import java.io.Serializable;
import java.util.BitSet;

public class ClusteringSet implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1815362774538376376L;
	//=============================
	BitSet position;            //the vector that represents the clustering status
	int currentWorkableLength;  //the length of assigned vectors
	//=============================
	
	public ClusteringSet(){
		position=new BitSet();
		currentWorkableLength=-1;
	}
	
	/**
	 * find identical number of bit positions
	 * @param pos
	 * @return
	 */
	public int findCommonBits(ClusteringSet pos){
		BitSet you=pos.getPosition();
		int len=Math.min(currentWorkableLength, pos.currentWorkableLength);
		if(len<=0){
			return 0;
		}
		int commons=0;
		for(int i=0;i<len;i++){
			
			boolean common=(position.get(i)&you.get(i))|((!position.get(i))&(!you.get(i)));
			if(common){
				commons++;
			}
		}
		return commons;
	}
	
	public void setLayer(int pos, boolean value){
		position.set(pos, value);
		//update the layer, the layer can not be reduced
		if(pos>currentWorkableLength){
			currentWorkableLength=pos;
		}
		
	}
	public void removeLayer(int pos){
		if(pos>=currentWorkableLength){
			currentWorkableLength=pos-1;
		}
	}
	
	public int getLayer(){
		return currentWorkableLength;
	}
	
	public BitSet getPosition() {
		return position;
	}

	public void setPosition(BitSet position) {
		this.position = position;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		StringBuffer buf=new StringBuffer();
		buf.append("dim="+currentWorkableLength+"\n");
		for(int i=0;i<currentWorkableLength;i++){
			buf.append(position.get(i)+", ");	
		}
		buf.append("\n");
		return buf.toString();
	}
	
	
}
