package edu.NUDT.pdl.Nina.net.lossMeasure;

import java.util.Hashtable;
import java.util.Iterator;

import edu.harvard.syrah.pyxida.ping.PingManager;
import edu.harvard.syrah.sbon.async.CBResult;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB0;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB1;
import edu.harvard.syrah.sbon.async.comm.AddressIF;

public class symmetricLoss{


	PingManager lossManager;
	
	public symmetricLoss(){
		lossManager=new PingManager();
	}
	
	public void init(final CB0 cbDone){
		lossManager.init(cbDone);
	}
	
	/**
	 * sending process,
	 * single end, no cooperation at the end
	 * @param remoteNode
	 * @param packetsize
	 * @param NumOfPackets
	 * @param interSendDelay
	 * @param cbPing
	 */
	public void ping(final AddressIF remoteNode,int packetsize,
			int PINGCounter, int PING_DELAY, final CB1<Double> cbPing){
		
		final int total=PINGCounter;
								
		lossManager.addPingRequest(remoteNode, packetsize,PINGCounter, PING_DELAY, new CB1<Hashtable>(){
			@Override
			protected void cb(CBResult result, Hashtable arg1) {
				switch(result.state){
				case OK:{
					// TODO Auto-generated method stub
					Iterator ier = arg1.keySet().iterator();
					int missing=0;
					int received=0;
					while(ier.hasNext()){
						
						Integer key = (Integer)ier.next();
						Double lat=(Double)arg1.get(key);
						if(lat<0){
							missing++;
						}else{
							received++;
						}
						
					}
					//=========================================
					cbPing.call(result, Double.valueOf(missing/(received+0.0)));
					break;
				}
				default:{
				
					 cbPing.call(result, Double.valueOf(-1));	
					 break;
				}								
				}

				
			}
			
		});		
	}
	
		
	
}
