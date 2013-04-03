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
package poke.server.conf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Routing information for the server - internal use only
 * 
 * TODO refactor StorageEntry to be neutral for cache, file, and db
 * 
 * @author gash
 * 
 */
@XmlRootElement(name = "conf")
@XmlAccessorType(XmlAccessType.FIELD)
public class ServerConf {
	private GeneralConf server;
	private List<ResourceConf> routing;
	private List<NodeConf> nodes;
	
	@XmlRootElement(name = "entry")
	@XmlAccessorType(XmlAccessType.FIELD)
	public static final class NodeConf {
		private String id;
		private String port;
		private String mgmt;
		private String storage;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getPort() {
			return port;
		}

		public void setPort(String port) {
			this.port = port;
		}

		public String getMgmt() {
			return mgmt;
		}

		public void setMgmt(String mgmt) {
			this.mgmt = mgmt;
		}

		public String getStorage() {
			return storage;
		}

		public void setStorage(String storage) {
			this.storage = storage;
		}

		public NodeConf(String id, String port, String mgmt, String storage) {
			this.id = id;
			this.port = port;
			this.mgmt = mgmt;
			this.storage = storage;
		}

		public NodeConf() {

		}

	}

	public List<NodeConf> getNodes() {
		return nodes;
	}

	public void setNodes(List<NodeConf> nodes) {
		this.nodes = nodes;
	}
	public NodeConf findById(String id) {
		return nodeMap().get(id);
	}

	private HashMap<String, NodeConf> idToNode;
	private HashMap<String,NodeConf> nodeMap() {
		if(idToNode !=null)
			return idToNode;
		if(idToNode==null){
			synchronized (this) {
				if(idToNode==null){
					idToNode = new HashMap<String, ServerConf.NodeConf>();
					if(nodes !=null){
						for(NodeConf entry: nodes){
							idToNode.put(entry.id, entry);
						}
					}
				}
			}
		}
		return idToNode;
	}


	private volatile HashMap<Integer, ResourceConf> idToRsc;

	private HashMap<Integer, ResourceConf> asMap() {
		if (idToRsc != null)
			return idToRsc;

		if (idToRsc == null) {
			synchronized (this) {
				if (idToRsc == null) {
					idToRsc = new HashMap<Integer, ResourceConf>();
					if (routing != null) {
						for (ResourceConf entry : routing) {
							idToRsc.put(entry.id, entry);
						}
					}
				}
			}
		}

		return idToRsc;
	}

	public void addGeneral(String name, String value) {
		if (server == null)
			server = new GeneralConf();

		server.add(name, value);
	}

	public GeneralConf getServer() {
		return server;
	}

	public void setServer(GeneralConf server) {
		this.server = server;
	}

	public void addResource(ResourceConf entry) {
		if (entry == null)
			return;
		else if (routing == null)
			routing = new ArrayList<ResourceConf>();

		routing.add(entry);
	}

	public ResourceConf findById(int id) {
		return asMap().get(id);
	}

	public List<ResourceConf> getRouting() {
		return routing;
	}

	public void setRouting(List<ResourceConf> conf) {
		this.routing = conf;
	}

	/**
	 * storage setup and configuration
	 * 
	 * @author gash1
	 * 
	 */
	@XmlRootElement(name = "general")
	@XmlAccessorType(XmlAccessType.FIELD)
	public static final class GeneralConf {
		private TreeMap<String, String> general;

		public String getProperty(String name) {
			return general.get(name);
		}

		public void add(String name, String value) {
			if (name == null)
				return;
			else if (general == null)
				general = new TreeMap<String, String>();

			general.put(name, value);
		}

		public TreeMap<String, String> getGeneral() {
			return general;
		}

		public void setGeneral(TreeMap<String, String> general) {
			this.general = general;
		}
	}

	/**
	 * command (request) delegation
	 * 
	 * @author gash1
	 * 
	 */
	@XmlRootElement(name = "entry")
	@XmlAccessorType(XmlAccessType.FIELD)
	public static final class ResourceConf {
		private int id;
		private String name;
		private String clazz;
		private boolean enabled;

		public ResourceConf() {
		}

		public ResourceConf(int id, String name, String clazz) {
			this.id = id;
			this.name = name;
			this.clazz = clazz;
		}

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getClazz() {
			return clazz;
		}

		public void setClazz(String clazz) {
			this.clazz = clazz;
		}

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}
	}
}
