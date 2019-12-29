package edu.NUDT.pdl.Nina.net.appPing;

public class pair {
	private String addr;
	private double rtt;
	public pair(String _addr, double _rtt){
		setAddr(_addr);
		setRtt(_rtt);
	}
	public void setAddr(String addr) {
		this.addr = addr;
	}
	public String getAddr() {
		return addr;
	}
	public void setRtt(double rtt) {
		this.rtt = rtt;
	}
	public double getRtt() {
		return rtt;
	}
}
