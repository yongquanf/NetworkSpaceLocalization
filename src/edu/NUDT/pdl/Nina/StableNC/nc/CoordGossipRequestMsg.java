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
import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

/**
 * Requests the receiving node's current coordinate, confidence and last update
 * time.
 */

public class CoordGossipRequestMsg extends ObjMessage {

	static final long serialVersionUID = 18L;

	final Coordinate Coord;

	final Set<AddressIF> nodes;
	public AddressIF from;

	final Coordinate VivaldiCoord;

	final Coordinate BasicHypCoord;

	/**
	 * Creates a GossipRequestMsg
	 * 
	 * @param _BasicHypCoord
	 * @param nodes
	 *            set of nodes sender is gossipping to receiver
	 */
	public CoordGossipRequestMsg(Coordinate _Coord, Set<AddressIF> _nodes,
			AddressIF _from, Coordinate _VivaldiCoord, Coordinate _BasicHypCoord) {
		Coord = _Coord;

		nodes = _nodes;
		from =_from;
		VivaldiCoord = _VivaldiCoord;

		BasicHypCoord = _BasicHypCoord;
	}
	
	public CoordGossipRequestMsg(
			AddressIF _from) {
		Coord = null;

		nodes = null;
		from =_from;
		VivaldiCoord = null;

		BasicHypCoord = null;
	}
}
