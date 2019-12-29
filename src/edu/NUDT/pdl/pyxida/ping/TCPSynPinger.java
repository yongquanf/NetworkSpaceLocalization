/*
 * Copyright 2008 Jonathan Ledlie and Peter Pietzuch
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.NUDT.pdl.pyxida.ping;

import jpcap.packet.EthernetPacket;
import jpcap.packet.IPPacket;
import jpcap.packet.Packet;
import jpcap.packet.TCPPacket;
import edu.NUDT.pdl.Nina.Ninaloader;
import edu.harvard.syrah.prp.ANSI;
import edu.harvard.syrah.prp.Log;
import edu.harvard.syrah.prp.PUtil;
import edu.harvard.syrah.sbon.async.CBResult;
import edu.harvard.syrah.sbon.async.EL;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB0;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB1;
import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;

class TCPSynPinger extends JpcapPinger implements PingerIF {
	private static final Log log = new Log(TCPSynPinger.class);

	protected static final int PING_DST_PORT = 80;

	@Override
	public void init(AddressIF defaultPingAddr, final CB0 cbDone) {
		super.init(defaultPingAddr, cbDone);
	}

	public void ping(AddressIF remoteNode, final CB1<Double> cbPing)
			throws UnsupportedOperationException {
		log.debug("Sending new ping to remoteNode=" + remoteNode + ":"
				+ PING_DST_PORT + " using TCPSyn");
		JpcapPingData pd = addJpcapRequest((short) 0, remoteNode, cbPing);
		pd.packetType = TCPPacket.class;
		sendTCPSyn(pd);

		/*
		 * We don't need to send a rst here because the kernel does it for us.
		 */
	}

	private void sendTCPSyn(JpcapPingData pd) {
		log.debug("Sending TCP syn");

		TCPPacket tcp = new TCPPacket(PUtil.getRandomInt() % 65535,
				PING_DST_PORT, 0, 0, false, false, false, false, true, false,
				false, false, 23360, 0);
		tcp.setIPv4Parameter(0, false, false, false, 0, false, false, false, 0,
				0, 100, IPPacket.IPPROTO_TCP, thisIP, pd.pingAddr
						.getInetAddress());
		tcp.data = new byte[0];

		EthernetPacket ether = new EthernetPacket();
		ether.frametype = EthernetPacket.ETHERTYPE_IP;
		ether.src_mac = device.mac_address;
		ether.dst_mac = gwMAC;
		tcp.datalink = ether;

		pd.sendTS = System.nanoTime() / 1000;
		sender.sendPacket(tcp);

		pd.sendPacket = tcp;

		log.debug("Done: sendPacket=" + pd.sendPacket + " sendTS=" + pd.sendTS);
		assert pd.sendTS > 0;
	}

	/*
	 * private void sendTCPRst(JpcapPingData pd) { assert false;
	 * log.debug("Sending TCP rst"); TCPPacket tcp = new TCPPacket(0,
	 * PING_DST_PORT, 0, 0, false, false, false, true, false, false, true, true,
	 * 23360, 10); tcp.setIPv4Parameter(0, false, false, false, 0, false, false,
	 * false, 0, 0, 100, IPPacket.IPPROTO_TCP, thisIP,
	 * pd.pingAddr.getInetAddress()); tcp.data = new byte[0];
	 * 
	 * EthernetPacket ether = new EthernetPacket(); ether.frametype =
	 * EthernetPacket.ETHERTYPE_IP; ether.src_mac = device.mac_address;
	 * ether.dst_mac = gwMAC; tcp.datalink = ether;
	 * 
	 * sender.sendPacket(tcp); log.debug("Done."); }
	 */

	@Override
	protected long parsePacket(short ident, Packet p) {
		log.debug("p=" + p + " p.class=" + (p != null ? p.getClass() : null));

		long recvTS = Long.MIN_VALUE;
		TCPPacket tcp = (TCPPacket) p;

		long sec = p.sec;
		long usec = p.usec;
		log.debug("ICMP_ECHOREPLY: " + tcp.src_ip + " sec=" + sec + " usec="
				+ usec);
		recvTS = (sec * 1000 * 1000) + usec;
		log.debug("recvTS=" + recvTS);

		return recvTS;
	}

	public void ping(AddressIF nodeA, AddressIF nodeB, CB1<Double> cbPing)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	public static void main(String[] args) {
		ANSI.use(true);

		if (args.length > 0 && args[0].equals("-d")) {
			Log.setPackageRoot(Ninaloader.class);
			String[] newArgs = new String[args.length - 1];
			for (int i = 0; i < newArgs.length; i++)
				newArgs[i] = args[i + 1];
			args = newArgs;
		}

		EL.set(new EL());

		if (args.length == 0) {
			System.out.println("usage: TCPSynPinger <hostname>");
			System.exit(1);
		}

		final String hostname = args[0];

		final PingerIF pinger = new TCPSynPinger();

		EL.get().registerTimerCB(new CB0() {
			@Override
			protected void cb(CBResult resultOK) {
				AddressFactory.createResolved("www.google.com",
						new CB1<AddressIF>() {
							@Override
							protected void cb(CBResult result,
									AddressIF defaultAddr) {
								switch (result.state) {
								case OK: {
									pinger.init(defaultAddr, new CB0() {
										@Override
										protected void cb(CBResult arg0) {
											AddressFactory.createResolved(
													hostname,
													new CB1<AddressIF>() {
														@Override
														protected void cb(
																CBResult result,
																AddressIF addr) {
															switch (result.state) {
															case OK: {
																System.out
																		.println("Pinging addr="
																				+ addr
																				+ ":"
																				+ PING_DST_PORT
																				+ " using TCPSyn");
																pinger
																		.testPing(addr);
																break;
															}
															case ERROR:
															case TIMEOUT: {
																log
																		.error(result.what);
															}
															}
														}
													});
										}
									});
									break;
								}
								case ERROR:
								case TIMEOUT: {
									log.error(result.what);
								}
								}
							}
						});
			}
		});
		EL.get().main();
	}

}
