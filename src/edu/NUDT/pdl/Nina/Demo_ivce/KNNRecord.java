package edu.NUDT.pdl.Nina.Demo_ivce;

public class KNNRecord {
	String from;
	String to;
	
	double RTT;
	double elapsedTime;
	int rank;
	
	public  KNNRecord(String _from, String _to, double _RTT, double _elapsedTime){
		from=_from;
		to=_to;
		
		 RTT=_RTT;
		elapsedTime=_elapsedTime;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		
		StringBuffer buf=new StringBuffer();
		buf.append(from+"\t"+to+"\t"+RTT+"\t"+rank+"\n");
		
		return buf.toString();
	}
	
}
