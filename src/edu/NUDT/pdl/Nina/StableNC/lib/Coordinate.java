/*
 * Copyright 2008 Jonathan Ledlie and Peter Pietzuch
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.NUDT.pdl.Nina.StableNC.lib;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

import edu.harvard.syrah.prp.Log;
import edu.harvard.syrah.sbon.async.Config;

/**
 * A coordinate in the Euclidian space.
 * 
 * @author Michael Parker, Jonathan Ledlie
 */
public class Coordinate implements Serializable {
	private static final Log log = new Log(Coordinate.class);

	private static final long serialVersionUID = 1L;

	final static protected int CLASS_HASH = Coordinate.class.hashCode();

	public double[] coords;

	protected byte version;
	protected int num_dims;

	public double num_curvs = Math.abs(-15); // hyperbolic
	// private static final int Curvratures=-20;

	public boolean isHyperbolic = true;

	public  double MIN_COORD = 2;

	
	//for bias
	public double bias=0;
	
	//for bias
	public double global_mean=0;
	
	
	// ------------------------------------------
	// for init process (rtt, coordinate)
	public double currentRTT = -1;
	public double r_error = -1;
	//use height
	public boolean USE_HEIGHT= Boolean.parseBoolean(Config
			.getConfigProps().getProperty("useHeight", "false"));
	
	public boolean isSymmetricTrigger = false;
	public long age = -1;
	public Coordinate VivaldiCoord=null;
	public Coordinate HyperVivaldiCoord=null;
	public double receivedRTT;
	public double  elapsedTime;
	// ----------------------------------------

	public byte getVersion() {
		return version;
	}

	/**
	 * set the use of height
	 * @param _USE_HEIGHT
	 */
	public void setHeight(boolean _USE_HEIGHT){
		USE_HEIGHT=_USE_HEIGHT;
	}
	
	/**
	 * Creates a copy of this <code>Coordinate</code> object, such that updates
	 * in this coordinate are not reflected in the returned object.
	 * 
	 * @return a copy of these coordinates
	 */
	public Coordinate makeCopy() {
		Coordinate tmp = new Coordinate(coords, true);
		tmp.isHyperbolic = this.isHyperbolic;
		tmp.num_curvs = this.num_curvs;
		tmp.currentRTT = this.currentRTT;
		tmp.age = this.age;
		tmp.isSymmetricTrigger = this.isSymmetricTrigger;
		tmp.r_error = this.r_error;
		tmp.USE_HEIGHT=this.USE_HEIGHT;
		tmp.elapsedTime=this.elapsedTime;
		tmp.receivedRTT=this.receivedRTT;
		tmp.bias=this.bias;
		tmp.global_mean=this.global_mean;
		
		return tmp;
		
	}
	public Coordinate makeCopy(Coordinate _VivaldiCoord, Coordinate _HyperVivaldiCoord) {
		Coordinate tmp = new Coordinate(coords, true);
		tmp.isHyperbolic = this.isHyperbolic;
		tmp.num_curvs = this.num_curvs;
		tmp.currentRTT = this.currentRTT;
		tmp.age = this.age;
		tmp.isSymmetricTrigger = this.isSymmetricTrigger;
		tmp.r_error = this.r_error;
		tmp.USE_HEIGHT=this.USE_HEIGHT;
		tmp.elapsedTime=this.elapsedTime;
		tmp.receivedRTT=this.receivedRTT;
		tmp.VivaldiCoord=new Coordinate(_VivaldiCoord);
		tmp.HyperVivaldiCoord=new Coordinate(_HyperVivaldiCoord);
		
		return tmp;
		
	}
	
	

	/**
	 * Creates a new coordinate having a position at the origin.
	 * 
	 * @param num_dimensions
	 *            the number of coordinate dimensions
	 */
	public Coordinate(int num_dimensions) {
		coords = new double[num_dimensions];
		reset();
		//version = NCClient.CURRENT_VERSION;
		if (USE_HEIGHT)
			num_dimensions--;
		num_dims = num_dimensions;

		// default

		// num_curvs=Curvratures;
	}

	/**
	 * Creates a new coordinate having a position at the origin.
	 * 
	 * @param num_dimensions
	 *            the number of coordinate dimensions
	 */
	public Coordinate(int num_dimensions, int num_curatures) {
		coords = new double[num_dimensions];
		reset();
		version = NCClient.CURRENT_VERSION;
		if (USE_HEIGHT)
			num_dimensions--;
		num_dims = num_dimensions;
		num_curvs = num_curatures;
	}

	/**
	 * 
	 * @param num_dimensions
	 * @param dis
	 * @throws IOException
	 */

	public Coordinate(int num_dimensions, DataInputStream dis)
			throws IOException {
		coords = new double[num_dimensions];
		reset();
		version = dis.readByte();
		for (int i = 0; i < num_dimensions; i++) {
			coords[i] = (dis.readFloat());
		}
		if (USE_HEIGHT)
			num_dimensions--;
		num_dims = num_dimensions;

	}

	public void toSerialized(DataOutputStream dos) throws IOException {
		final int num_dims = coords.length;
		dos.writeByte(version);
		for (int i = 0; i < num_dims; ++i) {
			// when writing, cast to float
			dos.writeFloat((float) coords[i]);
		}
		// if (VivaldiClient.USE_HEIGHT) dos.writeFloat((float)
		// coords[num_dims]);
	}

	public Coordinate(Coordinate c) {
		this(c.coords, true);

	}

	/**
	 * Creates a new coordinate having a position specified by the array
	 * <code>init_coords</code>. The number of dimensions is this equal to the
	 * array length.
	 * 
	 * @param init_pos
	 *            the position for this coordinate
	 * @param make_copy
	 *            whether a copy of the array should be made
	 */
	protected Coordinate(double[] init_pos, boolean make_copy) {
		if(init_pos==null){
			
			return;
		}
		int _num_dims = init_pos.length;
		if (make_copy) {
			coords = new double[_num_dims];
			System.arraycopy(init_pos, 0, coords, 0, _num_dims);
		} else {
			coords = init_pos;
		}

		version = NCClient.CURRENT_VERSION;
		if (USE_HEIGHT)
			_num_dims--;
		num_dims = _num_dims;
	}

	/**
	 * Creates a new coordinate having a position specified by the array
	 * <code>init_coords</code>. The number of dimensions is this equal to the
	 * array length.
	 * 
	 * @param init_pos
	 *            the position for this coordinate
	 */
	public Coordinate(float[] init_pos) {
		int _num_dims = init_pos.length;
		coords = new double[_num_dims];
		for (int i = 0; i < _num_dims; ++i) {
			coords[i] = init_pos[i];
		}
		version = NCClient.CURRENT_VERSION;
		if (USE_HEIGHT)
			_num_dims--;
		num_dims = _num_dims;
	}

	public boolean isCompatible(Coordinate _other) {
		if ((this.isHyperbolic && !_other.isHyperbolic) || !this.isHyperbolic
				&& _other.isHyperbolic) {
			return false;
		}
		if (_other == null)
			return false;
		if (version == _other.version)
			return true;
		return false;
	}

	/**
	 * Move a small distance along each dimension from where we are currently.
	 */

	public void bump() {
		for (int i = 0; i < coords.length; ++i) {
			double length = NCClient.random.nextDouble() + MIN_COORD;
			// don't set height to be negative, if we are using it
			if ((!USE_HEIGHT || i < coords.length - 1)
					&& (NCClient.random.nextBoolean())) {
				length *= -1.;
			}
			coords[i] += length;
		}
	}

	/**
	 * Returns the number of dimensions this coordinate has.
	 * 
	 * @return the number of coordinate dimensions.
	 */
	public int getNumDimensions() {
		return coords.length;
	}

	/**
	 * Returns the Euclidian distance to the given coordinate parameter.
	 * 
	 * @param c
	 *            the coordinate to find the Euclidian distance to
	 * @return the distance to parameter <code>c</code>
	 */
	public double distanceToNonOriginCoord(Coordinate c) {
		if (atOrigin() || c == null || c.atOrigin())
			return Double.NaN;
		return distanceTo(c);
	}

	/**
	 * Note that distanceTo, only returns the coordinate distance for
	 * Hyperbolic, it has to multiple the curvature
	 * 
	 * @param c
	 * @return
	 */
	public double distanceTo(Coordinate c) {

		// first test if is hyperbolic

		if (isHyperbolic) {
			double sum1 = 0.0;
			double sum2 = 0.0;
			double sum3 = 0.0;
			double product = 0.0;
			for (int i = 0; i < num_dims; ++i) {
				sum1 += Math.pow(coords[i], 2);
				sum2 += Math.pow(c.coords[i], 2);
				product += c.coords[i] * coords[i];
			}

			// icdcs08 wrong! sum3=Math.sqrt((sum1+1)*(sum2+1))-product;
			// IS:
			sum3 = Math.sqrt(1 + sum1) * Math.sqrt(1 + sum2) - product;
			// move a little, the derivative will be infinite
			if (sum3 == 1) {
				sum3 += 0.1;
			}
			// double x=Math.cosh(sum3);
			double x = sum3;
			// System.out.println("x: "+x
			// +" VS 1"+(x-1)+"sum1: "+sum1+", sum2: "+sum2+", product: "+product);
			double arccosh = Math.log(x + Math.sqrt(Math.pow(x, 2) - 1));
			// System.out.println("arcosh: "+arccosh);
			double sum = arccosh;
			if (USE_HEIGHT && sum > 0) {
				sum = sum + coords[coords.length - 1]
						+ c.coords[coords.length - 1];
			}
			// System.out.println("arcosh: "+sum*Math.abs(this.num_curvs));
			if (Double.isNaN(sum * Math.abs(this.num_curvs))) {
				System.err.println("d(x,y): is NaN");
				// System.exit(-1);
			}

			return sum;

		}

		// used for debugging so we can call distanceTo on something null
		//assert ((USE_HEIGHT && num_dims == coords.length - 1) || (!USE_HEIGHT && num_dims == coords.length));

		if (c == null) {
			// assert (!NCClient.SIMULATION);
			return -1.;
		}
		if(coords==null){
			log.warn("coords is null");
			return -1.;
		}else if(c.coords==null){
			log.warn("c.coords is null");
			return -1.;
		}else if (coords.length!=c.coords.length){
			log.warn("inequality coordDim "+coords.length+", c.dim "+c.coords.length+", real dim "+num_dims);
			return -1.;
		}
		
		double sum = 0.0;
		for (int i = 0; i < num_dims; ++i) {
			final double abs_dist = coords[i] - c.coords[i];
			sum += (abs_dist * abs_dist);
		}
		sum = Math.sqrt(sum);
		if (USE_HEIGHT && sum > 0) {
			sum = sum + coords[coords.length - 1] + c.coords[coords.length - 1];
		}
		return sum;
	}

	public double distanceTo1(Coordinate c) {

		// used for debugging so we can call distanceTo on something null
		assert ((USE_HEIGHT && num_dims == coords.length - 1) || (!USE_HEIGHT && num_dims == coords.length));

		if (c == null) {
			// assert (!NCClient.SIMULATION);
			return -1.;
		}
		double sum = 0.0;
		for (int i = 0; i < num_dims; ++i) {
			final double abs_dist = coords[i] - c.coords[i];
			sum += (abs_dist * abs_dist);
		}
		sum = Math.sqrt(sum);
		if (USE_HEIGHT && sum > 0) {
			sum = sum + coords[coords.length - 1] + c.coords[coords.length - 1];
		}
		return sum;
	}

	/*
	 * We choose the hyperboloid model, in which all points lie on the upper
	 * sheet of a hyperboloid, due to the simplicity of its metric.
	 */

	// The distance between points x = (x1 , x2 , . . . , xn ) and y = (y1 , y2
	// , . . . , yn ) in a n-dimensional
	// Hyperbolic space of curvature k
	// d(x,y)=arccosh(sqrt((1+sum_{i=1}^{n}x_i^2)(1+sum_{i=1}^{n}y_i^2))-sum_{i=1}^{n}x_iy_i)*k
	// x: arccosh: y:cosh, x=ln(y+ sqrt(y^2-1))
	public double distanceToHyper(Coordinate c) {
		if (isHyperbolic) {
			double sum1 = 0.0;
			double sum2 = 0.0;
			double sum3 = 0.0;
			double product = 0.0;
			for (int i = 0; i < num_dims; ++i) {
				sum1 += Math.pow(coords[i], 2);
				sum2 += Math.pow(c.coords[i], 2);
				product += c.coords[i] * coords[i];
			}
			sum3 = Math.sqrt((sum1 + 1) * (sum2 + 1)) - product;
			double x = Math.cosh(sum3);
			double arccosh = Math.log(x + Math.sqrt(Math.pow(x, 2) - 1));
			return arccosh * this.num_curvs;

		}
		return distanceTo(c);
	}

	// direction

	// u(sqrt(1+sum_{i=1}^{n}y_i)/sqrt(1+sum_{i=1}^{n}x_i)x-y)

	public Vec GetDirectionHyper(Coordinate c) {
		if (isHyperbolic) {
			double sum1 = 0.0;
			double sum2 = 0.0;
			double sum3 = 0.0;
			double product = 0.0;
			for (int i = 0; i < num_dims; ++i) {
				sum1 += coords[i];
				sum2 += c.coords[i];

			}
			double sca = Math.sqrt(1 + sum2) / Math.sqrt(1 + sum1);
			Coordinate tmp = this.makeCopy();
			for (int i = 0; i < num_dims; ++i) {
				tmp.coords[i] = tmp.coords[i] * sca - c.coords[i];
			}
			return getDirection(tmp);
		}
		return null;
	}

	// magnitude

	// (rtt_xy-d(x,y))/sqrt(cosh^2 d(x,y)-1)

	public double GetMagnitudeHyper(long rtt, Coordinate c) {
		if (isHyperbolic) {

			double dxy = this.distanceTo(c);

		}
		return -1;
	}

	// Same regardless of using height
	public void add(Vec v) {
		final int num_dims = coords.length;
		for (int i = 0; i < num_dims; ++i) {
			coords[i] += v.direction[i];
		}
	}

	/*
	 * protected method, do not expose to client
	 */
	protected Vec getDirection(Coordinate c) {

		// System.out.println("$: Old: "+this.toString()+", B: "+c.toString());
		double distance_ab_H = distanceTo(c);
		if (isHyperbolic) {
			double sum1 = 0.0;
			double sum2 = 0.0;
			double sum3 = 0.0;
			double product = 0.0;
			for (int i = 0; i < num_dims; ++i) {
				// Note. ^2
				sum1 += Math.pow(coords[i], 2);
				sum2 += Math.pow(c.coords[i], 2);

			}
			// System.out.println("$: Sum-"+sum1+", and "+sum2);
			double sca = Math.sqrt(1 + sum2) / Math.sqrt(1 + sum1);

			Coordinate tmp = makeCopy();

			for (int i = 0; i < num_dims; ++i) {
				tmp.coords[i] = (tmp.coords[i] * sca);
				// System.out.println("sinh: "+Math.sinh(distanceTo(c)/this.num_curvs));
				// tmp.coords[i]=(tmp.coords[i]*sca)/Math.sinh(distanceTo(c)/this.num_curvs);
				// tmp.coords[i]=tmp.coords[i]*sca-c.coords[i];
			}
			// TODO: direction of x' to c
			// System.out.println("$: A: "+tmp.toString()+", B: "+tmp2.toString());
			if (USE_HEIGHT) {
				// reset height
				tmp.coords[num_dims - 1] = this.coords[num_dims - 1];
				// tmp2.coords[num_dims-1]=c.coords[num_dims-1];
			}
			// icdcs08 wrong!
			// coordinate direction
			Vec dir = new Vec(tmp.coords, true, this.isHyperbolic);
			for (int i = 0; i < num_dims; ++i) {
				dir.direction[i] = (c.coords[i] - dir.direction[i]);
			}
			// multiply k and sinh d_ij^S
			// 9-16: remove the scale item:
			dir.scale(this.num_curvs * (1 / Math.sinh(distance_ab_H)));
			// then normalized, unit the coordinate
			double length = dir.makeUnit(this.num_curvs);
			if (USE_HEIGHT) {
				dir.direction[coords.length - 1] = (c.coords[coords.length - 1] + coords[coords.length - 1])
						/ length;
			}

			return dir;
		}

		return getDirection1(c);
	}

	/*
	 * protected method, do not expose to client
	 */
	protected Vec getDirection1(Coordinate c) {
		double length = distanceTo(c);
		if (length == 0)
			return null;
		final Vec new_vec = new Vec(coords.length, this.isHyperbolic);
		for (int i = 0; i < num_dims; ++i) {
			new_vec.direction[i] = (c.coords[i] - coords[i]) / length;
		}
		if (USE_HEIGHT) {
			new_vec.direction[coords.length - 1] = (c.coords[coords.length - 1] + coords[coords.length - 1])
					/ length;
		}
		return new_vec;
	}

	protected boolean assign(Coordinate c) {
		if (coords.length != c.coords.length)
			return false;
		for (int i = 0; i < coords.length; ++i) {
			coords[i] = c.coords[i];
		}
		return true;
	}

	public void checkHeight() {
		if (!USE_HEIGHT)
			return;
		if (coords[coords.length - 1] <= MIN_COORD) {
			coords[coords.length - 1] = NCClient.random.nextDouble()
					+ MIN_COORD;
		}
	}

	public boolean atOrigin() {
		
		for (int i = 0; i < coords.length; i++) {
			//System.out.print(coords[i]+", ");
			if (coords[i] != 0)
				return false;
		}
		//System.out.print("\n");
		return true;
	}

	public Vec asVectorFromZero(boolean make_copy) {
		Vec tmp = new Vec(coords, make_copy, this.isHyperbolic);

		return tmp;
	}

	public boolean isValid() {
		final double NEG_MAX_DIST_FROM_ORIGIN = -1
				* NCClient.MAX_DIST_FROM_ORIGIN;
		for (int i = 0; i < coords.length; ++i) {
			
			if (Double.isNaN(coords[i])||Double.isInfinite(coords[i])) {
				
					System.err.println("coord is: " + coords[i]);
				return false;
			}
			/*
			 * if (coords[i] > NCClient.MAX_DIST_FROM_ORIGIN || coords[i] <
			 * NEG_MAX_DIST_FROM_ORIGIN) { if (NCClient.SIM)
			 * System.err.println("coord too far from origin i=" + i + " coord="
			 * + coords[i]); return false; }
			 */
		}
		return true;
	}

	public void reset() {
		for (int i = 0; i < coords.length; ++i) {
			coords[i] = 0;
		}
		currentRTT = 1;
		r_error = 1;
		//bump();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Coordinate) {
			Coordinate c = (Coordinate) obj;
			final int num_dims = coords.length;
			for (int i = 0; i < num_dims; ++i) {
				if (coords[i] != c.coords[i]) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		final int num_dims = coords.length;
		int hc = CLASS_HASH;
		for (int i = 0; i < num_dims; ++i) {
			hc ^= new Double(coords[i]).hashCode();
		}
		return hc;
	}

	@Override
	public String toString() {
		final StringBuffer sbuf = new StringBuffer(1024);
		sbuf.append("[");

		final int num_dims = coords.length;
		for (int i = 0; true;) {
			if (i == num_dims - 1 && USE_HEIGHT) {
				sbuf.append('h');
			}
			//sbuf.append(NCClient.nf.format(coords[i]));
			sbuf.append(coords[i]);
			if (++i < num_dims) {
				sbuf.append(",");
			} else {
				break;
			}
		}
		sbuf.append("]");
		return sbuf.toString();
	}

	public String toStringAsVector() {
		final StringBuffer sbuf = new StringBuffer(1024);

		for (int i = 0; i < coords.length; i++) {
			if (i == coords.length - 1 && USE_HEIGHT)
				sbuf.append('h');
			sbuf.append(NCClient.nf.format(coords[i]));
			if (i != coords.length - 1)
				sbuf.append(" ");
		}
		return sbuf.toString();
	}

	public double innerProduct(Coordinate _r_coord) {
		// TODO Auto-generated method stub
		double sum=0.;
		
		for(int i=0;i<this.num_dims;i++){
			sum+=this.coords[i]*_r_coord.coords[i];			
		}
		return sum;
	}

	public double distanceToMatrixNorm(Coordinate systemCoords) {
		// TODO Auto-generated method stub
		return this.innerProduct(systemCoords)+this.bias+systemCoords.bias+(this.global_mean+systemCoords.global_mean)/2.0;
	}

	public void clear() {
		// TODO Auto-generated method stub
		this.coords=null;
	}

}
