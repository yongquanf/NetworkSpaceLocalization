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
import java.util.LinkedList;
import java.util.List;

import edu.NUDT.pdl.Nina.Ninaloader;
import edu.harvard.syrah.prp.Log;
import edu.harvard.syrah.sbon.async.CBResult;
import edu.harvard.syrah.sbon.async.LoopIt;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB0;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB1;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB2;
import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;

public class PingManager {
	private static final Log log = new Log(PingManager.class);

	//private static final String DEFAULT_PING_HOSTNAME = "www.google.com";

	private AddressIF defaultPingAddr;

	private List<PingerIF> pingers = new LinkedList<PingerIF>();

	public PingManager() {
		if (Ninaloader.USE_ICMP) {
			pingers.add(new ICMPPinger());
		}else {
			pingers.add(new TCPSynPinger());
		}
	}

	public void init(final CB0 cbDone) {
		AddressFactory.createResolved(Ninaloader.DEFAULT_PING_HOSTNAME,
				new CB1<AddressIF>() {
					@Override
					protected void cb(CBResult result, AddressIF addr) {
						PingManager.this.defaultPingAddr = addr;

						new LoopIt<PingerIF>(pingers, new CB2<PingerIF, CB0>() {
							@Override
							protected void cb(CBResult result, PingerIF pinger,
									CB0 cbNextIter) {
								log.debug("Initialising pinger="
										+ pinger.getClass());
								pinger.init(defaultPingAddr, cbNextIter);
							}
						}).execute(cbDone);
					}
				});
	}

	public void printStats() {
		// TODO add any periodic stats here with log.info
	}

	public void addPingRequest(AddressIF remoteNode, CB1<Double> cbMeasurement) {

		/*
		 * TODO replace this with a more advanced pinger selection depending on
		 * the target and its ping history etc. For now, we're picking the first
		 * pinger from the list.
		 */

		PingerIF pinger = pingers.get(0);
		pinger.ping(remoteNode, cbMeasurement);
	}

	public void addPingRequest(AddressIF nodeA, AddressIF nodeB,
			CB1<Float> cbMeasurement) {
		// TODO implement me
		throw new UnsupportedOperationException();
	}

	/**
	 * add the ping request
	 * @param remoteNode
	 * @param packetsize
	 * @param numOfPackets
	 * @param interSendDelay
	 * @param cb1
	 */
	public void addPingRequest(AddressIF remoteNode, int packetsize,
			int PINGCounter, int PING_DELAY, final CB1<Hashtable>  CBDone) {
		// TODO Auto-generated method stub
		PingerIF pinger = pingers.get(0);	
		Hashtable<Integer,Double> cachedPing=new Hashtable<Integer,Double>(2);
		
		pinger.testPing(cachedPing, PINGCounter, PING_DELAY, remoteNode, new CB1<Hashtable>(){

			@Override
			protected void cb(CBResult result, Hashtable arg1) {
				// TODO Auto-generated method stub
			CBDone.call(result, arg1);	
			}						
		});
		
		
	}

}
