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
package edu.NUDT.pdl.Nina.log;

import edu.NUDT.pdl.Nina.StableNC.lib.Coordinate;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class ReportCoordReqMsg extends ObjMessage {

	private static final long serialVersionUID = 51L;

	public Coordinate primarySysCoord;
	public double primarySysCoordError;
	public Coordinate primaryAppCoord;
	public AddressIF from;
	public double medianUpdateTime = 0;
	public double per80UpdateTime = 0;
	public double per95UpdateTime = 0;
	public double medianMovement = 0;

	// -------------------------------------
	// Vivaldi

	public Coordinate VivaldiCoord;
	public double VivaldiSysCoordError = 0;
	public double VivaldimedianUpdateTime = 0;
	public double VivaldimedianMovement = 0;

	// -------------------------------------
	// Hyperbolic
	public Coordinate BasicHypCoord;
	public double BasicHypSysCoordError = 0;
	public double BasicHypmedianUpdateTime = 0;
	public double BasicHypmedianMovement = 0;

	public ReportCoordReqMsg(Coordinate _primarySysCoord,
			double _primarySysCoordError, Coordinate _primaryAppCoord,
			AddressIF _from) {
		primarySysCoord = _primarySysCoord;
		primarySysCoordError = _primarySysCoordError;
		primaryAppCoord = _primaryAppCoord;
		from = _from;
	}
}
