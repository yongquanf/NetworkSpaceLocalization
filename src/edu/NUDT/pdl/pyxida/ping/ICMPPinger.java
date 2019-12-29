/*
 *  Copyright 2008 Jonathan Ledlie and Peter Pietzuch
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package edu.NUDT.pdl.pyxida.ping;

import jpcap.JpcapSender;
import jpcap.packet.EthernetPacket;
import jpcap.packet.ICMPPacket;
import jpcap.packet.IPPacket;
import jpcap.packet.Packet;
import edu.NUDT.pdl.Nina.Ninaloader;
import edu.harvard.syrah.prp.ANSI;
import edu.harvard.syrah.prp.Log;
import edu.harvard.syrah.sbon.async.CBResult;
import edu.harvard.syrah.sbon.async.EL;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB0;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB1;
import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;

class ICMPPinger extends JpcapPinger implements PingerIF {
	private static final Log log = new Log(JpcapPinger.class);

	protected static JpcapSender rawSender = null;
	protected static short counter = 1;

	@Override
	public void init(AddressIF defaultPingAddr, final CB0 cbDone) {
		super.init(defaultPingAddr, cbDone);
	}

	// TODO something higher up is asking us to ping 127.0.0.1.
	// It shouldn't be.
	// Hmm. It's actually: Ping to /127.0.1.1:55506 failed on
	// planetlab-4.imperial.ac.uk
	// This address is working from the command line.

	public void ping(AddressIF remoteNode, final CB1<Double> cbPing)
			throws UnsupportedOperationException {
		log.debug("Sending new ping to remoteNode=" + remoteNode
				+ " using ICMP");

		short ident = counter;
		counter++;
		if (counter > 8192)
			counter = 0;

		JpcapPingData pd = addJpcapRequest(ident, remoteNode, cbPing);
		pd.packetType = ICMPPacket.class;

		pd.sendPacket = createICMP(ident, pd.pingAddr);
		boolean usedRawSender = false;
		try {
			// need to change this to sending an ICMP message on a raw socket
			// apparently, the sendPacket function will do that if we set things
			// up differently though.
			if (rawSender == null) {
				rawSender = JpcapSender.openRawSocket();
			}
			// we set the timestamp before sending the packet
			// since it appears we need to do it at the app level :-(
			pd.sendTS = System.currentTimeMillis();

			if (rawSender != null) {
				rawSender.sendPacket(pd.sendPacket);
				usedRawSender = true;
			} else {
				sender.sendPacket(pd.sendPacket);
			}
		} catch (Exception e) {
			log.warn("Failed sending packet: " + e + " " + e.getMessage());
		}

		ICMPPacket icmp = (ICMPPacket) pd.sendPacket;
		// String data = new String (icmp.data);

		log.debug("SENT seq=" + icmp.seq + " id=" + icmp.id + " orig="
				+ icmp.orig_timestamp + " trans=" + icmp.trans_timestamp
				+ " recv=" + icmp.recv_timestamp);

		// pd.sendTS = System.nanoTime() / 1000;

		log.debug("sendPacket [raw=" + usedRawSender + "]=" + pd.sendPacket
				+ " sendTS=" + pd.sendTS);
		assert pd.sendTS > 0;
	}

	public void ping(AddressIF nodeA, AddressIF nodeB, CB1<Double> cbPing)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	private ICMPPacket createICMP(short ident, AddressIF addr) {
		assert device != null;

		log.debug("Creating ICMP ping to " + addr);

		/*
		 * Ledlie attempted using ICMP_TSTAMP so as to have
		 * recv/orig/trans_timestamp set, but this didn't occur.
		 */

		ICMPPacket icmp = new ICMPPacket();
		icmp.type = ICMPPacket.ICMP_ECHO;

		// We are not getting back ICMP_TSTAMPREPLYs
		// when sending TSTAMPs, nor are the timestamps being set :-(
		// icmp.type = ICMPPacket.ICMP_TSTAMP;

		// icmp.seq = (short)0;
		// icmp.seq = (short)0;
		// icmp.id = ident;

		icmp.seq = ident;
		icmp.id = (short) (Ninaloader.random.nextInt(8192));

		// This does not work :-(
		// icmp.orig_timestamp = (int) System.currentTimeMillis();

		icmp
				.setIPv4Parameter(0, false, false, false, 0, false, false,
						false, 0, 0, 100, IPPacket.IPPROTO_ICMP, thisIP, addr
								.getInetAddress());

		// icmp.data = ("data-"+ident).getBytes();

		// Weird. When this is longer, we tend to get more responses.
		// Ping has a 64 byte payload.
		icmp.data = ("datadatadatadatadatadatadatadatadatadatadatadatadatadata")
				.getBytes();

		EthernetPacket ether = new EthernetPacket();
		ether.frametype = EthernetPacket.ETHERTYPE_IP;
		ether.src_mac = device.mac_address;
		ether.dst_mac = gwMAC;
		icmp.datalink = ether;

		log.debug("Ethernet header for this ICMP ping is " + ether.toString());

		return icmp;
	}

	@Override
	protected long parsePacket(short ident, Packet p) {
		log.debug("p=" + p + " p.class=" + (p != null ? p.getClass() : null));

		long recvTS = Long.MIN_VALUE;
		ICMPPacket icmp = (ICMPPacket) p;

		if (icmp == null) {
			log.debug("Timeout");
			return recvTS;
		}

		switch (icmp.type) {
		case ICMPPacket.ICMP_TIMXCEED:
			icmp.src_ip.getHostName();
			log.debug("ICMP_TIMXCEED: " + icmp.src_ip.toString());
			break;
		case ICMPPacket.ICMP_UNREACH:
			icmp.src_ip.getHostName();
			log.debug("ICMP_UNREACH: " + icmp.src_ip.toString());
			break;
		case ICMPPacket.ICMP_ECHOREPLY:
			long sec = p.sec;
			long usec = p.usec;
			log.debug("ICMP_ECHOREPLY: " + icmp.src_ip + " sec=" + sec
					+ " usec=" + usec);
			// recvTS = (sec * 1000 * 1000) + usec;
			// convert to milliseconds
			recvTS = (sec * 1000) + (usec / 1000);
			log.debug("recvTS=" + recvTS);
			break;
		case ICMPPacket.ICMP_TSTAMPREPLY:
			log.debug("got TSTAMPREPLY");
			break;
		case ICMPPacket.ICMP_TSTAMP:
			log.debug("got TSTAMP");
			break;
		case ICMPPacket.ICMP_ECHO:
			log.debug("got ECHO");
			break;
		default:
			log.debug("got " + icmp.type);
			break;
		}

		// String data = new String (icmp.data);

		log.debug("Finished parseICMP icmp=" + icmp.toString());
		log.debug("RECV seq= " + icmp.seq + " id=" + icmp.id + " orig="
				+ icmp.orig_timestamp + " trans=" + icmp.trans_timestamp
				+ " recv=" + icmp.recv_timestamp);

		/*
		 * if (icmp.id != ident) { log.warn
		 * ("RECV icmp.id="+icmp.id+"!=ident="+ident); return 0; }
		 */

		return recvTS;
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
			System.out.println("usage: ICMPPinger <hostname>");
			System.exit(1);
		}

		final PingerIF pinger = new ICMPPinger();

		final String hostname = args[0];

		EL.get().registerTimerCB(new CB0() {
			@Override
			protected void cb(CBResult resultOK) {
				AddressFactory.createResolved("wiki.pdl", new CB1<AddressIF>() {
					@Override
					protected void cb(CBResult result, AddressIF defaultAddr) {
						switch (result.state) {
						case OK: {
							pinger.init(defaultAddr, new CB0() {
								@Override
								protected void cb(CBResult arg0) {
									AddressFactory.createResolved(hostname,
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
																		+ " using ICMP");
														pinger.testPing(addr);
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
