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
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.NUDT.pdl.Nina.Clustering.HSH_Manager;
import edu.NUDT.pdl.Nina.KNN.DatRequestMsg;
import edu.NUDT.pdl.Nina.KNN.KNNManager;
import edu.NUDT.pdl.Nina.KNN.AbstractNNSearchManager;
import edu.NUDT.pdl.Nina.KNN.MeridianSearchManager;
import edu.NUDT.pdl.Nina.KNN.UpdateRequestMsg;
import edu.NUDT.pdl.Nina.KNN.multiObjKNN.MultiTargetKNNManager;
import edu.NUDT.pdl.Nina.StableNC.nc.StableManager;
import edu.NUDT.pdl.Nina.api.APIManager;
import edu.NUDT.pdl.Nina.log.LogManager;
import edu.NUDT.pdl.Nina.net.appPing.PingNodes;
import edu.NUDT.pdl.Nina.net.lossMeasure.packetTrainComm;
import edu.NUDT.pdl.Nina.util.MainGeneric;
import edu.NUDT.pdl.Nina.util.MathUtil;


import edu.NUDT.pdl.pyxida.ping.PingManager;
import edu.harvard.syrah.prp.ANSI;
import edu.harvard.syrah.prp.Log;
import edu.harvard.syrah.prp.PUtil;
import edu.harvard.syrah.sbon.async.CBResult;
import edu.harvard.syrah.sbon.async.Config;
import edu.harvard.syrah.sbon.async.EL;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB0;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB1;
import edu.harvard.syrah.sbon.async.EL.Priority;
import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjComm;
import edu.harvard.syrah.sbon.async.comm.obj.ObjCommIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjUDPComm;

public class Ninaloader {
	private static final Log log = new Log(Ninaloader.class);

	private static final int MAJOR_VERSION = 0;
	private static final int MINOR_VERSION = 1;
	private static final int MINOR_MINOR_VERSION = 4;
	public static final String VERSION = MAJOR_VERSION + "." + MINOR_VERSION
			+ "." + MINOR_MINOR_VERSION;

	static {

		// All config properties in the file must start with 'HSHer.'

		Config.read("Nina", System
				.getProperty("Nina.config", "config/Nina.cfg"));
	}

	
	public static final String DEFAULT_PING_HOSTNAME = Config
	.getConfigProps().getProperty("DEFAULT_PING_HOSTNAME", "www.google.com");
	
	// Imperial blocks ports outside of 55000-56999
	public static final int COMM_PORT = Integer.parseInt(Config
			.getConfigProps().getProperty("port", "55504"));

	public static final int MeasureCOMM_PORT = Integer.parseInt(Config
			.getConfigProps().getProperty("MeasureCOMM_PORT", "55515"));
	
	public static final int Gossip_PORT = Integer.parseInt(Config
			.getConfigProps().getProperty("Gossip_PORT", "55515"));
	
	public static final int UDPCOMM_PORT= Integer.parseInt(Config
			.getConfigProps().getProperty("UDPCOMM_PORT", "55516"));
	
	public static final boolean USE_ICMP = Boolean.parseBoolean(Config
			.getConfigProps().getProperty("use_icmp", "false"));
	
	public static final boolean USE_NetCoord = Boolean.parseBoolean(Config
			.getConfigProps().getProperty("USE_NetCoord", "false"));
	
	public static final boolean USE_Ping_Command = Boolean.parseBoolean(Config
			.getConfigProps().getProperty("use_PingCommand", "false"));

	public static final String[] myRegion = Config.getConfigProps()
			.getProperty("myDNSAddress", "").split("[\\s]");
	public static AddressIF me;

	private static final long START_TIME = System.currentTimeMillis();

	// private Ninaloader NiNAer = null;

	// private NCManager_HSH_v1 ncManager;
	public StableManager ncManager;
	public AbstractNNSearchManager NNManager;
	public APIManager apiManager;
	private PingManager pingManager;
	private LogManager logManager;
	//private SubSetSearchManager subManager;

	private HSH_Manager clusteringManager;
	public PingNodes pingAllPairs;
	

	private ObjCommIF comm;
	
	private ObjCommIF GossipComm;
	
	private ObjCommIF measureComm;
	//public static packetTrainComm udpComm;

	public static Random random;
	public static MathUtil math = new MathUtil(100);

	// 9-14, for coordinate test
	public boolean testCoordOnly = true;

	private static Ninaloader instance = null;

	public static BufferedWriter logKNN = null;
	public static BufferedWriter logmultiKNN = null;
	public static BufferedWriter logPingHistory = null;
	
	public static BufferedWriter logCoordinate = null;
	public static BufferedWriter logCluster = null;
	
	public static BufferedWriter logControlTraffic=null;
	
	public static String KNNLogName = "KNN";
	public static String multiKNNLogName = "multiKNN";
	public static String PingHistoryLogName="PingHistory";
	public static String CoordinateLogName = "NetCoord";
	public static String ClusteringLogName = "HSHClustering";
	public static String controlLog="controlLog";
	
	public static long StartTime;
	/**
	 * ready threads
	 */
	public static ExecutorService execNina = Executors.newFixedThreadPool(15);
	
	// singleton mode
	public static boolean singletonMode = Boolean.parseBoolean(Config
			.getConfigProps()
			.getProperty("use_oneMeasurementInstance", "false"));;

	/*public static boolean useConstrainedKNN=Boolean.parseBoolean(Config
			.getConfigProps().getProperty("useConstrainedKNN", "false"));;	
	*/
	
	boolean useHSHClustering=false;
	
	
	boolean useTCP=Boolean.parseBoolean(Config
			.getConfigProps()
			.getProperty("useTCP", "true"));;
	
	/**
	 * singleton
	 * 
	 * @return
	 */
	public static Ninaloader getInstance() {
		if (instance == null) {
			instance = new Ninaloader();
		}
		return instance;
	}

	public void join(String hostname, CB0 cbDone) {

		// join the system
		int port = Ninaloader.COMM_PORT;
		// one instance only

		join(hostname, port, cbDone);

	}

	/*
	 * 
	 * join the measurement system, based on its IP address and the IP port
	 */
	public void join(String hostname, int port, final CB0 cbDone) {

		AddressFactory.createResolved(hostname, port, new CB1<AddressIF>() {
			
			protected void cb(CBResult result, AddressIF addr) {
				// TODO Auto-generated method stub
				switch (result.state) {
				case OK: {

					AddressIF remoteAddr = addr;
					me = AddressFactory.create(remoteAddr);

					try {
						init(cbDone);
					} catch (Exception e) {
						e.printStackTrace();
					}

					break;
				}
				case TIMEOUT:
				case ERROR: {
					log.error("Could not resolve my address: " + result.what);
					cbDone.callERROR();
					break;
				}
				}
			}
		});

	}

	public Ninaloader() { /* empty */

		
		try{
			Ninaloader.logPingHistory= new BufferedWriter(new FileWriter(new File(Ninaloader.PingHistoryLogName
					+ ".log")));
			Ninaloader.logKNN = new BufferedWriter(new FileWriter(new File(Ninaloader.KNNLogName
					+ ".log")));
			
			logmultiKNN= new BufferedWriter(new FileWriter(new File(Ninaloader.multiKNNLogName
					+ ".log")));
			
			logControlTraffic= new BufferedWriter(new FileWriter(new File(Ninaloader.controlLog
					+ ".log")));
			
		}catch(IOException e){
			
		}
		
		// add postfix
		// KNNLogName.concat(me.toString());
		StartTime = System.currentTimeMillis();
	}

	private void init(final CB0 cbDone) {
		random = new Random(System.currentTimeMillis());

		
		// Initiliase the ObjComm communication module
		comm = new ObjUDPComm();
		GossipComm =new ObjUDPComm();			
		measureComm=new ObjUDPComm();
		/*if(useTCP){
		//comm = new ObjComm();
		GossipComm = new ObjComm();				
		measureComm=new ObjComm();
		}else{
			
			//comm=new ObjUDPComm();
			GossipComm =new ObjUDPComm();
			measureComm=new ObjUDPComm();
		}*/
		//wildcard address
		//final AddressIF objCommAddr =AddressFactory.createServer(COMM_PORT);
		
		//knn
		final AddressIF objCommAddr =AddressFactory.create(me,COMM_PORT);
		

		
		//udpComm=new packetTrainComm(Priority.NORMAL);
		
		//final AddressIF objCommAddr =AddressFactory.create(me,COMM_PORT);
		//myAddress
		final AddressIF MeasureCommAddr = AddressFactory.create(me, Ninaloader.MeasureCOMM_PORT);
		
		//for gossip
		final AddressIF gossipCommAddr=AddressFactory.create(me, Ninaloader.Gossip_PORT );
		
		
		
		
		
		log.debug("Starting objcomm server...");
		comm.initServer(objCommAddr, new CB0() {
			
			protected void cb(CBResult result) {
				switch (result.state) {
				case OK: {
					// Initialise the measurement modules
					pingManager = new PingManager();
					log.debug("Initialising the PingManager...");
					pingManager.init(new CB0() {
						
						protected void cb(CBResult result) {

							// if test KNN, cancel this, testCoordOnly=false

							// Initialise the NCs that we're responsible for
							// ncManager = new NCManager_HSH_v1(comm,
							// pingManager);
							
							
							
							ncManager = new StableManager(comm, pingManager);
							

							System.out.println("Initializing the NCManger...");
							ncManager.init(new CB0() {

								
								protected void cb(CBResult result) {
									// TODO Auto-generated method stub
									switch (result.state) {
									case OK: {
										System.out
												.println("$: NCManager Initialized");
										
										
										pingAllPairs=new PingNodes(comm,pingManager,ncManager);
										
										// ----------------------------------
										// cancel the KNN search if
										// testCoordOnly=true

										/***************************/

										

										measureComm.initServer(MeasureCommAddr, new CB0() {

											@Override
											protected void cb(CBResult result) {
												// TODO Auto-generated method stub
											switch(result.state){
											case OK:{
																								//=================================================													
												
												//gossip
												GossipComm.initServer(gossipCommAddr, new CB0() {
													
													protected void cb(CBResult result) {
														switch (result.state) {
														case OK: {
															
															
												if(!AbstractNNSearchManager.startMeridian){
												//single target
													if(AbstractNNSearchManager.singleTargetKNN){
												   NNManager = new KNNManager(GossipComm,comm,measureComm,
														pingManager, ncManager
														);
													}else{
														//multiple targets
														  NNManager = new MultiTargetKNNManager(GossipComm,comm,measureComm,
																	pingManager, ncManager);	
													}
												
												}else{
													 NNManager = new MeridianSearchManager(GossipComm,comm,measureComm,
																pingManager, ncManager);
												}
													
												log.info("Initializing the KNN Manger...");
												NNManager.init(new CB0() {
													
													protected void cb(CBResult result) {
														switch (result.state) {
														case OK: {
															
															// Initialise the external
															// APIs
															// Initialise the external
															// APIs
														/*	apiManager = new APIManager(
																	ncManager,
																	NNManager,pingAllPairs);
														execNina.execute(apiManager);	*/													
														try {
															logKNN.append(NNManager.getParams());
														} catch (IOException e) {
															// TODO Auto-generated catch block
															e.printStackTrace();
														}
																						
															cbDone.call(result);																												
															break;
														}

														default: {
															cbDone.call(result);
															break;
														}
														}

													}
												});	
												
												
													//cbDone.call(result);
													break;
														}
													case ERROR:
													case TIMEOUT:{
														cbDone.call(result);
														break;
													}
															}
														}
													});
												
												
												break;
												
												
												}
											default:	
												{
												cbDone.call(result);	
												}
											}

												
												
											}
											
											
											
										});

										// -----------------------------------
										break;
									}
									default: {
										cbDone.call(result);
										break;
									}
									}

								}
							});

						}
					});
					break;
				}
				default: {
					cbDone.call(result);
					log.warn("error to init the server!");
					System.exit(0);
					break;
				}
				}
			}
		});
	}

	public static String getUptimeStr() {
		long now = System.currentTimeMillis();

		return PUtil.getDiffTimeStr(START_TIME, now);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log.main("NiNAer Version " + VERSION + " starting...");

		// TODO JTL: do you GetOpt magic here
		// we need a switch to specify the config file and to turn on debugging

		if (args.length > 0 && args[0].equals("-d")) {
			Log.setPackageRoot(Ninaloader.class);
			String[] newArgs = new String[args.length - 1];
			for (int i = 0; i < newArgs.length; i++)
				newArgs[i] = args[i];
			args = newArgs;
		}

		// Turn on assertions
		boolean assertsEnabled = false;
		assert assertsEnabled = true;
		if (!assertsEnabled)
			log.info(" assertions is not turned on: java -ea ...");

		// Turn on colour support
		ANSI.use(Boolean.valueOf(Config.getConfigProps().getProperty(
				"sbon.console.ansi", "true")));

		/*
		 * Create the event loop
		 */
		EL.set(new EL(Long.valueOf(Config.getConfigProps().getProperty(
				"sbon.eventloop.statedump", "600000")), Boolean.valueOf(Config
				.getConfigProps().getProperty("sbon.eventloop.showidle",
						"false"))));
		
		

		final Ninaloader NiNAer = new Ninaloader();

							try {
								
						 
								AddressFactory.createResolved(Arrays.asList(myRegion),
						   				Ninaloader.COMM_PORT, new CB1<Map<String, AddressIF>>() {
						   					
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
						   
						   							try {
						   								// init
						   								NiNAer.init(new CB0() {
						   									
						   									protected void cb(CBResult result) {
						   										switch (result.state) {
						   										case OK: {
						   											log
						   													.main("NiNAer node initialised successfully");
						   											break;
						   										}
						   										case TIMEOUT:
						   										case ERROR: {
						   											log.error("Could not initialise NiNAer node: "
						   															+ result.toString());
						   											System.exit(0);
						   											break;
						   										}
						   										}
						   									}
						   								});
						   
						   							} catch (Exception e) {
						   								// TODO Auto-generated catch block
						   								e.printStackTrace();
						   							}
						   
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
							}catch(Exception e){
								e.printStackTrace();
							}

		try {
			EL.get().main();
		} catch (OutOfMemoryError e) {
			EL.get().dumpState(true);
			e.printStackTrace();
			log.error("Error: Out of memory: " + e);
		}
		
/*		try {
			
			logPingHistory.close();
			logKNN.close();
			logCoordinate.close();
			logCluster.close();
			logOneWayLoss.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
		
		if(Ninaloader.logPingHistory!=null){
		try{
			Ninaloader.logPingHistory.close();
		}catch(IOException e){
			e.printStackTrace();
		}
		}
		if(Ninaloader.logKNN!=null){
			try{
			Ninaloader.logKNN.close();
			}catch(IOException e){
				e.printStackTrace();
			}
		}
		
		if(logmultiKNN!=null){
			try{
				logmultiKNN.close();
			}catch(IOException e){
				e.printStackTrace();
			}
		}
		
		
		
		log.main("Shutdown");
		System.exit(0);
	}

	public StableManager getNcManager() {
		return ncManager;
	}

	public void setNcManager(StableManager ncManager) {
		this.ncManager = ncManager;
	}

	public AbstractNNSearchManager getNNManager() {
		return NNManager;
	}

	public void setNNManager(AbstractNNSearchManager manager) {
		NNManager = manager;
	}

	public APIManager getApiManager() {
		return apiManager;
	}

	public void setApiManager(APIManager apiManager) {
		this.apiManager = apiManager;
	}

}
