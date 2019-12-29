package edu.NUDT.pdl.Nina.StableNC.lib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.Hashtable;
import java.util.Scanner;
import java.util.Vector;

import edu.NUDT.pdl.Nina.util.CoordFitFunction;
import edu.NUDT.pdl.Nina.util.MathUtil;
import edu.NUDT.pdl.Nina.util.Matrix;
import edu.NUDT.pdl.Nina.util.SimplexDownhill;

public class GNP {

	public MathUtil math = new MathUtil(50);
	public float MAX_ERR = (float) 1e20;
	int embedding;
	public float ftol; /* tolerance of convergence for simplex downhill */
	public int gd; /* 0 gradient decent, 1 downhill simplex, 2 powell */
	public float lambda;
	public int restarts;

	float[][] coordinates; /* all coordinates */
	int[] mpi; /* array of indices of the model probes */
	float[] model; /* array of fitted coordinates of mp */
	public Vector<Coordinate> CLandmarks;
	public float[][] dt2p; /* distance from each probe to target */
	public float[] txy; /* array of coordinates for targets */
	public float[] allxy; /* all nodes */
	InetAddress[] tips; /* address for targets */
	public int mp; /* number of probes used in the model */
	public static int dim; /* dims */
	public int trys; /* for models */

	public int norm; /* 1 (Manhattan), 2 (Euclidean), etc */
	public int normalized; /*
							 * 0 or 1 or 2, two ways to normalize, 2 means use
							 * log
							 */
	public int no_square = 0; /* 0 or 1, do not square */
	public int np; /* number of probes altogether */
	public int nt; /* number of targets altogether */
	int mps; /* subset of model probes to use for final host fitting */
	int[] mpsi; /* index of the subset of model probes */
	int try2; /* for targets */
	public int mid; /* to index different models we generate in each run */
	public int full_mesh_data;
	public int range;

	// ----------------
	final boolean isHyperbolic;

	Matrix landmarkLatencies = new Matrix();

	// --------------------------------------------------
	public GNP() {
		CLandmarks = new Vector<Coordinate>(10);
		isHyperbolic = false;
	}

	// --------------------------------------------------

	public void testDim(String fullMatrix, int landmarkMin, int landmarkMax,
			int MatrixType) {

		PrintWriter ps, ps1, ps2;

		//
		// init full matrix
		landmarkLatencies = new Matrix();
		landmarkLatencies.setFormat(MatrixType);
		landmarkLatencies.readlatencyMatrix(fullMatrix);

		try {
			long id = Math.round(Math.random() * 100);
			ps = new PrintWriter("GNPL" + landmarkLatencies.getColums() + "D"
					+ dim + "Matrix.dat" + id);
			ps1 = new PrintWriter("GNPL" + landmarkLatencies.getColums() + "D"
					+ dim + "all_coordinates" + id); // for coordinates

			//
			// int total=1;
			// for(int rept=1;rept<total;rept++){

			// allxy=new float[landmarkLatencies.getColums()*dim];
			// all coordinates

			initFromFullMatrix(landmarkMin, landmarkMax, MatrixType);
			// clear();

			// }
			ps.flush();
			ps1.flush();
			ps.close();
			ps1.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void initFromFullMatrix(int landmarkMin, int landmarkMax,
			int MatrixType) {

		PrintWriter ps, ps1, ps2;
		int i;
		int[] index;
		int targets;
		float[] targetLatency2P;
		boolean repeated = false;
		// random from min to max
		int iers; // pointers
		boolean firstModel = false;

		int landmarks = landmarkMin;
		Hashtable<Integer, Integer> test = new Hashtable<Integer, Integer>();
		float[][] matrix; /* measured distances between probes */
		for (; landmarks <= landmarkMax; landmarks++) {

			// initialize the LM's coordinates
			if (CLandmarks.size() > 0) {
				CLandmarks.clear();
				for (int ind = 0; ind < landmarkMax; ind++) {
					CLandmarks.add(new Coordinate(dim));
				}
			}
			//	  
			firstModel = true;
			iers = 0;
			test.clear();
			System.out.println("$ current " + landmarks);
			mp = landmarks;
			mpsi = new int[mp];
			matrix = new float[mp][mp];
			// model=new float[mp*dim];
			index = new int[landmarks]; // landmark index
			targetLatency2P = new float[mp];
			// ps1.print("\n$ "+mp+" landmark: ");
			// int []temp=new int[mp*10];// more choices
			while (true) {
				if (iers == landmarks) {
					break;
				}
				/*
				 * if(iers==temp.length){ break; }
				 */
				i = (int) math.uniform(0, landmarkLatencies.getRows() - 1);
				if (test.containsKey(i)) {
					continue;
				}

				index[iers] = i; // Note: maybe repeated, but for large matrix
				// ignored is resonable
				test.put(i, iers);
				// ps1.print(i+" ");
				// System.out.print(index[iers]+" ");
				iers++;
			}
			// ps1.print("\n");
			mpi = index;

			for (int h = 0; h < landmarks; h++) {
				for (int f = 0; f < landmarks; f++) {
					matrix[h][f] = landmarkLatencies.get(index[h], index[f]);
					if (!landmarkLatencies.exists(index[h], index[f])) {
						// current postion not exists
						System.err.format("error, empty landmarks exist");
					}
					System.out.print(" " + matrix[h][f]);
				}
				System.out.println();
			}
			// System.exit(1);

			Vector<Coordinate> CoordLandmark = null;
			for (int h = 0; h < 3; h++) {
				if (firstModel) {
					CoordLandmark = fitModel_Coord(matrix, null);
					firstModel = false;
					break;
				} else {

					fitModel_Coord(matrix, CoordLandmark);
				}

			}

			for (i = 0; i < landmarkLatencies.getColums(); i++) {
				if (test.containsKey(i) == true) {
					continue;
				}
				for (int h = 0; h < landmarks; h++) {
					targetLatency2P[h] = landmarkLatencies.get(i, index[h]);
					// System.out.print(" "+targetLatency2P[h]);
				}

				Coordinate xy = null;
				if (firstModel) {
					xy = fitTarget_Coord(targetLatency2P, null);
				} else {
					Vector<Coordinate> tmp = new Vector<Coordinate>(1);
					if (xy != null) {
						tmp.add(xy.makeCopy());
						xy = fitTarget_Coord(targetLatency2P, tmp);
					} else {
						xy = fitTarget_Coord(targetLatency2P, null);
					}
				}

			}

			// System.out.println("\n@ inRange:"+(float)inRange/(landmarkLatencies.getColums()-landmarks));
			// System.out.print("\n#######################");
			// float[] e=getRelativeErrors();
			/*
			 * System.out.println(e[0]); System.out.println(e[1]);
			 * System.out.println(e[2]); System.out.println(e[3]);
			 */

			// ------------------
			// clear();
		}

	}

	// ---------------------------------------------------
	/**
	 * 
	 * @param paramFile
	 */
	public void initParams(String paramFile) {

		try {
			Scanner s = new Scanner(new BufferedReader(new FileReader(
					"GNP_Params")));
			np = s.nextInt();
			embedding = s.nextInt();
			dim = s.nextInt();
			norm = s.nextInt();
			normalized = s.nextInt();
			no_square = s.nextInt();
			range = s.nextInt();
			lambda = s.nextInt();
			ftol = s.nextFloat();
			trys = s.nextInt();
			try2 = s.nextInt();
			restarts = s.nextInt();
			full_mesh_data = s.nextInt();
			gd = s.nextInt();
			/* forexample19 XXX 0 7 2 1 0 500 2000 0.000001 3 3 1 1 1 */
			System.out.format("$ dim%d, ftol %f norm %d full_mesh_data %d\n",
					dim, ftol, norm, full_mesh_data);
			/*
			 * mid=s.nextInt(); while(mid>0){ mid--; mp=s.nextInt(); mpi=new
			 * int[mp]; mpsi=new int[mp]; model=new float[mpdim]; init the
			 * model's coordinates for optimization for(int i=0;i<mp;i++){
			 * mpi[i]=s.nextInt(); //System.out.format("$ mpi: %d ",mpi[i]);
			 * /for(int j=0;j<dim;j++){ model[idim+j]=s.nextFloat();
			 * System.out.format(":  %f",model[idim+j]); }
			 * System.out.format("\n");
			 * 
			 * 
			 * }
			 * 
			 * fitModel(null); if(full_mesh_data==1){ fitRemain(null); }else{
			 * fitRemain(null); for(int i=0;i<nt;i++) {
			 * fitTarget(i,dt2p[i],null); }
			 * 
			 * 
			 * 
			 * 
			 * }
			 * 
			 * 
			 * 
			 * }
			 */

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param matrix
	 *            - latency matrix between landmarks
	 * @param index
	 *            - index of landmarks ' coordinates
	 */
	public void fitModel(final float[][] matrix, int[] index, float[] informedxy) {
		float error, dis1, dis2;
		boolean isCoord = false;
		error = solve(null, model, informedxy, mp * dim,
				new CoordFitFunction() {
					/* fit the model, from 1, not 0 */
					public float fitFunction(float[] rawCoordinates,
							int totalCoordinateElements, float[] HelperDis) {
						float dist, sum, error, sum2;
						int i, j;

						sum = 0;
						sum2 = 0;
						// System.out.println(matrix[0][1]);
						float[][] dist1 = new float[mp][mp];
						for (i = 0; i < mp; i++) {
							dist1[i][i] = 0;
							for (j = i + 1; j < mp; j++) {

								dist = MathUtil.linear_distance(rawCoordinates,
										dim * i + 1, rawCoordinates, dim * j
												+ 1, dim, norm);

								dist1[i][j] = dist1[j][i] = dist;
								if (normalized == 1) {
									/* normalized error */
									error = (matrix[i][j] - dist)
											/ Math.min(matrix[i][j], dist);
								} else if (normalized == 2) {
									/* log transform error */
									error = (float) Math.log(Math
											.abs(matrix[i][j] - dist) + 1.0);
								} else if (normalized == 3) {
									/* transform distances by log */
									error = (float) Math.log(matrix[i][j]
											/ dist);
								} else if (normalized == 4) {
									/* relative error */
									error = Math.max(matrix[i][j], dist)
											/ Math.min(matrix[i][j], dist);
								} else if (normalized == 5) {
									/* heavily skewed normalized error */
									error = (matrix[i][j] - dist)
											/ (float) Math.pow(matrix[i][j], 2);
								} else {
									/* absolute error */
									error = matrix[i][j] - dist;
								}

								if (no_square == 1) {
									error = Math.abs(error);
								} else {
									error = (float) Math.pow(error, 2);
								}

								sum += error;

							}

						}
						return sum;
					}

					
					public double fitFunction(
							Vector<Coordinate> rawCoordinates,
							int totalCoordinateElements, Vector HelperDis) {
						// TODO Auto-generated method stub
						return 0;
					}
				}, trys, isCoord);
		int k = -1;
		for (int i : index) {
			k++;
			for (int j = 0; j < dim; j++) {
				allxy[i * dim + j] = model[k * dim + j];
			}
		}
		System.out.println("@_@: error" + error / mp);

	}

	/**
	 * 
	 * @param matrix
	 *            - latency matrix between landmarks
	 * @param index
	 *            - index of landmarks ' coordinates
	 */
	public Vector<Coordinate> fitModel_Coord(final float[][] matrix,
			Vector<Coordinate> informedxy) {
		float error, dis1, dis2;
		boolean isCoord = true;
		error = solve_Coord(null, CLandmarks, informedxy, mp * dim,
				new CoordFitFunction() {
					/* fit the model, from 1, not 0 */
					
					public double fitFunction(
							Vector<Coordinate> rawCoordinates,
							int totalCoordinateElements, Vector HelperDis) {

						double dist, sum, error, sum2;
						int i, j;

						sum = 0;
						sum2 = 0;
						// System.out.println(matrix[0][1]);

						for (i = 0; i < mp; i++) {

							for (j = i + 1; j < mp; j++) {
								dist = rawCoordinates.get(i).distanceTo(
										rawCoordinates.get(j));
								// System.out.println("PHY: "+matrix[i][j]+", Virtual: "+dist);

								if (normalized == 1) {
									/* normalized error */
									error = (matrix[i][j] - dist)
											/ Math.min(matrix[i][j], dist);
								} else if (normalized == 2) {
									/* log transform error */
									error = (float) Math.log(Math
											.abs(matrix[i][j] - dist) + 1.0);
								} else if (normalized == 3) {
									/* transform distances by log */
									error = (float) Math.log(matrix[i][j]
											/ dist);
								} else if (normalized == 4) {
									/* relative error */
									error = Math.max(matrix[i][j], dist)
											/ Math.min(matrix[i][j], dist);
								} else if (normalized == 5) {
									/* heavily skewed normalized error */
									error = (matrix[i][j] - dist)
											/ (float) Math.pow(matrix[i][j], 2);
								} else {
									/* absolute error */
									error = matrix[i][j] - dist;
								}

								if (no_square == 1) {
									error = Math.abs(error);
								} else {
									error = (float) Math.pow(error, 2);
								}

								sum += error;
							}
						}

						return sum;
					}

					
					public float fitFunction(float[] rawCoordinates,
							int totalCoordinateElements, float[] HelperDis) {
						// TODO Auto-generated method stub
						return -1;
					}
				}, trys, isCoord);

		// --------------------------------------
		System.out.println("@_@: error" + error / mp);
		return CLandmarks;

	}

	/**
	 * 
	 * @param index
	 *            - index of the target
	 * @param Target
	 *            - the network latency
	 * @param informedxy
	 *            -the initial coordinates
	 * @return
	 */
	public float[] fitTarget(int index, float[] Target, float[] informedxy) {
		float error, dis1, dis2, dis3, sum;
		float[] xy;
		InetAddress a;
		int i, j, k;
		File f, f1, f2;

		xy = new float[dim];
		for (i = 0; i < mp; i++) {
			mpsi[i] = i;
		}
		boolean isCoord = false;
		error = solve(Target, xy, informedxy, dim, new CoordFitFunction() {
			public float fitFunction(float[] rawCoordinates,
					int totalCoordinateElements, float[] HelperDis) {
				float dist, sum, error, sum2;
				int i;

				sum = 0;
				sum2 = 0;
				float[] dist1 = new float[mp];
				for (i = 0; i < mp; i++) {

					dist = MathUtil.linear_distance(rawCoordinates, 1, model,
							dim * i, dim, norm);
					dist1[i] = dist;
					// System.out.println("dist: PHY "+HelperDis[i]+" coord "+dist);
					if (normalized == 1) {
						/* normalized error */
						error = (HelperDis[i] - dist)
								/ Math.min(HelperDis[i], dist);
					} else if (normalized == 2) {
						/* transform error by log */
						error = (float) Math
								.log(Math.abs(HelperDis[i] - dist) + 1.0);
					} else if (normalized == 3) {
						/* transform distances by log */
						error = (float) Math.log(HelperDis[i] / dist);
					} else {
						/* absolute error */
						error = HelperDis[i] - dist;
					}

					if (no_square == 1) {
						error = Math.abs(error);
					} else {
						error = (float) Math.pow(error, 2);
					}

					sum += error;
				}

				// System.out.println("$ SUM :"+"RE: "+sum);
				return sum;

			}

			
			public double fitFunction(Vector<Coordinate> rawCoordinates,
					int totalCoordinateElements, Vector HelperDis) {
				// TODO Auto-generated method stub
				return 0;
			}

		}, try2, isCoord);
		for (j = 0; j < dim; j++) {
			allxy[dim * index + j] = xy[j];
		}
		return xy;

		/*
		 * for(j=0;j<mp;j++){ dis1=math.linear_distance(txy, indexdim, model,
		 * jdim, dim, norm); dis2=dt2p[index][mpi[j]];
		 * dis3=Math.abs(dis1-dis2)/dis2; sum+=dis3; ps.format("%u %f %f %f\n",
		 * mpi[j], dis1, dis2,dis3); } ps.format("%f\n", sum);
		 */

	}

	/**
	 * Note: for each node
	 * 
	 * @param Target
	 *            , latency towards all landmarks
	 * @param informedxy
	 *            , single coordinate of the requesting node
	 * @return
	 */
	public Coordinate fitTarget_Coord(float[] Target,
			Vector<Coordinate> informedxy) {
		Vector helper = new Vector(10);
		if (Target != null && Target.length > 0) {
			for (int i = 0; i < Target.length; i++) {
				helper.add(Target[i]);
			}
		} else {
			return null;
		}
		boolean isCoord = true;
		float error, dis1, dis2, dis3, sum;
		Vector<Coordinate> xy = new Vector<Coordinate>(1);
		xy.add(new Coordinate(GNP.dim));

		error = solve_Coord(helper, xy, informedxy, dim,
				new CoordFitFunction() {
					
					public double fitFunction(
							Vector<Coordinate> rawCoordinates,
							int totalCoordinateElements, Vector HelperDis) {
						double dist, sum, error, sum2;
						int i;

						sum = 0;
						sum2 = 0;
						for (i = 0; i < mp; i++) {

							// coordinate distance to ith landmark
							dist = rawCoordinates.get(0).distanceTo(
									CLandmarks.get(i));

							// System.out.println("dist: PHY "+((Float)HelperDis.get(i)).floatValue()+" coord "+dist);
							if (normalized == 1) {
								/* normalized error */
								error = (((Float) HelperDis.get(i))
										.floatValue() - dist)
										/ Math.min(((Float) HelperDis.get(i))
												.floatValue(), dist);
							} else if (normalized == 2) {
								/* transform error by log */
								error = (float) Math.log(Math
										.abs(((Float) HelperDis.get(i))
												.floatValue()
												- dist) + 1.0);
							} else if (normalized == 3) {
								/* transform distances by log */
								error = (float) Math.log(((Float) HelperDis
										.get(i)).floatValue()
										/ dist);
							} else {
								/* absolute error */
								error = ((Float) HelperDis.get(i)).floatValue()
										- dist;
							}

							if (no_square == 1) {
								error = Math.abs(error);
							} else {
								error = (float) Math.pow(error, 2);
							}

							sum += error;
						}

						// System.out.println("$ SUM :"+"RE: "+sum);
						return sum;

					}

					
					public float fitFunction(float[] rawCoordinates,
							int totalCoordinateElements, float[] HelperDis) {
						// TODO Auto-generated method stub
						return 0;
					}

				}, try2, isCoord);

		return xy.get(0);

	}

	// --------------------------------------------------

	private float solve_Coord(Vector Latency2Landmarks,
			Vector<Coordinate> landmarks, Vector<Coordinate> informedxy,
			int num, CoordFitFunction fits, int mytry, boolean isCoord) {
		// 
		float min, fit = 0.0f, localftol;
		float[] solution;
		float[][] p;
		float[] y;
		int i, restarted;
		int[] j;
		j = new int[1];
		localftol = ftol;
		// myxy=new float[num+1];
		p = new float[num + 1 + 1][num + 1];
		y = new float[num + 1 + 1];
		Vector<Coordinate> rawCoordinates = new Vector<Coordinate>(5);
		// the number of nodes
		int sizeofNodes = num / dim;

		while (true) {
			min = MAX_ERR;
			for (i = 0; i < mytry; i++) {
				restarted = 0;

				// System.arraycopy(p[1], 1, myxy, 1, num);
				initMyCoordinate(p[1], informedxy, sizeofNodes);
				translate(p[1], rawCoordinates, sizeofNodes);
				/* use simplex downhill */
				while (true) {
					// latest p[1] copied
					translate(p[1], rawCoordinates, sizeofNodes);
					y[1] = (float) fits.fitFunction(rawCoordinates, num,
							Latency2Landmarks);
					for (j[0] = 2; j[0] <= num + 1; j[0]++) {
						memcpy(p[j[0]], p[1], num + 1);
						p[j[0]][j[0] - 1] += lambda;
						// copy again
						translate(p[j[0]], rawCoordinates, sizeofNodes);

						y[j[0]] = (float) fits.fitFunction(rawCoordinates, num,
								Latency2Landmarks); // from 1
					}
					float[] d = null;
					if (Latency2Landmarks != null
							&& Latency2Landmarks.size() > 0) {
						d = new float[Latency2Landmarks.size()];
						for (int ind = 0; ind < Latency2Landmarks.size(); ind++) {
							d[ind] = ((Float) Latency2Landmarks.get(ind))
									.floatValue();
						}
					}
					SimplexDownhill.simplex_downhill(p, y, num, localftol,
							fits, j, d, isCoord);
					if (j[0] < 0) {
						System.out.printf("No answer\n");
						break;
					}

					if (restarted < restarts) {
						restarted++;
						continue;
					} else {
						break;
					}
				}
				if (j[0] < 0) {
					continue;
				}
				// copy again
				translate(p[1], rawCoordinates, sizeofNodes);
				fit = (float) fits.fitFunction(rawCoordinates, num,
						Latency2Landmarks);
				System.out.printf("\n\n@_@Fit = %.15f\n", fit);

				if (fit < min) {
					min = fit;
					// solution-resulting coordinate
					solution = new float[num];
					for (j[0] = 0; j[0] < num; j[0]++) {
						solution[j[0]] = p[1][j[0] + 1];
					}
					for (int Index = 0; Index < sizeofNodes; Index++) {
						double[] instance = new double[dim];
						for (int from = 0; from < dim; from++) {
							instance[from] = solution[Index * dim + from];
						}
						Coordinate tmp = new Coordinate(dim);
						tmp.add(new Vec(instance, true, isHyperbolic));
						landmarks.add(tmp.makeCopy());
					}
				}
			}
			if (min == MAX_ERR) {
				/* not converge ,loose ftol and continue */
				localftol *= 10;
				continue;
			} else {
				break;
			}
		}
		/* finally */
		// System.out.format("\n$Now we have %f",min );
		return min;
	}

	/**
	 * move records from arrays to vectors
	 * 
	 * @param fs
	 * @param rawCoordinates
	 * @param sizeofNodes
	 */
	public static void translate(float[] fs, Vector<Coordinate> rawCoordinates,
			int sizeofNodes) {
		// TODO Auto-generated method stub
		int dim = Math.round((fs.length - 1) / sizeofNodes);
		// System.out.println("DIM: "+dim);

		if (rawCoordinates == null) {
			rawCoordinates = new Vector<Coordinate>(10);
		}
		// remove old records
		if (!rawCoordinates.isEmpty()) {
			rawCoordinates.clear();
		}

		for (int Index = 0; Index < sizeofNodes; Index++) {
			double[] instance = new double[dim];
			for (int from = 0; from < dim; from++) {
				int index = Index * dim + from + 1;
				// System.out.println("$: "+index);
				instance[from] = fs[index];
				// Note!!!, the coordinate begins from 1, NOT 0.
			}
			Coordinate tmp = new Coordinate(instance, false);
			// System.err.print("\n===========\ntranslated compted"+tmp.toString()+"\n===========\n ");
			rawCoordinates.add(tmp.makeCopy());
		}

	}

	private void initMyCoordinate(float[] myxy, Vector<Coordinate> info,
			int sizeofNodes) {
		if (info == null) {
			/* randomized */
			for (int i = 0; i <= sizeofNodes * dim; i++) {
				myxy[i] = (float) math.uniform(-range / 2.0f, range / 2.0f);
			}
		} else {
			// TODO: init the coordinate
			for (int Index = 0; Index < sizeofNodes; Index++) {
				Coordinate tmp = info.get(Index);
				for (int from = 0; from < dim; from++) {
					myxy[Index * dim + from + 1] = (float) tmp.coords[from];
				}

			}
		}

	}

	// -------------------------------------------------
	/**
	 * use informedinfo to init user coordinates
	 * 
	 * @param myxy
	 * @param info
	 * @param dim
	 */
	public void initMyCoordinate(float[] myxy, float[] info, int dims) {
		if (info == null) {
			/* randomized */
			for (int i = 0; i <= dims; i++) {
				myxy[i] = (float) math.uniform(-range / 2.0f, range / 2.0f);
			}
		} else {
			for (int i = 0; i < dims; i++) {
				myxy[i + 1] = info[i];
				// System.out.format("$ %d %f ",i+1,myxy[i+1]);
			}
			// System.out.format("\n");
		}

	}

	/**
	 * 
	 * @param <T>
	 * @param src
	 * @param dst
	 * @param len
	 */
	public <T> void memcpy(float[] dst, float[] src, int len) {
		for (int i = 0; i < len; i++) {
			dst[i] = src[i];
		}
	}

	/**
	 * per routine
	 * 
	 * @param d
	 * @param solution
	 * @param informedxy
	 * @param num
	 * @param fits
	 * @param mytry
	 * @return
	 */
	public float solve(float[] d, float[] solution, float[] informedxy,
			int num, CoordFitFunction fits, int mytry, boolean isCoord) {
		float min, fit = 0.0f, localftol;
		float[][] p;
		float[] y;
		int i, restarted;
		int[] j;
		j = new int[1];
		localftol = ftol;
		// myxy=new float[num+1];
		p = new float[num + 1 + 1][num + 1];
		y = new float[num + 1 + 1];

		while (true) {
			min = MAX_ERR;
			for (i = 0; i < mytry; i++) {
				restarted = 0;

				// System.arraycopy(p[1], 1, myxy, 1, num);
				initMyCoordinate(p[1], informedxy, num);
				/* use simplex downhill */
				while (true) {
					y[1] = fits.fitFunction(p[1], num, d);
					for (j[0] = 2; j[0] <= num + 1; j[0]++) {
						memcpy(p[j[0]], p[1], num + 1);
						p[j[0]][j[0] - 1] += lambda;
						y[j[0]] = fits.fitFunction(p[j[0]], num, d); // from 1
					}
					SimplexDownhill.simplex_downhill(p, y, num, localftol,
							fits, j, d, isCoord);
					if (j[0] < 0) {
						System.out.printf("No answer\n");
						break;
					}

					if (restarted < restarts) {
						restarted++;
						continue;
					} else {
						break;
					}
				}
				if (j[0] < 0) {
					continue;
				}

				fit = fits.fitFunction(p[1], num, d);
				System.out.printf("\n\n@_@Fit = %.15f\n", fit);

				if (fit < min) {
					min = fit;
					for (j[0] = 0; j[0] < num; j[0]++) {
						solution[j[0]] = p[1][j[0] + 1];
					}
				}
			}
			if (min == MAX_ERR) {
				/* not converge ,loose ftol and continue */
				localftol *= 10;
				continue;
			} else {
				break;
			}
		}
		/* finally */
		// System.out.format("\n$Now we have %f",min );
		return min;
	}

}
