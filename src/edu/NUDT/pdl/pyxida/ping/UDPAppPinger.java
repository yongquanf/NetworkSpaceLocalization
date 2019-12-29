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
import edu.harvard.syrah.sbon.async.CallbacksIF.CB0;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB1;
import edu.harvard.syrah.sbon.async.comm.AddressIF;

class UDPAppPinger extends Pinger implements PingerIF {
	private static final Log log = new Log(UDPAppPinger.class);

	public double ping(AddressIF remoteNode)
			throws UnsupportedOperationException {
		// TODO Auto-generated method stub
		return 0;
	}

	public double ping(AddressIF nodeA, AddressIF nodeB)
			throws UnsupportedOperationException {
		// TODO Auto-generated method stub
		return 0;
	}

	public void init(AddressIF defaultPingAddr) {
		// TODO Auto-generated method stub

	}

	public void shutdown() {
		// TODO Auto-generated method stub

	}

	public void run() {
		// TODO Auto-generated method stub

	}

	public void init(AddressIF defaultPingAddr, CB0 cbDone) {
		// TODO Auto-generated method stub

	}

	public void ping(AddressIF remoteNode, CB1<Double> cbPing)
			throws UnsupportedOperationException {
		// TODO Auto-generated method stub

	}

	public void ping(AddressIF nodeA, AddressIF nodeB, CB1<Double> cbPing)
			throws UnsupportedOperationException {
		// TODO Auto-generated method stub

	}

	@Override
	public void testPing(AddressIF remoteNode) {
		// TODO Auto-generated method stub

	}

}
