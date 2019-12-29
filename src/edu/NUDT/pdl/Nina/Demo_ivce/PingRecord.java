package edu.NUDT.pdl.Nina.Demo_ivce;

public class PingRecord {

	String from;
	String to;
	double RTT;
	int Rank; //starts from 1, the closest
	double time;
	
	public PingRecord(String _from, String _to, double _RTT, int _Rank){
		from=_from;
		to=_to;
		RTT=_RTT;
		Rank=_Rank;
	}
	public String toString() {
		// TODO Auto-generated method stub
		StringBuffer buf=new StringBuffer();
		
		buf.append(from+"\t"+to+"\t"+RTT+"\t"+Rank+"\n");
		
		return buf.toString();
	}
	
	
}
