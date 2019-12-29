package edu.NUDT.pdl.Nina.KNN;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RelativeCoordinate<T> implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1272194284725414333L;

	T nodeID;
	int dim; // dimension
	public List<NodesPair> coords;

	public RelativeCoordinate(T address, int dim) {
		this.dim = dim;
		this.nodeID = address;
		coords = new ArrayList<NodesPair>(2);
	}

	public void setCoordinate(List<NodesPair> coord) {
		this.coords.clear();
		this.coords.addAll(coord);
	}

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("dim: " + dim + "\n");
		Iterator<NodesPair> ier = coords.iterator();
		while (ier.hasNext()) {
			buf.append(" " + ier.next().toString() + ", ");
		}
		buf.append("\n");
		return buf.toString();
	}

	public void clear() {
		if (this.coords != null && !this.coords.isEmpty()) {
			this.coords.clear();
		}

	}

}
