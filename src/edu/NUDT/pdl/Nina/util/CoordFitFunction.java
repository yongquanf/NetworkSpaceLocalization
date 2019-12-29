package edu.NUDT.pdl.Nina.util;

import java.util.Vector;

import edu.NUDT.pdl.Nina.StableNC.lib.Coordinate;

public interface CoordFitFunction extends FitFunction {
	double fitFunction(Vector<Coordinate> rawCoordinates,
			int totalCoordinateElements, Vector HelperDis);
}
