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
package edu.NUDT.pdl.pyxida.ping;

import edu.harvard.syrah.prp.Log;
import edu.harvard.syrah.sbon.async.CBResult;
import edu.harvard.syrah.sbon.async.EL;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB0;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB1;
import edu.harvard.syrah.sbon.async.comm.AddressIF;

abstract class Pinger implements PingerIF {
	private static final Log log = new Log(Pinger.class);

	private static final long PING_DELAY = 1000;

	protected AddressIF localAddr;
	protected AddressIF defaultPingAddr;

	// protected Thread thread;

	protected boolean shutdown = false;

	class PingData {
		AddressIF pingAddr;
		double lat;
	}

	public void testPing(final AddressIF remoteNode) {
		System.out.print("  Sending ping... ");
		ping(remoteNode, new CB1<Double>() {
			@Override
			protected void cb(CBResult result, Double lat) {
				System.out.println(lat == 0 ? "Timeout" : "lat="
						+ (lat > 0 ? lat : "Neg"));
				EL.get().registerTimerCB(PING_DELAY, new CB0() {
					@Override
					protected void cb(CBResult result) {
						testPing(remoteNode);
					}
				});
			}
		});
	}

}
