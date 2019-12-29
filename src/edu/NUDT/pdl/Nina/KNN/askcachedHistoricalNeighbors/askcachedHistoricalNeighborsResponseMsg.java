package edu.NUDT.pdl.Nina.KNN.askcachedHistoricalNeighbors;

import java.util.Collection;

import edu.NUDT.pdl.Nina.Distance.NodesPairComp;
import edu.NUDT.pdl.Nina.KNN.NodesPair;
import edu.NUDT.pdl.Nina.util.HistoryNearestNeighbors;
import edu.harvard.syrah.prp.SortedList;
import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class askcachedHistoricalNeighborsResponseMsg extends ObjMessage {
	static final long serialVersionUID = 3535353353535L;

	public AddressIF from; // me
	public int K;
	public SortedList<NodesPair<AddressIF>> currentBestNearestNeighbors;
	public SortedList<NodesPair<AddressIF>> currentWorstNearestNeighbors;

	public askcachedHistoricalNeighborsResponseMsg(AddressIF _from, int _K) {
		from = _from;
		K = _K;

		currentBestNearestNeighbors = new SortedList<NodesPair<AddressIF>>(new NodesPairComp());
		currentWorstNearestNeighbors = new SortedList<NodesPair<AddressIF>>(new NodesPairComp());
	}

	/**
	 * add the results
	 * 
	 * @param forbidden
	 * @param lat
	 */
	public void setcachedHistoricalNeighbors(HistoryNearestNeighbors<AddressIF> _cachedKNNs) {
		_cachedKNNs.getHistoryNearestNeighbors(currentBestNearestNeighbors, currentWorstNearestNeighbors, K);
	}

	public void clear() {
		// TODO Auto-generated method stub
		this.currentBestNearestNeighbors.clear();
		this.currentWorstNearestNeighbors.clear();
	}
}
