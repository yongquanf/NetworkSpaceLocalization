package edu.NUDT.pdl.Nina.Demo_ivce;

import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;

import edu.harvard.syrah.prp.Log;
import edu.harvard.syrah.sbon.async.CBResult;
import edu.harvard.syrah.sbon.async.Config;
import edu.harvard.syrah.sbon.async.EL;
import edu.harvard.syrah.sbon.async.comm.xmlrpc.XMLRPCCB;
import edu.harvard.syrah.sbon.async.comm.xmlrpc.XMLRPCComm;
import edu.harvard.syrah.sbon.async.comm.xmlrpc.XMLRPCCommIF;

public class XMLRPCDEmo {

	private static Log log=new Log(XMLRPCDEmo.class);
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		/*
		 * Create the event loop
		 */
		EL.set(new EL(Long.valueOf(Config.getConfigProps().getProperty(
				"sbon.eventloop.statedump", "600000")), Boolean.valueOf(Config
				.getConfigProps().getProperty("sbon.eventloop.showidle",
						"false"))));
		
		XMLRPCCommIF apiComm = new XMLRPCComm();

		log.info("Testing the implementation of XMLRPCCommIF.");
		
	/*	
		String ANode="192.168.3.10";
		String BNode="192.168.3.184";
		
		

		//estimate RTT
		try {
			apiComm.call("http://192.168.3.180:55503/xmlrpc",
					"Nina.estimateRTT", new XMLRPCCB() {

						@Override
						public void cb(CBResult result, Object arg0) {
							System.out.println("Result of estimateRTT is: "
									+ arg0.toString());
							//format: <IPaddress, coordinate>
							//coordinate is saved as Vector<Double>					
							
						}
						
						
					},ANode, BNode);
		} catch (MalformedURLException e1) {
			log.error("Wrong URL: " + e1);
		} catch (UnknownHostException e2) {
			log.error("Unknown host: " + e2);
		}*/
		
		
		
		
		//get the coordinates of a set of nodes
		
/*		Hashtable<String, Object> ipAddrs=new Hashtable<String, Object>(5);
		ipAddrs.put("192.168.3.188", "192.168.3.188");
		ipAddrs.put("192.168.3.190", "192.168.3.190");
		ipAddrs.put("192.168.3.191", "192.168.3.191");
		ipAddrs.put("192.168.1.140", "192.168.1.140");
		ipAddrs.put("192.168.1.141", "192.168.1.141");
		
		String[] IPs={
		"192.168.1.138",		
		"192.168.1.140",
		"192.168.1.141",
		"192.168.1.142",
		"192.168.3.193",
		"192.168.3.191",
		"192.168.3.190",
		"192.168.3.189",
		"192.168.3.188",
		"192.168.3.187",
		"192.168.3.186",
		"192.168.3.185",
		"192.168.3.184",
		"192.168.3.183",
		"192.168.3.180"	};
		
		
		
		Vector<String> addrs=new Vector<String>(2);
		addrs.addAll(Arrays.asList(IPs));
		
		log.main("Calling Nina.getRemoteCoords");
		try {
			apiComm.call("http://192.168.3.180:55503/xmlrpc",
					"Nina.getRemoteCoords", new XMLRPCCB() {

						@Override
						public void cb(CBResult result, Object arg0) {
							System.out.println("Result of getRemoteCoords is: "+ arg0.toString());
							//format: <IPaddress, coordinate>
							//coordinate is saved as Vector<Double>
							Map<String, Object> resultMap=(Map<String, Object>)arg0;
							
							Iterator<Entry<String, Object>> ier = resultMap.entrySet().iterator();
							while(ier.hasNext()){
								Entry<String, Object> entry = ier.next();
								System.out.println("$: "+entry.getKey()+", "+entry.getValue().toString());
								
							}
							
						}
						
						
					}, addrs);
		} catch (MalformedURLException e1) {
			log.error("Wrong URL: " + e1);
		} catch (UnknownHostException e2) {
			log.error("Unknown host: " + e2);
		}
*/
		
		//KNN query
		
		
		log.main("Calling Nina.queryKNearestNeighbors");
		String Target="10.107.100.59";
		int k=10;
				
		try {
			apiComm.call("http://10.107.100.59:55503/xmlrpc",
					"Nina.queryKNearestNeighbors", new XMLRPCCB() {

						@Override
						public void cb(CBResult result, Object map) {
							if(map==null){
								System.err.println("error, empty object!");
							}else{
							System.out.println("Result of queryKNearestNeighbors is: "+ map.toString());
							
							//format: <ipAddress, rtt value>
							//rtt value is saved as Double
							Map<String, Object> KNNs=(Map<String, Object>)map;
							//The results must be sorted!!!
							//since the map does not maintain the sorting sequences!!!
							Iterator<Entry<String, Object>> ier = KNNs.entrySet().iterator();
							while(ier.hasNext()){
								Entry<String, Object> entry = ier.next();
								System.out.println("$: "+entry.getKey()+", "+((Double)entry.getValue()).doubleValue());
							}
							
							}
						}
						
						
					},Target,k );
		} catch (MalformedURLException e1) {
			log.error("Wrong URL: " + e1);
		} catch (UnknownHostException e2) {
			log.error("Unknown host: " + e2);
		}
	
	

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
