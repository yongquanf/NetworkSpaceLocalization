package edu.NUDT.pdl.Nina;

import java.util.HashSet;
import java.util.Set;

import edu.NUDT.pdl.Nina.KNN.DatRequestMsg;
import edu.NUDT.pdl.Nina.KNN.DatResponseMsg;
import edu.NUDT.pdl.Nina.KNN.NodesPair;
import edu.NUDT.pdl.Nina.KNNLogServer.ReportCoordMsgHandler;
import edu.NUDT.pdl.Nina.log.ReportCoordReqMsg;
import edu.harvard.syrah.prp.Log;
import edu.harvard.syrah.sbon.async.CBResult;
import edu.harvard.syrah.sbon.async.CBResult.CBState;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB0;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB1;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB2;
import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjComm;
import edu.harvard.syrah.sbon.async.comm.obj.ObjCommCB;
import edu.harvard.syrah.sbon.async.comm.obj.ObjCommIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjCommRRCB;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessageIF;

public class MeasureComm {
	
	private Log log=new Log(MeasureComm.class);
	public ObjCommIF comm=null;
	
	
	public MeasureComm(){
		comm = new ObjComm();
					
	}
	
	public void init(AddressIF objCommAddr, final CB0 cbDone) {
		
		cbDone.call(CBResult.OK());
		// Initiliase the ObjComm communication module
/*		comm.initServer(objCommAddr, new CB0() {
			
			protected void cb(CBResult result) {
				if (result.state == CBState.OK) {
					//register the communication channel					
				}
				cbDone.call(result);
			}
		});*/
		
	}
	

	
	
	
	/**
	 * 
	 * @return
	 */
	public ObjCommIF getComm(){
		return comm;
	}
}
