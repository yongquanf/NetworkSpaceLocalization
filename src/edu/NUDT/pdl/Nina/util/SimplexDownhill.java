package edu.NUDT.pdl.Nina.util;

import java.util.Vector;

import edu.NUDT.pdl.Nina.StableNC.lib.Coordinate;
import edu.NUDT.pdl.Nina.StableNC.lib.GNP;

public class SimplexDownhill {
	/**
	 * 
	 note: all effective array indices are 1..N, not 0..N-1 simplex - the
	 * initial simplex, must contain d + 1 points values - the initial function
	 * values at the simplex points d - the number of dimensions--dims* ftol -
	 * the tolerate for convergence obj - the objective function to minimize
	 * num_eval - the number of evaluations of obj stuff - things that are
	 * needed by obj
	 * 
	 * @param simplex
	 * @param values
	 * @param d
	 * @param ftol
	 * @param fit
	 * @param num_eval
	 *            current evaluations, one element
	 * @param stuff
	 * @return
	 */
	public static void simplex_downhill(float[][] simplex, float[] values,
			int d, float ftol, CoordFitFunction fit, int[] num_eval,
			float[] stuff, boolean isCoord) {

		int i, j, low, high, second_high, ssize;
		float rtol, sum, test_value, mult;
		float[] simplex_sum;
		float[] test_point; // we don't use the 0th element
		int NMAX = 50000;

		/* initializations */
		num_eval[0] = 0;
		ssize = d + 1; /* size of the simplex */
		/* for each dimension, pre-compute the sum of all points */
		/* this is used later to compute the average coordinates */
		simplex_sum = new float[d + 1]; /* array starts at 1 */
		for (i = 1; i <= d; i++) {
			sum = 0.0f;
			for (j = 1; j <= ssize; j++) {
				sum = sum + simplex[j][i];
			}
			simplex_sum[i] = sum;
		}
		/* test point */
		test_point = new float[d + 1];

		/* begin algorithm */
		while (true) {

			/*
			 * find the lowest point, the highest point and the second highest
			 * point in the simplex
			 */
			if (values[1] > values[2]) {
				low = 2;
				high = 1;
				second_high = 2;
			} else {
				low = 1;
				high = 2;
				second_high = 1;
			}
			for (i = 1; i <= ssize; i++) {
				if (values[i] > values[high]) {
					second_high = high;
					high = i;
				} else if (values[i] > values[second_high] && i != high) {
					second_high = i;
				} else if (values[i] <= values[low]) {
					low = i;
				}
			}

			/*
			 * we will quit if there are too many tries the tolerance is met, or
			 * if the function value is so low that we really don't care anymore
			 */
			rtol = 2.0f
					* Math.abs(values[high] - values[low])
					/ (Math.abs(values[high]) + Math.abs(values[low]) + Float.MIN_VALUE);
			if (num_eval[0] >= NMAX || rtol < ftol || values[low] < 1.0e-6) {
				values[1] = values[low];
				for (i = 1; i <= d; i++) {
					simplex[1][i] = simplex[low][i];
				}
				break;
			}
			/* first try to reflect the high point across the mean point */
			/* i.e. want (x_mean - x_high) = - (x_mean - x_test) */
			mult = 2.0f / d;
			for (i = 1; i <= d; i++) {
				test_point[i] = (simplex_sum[i] - simplex[high][i]) * mult
						- simplex[high][i];
			}
			// TODO: fit implementation based on coordinate
			if (!isCoord) {
				test_value = fit.fitFunction(test_point, d, stuff);
			} else {
				Vector<Coordinate> rawCoordinates = new Vector<Coordinate>(10);
				int sizeofNodes = d / GNP.dim;
				GNP.translate(test_point, rawCoordinates, sizeofNodes);
				Vector helper = null;
				if (stuff != null && stuff.length > 0) {
					helper = new Vector(10);
					for (int ii = 0; ii < stuff.length; ii++) {
						helper.add(stuff[ii]);
					}
				}
				test_value = (float) fit.fitFunction(rawCoordinates, d, helper);
			}
			num_eval[0]++;
			if (test_value < values[high]) {
				/* better point, update the simplex, update also the sum */
				values[high] = test_value;
				for (i = 1; i <= d; i++) {
					simplex_sum[i] = simplex_sum[i] - simplex[high][i]
							+ test_point[i];
					simplex[high][i] = test_point[i];
				}
			}

			if (test_value <= values[low]) {
				/*
				 * the new point is even better than our lowest point, okay,
				 * now, extend the point we just found even further i.e. want
				 * 2(x_mean - x_high) = (x_mean - x_test)
				 */
				mult = -1.0f / d;
				for (i = 1; i <= d; i++) {
					test_point[i] = (float) ((simplex_sum[i] - simplex[high][i])
							* mult + 2.0 * simplex[high][i]);
				}
				if (!isCoord) {
					test_value = fit.fitFunction(test_point, d, stuff);
				} else {
					Vector<Coordinate> rawCoordinates = new Vector<Coordinate>(
							10);
					int sizeofNodes = d / GNP.dim;
					GNP.translate(test_point, rawCoordinates, sizeofNodes);
					Vector helper = null;
					if (stuff != null && stuff.length > 0) {
						helper = new Vector(10);
						for (int ii = 0; ii < stuff.length; ii++) {
							helper.add(stuff[ii]);
						}
					}
					test_value = (float) fit.fitFunction(rawCoordinates, d,
							helper);
				}
				// test_value = fit.fitFunction(test_point, d, stuff);
				num_eval[0]++;
				if (test_value < values[high]) {
					/* better point, update the simplex, update also the sum */
					values[high] = test_value;
					for (i = 1; i <= d; i++) {
						simplex_sum[i] = simplex_sum[i] - simplex[high][i]
								+ test_point[i];
						simplex[high][i] = test_point[i];
					}
				}

			} else if (test_value >= values[second_high]) {
				/*
				 * the new point is still the highest, no improvement, so we are
				 * going to shrink the high point towards the mean i.e. want
				 * (x_mean - x_high) = 2(x_mean - x_test)
				 */
				mult = 0.5f / d;
				for (i = 1; i <= d; i++) {
					test_point[i] = (simplex_sum[i] - simplex[high][i]) * mult
							+ 0.5f * simplex[high][i];
				}
				if (!isCoord) {
					test_value = fit.fitFunction(test_point, d, stuff);
				} else {
					Vector<Coordinate> rawCoordinates = new Vector<Coordinate>(
							10);
					int sizeofNodes = d / GNP.dim;
					GNP.translate(test_point, rawCoordinates, sizeofNodes);
					Vector helper = null;
					if (stuff != null && stuff.length > 0) {
						helper = new Vector(10);
						for (int ii = 0; ii < stuff.length; ii++) {
							helper.add(stuff[ii]);
						}
					}
					test_value = (float) fit.fitFunction(rawCoordinates, d,
							helper);
				}
				// test_value = fit.fitFunction(test_point, d, stuff);
				num_eval[0]++;
				if (test_value < values[high]) {
					/* better point, update the simplex, update also the sum */
					values[high] = test_value;
					for (i = 1; i <= d; i++) {
						simplex_sum[i] = simplex_sum[i] - simplex[high][i]
								+ test_point[i];
						simplex[high][i] = test_point[i];
					}
				} else {
					/*
					 * no good, we better just contract the whole simplex toward
					 * the low point
					 */
					for (i = 1; i <= ssize; i++) {
						if (i != low) {
							for (j = 1; j <= d; j++) {
								simplex[i][j] = test_point[j] = 0.5f * (simplex[i][j] + simplex[low][j]);
							}
							if (!isCoord) {
								values[i] = fit.fitFunction(test_point, d,
										stuff);
							} else {
								Vector<Coordinate> rawCoordinates = new Vector<Coordinate>(
										10);
								int sizeofNodes = d / GNP.dim;
								GNP.translate(test_point, rawCoordinates,
										sizeofNodes);
								Vector helper = null;
								if (stuff != null && stuff.length > 0) {
									helper = new Vector(10);
									for (int ii = 0; ii < stuff.length; ii++) {
										helper.add(stuff[ii]);
									}
								}
								values[i] = (float) fit.fitFunction(
										rawCoordinates, d, helper);
							}
							// values[i]=fit.fitFunction(test_point, d, stuff);
						}
					}

					num_eval[0] = num_eval[0] + d;

					/* recompute the sums */
					for (i = 1; i <= d; i++) {
						sum = 0.0f;
						for (j = 1; j <= ssize; j++) {
							sum = sum + simplex[j][i];
						}
						simplex_sum[i] = sum;
					}
				}
			}

		}
	}

}
