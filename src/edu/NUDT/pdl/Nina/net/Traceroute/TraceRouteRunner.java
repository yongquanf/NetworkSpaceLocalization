package edu.NUDT.pdl.Nina.net.Traceroute;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.TimeZone;


import edu.NUDT.pdl.Nina.util.HashSetCache;
import edu.NUDT.pdl.Nina.util.MainGeneric;
import edu.NUDT.pdl.Nina.util.Util;
import edu.harvard.syrah.prp.POut;



/**
 * @author 
 *
 * The TraceRouteRunner class handles asynchronous recording of 
 * traceroute data.
 */
public class TraceRouteRunner implements Runnable {
	
	public class TraceEntry {
		public float rtt[] = new float[]{-1f,-1f,-1f};
		public String router[] = new String[3];
		public int numRouters = 0;
		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return rtt[0]+" "+rtt[1]+" "+rtt[2]+" "+router[0]+" "+router[1]+" "+router[2]+"\n";
		}
		
		
	}
	
	public class TraceResult {
		public String source;
		public String dest;
		public ArrayList<TraceEntry> entries;
		public long timestamp = Util.currentGMTTime();
	}
	
	private class TraceRouteRun{
		public String ip;
		public boolean recordInDB = true;
		
		public TraceRouteRun(String ip){
			this.ip = ip;
		}
		
		public TraceRouteRun(String ip, boolean record){
			this.ip = ip;
			recordInDB = record;
		} 
		@Override
		public boolean equals(Object obj) {

			return obj!=null && obj instanceof TraceRouteRun && ip.equals(((TraceRouteRun)obj).ip);
		}
		@Override
		public int hashCode() {

			return ip.hashCode();
		}
		
		
	}



	private static final boolean DEBUG = false;
	

	
    static BufferedReader in;
    static Calendar myCalendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
   

	private static TraceRouteRunner self;
	private static Process p;

	private static boolean active = true;
    TraceResult tr;
    HashSetCache<TraceRouteRun> pendingRuns;
    HashSetCache<TraceRouteRun> pendingRunsPrio;

    private TraceRouteRunner(){
    	pendingRuns = new HashSetCache<TraceRouteRun>(200);
    	pendingRunsPrio = new HashSetCache<TraceRouteRun>(50);
    	MainGeneric.createThread("TraceRouteRunner", this);
    	
    }
    
    static boolean isShuttingDown(){
		return false;
	}
    
    
    public static TraceRouteRunner getInstance(){
    	if (self==null && !isShuttingDown()){
    		active = true;
    		self = new TraceRouteRunner();
    	}
    	return self;
    }
    
	public void addIp( String dest ){
		synchronized(pendingRuns){
			pendingRuns.add(new TraceRouteRun(dest));
			pendingRuns.notifyAll();
		}
	}
	
	public void addIp( String dest, boolean recordInDB ){
		synchronized(pendingRuns){
			pendingRuns.add(new TraceRouteRun(dest, recordInDB));
			pendingRuns.notifyAll();
		}
	}
	
	public static void stop(){
		active = false;
		if (self==null || 
				(self.pendingRuns==null && self.pendingRunsPrio==null)) return;
		if (p!=null){
			p.destroy();
		}
		if (self.pendingRuns!=null){
			synchronized(self.pendingRuns){
				self.pendingRuns.notifyAll();
			}
		}

//		if (in!=null){
//			try {
//				in.close();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
		self = null;
		//in = null; // will be closed already
		myCalendar = null;
		p = null;
		
		
	}

	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		/** the ip address to trace routes on */
		String dest;
		
		TraceRouteRun trr = null;
		while (!isShuttingDown()){
			synchronized(pendingRuns){
				while (pendingRuns.size()==0 && pendingRunsPrio.size()==0 
						&& !isShuttingDown()){
					try {
						pendingRuns.wait(30*1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				Iterator<TraceRouteRun> it;
				if (pendingRunsPrio.size()>0){
					
					it = pendingRunsPrio.iterator();
					trr = it.next();
					it.remove();
					if (DEBUG) System.out.println("High priority traceroute to "+trr.ip);
				} else {
					it = pendingRuns.iterator();
					if (!it.hasNext()){
						continue;
					}
					trr = it.next();
					it.remove();
					if (DEBUG) System.out.println("Normal priority traceroute to "+trr.ip);
				}
			}
			if (!active) return;
			tr = new TraceResult();
			dest = trr.ip;
			
		ArrayList<TraceEntry> data = new ArrayList<TraceEntry>();
		 String line = null;
		try {
            
			if (isWindows()) {

                p = Runtime.getRuntime().exec("tracert -d -w 3000 " + dest);
                in = new BufferedReader(new InputStreamReader(
                            p.getInputStream()));

               

                while ((line = in.readLine()) != null) {                	
                    // parse data
                	String[] entries = line.split("[\\s]+");
                	if (entries.length<=1) continue;
                	try {
                		Integer.parseInt(entries[1]);
                	} catch (NumberFormatException e){
                		continue;
                	}
                	TraceEntry te = new TraceEntry();
                	int i = 2;
                	int numEntries = 0;
                	while (i < entries.length-1){
                        try{
                		te.rtt[numEntries] = getValue(entries[i]);
                        } catch (NumberFormatException e){
                            // failure during traceroute
                            te.router[te.numRouters++] = getRouterIp(entries[i]);
                            break;
                        }
                		if (te.rtt[numEntries++]<0) i++;
                		else i+=2;
                		if (numEntries==te.rtt.length) break;
                		
                	}
                	
                	te.router[te.numRouters++] = getRouterIp(entries[entries.length-1]);  
                	data.add(te);
                	if (data.size()>=30) break;
                }

                System.out.println(POut.toString(data));
                // then destroy
                p.destroy();         
                
            } else if (isLinux() ) {
                p = Runtime.getRuntime().exec("traceroute -n -w 3 " + dest);

                in = new BufferedReader(new InputStreamReader(
                            p.getInputStream()));
                

                while (in!=null && (line = in.readLine()) != null) {                	
                    // parse data
                	
                	String[] entries = line.split("[\\s]+");
                	//System.out.println(Arrays.toString(entries));
                	if (entries.length<=1) continue;
                	int index = 0;
                	while (entries[index].equals("")) index++;
                	try {
                		Integer.parseInt(entries[index]);
                	} catch (NumberFormatException e){
                		// not an entry we care about
                		continue;
                	}
                	TraceEntry te = new TraceEntry();
                	int rttCount = 0;
                	for (int i = index+1; i < entries.length; i++){
//                		System.out.println("i:"+i);
                		if (entries[i].contains("*")){
                			te.router[te.numRouters++] = getRouterIp(entries[i]);
                			te.rtt[rttCount++] = -1;
                			//System.out.println("Found asterisk!");
                		}
                		else {
                			if(entries[i].matches("([0-9]{1,3}\\.){3}[0-9]{1,3}")){
                				// if code reaches this point, this is an ip
                				te.router[te.numRouters++] = getRouterIp(entries[i]);
                				//System.out.println("Found router!");
                			}
                			else{
                				// not an ip, so it must be a value
                				try {
	                				te.rtt[rttCount] = Float.parseFloat(entries[i++]);
	                				rttCount++;
                				} catch (NumberFormatException e ){
                					
                					continue; // ignore the garbage
                				}
                				
                				//System.out.println("Found rtt!");
                				// i++ skips the "ms" entry
                				
                			}
                		}
                	}
                 
                	data.add(te);
                	if (data.size()>30) break;
                }

                // then destroy
                if (p!=null) p.destroy();
            }
            
/*            if (trr.recordInDB){
	            tr.dest = dest;
	            tr.source = MainGeneric.getPublicIpAddress();
	            tr.entries = data;
	            //Statistics.getInstance().addTraceRouteResult(tr);
            }*/
            
            
            if (in!=null) in.close();
            if (p!=null) p.destroy();            
        } catch (IOException e) {
        	if (p!=null)p.destroy();
            //e.printStackTrace();
        } catch (Exception e){
        	try {
				if (in !=null ) in.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        	
        	if (p!=null) p.destroy();
            if (active){
	        	//Statistics.getInstance().reportError(e);
	          //  Statistics.getInstance().reportError(new 
	            //		RuntimeException("Error input was: \n"+line));
            }
        }
        
		} // end while active

	}

	public static boolean isWindows() {
		String osName = System.getProperty("os.name");
		if (osName.indexOf("Windows") >= 0 || osName.indexOf("windows") >= 0) {
			return true;
		} else {
			return false;
		}

	}

	
	
	public static boolean isLinux() {
		return !isWindows();
	}
	
	
	

	private String getRouterIp(String maybeIp) {
		if(maybeIp.matches("([0-9]{1,3}\\.){3}[0-9]{1,3}")){
			// if code reaches this point, this is an ip	
			//Statistics.getInstance().addRouterForLookup(maybeIp);
			return maybeIp;
			//System.out.println("Found router!");
		}
		//Statistics.getInstance().addRouterForLookup("unknown");
		return "unknown";
	}

	private float getValue(String val) {
		if (val.contains("<"))return 0;
		else if (val.contains("*")) return -1;
    	else return Float.parseFloat(val);
	}

	public void addIp(String dest, boolean recordInDB, boolean sameCluster) {
		if (!sameCluster){
			synchronized(pendingRuns){
				pendingRuns.add(new TraceRouteRun(dest, recordInDB));
				pendingRuns.notifyAll();
			}
		} else {
			synchronized(pendingRuns){
				pendingRunsPrio.add(new TraceRouteRun(dest, recordInDB));

				pendingRuns.notifyAll();
			}
		}
		
	}

}