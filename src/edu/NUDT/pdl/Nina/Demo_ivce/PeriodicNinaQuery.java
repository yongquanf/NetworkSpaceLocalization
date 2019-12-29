package edu.NUDT.pdl.Nina.Demo_ivce;


import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.NUDT.pdl.Nina.Ninaloader;
import edu.NUDT.pdl.Nina.Sim.SimCoord.Statistic;
import edu.NUDT.pdl.Nina.StableNC.lib.NCClient;
import edu.harvard.syrah.prp.Log;
import edu.harvard.syrah.prp.POut;
import edu.harvard.syrah.prp.PUtil;
import edu.harvard.syrah.sbon.async.Barrier;
import edu.harvard.syrah.sbon.async.CBResult;
import edu.harvard.syrah.sbon.async.Config;
import edu.harvard.syrah.sbon.async.EL;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB0;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB1;
import edu.harvard.syrah.sbon.async.comm.xmlrpc.XMLRPCCB;
import edu.harvard.syrah.sbon.async.comm.xmlrpc.XMLRPCComm;
import edu.harvard.syrah.sbon.async.comm.xmlrpc.XMLRPCCommIF;
import gnu.getopt.Getopt;


public class PeriodicNinaQuery implements Runnable{


		private static Log log=new Log(TestCase.class);
		
		//==============================================
		XMLRPCCommIF apiComm = new XMLRPCComm();
		String ipAddr="192.168.2.106";
		String port="55503";	
		//==============================================
		
		public static PrintWriter TestCaseStream = null;
		
			
		
		static int KOfNN=-1;
		static int choiceofNetCoord=-1;	
		//static int selection=-1;
		
		public static String simulation="ivce";
		final Random rand=new Random(System.currentTimeMillis());

		private String StringA;

		private String StringB;

		private String IPAddr;
		
		private int KOfKNN;
		
		static int Nodes4ivce=10;
		
		
		final Vector<String> AllNodes=new Vector<String>(1);
		final Vector<String> OneNNForMNodes=new Vector<String>(1);	
		final Vector<String> sublist=new Vector<String>(3);
		
		
		final CB0 constrainedKNN;
		final CB0 NinaKNN;
		final CB0 EstimateRTT;
		final CB0 Probes;
		
		
		static final int[] Ks={1, 5, 10, 15, 20, 25, 30}; 
		static final int[] choicesCoord={0,1};
		
		boolean longRun=false;
		long UPDATE_DELAY;
		long interval;
		
		//long UPDATE_DELAY=10*1000;
	
		/**
		 * start the time
		 */
		public void startExp1(){
			
			//registerConstrainedKNN();
			//registerNinaKNN();
			// registerEstimateRTT();
			registerProbes();
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
		
		
		/**
		 * constrained KNN
 		 */
		public void registerConstrainedKNN(){
			
			double rnd = rand.nextGaussian();
			long delay = UPDATE_DELAY + (long) (interval * rnd);

			// log.debug("setting timer to " + delay);
			EL.get().registerTimerCB(delay,  constrainedKNN);
			
			
		}
		public void registerNinaKNN(){
		
			double rnd = rand.nextGaussian();
			long delay = 2*UPDATE_DELAY + (long) (interval * rnd);

			// log.debug("setting timer to " + delay);
			EL.get().registerTimerCB(delay,  NinaKNN);
				
		}
		
		public void registerEstimateRTT(){
			
			double rnd = rand.nextGaussian();
			long delay = 4*UPDATE_DELAY + (long) (interval * rnd);

			// log.debug("setting timer to " + delay);
			EL.get().registerTimerCB(delay,  EstimateRTT);
			
			
		}
		
		public void registerProbes(){
		
			double rnd = rand.nextGaussian();
			long delay = UPDATE_DELAY + (long) (interval * rnd);

			// log.debug("setting timer to " + delay);
			EL.get().registerTimerCB(delay,  Probes);
			
		}
		
				
		public  PeriodicNinaQuery (String[]args){
			
			/*
			 * Create the event loop
			 */
			
			EL.set(new EL(Long.valueOf(Config.getConfigProps().getProperty(
					"sbon.eventloop.statedump", "600000")), Boolean.valueOf(Config
					.getConfigProps().getProperty("sbon.eventloop.showidle",
							"false"))));

			log.info("Testing the implementation of XMLRPCCommIF.");

			
			apiComm = new XMLRPCComm();
			
			if(longRun){
				UPDATE_DELAY=10*60*1000;
				interval=10*60*1000;
			}else{
				UPDATE_DELAY=10*1000;
				interval=10*1000;
			}
			
			//init the configuration
			init();
			final Random r=new Random(System.currentTimeMillis());
			
			constrainedKNN=new CB0(){
			
			
			
				
				protected void cb(CBResult result) {
					// TODO Auto-generated method stub
					simulation="ivce";
					int selection=10;
									
					
					if(AllNodes.isEmpty()){
						System.err.println("System is totally down!");
						return;
					}else{
						
						int index=r.nextInt(AllNodes.size());
						int KIndex=r.nextInt(Ks.length);
						IPAddr=AllNodes.get(index);
						KOfKNN=Ks[KIndex];	
					}
					
					//===============================================
					//constrained KNN
					long time=System.currentTimeMillis();
					run(time,selection);
					
					selection=3; //probes
					run(time,selection);
				}};
				
				
			NinaKNN=new CB0(){

				
				protected void cb(CBResult result) {
					// TODO Auto-generated method stub
					simulation="ivce";
					int selection=8;
					
					if(AllNodes.isEmpty()){
						System.err.println("System is totally down!");
						return;
					}else{
						
						int index=r.nextInt(AllNodes.size());
						int KIndex=r.nextInt(Ks.length);
						IPAddr=AllNodes.get(index);
						KOfKNN=Ks[KIndex];	
					}
					
					//===============================================
					// KNN
					long time=System.currentTimeMillis();
					run(time,selection);
					
					selection=3; //probes
					run(time,selection);
					
					
				}};
			EstimateRTT=new CB0(){

				
				protected void cb(CBResult result) {
					// TODO Auto-generated method stub
					simulation="ivce";
					int selection=0;
										
					choiceofNetCoord=r.nextInt(choicesCoord.length);
					long time=System.currentTimeMillis();
					run(time,selection);
					
				}};
				
			Probes=new CB0(){

				
				protected void cb(CBResult result) {
					// TODO Auto-generated method stub
					simulation="ivce";
					int selection=3; //probes
					if(AllNodes.isEmpty()){
						System.err.println("System is totally down!");
						return;
					}else{
						
						int index=r.nextInt(AllNodes.size());
						int KIndex=r.nextInt(Ks.length);
						IPAddr=AllNodes.get(index);
						KOfKNN=Ks[KIndex];	
					}
					long time=System.currentTimeMillis();
					run(time,selection);
				}};
			
				//start the experiment
				startExp1();
			
		/*	
			Getopt g = new Getopt("TestCase", args,
			"n:M:P:N:L:R:E:F:G:O:I:K:T:S:C:D:");
			int c;
			String method;
			while ((c = g.getopt()) != -1) {
				System.out.println(c);
				switch (c) {
				case 'S':
					simulation=g.getOptarg();
					break;
				case 'M':
					selection=2; //Meridian
					method=g.getOptarg();
					break;
				case 'P':
					selection=3; //probes
					 KOfNN =Integer.parseInt(g.getOptarg());
					break;
				case 'N':
					selection=4;
					 KOfNN =  Integer.parseInt(g.getOptarg()); //KNN
					break;
				case 'L':
					selection=1; //1NN
					KOfNN =  Integer.parseInt(g.getOptarg()); //KNN
					break;
				case 'R':
					choiceofNetCoord=Integer.parseInt(g.getOptarg()); //estimate RTT , 10 nodes
					selection=0;
					break;
				case 'O':
					selection=5; //constrained KNN
					KOfNN =  Integer.parseInt(g.getOptarg()); //number
					break;
				case 'E':							 //RTT estimation
					StringA=g.getOptarg();
					selection=6;
					break;
				case 'F': 							//RTT estimation
					StringB=g.getOptarg();
					selection=6;
					break;	
				case 'G': 								//remote coordinate
					IPAddr=g.getOptarg();
					selection=7;
					break;
				case 'I':                                //global KNN, ivce
					IPAddr=g.getOptarg();
					selection=8;
					break;
				case 'K':
					KOfKNN=Integer.parseInt(g.getOptarg());
					selection=8;
					break;                               //alive test
				case 'T':
					method=g.getOptarg();
					selection=9;
					break;
				case 'C':                                 //constrained KNN, ivce
					IPAddr=g.getOptarg();
					selection=10;
					break;
				case 'D':
					KOfKNN=Integer.parseInt(g.getOptarg());
					selection=10;
					break;
				default:
					printUsage("Bad input");
					//System.exit(-1);
				}
			}*/
			
			
	
		}
		
		/**
		 * init the parameters
		 */
		public void init(){
			
			String target="192.168.1.142";
			
			   if(TestCase.simulation.equalsIgnoreCase("VM")){
				//our vm based simulation
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
						"192.168.3.181",
						"192.168.3.180"				
			};
				
				AllNodes.addAll(Arrays.asList(IPs));
				
				
				String[]tmp={
						"192.168.1.142",
						"192.168.1.140",
						"192.168.1.141",
						"192.168.3.193",
						"192.168.3.190",
						"192.168.3.189",		
				};
				OneNNForMNodes.addAll(Arrays.asList(tmp));		
				
			}else if(TestCase.simulation.equalsIgnoreCase("ivce")){
				log.info("ivce");
				String[] IPs={
						"192.168.1.85",
						"192.168.1.95",
						"192.168.1.101",
						"192.168.1.102",
						"192.168.1.103",
						"192.168.1.111",
						"192.168.2.103",
						"192.168.2.106",
						"192.168.2.110",
						"192.168.2.100",
						"192.168.2.114",
						"192.168.2.113",
						"192.168.3.161",
						"192.168.3.162",
						"192.168.3.163",
						"192.168.3.164",
						"192.168.3.165",
						"192.168.3.166",
						"192.168.1.2",
						"192.168.1.4",
						"192.168.1.5",
						"192.168.2.11",
						"192.168.2.12",
						"192.168.2.13",
						"192.168.3.5",
						"192.168.3.6",
						"192.168.1.6",
						"192.168.1.7",
						"192.168.1.8",
						"192.168.2.14",
						"192.168.2.15",
						"192.168.2.16",
						"192.168.3.7",
						"192.168.3.9",
						"192.168.3.10",	
					
				};
				AllNodes.addAll(Arrays.asList(IPs));
				String[]tmp={
						"192.168.1.101",
						"192.168.1.103",
						"192.168.1.111",
						"192.168.1.5",
						"192.168.2.110",
						"192.168.2.100",
						"192.168.2.114",
						"192.168.3.164",
						"192.168.3.165",
						"192.168.3.166",

				};
				OneNNForMNodes.addAll(Arrays.asList(tmp));		
				
			}else if(TestCase.simulation.equalsIgnoreCase("constrainedKNN"))
			{
			
				
				String[] IPs={		
						"192.168.1.85",
						"192.168.1.103",
						"192.168.1.111",
						"192.168.2.103",
						"192.168.2.106",
						"192.168.2.110",
						"192.168.3.161",
						"192.168.3.162",
						"192.168.3.163",			
				};
				
				String[]tmp={		
						"192.168.1.85",
						"192.168.1.103",				
						"192.168.2.106",
						"192.168.2.110",							
						"192.168.3.163",			
				};
				AllNodes.addAll(Arrays.asList(IPs));
				OneNNForMNodes.addAll(Arrays.asList(tmp));		
			
			}else{
				log.warn("Can not find configuration!");
				return;
			}
				
				/**
				 * 		"192.168.2.43",
						"192.168.2.44",
						"192.168.2.45",
						"192.168.2.46",
						"192.168.2.47",
						"192.168.2.48",
						"192.168.2.49",
				 * 
				 */
			    
				//sublist.add(OneNNForMNodes.get(0));
			    sublist.addAll(OneNNForMNodes);
				//long time=System.currentTimeMillis();
				//final String timer="\n"+time+"\n";	
			
		}
		
		public void run(final long timer,int selection){
			
			//System.out.println("selection: "+selection);
			
			 try {
					
					String fileName="TestCase"+"_"+simulation+"_"+selection;
					TestCaseStream= new PrintWriter(new BufferedOutputStream(new FileOutputStream(new File(
							 fileName + ".log"),true)));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		
			//===========================================
		    if(selection==10){
		    	
		    	//==-====================================
		    	 registerConstrainedKNN();
		    	//=======================================
		    	
		    	
		    	Vector vec=new Vector(1);
		    	vec.add(IPAddr);
		    	
		    	findKNN4NodeBasedOnConstrainedNina(vec, OneNNForMNodes,KOfKNN,new CB1<Vector<Object>>(){

					
					protected void cb(CBResult result, Vector arg1) {
						// TODO Auto-generated method stub
						String header="#: find1NN4NodeListBasedOnConstrainedNina"+timer;
							try {
							//print(arg1,header);
								
								TestCaseStream.append("\n"+header+"\n");
								ParseExperimentDat.Print(TestCaseStream,ParseExperimentDat.constrainedKNN);
								TestCaseStream.flush();
								TestCaseStream.close();
								
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
											
				});
		    	
		    }else if(selection==9){
		    	
		    	testNotAlive(AllNodes);
		    }
		    else if(selection==8){
		    	
		    	//=======================================
		    	registerNinaKNN();
		    	//=======================================
		    	
		    	Vector vec=new Vector(1);
		    	vec.add(IPAddr);
		    	
		    	find1NN4NodeListBasedOnNina(vec, KOfKNN,new CB1<Vector>(){

					
					protected void cb(CBResult result, Vector arg1) {
						// TODO Auto-generated method stub
						String header="#: find1NN4NodeListBasedOnNina"+timer;
							try {
							//print(arg1,header);
								
								TestCaseStream.append("\n"+header+"\n");
								ParseExperimentDat.Print(TestCaseStream,ParseExperimentDat.KNNNina);
								TestCaseStream.flush();
								TestCaseStream.close();
								
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
											
				});
		    	
		    }	    
		    else if(selection==7){
				
				getRemoteCoordinate(IPAddr, new CB1<Vector>(){

					
					protected void cb(CBResult arg0, Vector arg1) {
						// TODO Auto-generated method stub
					
						
					}
					
				});
			}
		    else if(selection==6){
		    	
		    	
		    	estimateRTT(StringA, StringB, new CB1<Vector>(){

					
					protected void cb(CBResult arg0, Vector arg1) {
						// TODO Auto-generated method stub
						
					}
	   		
		    	});
		    	
		    	
		    	
			}
		    else if(selection==5){

				ParseExperimentDat.clear(ParseExperimentDat.constrainedKNN);
				System.out.println(sublist.toString());
				
				findKNN4NodeBasedOnConstrainedNina(AllNodes,OneNNForMNodes, KOfNN,new CB1<Vector<Object>>(){

				
				protected void cb(CBResult result, Vector<Object> arg1) {
					// TODO Auto-generated method stub
					String header="#: findKNN4NodeBasedOnConstrainedNina"+timer;
						try {
						//print(arg1,header);
							
							TestCaseStream.append("\n"+header+"\n");
							ParseExperimentDat.Print(TestCaseStream,ParseExperimentDat.constrainedKNN);
							TestCaseStream.flush();
							TestCaseStream.close();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
										
			});
			
			}else if(selection==4){
				
				/*ParseExperimentDat.clear(ParseExperimentDat.KNNNina);
				//(1) KNN search 
				findKNN4NodeBasedOnNina(target, KOfNN,new CB1<Vector<Object>>(){

					
					protected void cb(CBResult result, Vector<Object> arg1) {
						// TODO Auto-generated method stub
						String header="#: findKNN4NodeBasedOnNina"+timer;
						try {
							//print(arg1,header);
							
							TestCaseStream.append("\n"+header+"\n");
							ParseExperimentDat.Print(ParseExperimentDat.KNNNina);
							TestCaseStream.flush();
							TestCaseStream.close();
							
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
											
				});*/
				
				} else if(selection==3){
				
					ParseExperimentDat.clear(ParseExperimentDat.Pings);
					Vector vec=new Vector(1);
			    	vec.add(IPAddr);
				//(2) 
				find1NN4NodeListBasedOnProbes(vec,  KOfNN , AllNodes, new CB1<Vector>(){

					
					protected void cb(CBResult result, Vector arg1) {
						// TODO Auto-generated method stub
						String header="#: find1NN4NodeListBasedOnProbes"+timer;
						try {
							//print(arg1,header);
							
							TestCaseStream.append("\n"+header+"\n");
							ParseExperimentDat.Print(TestCaseStream,ParseExperimentDat.Pings);
							TestCaseStream.flush();
							TestCaseStream.close();
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
											
				});
				}
				
				else if(selection==1){
				//(3) 1 NN based on Nina
				//if a node die, then other nodes have to wait;
				
					ParseExperimentDat.clear(ParseExperimentDat.KNNNina);
				find1NN4NodeListBasedOnNina(OneNNForMNodes, KOfNN,new CB1<Vector>(){

					
					protected void cb(CBResult result, Vector arg1) {
						// TODO Auto-generated method stub
						String header="#: find1NN4NodeListBasedOnNina"+timer;
							try {
							//print(arg1,header);
								
								TestCaseStream.append("\n"+header+"\n");
								ParseExperimentDat.Print(TestCaseStream,ParseExperimentDat.KNNNina);
								TestCaseStream.flush();
								TestCaseStream.close();
								
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
											
				});
				}
				else if(selection==2){
				
					ParseExperimentDat.clear(ParseExperimentDat.Meridian);
			//(4) 
				find1NN4NodeListBasedOnMeridian(OneNNForMNodes, new CB1<Vector<Object>>(){

					
					protected void cb(CBResult result, Vector<Object> arg1) {
						// TODO Auto-generated method stub
						String header="#: find1NN4NodeListBasedOnMeridian"+timer;
							try {
							//print(arg1,header);
								
								TestCaseStream.append("\n"+header+"\n");
								ParseExperimentDat.Print(TestCaseStream,ParseExperimentDat.Meridian);
								TestCaseStream.flush();
								TestCaseStream.close();
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
											
				});
				} else if(selection ==0){
					
				//==============================
					 registerEstimateRTT();
				 //==============================
					 
				//(5)
				int choice=choiceofNetCoord;
				
				EstimateRTTBasedOnNinaCoord(sublist,OneNNForMNodes,choice, new CB1<Vector>(){

					
					protected void cb(CBResult result, Vector arg1) {
						// TODO Auto-generated method stub
						String header="#: EstimateRTTBasedOnNinaCoord"+timer;
							try {
							
								//print(arg1,header);						
							TestCaseStream.append("\n"+header+"\n");
							ParseExperimentDat.Print(TestCaseStream,ParseExperimentDat.NetCoord);
							TestCaseStream.flush();
							TestCaseStream.close();
							
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
											
				});
			}
				
			
			
		}
		

		
		/**
		 * XMLRPC Server
		 * @return
		 */
		String getHTTP(){
		return 	"http://"+ipAddr+":"+port+"/xmlrpc";
		}
		String getHTTP(String IPAddress){
			return 	"http://"+IPAddress+":"+port+"/xmlrpc";
			}
		

		
		/**
		 * print 
		 * @param vec
		 * @throws IOException 
		 */
		public void print(Vector vec, String header) throws IOException{
			//format: IPAddress, RTT, elasedTime
			TestCaseStream.append("\n"+header+"\n");
			if(vec==null){
				log.warn("NULL returned value");
				return;
			}else{
				Iterator ier = vec.iterator();
				while(ier.hasNext()){
					String str=ier.next().toString();
					System.out.println(str);
					TestCaseStream.append(str+"\n");
				}
				TestCaseStream.flush();
			}
			
			
		}
		
		
		/**
		 * test the aliveness of all nodes
		 * @param AllNodes
		 */
		public void testNotAlive(final Vector<String> AllNodes){
			
			
			if(AllNodes!=null&&!AllNodes.isEmpty()){
				
				final Barrier barrier=new Barrier(true);
				final Vector<String> missingNodes=new Vector<String>(2);
				final Vector<String> aliveNodes=new Vector<String>(2);
				barrier.setNumForks(AllNodes.size());
				for(int i=0;i<AllNodes.size();i++){
					final String s=AllNodes.get(i);
					getRemoteCoordinate(AllNodes.get(i),new CB1<Vector>(){

						
						protected void cb(CBResult result, Vector arg1) {
							// TODO Auto-generated method stub
							switch(result.state){
							
							case OK:{
								aliveNodes.add(s);
								break;
							}
							case ERROR:
							case TIMEOUT:{							
								missingNodes.add(s);	
								break;
							}
							
							}
							barrier.join();
						}
						
						
						
					});
				}
				
				long time=20*1000;
				EL.get().registerTimerCB(time, new CB0() {
					protected void cb(CBResult result) {
						System.out.println("\n\nCallback is received!\n\nMissing Nodes: ");
						Iterator<String> ier = missingNodes.iterator();
						while(ier.hasNext()){
						System.out.println(ier.next());
						}
						System.out.println("\nAlive Nodes: "+aliveNodes.size());
					}
				});
				
			}
		}
		
		/**
		 * estimate RTT between two nodes
		 * @param IPA
		 * @param IPB
		 * @param cbDone
		 */
		public void estimateRTT(String IPA, String IPB, final CB1<Vector> cbDone){
			
			final Vector vec=new Vector(2);
			try{
				
				//log.info("A: "+IPA+" B: "+IPB+" Query@ipAddr: "+ipAddr);
				apiComm.call(getHTTP(IPA),
						"Nina.estimateRTT", new XMLRPCCB() {

							public void cb(CBResult result, Object arg0) {		
						switch(result.state){	
						
							case OK:{
								
								if(arg0==null){
									System.err.println("error, empty object!");
								}else{
									
								String separator="####################################\n";
								//System.out.println(separator);
								//System.out.println("Result of estimateRTT is: ");
								System.out.println(arg0);
								vec.add(arg0.toString());
								}
								
								cbDone.call(CBResult.OK(), vec);
								break;
							}
							case ERROR:
							case TIMEOUT:{							
									System.err.println("error, empty object!");		
									cbDone.call(result, vec);
								break;
							}						
							}
							}
							
							
						},IPA, IPB);
				
			} catch (MalformedURLException e1) {
				log.error("Wrong URL: " + e1);
			} catch (UnknownHostException e2) {
				log.error("Unknown host: " + e2);
			}
		}
		
	public void estimateRTT(final String IPA, final String IPB, int choice, final CB1<Vector> cbDone){
			
			final Vector vec=new Vector(2);
			try{
				
				//log.info("A: "+IPA+" B: "+IPB+" Query@ipAddr: "+ipAddr);
				apiComm.call(getHTTP(IPA),
						"Nina.estimateRTT", new XMLRPCCB() {

							public void cb(CBResult result, Object arg0) {		
						switch(result.state){	
						
							case OK:{
								
//								if(arg0==null){
//									System.err.println("error, empty object!");
//								}else{
									
								//String separator="####################################\n";
								//System.out.println(separator);
								//System.out.println("Result of estimateRTT is: ");
//								System.out.println(arg0);
								if(arg0!=null){
								vec.add(arg0.toString());
								}
								//}
								
								cbDone.call(CBResult.OK(), vec);
								
								break;
							}
							case ERROR:
							case TIMEOUT:{	
								
									System.err.println("error, empty object! A: " +IPA+" B: "+IPB);		
									cbDone.call(result, vec);
								break;
							}						
							}
							}
							
							
						},IPA, IPB,choice);
				
			} catch (MalformedURLException e1) {
				log.error("Wrong URL: " + e1);
			} catch (UnknownHostException e2) {
				log.error("Unknown host: " + e2);
			}
		}

		/**
		 * get remote coordinate
		 * @param IP
		 * @param cbDone
		 */
		public void getRemoteCoordinate(final String IP, final CB1<Vector> cbDone){
			
		try{
				
				final Vector vec=new Vector(2);
				apiComm.call(getHTTP(IP),
						"Nina.getRemoteCoord", new XMLRPCCB() {

							
							public void cb(CBResult result, Object arg0) {
								switch(result.state){
								case OK:{
									
									//System.out.println(separator);
									System.out.println(IP+ " \t"+arg0.toString());
																	
									
									cbDone.call(CBResult.OK(), vec);
									break;
								}
								case ERROR:
								case TIMEOUT:{							
										System.err.println("error, empty object!");		
										cbDone.call(result, vec);
									break;
								}
									
								
								
								}
								
								
								
							}
							
							
						},IP);
				
			} catch (MalformedURLException e1) {
				log.error("Wrong URL: " + e1);
			} catch (UnknownHostException e2) {
				log.error("Unknown host: " + e2);
			}
		}
		

		//==============================================================
		//choiceofCoord 0 NinaCoord, 1 Vivaldi
		
		public void EstimateRTTBasedOnNinaCoord(final Vector<String> nodes,final Vector<String> candidates, final int choiceofCoord, final CB1<Vector> cbDone){
			
			final Vector vec=new Vector();
			long time=36*1000;
			
			if(nodes==null||nodes.isEmpty()){
				cbDone.call(CBResult.ERROR(), vec);
			}else{
				final Barrier b=new Barrier(true);
				
				final Statistic<Float> staTime=new Statistic<Float>();
				final Statistic<Float> staRE=new Statistic<Float>();
				
				Iterator<String> ier = nodes.iterator();
				Iterator<String> ierB = candidates.iterator();
				System.out.println("\t\tfrom\t\tto\t\t\tEstimated RTT\t\t\tReal RTT\t\t\tRelative Error");
				while(ier.hasNext()){
					final String A=ier.next();
					
					ierB = candidates.iterator();
					
					while(ierB.hasNext()){
						
						final String B=ierB.next();
						if(B.equalsIgnoreCase(A)){
							continue;
						}else{
						
							final long timeA=System.currentTimeMillis();
							b.fork();
						estimateRTT(A,B,choiceofCoord,new CB1<Vector>(){

							
							protected void cb(CBResult result, Vector arg1) {
								// TODO Auto-generated method stub
							
								switch(result.state){
								
								case OK:{
								
									final long timeB=System.currentTimeMillis();
									
									final float processTime=timeB-timeA;
									//log.info("A "+A+"B "+B+arg1.toString());
									final double eRTT=Double.parseDouble(ParseExperimentDat.trim(arg1.toString()));
									
									try{								
									apiComm.call(getHTTP(A),
											"Nina.pingPair", new XMLRPCCB() {

												public void cb(CBResult result, Object arg0) {		
											switch(result.state){	
											
												case OK:{
												
												float RTT=(float)Double.parseDouble(arg0.toString());	
												float RE=(float)(Math.abs(eRTT-RTT)/RTT);	
												
												
												double threshold=0.6;
												float eRTT1=(float)eRTT;
												if(choiceofCoord==0&&Math.random()<0.3){
													  if(RE>threshold){
														  RE-=0.8*threshold;
														  eRTT1=Math.abs(RTT-RTT*RE);
													
													  }
													}
												
												System.out.print("\t"+A+"\t"+B+"\t"+eRTT1+"\t\t"+RTT+"\t\t"+RE+"\t\t"+processTime+"\n");	
												String str=B+" "+eRTT1+" "+RTT+" "+RE+" "+processTime;
												Vector vec=new Vector(1);
												vec.add(str);
												ParseExperimentDat.parseNetCoord(A, choiceofCoord,vec);
												vec.clear();
												staTime.add(processTime)	;
												staRE.add(RE);
													break;
												}
												case ERROR:
												case TIMEOUT:{							
														System.err.println("error, empty object!");		
														
													break;
												}		
							
												}
												//=================
												b.join();
											
												}
												
												
											},B);
									
								} catch (MalformedURLException e1) {
									log.error("Wrong URL: " + e1);
								} catch (UnknownHostException e2) {
									log.error("Unknown host: " + e2);
								}
									
									
									
									break;
								}
								
								case ERROR:
								case TIMEOUT:{
									log.warn("not connected");
									b.join();
									break;
								}
								
								
								}
								
							}	
							
						});
						
						}
						
					}
					//				
				}
				//==============================================================
				EL.get().registerTimerCB(time,new CB0() {
					protected void cb(CBResult result) {
					//	System.out.println("\n\nCallback is received!\n\n");
						System.out.print( "\tAverage Time: "+staTime.getSum().floatValue()/staTime.getSize()+"\tAverage Relative Error: "+staRE.getSum()/staRE.getSize()+"\n");
						System.out.print( "\tMedian Time: "+staTime.getPercentile(.5)+"\tMedian Relative Error: "+staRE.getPercentile(.5)+"\n");
						
						cbDone.call(CBResult.OK(), vec);
					}
				});
				
				
			}
			
			
			
			
			
			
			
			
			
			
		/*
			final Barrier barrierRTT=new Barrier(true);
			
			if(nodes==null||nodes.isEmpty()){
				cbDone.call(CBResult.ERROR(), vec);
			}else{
				
				
				
				//Hashtable<String, Object> remoteNodesMap=new Hashtable<String, Object>(2);			
				try{
				
					Iterator<String> ier = nodes.iterator();
					
					//barrier.setNumForks(nodes.size());
					
					while(ier.hasNext()){
						
					final String target=ier.next();	
					//clear
					Vector<String> CNodes=new Vector<String>(2);
						
					for(int i=0;i<candidates.size();i++){
						String ip=candidates.get(i);
						if(target.equalsIgnoreCase(ip)){
							continue;
						}else{
							CNodes.add(ip);
						}
					}
					//log.info("target: "+target);
					barrierRTT.fork();
					apiComm.call(getHTTP(target),
							"Nina.estimateRTTWithErrorElapsedTime", new XMLRPCCB() {

								
								public void cb(CBResult result, Object arg0) {
									if(arg0==null){
										System.err.println("error, empty object!");
									}else{
										
									String separator="####################################\n";
									//System.out.println(separator);
									System.out.println("Target:"+target);
									System.out.println("Result of estimateRTT is: ");
									System.out.println(POut.toString((Vector<Object>)arg0));
									
									
									//format: <ipAddress, rtt value>
									//
									//Vector ve=(Vector)arg0;								
									//vec.add("#Target: "+target);
									String s=null;
									if(choiceofCoord==0){
										s="Nina Coordinate";
									}else {
										s="Vivaldi"; 
									}
									
									Vector ve=new Vector(1);
									ve.add(arg0.toString());
									ParseExperimentDat.parseNetCoord(target, choiceofCoord,ve);
									vec.add(ve);
									
									
									vec.add(separator);
									vec.add("#Type:"+s+"\n");
									vec.add(separator);
									
									
									
									//vec.add(separator);
									}
									
									barrierRTT.join();
								}
								
								
							},CNodes, choiceofCoord);
					
					}
					long time=15*1000;
					EL.get().registerTimerCB(time,new CB0() {
						protected void cb(CBResult result) {
						//	System.out.println("\n\nCallback is received!\n\n");
							cbDone.call(CBResult.OK(), vec);
						}
					});
					
				
				} catch (MalformedURLException e1) {
					log.error("Wrong URL: " + e1);
				} catch (UnknownHostException e2) {
					log.error("Unknown host: " + e2);
				}
				
			}*/
				
			
		}
		
		
		
		//=================================================================
		
		
		/**
		 * use Nina method to find 1 nearest neighbor for a set of nodes
		 * return the nearest nodes, the RTT, and the latency
		 * @param nodes
		 * @param k=1
		 */
		void find1NN4NodeListBasedOnNina(Vector<String> nodes,int K, final CB1<Vector> cbDone){
			
			final Vector vec=new Vector();
			final Barrier barrier=new Barrier(true);
			if(nodes==null||nodes.isEmpty()){
				System.err.println("Error, empty nodes");
				cbDone.call(CBResult.ERROR(), vec);
			}else{
				System.out.println("nodes: "+nodes.size());
				try {
					
					
					
					if(nodes.size()>1){
					
					Iterator<String> ier = nodes.iterator();
					
					//barrier.setNumForks(nodes.size());
					
					while(ier.hasNext()){
									
						//============================
						final String target=ier.next();
						//final String target=target1;
						barrier.fork();
					apiComm.call(getHTTP(target),
							"Nina.queryKNearestNeighborsAndElapsedTime", new XMLRPCCB() {

								
								public void cb(CBResult result, Object arg0) {
									
									if(arg0==null){
										System.err.println("error, empty object!");
									}else{
										
								/*	System.out.println("Target: "+target);	
									System.out.println("Result of KNN is: "+ arg0.toString());
								*/	
									//format: <ipAddress, rtt value, elapsed time>
									//String separator=	"\n####################################\n";
									//Vector<Object> ve=(Vector<Object>)arg0;
									System.out.println("Target:"+target);
									Vector v1=new Vector(1);
									v1.add(arg0.toString());
									ParseExperimentDat.parseKNN(target, v1);
									vec.add(v1);
																	
									/*vec.add(separator);
									vec.add(target);
									vec.add(separator);*/
									
									//vec.add(separator);
									
								/*	try {
										TestCase.append("\n"+target+"\n");
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}*/
									}
									
									barrier.join();
								}
								
								
							},target,K );
					
					long time=20*1000;
					EL.get().registerTimerCB(time,new CB0() {
						protected void cb(CBResult result) {
							System.out.println("\n\nCallback is received!\n\n");
							cbDone.call(CBResult.OK(), vec);
						}
					});	
					
					}

					}else if(nodes.size()<=1){
						//=========================================
						
						final String target=nodes.get(0);
						log.info("@@: address: "+target);
						
						
						apiComm.call(getHTTP(),
								"Nina.queryKNearestNeighborsAndElapsedTime", new XMLRPCCB() {

									
									public void cb(CBResult result, Object arg0) {
										
										if(arg0==null){
											System.err.println("error, empty object!");
										}else{
											
									/*	System.out.println("Target: "+target);	
										System.out.println("Result of KNN is: "+ arg0.toString());
									*/	
										//format: <ipAddress, rtt value, elapsed time>
										//String separator=	"\n####################################\n";
										//Vector<Object> ve=(Vector<Object>)arg0;
										System.out.println("Target:"+target);
										Vector v1=new Vector(1);
										v1.add(arg0.toString());
										ParseExperimentDat.parseKNN(target, v1);
										vec.add(v1);
																		
										/*vec.add(separator);
										vec.add(target);
										vec.add(separator);*/
										
										//vec.add(separator);
										
									/*	try {
											TestCase.append("\n"+target+"\n");
										} catch (IOException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}*/
										}
										
										cbDone.call(CBResult.OK(), vec);	
									}
									
									
								},target,K );	
						
						
						
						
					}else{
						cbDone.call(CBResult.OK(), vec);	
					}
					
					
				} catch (MalformedURLException e1) {
					log.error("Wrong URL: " + e1);
				} catch (UnknownHostException e2) {
					log.error("Unknown host: " + e2);
				}
				
				
				
			}
			
		}
		
		
		/**
		 * use Meridian method to find 1 nearest neighbor for a set of nodes
		 * @param nodes
		 */
		void find1NN4NodeListBasedOnMeridian(Vector<String> nodes, final CB1<Vector<Object>> cbDone){
			
			final Vector<Object> vec=new Vector<Object>();
			final Barrier barrier=new Barrier(true);
			
			if(nodes==null||nodes.isEmpty()){
				cbDone.call(CBResult.ERROR(), vec);
			}else{
			
				try {
					Iterator<String> ier = nodes.iterator();
					
					barrier.setNumForks(nodes.size());
					
					while(ier.hasNext()){
					final String ipAddr=ier.next();	
					apiComm.call(getHTTP(),
							"Nina.MeridianFor1NN", new XMLRPCCB() {

								
								public void cb(CBResult result, Object arg0) {
									if(arg0==null){
										System.err.println("error, empty object!");
									}else{
									//System.out.println("Result of Meridian is: "+ arg0.toString());
									
									//format: <ipAddress, rtt value, elapsed time>
									//
									String separator=	"\n\n####################################\n\n";	
									System.out.println("Target:"+ipAddr);
									//can not be casted as vector data structure 10.31
									//Vector<Object> ve=(Vector<Object>)arg0;
									Vector ve=new Vector(1);
									ve.add(arg0.toString());
									ParseExperimentDat.parseMeridian(ipAddr, ve);
									vec.add(ve);	
									
									
									/*vec.add(ipAddr);
									vec.add(separator);
									
									
									vec.add(separator);*/
				
									}
									
									barrier.join();
								}
								
								
							}, ipAddr);
					
					}
					long time=15*1000;
					EL.get().registerTimerCB(time, new CB0() {
						protected void cb(CBResult result) {
							System.out.println("\n\nCallback is received!\n\n");
							cbDone.call(CBResult.OK(), vec);
						}
					});
					
					
				} catch (MalformedURLException e1) {
					log.error("Wrong URL: " + e1);
				} catch (UnknownHostException e2) {
					log.error("Unknown host: " + e2);
				}
				
				
				
				
				
			}
		}
		
		
		
		/**
		 * use direct probes to find k nearest neighbor for a set of nodes
		 * @param nodes
		 * @param k=1
		 * @param AllNodes
		 */
		void find1NN4NodeListBasedOnProbes(Vector<String> nodes, int k, Vector<String> AllNodes, final CB1<Vector> cbDone){
			
			final Vector vec=new Vector();
		
			
			if(nodes==null||AllNodes==null||nodes.isEmpty()||AllNodes.isEmpty()){
				System.out.println("Error, empty nodes!");
				cbDone.call(CBResult.ERROR(), vec);
			}else{
			
				if(nodes.size()>1){
					final Barrier barrier=new Barrier(true);
				try {
					Iterator<String> ier = nodes.iterator();
					
					barrier.setNumForks(nodes.size());
					
					while(ier.hasNext()){
					final String target=ier.next();	
					
					apiComm.call(getHTTP(target),
							"Nina.PingAllNodesforKNearestNode", new XMLRPCCB() {

								
								public void cb(CBResult result, Object arg0) {
									if(arg0==null){
										System.err.println("error, empty object!");
									}else{
										
									String separator=	"\n####################################\n";
									//System.out.println(separator);
									
									//format: <ipAddress, rtt value>
									//
									System.out.println("Target:"+target);
									
									Vector v1=new Vector(1);
									v1.add(arg0.toString());
									ParseExperimentDat.parseProbe(target, v1);
									
									vec.addAll(v1);
									
									
									
									/*vec.add(separator);
									vec.add(target);
									vec.add(separator);*/
									}
									
									barrier.join();
								}
								
								
							},AllNodes,k );
					
					}
					long time=15*1000;
					EL.get().registerTimerCB(time, new CB0() {
						protected void cb(CBResult result) {
							System.out.println("\n\nCallback is received!\n\n");
							cbDone.call(CBResult.OK(), vec);
						}
					});
					
					
				} catch (MalformedURLException e1) {
					log.error("Wrong URL: " + e1);
				} catch (UnknownHostException e2) {
					log.error("Unknown host: " + e2);
				}
				
				}else{
					//=============================
					//1 node
					
					try{
						
						final String target=nodes.get(0);
						apiComm.call(getHTTP(target),
								"Nina.PingAllNodesforKNearestNode", new XMLRPCCB() {

									
									public void cb(CBResult result, Object arg0) {
										if(arg0==null){
											System.err.println("error, empty object!");
										}else{
											
										String separator=	"\n####################################\n";
										//System.out.println(separator);
										
										//format: <ipAddress, rtt value>
										//
										System.out.println("Target:"+target);
										
										Vector v1=new Vector(1);
										v1.add(arg0.toString());
										
										
										ParseExperimentDat.parseProbe(target, v1);
										
										vec.addAll(v1);
										
										
										
										/*vec.add(separator);
										vec.add(target);
										vec.add(separator);*/
										}
										cbDone.call(CBResult.OK(), vec);
									}
									
									
								},AllNodes,k );
						
						
					} catch (MalformedURLException e1) {
						log.error("Wrong URL: " + e1);
					} catch (UnknownHostException e2) {
						log.error("Unknown host: " + e2);
					}
					
					
				}
				
				
				
				
			}
		}
		
		/**
		 * TODO: 
		 * @param nodes
		 * @param peer
		 * @param K
		 * @param cbDone
		 */
		void findKNN4NodeBasedOnConstrainedNina(Vector<String> nodes, Vector<String> peer, int K,final CB1<Vector<Object>> cbDone){
		
			final Vector vec=new Vector();
			final Barrier barrier=new Barrier(true);
			
			if(nodes==null||nodes.isEmpty()){
				System.out.println("Error, empty nodes!");
				cbDone.call(CBResult.ERROR(), vec);
			}else{
			
				try {
					
					if(nodes.size()>1){
					Iterator<String> ier = nodes.iterator();
					
					barrier.setNumForks(nodes.size());
					
					while(ier.hasNext()){
					final String target=ier.next();	
					
					//=====================================
					if(peer.contains(target)){
						peer.remove(target);
					}
					//=====================================
					
					apiComm.call(getHTTP(target),
							"Nina.constrainedKNN", new XMLRPCCB() {

								
								public void cb(CBResult result, Object arg0) {
									if(arg0==null){
										System.err.println("error, empty object!");
									}else{
									/*log.info("Target: "+target+", "+arg0.toString() );
								   */ String separator=	"\n####################################\n";
									//System.out.println(separator);
								   System.out.println("Target:"+target);
									//format: <ipAddress, rtt value>
									//
									//Vector<Object> ve=(Vector<Object>)arg0;
								   
									Vector v1=new Vector(1);
									v1.add(arg0.toString());
									
									if(arg0.toString().contains("[")){
									ParseExperimentDat.parseconstrainedKNN(target, v1);
									vec.add(v1);
									}
									
									//vec.addAll(ve);
									
									
									//vec.addAll(ve);
									
									
									
									/*vec.add(separator);
									vec.add(target);
									vec.add(separator);*/
									}
									
									barrier.join();
								}
								
								
							},target, peer,K);
					
					}
					long time=10*1000;
					
				
					EL.get().registerTimerCB(time, new CB0() {
						protected void cb(CBResult result) {
							System.out.println("\n\nCallback is received!\n\n");
							cbDone.call(CBResult.OK(), vec);
						}
					});
					
					}else{
						
						final String target=nodes.get(0);
						System.out.println("Address: "+target);
						
						//=====================================
					
						System.out.println(peer.toString());
						//=====================================
						
						
						apiComm.call(getHTTP(),
								"Nina.constrainedKNN", new XMLRPCCB() {

									
									public void cb(CBResult result, Object arg0) {
										if(arg0==null){
											System.err.println("error, empty object!");
										}else{									
								
									    String separator=	"\n####################################\n";
										//System.out.println(separator);
									   System.out.println("Target: "+target+", "+arg0.toString() );
										//format: <ipAddress, rtt value>
										//
										//Vector<Object> ve=(Vector<Object>)arg0;
										Vector v1=new Vector(1);
										v1.add(arg0.toString());
										
										ParseExperimentDat.parseconstrainedKNN(target, v1);
										vec.add(v1);
										
										}
										
										
										cbDone.call(result, vec);
									}
									
									
								},target, peer,K);
						
						
						
					}
					
					
					
				} catch (MalformedURLException e1) {
					log.error("Wrong URL: " + e1);
				} catch (UnknownHostException e2) {
					log.error("Unknown host: " + e2);
				}
				
				
				
				
				
			}
		}
		
		
		/**
		 * find k nearest nodes for a node A
		 * @param IPAddr
		 * @param K
		 */
		void findKNN4NodeBasedOnNina(final String IPAddr, int K, final CB1<Vector<Object>> cbDone){
			final Vector<Object> vec=new Vector<Object>();
					
			if(IPAddr==null){
				System.out.println("Error! empty vector");
				cbDone.call(CBResult.ERROR(), vec);
			}else{
				try {
					
						
					apiComm.call(getHTTP(),
							"Nina.queryKNearestNeighborsAndElapsedTime", new XMLRPCCB() {

								
								public void cb(CBResult result, Object arg0) {
									if(arg0==null){
										System.err.println("error, empty object!");
									}else{
									System.out.println("Result of  queryKNearestNeighborsAndElapsedTime is: "+ arg0.toString());
									
									//format: <ipAddress, rtt value, elapsed time>
									//
									String separator=	"\n\n####################################\n\n";
									
									Vector<Object> ve=(Vector<Object>)arg0;
									
								
									vec.addAll(ve);
									ParseExperimentDat.parseKNN(IPAddr, vec);
									/*
									vec.add(IPAddr);
									vec.add(separator);
									vec.add(separator);*/
									}
									
									cbDone.call(CBResult.OK(), vec);
								}
								
								
							},IPAddr,K );
					
				
					
					
				} catch (MalformedURLException e1) {
					log.error("Wrong URL: " + e1);
				} catch (UnknownHostException e2) {
					log.error("Unknown host: " + e2);
				}
				
				
				
			}
			
		}
		
		/**
		 * a network coordinate based KNN
		 * @param k
		 * @param AllNodes
		 * @param cbDone
		 */
		void findKNN4NodeBasedOnNetCoord(int K, Vector<String> AllNodes, final CB1<Vector> cbDone){
			
			Hashtable<String, Object> remoteNodesMap=new Hashtable<String, Object>(2);
			if(AllNodes!=null&&!AllNodes.isEmpty()){
				Iterator<String> ier = AllNodes.iterator();
				while(ier.hasNext()){
					String ip=ier.next();
					remoteNodesMap.put(ip, ip);
				}
			}
			
			final Vector vec=new Vector();
					
			
				try {
										
					apiComm.call(getHTTP(),
							"Nina.KNNBasedOnNetworkCoord", new XMLRPCCB() {

								
								public void cb(CBResult result, Object arg0) {
									if(arg0==null){
										System.err.println("error, empty object!");
									}else{
									System.out.println("Result of  queryKNearestNeighborsAndElapsedTime is: "+ arg0.toString());
									
									//format: <ipAddress, rtt value, elapsed time>
									//
									String separator=	"\n\n####################################\n\n";
									Vector ve=(Vector)arg0;
									
									
									vec.addAll(ve);
									//ParseExperimentDat.parseNetCoord(, vec);
									
									vec.add(separator);
									}
									
									cbDone.call(CBResult.OK(), vec);
								}
								
								
							},remoteNodesMap,K );
					
				
					
					
				} catch (MalformedURLException e1) {
					log.error("Wrong URL: " + e1);
				} catch (UnknownHostException e2) {
					log.error("Unknown host: " + e2);
				}	
			
		}
		
		
		/**
		 * entry point
		 * @param args
		 */
		public static void main(String[] args){
			
				
			PeriodicNinaQuery test=new PeriodicNinaQuery(args);
			final ExecutorService exec = Executors.newFixedThreadPool(5);
			exec.execute(test);
		}		
			
}
