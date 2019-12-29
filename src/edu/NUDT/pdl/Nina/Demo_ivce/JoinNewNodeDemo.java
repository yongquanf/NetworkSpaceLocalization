package edu.NUDT.pdl.Nina.Demo_ivce;

import java.util.List;

import edu.NUDT.pdl.Nina.Ninaloader;
import edu.NUDT.pdl.Nina.KNN.NodesPair;
import edu.harvard.syrah.prp.Log;
import edu.harvard.syrah.sbon.async.CBResult;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB0;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB1;
import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;

public class JoinNewNodeDemo {

	private static final Log log = new Log(JoinNewNodeDemo.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		String hostname = "";
		int port = Ninaloader.COMM_PORT;

		final boolean[] joined = new boolean[1];

		// join the system
		final Ninaloader instance = new Ninaloader();
		// one instance only
		if (Ninaloader.singletonMode) {
			Ninaloader.getInstance().join(hostname, port, new CB0() {
				@Override
				protected void cb(CBResult result) {
					System.out.println("Join correctly as a singleton!");
					joined[0] = true;
				}

			});
		} else {
			instance.join(hostname, port, new CB0() {
				@Override
				protected void cb(CBResult result) {
					System.out.println("Join correctly as one instance!");
					joined[0] = true;
				}

			});
		}

		// application of network measurements

		// query the KNN
		int k = 5;
		AddressIF target = AddressFactory.createUnresolved(hostname, port);
		instance.NNManager.queryKNearestNeighbors(target, k,
				new CB1<List<NodesPair>>() {

					@Override
					protected void cb(CBResult result, List<NodesPair> KNNs) {
						// TODO Auto-generated method stub
						System.out
								.println("KNN query results are returned, and saved into the Set KNNs");

					}

				});

		// estimate the latency
		String hostA = "";
		String hostB = "";

		AddressIF nodeA = AddressFactory.createUnresolved(hostA, port);
		AddressIF nodeB = AddressFactory.createUnresolved(hostB, port);

		instance.ncManager.estimateRTT(nodeA, nodeB, new CB1<Double>() {

			@Override
			protected void cb(CBResult result, Double latency) {
				// TODO Auto-generated method stub
				System.out.println("Latency query results are returned as: "
						+ latency.doubleValue());

			}

		});

		log.main("Shutdown");
		System.exit(0);
	}

}
