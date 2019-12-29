package edu.NUDT.pdl.Nina.Clustering;

import java.util.List;

public class joinRecords<T> {
	/*
	* ####################################################
	*   joinRecords<T>
	*####################################################
	*/
	//for recording nodes used for clustering 

		T bootNode;
		List<T> landmarks;
		int layer;
		
		public int getLayer() {
			return layer;
		}
		public void setLayer(int layer) {
			this.layer = layer;
		}
		public T getBootNode() {
			return bootNode;
		}
		public List<T> getLandmarks() {
			return landmarks;
		}
		/**
		 * @param bootNode
		 * @param landmarks
		 */
		public joinRecords(T bootNode, List<T> landmarks) {
			this.bootNode = bootNode;
			this.landmarks = landmarks;
		}
	
		
}
