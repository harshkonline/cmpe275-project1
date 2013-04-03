/*
 * copyright 2012, gash
 * 
 * Gash licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package poke.server.management;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.GeneratedMessage;

import eye.Comm.Heartbeat;
import eye.Comm.Management;

import poke.monitor.ServerStatus;
import poke.server.Server;

public class ServerHeartbeat extends Thread {
	protected static Logger logger = LoggerFactory.getLogger("management");
	protected static AtomicReference<ServerHeartbeat> instance = new AtomicReference<ServerHeartbeat>();

	static final int sHeartRate = 5000; // msec

	String nodeId;
	ManagementQueue mqueue;
	boolean forever = true;
	HashMap<Channel, HeartData> group = new HashMap<Channel, HeartData>();

	public static ServerHeartbeat getInstance(String id) {
		instance.compareAndSet(null, new ServerHeartbeat(id));
		return instance.get();
	}

	public static ServerHeartbeat getInstance() {
		return instance.get();
	}

	protected ServerHeartbeat(String nodeId) {
		this.nodeId = nodeId;
		// group = new DefaultChannelGroup();
	}

	public void addChannel(String nodeId, Channel ch, SocketAddress sa) {
		if (!group.containsKey(ch)) {
			HeartData heart = new HeartData(nodeId, ch, sa);
			group.put(ch, heart);

			// when the channel closes, remove it from the group
			ch.getCloseFuture().addListener(new CloseHeartListener(heart));
		}
	}

	public void release() {
		forever = true;
	}

	private Management generateHB() {
		Heartbeat.Builder h = Heartbeat.newBuilder();
		h.setTimeRef(System.currentTimeMillis());
		h.setNodeId(nodeId);

		Management.Builder b = Management.newBuilder();
		b.setBeat(h.build());

		return b.build();
	}

	@Override
	public void run() {
		logger.info("starting heartbeat");

		while (forever) {
			try {
				Thread.sleep(sHeartRate);

				// ignore until we have edges with other nodes
				if (group.size() > 0) {
					// TODO verify known node's status

					// send my status (heartbeat)
					GeneratedMessage msg = generateHB();
					for (Channel ch : group.keySet()) {
						if (ch.isOpen()) {
							HeartData hd = group.get(ch);
							
							ch.write(msg, hd.sa);
							Server.availability.put(hd.nodeId, true);
							logger.info("beat: " + hd.nodeId +Server.availability.get(hd.nodeId));
							
						}
					}
				}
			} catch (InterruptedException ie) {
				break;
			} catch (Exception e) {
				logger.error("Unexpected management communcation failure", e);
				break;
			}
		}

		if (!forever) {
			logger.info("management outbound queue closing");
		}
	}

	public static class HeartData {
		public HeartData(String nodeId, Channel channel, SocketAddress sa) {
			this.nodeId = nodeId;
			this.channel = channel;
			this.sa = sa;
		}

		public String nodeId;
		public SocketAddress sa;
		public Channel channel;
	}

	public class CloseHeartListener implements ChannelFutureListener {
		private HeartData heart;
		public CloseHeartListener(HeartData heart) {
			this.heart = heart;
		}

		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			logger.warn("channel closing for node '" + heart.nodeId + "'");
			group.remove(future.getChannel());
			Server.availability.put(heart.nodeId,false);
			
		}
	}
}
