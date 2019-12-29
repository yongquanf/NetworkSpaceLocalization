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

import edu.NUDT.pdl.Nina.Ninaloader;
import edu.NUDT.pdl.Nina.StableNC.nc.StableManager;
import edu.NUDT.pdl.pyxida.ping.PingManager;
import edu.harvard.syrah.prp.Log;
import edu.harvard.syrah.sbon.async.CBResult;
import edu.harvard.syrah.sbon.async.Config;
import edu.harvard.syrah.sbon.async.EL;
import edu.harvard.syrah.sbon.async.CBResult.CBState;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB0;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB1;
import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjCommIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjCommRRCB;

public class LogManager {
	private static final Log log = new Log(LogManager.class);

	private static final String LOG_SERVER_HOSTNAME = Config
			.getProperty("logserver");

	static final long ERROR_RETRY_INTERVAL = Long.valueOf(Config.getProperty(
			"logserver.error_interval", "600000"));

	public static final long STATS_PERIOD = Long.parseLong(Config
			.getConfigProps().getProperty("stats_period", "600000"));

	private ObjCommIF comm;
	private StableManager ncManager;
	private PingManager pingManager;

	final CB0 statCB;

	public LogManager(ObjCommIF objComm, StableManager ncManager,
			PingManager pingManager) {
		this.comm = objComm;
		this.ncManager = ncManager;
		this.pingManager = pingManager;

		statCB = new CB0() {
			@Override
			protected void cb(CBResult result) {
				printStats();
				registerStatsTimer();
			}
		};
	}

	public void init(final CB0 cb0) {

		registerStatsTimer();
		if (LOG_SERVER_HOSTNAME == null) {
			log.debug("Not logging to log server");
			cb0.callOK();
			return;
		}

		AddressFactory.createResolved(LOG_SERVER_HOSTNAME, Ninaloader.COMM_PORT, new CB1<AddressIF>() {
			@Override
			protected void cb(CBResult result, AddressIF logServerAddr) {
				if (result.state == CBState.OK) {
					log.debug("Logging to logserver=" + logServerAddr);
					EL.get().registerTimerCB(
							new ReportCoordCB(comm, logServerAddr, ncManager));
				}
				cb0.call(result);
			}
		});

	}

	void printStats() {
		ncManager.printStats();
		pingManager.printStats();
	}

	void registerStatsTimer() {
		EL.get().registerTimerCB(STATS_PERIOD, statCB);
	}
}

class ReportCoordCB extends CB0 {
	private static final Log log = new Log(ReportCoordCB.class);

	private ObjCommIF comm;

	private AddressIF logServerAddr;

	private StableManager ncManager;

	ReportCoordCB(ObjCommIF objComm, AddressIF logServerAddr,
			StableManager ncManager) {
		this.comm = objComm;
		this.logServerAddr = logServerAddr;
		this.ncManager = ncManager;
	}

	@Override
	protected void cb(CBResult result) {
		ReportCoordReqMsg reqMsg = new ReportCoordReqMsg(ncManager
				.getLocalCoord(), ncManager.getLocalError(), ncManager
				.getLocalCoord(), Ninaloader.me);
		// reqMsg.primarySysCoord = ncManager.getLocalCoord();
		// reqMsg.primarySysCoordError = ncManager.getLocalError();
		// reqMsg.primaryAppCoord = ncManager.getLocalCoord();
		//		
		// computation time
		// if(ncManager.running_Update_time.>0){
		reqMsg.medianUpdateTime = ncManager.running_Update_time.get();
		// reqMsg.per80UpdateTime=ncManager.running_Update_time.getPercentile(.8);
		// reqMsg.per95UpdateTime=ncManager.running_Update_time.getPercentile(.95);
		// }//movement
		reqMsg.medianMovement = ncManager.localNC.primaryNC.getDistanceDelta();
		// reqMsg.primarySysCoord = ncManager.getLocalCoord();
		// reqMsg.primarySysCoordError = ncManager.getLocalError();
		// reqMsg.primaryAppCoord = ncManager.getLocalCoord();

		reqMsg.VivaldiCoord = ncManager.localVivaldi.primaryNC.sys_coord;
		reqMsg.VivaldiSysCoordError = ncManager.localVivaldi.primaryNC
				.getSystemError();
		reqMsg.VivaldimedianUpdateTime = ncManager.Vivaldi_running_Update_time
				.get();
		reqMsg.VivaldimedianMovement = ncManager.localVivaldi.primaryNC
				.getDistanceDelta();

		// -----------------------------------------------------------------------
		reqMsg.BasicHypCoord = ncManager.localBasicHyperbolic.primaryNC.sys_coord;
		reqMsg.BasicHypSysCoordError = ncManager.localBasicHyperbolic.primaryNC
				.getSystemError();
		reqMsg.BasicHypmedianUpdateTime = ncManager.BasicHyper_running_Update_time
				.get();
		reqMsg.BasicHypmedianMovement = ncManager.localBasicHyperbolic.primaryNC
				.getDistanceDelta();

		//		
		log.debug("Reporting c1=" + reqMsg.primarySysCoord + " to logserver");
		comm.sendRequestMessage(reqMsg, logServerAddr,
				new ObjCommRRCB<ReportCoordReplyMsg>() {
					@Override
					protected void cb(CBResult result,
							ReportCoordReplyMsg replyMsg, AddressIF remoteAddr,
							Long rtt) {
						switch (result.state) {
						case OK: {
							log.debug("Coord updated successfully. interval="
									+ replyMsg.interval);
							EL.get().registerTimerCB(replyMsg.interval,
									ReportCoordCB.this);
							break;
						}
						case TIMEOUT:
						case ERROR: {
							log.debug("LogServer error: " + result.what
									+ ". Retrying in "
									+ LogManager.ERROR_RETRY_INTERVAL + " ms");
							EL.get().registerTimerCB(
									LogManager.ERROR_RETRY_INTERVAL,
									ReportCoordCB.this);
							break;
						}
						}
					}
				});
	}

}
