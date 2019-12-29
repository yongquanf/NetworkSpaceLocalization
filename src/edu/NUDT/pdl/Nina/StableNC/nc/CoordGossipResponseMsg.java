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

import java.util.Set;

import edu.NUDT.pdl.Nina.StableNC.lib.Coordinate;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

/**
 * Response to GossipRequestMsg. Responds with the receiving node's current
 * coordinate, confidence and last update time.
 */
public class CoordGossipResponseMsg extends ObjMessage {

	static final long serialVersionUID = 18L;

	final Coordinate remoteCoord;
	final double remoteError;
	final long remoteAge;
	public AddressIF from;

	final Set<AddressIF> nodes;

	final Coordinate VivaldiCoord;
	final double VivaldiremoteError;
	final long VivaldiremoteAge;

	final Coordinate BasicHypCoord;
	final double BasicHypremoteError;
	final long BasicHypremoteAge;

	public CoordGossipResponseMsg(Coordinate _remoteCoord, double _remoteError,
			long _remoteAge, Set<AddressIF> _nodes, AddressIF _from,
			Coordinate _VivaldiCoord, double _VivaldiremoteError,
			long _VivaldiremoteAge, Coordinate _BasicHypCoord,
			double _BasicHypremoteError, long _BasicHypremoteAge) {
		remoteCoord = _remoteCoord;
		remoteError = _remoteError;
		remoteAge = _remoteAge;

		nodes = _nodes;
		from = _from;
		VivaldiCoord = _VivaldiCoord;
		VivaldiremoteError = _VivaldiremoteError;
		VivaldiremoteAge = _VivaldiremoteAge;

		BasicHypCoord = _BasicHypCoord;
		BasicHypremoteError = _BasicHypremoteError;
		BasicHypremoteAge = _BasicHypremoteAge;
	}
	
	public CoordGossipResponseMsg(Coordinate _remoteCoord, double _remoteError,
			long _remoteAge) {
		remoteCoord = _remoteCoord;
		remoteError = _remoteError;
		remoteAge = _remoteAge;
		
		nodes = null;
		from = null;
		VivaldiCoord = null;
		VivaldiremoteError = -1;
		VivaldiremoteAge = -1;

		BasicHypCoord = null;
		BasicHypremoteError = -1;
		BasicHypremoteAge =-1;
	}

	public Coordinate getRemoteCoord() {
		return remoteCoord;
	}

	public double getRemoteError() {
		return remoteError;
	}

	public long getRemoteAge() {
		return remoteAge;
	}
	
	
	
}
