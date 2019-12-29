package edu.NUDT.pdl.Nina.KNN;

import java.util.ArrayList;
import java.util.List;

import edu.NUDT.pdl.Nina.StableNC.lib.Coordinate;
import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class FarthestRequestMsg extends ObjMessage {
	static final long serialVersionUID = 4444444444L;
	public AddressIF from;
	public AddressIF OriginFrom;
	public AddressIF target;
	public ClosestRequestMsg CMsg = null;
	public SubSetClosestRequestMsg SCMsg = null;
	public List<AddressIF> HopRecords;

	public Coordinate targetCoordinates = null;

	public int totalSendingMessages = 0;

	/**
	 * multiple objective KNNs
	 */
	public ArrayList<AddressIF> targets = null;

	public void setTargetsForMultiObjKNN(ArrayList<AddressIF> _targets) {
		targets = new ArrayList<AddressIF>(2);
		targets.addAll(_targets);
	}

	public FarthestRequestMsg(AddressIF _from, AddressIF _target, AddressIF _OriginFrom, ClosestRequestMsg _CMsg) {
		from = _from;
		target = _target;
		OriginFrom = _OriginFrom;
		CMsg = ClosestRequestMsg.makeCopy(_CMsg);
		HopRecords = new ArrayList<AddressIF>(1);
		HopRecords.clear();
		SCMsg = null;
	}

	public FarthestRequestMsg(AddressIF _from, AddressIF _target, AddressIF _OriginFrom,
			SubSetClosestRequestMsg _SCMsg) {
		from = _from;
		target = _target;
		OriginFrom = _OriginFrom;
		SCMsg = _SCMsg;
		HopRecords = new ArrayList<AddressIF>(1);
		HopRecords.clear();
		CMsg = null;
	}

	/**
	 * add the hops
	 * 
	 * @param previousNodes
	 * @param curNode
	 */
	public void addCurrentHop(List<AddressIF> previousNodes, AddressIF curNode) {

		HopRecords.addAll(previousNodes);

		if (curNode != null) {
			HopRecords.add(curNode);
		}
	}

	public void clear() {

		this.CMsg = null;
		if (this.HopRecords != null && !this.HopRecords.isEmpty()) {
			this.HopRecords.clear();
		}
		this.SCMsg = null;
		if (this.targets != null && !this.targets.isEmpty()) {
			this.targets.clear();
		}

	}
}
