/*
 * Pyxida - a network coordinate library
 * 
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
package edu.NUDT.pdl.Nina.Sim.SimCoord;

import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

// this is unbelievable ugly

public class Statistic<T extends Number> {

	public final Vector<T> samples;
	int dataType;
	static final int INT_TYPE = 0;
	static final int FLOAT_TYPE = 1;
	static final int DOUBLE_TYPE = 2;

	boolean typeHasBeenSet = false;

	public Statistic() {
		samples = new Vector<T>();
	}

	public void clear() {
		samples.clear();
	}

	public void add(T sample) {
		if (!typeHasBeenSet)
			setType(sample);
		samples.add(sample);
	}

	void setType(T sample) {
		typeHasBeenSet = true;
		if (sample instanceof Integer) {
			dataType = INT_TYPE;
		} else if (sample instanceof Double) {
			dataType = DOUBLE_TYPE;
		} else if (sample instanceof Float) {
			dataType = FLOAT_TYPE;
		} else {
			System.err.println("Statistic must use Integer, Double, or Float");
		}
	}

	public int getSize() {
		return samples.size();
	}

	public T getPercentile(double p) {
		if (samples.size() == 0)
			return null;
		Collections.sort(samples, new Comparator<T>() {

			public int compare(T o1, T o2) {
				switch (dataType) {
				case INT_TYPE:
					return ((Integer) o1).compareTo((Integer) o2);
				case DOUBLE_TYPE:
					return ((Double) o1).compareTo((Double) o2);
				case FLOAT_TYPE:
					return ((Float) o1).compareTo((Float) o2);
				}
				return 0;
			}

		});

		int percentile = (int) (samples.size() * p);
		T val = samples.get(percentile);
		return val;
	}

	public T getSum() {
		if (samples.size() == 0)
			return null;
		switch (dataType) {
		case INT_TYPE:
			int sampleSum = 0;
			for (int i = 0; i < samples.size(); i++) {
				sampleSum = samples.get(i).intValue() + sampleSum;
			}
			return (T) new Integer(sampleSum);
		case DOUBLE_TYPE:
			double sampleSumD = 0;
			for (int i = 0; i < samples.size(); i++) {
				sampleSumD = samples.get(i).doubleValue() + sampleSumD;
			}
			return (T) new Double(sampleSumD);
		case FLOAT_TYPE:
			float sampleSumF = 0;
			for (int i = 0; i < samples.size(); i++) {
				sampleSumF = samples.get(i).floatValue() + sampleSumF;
			}
			return (T) new Float(sampleSumF);
		}
		return null;
	}

	/*
	 * public T getMean () { return }
	 */

}
