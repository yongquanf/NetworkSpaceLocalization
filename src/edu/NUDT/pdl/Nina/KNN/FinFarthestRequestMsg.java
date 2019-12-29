package edu.NUDT.pdl.Nina.KNN;

import java.util.ArrayList;
import java.util.List;

import edu.NUDT.pdl.Nina.StableNC.lib.Coordinate;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class FinFarthestRequestMsg extends ObjMessage {
	static final long serialVersionUID = 44000000L;
	public AddressIF from;
	public AddressIF OriginFrom;
	public AddressIF target;
	public ClosestRequestMsg CMsg = null;
	public SubSetClosestRequestMsg SCMsg = null;
	public List<AddressIF> HopRecords;

	public Coordinate targetCoordinates = null;

	/**
	 * record the total sending messages
	 */
	public int totalSendingMessages = 0;

	public void increaseMessage(int count) {
		totalSendingMessages += count;
	}

	/**
	 * multiple objective KNNs
	 */
	public ArrayList<AddressIF> targets = null;

	public void setTargetsForMultiObjKNN(ArrayList<AddressIF> _targets) {
		targets = new ArrayList<AddressIF>(2);
		targets.addAll(_targets);
	}

	public FinFarthestRequestMsg(AddressIF _from, AddressIF _target, AddressIF _OriginFrom, ClosestRequestMsg _CMsg) {
		from = _from;
		target = _target;
		OriginFrom = _OriginFrom;
		CMsg = _CMsg;
		HopRecords = new ArrayList<AddressIF>(1);
		SCMsg = null;
	}

	public FinFarthestRequestMsg(AddressIF _from, AddressIF _target, AddressIF _OriginFrom,
			SubSetClosestRequestMsg _SCMsg) {
		from = _from;
		target = _target;
		OriginFrom = _OriginFrom;
		SCMsg = _SCMsg;
		HopRecords = new ArrayList<AddressIF>(1);
		CMsg = null;
	}

	public void addCurrentHop(AddressIF curNode) {
		if (curNode != null) {
			HopRecords.add(curNode);
		}
	}

	/**
	 * 
	 * @param msg
	 * @return
	 */
	public static FinFarthestRequestMsg makeCopy(FarthestRequestMsg msg) {

		if (msg.CMsg == null) {

			FinFarthestRequestMsg tmp = new FinFarthestRequestMsg(msg.from, msg.target, msg.OriginFrom, msg.SCMsg);
			if (msg.targets != null && !msg.targets.isEmpty()) {
				tmp.setTargetsForMultiObjKNN(msg.targets);
			}

			return tmp;
		} else {
			FinFarthestRequestMsg tmp = new FinFarthestRequestMsg(msg.from, msg.target, msg.OriginFrom, msg.CMsg);
			if (msg.targets != null && !msg.targets.isEmpty()) {
				tmp.setTargetsForMultiObjKNN(msg.targets);
			}

			return tmp;
		}
	}

	public void clear() {
		// TODO Auto-generated method stub
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
