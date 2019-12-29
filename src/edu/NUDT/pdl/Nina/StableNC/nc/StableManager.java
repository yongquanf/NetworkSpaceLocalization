package edu.NUDT.pdl.Nina.StableNC.nc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import edu.NUDT.pdl.Nina.Ninaloader;
import edu.NUDT.pdl.Nina.KNN.AbstractNNSearchManager;
import edu.NUDT.pdl.Nina.KNN.ConcentricRing;
import edu.NUDT.pdl.Nina.StableNC.lib.Coordinate;
import edu.NUDT.pdl.Nina.StableNC.lib.EWMAStatistic;
import edu.NUDT.pdl.Nina.StableNC.lib.NCClient;
import edu.NUDT.pdl.Nina.StableNC.lib.RemoteState;
import edu.NUDT.pdl.Nina.util.MainGeneric;
import edu.NUDT.pdl.pyxida.ping.PingManager;
import edu.harvard.syrah.prp.Log;
import edu.harvard.syrah.prp.NetUtil;
import edu.harvard.syrah.prp.POut;
import edu.harvard.syrah.prp.PUtil;
import edu.harvard.syrah.prp.SortedList;
import edu.harvard.syrah.prp.Stat;
import edu.harvard.syrah.sbon.async.Barrier;
import edu.harvard.syrah.sbon.async.CBResult;
import edu.harvard.syrah.sbon.async.Config;
import edu.harvard.syrah.sbon.async.EL;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB0;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB1;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB2;
import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.NetAddress;
import edu.harvard.syrah.sbon.async.comm.obj.ObjCommCB;
import edu.harvard.syrah.sbon.async.comm.obj.ObjCommIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjCommRRCB;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessageIF;

/**
 * 
 * Stable version of Network coordinate 9-23 Nina, Vivaldi(icdcs06), Hyperbolic
 * space based Vivaldi(icdcs08) are implemented
 * 
 * @author ericfu
 * 
 */
public class StableManager {
	
	// data structure
	static final Log log = new Log(StableManager.class);
	
	
//	private Lock lock = new ReentrantLock(); 

	// Number of dimensions
	public static final int NC_NUM_DIMS = Integer.parseInt(Config.getProperty(
			"dimensions", "5"));

	// height vector
	// TODO: Hyperbolic coordinate
	//public static final boolean useHeight = Boolean.parseBoolean(Config.getConfigProps().getProperty("useHeight", "false"));

	public static final boolean WATCH_NEIGHBORS = Boolean
			.parseBoolean(Config.getConfigProps().getProperty(
					"ncmanager.watch_neighbors", "false"));

	// space
	public static final boolean SpaceChoice = Boolean.parseBoolean(Config
			.getConfigProps().getProperty("isEuclidean", "true"));
	// stable process
	public static final boolean useStabilization = Boolean.parseBoolean(Config
			.getConfigProps().getProperty("useStabilization", "true"));

	public static final boolean useSoftFit = Boolean.parseBoolean(Config
			.getConfigProps().getProperty("useSoftFit", "true"));

	
	public static final boolean UseGroupEnhancement = Boolean.parseBoolean(Config
			.getConfigProps().getProperty("UseGroupEnhancement", "false"));

	
	
	public static final boolean UseSwitching = Boolean.parseBoolean(Config
			.getConfigProps().getProperty("UseSwitching", "false"));
	
	// symmetric process
	public static final boolean useSymmetricScheme = Boolean
			.parseBoolean(Config.getConfigProps().getProperty(
					"useSymmetricScheme", "false"));

	// symmetric rounds
	public static final int symmetricRounds = Integer.parseInt(Config
			.getProperty("symmetricRounds", "1"));

	// init process
	public static final boolean useInitialization = Boolean.parseBoolean(Config
			.getConfigProps().getProperty("useInitialization", "false"));
	// number of landmarks
	public static final int numOfLandmarks = Integer.parseInt(Config
			.getProperty("numOfLandmarks", "8"));

	// Height is set within NCClient and is the "last dimension"
	// of the coordinates array.
	// Thus, setting this to 5 is 4d+height if height is in use.
	// This allows the size on the wire to remain constant
	// which is important for Azureus integration.

	String bootstrapList[] = Config.getProperty("bootstraplist",
			"onelab02.inria.fr").split("[\\s]");
	Set<AddressIF> bootstrapAddrs;

	/**
	 * Time between gossip messages to coordinate neighbors. Default is 10
	 * seconds.
	 */
	public static final long UPDATE_DELAY =Integer.parseInt(Config
			.getConfigProps().getProperty("UPDATE_DELAY", "300000"));

	private PingManager pingManager = null;

	final ObjCommIF comm;
	
	final ExecutorService execNetCoord =Ninaloader.execNina;
	
	public final CoordClient localNC;
	public final VivaldiClient localVivaldi;
	public final HyperClient localBasicHyperbolic;

	public Set<AddressIF> upNeighbors;
	Set<AddressIF> downNeighbors;
	public Set<AddressIF> pendingNeighbors;

	public EWMAStatistic running_Update_time;
	public EWMAStatistic Vivaldi_running_Update_time;
	public EWMAStatistic BasicHyper_running_Update_time;

	public Map<AddressIF, RemoteState<AddressIF>> pendingLatency = new HashMap<AddressIF, RemoteState<AddressIF>>(
			1);

	public static int maxPending=Integer.parseInt(Config
			.getProperty("maxPending", "50"));
	// ----------------------------------------------
	/**
	 * Create a coordinate manager. Does not block.
	 */
	public StableManager(ObjCommIF _comm, PingManager pingManager) {
		comm = _comm;
		

		//release the memory overhead
		localNC =null;
		
		
		localVivaldi = null;
		localBasicHyperbolic  = null;
		running_Update_time  = null;
		BasicHyper_running_Update_time  = null;
		Vivaldi_running_Update_time  = null;
		
/*		this.pingManager = pingManager;
		// Initialise the local coords first
		
		localNC = new CoordClient();
		
		
		localVivaldi = new VivaldiClient();
		localBasicHyperbolic = new HyperClient();
		running_Update_time = new EWMAStatistic();
		BasicHyper_running_Update_time = new EWMAStatistic();
		Vivaldi_running_Update_time = new EWMAStatistic();*/
	}

	/**
	 * Asynchronous initialization of coordinate manager. Resolves bootstrap
	 * neighbors and starts gossip for local coordinate. Starts listening for
	 * gossip messages.
	 */
	public void init(final CB0 cbDone) {

		//comm.registerMessageCB(CoordGossipRequestMsg.class,new GossipHandler());
		//comm.registerMessageCB(CoordRequestMsg.class, new CoordHandler());
		comm.registerMessageCB(CollectRequestMsg.class, new CollectHandler());

		
		cbDone.callOK();
		
		
		/*upNeighbors = new HashSet<AddressIF>();
		downNeighbors = new HashSet<AddressIF>();
		pendingNeighbors = new HashSet<AddressIF>();
		bootstrapAddrs = new HashSet<AddressIF>();

		log.debug("Resolving bootstrap list");
		AddressFactory.createResolved(Arrays.asList(bootstrapList),
				Ninaloader.COMM_PORT, new CB1<Map<String, AddressIF>>() {
					
					protected void cb(CBResult result,
							Map<String, AddressIF> addrMap) {
						switch (result.state) {
						case OK: {
							for (String remoteNode : addrMap.keySet()) {
								log.debug("remoteNode='" + remoteNode + "'");
								AddressIF remoteAddr = addrMap.get(remoteNode);
								// we keep these around in case we run out of
								// neighbors in the future
								if (!remoteAddr.equals(Ninaloader.me)) {
									bootstrapAddrs.add(remoteAddr);
									addPendingNeighbor(remoteAddr);
								}
							}

							// Starts local coordinate timer
							
							if(Ninaloader.USE_NetCoord){
								localNC.init();
							//start the localNC thread
								execNetCoord.execute(localNC);
							}						
							cbDone.callOK();
							break;
						}
						case TIMEOUT:
						case ERROR: {
							log.error("Could not resolve bootstrap list: "
									+ result.what);
							cbDone.callERROR();
							break;
						}
						}
					}
				});*/
	}

	public void printStats() {
		log.info("coord=" + localNC.primaryNC + " " + summarizeNeighbors());
		log.info(localNC.primaryNC.printNeighbors());
	}

	/**
	 * @return local coordinate
	 */
	public Coordinate getLocalCoord() {
		Coordinate tmp=localNC.primaryNC.getSystemCoords();
		tmp.VivaldiCoord=localVivaldi.primaryNC.getSystemCoords();
		tmp.HyperVivaldiCoord=localBasicHyperbolic.primaryNC.getSystemCoords();
		return tmp;
	}

	/**
	 * @return local coordinate
	 */
	/*
	 * public Coordinate getStableCoord() { return
	 * localNC.primaryNC.getSystemCoords(); }
	 */

	/**
	 * @return local error
	 */
	public double getLocalError() {
		return localNC.primaryNC.getSystemError();
	}

	// handler

	// define what to do once the different types of request messages have been
	// received
	abstract class ResponseObjCommCB<T extends ObjMessageIF> extends
			ObjCommCB<T> {
		void sendResponseMessage(final String handler,
				final AddressIF remoteAddr11, final ObjMessage response,
				long requestMsgId, final String errorMessage,
				final CB1<Boolean> cbHandled) {
			if (errorMessage != null) {
				log.warn(handler + " : " + errorMessage);
			}
			log.debug("@: ResponseObjCommCB"+remoteAddr11);
			comm.sendResponseMessage(response, remoteAddr11, requestMsgId,
					new CB0() {
						protected void cb(CBResult sendResult) {
							switch (sendResult.state) {
							case TIMEOUT:
							case ERROR: {
								log.warn(handler + ": " + sendResult.what);
								return;
							}
							}
						}
					});
			cbHandled.call(CBResult.OK(), true);
		}
	}

	class GossipHandler extends ResponseObjCommCB<CoordGossipRequestMsg> {

		public void cb(CBResult result, CoordGossipRequestMsg msg,
				AddressIF remoteAddr, Long ts, final CB1<Boolean> cbHandled) {
			log.debug("in GossipHandler cb");
			// we just heard from him so we know he is up
			addUpNeighbor(msg.from);
			if(localNC.primaryNC.containsHost(msg.from)){
				localNC.primaryNC.getRs_map().get(msg.from).last_coords=msg.Coord;
			}
			addPendingNeighbors(msg.nodes);
			
			long curr_time = System.currentTimeMillis();
			sendResponseMessage("Gossip", msg.from, new CoordGossipResponseMsg(
					localNC.primaryNC.getSystemCoords(), localNC.primaryNC
							.getSystemError(), localNC.primaryNC
							.getAge(curr_time), getUpNeighbors(),
					Ninaloader.me, localVivaldi.primaryNC.getSystemCoords(),
					localVivaldi.primaryNC.getSystemError(),
					localVivaldi.primaryNC.getAge(curr_time),
					localBasicHyperbolic.primaryNC.getSystemCoords(),
					localBasicHyperbolic.primaryNC.getSystemError(),
					localBasicHyperbolic.primaryNC.getAge(curr_time)), msg
					.getMsgId(), null, cbHandled);

		}
	}

	class CoordHandler extends ResponseObjCommCB<CoordRequestMsg> {
		public void cb(CBResult result, CoordRequestMsg msg,
				AddressIF remoteAddr, Long ts, final CB1<Boolean> cbHandled) {
			log.debug("in CoordHandler remoteAddr=" + msg.from);
			long curr_time = System.currentTimeMillis();

			double rtt = -1;
			if (msg.myCoord != null) {
				rtt = msg.RTT;
			}		
			log.debug("$: Acked received RTT: " + rtt);
			
			//if (msg.myCoord != null) {
			Coordinate VivaldiCoord=localVivaldi.primaryNC.getSystemCoords();
			VivaldiCoord.r_error=localVivaldi.primaryNC.getSystemError();
			Coordinate BasicHyperCoord=localBasicHyperbolic.primaryNC.getSystemCoords();
			BasicHyperCoord.r_error=localBasicHyperbolic.primaryNC.getSystemError();
			// then we send the response message
			sendResponseMessage("Coord", msg.from, new CoordResponseMsg(
					Ninaloader.me, localNC.primaryNC.sys_coord.makeCopy(VivaldiCoord, 
							BasicHyperCoord		
					),
					localNC.primaryNC.getSystemError(), localNC.primaryNC
							.getAge(curr_time), rtt), msg.getMsgId(), null,
					cbHandled);
			
		/*	}else{
				sendResponseMessage("Coord", msg.from, new CoordResponseMsg(
						Ninaloader.me, localNC.primaryNC.sys_coord,
						localNC.primaryNC.getSystemError(), localNC.primaryNC
								.getAge(curr_time), rtt), msg.getMsgId(), null,
						cbHandled);
			}
		
*/
			// symmetric update
			if (useSymmetricScheme) {
				if (msg.myCoord != null) {
					// symmetric
					if (rtt > 0) {
						log.debug(" RTT=" + rtt);
						if (useStabilization) {
							// and update our coordinate
							localNC.primaryNC.processSample(msg.from,
									msg.myCoord, msg.myCoord.r_error,
									rtt, msg.myCoord.age,
									curr_time, true);
						} else {
							localNC.primaryNC.processSample_noStable(msg.from,
									msg.myCoord, msg.myCoord.r_error,
									rtt, msg.myCoord.age,
									curr_time, true);
						}

					}
				}
			}
		}
	}

	public class CollectHandler extends ResponseObjCommCB<CollectRequestMsg> {

		public void cb(CBResult result, CollectRequestMsg msg,
				AddressIF remoteAddr1, Long ts, final CB1<Boolean> cbHandled) {
			log.debug("in CollectHandler cb");
			// response
			CollectResponseMsg msg1 = new CollectResponseMsg();
			msg1.setResponse(true);
			msg1.setMsgId(msg.getMsgId());
			log.debug("$ @@ Maybe CollectRequest from: " + remoteAddr1
					+ ", but from: " + msg.from);

			sendResponseMessage("Collect", msg.from, msg1, msg.getMsgId(),
					null, cbHandled);

		}
	}

	/**
	 * simulating the Vivaldi
	 * 
	 * @author ericfu
	 * 
	 */
	public class VivaldiClient {
		public final NCClient<AddressIF> primaryNC;

		// final CB0 updateCB;

		public VivaldiClient() {
			log.debug("Making a CoordClient");
			primaryNC = new NCClient<AddressIF>(StableManager.NC_NUM_DIMS);
			primaryNC.sys_coord.isHyperbolic = false;
		}

	}

	public class HyperClient {
		public final NCClient<AddressIF> primaryNC;

		// final CB0 updateCB;

		public HyperClient() {
			log.debug("Making a CoordClient");
			primaryNC = new NCClient<AddressIF>(StableManager.NC_NUM_DIMS);
			primaryNC.sys_coord.isHyperbolic = true;
			primaryNC.UseICDCS08Hyperbolic = true;
			primaryNC.UseSwitching = false;
			primaryNC.oneNeighborPerUpdate = true;
		}

	}

	// ----------------------------------------------
	// Coordinate Client
	public class CoordClient implements Runnable{
		public final NCClient<AddressIF> primaryNC;
		final CB0 updateCB;
		final CB0 updateNeighborcoordinateCB;

		int delayedUpdate = 5; // delay 10 times

		public CoordClient() {
			log.debug("Making a CoordClient");
			primaryNC = new NCClient<AddressIF>(StableManager.NC_NUM_DIMS);
			if (SpaceChoice) {
				primaryNC.sys_coord.isHyperbolic = false;
			} else {
				primaryNC.sys_coord.isHyperbolic = true;
			}
			primaryNC.oneNeighborPerUpdate=false;
			primaryNC.UseSwitching = StableManager.UseSwitching;
			primaryNC.UseGroupEnhancement = StableManager.UseGroupEnhancement; // group
			//Ericfu- do not use fit function
			primaryNC.UseFitFunction =StableManager.useSoftFit;
			primaryNC.stableProcess = StableManager.useStabilization;
			primaryNC.UseICDCS08Hyperbolic = false;
			//Ericfu- use height
			//primaryNC.sys_coord.USE_HEIGHT=false;
			
			updateCB = new CB0() {
				protected void cb(CBResult result) {
					update();
				}
			};
			
			updateNeighborcoordinateCB = new CB0() {
				protected void cb(CBResult result) {
					updateNeighborCoordinate();

				}
			};
		}
			
		/**
		 * update neighbors' coordinates
		 */
			private void updateNeighborCoordinate() {
				// TODO Auto-generated method stub
				
				registerPullCoordinateTimer();
				
				long curr_time=System.currentTimeMillis();
				
				final AddressIF neighbor=primaryNC.getRandomNeighbor();
				
				
				if(neighbor==null){
					return;
				}
				CoordGossipRequestMsg msg = new CoordGossipRequestMsg(
						localNC.primaryNC.getSystemCoords(), getUpNeighbors(),
						Ninaloader.me, null,
						null);
				
				final long sendStamp = System.nanoTime();

				//log.info("Sending gossip request to " + neighbor);
				log.debug("CoordGossipRequestMsg: "+neighbor);
				comm.sendRequestMessage(msg, neighbor,
						new ObjCommRRCB<CoordGossipResponseMsg>() {

							protected void cb(CBResult result,
									final CoordGossipResponseMsg responseMsg,
									AddressIF remoteAddr, Long ts) {

								double rtt = (System.nanoTime() - sendStamp) / 1000000d;

								switch (result.state) {
								case OK: {
									long curr_time=System.currentTimeMillis();
									RemoteState<AddressIF> node = localNC.primaryNC.getRs_map().get(neighbor);
									if (node == null) {										
										localNC.primaryNC.addHost(neighbor);
										node  = localNC.primaryNC.getRs_map().get(neighbor);
									}
									node.addSample(rtt, responseMsg.remoteAge, responseMsg.remoteCoord,  responseMsg.remoteError, curr_time);
																		
									break;
								}
								case ERROR:
								case TIMEOUT:{									
									addDownNeighbor(neighbor);
									break;
								}
								}
							}
				}
				);
				
			}

			
		void registerTimer() {
			// LOWTODO adaptive delay
			double rnd = Ninaloader.random.nextGaussian();
			long delay = UPDATE_DELAY + (long) (20*60*1000 * rnd);

			log.debug("setting timer to " + delay);
			EL.get().registerTimerCB(delay, updateCB);

		}
		
		void registerPullCoordinateTimer() {
			// LOWTODO adaptive delay
			double rnd = Ninaloader.random.nextGaussian();
			long delay = UPDATE_DELAY + (long) (20*60*1000 * rnd);

			log.debug("setting timer to " + delay);
			EL.get().registerTimerCB(delay, updateNeighborcoordinateCB);

		}

		public void init() {
			registerTimer();
			registerPullCoordinateTimer();
		}

		void update() {
			registerTimer();

			// if I am not the landmarks, then I initialize my coordinate
			// according to the landmarks
			if (useInitialization) {
				if (!bootstrapAddrs.contains(Ninaloader.me)) {
					if (!primaryNC.initialized) {
						delayedUpdate--;
						if (delayedUpdate == 0) {
							initMyPosition();
						}
					}
				}
			}

			final AddressIF neighbor = pickGossipNode();

			if (neighbor == null) {
				log.warn("Nobody to gossip with. Waiting...");
				return;
			}

			// send him a gossip msg
			// LOWTODO could bias which nodes are sent based on his coord
			CoordGossipRequestMsg msg = new CoordGossipRequestMsg(
					localNC.primaryNC.getSystemCoords(), getUpNeighbors(),
					Ninaloader.me, localVivaldi.primaryNC.getSystemCoords(),
					localBasicHyperbolic.primaryNC.getSystemCoords());
			final long sendStamp = System.nanoTime();

			//log.info("Sending gossip request to " + neighbor);
			log.debug("CoordGossipRequestMsg: "+neighbor);
			comm.sendRequestMessage(msg, neighbor,
					new ObjCommRRCB<CoordGossipResponseMsg>() {

						protected void cb(CBResult result,
								final CoordGossipResponseMsg responseMsg,
								AddressIF remoteAddr, Long ts) {

							double rtt = (System.nanoTime() - sendStamp) / 1000000d;

							switch (result.state) {
							case OK: {
								// keep track of new guys he's told us about
								addPendingNeighbors(responseMsg.nodes);

								// log.info("app rtt of " + rtt + " to "+
								// neighbor);

								if (Ninaloader.USE_ICMP) {

									// and ping him
									pingManager.addPingRequest(neighbor,
											new CB1<Double>() {
												protected void cb(
														CBResult pingResult,
														Double latency) {
													switch (pingResult.state) {
													case OK: {

														double[] RTT = { latency
																.doubleValue() };
														postProcessCoordGossip(
																sendStamp, RTT,
																responseMsg);

														log
																.info("update1: "
																		+ localNC.primaryNC);

														break;
													}
													case TIMEOUT:
													case ERROR: {
														log.warn("Ping to "
																+ neighbor
																+ " failed");
														addDownNeighbor(neighbor);
														break;
													}
													}
												}
											});
								} else {

									double[] RTT = { rtt };
									postProcessCoordGossip(sendStamp, RTT,
											responseMsg);
									// -----------------------------------------
								}
								break;
							}
							case ERROR:
							case TIMEOUT: {
								log
										.warn("Did not receive gossip response from "
												+ neighbor);
								addDownNeighbor(neighbor);
								break;
							}
							}

						}
					});
		}

		/**
		 * post process the measurement process
		 * 
		 * @param sendStamp
		 * @param RTT
		 * @param responseMsg
		 */
		synchronized void postProcessCoordGossip(final long sendStamp,final double[] RTT,
				final CoordGossipResponseMsg responseMsg) {
			
			//fork a new thread
			execNetCoord.execute(new Runnable(){

				
				public void run() {
					// TODO Auto-generated method stub
					


			double rtt = RTT[0];
			AddressIF neighbor = responseMsg.from;

			// both calls worked
			addUpNeighbor(neighbor);

			// NOT using PingManager
			long curr_time = System.currentTimeMillis();
			long cur_Nano = System.nanoTime();
			double communicationTime = (cur_Nano - sendStamp) / 1000000d;

			// ----------------------------------------------------------------
			// our method
			if (useStabilization) {
				// and update our coordinate
				localNC.primaryNC.processSample(neighbor,
						responseMsg.remoteCoord, responseMsg.remoteError, rtt,
						responseMsg.remoteAge, curr_time, true);
			} else {
				localNC.primaryNC.processSample_noStable(neighbor,
						responseMsg.remoteCoord, responseMsg.remoteError, rtt,
						responseMsg.remoteAge, curr_time, true);

			}

			long completed_time = System.nanoTime();
			running_Update_time.add(communicationTime
					+ (completed_time - cur_Nano) / 1000000d);

			// ------------------------------------------------------------------
			// Vivaldi
			localVivaldi.primaryNC.Vivaldi_ProcessSample(neighbor,
					responseMsg.VivaldiCoord, responseMsg.VivaldiremoteError,
					rtt, responseMsg.VivaldiremoteAge, curr_time, true);

			long Vivaldi_completed_time = System.nanoTime();
			Vivaldi_running_Update_time.add(communicationTime
					+ (Vivaldi_completed_time - completed_time) / 1000000d);
			// time statistics
			// in ms
			// ------------------------------------------------------------------
			// Hyperbolic basic method
			localBasicHyperbolic.primaryNC.processSample_noStable(neighbor,
					responseMsg.BasicHypCoord, responseMsg.BasicHypremoteError,
					rtt, responseMsg.BasicHypremoteAge, curr_time, true);
			long BasicHyper_completed_time = System.nanoTime();
			BasicHyper_running_Update_time.add(communicationTime
					+ (BasicHyper_completed_time - Vivaldi_completed_time)
					/ 1000000d);
			// --------------------------------------------------------------------

			Ninaloader.logCoordinate=null;
			
			try {
				Ninaloader.logCoordinate = new BufferedWriter(new FileWriter(new File(
						Ninaloader.CoordinateLogName + ".log")));
				
				Ninaloader.logCoordinate.write("Timer:"
						+ (System.currentTimeMillis() - Ninaloader.StartTime)/ 1000d+"\n"
						 + Ninaloader.me.toString()+" "+neighbor.toString()+" "+rtt+"\n");
				Ninaloader.logCoordinate.write("Communication Time: "
						+ communicationTime + "\n");
				Ninaloader.logCoordinate.write("Nina update1: "
						+ localNC.primaryNC + "Cycle="
						+ running_Update_time.get() + "\n");
				Ninaloader.logCoordinate.write("Vivaldi update2: "
						+ localVivaldi.primaryNC + "Cycle="
						+ Vivaldi_running_Update_time.get() + "\n");
				Ninaloader.logCoordinate.write("Hyperbolic update3: "
						+ localBasicHyperbolic.primaryNC + "Cycle="
						+ BasicHyper_running_Update_time.get() + "\n");
				Ninaloader.logCoordinate.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}finally{
				if(Ninaloader.logCoordinate!=null){
				try{
					Ninaloader.logCoordinate.close();
				}catch(IOException e){
					e.printStackTrace();
				}
				}
			}

			if (StableManager.useSymmetricScheme) {
				// TODO: send updated coordinate to
				// remoteNode symmetric update
				// ----------------------------------------
				Coordinate tmp = localNC.primaryNC.sys_coord.makeCopy();
				tmp.isSymmetricTrigger = true;
				tmp.currentRTT = rtt;
				tmp.r_error = localNC.primaryNC.getSystemError();
				tmp.age = localNC.primaryNC.getAge(curr_time);
				log.debug("StableManager.useSymmetricScheme"+neighbor);
				comm.sendRequestMessage(
						new CoordRequestMsg(Ninaloader.me, tmp,rtt), neighbor,
						new ObjCommRRCB<CoordResponseMsg>() {
							protected void cb(CBResult result,
									final CoordResponseMsg resp,
									AddressIF remoteAddr, Long ts) {
								switch (result.state) {
								case OK: {

								}
								case ERROR:
								case TIMEOUT: {

									break;
								}
								}
							}
						});
			}
			
			
			
				}});

		}

		void resetBootstrapNeighbors() {
			for (AddressIF remoteAddr : bootstrapAddrs) {
				
					upNeighbors.remove(remoteAddr);
					downNeighbors.remove(remoteAddr);
					pendingNeighbors.add(remoteAddr);
				
			}
		}

		// Note that primaryNC and secondaryNC are using the same set of ping
		// neighbors
		// (drawn from primary's suggestions)

		AddressIF pickGossipNode() {
			AddressIF neighbor;
			// ask our ncClient if it has a preferred gossip node

			if (localNC.primaryNC.stabilized()
					&& localNC.primaryNC.hasAllNeighbors()) {
				log.debug("NCManager - stabilized and full");
				neighbor = localNC.primaryNC.getNeighborToPing(System
						.currentTimeMillis());
				if (neighbor == null) {
					log
							.debug("NCManager - stabilized and full but null neighbor");
					neighbor = getUpNeighbor();
					if (neighbor == null) {
						resetBootstrapNeighbors();
						neighbor = getUpNeighbor();
					}
				}
			} else {
				// if not, use somebody from our neighbor set
				neighbor = getUpNeighbor();
				if (neighbor == null) {
					resetBootstrapNeighbors();
					neighbor = getUpNeighbor();
				}
			}
			// assert (neighbor != null) :
			// "Could not find a bootstrap neighbour";
			int count=3;
			int i=0;
			while(neighbor!=null&&neighbor.equals(Ninaloader.me)){
			   if(i>3){
				   neighbor=null;
				   break;
			   }else{
				   i++;
				   neighbor = getUpNeighbor();
			   }
			   }
			return neighbor;
		}

		
		public void run() {
			// TODO Auto-generated method stub
			init() ;
		}
	}

	/**
	 * ping a set of nodes
	 * @param nodes
	 * @param cbDone
	 */
	public void doPing(Collection<AddressIF> nodes, final CB1<Map<AddressIF,Double>> cbDone){
		
		final Map<AddressIF,Double> addr2rtt = new HashMap<AddressIF,Double>();
		
		if (nodes==null||nodes.size() == 0) {
	
			 cbDone.call(CBResult.ERROR(), addr2rtt);
			return;
		}

		
		final Barrier barrier = new Barrier(true);
		
		//nodes
		barrier.setNumForks(nodes.size());
		
		for (final AddressIF addr : nodes) {
			
			doPing(addr, new CB2<Double, Long>(){

				
				protected void cb(CBResult result, Double arg1, Long arg2) {
					// TODO Auto-generated method stub
					if(arg1.doubleValue()>=0){
						addr2rtt.put(addr, arg1);
					}
					
					barrier.join();
				}
			
			});
			
		}
		
		long time=3*1000;
		EL.get().registerTimerCB(barrier,new CB0() {
			protected void cb(CBResult result) {
				
				 cbDone.call(CBResult.OK(), addr2rtt);
			}
		});
		
	}
	
	
	/**
	 * ping a node
	 * @param target
	 * @param cbDone
	 */
	void doPing(String target, final CB2<Double, Long> cbDone) {

		AddressFactory.createResolved(target, Ninaloader.COMM_PORT,
				new CB1<AddressIF>() {
					
					protected void cb(CBResult result, AddressIF addr) {

						final double[] lat = new double[1];
						lat[0] = -1;
						final long timer = System.currentTimeMillis();

						// TODO Auto-generated method stub
						switch (result.state) {
						case OK: {

							AddressIF neighbor = addr;
							if (Ninaloader.USE_ICMP) {

								log.info("ICMP based ping to " + neighbor);
								// ICMP ping
								pingManager.addPingRequest(neighbor,
										new CB1<Double>() {
											protected void cb(
													CBResult pingResult,
													Double latency) {
												switch (pingResult.state) {
												case OK: {
													// both calls worked
													lat[0] = latency
															.doubleValue();
													cbDone
															.call(
																	pingResult,
																	Double
																			.valueOf(lat[0]),
																	Long
																			.valueOf(timer));
													break;
												}
												case TIMEOUT:
												case ERROR: {
													cbDone
															.call(
																	pingResult,
																	Double
																			.valueOf(lat[0]),
																	Long
																			.valueOf(timer));
													break;
												}
												}
											}
										});
							} else {
								// use app RTT

								CollectRequestMsg msg = new CollectRequestMsg(
										Ninaloader.me);
								final long sendStamp = System.nanoTime();

							/*	log.info("Sending gossip request to "
										+ neighbor);*/
								log.debug("doPing "+neighbor);
								comm.sendRequestMessage(msg, neighbor,
										new ObjCommRRCB<CollectResponseMsg>() {

											protected void cb(
													CBResult result,
													final CollectResponseMsg responseMsg,
													AddressIF remoteAddr,
													Long ts) {

												double rtt = (System.nanoTime() - sendStamp) / 1000000d;

												switch (result.state) {
												case OK: {
													lat[0] = rtt;
													cbDone
															.call(
																	result,
																	Double
																			.valueOf(lat[0]),
																	Long
																			.valueOf(timer));
													break;
												}
												case TIMEOUT:
												case ERROR: {
													cbDone
															.call(
																	result,
																	Double
																			.valueOf(lat[0]),
																	Long
																			.valueOf(timer));
													break;
												}
												}
											}
										});

							}

							break;
						}
						case TIMEOUT:
						case ERROR: {
							log.error("Could not resolve my address: "
									+ result.what);
							cbDone.call(result, Double.valueOf(lat[0]), Long
									.valueOf(timer));
							break;
						}
						}
					}
				});

	}

	/**
	 * if the address is cached
	 * @param target
	 * @param lat
	 * @return
	 */
	public boolean isCached(AddressIF target,double[] lat,double curLatency){
		final AddressIF neighbor = target;
		//=========================================
		final long timer = System.currentTimeMillis();
		//speed up the ping process
		// history of pendingLatency
		
/*		synchronized(pendingLatency){
			
		if(pendingLatency.containsKey(neighbor)&&
				(timer-pendingLatency.get(neighbor).getLastUpdateTime())< ConcentricRing.MAINTENANCE_PERIOD){
			if(curLatency>0){
			pendingLatency.get(neighbor).addSample(curLatency);
			}
			lat[0]=pendingLatency.get(neighbor).getSample();
			
			return true;
		}
		}*/
		
	
		/*if(localNC.primaryNC.getRs_map().containsKey(neighbor)){
			
			if(curLatency>0){
				localNC.primaryNC.getRs_map().get(neighbor).addSample(curLatency);
				}
			lat[0]=localNC.primaryNC.getRs_map().get(neighbor).getSample();
			return true;
			
		}else*/ 
	/*	if(AbstractNNSearchManager.pendingNeighbors.containsKey(neighbor)){
			
			if(curLatency>0){
				AbstractNNSearchManager.pendingNeighbors.get(neighbor).addSample(curLatency);
			}
			lat[0]=AbstractNNSearchManager.pendingNeighbors.get(neighbor).getSample();
			return true;
			
		} else {
			lat[0]=-1;
			return false;
		}*/
		
		return false;
	}
	
	
	/**
	 * ping several time, 
	 * @param target
	 * @param cbDone
	 */
	public void Ping(AddressIF target,  final CB1<Double> cbDone){
		
		int count=2;
		
		final Vector<Double> vec=new Vector<Double>(3);
		final Barrier barrier=new Barrier(true);
		barrier.setNumForks(count);
		for(int i=0;i<count;i++){
			doPing(target, new CB2<Double, Long> (){
				
				protected void cb(CBResult result, Double arg1, Long arg2) {
					// TODO Auto-generated method stub
					
					if(arg1.doubleValue()>=0){
						
						vec.add(arg1);
					}
										
					barrier.join();
				}

			});
		}
		long time=3*1000;
		EL.get().registerTimerCB(barrier,time,new CB0() {
			protected void cb(CBResult result) {
				double val=-1;
				if(vec.isEmpty()){
					cbDone.call(CBResult.ERROR(), Double.valueOf(val));
					
				}else{
				for(Double v: vec){
					val+=v.doubleValue();
				}
				double avg=val/vec.size();
				cbDone.call(CBResult.OK(), Double.valueOf(avg));
				}
			}
		});
		
	}
	/**
	 * ping a specific node 
	 * @param target
	 * @param cbDone
	 */
	public void doPing(final AddressIF target, final CB2<Double, Long> cbDone) {

		
		
				
			
		final long timer = System.currentTimeMillis();
		
		final double[] lat = new double[1];
		lat[0] = -1;
		
		final AddressIF neighbor = target;
		
		//=========================================
		//speed up the ping process
		//do not use the cached RTT, since it perturbs a lot
		// history of pendingLatency
/*		if(isCached(neighbor,lat,-1)){
			cbDone.call(CBResult.OK(), Double.valueOf(lat[0]), Long
					.valueOf(timer));
			return;
		}*/
		
		
		if (Ninaloader.USE_Ping_Command) {

/*			log.info("ICMP based ping to " + neighbor);
			// ICMP ping
			pingManager.addPingRequest(neighbor, new CB1<Double>() {
				protected void cb(CBResult pingResult, Double latency) {
					final long timer = System.nanoTime();

					switch (pingResult.state) {
					case OK: {
						// both calls worked
						lat[0] = latency.doubleValue();
						// latency
						
						if (!pendingLatency.containsKey(neighbor)) {
							pendingLatency.put(neighbor,
									new RemoteState<AddressIF>(neighbor));
						}
						pendingLatency.get(neighbor).addSample(lat[0], timer);
						lat[0] = pendingLatency.get(neighbor).getSample();

						cbDone.call(pingResult, Double.valueOf(lat[0]), Long
								.valueOf(timer));
						break;
					}
					case TIMEOUT:
					case ERROR: {
						cbDone.call(pingResult, Double.valueOf(lat[0]), Long
								.valueOf(timer));
						break;
					}
					}
				}
			});*/
			//icmp Ericfu
			
			String ip=NetUtil.byteIPAddrToString(((NetAddress)target).getByteIPAddr());
			if(ip==null||ip.isEmpty()){
				ip=target.getHostname();;
			}
			if(ip==null||ip.isEmpty()){
				log.warn("empty address");
				cbDone.call(CBResult.ERROR(), Double.valueOf(-1),
						Long.valueOf(timer));
				return;
			}else{
			MainGeneric.doPing(ip, new CB1<Double>(){

				
				protected void cb(CBResult result, Double arg1) {
					// TODO Auto-generated method stub
					switch(result.state){
					case OK:{
										
						cbDone.call(result, arg1,
								Long.valueOf(timer));
						break;
					}
					default:{
						
						cbDone.call(result,Double.valueOf(-1), Long.valueOf(timer));
						break;
					}
										
					}
						
				}
			
			});
			}
		} else {
			// use app RTT

			CollectRequestMsg msg = new CollectRequestMsg(Ninaloader.me);
			final long sendStamp = System.nanoTime();

			// log.info("Sending gossip request to " + neighbor);
			log.debug("doPing "+neighbor);
			comm.sendRequestMessage(msg, neighbor,
					new ObjCommRRCB<CollectResponseMsg>() {

						protected void cb(CBResult result,
								final CollectResponseMsg responseMsg,
								AddressIF remoteAddr, Long ts) {

							double rtt = (System.nanoTime() - sendStamp) / 1000000d;

							final long timer = System.nanoTime();

							switch (result.state) {
							case OK: {
								lat[0] = rtt;
								
								synchronized(pendingLatency){
								// latency
								if (!pendingLatency.containsKey(neighbor)) {
									pendingLatency
											.put(neighbor,
													new RemoteState<AddressIF>(
															neighbor));
								}
								pendingLatency.get(neighbor).addSample(lat[0],
										timer);
								lat[0] = pendingLatency.get(neighbor)
										.getSample();
								}
								

								cbDone.call(result, Double.valueOf(lat[0]),
										Long.valueOf(timer));
								break;
							}
							case TIMEOUT:
							case ERROR: {

								cbDone.call(result, Double.valueOf(lat[0]),
										Long.valueOf(timer));
								break;
							}
							}
						}
					});

					}

	}

	// help function
	/**
	 * maintain pending latency measurements
	 */
/*	public synchronized void maintainPendingLatencies(long timer) {
		// TODO Auto-generated method stub
		synchronized(pendingLatency){
		Iterator<Entry<AddressIF, RemoteState<AddressIF>>> ier = pendingLatency
				.entrySet().iterator();
		while (ier.hasNext()) {
			Entry<AddressIF, RemoteState<AddressIF>> tmp = ier.next();
			if (timer - tmp.getValue().getLastUpdateTime() > ConcentricRing.MAX_PING_RESPONSE_TIME) {
				ier.remove();
			}
		}
		}
	}*/

	public long lastUpdate = 0;

	AddressIF getUpNeighbor() {
		if (upNeighbors.size() == 0 && pendingNeighbors.size() == 0) {
			log.warn("we are lonely and have no one to gossip with");
			return null;
		}

		final double pctUsePendingNeighbor = 0.1;

		AddressIF upNeighbor;
		if (upNeighbors.size() == 0
				|| (pendingNeighbors.size() > 0 && Ninaloader.random
						.nextDouble() < pctUsePendingNeighbor)) {
			upNeighbor = PUtil.getRandomObject(pendingNeighbors);
			log.debug("getUpNeighbor using pending: " + upNeighbor);
		} else {
			upNeighbor = PUtil.getRandomObject(upNeighbors);
			log.debug("getUpNeighbor using up: " + upNeighbor);
		}

		return upNeighbor;
	}

	public Set<AddressIF> getUpNeighbors() {
		Set<AddressIF> nodes = new HashSet<AddressIF>();
		// LOWTODO add option of loop here
		AddressIF node = getUpNeighbor();
		if (node != null) {
			nodes.add(node);
		}
		return nodes;
	}

	public void addPendingNeighbors(Set<AddressIF> nodes) {
		for (AddressIF node : nodes) {
			addPendingNeighbor(node);
		}
	}

	// If this guy is in an unknown state add him to pending.
	public void addPendingNeighbor(AddressIF node) {
		assert node != null : "Pending neighbour is null?";
		
		if(pendingNeighbors.size()>StableManager.maxPending){
			List<AddressIF> nodes=new ArrayList<AddressIF>(2);
			nodes.addAll(pendingNeighbors);
			Iterator<AddressIF> ier = nodes.iterator();
			int counter=pendingNeighbors.size()-StableManager.maxPending+5;
			while(ier.hasNext()&&counter>0){
				ier.remove();
				counter--;
			}
			pendingNeighbors.clear();
			pendingNeighbors.addAll(nodes);
			
			nodes.clear();
		}

		if (node.equals(comm.getLocalAddress())||node.equals(Ninaloader.me))
			return;
		if (!pendingNeighbors.contains(node) && !upNeighbors.contains(node)
				&& !downNeighbors.contains(node)) {
			pendingNeighbors.add(node);
			log.debug("addPendingNeighbor: " + node);
		} else {
			log.debug("!addPendingNeighbor: " + node);
		}
		if (WATCH_NEIGHBORS)
			dumpNeighbors();
	}

	public void addUpNeighbor(AddressIF node) {
		if (node.equals(comm.getLocalAddress()))
			return;
		downNeighbors.remove(node);
		pendingNeighbors.remove(node);
		upNeighbors.add(node);
		log.debug("addUpNeighbor: " + node);
		if (WATCH_NEIGHBORS)
			dumpNeighbors();
	}

	public void addDownNeighbor(AddressIF node) {
		if (node.equals(comm.getLocalAddress()))
			return;
		pendingNeighbors.remove(node);
		upNeighbors.remove(node);
		downNeighbors.add(node);
		log.debug("addDownNeighbor: " + node);
		if (WATCH_NEIGHBORS)
			dumpNeighbors();
	}

	void dumpNeighbors() {
		log.debug(listNeighbors());
	}

	String listNeighbors() {
		StringBuffer sb = new StringBuffer();
		sb.append("pending:");
		for (AddressIF node : pendingNeighbors) {
			sb.append(" " + node);
		}
		sb.append(" up:");
		for (AddressIF node : upNeighbors) {
			sb.append(" " + node);
		}
		sb.append(" down:");
		for (AddressIF node : downNeighbors) {
			sb.append(" " + node);
		}
		return new String(sb);
	}

	String summarizeNeighbors() {
		StringBuffer sb = new StringBuffer();
		return new String("p= " + pendingNeighbors.size() + " u= "
				+ upNeighbors.size() + " d= " + downNeighbors.size());
	}

	public Set<AddressIF> getPendingNeighbours() {
		return pendingNeighbors;
	}

	public Set<AddressIF> getUpNeighbours() {
		return upNeighbors;
	}

	public Set<AddressIF> getDownNeighbours() {
		return downNeighbors;
	}

	/**
	 * initialize my position by contacting the landmarks
	 */
	public void initMyPosition() {
		List<AddressIF> nodes = new ArrayList<AddressIF>(10);

		if (bootstrapAddrs != null && bootstrapAddrs.size() > 0) {
			Iterator<AddressIF> ier = bootstrapAddrs.iterator();
			while (ier.hasNext()) {
				nodes.add(ier.next());
			}
		}
		int counter = nodes.size();
		// get landmarks from neighbors
		if (counter < numOfLandmarks) {
			Set<AddressIF> tmpNeighbors = getUpNeighbors();
			Iterator<AddressIF> iers = tmpNeighbors.iterator();
			// reach k or empty neighbor lists
			while (iers.hasNext()) {
				if (counter == numOfLandmarks) {
					break;
				}
				AddressIF tmp = iers.next();
				if (!nodes.contains(tmp)) {
					nodes.add(tmp);
					counter++;
				}
			}
		}
		// threshold
		// we require numOfLandmarks landmarks, otherwise, we do not initialize
		if (counter < numOfLandmarks) {
			return;
		}

		getRemoteCoords(nodes, new CB2<Map<AddressIF, Coordinate>, String>() {
			
			protected void cb(CBResult result,
					Map<AddressIF, Coordinate> bootNodes, String arg2) {
				switch (result.state) {
				case OK: {
					// TODO: with remote nodes' coordinates and errors,
					// update my coordinate in the first time
					Set<AddressIF> addrs = new HashSet<AddressIF>();
					Set<Coordinate> remoteLandmarks = new HashSet<Coordinate>();
					Set<Double> r_errors = new HashSet<Double>();
					Set<Double> RTTs = new HashSet<Double>();
					if (bootNodes != null && bootNodes.size() > 0) {
						Iterator<Entry<AddressIF, Coordinate>> entryIers = bootNodes
								.entrySet().iterator();
						if (entryIers.hasNext()) {
							Entry<AddressIF, Coordinate> tmp = entryIers.next();
							addrs.add(tmp.getKey());
							remoteLandmarks.add(tmp.getValue().makeCopy());
							r_errors
									.add(Double.valueOf(tmp.getValue().r_error));
							RTTs.add(Double.valueOf(tmp.getValue().currentRTT));
						}
						if (RTTs.size() > 0) {
							localNC.primaryNC.initFirstCoord(addrs,
									remoteLandmarks, r_errors, RTTs);
							addrs.clear();
							remoteLandmarks.clear();
							r_errors.clear();
							RTTs.clear();
						}
					}

				}
				case ERROR:
				case TIMEOUT: {
					log.warn("Did not initialize the coordinate with- "
							+ bootstrapAddrs.toString());

					break;
				}
				}

			}
		});
		nodes.clear();

	}

/*	// ------------------------------
	// API
	// -----------------------------
	//choiceofCoord 0 NinaCoord, 1 Vivaldi
	public void estimateRTTWithErrorElapsedTime(final Vector<String>  CNodes,
			final int choiceofCoord, final CB1<Vector> cbsortedNodes){
	
	
		int i=0;
		List<AddressIF> AllAddrs=new ArrayList<AddressIF>(10);
		for(i=0;i<CNodes.size();i++){
			 AllAddrs.add(AddressFactory.createUnresolved(CNodes.get(i), Ninaloader.COMM_PORT));
			
		}
		
		//final Map<String, Object> resultMap = new Hashtable<String, Object>();
		final Vector resultVector=new Vector(2);
		


							// StringBuffer dnsErrors = new StringBuffer();
						//	List<AddressIF> remoteNodes = new ArrayList<AddressIF>();

						
							
							
							final double curTime=System.nanoTime();
							
							getRemoteCoords(
									 AllAddrs,
									new CB2<Map<AddressIF, Coordinate>, String>() {
										protected void cb(
												CBResult ncResult,
												Map<AddressIF, Coordinate> addr2coord,
												String errorString) {
											// TODO this does not work
											// We want to return DNS failures to
											// the client

											// combine the two errors if there
											// is already one in there
											// caused through DNS resolution
											// problem below
														

											// get the coordinates of others
											
											//myCoord
											
											
												SortedList<NodeRecord> sortedL = new SortedList<NodeRecord>(
														new EntryComarator());

												for (Entry<AddressIF, Coordinate> Entry : addr2coord
														.entrySet()) {
													
													AddressIF entry=Entry.getKey();
													// myselfy
													if (Ninaloader.me.equals(Entry.getKey())) {
														continue;
													}

													Coordinate me=null;
													Coordinate you=null;
													double myError=-1;
													double yourError=-1;
													
													if(choiceofCoord==0){
														
														//synchronized(localNC.primaryNC){
															
														me=localNC.primaryNC.sys_coord;
														you=Entry.getValue();
														myError=localNC.primaryNC.getSystemError();
														yourError=you.r_error;
														
														//}
														
														
													}else if(choiceofCoord==1){
														
														//synchronized(localVivaldi.primaryNC){
															
														me=localVivaldi.primaryNC.sys_coord;
														you=Entry.getValue().VivaldiCoord;
														
														myError=localVivaldi.primaryNC.getSystemError();
														yourError=you.r_error;
														
														//}
														
													}else{
														//unknown choice
														continue;
													}
													
													if(me==null||you==null){
														continue;
														
													}else{
													//estimated RTT
													NodeRecord r = new NodeRecord(entry,me.distanceTo(you));
													
													
													r.RTT=Entry.getValue().currentRTT;
													r.error=Math.abs((r.coordDist-r.RTT)/r.RTT);
													r.RTT_received=Entry.getValue().receivedRTT;
													r.elapsedTime=Entry.getValue().receivedRTT;
													
													//sortedL.add(r);
													resultVector.add(r.addr.toString()); //remote node
													//resultVector						
													resultVector.add(r.coordDist);	//estimated RTT
													resultVector.add(r.RTT);      	//real RTT
													//resultVector.add(r.RTT-r.coordDist);
													resultVector.add(Math.abs((r.coordDist-r.RTT)/r.RTT));
													resultVector.add(r.elapsedTime);
													
												}
												//Vector<String> sortedPerNode = new Vector<String>(1);
												//Iterator<NodeRecord> ier = sortedL.iterator();
																								
//													sortedPerNode.add((AddressFactory.create(ier.next().addr))
//																	.toString());
													//format: addr, estimated RTT, absolute error time, received RTT
													
												
												
												//double time=(System.nanoTime()-curTime)/1000000d;
												
												
											cbsortedNodes.call(ncResult,resultVector);
											//sortedL.clear();
											
										}
									});
//							break;
//						}
//
//						case ERROR: {
//							// problem resolving one or more of the addresses
//							log.warn(result.toString());
//					
//							cbsortedNodes.call(CBResult.ERROR(), resultVector);
//						}
//						}
//					}
//				}
//);

			
	//	});
	
	}*/
	
	// TODO: provide sort interface
	public void sortNodesByDistances(final Hashtable<String, Object> remoteNodesMap,
			final int k, final CB1<Vector> cbsortedNodes) {

		String[] nodesStr = new String[remoteNodesMap.size()];
		int i = 0;
		for (Object obj : remoteNodesMap.keySet()) {
			String remoteNode = (String) obj;
			log.debug("getRemoteCoords[" + i + "]= " + remoteNode);
			nodesStr[i] = remoteNode;
			i++;
		}

		//final Map<String, Object> resultMap = new Hashtable<String, Object>();
		final Vector resultVector=new Vector(2);
		
		
		
		AddressFactory.createResolved(Arrays.asList(nodesStr),Ninaloader.COMM_PORT,
				new CB1<Map<String, AddressIF>>() {
					protected void cb(CBResult result,
							Map<String, AddressIF> addrMap) {
						switch (result.state) {
						case OK:

							// TODO jonathan: why do you handle OK and TIMEOUT
							// together and why is there an ERROR clause?
							// The way createResolved works is that it always
							// returns OK but only includes the hostnames
							// that resolved correctly in the map. Let me know
							// if you don't like this behaviour and I can change
							// it.
						case TIMEOUT: {

							// StringBuffer dnsErrors = new StringBuffer();
							List<AddressIF> remoteNodes = new ArrayList<AddressIF>();

							for (AddressIF node : addrMap.values()) {
								if (node != null && node.hasPort()) {
									remoteNodes.add(node);
								}
								// else {
								// dnsErrors.append("Could not resolve "+node);
								// }
							}

							getRemoteCoords(
									remoteNodes,
									new CB2<Map<AddressIF, Coordinate>, String>() {
										protected void cb(
												CBResult ncResult,
												Map<AddressIF, Coordinate> addr2coord,
												String errorString) {
											// TODO this does not work
											// We want to return DNS failures to
											// the client

											// combine the two errors if there
											// is already one in there
											// caused through DNS resolution
											// problem below
														

											// get the coordinates
											for (Map.Entry<AddressIF, Coordinate> curEntry : addr2coord
													.entrySet()) {
												SortedList<NodeRecord> sortedL = new SortedList<NodeRecord>(
														new EntryComarator());

												for (Map.Entry<AddressIF, Coordinate> Entry : addr2coord
														.entrySet()) {
													// myselfy
													if (curEntry
															.getKey()
															.equals(
																	Entry
																			.getKey())) {
														continue;
													}

													NodeRecord r = new NodeRecord(
															Entry.getKey(),
															Entry
																	.getValue()
																	.distanceTo(
																			curEntry
																					.getValue()));
													r.RTT=Entry.getValue().currentRTT;
													sortedL.add(r);

												}
												Vector<String> sortedPerNode = new Vector<String>(
														1);
												Iterator<NodeRecord> ier = sortedL
														.iterator();
												
												//resultVector.add("######################");
												//resultVector.add(Ninaloader.me.toString());
												//resultVector.add("######################");
												
												int counter=0;
												while (ier.hasNext()) {
//													sortedPerNode.add((AddressFactory.create(ier.next().addr))
//																	.toString());
													if((k>0)&&(counter==k)){
														break;
													}
													NodeRecord r=ier.next();
																		
													resultVector.add(NetUtil.byteIPAddrToString(r.addr.getByteIPAddrPort())); //remote node
													resultVector.add(r.RTT);													
													resultVector.add(r.coordDist);
													resultVector.add(counter);
													counter++;
													
												}
												String separator="\n######################################\n";
												resultVector.add(separator);
												sortedL.clear();
												sortedL=null;
											}

											cbsortedNodes.call(CBResult.OK(),
													resultVector);
										}
									});
							break;
						}

						case ERROR: {
							// problem resolving one or more of the addresses
							log.warn(result.toString());
					
							cbsortedNodes.call(CBResult.ERROR(), resultVector);
						}
						}
					}
				});

	}

	class NodeRecord {
		AddressIF addr;
		double coordDist;
		double RTT;
		double error;
		double RTT_received;
		double elapsedTime;

		NodeRecord(AddressIF _addr, double _coordDist) {
			addr = _addr;
			coordDist = _coordDist;
		}
	}

	class EntryComarator implements Comparator {

		
		public int compare(Object o1, Object o2) {
			// TODO Auto-generated method stub
			double v1 = ((NodeRecord) o1).coordDist;
			double v2 = ((NodeRecord) o2).coordDist;

			if (v1 < v2) {
				return -1;
			} else if (v1 == v2) {
				return 0;
			} else {
				return 1;
			}

		}

	}

	/**
	 * The first item is target
	 */
	public void sortNodesAccordingToTarget(String target, final String[] peers,
			final CB2<Vector<String>, Vector<Double>> cbDone) {

		final Vector<String> addrs = new Vector<String>(1);
		final Vector<Double> estRTTs = new Vector<Double>(1);

		String[] Target = { target };
		AddressFactory.createResolved(target, Ninaloader.COMM_PORT,
				new CB1<AddressIF>() {

					
					protected void cb(CBResult result, AddressIF TT) {
						// TODO Auto-generated method stub
						switch (result.state) {
						case OK: {
							System.out.println("Target: " + TT);

							final AddressIF TargetAddr = TT;

							AddressFactory.createResolved(Arrays.asList(peers),
									Ninaloader.COMM_PORT,
									new CB1<Map<String, AddressIF>>() {
										protected void cb(CBResult result,
												Map<String, AddressIF> addrMap) {
											switch (result.state) {
											case OK:

												// TODO jonathan: why do you
												// handle OK and TIMEOUT
												// together and why is there an
												// ERROR clause?
												// The way createResolved works
												// is that it always returns OK
												// but only includes the
												// hostnames
												// that resolved correctly in
												// the map. Let me know if you
												// don't like this behaviour and
												// I can change it.
											case TIMEOUT: {

												// StringBuffer dnsErrors = new
												// StringBuffer();
												List<AddressIF> remoteNodes = new ArrayList<AddressIF>();
												remoteNodes.add(TargetAddr);

												for (AddressIF node : addrMap
														.values()) {
													if (node != null
															&& node.hasPort()) {
														System.out
																.println("node: "
																		+ node
																				.toString());
														remoteNodes.add(node);
													}
													// else {
													// dnsErrors.append("Could not resolve "+node);
													// }
												}

												getRemoteCoords(
														remoteNodes,
														new CB2<Map<AddressIF, Coordinate>, String>() {
															protected void cb(
																	CBResult ncResult,
																	Map<AddressIF, Coordinate> addr2coord,
																	String errorString) {
																// TODO this
																// does not work
																// We want to
																// return DNS
																// failures to
																// the client

																// combine the
																// two errors if
																// there is
																// already one
																// in there
																// caused
																// through DNS
																// resolution
																// problem below
																switch (ncResult.state) {
																case OK: {
																	System.out
																			.println("responding to client addr2coord="
																					+ addr2coord
																							.size()
																					+ " errorString="
																					+ errorString);

																	// get the
																	// coordinates
																	Coordinate addr = addr2coord
																			.get(TargetAddr);
																	if (addr != null) {
																		SortedList<NodeRecord> sortedL = new SortedList<NodeRecord>(
																				new EntryComarator());

																		for (Map.Entry<AddressIF, Coordinate> Entry : addr2coord
																				.entrySet()) {

																			// myselfy
																			if (TargetAddr
																					.equals(Entry
																							.getKey())) {
																				continue;
																			}

																			NodeRecord r = new NodeRecord(
																					Entry
																							.getKey(),
																					Entry
																							.getValue()
																							.distanceTo(
																									addr));
																			sortedL
																					.add(r);

																		}

																		Iterator<NodeRecord> ier = sortedL
																				.iterator();
																		double dist = -1;
																		while (ier
																				.hasNext()) {
																			NodeRecord r = ier
																					.next();
																			addrs
																					.add(r.addr
																							.toString());

																			dist = r.coordDist;
																			if (addr.isHyperbolic) {
																				dist = r.coordDist
																						* addr.num_curvs;
																			}

																			estRTTs
																					.add(Double
																							.valueOf(dist));
																		}

																	}

																	cbDone
																			.call(
																					CBResult
																							.OK(),
																					addrs,
																					estRTTs);
																	break;
																}
																case TIMEOUT:
																case ERROR: {
																	log
																			.warn(ncResult
																					.toString());
																	cbDone
																			.call(
																					CBResult
																							.OK(),
																					addrs,
																					estRTTs);
																	break;
																}

																}

															}
														});
												break;
											}

											case ERROR: {
												// problem resolving one or more
												// of the addresses
												log.warn(result.toString());
												cbDone.call(CBResult.OK(),
														addrs, estRTTs);
											}
											}
										}
									});
							break;
						}
						case TIMEOUT:
						case ERROR: {
							log.error("Could not resolve my address: "
									+ result.what);
							cbDone.call(result, addrs, estRTTs);
							break;
						}
						}
					}

				});

	}

	/**
	 * Fetch coordinate from remote node.
	 */
	public void getRemoteCoord(final AddressIF remoteNode,
			final CB1<Coordinate> cbCoord) {

		if (remoteNode.equals(Ninaloader.me)) {
			cbCoord.call(CBResult.OK(), localNC.primaryNC.getSystemCoords());
			return;
		}
		log.debug("getRemoteCoord" +remoteNode);
		comm.sendRequestMessage(new CoordRequestMsg(Ninaloader.me, null,-1),
				remoteNode, new ObjCommRRCB<CoordResponseMsg>() {
					protected void cb(CBResult result,
							final CoordResponseMsg resp, AddressIF remoteAddr,
							Long ts) {
						switch (result.state) {
						case OK: {
							log.debug("received coord back from " + remoteNode);
							cbCoord.call(CBResult.OK(), resp.coord);
							break;
						}
						case TIMEOUT:
						case ERROR: {
							log.warn("Coord request to " + remoteNode
									+ " failed");
							cbCoord.call(result, null);
						}
						}
					}
				});
	}

	// (1): Get Coordinate
	public void getRemoteCoords(final Collection<AddressIF> nodes,
			final CB2<Map<AddressIF, Coordinate>, String> cbCoords) {
		
		final Map<AddressIF, Coordinate> addr2coord = new HashMap<AddressIF, Coordinate>();
		if (nodes.size() == 0) {
			String errorString = "getRemoteCoords: no valid nodes";
			log.warn(errorString);
			cbCoords.call(CBResult.OK(), addr2coord, errorString);
			return;
		}

		
		final Barrier barrier = new Barrier(true);
		final StringBuffer errorBuffer = new StringBuffer();
		
		//nodes
		barrier.setNumForks(nodes.size());
		
		for (AddressIF addr : nodes) {
			getRemoteOrLocalCoordinate(addr, barrier, addr2coord, errorBuffer);
		}
		
		long time=5*1000;
		EL.get().registerTimerCB(barrier,time, new CB0() {
			protected void cb(CBResult result) {
				String errorString;
				if (errorBuffer.length() == 0) {
					errorString = new String("Success");
				} else {
					errorString = new String(errorBuffer);
				}
				cbCoords.call(CBResult.OK(), addr2coord, errorString);
			}
		});
		
	}

	 void getRemoteOrLocalCoordinate1(final AddressIF node,
				final Barrier barrier, final Map<AddressIF, Coordinate> addr2coord,
				final StringBuffer errorBuffer) {

			
			 
			System.out.println("getRemoteOrLocalCoordinate node=" + node);
			// myself
			if (node.equals(Ninaloader.me)) {
				 
				addr2coord.put(node, getLocalCoord());
				
				
				System.out.println("got local coord " + getLocalCoord());
				
				barrier.join();
				
				return;
			}
			
				
				try{
					//barrier.fork();
				comm.sendRequestMessage(new CoordRequestMsg(Ninaloader.me, null,-1), node,
						new ObjCommRRCB<CoordResponseMsg>() {
							protected void cb(CBResult result,
									final CoordResponseMsg resp, AddressIF remoteAddr,
									Long ts) {
								
								
								switch (result.state) {
								case OK: {
									
									
									
														
									resp.coord.r_error = resp.error;
									// ------------------------------
									System.out.println("received coord back from "
											+ node + " coord=" + resp.coord);
									addr2coord.put(node, resp.coord);
									
									
									break;
								}
								case TIMEOUT:
								case ERROR: {
									/*String error = "Coord request to "
											+ node.toString(false) + " failed:"
											+ result.toString();
									System.err.println(error);
									if (errorBuffer.length() != 0) {
										errorBuffer.append(",");
									}
									errorBuffer.append(error);*/

									// Remove the barrier to clear up state
									// barrier.remove();
									break;
								}
								}
															
								//barrier
								barrier.join();
													
								
							}
						});	
				}catch( ConcurrentModificationException e){
					e.printStackTrace();
				}
				
				}
				
	
	  void getRemoteOrLocalCoordinate(final AddressIF node,
			final Barrier barrier, final Map<AddressIF, Coordinate> addr2coord,
			final StringBuffer errorBuffer) {

		
		 
		System.out.println("getRemoteOrLocalCoordinate node=" + node);
		// myself
		if (node.equals(Ninaloader.me)) {
			
			addr2coord.put(node, getLocalCoord());
			
			
			System.out.println("got local coord " + getLocalCoord());
			
			barrier.join();
			
			return;
		}
		
		
		/*doPing(node,new  CB2<Double, Long>(){

			
			protected void cb(CBResult arg0, Double arg1, Long arg2) {
				// TODO Auto-generated method stub
			final double rtt=arg1.doubleValue();
		
				
			if(rtt<=0){
				
				//wrong 
				barrier.join();
				
				return;
			}else{*/
			// remote access
			//barrier.fork();
				
			final long sendStamp = System.nanoTime();
			
		
			try{
			comm.sendRequestMessage(new CoordRequestMsg(Ninaloader.me, null,-1), node,
					new ObjCommRRCB<CoordResponseMsg>() {
						protected void cb(CBResult result,
								final CoordResponseMsg resp, AddressIF remoteAddr,
								Long ts) {
							
							
							switch (result.state) {
							case OK: {
								
								
							
									
								double rtt_received = (System.nanoTime() - sendStamp) / 1000000d;
								/*System.out.println("app rtt of " + rtt + " to "
										+ node);*/
								resp.coord.currentRTT = rtt_received;
								resp.coord.r_error = resp.error;
								resp.coord.elapsedTime=rtt_received;
								// ------------------------------
								System.out.println("received coord back from "
										+ node + " coord=" + resp.coord);
								addr2coord.put(node, resp.coord);
								
								
								
								break;
							}
							case TIMEOUT:
							case ERROR: {
								/*String error = "Coord request to "
										+ node.toString(false) + " failed:"
										+ result.toString();
								System.err.println(error);
								if (errorBuffer.length() != 0) {
									errorBuffer.append(",");
								}
								errorBuffer.append(error);*/

								// Remove the barrier to clear up state
								// barrier.remove();
								break;
							}
							}
														
							//barrier
							barrier.join();
												
							
						}
					});	
			
			}catch( ConcurrentModificationException e){
				e.printStackTrace();
			}
			
			
			
		/*	}
			//=========================================
			
			
			}
								
		} );*/
		
		

	}

	// (2): estimate distance between two nodes
	public void estimateRTT(final AddressIF nodeA, final AddressIF nodeB,
			final CB1<Double> cbDistance) {
		
		if(nodeA==null||nodeB==null){
			cbDistance.call(CBResult.ERROR(), Double.valueOf(-1));
			return;
		}
		
		log.debug("estimateRTT a=" + nodeA + " b=" + nodeB);

		//heuristic
		double eRTT=ifSameSubsetThenDo(nodeA,nodeB);
		if(eRTT>=0){
			
			cbDistance.call(CBResult.OK(), Double.valueOf(eRTT));
			return;
		}
		// Creates a new barrier, which is triggered by default, i.e. if there's
		// no call
		// to fork, then the barrier CB will be executed immediately.
		final Barrier barrier = new Barrier(true);
		final Map<AddressIF, Coordinate> addr2coord = new HashMap<AddressIF, Coordinate>();
		final StringBuffer errorBuffer = new StringBuffer();
		//fork 2 
		barrier.setNumForks(2);
		
		getRemoteOrLocalCoordinate1(nodeA, barrier, addr2coord, errorBuffer);
		getRemoteOrLocalCoordinate1(nodeB, barrier, addr2coord, errorBuffer);

		EL.get().registerTimerCB(barrier, new CB0() {
			protected void cb(CBResult result) {

				Coordinate coordA = addr2coord.get(nodeA);
				Coordinate coordB = addr2coord.get(nodeB);

				if (coordA != null && coordB != null) {

					double distance = coordA.distanceTo(coordB);
					if (coordA.isHyperbolic) {
						distance *= coordA.num_curvs;
					}
					System.out.println("distance= " + distance);
					cbDistance.call(CBResult.OK(), Double.valueOf(distance));
				} else {
					String errorString = new String(errorBuffer);
					System.out.println(errorString);
					cbDistance
							.call(CBResult.ERROR(errorString), new Double(-1));
				}

			}
		});
	}

	//Ericfu - add heuristic
	/**
	 * -1, not in the same subnet
	 * >0  yes
	 */
	public double ifSameSubsetThenDo(final AddressIF nodeA, final AddressIF nodeB){
		return MainGeneric.ifSameSubsetThenDo(nodeA, nodeB);
	}
	
	public void estimateRTT(final String ANode, final String BNode,
			final CB1<Double> cbDistance) {

		AddressFactory.createResolved(Arrays.asList(new String[] {ANode, BNode}), Ninaloader.COMM_PORT,new CB1<Map<String, AddressIF>>() {
			protected void cb(CBResult result, Map<String, AddressIF> addrMap) {
				switch (result.state) {
					case OK: {
						log.debug("estimateRTT a="+ANode+" b="+BNode);
						if (addrMap.get(ANode) == null) {
							String error = "Could not resolve a="+ANode;
							log.warn(error);
							cbDistance.call(CBResult.ERROR(error),new Double(0));
						} else if (addrMap.get(BNode) == null) {
							String error = "Could not resolve b="+BNode;
							log.warn(error);
							cbDistance.call(CBResult.ERROR(error),new Double(0));
						} else {
							estimateRTT(addrMap.get(ANode), addrMap.get(BNode), cbDistance);
						}
						break;
					}
					case TIMEOUT:
					case ERROR: {
						log.warn(result.toString());
						cbDistance.call(result,new Double(0));
					}
				}
			}			
		});
		
	

	}

}
