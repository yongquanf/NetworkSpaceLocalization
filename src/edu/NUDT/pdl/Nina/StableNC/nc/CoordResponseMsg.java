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
 * Response to GossipRequestMsg. Responds with the receiving node's current
 * coordinate, confidence and last update time.
 */
public class CoordResponseMsg extends ObjMessage {

	static final long serialVersionUID = 17L;

	final Coordinate coord;
	final double error;
	final long age;
	final double currentRTT;
	public AddressIF from;

	public CoordResponseMsg(AddressIF _from, Coordinate _remoteCoordinate,
			double _remoteError, long _remoteAge, double _currentRTT) {
		coord = _remoteCoordinate;
		error = _remoteError;
		age = _remoteAge;
		currentRTT = _currentRTT;
		from = _from;

	}
}
