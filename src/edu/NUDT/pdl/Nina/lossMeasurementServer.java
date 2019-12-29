package edu.NUDT.pdl.Nina;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.NUDT.pdl.Nina.net.lossMeasure.packetTrainComm;
import edu.NUDT.pdl.Nina.util.MainGeneric;
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

public class lossMeasurementServer {
	
	private static Log log=new Log(lossMeasurementServer.class);
	
	static {
		/*
		 * All config properties in the file must start with 'Nina.'
		 */
		Config.read("Nina", System.getProperty("config", "config/Nina.cfg"));
	}

	private static final String CONFIG_FILE = System.getProperty("config",
			"config/Nina.cfg");

	public static final int UDPCOMM_PORT= Integer.parseInt(Config
			.getConfigProps().getProperty("UDPCOMM_PORT", "55516"));
	
	public static final boolean useOneWayLoss = Boolean.parseBoolean(Config
			.getConfigProps().getProperty("useOneWayLoss", "false"));
	
	//loss measurement
	public static final int PING_DELAY =Integer.parseInt(Config
			.getConfigProps().getProperty("PING_DELAY", "100"));
	
	//measurement period
	public static final int doOneWayLossMeasurementPeriod =Integer.parseInt(Config
			.getConfigProps().getProperty("doOneWayLossMeasurementPeriod", "60000"));
	
	//total pings
	public static final int PINGCounter =Integer.parseInt(Config
			.getConfigProps().getProperty("PINGCounter", "100"));
	//packetsize
	public static final int packetsize =Integer.parseInt(Config
			.getConfigProps().getProperty("packetsize", "100"));
	
	
	//all nodes
	public static final String AllNodes=Config.getProperty("AllNodes",
	"nodeList4Nina.src");

	final CB0 OneWayLossMeasurementCB;
	
	public List<AddressIF> AllAliveNodes;
			
	public static String oneWayLossLogName = "oneWayLoss";
	public static BufferedWriter logOneWayLoss = null;
	
	public static final String[] myRegion = Config.getProperty("myDNSAddress",
	"").split("[\\s]");
	
	public static AddressIF me;
	private static lossMeasurementServer logServer = null;
	
	
	
	//public static ExecutorService execNina = Executors.newFixedThreadPool(15);
	
	
	
	Random random=new Random();
	
	
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
	
	//======================================================
	public static packetTrainComm udpComm;
	
	
	
	public lossMeasurementServer (){
		
		OneWayLossMeasurementCB= new CB0() {
			protected void cb(CBResult result) {
				log.debug("$: Ready for test Meridian!");
				OneWayLossMeasurement();

			}
		};
		
		
		try{
			logOneWayLoss=new BufferedWriter(new FileWriter(new File(
					oneWayLossLogName + ".log")));
			
		}catch(IOException e){
			e.printStackTrace();
		}
		
		
		
		
	}
	
	/**
	 * parse all nodes
	 */
	void parseAllNodes(int port,final CB0 cbDone){
		AllAliveNodes=new ArrayList<AddressIF>(100);	
		List<String> nodeAddrs = MainGeneric.parseAllNodes(AllNodes, port);
		
		AddressFactory.createResolved(nodeAddrs,
				port, new CB1<Map<String, AddressIF>>() {
					
					protected void cb(CBResult result,
							Map<String, AddressIF> addrMap) {
						switch (result.state) {
						case OK: {
							for (String remoteNode : addrMap.keySet()) {
								// System.out.println("remoteNode='" +
								// remoteNode + "'");
								AddressIF remoteAddr = addrMap.get(remoteNode);
								// we keep these around in case we run out of
								// neighbors in the future
								// not me
								// if(!remoteAddr.equals(Ninaloader.me)){
								//bootstrapAddrs.add(remoteAddr);
								//addPendingNeighbor(remoteAddr);
								// }
								AllAliveNodes.add(remoteAddr);
							}

							cbDone.callOK();
							break;
						}
						case TIMEOUT:
						case ERROR: {
							log.error("Could not resolve bootstrap list: "
									+ result.what);
							cbDone.call(result);
							break;
						}
						}
					}
				});
	}
	
	
	
	public void init(final CB0 cbDone){
		
		//loss measurement
		final AddressIF udpCommAddr =AddressFactory.create(me,UDPCOMM_PORT);
		
		udpComm=new packetTrainComm(Priority.NORMAL);
		log.info("initialize the loss measurement ");
		
		
		parseAllNodes(UDPCOMM_PORT,new CB0(){

			@Override
			protected void cb(CBResult result) {
				// TODO Auto-generated method stub
				switch(result.state){
				case OK:{
					udpComm.initMyServer(udpCommAddr, new CB0(){
						@Override
						protected void cb(CBResult result) {
							// TODO Auto-generated method stub
						switch(result.state){
						case OK:{
							log.info("udpCommA.myContactAddress: "+udpComm.myContactAddress.toString());
							
							if(useOneWayLoss){
								registerOneWayLossMeasurement();
							}
							cbDone.call(result);
							break;
						}
						default:{
							log.warn("udpCommA is not initialized!\t"+result.toString());
							cbDone.call(result);
							break;
						}
						}
						}						
					});
					break;
				}
				default:{
					cbDone.call(result);
					break;
				}
				
				}
			}});
		
		

		
	
	}
	
	
	
	void registerOneWayLossMeasurement(){
		double rnd = random.nextGaussian();
		long delay2 =doOneWayLossMeasurementPeriod + (long) (doOneWayLossMeasurementPeriod * rnd);

		// log.debug("setting timer to " + delay2);
		EL.get().registerTimerCB(delay2, OneWayLossMeasurementCB);
		
	}
	
	
	/**
	 * measurement	
	 */
	void OneWayLossMeasurement(){
		registerOneWayLossMeasurement();	
	/*	execNina.execute(new Runnable(){
			@Override
			public void run() {*/
				// TODO Auto-generated method stub

				AddressIF randTarget=PUtil.getRandomObject(AllAliveNodes);
				int[] Counter={PINGCounter};
				udpComm.sendProbeTrains(randTarget, packetsize,Counter , PING_DELAY);
			
		/*	}
					
		});*/	
	}
	
	
	public static void main(String[] args){
		
		
		
		if (args.length > 0 && args[0].equals("-d")) {
			Log.setPackageRoot(CoordinateLogServer.class);
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

		logServer = new lossMeasurementServer();

		AddressFactory.createResolved(Arrays.asList(myRegion),UDPCOMM_PORT,
				new CB1<Map<String, AddressIF>>() {
					@Override
					protected void cb(CBResult result,
							Map<String, AddressIF> addrMap) {
						switch (result.state) {
						case OK: {

							for (String node : addrMap.keySet()) {

								AddressIF remoteAddr = addrMap.get(node);
								me = AddressFactory.create(remoteAddr);
								log.info("MyAddr='" + me + "'");

								// System.exit(-1);

								break;
							}
							// init

							logServer.init(new CB0() {
								@Override
								protected void cb(CBResult result) {
									switch (result.state) {
									case OK: {
										log.info("Nina loss measurement server initialised successfully");
										break;
									}
									case TIMEOUT:
									case ERROR: {
										log.error("Could not initialise Nina log server: "
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

		
		if(logOneWayLoss!=null){
			try{
				logOneWayLoss.close();	
			}catch(IOException e){
				e.printStackTrace();
			}
		}
		
		
		log.main("Shutdown");
		System.exit(0);
		

	}
	
}
