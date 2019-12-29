/**
 * Ono Project
 *
 * File:         Util.java
 * RCS:          $Id: Util.java,v 1.11 2008/03/18 15:23:36 drc915 Exp $
 * Description:  Util class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Aug 28, 2006 at 11:21:35 AM
 * Language:     Java
 * Package:      edu.northwestern.ono.util
 * Status:       Experimental (Do Not Distribute)
 *
 * (C) Copyright 2006, Northwestern University, all rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */
package edu.NUDT.pdl.Nina.util;



import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.Calendar;
import java.util.HashMap;
import java.util.TimeZone;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The Util class provides utility functions for the package.
 */
public class Util {
    
	static BufferedReader in;

	private static Util self;
    
    
    public static interface PingResponse {
    	public void response(double rtt);
    }

    
    public static long currentGMTTime(){
    	return System.currentTimeMillis()-TimeZone.getDefault().getRawOffset();
    } 
    
    public synchronized static Util getInstance(){

	    	if (self == null){
	    		self = new Util();
	    	}
    	
    	return self;
    }

   

    /**
     * @param newEdge
     * @return
     */
    public static String getClassCSubnet(String ipaddress) {
        // TODO Auto-generated method stub
        return ipaddress.substring(0, ipaddress.lastIndexOf("."));
    }

	public static byte[] convertLong(long l) {
		byte[] bArray = new byte[8];
        ByteBuffer bBuffer = ByteBuffer.wrap(bArray);
        bBuffer.order(ByteOrder.LITTLE_ENDIAN);
        LongBuffer lBuffer = bBuffer.asLongBuffer();
        lBuffer.put(0, l);
        return bArray;
	}
	
	public static  byte[] convertShort(short s) {
		byte[] bArray = new byte[2];
        ByteBuffer bBuffer = ByteBuffer.wrap(bArray);
        bBuffer.order(ByteOrder.LITTLE_ENDIAN);
        ShortBuffer sBuffer = bBuffer.asShortBuffer();
        sBuffer.put(0, s);
        return bArray;
	}
	
	public static  byte[] convertInt(int s) {
		byte[] bArray = new byte[4];
        ByteBuffer bBuffer = ByteBuffer.wrap(bArray);
        bBuffer.order(ByteOrder.LITTLE_ENDIAN);
        IntBuffer sBuffer = bBuffer.asIntBuffer();
        sBuffer.put(0, s);
        return bArray;
	}
	
	public static  byte[] convertFloat(float s) {
		byte[] bArray = new byte[4];
        ByteBuffer bBuffer = ByteBuffer.wrap(bArray);
        bBuffer.order(ByteOrder.LITTLE_ENDIAN);
        FloatBuffer sBuffer = bBuffer.asFloatBuffer();
        sBuffer.put(0, s);
        return bArray;
	}
	
	public static long byteToLong(byte data[]){
		ByteBuffer bBuffer = ByteBuffer.wrap(data);
		bBuffer.order(ByteOrder.LITTLE_ENDIAN);
		LongBuffer  lBuffer = bBuffer.asLongBuffer();
		return lBuffer.get();
	}

	public static byte[] convertStringToBytes(String key) {
		try {
			return key.getBytes("ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public static String convertByteToString(byte[] value){
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			baos.write(value);
		
			return baos.toString("ISO-8859-1");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
}
