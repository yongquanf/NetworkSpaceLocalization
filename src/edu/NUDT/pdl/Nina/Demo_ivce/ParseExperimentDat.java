package edu.NUDT.pdl.Nina.Demo_ivce;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.util.Map.Entry;

import edu.NUDT.pdl.Nina.Sim.SimCoord.Statistic;
import edu.harvard.syrah.prp.Log;
import edu.harvard.syrah.prp.PUtil;

public class ParseExperimentDat {

	private static Log log=new Log(ParseExperimentDat.class);
	
	
	static Hashtable<String,Vector<CoordRecord>> cachedNetCoord=new Hashtable<String,Vector<CoordRecord>>(2);
	static Hashtable<String,Vector<KNNRecord>> cachedKNNNina=new Hashtable<String,Vector<KNNRecord>>(2);
	static Hashtable<String,Vector<PingRecord>> cachedPings=new Hashtable<String,Vector<PingRecord>>(2);
	static Hashtable<String,Vector<KNNRecord>> cachedMeridian=new Hashtable<String,Vector<KNNRecord>>(2);
	static Hashtable<String,Vector<KNNRecord>> cachedconstrainedKNNNina=new Hashtable<String,Vector<KNNRecord>>(2);
	

	public static String choice;
	public static String NetCoord="cachedNetCoord";
	public static String KNNNina="cachedKNNNina";
	public static String Pings="cachedPings";
	public static String Meridian="cachedMeridian";
	public static String constrainedKNN="cachedKNN";
	
	static boolean offset=true;
	static double threshold=.6;
	
	
	public static void clear(String choice){
		
	if(choice.equalsIgnoreCase(NetCoord)){
			
		cachedNetCoord.clear();
			
		}else if(choice.equalsIgnoreCase(KNNNina)){
			
			cachedKNNNina.clear();
			
			
		}else if(choice.equalsIgnoreCase(Pings)){
			
			cachedPings.clear();
			
			
		}else if(choice.equalsIgnoreCase(Meridian)){
			
			 cachedMeridian.clear();
			
		}else if(choice.equalsIgnoreCase(constrainedKNN)){
			cachedconstrainedKNNNina.clear();
		
		}else {
			log.warn("No matched choice");
		}
	}
	/**
	 * print the results
	 * @param choice
	 * @return
	 */
	public static void Print(PrintWriter  s, String choice)throws Exception{
		
		//StringBuilder s=new StringBuilder(100);
		
		
		Statistic<Double> sta=new Statistic<Double>();
		
		if(choice.equalsIgnoreCase(NetCoord)){
			
			
			Iterator<Entry<String, Vector<CoordRecord>>> ier = cachedNetCoord.entrySet().iterator();
			String header="\n\t\tType\t\tfrom\t\t\t\tto\t\t\t\t\t\t\t\t\t\t"+"realRTT\t\t\t\t"+"coordinate_distance\t\t"+"RelativeError\n";
			s.print(header);
			log.debug(header);
			
			double avgERR=0.;
			double avgTime=0.;
			int counter=0;
			int type=-1;
			while(ier.hasNext()){
				Entry<String, Vector<CoordRecord>> t = ier.next();
				Iterator<CoordRecord> tmp = t.getValue().iterator();
				while(tmp.hasNext()){
					CoordRecord rr = tmp.next();				
					sta.add(rr.RelativeError);
					type=rr.type;						
					avgTime+=rr.elapsedTime;
					counter++;
					s.print("\n\t"+rr.toString());
					log.debug("\n\t"+rr.toString());
										
				}
				s.print("\n");
				log.debug("\n");
				s.flush();
			}
		s.print("\n--------------------------------------------------\n");
		log.debug("\n--------------------------------------------------\n");
		//Nina
		
		if(counter>0){
		s.print("\t\tAverage Relative Error: "+sta.getSum()/counter+" \t Average Time: "+avgTime/counter+"\n");	
		log.debug("\t\tAverage Relative Error: "+sta.getPercentile(.5)+" \t Average Time: "+avgTime/counter+"\n");
		}
		s.flush();
		
		//clear
		cachedNetCoord.clear();
		
		}else if(choice.equalsIgnoreCase(KNNNina)){
			
			
			 Iterator<Entry<String, Vector<KNNRecord>>> ier = cachedKNNNina.entrySet().iterator();
				String header="\n\t\tfrom\t\t\t\t"+"to\t\t\t\t\t\t\t\t\t\t"+"RTT\t\t\t\tRank\n";
				s.print(header);
				System.out.print(header);
				double time=-1;
				int count=0;
			 while(ier.hasNext()){
				Entry<String, Vector<KNNRecord>> t = ier.next();
				Iterator<KNNRecord> tmp = t.getValue().iterator();
				
				while(tmp.hasNext()){
					KNNRecord rec = tmp.next();
					time+=rec.elapsedTime;
					s.print("\n\t"+rec.toString());
					System.out.print("\n\t"+rec.toString());
					count++;
				}
				s.print("\n");
				System.out.print("\n");
				s.flush();
			}
			 s.print("\n--------------------------------------------------\n");
			 System.out.print("\n--------------------------------------------------\n");
			 s.print("\t\tTime: "+time/count);
			 System.out.print("\t\tTime: "+time/count);
			 s.flush();
			 
			 cachedKNNNina.clear();
			
		}else if(choice.equalsIgnoreCase(Pings)){
			
			Iterator<Entry<String, Vector<PingRecord>>> ier = cachedPings.entrySet().iterator();
			String header="\n\tfrom\t\t\t\t"+"to\t\t\t\t\t\t\t\t\t\t\t"+"RTT\t"+"Rank\n";
			s.print(header);
			double time=-1;
			int count=0;
			while(ier.hasNext()){
				Entry<String, Vector<PingRecord>> t = ier.next();

				Iterator<PingRecord> tmp = t.getValue().iterator();
			
				while(tmp.hasNext()){
					PingRecord rec = tmp.next();
					time+=rec.time;
					s.print("\n\t"+rec.toString());
					count++;
				}
				s.print("\n");
				s.flush();
			}
			 s.print("\n--------------------------------------------------\n");
			 s.print("\t\tTime: "+time/count);
			 s.flush();
			 
			 cachedPings.clear();
			
		}else if(choice.equalsIgnoreCase(Meridian)){
			
			Iterator<Entry<String, Vector<KNNRecord>>> ier = cachedMeridian.entrySet().iterator();
			String header="\n\tfrom\t\t\t\t"+"to\t\t\t\t\t\t\t\t\t\t\t"+"RTT\t"+"elapsedTime\n";
			s.print(header);
			while(ier.hasNext()){
				Entry<String, Vector<KNNRecord>> t = ier.next();

				Iterator<KNNRecord> tmp = t.getValue().iterator();
				while(tmp.hasNext()){
					s.print("\n\t"+tmp.next().toString());
				}
				s.print("\n");
				s.flush();
			}
			s.flush();
			
			cachedMeridian.clear();
			
		}else if(choice.equalsIgnoreCase(constrainedKNN)){
		

			 Iterator<Entry<String, Vector<KNNRecord>>> ier = cachedconstrainedKNNNina.entrySet().iterator();
				String header="\n\t\tfrom\t\t\t\t"+"to\t\t\t\t\t\t\t\t\t\t"+"RTT\t\t\t\tRank\n";
				s.print(header);
				System.out.print(header);
				double time=-1;
				
			 while(ier.hasNext()){
				Entry<String, Vector<KNNRecord>> t = ier.next();
				Iterator<KNNRecord> tmp = t.getValue().iterator();				
				while(tmp.hasNext()){
					KNNRecord rec = tmp.next();
					time=rec.elapsedTime;
					s.print("\n\t"+rec.toString());
					System.out.print("\n\t"+rec.toString());
				}
				s.print("\n");
				System.out.print("\n");
				s.flush();
			}
			 s.print("\n--------------------------------------------------\n");
			 System.out.print("\n--------------------------------------------------\n");
			 s.print("\t\tTime: "+time);
			 System.out.print("\t\tTime: "+time);
			 s.flush();
			 
			 cachedconstrainedKNNNina.clear();
		
		}else {
			log.warn("No matched choice");
		}
		
		s.flush();

	}
	
	
	/**
	 * parse the coordinate
	 * @param target
	 * @param vec
	 */
	public static void parseNetCoord(String target, int type, Vector vec){
		//separate a sequence of records by" /n" "/n" pair
		//each record consists of <addr, estimated RTT, real RTT, relative error>
		String str=vec.toString();
	//	log.debug(str);
		if(!testValid(target,str)){			
			return;
		}
	
		while(str.startsWith("[")){
			str=str.substring(1);
		}
		while(str.endsWith("]")){
			str=str.substring(0, str.length()-1);
		}		
	//	log.debug(str);
		
		
		String[]val=str.split("[\t, \n]+");
		
		if(val==null||val.length==0){
			log.warn("empty string!");
			return;
		}
		//log.debug("$: length: "+val.length);
		
		int counter=1;
		CoordRecord record;
		String from=target;
		String to="";
		Object coord=null;
		double realRTT=-1;
		double coordinate_distance=-1;
		double RelativeError=-1;
		double receivedRTT=-1;
		
		for(int i=0;i<val.length;i++){
			String curString=val[i];
			if(curString.isEmpty()){
				continue;
			}
			curString=trim(curString);
			//start a new record
			//=======================================
			if(counter%5==1){
				//to
				to=curString;
			}else if(counter%5==2){
				realRTT=Double.parseDouble(curString);
				
			}else if(counter%5==3){
				coordinate_distance=Double.parseDouble(curString);;
			}else if (counter%5==4){
				RelativeError=Double.parseDouble(curString);
				//create new record
				
			}else{
				receivedRTT=Double.parseDouble(curString);
				record=new  CoordRecord(from,to,coord,realRTT,coordinate_distance,RelativeError);
				record.type=type;
				record.elapsedTime=receivedRTT;
				
				/*if(record.type==0){
				  if(record.RelativeError>threshold){
					  record.RelativeError-=0.8*threshold;
					  record.coordinate_distance=Math.abs(record.realRTT-record.realRTT*record.RelativeError);
					  //record.realRTT=Math.abs(record.coordinate_distance/(1-record.RelativeError));
				  }
				}*/
				
				if(!cachedNetCoord.containsKey(from)){
					cachedNetCoord.put(from,new Vector<CoordRecord>(2));
				}
				cachedNetCoord.get(from).add(record);
				//print the record
				//log.debugln();
			}
			
			//=======================================
			counter++;
		}
	}
	
	/**
	 * parse the Nina coordinate
	 * @param vec
	 */
	public static void parseProbe(String target, Vector vec){
		
		String str=vec.toString();
		log.debug(str);
		
		if(!testValid(target,str)){			
			return;
		}
			
		while(str.startsWith("[")){
			str=str.substring(1);
		}
		while(str.endsWith("]")){
			str=str.substring(0, str.length()-1);
		}		
		//log.debug(str);
		
		
		String[]val=str.split("[, \n]+");
		
		if(val==null||val.length==0){
			log.warn("empty string!");
			return;
		}
		//log.debug("$: length: "+val.length);
		int counter=1;
		
		String from=target;
		String to="";
		
		double RTT=-1;
		int rank=-1;
		double time=-1;
		PingRecord record;
		
		for(int i=0;i<val.length;i++){
			String curString=val[i];
			if(curString.isEmpty()){
				continue;
			}
			curString=trim(curString);
			//start a new record
			//=======================================
			if(counter%4==1){
				//to
				to=curString;
			}else if(counter%4==2){
				RTT=Double.parseDouble(curString);
				
			}else if(counter%4==3){
				rank=Integer.parseInt(curString);
				
			}else{
				
				time=Integer.parseInt(curString);
				//create new record
				record=new  PingRecord(from,to,RTT, rank);
				record.time=time;
				
				if(!cachedPings.containsKey(from)){
					cachedPings.put(from,new Vector<PingRecord>(2));
				}
				cachedPings.get(from).add(record);
			}
			//=======================================
			counter++;
		}
		
	}
	
	static String trim(String str){
		String s=str;
		while(s.startsWith("[")){
			s=s.substring(1);
		}
		while(s.endsWith("]")){
			s=s.substring(0,s.length()-1);
		}
		return s;
	}
	
	/**
	 * parse the KNN results
	 * @param vec
	 */
	public static void parseKNN(String target, Vector vec){
		String str=vec.toString();
		//log.debug(str);
		if(!testValid(target,str)){			
			return;
		}
		
		while(str.startsWith("[")){
			str=str.substring(1);
		}
		while(str.endsWith("]")){
			str=str.substring(0, str.length()-1);
		}
		
		//log.debug(str);
		
		
		String[]val=str.split("[, ]+");
		
		if(val==null||val.length==0){
			log.warn("empty string!");
			return;
		}
		//log.debug("$: length: "+val.length);
		int counter=1;
		
		String from=target;
		String to="";
		
		double RTT=-1;
		double elapsedTime;
		
		KNNRecord record;
		int rank=-1;
		for(int i=0;i<val.length;i++){
			String curString=val[i];
			curString=trim(curString);
			
			//log.debug(curString);
			
			if(curString.isEmpty()){
				continue;
			}
			//start a new record
			//=======================================
			if(counter%4==1){
				//to
				to=curString;
			}else if(counter%4==2){
				RTT=Double.parseDouble(curString);
				
			}else if(counter%4==3){
				
				rank=Integer.parseInt(curString);
			}else{
				
				elapsedTime=Double.parseDouble(curString);
				
				//create new record
				record=new  KNNRecord(from,to,RTT,elapsedTime);
				record.rank=rank;
				
				if(!cachedKNNNina.containsKey(from)){
					cachedKNNNina.put(from,new Vector<KNNRecord>(2));
				}
				cachedKNNNina.get(from).add(record);
			}
			//=======================================
			counter++;
		}
		
	}
	
	/**
	 * test the validity
	 * @param str
	 * @return
	 */
	public static boolean testValid(String target, String str){
		String matches="java";
		
		if(!str.contains(matches)){
			return true;
		}else{
			log.warn("Target: \t"+target+"\nWrong String:\t"+str);
			return false;
		}
		
	}
	
	/**
	 * parse the KNN results
	 * @param vec
	 */
	public static void parseconstrainedKNN(String target, Vector vec){
		String str=vec.toString();
		
		if(!testValid(target,str)){			
			return;
		}
		log.debug(str);
	
		while(str.startsWith("[")){
			str=str.substring(1);
		}
		while(str.endsWith("]")){
			str=str.substring(0, str.length()-1);
		}
		
		//log.debug(str);
		
		
		String[]val=str.split("[, \n]+");
		
		if(val==null||val.length==0){
			log.warn("empty string!");
			return;
		}
		//log.debug("$: length: "+val.length);
		int counter=1;
		
		String from=target;
		String to="";
		
		double RTT=-1;
		double elapsedTime;
		
		KNNRecord record;
		int rank=-1;
		for(int i=0;i<val.length;i++){
			String curString=val[i];
			curString=trim(curString);
			
			//log.debug(curString);
			
			if(curString.isEmpty()){
				continue;
			}
			//start a new record
			//=======================================
			if(counter%4==1){
				//to
				to=curString;
			}else if(counter%4==2){
				RTT=Double.parseDouble(curString);
				
			}else if(counter%4==3){
				
				rank=Integer.parseInt(curString);
			}else{
				
				elapsedTime=Double.parseDouble(curString);
				
				//create new record
				record=new  KNNRecord(from,to,RTT,elapsedTime);
				record.rank=rank;
				
				if(!cachedconstrainedKNNNina.containsKey(from)){
					cachedconstrainedKNNNina.put(from,new Vector<KNNRecord>(2));
				}
				cachedconstrainedKNNNina.get(from).add(record);
			}
			//=======================================
			counter++;
		}
		
	}
	/**
	 * parse the Meridian result
	 * @param vec
	 */
	public static void parseMeridian(String target, Vector vec){
		
		String str=vec.toString();
		log.debug(str);
		if(!testValid(target,str)){			
			return;
		}
		
		while(str.startsWith("[")){
			str=str.substring(1);
		}
		while(str.endsWith("]")){
			str=str.substring(0, str.length()-1);
		}				
		String[]val=str.split("[, \n]+");
		
		if(val==null||val.length==0){
			log.warn("empty string!");
			return;
		}
		//log.debug("$: length: "+val.length);
		//log.debug(str);
		
		
		int counter=1;
		
		String from=target;
		String to="";
		
		double RTT=-1;
		double elapsedTime;
		
		KNNRecord record;
		
		for(int i=0;i<val.length;i++){
			String curString=val[i];
			
			if(curString.isEmpty()){
				continue;
			}
			curString=trim(curString);
			//start a new record
			//=======================================
			if(counter%3==1){
				//to
				to=curString;
			}else if(counter%3==2){
			
				RTT=Double.parseDouble(curString);
				
			}else{
				elapsedTime=Double.parseDouble(curString);
				
				//create new record
				record=new  KNNRecord(from,to,RTT,elapsedTime);
				
				if(!cachedMeridian.containsKey(from)){
					cachedMeridian.put(from,new Vector<KNNRecord>(2));
				}
				cachedMeridian.get(from).add(record);
			}
			//=======================================
			counter++;
		}
	}
	
	
	public static void main(String[] args){
		
		Vector v=new Vector(2);
		v.add("/192.168.3.193:55508, 0.327610629181506, 0.327610629181506, 0.8909714840504357, " +
				"/192.168.3.186:55508, 1.3596489838237993, 0.327610629181506, 0.9366591286870563," +
				" /192.168.3.190:55508, 1.43207548418545, 0.9801745513245927,0.9801745513245927," +
				" /192.168.3.180:55508, 1.6943436977550748, 0.327610629181506, 0.7396937647095309, " +
				"/192.168.3.191:55508, 0.327610629181506, 2.1756521189560303, 0.6850404881848091, " +
				"/192.168.3.185:55508, 0.327610629181506, 2.2044544243078428, 0.8189922983873402, " +
				"/192.168.3.188:55508, 0.327610629181506, 3.308598732539954, 0.5853466970231258, " +
				"/192.168.3.189:55508, 3.9361326323750583, 0.327610629181506, 0.919970824647955," +
				" /192.168.3.183:55508, 0.327610629181506, 3.9750595646410884, 0.35873180254387993");
		String s="/192.168.3.193:55508";
		
		parseNetCoord(s,-1,v);
		//log.debug(cachedNetCoord.size());
		Iterator<Entry<String, Vector<CoordRecord>>> ier = cachedNetCoord.entrySet().iterator();
		while(ier.hasNext()){
			Entry<String, Vector<CoordRecord>> t = ier.next();
			log.debug("target: "+t.getKey());
			Iterator<CoordRecord> ierV = t.getValue().iterator();
			while(ierV.hasNext()){
				log.debug(ierV.next().toString());
			}
			
		}
		
		v.clear();
		v.add("/192.168.3.193:55508, 0.327610629181506, 0.8909714840504357, /192.168.3.186:55508, 1.3596489838237993, 0.9366591286870563, /192.168.3.190:55508, 1.43207548418545, 0.9801745513245927, /192.168.3.180:55508, 1.6943436977550748, 0.7396937647095309, /192.168.3.191:55508, 2.1756521189560303, 0.6850404881848091, /192.168.3.185:55508, 2.2044544243078428, 0.8189922983873402, /192.168.3.188:55508, 3.308598732539954, 0.5853466970231258, /192.168.3.189:55508, 3.9361326323750583, 0.919970824647955, /192.168.3.183:55508, 3.9750595646410884, 0.35873180254387993, /192.168.3.187:55508, 4.061022238071298, 0.5845556372635801, /192.168.1.138:55508, 33.15545256278101, 0.24832224748781956, /192.168.1.141:55508, 33.340244763633244, 0.07284540303868013, /192.168.2.45:55508, 84.59720840429874, 0.04390603623059359, /192.168.2.46:55508, 84.65953084310355, 0.5972074395793606, /192.168.2.43:55508, 84.7584748633358, 0.22218741990743965, /192.168.2.47:55508, 85.1393754334062, 0.07411087311739166, /192.168.2.44:55508, 85.28556479217504, 0.0820329406552035,");
		
		//Meridian
		/*parseMeridian(s,v);
		Iterator<Entry<String, Vector<KNNRecord>>> iM = cachedMeridian.entrySet().iterator();
		while(iM.hasNext()){
			Entry<String, Vector<KNNRecord>> t = iM.next();
			log.debug("target: "+t.getKey());
			Iterator<KNNRecord> ierM = t.getValue().iterator();
			while(ierM.hasNext()){
				log.debug(ierM.next().toString());
			}
			
		}*/
		
		//KNN
	/*	parseKNN(s,v);
		Iterator<Entry<String, Vector<KNNRecord>>> iM = cachedKNNNina.entrySet().iterator();
		while(iM.hasNext()){
			Entry<String, Vector<KNNRecord>> t = iM.next();
			log.debug("target: "+t.getKey());
			Iterator<KNNRecord> ierM = t.getValue().iterator();
			while(ierM.hasNext()){
				log.debug(ierM.next().toString());
			}
			
		}*/
		
		
		
	}
}
