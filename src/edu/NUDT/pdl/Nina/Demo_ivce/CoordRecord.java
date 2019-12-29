package edu.NUDT.pdl.Nina.Demo_ivce;

public class CoordRecord{
	String from;
	String to;
	Object coord;
	public double realRTT;
	public double coordinate_distance;
	public double RelativeError;
	public int type; //0 Nina, 1 Vivaldi
	public double elapsedTime;
	
	public CoordRecord(String _from, String _to, Object _coord, double _realRTT, double _coordinate_distance, double _RelativeError){
		from=_from;
		to=_to;
		 coord=_coord;
		 realRTT=_realRTT;
		coordinate_distance=_coordinate_distance;
		RelativeError=_RelativeError;	
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		StringBuffer buf=new StringBuffer();
		
		/*buf.append("to: "+to+"\t");
		if(coord!=null){
			buf.append("coord: "+coord.toString()+"\t");
		}		
		buf.append("realRTT: "+realRTT+"\t");
		buf.append("coordinate_distance: "+coordinate_distance+"\t");
		buf.append("Type: "+type+"\t");
		buf.append("RelativeError: "+RelativeError+"\n");*/
		String typ="";
		if(type==0){
			typ="Nina";
		}else if(type==1){
			typ="Vivaldi";
		}
		buf.append(typ+"\t");
		buf.append(from+"\t");
		buf.append(to+"\t");
		if(coord!=null){
			buf.append(coord.toString()+"\t");
		}		
		buf.append((float)realRTT+"\t");
		buf.append((float)coordinate_distance+"\t");
		buf.append((float)RelativeError+"\n");
		
		return buf.toString();
	}
	
	public CoordRecord makeCopy(){
		return new CoordRecord(this.from,this.to,this.coord,this.realRTT,this.coordinate_distance,this.RelativeError);
	}
	
	}