package edu.NUDT.pdl.Nina.KNN;

import java.util.ArrayList;

import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class AnswerUpdateRequestMsg extends ObjMessage {
	static final long serialVersionUID = 40L;
	public AddressIF from;
	public AddressIF target;

	/**
	 * multiple objective KNNs
	 */
	public ArrayList<AddressIF> targets = null;
	final long timerCode;

	public void setTargetsForMultiObjKNN(ArrayList<AddressIF> _targets) {
		targets = new ArrayList<AddressIF>(2);
		targets.addAll(_targets);
	}

	AnswerUpdateRequestMsg(long _timerCode, AddressIF _from, AddressIF _target) {
		timerCode = _timerCode;
		from = _from;
		target = _target;

	}

	AnswerUpdateRequestMsg(long _timerCode, AddressIF _from, ArrayList<AddressIF> _targets) {
		timerCode = _timerCode;
		from = _from;
		setTargetsForMultiObjKNN(_targets);

	}

	public void clear() {
		// TODO Auto-generated method stub
		if (targets != null && !targets.isEmpty()) {
			this.targets.clear();
		}
		from = null;
		target = null;
	}
}
