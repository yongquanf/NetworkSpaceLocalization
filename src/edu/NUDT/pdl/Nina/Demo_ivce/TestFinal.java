package edu.NUDT.pdl.Nina.Demo_ivce;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import edu.harvard.syrah.prp.SortedList;

public class TestFinal {

	final String error = "AA";
	final Random r = new Random();
	final B test;

	public TestFinal() {
		test = new B();
	}

	final class B {

		final int a[] = { 1, 3, 4 };
		final Vector val = new Vector(1);

		public void addItems() {
			int a = r.nextInt();
			val.add(a);
		}

		public B() {

		}
	}

	SortedList<Double> tmp = new SortedList<Double>(new DoubleComp());

	class DoubleComp implements Comparator {

		
		public int compare(Object o1, Object o2) {
			// TODO Auto-generated method stub
			double d1 = ((Double) o1).doubleValue();
			double d2 = ((Double) o2).doubleValue();
			if (d1 < d2) {
				return -1;
			} else if (d1 == d2) {
				return 0;
			} else {
				return 1;
			}

		}

	}

	/**
	 * @param args
	 */
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {

		TestFinal test = new TestFinal();
		System.out.println(test.test.val.size());

		test.test.addItems();
		System.out.println(test.test.val.size());
		test.test.addItems();
		System.out.println(test.test.val.size());

		System.out.println(test.test.a[0]);
		test.test.a[0] = 2;
		System.out.println(test.test.a[0]);

		final List ancestors = new ArrayList(1);
		ancestors.add(15);
		ancestors.add(-1);
		ancestors.add(3);
		ancestors.add(2);
		ancestors.add(1);
		final List pointer = ancestors;

		System.out.println(pointer.size());
		System.out.println(pointer.toString());
		List sub = new ArrayList(1);
		sub.addAll(pointer.subList(3, pointer.size()));
		pointer.removeAll(sub);
		System.out.println(pointer.size());
		System.out.println(pointer.toString());
		System.out.println(pointer.get(0));

		Iterator<Integer> ier = pointer.iterator();
		while (ier.hasNext()) {
			test.tmp.add(Double.valueOf(ier.next().intValue()));
		}
		Iterator ier1 = test.tmp.iterator();
		while (ier1.hasNext()) {
			System.out.print(ier1.next() + " ");
		}

		System.out.println(System.getProperty("os.name"));

	}

}
