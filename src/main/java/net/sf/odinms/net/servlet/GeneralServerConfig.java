/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.servlet;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 通用服务器配置
 *
 * @author hxms 架构
 */
public abstract class GeneralServerConfig extends GeneralServerTools {

	protected Logger log = Logger.getLogger(GeneralServerConfig.class);
	private GeneralServerType type;
	protected GeneralServer _Server;
	protected Document document;

	public GeneralServerConfig(GeneralServerType type) {
		this.type = type;
	}

	public GeneralServerConfig(GeneralServer server) {
		this.type = server.getServerType();
	}

	public GeneralServerType getType() {
		return type;
	}

	public abstract void initConfig(String fileName, GeneralServer server);

	public abstract void loadConfigFromXmlNode(Node node);

	protected void analyzingFile(String fileName, GeneralServer server,
			String NodeName) {
		this._Server = server;
		if (document == null) {
			this.document = getDocument(fileName);
		}
		if (document != null) {
			try {
				NodeList nodeList = document.getElementsByTagName(NodeName);
				if (nodeList.getLength() == 0) {
					throw new IllegalArgumentException("配置文件错误！无法读取 频道服务节点！");
				}
				Node node = nodeList.item(0);
				loadConfigFromXmlNode(node);
			} catch (Exception e) {
				log.error("读取配置错误！", e);
			}
		}
	}
}
