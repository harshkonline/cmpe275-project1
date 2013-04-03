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
package poke.server.routing;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelState;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.GeneratedMessage;

import eye.Comm;
import eye.Comm.Finger;
import eye.Comm.Header;
import eye.Comm.Payload;
import eye.Comm.PayloadReply;
import eye.Comm.Request;
import eye.Comm.Response;
import eye.Comm.Header.ReplyStatus;

import poke.monitor.MonitorPipeline;
import poke.server.Server;
import poke.server.conf.ServerConf;
import poke.server.queue.ChannelQueue;
import poke.server.queue.QueueFactory;
import poke.server.resources.ResourceFactory;
import poke.client.ClientConnection;
import poke.clientServer.ServerConnection;

/**
 * As implemented, this server handler does not share queues or worker threads
 * between connections. A new instance of this class is created for each socket
 * connection.
 * 
 * This approach allows clients to have the potential of an immediate response
 * from the server (no backlog of items in the queue); within the limitations of
 * the VM's thread scheduling. This approach is best suited for a low/fixed
 * number of clients (e.g., infrastructure).
 * 
 * Limitations of this approach is the ability to support many connections. For
 * a design where many connections (short-lived) are needed a shared queue and
 * worker threads is advised (not shown).
 * 
 * @author gash
 * 
 */
public class ServerHandler extends SimpleChannelUpstreamHandler {
	protected static Logger logger = LoggerFactory.getLogger("server");
	private ChannelQueue queue;
	private volatile Channel channel;
	private static Channel myChannel;
	private static Boolean status;
	
	ServerConf cf = ResourceFactory.getCfg();
	String str = cf.getServer().getProperty("port");
	
	public ServerHandler() {
		// logger.info("** ServerHandler created **");
	}

	/**
	 * override this method to provide processing behavior
	 * 
	 * @param msg
	 */

	public boolean send(GeneratedMessage msg) {
		// TODO a queue is needed to prevent overloading of the socket
		// connection. For the demonstration, we don't need it
		ChannelFuture cf = channel.write(msg);
		if (cf.isDone() && !cf.isSuccess()) {
			logger.error("failed to poke!");
			return false;
		}

		return true;
	}

	
	public static Channel getMyChannel() {
		return myChannel;
	}

	public static void setMyChannel(Channel myChannel) {
		ServerHandler.myChannel = myChannel;
	}

	public void handleMessage(eye.Comm.Request request, Channel channel,
			MessageEvent e) {
		
		if (request == null) {
			logger.error("ERROR: Unexpected content - null");
			return;
		}
		// processing is deferred to the worker threads
	
		if (request.getHeader().getMessageType().equals("request")) {
			
			if(request.getHeader().getRoutingPath().length() == 0){
				
				setMyChannel(channel);
				System.out.println("was here");
			}
			
			if(checkIfCanProcess(request)){
				queueInstance(channel).enqueueRequest(request);
			}
			else{
				String visitedPath = request.getHeader().getRoutingPath();
				
				Request.Builder rb = Request.newBuilder();
				
				Finger.Builder f = eye.Comm.Finger.newBuilder();
				f.setTag(request.getHeader().getTag());
				f.setNumber(1);
				
				Payload.Builder pRep = Payload.newBuilder();
				pRep.setFinger(f.build());
				
				rb.setBody(pRep.build());
				
				Header.Builder hrep = Header.newBuilder();
				hrep.setRoutingPath(visitedPath+"/"+str);
				hrep.setOriginator(request.getHeader().getOriginator());
				hrep.setTag(request.getHeader().getTag());
				hrep.setTime(System.currentTimeMillis());
				hrep.setRoutingId(eye.Comm.Header.Routing.FINGER);
				hrep.setReplyMsg(request.getHeader().getReplyMsg());
				hrep.setReplyCode(ReplyStatus.FAILURE);
				hrep.setMessageType("request");
				
				rb.setHeader(hrep.build());

				request = rb.build();
				fwdRequest(request);
			}
		} 
		else {
			
			System.out.println("Routing Finger response: ");
			System.out.println(" - Tag : " + request.getHeader().getTag());
			System.out.println(" - Time : " + request.getHeader().getTime());
			System.out.println(" - Status : " + request.getHeader().getReplyCode());
			System.out.println(" - Type : " + request.getHeader().getMessageType());
			System.out.println(" - Destination: "+request.getHeader().getDestination());
			System.out.println("\nInfo:");
			
			
			eye.Comm.Response response = buildResponse(request);

			if (response.getHeader().getMessageType().equals("response")) {
				String path = response.getHeader().getRoutingPath();
				int pos = path.indexOf(str);
				String temp = path.substring(0, pos - 1);
				int new_pos = temp.lastIndexOf("/");
				String previous = temp.substring(new_pos + 1);
				
				if(null != previous && previous.length() != 0){
					int dPort = Integer.parseInt(previous);
					ServerConnection sc = ServerConnection.initConnection(
							"localhost", dPort);
					sc.fwdResponse(response);
				}
				else{
					getMyChannel().write(response);
				}

			}
		}
	}
	
	public Boolean checkIfCanProcess(eye.Comm.Request request){
		Boolean result = false;
		String visitedPath = request.getHeader().getRoutingPath();
		
		if(!visitedPath.contains(str)){
			if (cf.getServer().getProperty("node.id").equals("three")) {
				result = true;
			}
		}
		return result; 
	}
	
	public void fwdRequest(eye.Comm.Request request){
		if (null != cf.getNodes() && cf.getNodes().size() > 0) {
			Iterator itr = cf.getNodes().iterator();
			Boolean result = false; 
			// broadcast packet to unvisited neighbors
			while (itr.hasNext()) {
				ServerConf.NodeConf node = (ServerConf.NodeConf) itr.next(); 
				String strPort = node.getPort();
				String nodeId = node.getId();

				// Check if adjacent node is visited
				String visitedPath = request.getHeader().getRoutingPath();
				
				if (!visitedPath.contains(strPort)) {
					
					//forward packet if not visited
					int dPort = Integer.parseInt(strPort);
					
					/* circuit breaker to check availability of server */
					for(int count = 0; count<3; count++){
						if(null != Server.availability.get(nodeId) && Server.availability.get(nodeId)){
							result = true;
						}
						else{
							result = false;
						}
					}
					if(result){
						System.out.println("here for "+nodeId);
						ServerConnection cc = ServerConnection
								.initConnection("localhost", dPort);
					
						cc.poke(request);
					}
				}
			}
		}
	}
	
	public eye.Comm.Response buildResponse(Comm.Request request){
		
		Response.Builder rb = Response.newBuilder();
		PayloadReply.Builder pRep = PayloadReply.newBuilder();
		rb.setBody(pRep.build());
		
		Header.Builder hrep = Header.newBuilder();
		hrep.setOriginator(request.getHeader().getOriginator());
		hrep.setTag(request.getHeader().getTag());
		hrep.setTime(request.getHeader().getTime());
		hrep.setRoutingId(request.getHeader().getRoutingId());
		hrep.setReplyMsg(request.getHeader().getReplyMsg());
		hrep.setReplyCode(ReplyStatus.SUCCESS);
		hrep.setDestination(request.getHeader().getDestination());
		hrep.setRoutingPath(request.getHeader().getRoutingPath());
		hrep.setMessageType("response");
		rb.setHeader(hrep.build());
		
		Response reply = rb.build();
		return reply;
	}

	/**
	 * find the queue. Note this cannot return null.
	 * 
	 * @param channel
	 * @return
	 */
	private ChannelQueue queueInstance(Channel channel) {
		// if a single queue is needed, this is where we would obtain a
		// handle to it.

		if (queue != null)
			return queue;
		else {
			queue = QueueFactory.getInstance(channel);

			// on close remove from queue
			channel.getCloseFuture().addListener(
					new ConnectionClosedListener(queue));
		}

		return queue;
	}
	
	@Override
	public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e)
			throws Exception {
		channel = e.getChannel();
		super.channelOpen(ctx, e);
	}

	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e)
			throws Exception {
		if (channel.isConnected())
			channel.write(ChannelBuffers.EMPTY_BUFFER).addListener(
					ChannelFutureListener.CLOSE);
	}
	@Override
	public void channelInterestChanged(ChannelHandlerContext ctx,
			ChannelStateEvent e) throws Exception {
		if (e.getState() == ChannelState.INTEREST_OPS
				&& ((Integer) e.getValue() == Channel.OP_WRITE)
				|| (Integer) e.getValue() == Channel.OP_READ_WRITE)
			logger.warn("channel is not writable! <--------------------------------------------");
	}


	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
		handleMessage((eye.Comm.Request) e.getMessage(), e.getChannel(), e);
	}
	

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
		logger.error(
				"ServerHandler error, closing channel, reason: " + e.getCause(),
				e);
		e.getCause().printStackTrace();
		e.getChannel().close();
	}

	public static class ConnectionClosedListener implements
			ChannelFutureListener {
		private ChannelQueue sq;

		public ConnectionClosedListener(ChannelQueue sq) {
			this.sq = sq;
		}

		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			if (sq != null)
				sq.shutdown(true);
			sq = null;
		}
	}
}
