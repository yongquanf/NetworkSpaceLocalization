package edu.NUDT.pdl.Nina.net.appPing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import edu.NUDT.pdl.Nina.Ninaloader;
import edu.NUDT.pdl.Nina.KNN.NodesPair;
import edu.NUDT.pdl.Nina.StableNC.nc.CollectRequestMsg;
import edu.NUDT.pdl.Nina.StableNC.nc.CollectResponseMsg;
import edu.NUDT.pdl.Nina.StableNC.nc.StableManager;
import edu.NUDT.pdl.Nina.util.MainGeneric;
import edu.NUDT.pdl.pyxida.ping.PingManager;

import edu.harvard.syrah.prp.Log;
import edu.harvard.syrah.prp.NetUtil;
import edu.harvard.syrah.prp.SortedList;
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
import edu.harvard.syrah.sbon.async.comm.obj.ObjCommIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjCommRRCB;


public class PingNodes implements pingNodeList<String> {

	static Log log=new Log(PingNodes.class);
	
	boolean useICMP=false;

	private ObjCommIF comm;
	private PingManager pingManager = null;
	private StableManager ncManager;
	boolean standAlone=false;
	
	public PingNodes(ObjCommIF _comm, PingManager _pingManager, StableManager _ncManager){
		comm = _comm;
		pingManager=_pingManager;
		ncManager=_ncManager;
		
	}
	
	
	public void ping(Vector<String> nodes, final CB2<Vector<String>, Vector<Double>> cbDone) {
		final Vector<String> addrs =new Vector<String>(2);
		final Vector<Double> rtts=new Vector<Double>(2);
		
		if(nodes==null||nodes.isEmpty()){
			cbDone.call(CBResult.ERROR(), addrs, rtts);
		}else{
			
			
			//REMOVE MYSELF
			String myIP=NetUtil.byteIPAddrToString(((NetAddress)Ninaloader.me).getByteIPAddr());
			
			if(nodes.contains(myIP)){
				log.warn("\n\n\nRemove myself\n\n\n");
				nodes.remove(myIP);
			}
			
			Iterator<String> ier = nodes.iterator();
			final Barrier barrier = new Barrier(true);
			barrier.setNumForks(nodes.size());
			
			while(ier.hasNext()){				
			    String tmp = ier.next();
			   // System.out.println("$: "+tmp);
				final String t=tmp;
				
				ping(tmp, new CB1<Double>(){

					@Override
					protected void cb(CBResult result, Double arg1) {
						// TODO Auto-generated method stub
						switch(result.state){
						case OK:{
							addrs.add(t);
							rtts.add(arg1);
							//System.out.println("OK @"+t);
							break;
						}
						case ERROR:
						case TIMEOUT:{
							System.err.println("fail @ "+t);
						break;	
						}
						}
						
						barrier.join();
					}					
										
				});
			}
			
			long time=4*1000;
			EL.get().registerTimerCB(barrier, time,new CB0() {
				protected void cb(CBResult result) {
					SortedList<pair> sortedL = new SortedList<pair>(
							new NPair());	
					
					for(int i=0;i<addrs.size();i++){
						sortedL.add(new pair(addrs.get(i),rtts.get(i)));
					}
					addrs.clear();
					rtts.clear();
					for(int i=0;i<sortedL.size();i++){
						addrs.add(sortedL.get(i).getAddr());
						rtts.add(sortedL.get(i).getRtt());
					}
					
					cbDone.call(CBResult.OK(), addrs, rtts);	
					
				}
			});
			
			
		}
	}


	


	
	
	public void ping(String node,final CB1<Double> cbDone) {
		if(useICMP){
		MainGeneric.doPing(node,  cbDone); 
		}else{
			//use application level ping
			doPing(node,new  CB2<Double, Long>(){
				@Override
				protected void cb(CBResult result, Double arg1, Long arg2) {
					// TODO Auto-generated method stub
					cbDone.call(result, arg1);
					
				}
							
			});
		}
	}

	public void doPing(String target, final CB2<Double, Long> cbDone) {

		final double[] lat = new double[1];
		lat[0] = -1;
		final long timer = System.currentTimeMillis();

		AddressFactory.createResolved(target, Ninaloader.COMM_PORT, new CB1<AddressIF>() {
			@Override
			protected void cb(CBResult result, AddressIF addr) {
				// TODO Auto-generated method stub
				switch (result.state) {
				case OK: {

					AddressIF  neighbor = addr;
					
					

					if(neighbor.equals(Ninaloader.me)){
						//myself
						cbDone.call(CBResult.OK(), Double.valueOf(0), Long.valueOf(0));
						return;
					}
					
					
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
						
						if(standAlone){
						
						
						CollectRequestMsg msg = new CollectRequestMsg(
								Ninaloader.me);
						final long sendStamp = System.nanoTime();

						//log.info("Sending gossip request to "+ neighbor);

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
					}else{
						ncManager.Ping(neighbor, new CB1<Double>(){

							@Override
							protected void cb(CBResult result,
									Double arg1) {
								// TODO Auto-generated method stub
							
								cbDone
								.call(result,arg1,Long.valueOf(timer));	
								
							}
																
							
						});
					}
						
						
						

					}
					break;
				}
				case TIMEOUT:
				case ERROR: {
					log.error("Could not resolve  address: " + result.what);
					
					cbDone.call(result, Double.valueOf(lat[0]), Long
							.valueOf(timer));
					break;
				}
				}
			}
		});
		
		
		
		/*AddressFactory.createResolved(target, Ninaloader.COMM_PORT,
				new CB1<AddressIF>() {
					@Override
					protected void cb(CBResult result, AddressIF addr) {

						final double[] lat = new double[1];
						lat[0] = -1;
						final long timer = System.currentTimeMillis();

						// TODO Auto-generated method stub
						switch (result.state) {
						case OK: {*/

							

				/*			break;
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
				});*/

	}
	
	
	
	public static void main(String[] args){
		
		
		/*
		 * Create the event loop
		 */
		EL.set(new EL(Long.valueOf(Config.getConfigProps().getProperty(
				"sbon.eventloop.statedump", "600000")), Boolean.valueOf(Config
				.getConfigProps().getProperty("sbon.eventloop.showidle",
						"false"))));

		
		
		String[] list={"192.168.1.138", "192.168.1.141","192.168.2.45","192.168.2.47","192.168.3.188","192.168.3.180"};
		
		PingNodes test=new PingNodes(null,null,null);
		Vector<String> addrs=new Vector<String>(2);
		addrs.addAll(Arrays.asList(list));
		test.ping(addrs, new CB2<Vector<String>, Vector<Double>>(){

			@Override
			protected void cb(CBResult result, Vector<String> arg1,
					Vector<Double> arg2) {
				// TODO Auto-generated method stub
				int len=arg1.size();
				for(int i=0;i<len;i++){
					System.out.println("$: "+i+"\t"+arg1.get(i)+",\t"+arg2.get(i));
										
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


}
