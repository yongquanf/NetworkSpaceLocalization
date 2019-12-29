package edu.NUDT.pdl.Nina.Sim.SimCoord;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class testFileDir {
	private static final String DUMP_COORD_PATH = "nc/log-Nina-coords";
	final static SimpleDateFormat dateFormat = new SimpleDateFormat(
			"yyyyMMdd-HHmm");

	public static void main(String[] args) {
		String file = DUMP_COORD_PATH + "/" + dateFormat.format(new Date())
				+ ".coords";
		String currentFile = DUMP_COORD_PATH + "/" + "current.coords";
		System.out.println("Dumping coords to file " + file);

		File coordFile = new File(file);
		try {
			PrintWriter out = new PrintWriter(new BufferedWriter(
					new FileWriter(file)));
			PrintWriter currentOut = new PrintWriter(new BufferedWriter(
					new FileWriter(currentFile)));

			for (int i = 0; i < 100; i++) {
				String line = "Hello world!\n";
				out.write(line);
				currentOut.write(line);
			}

			out.flush();
			out.close();
			currentOut.flush();
			currentOut.close();
			System.out.println("wrote to file " + file);
		} catch (Exception ex) {
			System.err.println("writing " + file + " failed.  Exiting");
		}
	}
}
