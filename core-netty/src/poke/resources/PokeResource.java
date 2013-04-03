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
import eye.Comm.Header;
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
		Response reply;	
		// Processing successfully
		ServerConf cf = ResourceFactory.getCfg();
		String str = cf.getServer().getProperty("port");
		
		String visitedPath = request.getHeader().getRoutingPath();
		
			logger.info("----------Server is responding: "
					+ request.getBody().getFinger().getTag());
			
			
				Response.Builder rb = Response.newBuilder();
		
				PayloadReply.Builder pRep = PayloadReply.newBuilder();
				rb.setBody(pRep.build());
				
				Header.Builder hrep = Header.newBuilder();
				hrep.setRoutingPath(visitedPath+"/"+str);
				hrep.setOriginator(request.getHeader().getOriginator());
				hrep.setTag(request.getHeader().getTag());
				hrep.setTime(System.currentTimeMillis());
				hrep.setRoutingId(eye.Comm.Header.Routing.FINGER);
				hrep.setReplyMsg("Data present");
				hrep.setReplyCode(ReplyStatus.SUCCESS);
				hrep.setDestination(str);
				hrep.setMessageType("response");
				hrep.setFirstChannel(request.getHeader().getFirstChannel());
				rb.setHeader(hrep.build());
		
				reply = rb.build();
			
		return reply;
	}

}
