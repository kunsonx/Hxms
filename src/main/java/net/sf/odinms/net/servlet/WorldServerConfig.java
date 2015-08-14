/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.servlet;

import org.w3c.dom.Node;

/**
 * 世界服务配置器
 *
 * @author hxms 架构
 */
public class WorldServerConfig extends GeneralServerConfig {

	private int worldId;

	public WorldServerConfig() {
		super(GeneralServerType.WORLD);
	}

	public int getWorldId() {
		return worldId;
	}

	public void setWorldId(int worldId) {
		this.worldId = worldId;
	}

	@Override
	public void initConfig(String fileName, GeneralServer server) {
		analyzingFile(fileName, server, "WorldServer");
	}

	@Override
	public void loadConfigFromXmlNode(Node node) {
		try {
		} catch (Exception e) {
			log.error("读取配置错误！", e);
		}
	}
}
