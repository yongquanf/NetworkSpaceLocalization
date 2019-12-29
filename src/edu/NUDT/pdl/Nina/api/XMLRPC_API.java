package edu.NUDT.pdl.Nina.api;

import edu.NUDT.pdl.Nina.Ninaloader;
import edu.harvard.syrah.prp.Log;
import edu.harvard.syrah.sbon.async.Config;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB0;
import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.xmlrpc.XMLRPCComm;
import edu.harvard.syrah.sbon.async.comm.xmlrpc.XMLRPCCommIF;

public class XMLRPC_API implements APIInstanceIF {
	private static final Log log = new Log(XMLRPC_API.class);

	private static final String XMLRPC_OBJECT_NAME = "Nina";

	static final int API_PORT = Integer.parseInt(Config.getConfigProps()
			.getProperty("api.port", "55501"));

	// App comm interface
	public XMLRPCCommIF apiComm;

	private ExternalAPI api;

	XMLRPC_API(ExternalAPI api) {
		this.api = api;
	}

	public void init(CB0 cbDone) {
		apiComm = new XMLRPCComm();
		log.debug("Using port=" + API_PORT);

		AddressIF apiAddress = AddressFactory.create(Ninaloader.me, API_PORT); //
		// = AddressFactory.createServer(API_PORT);

		apiComm.registerXMLRPCHandler(XMLRPC_OBJECT_NAME, api);
		apiComm.registerHandler("/", api);

		apiComm.initServer(apiAddress, cbDone);
	}

}
