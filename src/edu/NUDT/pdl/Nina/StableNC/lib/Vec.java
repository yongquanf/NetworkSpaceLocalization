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

import edu.harvard.syrah.sbon.async.Config;

/*
 * A vector in the Euclidian space.
 */

public class Vec {

	final protected static int CLASS_HASH = Vec.class.hashCode();

	final protected double[] direction;

	final protected int num_dims;

	// set the Hyperbolic
	public final boolean isHyperbolic;

	
	boolean USE_HEIGHT=Boolean.parseBoolean(Config
			.getConfigProps().getProperty("useHeight", "false"));
	/*
	 * 
	 * public static Vec add(Vec lhs, Vec rhs) {
	 * 
	 * Vec sum = new Vec(lhs);
	 * 
	 * sum.add(rhs);
	 * 
	 * return sum;
	 * 
	 * }
	 * 
	 * 
	 * 
	 * public static Vec subtract(Vec lhs, Vec rhs) {
	 * 
	 * Vec diff = new Vec(lhs);
	 * 
	 * diff.subtract(rhs);
	 * 
	 * return diff;
	 * 
	 * }
	 */

	public static Vec scale(Vec lhs, double k) {

		Vec scaled = new Vec(lhs, lhs.isHyperbolic);

		scaled.scale(k);

		return scaled;

	}

	/*
	 * 
	 * public static Vec makeUnit(Vec v) {
	 * 
	 * Vec unit = new Vec(v);
	 * 
	 * unit.makeUnit();
	 * 
	 * return unit;
	 * 
	 * }
	 */

	/*
	 * 
	 * public static Vec makeRandomUnit(int num_dims) {
	 * 
	 * final Vec v = makeRandom (num_dims, 1.);
	 * 
	 * v.makeUnit();
	 * 
	 * return v;
	 * 
	 * }
	 */

	public Vec makeRandom(int num_dims, double axisLength,
			boolean isHyperbolic) {

		final Vec v = new Vec(num_dims, isHyperbolic);

		for (int i = 0; i < num_dims; ++i) {

			double length = NCClient.random.nextDouble() * axisLength;

			if ((!USE_HEIGHT || i < num_dims - 1) &&

			(NCClient.random.nextBoolean())) {

				length *= -1.;

			}

			v.direction[i] = length;

		}

		return v;

	}

	public Vec(int _num_dims, boolean isHyperbolic) {

		direction = new double[_num_dims];
		for (int i = 0; i < _num_dims; i++) {
			direction[i] = 0;
		}
		if (USE_HEIGHT)
			_num_dims--;

		num_dims = _num_dims;
		this.isHyperbolic = isHyperbolic;

	}

	public Vec(Vec v, boolean isHyperbolic) {

		this(v.direction, true, isHyperbolic);

	}

	public Vec(double[] init_dir, boolean make_copy, boolean isHyperbolic) {

		if (make_copy) {

			final int num_dims = init_dir.length;

			direction = new double[num_dims];

			System.arraycopy(init_dir, 0, direction, 0, num_dims);

		}

		else {

			direction = init_dir;

		}

		int _num_dims = init_dir.length;

		if (USE_HEIGHT)
			_num_dims--;

		num_dims = _num_dims;

		this.isHyperbolic = isHyperbolic;
	}

	public int getNumDimensions() {

		// keep num_dimensions internal
		return direction.length;

	}

	public double[] getComponents() {

		final double[] dir_copy = new double[direction.length];

		System.arraycopy(direction, 0, dir_copy, 0, direction.length);

		return dir_copy;

	}

	
	public Vec sign(){
		Vec tmp=new Vec(this,false);
		for(int i=0;i<tmp.num_dims;i++){
		if(this.direction[i]>0){
			tmp.direction[i]=1;
		}else if(this.direction[i]<0){
			tmp.direction[i]=-1;
		}else{
			tmp.direction[i]=-1;
		}			
		}
		return tmp;
				
	}
	
	
	// Same regardless of using height
	public void add(Vec v) {

		for (int i = 0; i < direction.length; ++i) {

			direction[i] += v.direction[i];

		}

	}

	// only done with gravity, ignores height
	public void subtract(Vec v) {

		for (int i = 0; i < direction.length; ++i) {

			direction[i] -= v.direction[i];

		}

	}

	public double plane_length_hyp() {
		double l = 0;
		for (int i = 0; i < this.num_dims; i++) {
			l += direction[i] * direction[i];
		}
		l = Math.sqrt(l);
		return l;
	}

	/*
	 * 
	 * public void subtract(Vec v) {
	 * 
	 * for (int i = 0; i < num_dims; ++i) {
	 * 
	 * direction[i] -= v.direction[i];
	 * 
	 * }
	 * 
	 * if (USE_HEIGHT) {
	 * 
	 * direction[direction.length-1] += v.direction[direction.length-1];
	 * 
	 * }
	 * 
	 * }
	 */

	public void scale(double k) {

		for (int i = 0; i < direction.length; ++i) {

			direction[i] *= k;

		}

	}

	public boolean isUnit(double Hyperbolic_curv) {

		return (getLength(Hyperbolic_curv) == 1.0);

	}

	//get Euclidean length
	public double getLength() {

	    double sum = getPlanarLength1();

	    if (USE_HEIGHT)

	      sum += direction[direction.length-1];

	    return sum;

	  }



	  double getPlanarLength1() {

	    double sum = 0;

	    for (int i = 0; i < num_dims; ++i) {

	      sum += (direction[i] * direction[i]);

	    }

	    return Math.sqrt(sum);

	  }

		public void makeUnit() {

			final double length = getLength();

			if (length != 1.0) {

	      scale (1./length);

	    }

		}

	
	public double getLength(double Hyperbolic_curv) {

		double sum = 0;
		if (!isHyperbolic) {
			sum = getPlanarLength();
		} else if (isHyperbolic) {
			sum = getHyperbolicLength(Hyperbolic_curv);
		}

		if (USE_HEIGHT)

			sum += direction[direction.length - 1];

		return sum;
	}

	public double getHyperbolicLength(double num_curvs) {
		// Hyerpbolic ditance to the origin

		double sum = 0;

		double sum1 = 0.0;
		double sum2 = 0.0;
		double sum3 = 0.0;
		double product = 0.0;
		for (int i = 0; i < num_dims; ++i) {
			sum1 += Math.pow(direction[i], 2);
		}

		// icdcs08 wrong! sum3=Math.sqrt((sum1+1)*(sum2+1))-product;
		// IS:
		sum3 = Math.sqrt(1 + sum1) * Math.sqrt(1 + sum2) - product;

		// double x=Math.cosh(sum3);
		double x = sum3;
		// System.out.println("x: "+x
		// +" VS 1"+(x-1)+"sum1: "+sum1+", sum2: "+sum2+", product: "+product);

		// normalized with the curvs can increase the accuracy
		// 
		double arccosh = Math.log(x + Math.sqrt(Math.pow(x, 2) - 1))
				* num_curvs;

		// low accuracy

		// double arccosh=Math.log(x+Math.sqrt(Math.pow(x, 2)-1));

		// System.out.println("arcosh: "+arccosh);
		sum = arccosh;

		return sum;

	}

	double getPlanarLength() {

		double sum = 0;

		for (int i = 0; i < num_dims; ++i) {

			sum += (direction[i] * direction[i]);

		}

		return Math.sqrt(sum);

	}

	public double makeUnit(double Hyperbolic_curv) {

		final double length = getLength(Hyperbolic_curv);

		if (length != 1.0) {

			scale(1. / length);

		}
		return length;
	}

	public Coordinate asCoordinateFromZero(boolean make_copy) {

		// Hyperbolic space registered
		Coordinate tmp = new Coordinate(direction, make_copy);
		tmp.isHyperbolic = this.isHyperbolic;

		return tmp;

	}

	@Override
	public boolean equals(Object obj) {

		if (obj instanceof Vec) {

			Vec v = (Vec) obj;

			final int num_dims = direction.length;

			for (int i = 0; i < num_dims; ++i) {

				if (direction[i] != v.direction[i]) {

					return false;

				}

			}

			return true;

		}

		return false;

	}

	@Override
	public int hashCode() {

		final int num_dims = direction.length;

		int hc = CLASS_HASH;

		for (int i = 0; i < num_dims; ++i) {

			hc ^= new Double(direction[i]).hashCode();

		}

		return hc;

	}

	@Override
	public String toString() {

		final StringBuffer sbuf = new StringBuffer(1024);

		sbuf.append("[");

		final int num_dims = direction.length;

		for (int i = 0; true;) {

			if (i == num_dims - 1 && USE_HEIGHT) {

				sbuf.append('h');

			}

			sbuf.append(NCClient.nf.format(direction[i]));

			if (++i < num_dims) {

				sbuf.append(",");

			}

			else {

				break;

			}

		}

		sbuf.append("]");
		sbuf.append(", Hyperbolic=" + this.isHyperbolic);

		return sbuf.toString();

	}

	public void clear() {
		// TODO Auto-generated method stub
		
	}

}
