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
package edu.NUDT.pdl.Nina;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import edu.NUDT.pdl.Nina.StableNC.lib.Coordinate;
import edu.NUDT.pdl.Nina.log.ReportCoordReplyMsg;
import edu.NUDT.pdl.Nina.log.ReportCoordReqMsg;
import edu.harvard.syrah.prp.ANSI;
import edu.harvard.syrah.prp.Log;
import edu.harvard.syrah.sbon.async.CBResult;
import edu.harvard.syrah.sbon.async.Config;
import edu.harvard.syrah.sbon.async.EL;
import edu.harvard.syrah.sbon.async.CBResult.CBState;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB0;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB1;
import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjComm;
import edu.harvard.syrah.sbon.async.comm.obj.ObjCommCB;
import edu.harvard.syrah.sbon.async.comm.obj.ObjCommIF;

public class KNNLogServer {
	private static final Log log = new Log(KNNLogServer.class);

	static {
		/*
		 * All config properties in the file must start with 'Nina.'
		 */
		Config.read("Nina", System.getProperty("config", "config/Nina.cfg"));
	}

	private static final String CONFIG_FILE = System.getProperty("config",
			"config/Nina.cfg");

	// Imperial blocks ports outside of 55000-56999
	public static final int COMM_PORT = Integer.parseInt(Config
			.getConfigProps().getProperty("port", "55504"));

	// By default tell nodes to report their coords every 10 seconds
	private static final long LOG_INTERVAL = Long.valueOf(Config
			.getConfigProps().getProperty("logserver.log_interval", "10000"));

	// Output a summary of nodes we have heard from recently at this rate (10
	// minutes)
	private static final long DUMP_COORD_INTERVAL = Long.valueOf(Config
			.getConfigProps().getProperty("logserver.dump_coord_interval",
					"600000"));
	// private static final long DUMP_COORD_INTERVAL =
	// Long.valueOf(Config.getConfigProps().getProperty("logserver.dump_coord_interval",
	// "200000"));
	private static final String DUMP_COORD_PATH = String.valueOf(Config
			.getConfigProps().getProperty("logserver.dump_coord_path",
					"nc/log-Nina-coords"));

	public static final String[] myRegion = Config.getProperty("myDNSAddress",
			"").split("[\\s]");
	public static AddressIF me;

	private static KNNLogServer logServer = null;

	ObjCommIF comm;
	final CB0 dumpCoordsCB;
	final SimpleDateFormat dateFormat;
	Map<AddressIF, CoordDesc> addr2coords = new HashMap<AddressIF, CoordDesc>();

	final static protected NumberFormat nf = NumberFormat.getInstance();

	final static protected int NFDigits = 3;

	static {
		if (nf.getMaximumFractionDigits() > NFDigits) {
			nf.setMaximumFractionDigits(NFDigits);
		}
		if (nf.getMinimumFractionDigits() > NFDigits) {
			nf.setMinimumFractionDigits(NFDigits);
		}
		nf.setGroupingUsed(false);
	}

	public KNNLogServer() {
		dateFormat = new SimpleDateFormat("yyyyMMdd-HHmm");
		dumpCoordsCB = new CB0() {
			
			protected void cb(CBResult result) {
				dumpCoords();
				registerDumpCoordsTimer();
			}
		};
	}

	private void init(final CB0 cbDone) {
		// Initiliase the ObjComm communication module
		comm = new ObjComm();
		AddressIF objCommAddr = AddressFactory.createServer(COMM_PORT);
		log.debug("Starting objcomm server...");
		comm.initServer(objCommAddr, new CB0() {
			
			protected void cb(CBResult result) {
				if (result.state == CBState.OK) {
					// coordinate report msg
					comm.registerMessageCB(ReportCoordReqMsg.class,
							new ReportCoordMsgHandler());
				}
				cbDone.call(result);
			}
		});
		registerDumpCoordsTimer();
	}

	void registerDumpCoordsTimer() {
		EL.get().registerTimerCB(DUMP_COORD_INTERVAL, dumpCoordsCB);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		log.main("Nina Log Server Version 0.1 starting...");

		if (args.length > 0 && args[0].equals("-d")) {
			Log.setPackageRoot(KNNLogServer.class);
			String[] newArgs = new String[args.length - 1];
			for (int i = 0; i < newArgs.length; i++)
				newArgs[i] = args[i];
			args = newArgs;
		}

		// Turn on assertions
		boolean assertsEnabled = false;
		assert assertsEnabled = true;
		if (!assertsEnabled)
			log
					.error("Please run the code with assertions turned on: java -ea ...");

		// Turn off colour support
		ANSI.use(false);

		/*
		 * Create the event loop
		 */
		EL.set(new EL(Long.valueOf(Config.getConfigProps().getProperty(
				"sbon.eventloop.statedump", "600000")), Boolean.valueOf(Config
				.getConfigProps().getProperty("sbon.eventloop.showidle",
						"false"))));

		logServer = new KNNLogServer();

		AddressFactory.createResolved(Arrays.asList(myRegion), COMM_PORT,
				new CB1<Map<String, AddressIF>>() {
					
					protected void cb(CBResult result,
							Map<String, AddressIF> addrMap) {
						switch (result.state) {
						case OK: {

							for (String node : addrMap.keySet()) {

								AddressIF remoteAddr = addrMap.get(node);
								me = AddressFactory.create(remoteAddr);
								System.out.println("MyAddr='" + me + "'");

								// System.exit(-1);

								break;
							}
							// init

							logServer.init(new CB0() {
								
								protected void cb(CBResult result) {
									switch (result.state) {
									case OK: {
										log
												.main("Nina log server initialised successfully");
										break;
									}
									case TIMEOUT:
									case ERROR: {
										log
												.error("Could not initialise Nina log server: "
														+ result.toString());
										break;
									}
									}
								}
							});

							break;

						}
						case TIMEOUT:
						case ERROR: {
							log.error("Could not resolve bootstrap list: "
									+ result.what);
							break;
						}
						}
					}

				});

		try {
			EL.get().main();
		} catch (OutOfMemoryError e) {
			EL.get().dumpState(true);
			e.printStackTrace();
			log.error("Error: Out of memory: " + e);
		}

		log.main("Shutdown");
		System.exit(0);
	}

	public class CoordDesc {
		public final Coordinate primarySysCoord;
		public final double primarySysCoordError;
		public final Coordinate primaryAppCoord;
		public final double medianUpdateTime;
		public final double medianMovement;

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

		// public final Coordinate secondarySysCoord;
		// public final double secondarySysCoordError;
		// public final Coordinate secondaryAppCoord;

		// could be used to preserve coords longer than window
		// public final long timestamp;

		// could add counter to tell number of updates from a node

		// not doing clone -- problem?

		public CoordDesc(ReportCoordReqMsg msg) {
			primarySysCoord = msg.primarySysCoord;
			primarySysCoordError = msg.primarySysCoordError;
			primaryAppCoord = msg.primaryAppCoord;
			medianUpdateTime = msg.medianUpdateTime;
			medianMovement = msg.medianMovement;

			// -------------------------------------
			// Vivaldi

			VivaldiCoord = msg.VivaldiCoord;
			VivaldiSysCoordError = msg.VivaldiSysCoordError;
			VivaldimedianUpdateTime = msg.VivaldimedianUpdateTime;
			VivaldimedianMovement = msg.VivaldimedianMovement;

			// -------------------------------------
			// Hyperbolic
			BasicHypCoord = msg.BasicHypCoord;
			BasicHypSysCoordError = msg.BasicHypSysCoordError;
			BasicHypmedianUpdateTime = msg.BasicHypmedianUpdateTime;
			BasicHypmedianMovement = msg.BasicHypmedianMovement;
			// secondarySysCoord = msg.secondarySysCoord;
			// secondarySysCoordError = msg.secondarySysCoordError;
			// secondaryAppCoord = msg.secondaryAppCoord;
		}

		
		public String toString() {
			// return
			// ("cP="+primaryAppCoord.toString()+" erP="+nf.format(primarySysCoordError)+
			// " cS="+secondaryAppCoord.toString()+" erS="+nf.format(secondarySysCoordError));
			return ("coordinate=" + primaryAppCoord.toString() + " err="
					+ nf.format(primarySysCoordError) + " dd="
					+ nf.format(medianMovement) + "Cycle="
					+ nf.format(medianUpdateTime) + "Vivaldicoordinate="
					+ VivaldiCoord.toString() + " Vivaldierr="
					+ nf.format(VivaldiSysCoordError) + " Vivaldidd="
					+ nf.format(VivaldimedianUpdateTime) + "VivaldiCycle="
					+ nf.format(VivaldimedianMovement) + "BasicHypcoordinate="
					+ BasicHypCoord.toString() + "BasicHyperr="
					+ nf.format(BasicHypSysCoordError) + "BasicHypdd="
					+ nf.format(BasicHypmedianUpdateTime) + "BasicHypCycle=" + nf
					.format(BasicHypmedianMovement));
		}

	}

	void dumpCoords() {

		// TODO: windows platform
		String file = DUMP_COORD_PATH + "/" + dateFormat.format(new Date())
				+ ".coords";
		String currentFile = DUMP_COORD_PATH + "/" + "current.coords";
		log.info("Dumping coords to file " + file);

		File coordFile = new File(file);
		try {
			PrintWriter out = new PrintWriter(new BufferedWriter(
					new FileWriter(file)));
			PrintWriter currentOut = new PrintWriter(new BufferedWriter(
					new FileWriter(currentFile)));

			for (Map.Entry<AddressIF, CoordDesc> entry : addr2coords.entrySet()) {
				String line = entry.getKey() + " " + entry.getValue() + "\n";
				out.write(line);
				currentOut.write(line);
			}

			out.flush();
			out.close();
			currentOut.flush();
			currentOut.close();
			log.info("wrote to file " + file);
		} catch (Exception ex) {
			System.err.println("writing " + file + " failed.  Exiting");
		}

		addr2coords.clear();
	}

	class ReportCoordMsgHandler extends ObjCommCB<ReportCoordReqMsg> {
		private final Log log = new Log(ReportCoordMsgHandler.class);

		
		protected void cb(CBResult result, ReportCoordReqMsg reqMsg,
				AddressIF remoteAddr1, Long delay, CB1<Boolean> cbHandled) {
			switch (result.state) {
			case OK: {
				/*
				 * log.info("a=" + remoteAddr + " sc1=" + reqMsg.primarySysCoord
				 * + " er1="+ nf.format(reqMsg.primarySysCoordError)+" ac1=" +
				 * reqMsg.primaryAppCoord + " sc2=" + reqMsg.secondarySysCoord +
				 * " er2=" + nf.format(reqMsg.secondarySysCoordError)+ " ac2=" +
				 * reqMsg.secondaryAppCoord);
				 */
				AddressIF remoteAddr = reqMsg.from;
				log.info("addr=" + remoteAddr + " sc1="
						+ reqMsg.primarySysCoord + " er1="
						+ nf.format(reqMsg.primarySysCoordError)
						+ " avgUpdateTime=" + reqMsg.medianUpdateTime
						+ " avgMovement=" + reqMsg.medianMovement
						+ " Vivaldi sc1=" + reqMsg.VivaldiCoord
						+ " Vivaldi er1="
						+ nf.format(reqMsg.VivaldiSysCoordError)
						+ " Vivaldi avgUpdateTime="
						+ reqMsg.VivaldimedianUpdateTime
						+ " Vivaldi avgMovement="
						+ reqMsg.VivaldimedianMovement + " BasicHyp sc1="
						+ reqMsg.BasicHypCoord + " BasicHyp er1="
						+ nf.format(reqMsg.BasicHypSysCoordError)
						+ " BasicHyp avgUpdateTime="
						+ reqMsg.BasicHypmedianUpdateTime
						+ " BasicHyp avgMovement="
						+ reqMsg.BasicHypmedianMovement);
				// just scribble over old version if there is one
				addr2coords.put(remoteAddr, new CoordDesc(reqMsg));

				ReportCoordReplyMsg replyMsg = new ReportCoordReplyMsg(
						LOG_INTERVAL, me);
				// replyMsg.interval = LOG_INTERVAL;

				KNNLogServer.this.comm.sendResponseMessage(replyMsg,
						remoteAddr, reqMsg.getMsgId(), new CB0() {
							
							protected void cb(CBResult result) {
								if (result.state != CBState.OK) {
									log.warn("Could not send reply: " + result);
								}
							}
						});
				cbHandled.call(CBResult.OK(), true);
				break;
			}
			case TIMEOUT:
			case ERROR: {
				log.warn(result.toString());
				break;
			}
			}
		}
	}
}
