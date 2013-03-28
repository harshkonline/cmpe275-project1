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
package poke.resources;

import java.util.Iterator;

import org.jboss.netty.bootstrap.Bootstrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.server.resources.Resource;
import poke.server.resources.ResourceFactory;
import poke.server.resources.ResourceUtil;
import eye.Comm.Header.ReplyStatus;
import eye.Comm.Document;
import eye.Comm.Finger;
import eye.Comm.Payload;
import eye.Comm.PayloadReply;
import eye.Comm.PayloadReplyOrBuilder;
import eye.Comm.Request;
import eye.Comm.Response;
import poke.server.conf.ServerConf;
import poke.client.ClientConnection;
import poke.clientServer.ServerConnection;

public class PokeResource implements Resource {
	protected static Logger logger = LoggerFactory.getLogger("server");

	public PokeResource() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see poke.server.resources.Resource#process(eye.Comm.Finger)
	 */
	public Response process(Request request) {
		// TODO add code to process the message/event received

		ServerConf cf = ResourceFactory.getCfg();
		String str = cf.getServer().getProperty("port");
		int port = Integer.parseInt(str);
		Response reply = null;

	/*if (port == 5580) {
			logger.info("poke: " + request.getBody().getFinger().getTag());
			Response.Builder r = Response.newBuilder();
			r.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
					ReplyStatus.SUCCESS, null,"processing"));
			PayloadReply.Builder pr = PayloadReply.newBuilder();
			r.setBody(pr.build());
			reply = r.build();
		}*/
	/*else
	{*/
		String visitedPath = request.getHeader().getRoutingPath();
		System.out.println("PATH "+request.getHeader().getRoutingPath());
		visitedPath = visitedPath + "/" + str;
		Request.Builder reqB = Request.newBuilder();
		reqB.setHeader(ResourceUtil.buildHeaderFrom(
				request.getHeader(), ReplyStatus.SUCCESS, null,visitedPath));
		logger.info("poke: " + request.getBody().getFinger().getTag()+" Path:"+visitedPath);
			if (null != cf.getNodes() && cf.getNodes().size() > 0) {
				Iterator itr = cf.getNodes().iterator();

				// broadcast packet to unvisited neighbors
				while (itr.hasNext() ) {
					String strPort = ((ServerConf.NodeConf) itr.next())
							.getPort();
					
					// check if visited
					
					if (!visitedPath.contains(strPort)) {
						int dPort = Integer.parseInt(strPort);
						ServerConnection cc = ServerConnection.initConnection(
								"localhost", dPort);
						
						int count = 1;
						Request req;
						
						req = cc.poke(request.getBody().getFinger().getTag(), count);
						Response.Builder r = Response.newBuilder();
						r.setHeader(ResourceUtil.buildHeaderFrom(
								req.getHeader(), ReplyStatus.SUCCESS, null,visitedPath));
						PayloadReply.Builder pr = PayloadReply.newBuilder();
						r.setBody(pr.build());
						reply = r.build();
					}
					
				}
			}
//	}	

		return reply;
	}
	
}
