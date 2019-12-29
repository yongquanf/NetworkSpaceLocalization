package edu.NUDT.pdl.Nina.api;

import java.util.LinkedList;
import java.util.List;

import edu.NUDT.pdl.Nina.KNN.AbstractNNSearchManager;
import edu.NUDT.pdl.Nina.StableNC.nc.StableManager;
import edu.NUDT.pdl.Nina.net.appPing.PingNodes;
import edu.harvard.syrah.prp.Log;
import edu.harvard.syrah.sbon.async.CBResult;
import edu.harvard.syrah.sbon.async.LoopIt;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB0;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB2;

public class APIManager implements Runnable{
	private static final Log log = new Log(APIManager.class);

	private ExternalAPI api;

	private StableManager ncManager;

	public List<APIInstanceIF> apiInstances = new LinkedList<APIInstanceIF>();

	public APIManager(StableManager ncManager, AbstractNNSearchManager NNManager, PingNodes _pingAllPairs) {
		this.ncManager = ncManager;
		this.api = new ExternalAPI(ncManager, NNManager,_pingAllPairs);

		/*
		 * Initialise the different APIs One day this could be done dynamically
		 */
		apiInstances.add(new XMLRPC_API(api));
	}

	public void init(CB0 cbDone) {

		new LoopIt<APIInstanceIF>(apiInstances, new CB2<APIInstanceIF, CB0>() {
			
			protected void cb(CBResult result, APIInstanceIF externalAPI,
					CB0 cbNextIter) {
				log.debug("Initialising externalAPI=" + externalAPI.getClass());
				externalAPI.init(cbNextIter);
			}
		}).execute(cbDone);
	}

	
	public void run() {
		// TODO Auto-generated method stub
		init(new CB0(){

			
			protected void cb(CBResult result) {
				// TODO Auto-generated method stub
				log.info("init the API manager");
			}
			
			
		});
		
	}

}
