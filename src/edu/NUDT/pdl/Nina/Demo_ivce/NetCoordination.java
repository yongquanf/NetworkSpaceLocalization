package edu.NUDT.pdl.Nina.Demo_ivce;


import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.harvard.syrah.prp.Log;
import edu.harvard.syrah.sbon.async.CBResult;
import edu.harvard.syrah.sbon.async.Config;
import edu.harvard.syrah.sbon.async.EL;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB1;
import edu.harvard.syrah.sbon.async.comm.xmlrpc.XMLRPCCB;
import edu.harvard.syrah.sbon.async.comm.xmlrpc.XMLRPCComm;
import edu.harvard.syrah.sbon.async.comm.xmlrpc.XMLRPCCommIF;

public class NetCoordination implements Runnable{
	private static Log log=new Log(XMLRPCDEmo.class);
	XMLRPCCommIF apiComm ;
	
	public NetCoordination(){
		/*
		 * Create the event loop
		 */
		
		EL.set(new EL(Long.valueOf(Config.getConfigProps().getProperty(
				"sbon.eventloop.statedump", "600000")), Boolean.valueOf(Config
				.getConfigProps().getProperty("sbon.eventloop.showidle",
						"false"))));

		log.info("Testing the implementation of XMLRPCCommIF.");
		apiComm = new XMLRPCComm();
			
	}
	
	
	private void start(){
		try {
			EL.get().main();
		} catch (OutOfMemoryError e) {
			EL.get().dumpState(true);
			e.printStackTrace();
			log.error("Error: Out of memory: " + e);
		}
	}
	
	
	public void run() {
		start();	
	}
	
	public void queryCoordination(Hashtable<String, Object> ipAddress, final CB1<Map<String, Object>> cbNC){
		try {
			apiComm.call("http://10.107.100.131:55503/xmlrpc",
					"Nina.getRemoteCoords", new XMLRPCCB() {
						
						
						public void cb(CBResult result, Object arg0) {
							switch(result.state){
							
							case OK:{
								//System.out.println("Result of getRemoteCoords is: "+ arg0.toString());
								//format: <IPaddress, coordinate>
								//coordinate is saved as Vector<Double>
								Map<String, Object> resultMap=(Map<String, Object>)arg0;
								System.out.println("\n==================================\n");
								Iterator<Entry<String, Object>> ier = resultMap.entrySet().iterator();
								while(ier.hasNext()){
									Entry<String, Object> entry = ier.next();
									System.out.println("$: "+entry.getKey()+", "+entry.getValue().toString());
									
								}
								//log.info("OK!");
								cbNC.call(CBResult.OK(), resultMap);
								break;
							}
							case ERROR:{
								log.warn("Error, can not contact!");
								cbNC.call(CBResult.ERROR(),null);
								break;
							}
							case TIMEOUT:{
								log.warn("Timeout, can not contact!");
								cbNC.call(CBResult.ERROR(),null);
								break;
							}
							
							}
							
						}
						
						
					}, ipAddress);
		} catch (MalformedURLException e1) {
			log.error("Wrong URL: " + e1);
		} catch (UnknownHostException e2) {
			log.error("Unknown host: " + e2);
		}
	}
	
	public void querryknn(final String name, int k) {
		log.main("Calling Nina.queryKNearestNeighbors");
		XMLRPCCommIF apiComm = new XMLRPCComm();
		try {
			System.out.println("In try-catch block");
			apiComm.call("http://10.107.100.131:55503/xmlrpc",
					"Nina.queryKNearestNeighbors", new XMLRPCCB() {
						
						public void cb(CBResult result, Object arg0) {
							System.out.println("In queryknn() function");
							String nodeName = name;
							System.out.println("Result of estimateRTT is: "
									+ arg0.toString());							
							// format: <ipAddress, rtt value>
							// rtt value is saved as Double
							//
							Map<String, Object> KNNs=(Map<String, Object>)arg0;
							
							int size = KNNs.size();
							double[] orderArray = new double[size];
							String[] nameArray = new String[size];
							Iterator<Entry<String, Object>> ier = KNNs.entrySet().iterator();
							for (int i=0; (i<size)&&(ier.hasNext()); i++){
								Entry<String, Object> entry2 = ier.next();
								orderArray[i] = ((Double)entry2.getValue()).doubleValue();
								nameArray[i] = entry2.getKey();								
								double temp1 = 0.0;
								String temp2 =" ";
								for (int j=0; j<i; j++){
									if ( orderArray[j] < orderArray[i] ){
										temp1 = orderArray[j];
										temp2 = nameArray[j];
										orderArray[j] = orderArray[i];
										nameArray[j] = nameArray[i];
										orderArray[i] = temp1;
										nameArray[i] =temp2;
									}									
								}
							}
							for(int m=0; m<size; m++){
								System.out.println("knn node "+ m + " " + nameArray[m] + ", it's rttvalue is " + orderArray[m]);
							}
						
						}						
					},name,k );
		} catch (MalformedURLException e1) {
			log.error("Wrong URL: " + e1);
		} catch (UnknownHostException e2) {
			log.error("Unknown host: " + e2);
		}
		
	}
	
	
	public void queryKNN(String vertex, int number){
		
	}
	public static void main(String[] args){
		
		
		NetCoordination test=new NetCoordination();
		final ExecutorService exec = Executors.newFixedThreadPool(5);
		
		Hashtable<String, Object> ipAddrs=new Hashtable<String, Object>(5);
		
		
		ipAddrs.put("10.107.100.59", "10.107.100.59");
		
		
		exec.execute(test);
		
		test.queryCoordination(ipAddrs,new CB1<Map<String, Object>>(){

			
			protected void cb(CBResult arg0, Map<String, Object> arg1) {
				// TODO Auto-generated method stub
				
				
				}
			
		});
		ipAddrs.clear();
		ipAddrs.put("10.107.100.131", "10.107.100.131");
		
		
		test.queryCoordination(ipAddrs,new CB1<Map<String, Object>>(){

			
			protected void cb(CBResult arg0, Map<String, Object> arg1) {
				// TODO Auto-generated method stub
				}
			
		});
		ipAddrs.clear();
		ipAddrs.put("10.107.100.131", "10.107.100.131");
		test.queryCoordination(ipAddrs,new CB1<Map<String, Object>>(){

			
			protected void cb(CBResult arg0, Map<String, Object> arg1) {
				// TODO Auto-generated method stub
				}
			
		});
		
		
		//ipAddrs.put("192.168.1.140", "192.168.1.140");
		
		test.queryCoordination(ipAddrs,new CB1<Map<String, Object>>(){

			
			protected void cb(CBResult arg0, Map<String, Object> arg1) {
				// TODO Auto-generated method stub
				}
			
		});
	}
	
/*	*//**
	 * @param args
	 *//*
	public static void main(String[] args) {
		
		
		
	


		//get the coordinates of a set of nodes
		
		Hashtable<String, Object> ipAddrs=new Hashtable<String, Object>();
		ipAddrs.put("192.168.3.188", "192.168.3.188");
		ipAddrs.put("192.168.3.185","192.168.3.185");
		ipAddrs.put(  "192.168.3.184",  "192.168.3.184");
		ipAddrs.put("192.168.1.140","192.168.1.140");
		ipAddrs.put("192.168.1.141","192.168.1.141");
		ipAddrs.put("192.168.1.142","192.168.1.142");
			
		log.main("Calling Nina.getRemoteCoords");
		try {
			apiComm.call("http://192.168.3.185:55503/xmlrpc",
					"Nina.getRemoteCoords", new XMLRPCCB() {

						
						public void cb(CBResult result, Object arg0) {
							System.out.println("Result of coordinate is: "
									+ arg0.toString());
							//format: <IPaddress, coordinate>
							//coordinate is saved as Vector<Double>
							final Map<String, Object> resultMap=(Map<String, Object>)arg0;
							Iterator<Entry<String, Object>> ier = resultMap.entrySet().iterator();
							while(ier.hasNext()){
								Entry<String, Object> entry = ier.next();
								System.out.println("$: "+entry.getKey()+", "+entry.getValue().toString()+"\n");								
							}							
						}						
					}, ipAddrs);
		} catch (MalformedURLException e1) {
			log.error("Wrong URL: " + e1);
		} catch (UnknownHostException e2) {
			log.error("Unknown host: " + e2);
		}		
		//KNN query		
		log.main("Calling Nina.queryKNearestNeighbors");
		String Target="192.168.1.142";
		int k=10;
				
		try {
			apiComm.call("http://192.168.3.185:55503/xmlrpc",
					"Nina.queryKNearestNeighbors", new XMLRPCCB() {

						
						public void cb(CBResult result, Object arg0) {
							System.out.println("Result of estimateRTT is: "
									+ arg0.toString());
							s
							//format: <ipAddress, rtt value>
							//rtt value is saved as Double
							Map<String, Object> KNNs=(Map<String, Object>)arg0;
							//The results must be sorted!!!
							//since the map does not maintain the sorting sequences!!!
							Iterator<Entry<String, Object>> ier = KNNs.entrySet().iterator();
							while(ier.hasNext()){
								Entry<String, Object> entry = ier.next();
								System.out.println("$: "+entry.getKey()+", "+((Double)entry.getValue()).doubleValue());
							}
							
							
						}
						
						
					},Target,k );
		} catch (MalformedURLException e1) {
			log.error("Wrong URL: " + e1);
		} catch (UnknownHostException e2) {
			log.error("Unknown host: " + e2);
		}
		
	

	

		log.main("Shutdown");
		System.exit(0);

	}*/

}
