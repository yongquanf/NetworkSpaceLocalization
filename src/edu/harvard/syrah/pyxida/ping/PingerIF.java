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

import edu.harvard.syrah.sbon.async.CallbacksIF.CB0;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB1;
import edu.harvard.syrah.sbon.async.comm.AddressIF;

interface PingerIF extends Runnable {

	/**
	 * This creates a new thread
	 * 
	 */
	public void init(AddressIF defaultPingAddr, CB0 cbDone);

	public void ping(AddressIF remoteNode, CB1<Double> cbPing)
			throws UnsupportedOperationException;

	public void ping(AddressIF nodeA, AddressIF nodeB, CB1<Double> cbPing)
			throws UnsupportedOperationException;

	public void testPing(final Hashtable<Integer,Double> cachedPing, final int PINGCounter, final int PING_DELAY, final AddressIF remoteNode,final CB1<Hashtable> CBDone);

	public void shutdown();

}
