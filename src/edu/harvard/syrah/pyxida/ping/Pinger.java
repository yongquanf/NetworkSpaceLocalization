/*
 * Copyright 2008 Jonathan Ledlie and Peter Pietzuch
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.harvard.syrah.pyxida.ping;

import java.util.Hashtable;

import edu.harvard.syrah.prp.Log;
import edu.harvard.syrah.sbon.async.CBResult;
import edu.harvard.syrah.sbon.async.EL;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB0;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB1;
import edu.harvard.syrah.sbon.async.comm.AddressIF;

abstract class Pinger implements PingerIF {
	private static final Log log = new Log(Pinger.class);

	//public long PING_DELAY = 1000;
	
	//public long PING_DELAY = 1000;
	
	protected AddressIF localAddr;
	protected AddressIF defaultPingAddr;

	// protected Thread thread;

	protected boolean shutdown = false;

	class PingData {
		AddressIF pingAddr;
		double lat;
	}

	/**
	 * ping the target
	 * @param cachedPing
	 * @param PINGCounter
	 * @param PING_DELAY
	 * @param remoteNode
	 * @param CBDone
	 */
	public void testPing(final Hashtable<Integer,Double> cachedPing, final int PINGCounter, final int PING_DELAY, final AddressIF remoteNode,final CB1<Hashtable> CBDone) {
		System.out.print("  Sending ping... ");
		if(cachedPing==null){
			log.warn("empty cachedPing");
			
			return;
		}
		if(PINGCounter==0){
			//TODO: caback
			CBDone.call(CBResult.OK(), cachedPing);
		}else{
		
			ping(remoteNode, new CB1<Double>() {
			@Override
			protected void cb(CBResult result, Double lat) {
				System.out.println(lat == 0 ? "Timeout" : "lat="
						+ (lat > 0 ? lat : "Neg"));
				
				cachedPing.put(Integer.valueOf(PINGCounter), lat);
				
				EL.get().registerTimerCB(PING_DELAY, new CB0() {
					@Override
					protected void cb(CBResult result) {
						testPing(cachedPing,PINGCounter-1,PING_DELAY,remoteNode,CBDone);
					}
				});
			}
		});
		
		}
	}

}
