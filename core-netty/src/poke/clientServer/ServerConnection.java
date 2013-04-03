package poke.clientServer;

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

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

import org.apache.commons.collections.bag.SynchronizedBag;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.util.internal.ReusableIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.server.resources.ResourceUtil;
import poke.server.routing.ServerDecoderPipeline;
import poke.server.routing.ServerHandler;
import poke.client.ClientDecoderPipeline;
import poke.client.ClientHandler;
import poke.clientServer.RoutingHandler;
import com.google.protobuf.GeneratedMessage;

import eye.Comm.Finger;
import eye.Comm.Header;
import eye.Comm.Payload;
import eye.Comm.Request;
import eye.Comm.Response;
import eye.Comm.Header.ReplyStatus;
/**
 * provides an abstraction of the communication to the remote server.
 * 
 * @author gash
 * 
 */
public class ServerConnection {
	protected static Logger logger = LoggerFactory.getLogger("client");
	static Boolean requestLock = true; 
	private String host;
	private int port;
	private ChannelFuture channel; // do not use directly call connect()!
	private ClientBootstrap bootstrap;
	private LinkedBlockingDeque<com.google.protobuf.GeneratedMessage> outbound;
	private OutboundWorker outWorker;
	
	
	protected ServerConnection(String host, int port) {
		this.host = host;
		this.port = port;

		init();
	}

	/**
	 * release all resources
	 */
	public void release() {
		bootstrap.releaseExternalResources();
	}

	public static ServerConnection initConnection(String host, int port) {

		ServerConnection rtn = new ServerConnection(host, port);
		return rtn;
	}

	public void poke(Request request) {
		
		try {
			// enqueue message
			outbound.put(request);
			System.out.println("Enqueued req");
			
		} catch (InterruptedException e) {
			logger.warn("Unable to deliver message, queuing");
		}
		
		
	}
	
	public void fwdResponse(Response response){
		
		try{
			outbound.put(response);
		}catch(InterruptedException e){
			logger.warn("Unable to deliver message, queuing");
		}
		
	}

	private void init() {
		// the queue to support client-side surging
		outbound = new LinkedBlockingDeque<com.google.protobuf.GeneratedMessage>();
		// Configure the client.
		bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(
				Executors.newCachedThreadPool(),
				Executors.newCachedThreadPool()));

		bootstrap.setOption("connectTimeoutMillis", 10000);
		bootstrap.setOption("tcpNoDelay", true);
		bootstrap.setOption("keepAlive", true);

		// Set up the pipeline factory.
		bootstrap.setPipelineFactory(new ServerDecoderPipeline());
		

		// start outbound message processor
		outWorker = new OutboundWorker(this);
	
		outWorker.start();
		
	}

	/**
	 * create connection to remote server
	 * 
	 * @return
	 */
	protected Channel connect() {
		// Start the connection attempt.
		if (channel == null) {
			// System.out.println("---> connecting");
			channel = bootstrap.connect(new InetSocketAddress(host, port));

			// cleanup on lost connection

		}
		// wait for the connection to establish
		channel.awaitUninterruptibly();

		if (channel.isDone() && channel.isSuccess())
			return channel.getChannel();
		else
			throw new RuntimeException(
					"Not able to establish connection to server "+port);
	}

	/**
	 * queues outgoing messages - this provides surge protection if the client
	 * creates large numbers of messages.
	 * 
	 * @author gash
	 * 
	 */
	protected class OutboundWorker extends Thread {
		ServerConnection conn;
		boolean forever = true;

		public OutboundWorker(ServerConnection conn) {
			this.conn = conn;

			if (conn.outbound == null)
				throw new RuntimeException(
						"connection worker detected null queue");
		}

		@Override
		public void run() {
			Channel ch = conn.connect();
			if (ch == null || !ch.isOpen()) {
				ServerConnection.logger
						.error("connection missing, no outbound communication");
				return;
			}

			while (true) {
				if (!forever && conn.outbound.size() == 0)
					break;

				try {
					// block until a message is enqueued
					GeneratedMessage msg = conn.outbound.take();
					if (ch.isWritable()) {
						 ServerHandler handler = conn.connect().getPipeline()
								.get(ServerHandler.class);

						if (!handler.send(msg))
							conn.outbound.putFirst(msg);

					} else
						conn.outbound.putFirst(msg);
				} catch (InterruptedException ie) {
					break;
				} catch (Exception e) {
					ServerConnection.logger.error(
							"Unexpected communcation failure", e);
					break;
				}
			}

			if (!forever) {
				ServerConnection.logger.info("connection queue closing");
			}
		}
	}
	
}
