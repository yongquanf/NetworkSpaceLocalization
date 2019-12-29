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
package edu.harvard.syrah.pyxida.ping;

import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import jpcap.JpcapCaptor;
import jpcap.JpcapSender;
import jpcap.NetworkInterface;
import jpcap.NetworkInterfaceAddress;
import jpcap.PacketReceiver;
import jpcap.packet.EthernetPacket;
import jpcap.packet.ICMPPacket;
import jpcap.packet.IPPacket;
import jpcap.packet.Packet;
import edu.harvard.syrah.prp.Log;
import edu.harvard.syrah.prp.POut;
import edu.harvard.syrah.sbon.async.CBResult;
import edu.harvard.syrah.sbon.async.EL;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB0;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB1;
import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;

/**
 * This is the parent class of the Pingers that use the JPCap library
 * 
 * @author peter
 * 
 */
abstract class JpcapPinger extends Pinger implements PingerIF {
	private static final Log log = new Log(JpcapPinger.class);

	private static final long CAPTION_TIMEOUT = -1;
	protected static final long PING_TIMEOUT = 3000;

	// protected static final int PING_SRC_PORT = 55000;

	private static JpcapCaptor captor;
	protected static JpcapSender sender;
	protected static NetworkInterface device;

	protected static InetAddress thisIP;
	protected static byte[] gwMAC;

	protected static Thread jpcapThread;

	protected CB0 cbTimeout;

	class JpcapPingData extends PingData {
		long sendTS;
		long recvTS;
		Packet sendPacket;
		Packet recvPacket;
		Class packetType;
		CB0 cbDone;
	}

	private static List<JpcapPingData> currentPingList = Collections
			.synchronizedList(new ArrayList<JpcapPingData>());

	public void init(AddressIF defaultPingAddr, final CB0 cbDone) {
		this.localAddr = AddressFactory.createLocalAddress();
		this.defaultPingAddr = defaultPingAddr;

		// Are we already initialised?
		if (jpcapThread != null)
			cbDone.callOK();

		jpcapThread = new Thread() {
			@Override
			public void run() {

				NetworkInterface[] devices = JpcapCaptor.getDeviceList();
				// log.debug("devices=" + POut.toString(devices));

				if (devices.length == 0)
					log.error("This must be run as root.");

				nextDevice: for (int i = 0; i <= devices.length; i++) {
					log.debug("Opening new device " + devices[i].name);
					try {
						// TODO The caption timeout is being ignored here, we
						// need a different solution
						captor = JpcapCaptor.openDevice(devices[i], 2000,
								false, 5000);
					} catch (IOException e) {
						log.debug("Could not open interface no " + i
								+ ". Trying next one");
						continue;
					}

					device = devices[i];
					log.debug("Device " + i + " opened successfully.");

					if (device.loopback) {
						log.debug("Ignoring loopback device.");
						// captor.close();
						continue;
					}

					for (NetworkInterfaceAddress addr : device.addresses)
						if (addr.address instanceof Inet4Address) {
							thisIP = addr.address;
							log.debug("thisIP=" + thisIP);
							break;
						}

					if (thisIP == null) {
						log.debug("Local addr not found");
						// captor.close();
						continue;
					}

					// Obtain MAC address of the default gateway
					InetAddress pingAddr = JpcapPinger.this.defaultPingAddr
							.getInetAddress();
					log.debug("defaultPingAddr=" + pingAddr);
					try {
						captor.setFilter("icmp and dst host "
								+ pingAddr.getHostAddress(), false);
					} catch (IOException ex) {
						log.error("Could not set captor filter: " + ex);
					}
					InputStream is = null;
					Packet ping = null;
					while (true) {
						// TODO this is ugly -- is there a better way to obtain
						// the MAC address?
						try {
							log.debug("Trying to open web connection");
							pingAddr.isReachable(1);
							log.debug("Waiting for packet...");
							ping = captor.getPacket();

						} catch (MalformedURLException e) {
							log.error(e.toString());
						} catch (IOException e) {
							log.error(e.toString());
						}
						log.debug("Got one: " + ping);

						if (ping == null) {
							log
									.warn("Cannot obtain MAC address of default gateway.");
							// captor.close();
							continue nextDevice;
						} else if (Arrays.equals(
								((EthernetPacket) ping.datalink).dst_mac,
								device.mac_address)) {
							log.debug("doing it again");
							continue;
						}
						gwMAC = ((EthernetPacket) ping.datalink).dst_mac;
						log.debug("gwMAC=" + POut.toString(gwMAC));
						break;
					}
					break;
				}

				sender = captor.getJpcapSenderInstance();

				/*
				 * captor.setFilter("icmp and dst host " +
				 * thisIP.getHostAddress(), true);
				 * sendICMP(ICMPPinger.this.defaultPingAddr); receiveICMP();
				 */

				log.debug("JpcapPinger initalised.");

				EL.get().registerTimerCB(cbDone);

				JpcapPinger.this.run();
			}
		};
		jpcapThread.start();
	}

	public void shutdown() {
		shutdown = true;
		jpcapThread.interrupt();
	}

	protected void addRequest(JpcapPingData pd) {
	}

	protected void removeRequest(JpcapPingData pd) {
		boolean found = currentPingList.remove(pd);
		assert found : "PingData pd=" + pd + " not found in list: "
				+ currentPingList;
	}

	public void run() {
		try {
			// captor.setFilter("dst host " + thisIP.getHostAddress() +
			// " and (icmp or (tcp and tcp[tcpflags] & (tcp-syn|tcp-ack) != 0))",
			// true);

			captor.setFilter("dst host " + thisIP.getHostAddress()
					+ " and icmp and icmp[icmptype] == icmp-echoreply", false);

			// captor.setFilter("icmp", true);
		} catch (IOException ex) {
			log.error("Could not set captor filter: " + ex);
		}

		// captor.setNonBlockingMode (true);

		// It appears that the handler is only called so fast
		// and that this misses some packets if we aren't ready.
		// We try to only receive packets we might be interested in
		// with a stricter filter above :-(

		captor.setPacketReadTimeout(0);
		PingPacketReceiver handler = new PingPacketReceiver();
		// captor.processPacket (0, handler);
		captor.loopPacket(-1, handler);

		log.error("exited loop packet handler");
		// while (true) { }

	}

	class PingPacketReceiver implements PacketReceiver {

		public void receivePacket(final Packet packet) {

			log.debug("in receivePacket packet=" + packet);

			if (packet != null && packet instanceof ICMPPacket) {
				IPPacket ip = (IPPacket) packet;

				if (ip.src_ip != null) {
					log.debug("Captured a packet from src=" + ip.src_ip);

					synchronized (currentPingList) {
						boolean foundMatch = false;
						for (Iterator<JpcapPingData> it = currentPingList
								.iterator(); it.hasNext();) {

							// Note that this is not very robust:
							// we will ignore both packets if they cross in
							// flight
							// and we receive one ident before the other.
							// That seems better than wrongly assuming low or
							// high latency.
							// A perhaps better approach would be to parse the
							// packet first,
							// extract the ident and the timestamps, and then
							// match
							// it up with the right pending request to this
							// node.
							// However, parsing outside of the handler thread
							// seems like the right idea.

							JpcapPingData currentPing = it.next();
							if (ip.getClass() == currentPing.packetType
									&& ip.src_ip.equals(currentPing.pingAddr
											.getInetAddress())) {
								it.remove();
								currentPing.recvPacket = ip;
								log.debug("Found a matching ping request");
								foundMatch = true;
								EL.get().registerTimerCB(currentPing.cbDone);
							}
						}
					}
				}
			}
		}
	}

	/*
	 * public void run() { try { //captor.setFilter("dst host " +
	 * thisIP.getHostAddress() +
	 * " and (icmp or (tcp and tcp[tcpflags] & (tcp-syn|tcp-ack) != 0))", true);
	 * //captor.setFilter("dst host " + thisIP.getHostAddress() + " and icmp",
	 * true); captor.setFilter("icmp", false);
	 * 
	 * 
	 * //captor.setFilter("icmp", true); } catch (IOException ex) { log.error
	 * ("Could not set captor filter: "+ex); }
	 * 
	 * while (!jpcapThread.isInterrupted()) { IPPacket ip = null; while(true) {
	 * // TODO is this a busy wait?
	 * 
	 * log.debug("Waiting for a packet..."); ip = (IPPacket) captor.getPacket();
	 * if (ip != null && ip.src_ip != null) {
	 * log.debug("Captured a packet from src=" + ip.src_ip);
	 * 
	 * synchronized(currentPingList) { boolean foundMatch = false; for
	 * (Iterator<JpcapPingData> it = currentPingList.iterator(); it.hasNext();)
	 * {
	 * 
	 * // Note that this is not very robust: // we will ignore both packets if
	 * they cross in flight // and we receive one ident before the other. //
	 * That seems better than wrongly assuming low or high latency. // A perhaps
	 * better approach would be to parse the packet first, // extract the ident
	 * and the timestamps, and then match // it up with the right pending
	 * request to this node.
	 * 
	 * JpcapPingData currentPing = it.next(); if (ip.getClass() ==
	 * currentPing.packetType &&
	 * ip.src_ip.equals(currentPing.pingAddr.getInetAddress())) { it.remove();
	 * currentPing.recvPacket = ip; log.debug("Found a matching ping request");
	 * foundMatch = true; EL.get().registerTimerCB(currentPing.cbDone); } }
	 * 
	 * if (!foundMatch) {
	 * 
	 * // Our own ECHO requests land here on their way out to the wire. // so no
	 * need to parse them.
	 * 
	 * log.debug("Did not find a matching ping request"); if (ip instanceof
	 * ICMPPacket) { parsePacket((short)0, ip); } }
	 * 
	 * }
	 * 
	 * } else { log.warn("Captured a null packet"); }
	 * 
	 * } } }
	 */

	public JpcapPingData addJpcapRequest(final short ident,
			final AddressIF remoteNode, final CB1<Double> cbPing) {
		final JpcapPingData pd = new JpcapPingData();
		pd.pingAddr = remoteNode;

		pd.cbDone = new CB0(PING_TIMEOUT) {
			@Override
			protected void cb(CBResult result) {
				switch (result.state) {
				case OK: {
					log.debug("pd.recvPacket=" + pd.recvPacket);
					pd.recvTS = parsePacket(ident, pd.recvPacket);
					// returns 0 if idents do not match
					if (pd.recvTS > 0) {
						assert pd.sendTS > 0;

						long lat = pd.recvTS - pd.sendTS;

						// Doing a little fudging because kernel can timestamp
						// ping receipts from nodes in the same cluster
						// a millisecond or two earlier than we stamped the
						// send on the way out.

						if (lat < -3) {
							String msg = "Ignoring latency node=" + remoteNode
									+ " recvTS=" + pd.recvTS + " sendTS="
									+ pd.sendTS;
							log.warn(msg);
							cbPing.call(CBResult.ERROR("Invalid latency: "
									+ msg), 0.);
							return;
						}

						if (lat >= -3 && lat <= 0) {
							// TODO check that in same subnet
							log.info("Nearby node negative latency="
									+ remoteNode + " recvTS=" + pd.recvTS
									+ " sendTS=" + pd.sendTS);
							lat = 1;
						}

						log.debug("node=" + remoteNode + " lat=" + lat
								+ " pd.recvTS=" + pd.recvTS);
						// cbPing.call(result, lat / 1000.0);
						// already in milliseconds
						cbPing.call(result, (double) lat);
					} else {
						log.debug("ignoring negative recvTS=" + pd.recvTS);
					}
					break;
				}
				case TIMEOUT:
					log.warn("timeout ident=" + ident);
					removeRequest(pd);
					cbPing.call(result, -2.0);
					break;
				case ERROR: {
					// would be nice to use this to track loss
					//mark the loss as -1
					log.warn("error ident=" + ident);
					removeRequest(pd);
					cbPing.call(result, -1.0);
					break;
				}
				}
			}
		};

		currentPingList.add(pd);
		return pd;
	}

	protected abstract long parsePacket(short ident, Packet p);

}
