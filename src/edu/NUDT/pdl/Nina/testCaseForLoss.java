package edu.NUDT.pdl.Nina;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import edu.NUDT.pdl.Nina.net.lossMeasure.packetTrainComm;
import edu.harvard.syrah.prp.ANSI;
import edu.harvard.syrah.prp.Log;
import edu.harvard.syrah.sbon.async.CBResult;
import edu.harvard.syrah.sbon.async.Config;
import edu.harvard.syrah.sbon.async.EL;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB0;
import edu.harvard.syrah.sbon.async.EL.Priority;
import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;

public class testCaseForLoss {

	/*static {

		// All config properties in the file must start with 'HSHer.'

		Config.read("Nina", System
				.getProperty("Nina.config", "config/Nina.cfg"));
	}
	
	private static final String CONFIG_FILE = System.getProperty("config",
	"config/Nina.cfg");*/
	static Log log=new Log(testCaseForLoss.class);
	
	/**
	 * port
	 */
	static int COMM_PORTA=55520;
	static int COMM_PORTB=55521;
	
	static AddressIF myAddrA=AddressFactory.createUnresolved("202.197.22.56", COMM_PORTA);
	static AddressIF myAddrB=AddressFactory.createUnresolved("202.197.22.56", COMM_PORTB);
	
	/**
	 * UDP communication channel
	 */
	packetTrainComm udpCommA;
	packetTrainComm udpCommB;
	
	
	static final int packetsize=100;
	static final int[] PINGCounter={100};
	
	static final int PING_DELAY=50;
	
	
	public testCaseForLoss(){
		
	}
	
	private void init(final CB0 cbDone) {
		
		udpCommA=new packetTrainComm(Priority.NORMAL);
		udpCommB=new packetTrainComm(Priority.NORMAL);
		
		udpCommA.initMyServer(myAddrA, new CB0(){
			@Override
			protected void cb(CBResult result) {
				// TODO Auto-generated method stub
			switch(result.state){
			case OK:{
				log.info("udpCommA.myContactAddress: "+udpCommA.myContactAddress.toString());
			
				udpCommB.initMyServer(myAddrB, new CB0(){
					@Override
					protected void cb(CBResult result) {
						// TODO Auto-generated method stub
					switch(result.state){
					case OK:{
						log.info("udpCommB.myContactAddress: "+udpCommB.myContactAddress.toString());
						cbDone.call(result);
						break;
					}
					default:{
						log.warn("udpCommB is not initialized!\t"+result.toString());
						cbDone.call(result);
						break;
					}
								}
					}						
				});
				
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
	}
	
	
	public void testPacketTrains(final AddressIF remoteNodeA,final AddressIF remoteNodeB, final int packetsize,
			final int[] PINGCounter,final int PING_DELAY){
		
		udpCommA.sendProbeTrains(remoteNodeB, packetsize, PINGCounter, PING_DELAY);
		
	}
	
	public static void main(String[] args){

		ANSI.use(true);
		/*
		 * Create the event loop
		 */
		EL.set(new EL(Long.valueOf(Config.getConfigProps().getProperty(
				"sbon.eventloop.statedump", "600000")), Boolean.valueOf(Config
				.getConfigProps().getProperty("sbon.eventloop.showidle",
						"false"))));
		
		log.info(args[0]+", "+args[1]);
		
		myAddrA=AddressFactory.createUnresolved("127.0.0.1", COMM_PORTA);
		myAddrB=AddressFactory.createUnresolved("127.0.0.1", COMM_PORTB);
		

		try{
			lossMeasurementServer.logOneWayLoss=new BufferedWriter(new FileWriter(new File(
					lossMeasurementServer.oneWayLossLogName + ".log")));
			
		}catch(IOException e){
			e.printStackTrace();
		}
		
		final testCaseForLoss test=new testCaseForLoss();
		test.init(new CB0(){

			@Override
			protected void cb(CBResult result) {
				// TODO Auto-generated method stub
			switch(result.state){
			case OK:{
				log.info("init succeed");
				//A->B
				test.testPacketTrains(myAddrA, myAddrB, packetsize, PINGCounter, PING_DELAY);
				
				break;
			}
			default:{
				
			log.warn("init failed");
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
		
		
		if(lossMeasurementServer.logOneWayLoss!=null){
			try{
				lossMeasurementServer.logOneWayLoss.close();	
			}catch(IOException e){
				e.printStackTrace();
			}
		}

		log.main("Shutdown");

		System.exit(0);
		
	}
	
}
