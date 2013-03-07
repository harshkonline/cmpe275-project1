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
package poke.client;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.util.PrintNode;

import com.google.protobuf.GeneratedMessage;

import eye.Comm.Document;
import eye.Comm.Header;
import eye.Comm.NameValueSet;

public class ClientHandler extends SimpleChannelUpstreamHandler {
	protected static Logger logger = LoggerFactory.getLogger("client");

	private volatile Channel channel;

	public ClientHandler() {
	}

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

	public void handleMessage(eye.Comm.Response msg) {
		if (msg.getHeader().getRoutingId() == Header.Routing.FINGER) {
			System.out.println("Finger response: ");
			System.out.println(" - Tag : " + msg.getHeader().getTag());
			System.out.println(" - Time : " + msg.getHeader().getTime());
			System.out.println(" - Status : " + msg.getHeader().getReplyCode());
			System.out.println("\nInfo:");
			printDocument(msg.getBody().getFinger());
		}
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
		handleMessage((eye.Comm.Response) e.getMessage());
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
		System.out.println("ERROR: " + e.getCause());

		// TODO do we really want to do this? try to re-connect?
		e.getChannel().close();
	}

	private void printDocument(Document doc) {
		if (doc == null) {
			System.out.println("document is null");
			return;
		}

		if (doc.hasNameSpace())
			System.out.println("NameSpace: " + doc.getNameSpace());

		if (doc.hasDocument()) {
			NameValueSet nvs = doc.getDocument();
			PrintNode.print(nvs);
		}
	}

}
