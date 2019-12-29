package edu.NUDT.pdl.Nina.Sim.SimCoord;

import java.io.File;

import edu.NUDT.pdl.Nina.StableNC.lib.GNP;

public class ConvergenceTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		GNP gnp = new GNP();
		gnp.initParams("GNP_Params");
		// gnp.initFromFullMatrix(new File(args[0]), 12,12,1);
		// gnp.initFromFullMatrix(new File(args[0]), 20, 20,1);
		int landmarks = Integer.parseInt(args[1]);
		int matrixType = Integer.parseInt(args[2]);
		gnp.testDim(args[0], landmarks, landmarks, matrixType);
	}

}
