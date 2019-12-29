package edu.NUDT.pdl.Nina.Sim.SimClustering;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Random;

public class HSH_SimV3 {

	int row;
	int column;
	double[][] mat;

	// static String PATH = "169.dat";Bandwidth
	static String PATH = "Capacity";

	public static final int LANDMARK_NUM = 25;
	public static final int DIMS_NUM = 6;
	static int[] landmarkNodes;

	// S r*r HSH_SimV3
	static HSH_SimV3 S;
	// H n*r landmark coordinate vector HSH_SimV3
	static HSH_SimV3 H;
	// D n*n distance matirx of n nodes
	static HSH_SimV3 D;

	public static BufferedWriter logCluster = null;
	static String ClusterLogName = "ClusterLogName";

	HSH_SimV3() {
		row = 1;
		column = 1;
		mat[0][0] = 0;
	}

	HSH_SimV3(int m, int n) {
		row = m;
		column = n;
		mat = new double[row][column];
		for (int i = 0; i < row; i++)
			for (int j = 0; j < column; j++)
				mat[i][j] = 0;
	}

	public void Print() {
		if (this == null) {
			// System.out.println("");
			return;
		}
		for (int i = 0; i < row; i++) {
			for (int j = 0; j < column; j++) {
				if (Math.abs(mat[i][j]) < .1E-5)
					mat[i][j] = 0;
				System.out.print(mat[i][j] + "\t");
			}
			System.out.println();
		}
	}

	/*
	 * initialize HSH_SimV3s
	 */
	// create a HSH_SimV3 with randam elements
	void Rand() {
		for (int i = 0; i < row; i++)
			for (int j = 0; j < column; j++)
				mat[i][j] = Math.random() * 100;
	}

	// create a symmetric HSH_SimV3
	void CreatSymHSH_SimV3() {
		if (row != column)
			return;
		for (int i = 0; i < row; i++) {
			mat[i][i] = 0;
			for (int j = 0; j < i; j++) {
				mat[i][j] = Math.random() * 100;
				mat[j][i] = mat[i][j];
			}
		}
		return;
	}

	/*
	 * read file
	 */
	// read file information which records the distance HSH_SimV3
	// the first line of the file implys the distance HSH_SimV3's row No.(viz
	// column No.)
	static int getRowNo(File file) {
		int row = -1;
		if (file != null) {
			try {
				RandomAccessFile raf = new RandomAccessFile(file, "r");
				if (raf == null) {
					System.err.println("empty file");
					return row;
				} else {
					String curLine = raf.readLine();
					row = Integer.parseInt(curLine);
				}
				raf.close();
				return row;
			} catch (Exception e) {
			}
		}
		System.err.println(" empty file");
		return row;
	}

	// read file and creat a HSH_SimV3 objective which includes the same
	// distance HSH_SimV3
	static public HSH_SimV3 readFile2HSH_SimV3(File file) {
		if (file != null) {
			try {
				RandomAccessFile raf = new RandomAccessFile(file, "r");
				if (raf == null) {
					System.err.println("empty file");
					return null;
				} else {
					int row = 0;
					int column = 0;
					int line = -1;

					String curLine;
					curLine = raf.readLine();

					String[] s = curLine.split("[ \\s\t ]");
					row = column = Integer.parseInt(s[0]);

					HSH_SimV3 latencyHSH_SimV3 = new HSH_SimV3(row, column);

					while (true) {
						curLine = raf.readLine();
						line++;

						if (curLine == null)
							break;

						s = curLine.split("[ \\s\t ]");

						for (int j = 0; j < column; j++)
							latencyHSH_SimV3.mat[line][j] = Double
									.parseDouble(s[j]);
					}
					raf.close();
					return latencyHSH_SimV3;
				}
			} catch (Exception e) {
			}
		}
		System.err.println(" empty file");
		return null;
	}

	/*
	 * HSH_SimV3 operation
	 */
	// transpose the HSH_SimV3
	HSH_SimV3 ToRank() {
		HSH_SimV3 result = new HSH_SimV3(column, row);
		for (int i = 0; i < column; i++)
			for (int j = 0; j < row; j++)
				result.mat[i][j] = mat[j][i];
		return result;
	}

	// multuply operation between the corresponding elements of two HSH_SimV3
	HSH_SimV3 DotMultiply(HSH_SimV3 temp) {
		if (column != temp.column || row != temp.row) {
			// System.out.println("");
			return null;
		} else {
			HSH_SimV3 result = new HSH_SimV3(row, column);
			for (int i = 0; i < row; i++)
				for (int j = 0; j < column; j++)
					result.mat[i][j] = mat[i][j] * temp.mat[i][j];
			return result;
		}
	}

	// divide operation between the corresponding elements of two HSH_SimV3
	HSH_SimV3 DotDivide(HSH_SimV3 temp) {
		if (column != temp.column || row != temp.row) {
			// System.out.println("");
			return null;
		} else {
			HSH_SimV3 result = new HSH_SimV3(row, column);
			for (int i = 0; i < row; i++)
				for (int j = 0; j < column; j++)
					result.mat[i][j] = mat[i][j] / temp.mat[i][j];
			return result;
		}
	}

	// add operation between the corresponding elements of two HSH_SimV3
	HSH_SimV3 DotAdd(HSH_SimV3 temp) {
		if (column != temp.column || row != temp.row) {
			// System.out.println("");
			return null;
		} else {
			HSH_SimV3 result = new HSH_SimV3(row, column);
			for (int i = 0; i < row; i++)
				for (int j = 0; j < column; j++)
					result.mat[i][j] = mat[i][j] + temp.mat[i][j];
			return result;
		}
	}

	// sqrt operation for each element respectively
	HSH_SimV3 Sqrt() {
		HSH_SimV3 result = new HSH_SimV3(row, column);
		for (int i = 0; i < row; i++)
			for (int j = 0; j < column; j++) {
				if (mat[i][j] >= 0)
					result.mat[i][j] = Math.sqrt(mat[i][j]);
				else {
					// System.out.println("");
					return null;
				}
			}
		return result;
	}

	HSH_SimV3 HSH_SimV3Multiply(HSH_SimV3 temp) {
		if (column != temp.row) {
			// System.out.println("");
			return null;
		} else {
			HSH_SimV3 result = new HSH_SimV3(row, temp.column);
			for (int i = 0; i < row; i++)
				for (int j = 0; j < temp.column; j++)
					for (int k = 0; k < column; k++)
						result.mat[i][j] += mat[i][k] * temp.mat[k][j];
			return result;
		}
	}

	// get one HSH_SimV3's inverse HSH_SimV3
	HSH_SimV3 Inverse() {
		if (row != column) {
			// System.out.println("Max is not square");
			return null;
		}

		//
		HSH_SimV3 B = new HSH_SimV3(row, column);

		for (int i = 0; i < row; i++) {
			for (int j = 0; j < column; j++)
				B.mat[i][j] = mat[i][j];
		}
		//
		double t = 0;
		//	
		for (int k = 0; k < column - 1; k++) {
			//		
			int p = -1;
			//
			for (int m = k; m < row; m++) {
				if (Math.abs(B.mat[m][k]) > .1E-5) {
					p = m;
					break;
				}
				B.mat[m][k] = 0;
			}
			//
			if (p == -1) {
				// //System.out.println("");
				return null;
			}
			//		
			if (p != k) {
				for (int j = 0; j < column; j++) {
					//
					t = B.mat[p][j];
					B.mat[p][j] = B.mat[k][j];
					B.mat[k][j] = t;
				}
			}
			//		
			for (int i = k + 1; i < row; i++) {
				t = B.mat[i][k] / B.mat[k][k];
				for (int j = k; j < column; j++) {
					B.mat[i][j] -= t * B.mat[k][j];
					if (Math.abs(B.mat[i][j]) < .1E-5)
						B.mat[i][j] = 0;
				}
			}
		}
		//
		if (B.mat[row - 1][column - 1] == 0) {
			// //System.out.println(");
			return null;
		}

		//
		HSH_SimV3 temp = new HSH_SimV3(row, 2 * column);
		for (int i = 0; i < row; i++)
			for (int j = 0; j < column; j++)
				temp.mat[i][j] = mat[i][j];
		for (int j = 0; j < column; j++)
			temp.mat[j][j + column] = 1;

		for (int i = 0; i < row; i++) {
			if (temp.mat[i][i] == 0) {
				for (int m = i; m < row; m++)
					if (Math.abs(temp.mat[m][i]) > .1E-5) {
						for (int j = i; j < 2 * column; j++)
							temp.mat[i][j] += temp.mat[m][j];
						break;
					}

			}

			if (temp.mat[i][i] != 1) {
				double bs = temp.mat[i][i];
				temp.mat[i][i] = 1;
				for (int p = i + 1; p < temp.column; p++)
					temp.mat[i][p] /= bs;
			}

			for (int q = 0; q < row; q++) {
				if (q != i) {
					double bs = temp.mat[q][i];
					for (int s = 0; s < temp.column; s++)
						temp.mat[q][s] -= bs * temp.mat[i][s];
				} else
					continue;
			}
		}
		HSH_SimV3 result = new HSH_SimV3(row, column);
		for (int i = 0; i < row; i++)
			for (int j = 0; j < column; j++)
				result.mat[i][j] = temp.mat[i][j + column];
		return result;
	}

	static void init_SymmetricNMF() throws IOException {
		File file = new File(PATH);
		int nodesNum = getRowNo(file);

		H = new HSH_SimV3(nodesNum, DIMS_NUM);
		S = new HSH_SimV3(DIMS_NUM, DIMS_NUM);
		D = new HSH_SimV3(nodesNum, nodesNum);
		H.Rand();
		S.Rand();
		D = readFile2HSH_SimV3(file);

		logCluster = new BufferedWriter(new FileWriter(new File(ClusterLogName
				+ ".log")));

	}

	static void init_NMF() throws IOException {
		H = new HSH_SimV3(LANDMARK_NUM, DIMS_NUM);
		S = new HSH_SimV3(DIMS_NUM, DIMS_NUM);
		H.Rand();
		S.Rand();

		File file = new File(PATH);
		int nodesNum = getRowNo(file);
		D = new HSH_SimV3(nodesNum, nodesNum);
		D = readFile2HSH_SimV3(file);
		// used in distrirbuted computation
		landmarkNodes = new int[LANDMARK_NUM];

		logCluster = new BufferedWriter(new FileWriter(new File(ClusterLogName
				+ ".log")));
	}

	void CheckError(HSH_SimV3 DD) {
		HSH_SimV3 result = new HSH_SimV3(row, column);
		for (int i = 0; i < row; i++)
			for (int j = 0; j < column; j++)
				result.mat[i][j] = Math.abs(DD.mat[i][j] - D.mat[i][j]);
		// System.out.println("\nABSOLUTE ERROR:");
		result.Print();

		for (int i = 0; i < row; i++)
			for (int j = 0; j < column; j++) {
				if (i == j)
					result.mat[i][i] = 0;
				else
					result.mat[i][j] = 100 * result.mat[i][j]
							/ Math.max(DD.mat[i][j], D.mat[i][j]);
				// if(result.mat[i][j]>50)
				// //System.out.println("TTTTTTTTTTTTTTTTTTTTTT");////////////////////////////
			}
		// System.out.println("\nRELATIVE ERROR(%):");
		result.Print();
		return;
	}

	/*
	 * factor_3_symmetricNMF
	 */
	public static void symmetric_NMF(HSH_SimV3 landmark2Landmark) {

		// init();
		HSH_SimV3 landmark2Landmark_ = new HSH_SimV3(LANDMARK_NUM, LANDMARK_NUM);
		landmark2Landmark_ = landmark2Landmark.ToRank();

		HSH_SimV3 H_ = new HSH_SimV3(DIMS_NUM, LANDMARK_NUM);

		long startTime = System.currentTimeMillis();
		int iter = 0;
		final int COUNT = 2000;
		do {
			H_ = H.ToRank();

			H = H.DotMultiply((landmark2Landmark_.HSH_SimV3Multiply(H
					.HSH_SimV3Multiply(S)).DotDivide(H.HSH_SimV3Multiply(H_
					.HSH_SimV3Multiply(landmark2Landmark_.HSH_SimV3Multiply(H
							.HSH_SimV3Multiply(S)))))).Sqrt());

			S = S.DotMultiply((H_.HSH_SimV3Multiply(landmark2Landmark
					.HSH_SimV3Multiply(H)).DotDivide(H_.HSH_SimV3Multiply(H
					.HSH_SimV3Multiply(S.HSH_SimV3Multiply(H_
							.HSH_SimV3Multiply(H)))))).Sqrt());
			iter++;
		} while (iter < COUNT);

		System.out.println("\nRUNNING TIME(s):"
				+ (System.currentTimeMillis() - startTime) / 1000);
	}

	/*
	 * */
	HSH_SimV3 Hermite() {
		HSH_SimV3 result = new HSH_SimV3(row, column);
		for (int i = 0; i < row; i++)
			for (int j = 0; j < column; j++)
				result.mat[i][j] = mat[i][j];
		// 变量
		double var;
		//
		int temp = -1;
		// 标记消元进行到哪一行、哪一列
		int rowMark = -1;
		int columnMark = -1;
		// 当第k列存在非零值时，flag置true
		boolean flag;
		// 开始消元
		for (int k = 0; k < column; k++) {
			flag = false;
			// 在第k列查找第一个出现的非零，将其所在行，记录在temp中
			for (int m = rowMark + 1; m < row; m++)
				if (result.mat[m][k] != 0) {
					temp = m;
					flag = true;
					rowMark++;
					columnMark++;
					break;
				}
			// 若第k列没有非零元素了
			if (!flag) {
				columnMark++;
				// 消元已经到了最后一列
				if (k == column - 1) {
					return result;
				}
				continue;
			}
			if (temp != rowMark) {
				for (int j = 0; j < column; j++) {
					// 用来交换矩阵中的两行
					var = result.mat[temp][j];
					result.mat[temp][j] = result.mat[rowMark][j];
					result.mat[rowMark][j] = var;
				}
			}
			// 标准化第rowMark行
			for (int j = columnMark + 1; j < column; j++)
				result.mat[k][j] /= result.mat[rowMark][columnMark];
			result.mat[rowMark][columnMark] = 1;
			// 对其他行，逐行地进行消元
			for (int i = 0; i < row; i++) {
				if (i == rowMark)
					continue;
				var = result.mat[i][k];
				for (int j = columnMark; j < column; j++) {
					result.mat[i][j] -= var * result.mat[rowMark][j];
					if (Math.abs(result.mat[i][j]) < .1E-5)
						result.mat[i][j] = 0;
				}
			}
		}
		return result;
	}

	HSH_SimV3 Moore_PenroseInverse() {
		HSH_SimV3 B = new HSH_SimV3(row, column);
		B = Hermite();
		// 记录B的秩
		int count = 0;
		// 如果B中的一行不全为零，flag置为true
		boolean flag;
		for (int i = 0; i < row; i++) {
			flag = false;
			for (int j = i; j < column; j++) {
				if (B.mat[i][j] != 0) {
					count++;
					flag = true;
					continue;
				}
			}
			// B中第i行全为零时
			if (!flag)
				break;
		}
		int[] record = new int[count];

		for (int i = 0; i < count; i++)
			for (int j = i; j < column; j++)
				if (B.mat[i][j] != 0)
					record[i] = j;
		// 取F为A的J1，J2，...，Jr列构成row*count矩阵
		HSH_SimV3 F = new HSH_SimV3(row, count);
		for (int j = 0; j < count; j++) {
			int t = record[j];
			for (int i = 0; i < row; i++)
				F.mat[i][t] = mat[i][t];
		}
		// G为B的前r行构成count*column矩阵
		HSH_SimV3 G = new HSH_SimV3(count, column);
		for (int i = 0; i < count; i++) {
			for (int j = 0; j < column; j++)
				G.mat[i][j] = B.mat[i][j];
		}
		// 计算A的Moore-Penrose逆矩阵（A+）
		// A+=G__*Inverse(G*G__)*Inverse(F__*F)*F__
		// 由于F、G皆为实矩阵，其共轭转置矩阵即其转置矩阵
		HSH_SimV3 result = new HSH_SimV3(column, row);
		// temp1=G_*Inverse(G*G_)
		HSH_SimV3 temp1 = new HSH_SimV3(column, count);
		temp1 = (G.ToRank())
				.HSH_SimV3Multiply((G.HSH_SimV3Multiply(G.ToRank())).Inverse());

		// temp2=Inverse(F_*F)*F_
		HSH_SimV3 temp2 = new HSH_SimV3(count, row);
		temp2 = (((F.ToRank()).HSH_SimV3Multiply(F)).Inverse())
				.HSH_SimV3Multiply(F.ToRank());
		// （A+）=temp1*temp2
		result = temp1.HSH_SimV3Multiply(temp2);

		return result;
	}

	/*
	 * distributed_newhosts_symmetricNMF host2Landmark: distances from host to
	 * landamrks
	 */
	public static HSH_SimV3 D_N_S_NMF(HSH_SimV3 host2Landmark) {
		if (H.row != host2Landmark.column)
			return null;

		HSH_SimV3 temp1 = new HSH_SimV3(S.row, H.row);
		HSH_SimV3 temp2 = new HSH_SimV3(H.row, S.row);
		HSH_SimV3 temp3 = new HSH_SimV3(host2Landmark.column, 1);
		HSH_SimV3 temp4 = new HSH_SimV3(1, S.row);
		HSH_SimV3 temp5 = new HSH_SimV3(S.row, H.row);

		temp1 = S.HSH_SimV3Multiply(H.ToRank());
		temp2 = temp1.ToRank();
		temp5 = temp2.Moore_PenroseInverse();

		HSH_SimV3 result = new HSH_SimV3(host2Landmark.row, S.row);

		for (int i = 0; i < host2Landmark.row; i++) {
			for (int j = 0; j < host2Landmark.column; j++)
				temp3.mat[j][0] = host2Landmark.mat[i][j];
			temp4 = (temp5.HSH_SimV3Multiply(temp3)).ToRank();
			if (temp4 != null) {
				for (int m = 0; m < S.row; m++)
					result.mat[i][m] = temp4.mat[0][m];
			}
		}

		return result;
	}

	/*
	 * The final algorithm: Step1: choose the landmark nodes randomly Step2:
	 * call symmetric_NMF algorithm use landmark2Landmark, a subset of distace
	 * HSH_SimV3. Step3: according to the S & H which have been computed in
	 * Step2, call D_N_S_NMF algorithm and get the host2Landmark Result:
	 */
	public static void NMF() throws IOException {

		File file = new File(PATH);
		int nodesNum = getRowNo(file);

		try {
			init_NMF();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Step1:
		Random rand = new Random();
		for (int i = 0; i < LANDMARK_NUM; i++) {
			int tmp = -1;
			do {
				tmp = rand.nextInt(nodesNum);
				for (int j = 0; j < i; j++)
					if (tmp == landmarkNodes[j])
						tmp = -1;
			} while (tmp == -1);
			landmarkNodes[i] = tmp;
		}
		java.util.Arrays.sort(landmarkNodes);

		int hostNodes[] = new int[nodesNum - LANDMARK_NUM];
		int k = 0;
		for (int i = 0; i < nodesNum; i++) {
			if (k < LANDMARK_NUM && landmarkNodes[k] == i)
				k++;
			else
				hostNodes[i - k] = i;
		}

		logCluster.write("\nlandmarkNodes:\n");// ///////////////////////
		for (int i = 0; i < LANDMARK_NUM; i++)
			// /////////////////////////////
			logCluster.write(landmarkNodes[i] + "\t");// /////////////////
		// System.out.println();//////////////////////////////////////////
		logCluster.write("\n hosts:\n");
		// System.out.println("hostNodes: ");/////////////////////////////
		for (int i = 0; i < nodesNum - LANDMARK_NUM; i++)
			// ////////////////////
			logCluster.write(hostNodes[i] + "\t");// /////////////////////
		// System.out.println();//////////////////////////////////////////
		logCluster.write("\n");
		// Step2:
		HSH_SimV3 landmark2Landmark = new HSH_SimV3(LANDMARK_NUM, LANDMARK_NUM);
		for (int i = 0; i < LANDMARK_NUM; i++)
			for (int j = 0; j < LANDMARK_NUM; j++)
				landmark2Landmark.mat[i][j] = D.mat[landmarkNodes[i]][landmarkNodes[j]];
		symmetric_NMF(landmark2Landmark);

		// Step3:

		HSH_SimV3 host2Landmark = new HSH_SimV3(nodesNum - LANDMARK_NUM,
				LANDMARK_NUM);
		for (int i = 0; i < nodesNum - LANDMARK_NUM; i++)
			for (int j = 0; j < LANDMARK_NUM; j++)
				host2Landmark.mat[i][j] = D.mat[hostNodes[i]][landmarkNodes[j]];
		HSH_SimV3 hostCoords = new HSH_SimV3(nodesNum - LANDMARK_NUM,
				LANDMARK_NUM);
		hostCoords = D_N_S_NMF(host2Landmark);

		HSH_SimV3 coordsHSH_SimV3 = new HSH_SimV3(hostNodes.length
				+ landmarkNodes.length, DIMS_NUM);
		coordsHSH_SimV3 = mergeCoordsHSH_SimV3(hostCoords, H, hostNodes,
				landmarkNodes);
		// System.out.println();////////////////////////////////////////
		// //System.out.println("coordsHSH_SimV3 : ");///////////////////////
		// coordsHSH_SimV3.Print();////////////////////////////////////////

		// //System.out.println();////////////////////////////////////////
		// //System.out.println("D: ");///////////////////////////////////
		// D.Print();///////////////////////////////////////////////////

		// D.CheckError(getEstimateDistHSH_SimV3(coordsHSH_SimV3));///////////

		showArrayStat(Silhouette_Coefficient(hostCoords, H, hostNodes,
				landmarkNodes));
	}

	static HSH_SimV3 getEstimateDistHSH_SimV3(HSH_SimV3 coordsHSH_SimV3) {
		int nodesNum = coordsHSH_SimV3.row;
		HSH_SimV3 DD = new HSH_SimV3(nodesNum, nodesNum);
		HSH_SimV3 x = new HSH_SimV3(1, DIMS_NUM);
		HSH_SimV3 y = new HSH_SimV3(DIMS_NUM, 1);
		int i, j;
		for (i = 0; i < nodesNum; i++) {
			for (int t = 0; t < DIMS_NUM; t++)
				x.mat[0][t] = coordsHSH_SimV3.mat[i][t];
			for (j = 0; j < nodesNum; j++) {
				if (i == j)
					continue;
				for (int t = 0; t < DIMS_NUM; t++)
					y.mat[t][0] = coordsHSH_SimV3.mat[j][t];
				DD.mat[i][j] = x.HSH_SimV3Multiply(S.HSH_SimV3Multiply(y)).mat[0][0];
			}
		}
		// System.out.println();////////////////////////////
		// System.out.println("DD: ");//////////////////////
		// DD.Print();//////////////////////////////////////
		return DD;
	}

	static HSH_SimV3 mergeCoordsHSH_SimV3(HSH_SimV3 hostCoords,
			HSH_SimV3 landmarkCoords, int[] hostNodes, int[] landmarkNodes) {
		int hostNum = hostNodes.length;
		int landmarkNum = landmarkNodes.length;
		HSH_SimV3 coordsHSH_SimV3 = new HSH_SimV3(hostNum + landmarkNum,
				DIMS_NUM);

		for (int i = 0; i < hostNum; i++)
			for (int j = 0; j < DIMS_NUM; j++)
				coordsHSH_SimV3.mat[hostNodes[i]][j] = hostCoords.mat[i][j];

		for (int i = 0; i < landmarkNum; i++)
			for (int j = 0; j < DIMS_NUM; j++)
				coordsHSH_SimV3.mat[landmarkNodes[i]][j] = landmarkCoords.mat[i][j];
		return coordsHSH_SimV3;
	}

	// divideCluster(mergeCoordsHSH_SimV3(hostCoords, H, hostNodes,
	// landmarkNodes));
	static int[] divideCluster(HSH_SimV3 coordsHSH_SimV3) {
		int nodesNum = coordsHSH_SimV3.row;
		// record the cluster No. in int[] nodes2Cluster
		int nodes2Cluster[] = new int[nodesNum];
		for (int i = 0; i < nodesNum; i++) {
			double max = Math.abs(coordsHSH_SimV3.mat[i][0]);
			nodes2Cluster[i] = 0;
			for (int j = 1; j < DIMS_NUM; j++)
				if (max < Math.abs(coordsHSH_SimV3.mat[i][j])) {
					max = Math.abs(coordsHSH_SimV3.mat[i][j]);
					nodes2Cluster[i] = j;
				}
		}
		return nodes2Cluster;
	}

	// according to the network coordinates, compute the average distance from
	// one certain node to nodes within the same cluster
	static double intraClusterDist(HSH_SimV3 coordsHSH_SimV3,
			int[] nodes2Cluster, int nodeNo) {
		int clusterNo = nodes2Cluster[nodeNo];
		int nodesNum = coordsHSH_SimV3.row;

		HSH_SimV3 x = new HSH_SimV3(1, DIMS_NUM);
		for (int j = 0; j < DIMS_NUM; j++)
			x.mat[0][j] = coordsHSH_SimV3.mat[nodeNo][j];
		HSH_SimV3 y = new HSH_SimV3(DIMS_NUM, 1);
		int count = 0;// the Sum of other nodes in the cluster except the node
						// itself
		double dist = 0;

		// test not -1
		double testNeg = 0;
		for (int i = 0; i < nodesNum; i++)
			if (nodes2Cluster[i] == clusterNo && i != nodeNo) {
				for (int j = 0; j < DIMS_NUM; j++)
					y.mat[j][0] = coordsHSH_SimV3.mat[i][j];
				testNeg = x.HSH_SimV3Multiply(S.HSH_SimV3Multiply(y)).mat[0][0];
				if (testNeg > 0) {
					dist += testNeg;
				} else {
					continue;
				}
				count++;
			}
		if (count != 0)
			return dist / count;
		return 0;
	}

	// return the quantity of clusters
	static int getClusterSum(int[] nodes2Cluster) {
		int clusterSum = 0;// record the quantity of clusters
		int nodesNum = nodes2Cluster.length;
		int tmp[] = new int[nodesNum];
		// copy from nodes2Cluster
		System.arraycopy(nodes2Cluster, 0, tmp, 0, nodesNum);
		int temp = -1;
		for (int i = 0; i < nodesNum; i++)
			if (tmp[i] != -1) {
				temp = tmp[i];
				for (int j = i; j < nodesNum; j++)
					if (temp == tmp[j])
						tmp[j] = -1;
				clusterSum++;
			}
		return clusterSum;
	}

	// get the clusters' No. which the node does not belong to
	static int[] getOtherClusterNo(int[] nodes2Cluster, int clusterNo) {
		int clusterSum = getClusterSum(nodes2Cluster);
		int nodesNum = nodes2Cluster.length;
		int tmp[] = new int[nodesNum];
		// copy from nodes2Cluster
		System.arraycopy(nodes2Cluster, 0, tmp, 0, nodesNum);
		for (int i = 0; i < nodesNum; i++)
			if (tmp[i] == clusterNo)
				tmp[i] = -1;
		int otherClusterNo[] = new int[clusterSum - 1];
		int temp = -1;
		for (int i = 0; i < nodesNum && temp < clusterSum - 1; i++)
			if (tmp[i] != -1) {
				otherClusterNo[++temp] = tmp[i];
				for (int j = i; j < nodesNum; j++)
					if (otherClusterNo[temp] == tmp[j])
						tmp[j] = -1;
			}
		return otherClusterNo;
	}

	// according to the network coordinates, compute the average distance from
	// one certain node to nodes outside the same cluster
	static double interClusterDist(HSH_SimV3 coordsHSH_SimV3,
			int[] nodes2Cluster, int nodeNo) {
		int clusterNo = nodes2Cluster[nodeNo];
		int nodesNum = coordsHSH_SimV3.row;
		int otherClusterNo[] = new int[getClusterSum(nodes2Cluster) - 1];
		otherClusterNo = getOtherClusterNo(nodes2Cluster, clusterNo);

		HSH_SimV3 x = new HSH_SimV3(1, DIMS_NUM);
		for (int j = 0; j < DIMS_NUM; j++)
			x.mat[0][j] = coordsHSH_SimV3.mat[nodeNo][j];
		HSH_SimV3 y = new HSH_SimV3(DIMS_NUM, 1);
		int count;// the quantity of other nodes in "clusterNo" cluster
		double dist_;
		double dist = 1000000;// Infinity

		double testNeg = 0;

		for (int k = 0; k < otherClusterNo.length; k++) {
			count = 0;
			dist_ = 0;
			for (int i = 0; i < nodesNum; i++)
				if (nodes2Cluster[i] == otherClusterNo[k]) {
					for (int j = 0; j < DIMS_NUM; j++) {
						y.mat[j][0] = coordsHSH_SimV3.mat[i][j];
					}
					testNeg = x.HSH_SimV3Multiply(S.HSH_SimV3Multiply(y)).mat[0][0];
					if (testNeg > 0) {
						dist_ += testNeg;
					} else {
						continue;
					}
					count++;
				}
			dist_ /= count;
			dist = dist < dist_ ? dist : dist_;
		}
		return dist;
	}

	/**
	 * distributed version
	 * 
	 * @param hostCoords
	 * @param landmarkCoords
	 * @param hostNodes
	 * @param landmarkNodes
	 * @return
	 * @throws IOException
	 */
	static double[] Silhouette_Coefficient(HSH_SimV3 hostCoords,
			HSH_SimV3 landmarkCoords, int[] hostNodes, int[] landmarkNodes)
			throws IOException {
		int nodesNum = hostNodes.length + landmarkNodes.length;
		HSH_SimV3 coordsHSH_SimV3 = new HSH_SimV3(nodesNum, DIMS_NUM);
		coordsHSH_SimV3 = mergeCoordsHSH_SimV3(hostCoords, landmarkCoords,
				hostNodes, landmarkNodes);

		int nodes2Cluster[] = new int[nodesNum];
		nodes2Cluster = divideCluster(coordsHSH_SimV3);

		logCluster.write("nodes2Cluster : \n");// /////////////////////
		for (int i = 0; i < nodesNum; i++)
			// ////////////////////////////////
			logCluster.write(nodes2Cluster[i] + "\t");// ////////////////
		logCluster.write("\n");// ///////////////////////////////////////
		showClusterInfo(nodes2Cluster);// /////////////////////////////

		double a, b;
		double SC[] = new double[nodesNum];
		for (int nodeNo = 0; nodeNo < nodesNum; nodeNo++) {
			a = intraClusterDist(coordsHSH_SimV3, nodes2Cluster, nodeNo);
			b = interClusterDist(coordsHSH_SimV3, nodes2Cluster, nodeNo);
			// //System.out.println("a = " + a);//////////////////////
			// //System.out.println("b = " + b);////////////////////////
			SC[nodeNo] = -(b - a) / Math.max(a, b);
			System.out.println("SC[" + nodeNo + "] =" + SC[nodeNo]);

		}
		return SC;
	}

	/**
	 * centralized version
	 * 
	 * @param landmarkCoords
	 * @return
	 */
	static double[] Silhouette_Coefficient(HSH_SimV3 landmarkCoords) {
		int nodesNum = landmarkCoords.row;
		int nodes2Cluster[] = new int[nodesNum];
		nodes2Cluster = divideCluster(landmarkCoords);

		System.out.println("nodes2Cluster : ");// /////////////////////
		for (int i = 0; i < nodesNum; i++)
			// ////////////////////////////////
			System.out.print(nodes2Cluster[i] + "\t");// ////////////////
		System.out.println();// ///////////////////////////////////////
		showClusterInfo(nodes2Cluster);// /////////////////////////////

		double a, b;
		double SC[] = new double[nodesNum];
		for (int nodeNo = 0; nodeNo < nodesNum; nodeNo++) {
			a = intraClusterDist(landmarkCoords, nodes2Cluster, nodeNo);
			b = interClusterDist(landmarkCoords, nodes2Cluster, nodeNo);
			// //System.out.println("a = " + a);//////////////////////
			// //System.out.println("b = " + b);////////////////////////
			SC[nodeNo] = (b - a) / Math.max(a, b);
			// //System.out.println("SC[" + nodeNo+ "] =" + SC[nodeNo]);

		}
		return SC;
	}

	// show the given array's Stat. in detail
	static void showArrayStat(double[] array) {
		double maxValue, minValue, averageValue, middleValue;
		double count_LargerThanZero, count_LargerThanPointTwo, count_LargerThanPointFive;
		double percent_LargerThanZero, percent_LargerThanPointTwo, percent_LargerThanPointFive;
		int count = array.length;

		double cp[] = new double[count];
		// copy from array
		System.arraycopy(array, 0, cp, 0, count);
		// sort cp
		java.util.Arrays.sort(cp);

		maxValue = cp[count - 1];
		minValue = cp[0];
		middleValue = cp[count / 2];
		averageValue = 0;
		for (int i = 0; i < count; i++)
			averageValue += cp[i];
		averageValue /= count;

		int index1;
		for (index1 = 0; index1 < count && cp[index1] <= 0; index1++)
			;
		int index2;
		for (index2 = index1; index2 < count && cp[index2] <= .2; index2++)
			;
		int index3;
		for (index3 = index2; index3 < count && cp[index3] <= .5; index3++)
			;
		count_LargerThanZero = count - index1;
		count_LargerThanPointTwo = count - index2;
		count_LargerThanPointFive = count - index3;
		percent_LargerThanZero = 100 * count_LargerThanZero / count;
		percent_LargerThanPointTwo = 100 * count_LargerThanPointTwo / count;
		percent_LargerThanPointFive = 100 * count_LargerThanPointFive / count;

		System.out.println();
		System.out.println("Stat. of the array:");
		System.out.println("    MAX: \t" + maxValue);
		System.out.println("    MIN: \t" + minValue);
		System.out.println("AVERAGE: \t" + averageValue);
		System.out.println(" MIDDLE: \t" + middleValue);
		System.out.println("QUANTITY(>=0.0): \t" + count_LargerThanZero
				+ "\t (" + percent_LargerThanZero + " %)");
		System.out.println("QUANTITY(>=0.2): \t" + count_LargerThanPointTwo
				+ "\t (" + percent_LargerThanPointTwo + " %)");
		System.out.println("QUANTITY(>=0.5): \t" + count_LargerThanPointFive
				+ "\t (" + percent_LargerThanPointFive + " %)");

		return;
	}

	/*
	 * Suppose there are m clusters, the clusters' serial No start from 0 to m-1
	 * Actually, we divide clusters according the DIMS_NUM (viz. m == DIMS)In
	 * the following function we denote m as clusterSum,and the information
	 * about which cluster one certain node belongs to record in the
	 * nodes2Cluster
	 */
	static void showClusterInfo(int[] nodes2Cluster) {
		int nodesNum = nodes2Cluster.length;
		int clusterSum = getClusterSum(nodes2Cluster);

		int count;
		int tmp[] = new int[clusterSum];
		for (int i = 0; i < clusterSum; i++) {
			count = 0;
			for (int j = 0; j < nodesNum; j++)
				if (nodes2Cluster[j] == i)
					count++;
			tmp[i] = count;
		}

		// System.out.println();
		for (int i = 0; i < clusterSum; i++) {
			if (tmp[i] == 0)
				System.out.println("Cluster" + i + " includes 0 node");
			else if (tmp[i] == 1)
				System.out.println("Cluster" + i + " includes 1 node");
			else
				System.out.println("Cluster" + i + " includes " + tmp[i]
						+ " nodes");
		}
		// //System.out.println();

		return;
	}

	public static void main(String[] args) {

		if (args[0].equalsIgnoreCase("0")) {
			try {
				init_SymmetricNMF();
				symmetric_NMF(D);
				// D.CheckError(H.HSH_SimV3Multiply(S.HSH_SimV3Multiply(H.ToRank())));
				showArrayStat(Silhouette_Coefficient(H));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		if (args[0].equalsIgnoreCase("1")) {
			try {
				NMF();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		try {
			logCluster.flush();
			logCluster.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
