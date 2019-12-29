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
package edu.NUDT.pdl.Nina.StableNC.nc;

import edu.NUDT.pdl.Nina.StableNC.lib.Coordinate;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

/**
 * Requests the receiving node's current coordinate, confidence and last update
 * time.
 */

public class CoordRequestMsg extends ObjMessage {

	static final long serialVersionUID = 17L;
	public AddressIF from;
	public Coordinate myCoord;
	double RTT;
	/**
	 * Creates a CoordRequestMsg
	 * @param _RTT 
	 */
	public CoordRequestMsg(AddressIF _from, Coordinate _myCoord, double _RTT) {
		from = _from;
		myCoord = _myCoord;
		RTT=_RTT;
	}
}
