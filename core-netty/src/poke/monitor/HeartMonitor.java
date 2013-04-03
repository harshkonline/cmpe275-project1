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
package poke.monitor;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eye.Comm.Management;
import eye.Comm.Network;
import eye.Comm.Network.Action;

public class HeartMonitor {
	protected static Logger logger = LoggerFactory.getLogger("monitor");

	private String host;
	private int port;
	protected ChannelFuture channel; // do not use directly call connect()!
	protected ClientBootstrap bootstrap;

	// protected ChannelFactory cf;

	protected HeartMonitor(String host, int port) {
		this.host = host;
		this.port = port;

		initTCP();
	}

	protected void release() {
		// if (cf != null)
		// cf.releaseExternalResources();
	}

	protected void initUDP() {
		NioDatagramChannelFactory cf = new NioDatagramChannelFactory(
				Executors.newCachedThreadPool());
		ConnectionlessBootstrap bootstrap = new ConnectionlessBootstrap(cf);

		bootstrap.setOption("connectTimeoutMillis", 10000);
		bootstrap.setOption("keepAlive", true);

		// Set up the pipeline factory.
		bootstrap.setPipelineFactory(new MonitorPipeline());
	}

	protected void initTCP() {
		bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(
				Executors.newCachedThreadPool(),
				Executors.newFixedThreadPool(2)));

		bootstrap.setOption("connectTimeoutMillis", 10000);
		bootstrap.setOption("tcpNoDelay", true);
		bootstrap.setOption("keepAlive", true);

		bootstrap.setPipelineFactory(new MonitorPipeline());

	}

	/**
	 * create connection to remote server
	 * 
	 * @return
	 */
	protected Channel connect() {
		// Start the connection attempt.
		if (channel == null) {
			logger.info("connecting");
			channel = bootstrap.connect(new InetSocketAddress(host, port));
		}

		// wait for the connection to establish
		channel.awaitUninterruptibly();

		if (channel.isDone() && channel.isSuccess())
			return channel.getChannel();
		else
			throw new RuntimeException(
					"Not able to establish connection to server");
	}

	protected void waitForever(String nodeId) {
		try {
			Channel ch = connect();
			Network.Builder n = Network.newBuilder();
			n.setNodeId(nodeId);
			n.setAction(Action.NODEJOIN);
			Management.Builder m = Management.newBuilder();
			m.setGraph(n.build());
			ch.write(m.build());
			while (true) {
				Thread.sleep(1000);
			}
		} catch (InterruptedException e) {
			System.out.println("came here");
			e.printStackTrace();
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int mPort = Integer.parseInt(args[1]);
		HeartMonitor hm = new HeartMonitor(args[0], mPort);
		hm.waitForever(args[2]);
	}

}
