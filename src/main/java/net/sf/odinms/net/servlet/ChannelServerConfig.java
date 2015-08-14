/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.servlet;

import java.util.ArrayList;
import java.util.List;
import net.sf.odinms.net.channel.MaplePvp;
import net.sf.odinms.net.channel.www.WebServer;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author hxms 架构
 */
public class ChannelServerConfig extends GeneralServerConfig {

	private String id, worldHost, key, serverMessage, servername, ip;
	private int worlds, channels, expRate, mesoRate, dropRate, bossdropRate,
			petExpRate, port;
	private boolean dropUndroppables, moreThanOne, cashshop, mts,
			useAddMaxAttack, gateway;
	private List<String> eventSM = new ArrayList<String>();

	public ChannelServerConfig() {
		super(GeneralServerType.CHANNEL);
	}

	public ChannelServerConfig(String id) {
		super(GeneralServerType.CHANNEL);
		this.id = id;
	}

	@Override
	public void initConfig(String fileName, GeneralServer server) {
		analyzingFile(fileName, server, "ChannelServer");
	}

	@Override
	public void loadConfigFromXmlNode(Node node) {
		Element element = (Element) node;
		if (_Server != null) {
			Element el = (Element) getSingleNode(element, "ChannelKeys");
			NodeList list = el.getChildNodes();
			for (int i = 0; i < list.getLength(); i++) {
				Node n = list.item(i);
				if (n instanceof Element) {
					el = (Element) n;
					if (el.getNodeName().equals("Key")
							&& el.getAttribute("name").equals(id)) {
						key = el.getTextContent();
					}
				}
			}
			expRate = Integer.parseInt(getSingleNodeValue(element, "ExpRate",
					""));
			mesoRate = Integer.parseInt(getSingleNodeValue(element, "MesoRate",
					""));
			dropRate = Integer.parseInt(getSingleNodeValue(element, "DropRate",
					""));
			bossdropRate = Integer.parseInt(getSingleNodeValue(element,
					"BossDropRate", ""));
			petExpRate = Integer.parseInt(getSingleNodeValue(element,
					"PetRate", ""));
			serverMessage = getSingleNodeValue(element, "ServerMessage", "");
			dropUndroppables = Boolean.parseBoolean(getSingleNodeValue(element,
					"AllDrop", "false"));
			moreThanOne = Boolean.parseBoolean(getSingleNodeValue(element,
					"MoreThanOne", "false"));
			Node _t = getSingleNode(element, "LoadEvents");
			eventSM.clear();
			if (_t instanceof Element) {
				Element _e = (Element) _t;
				NodeList nl = _e.getChildNodes();
				for (int i = 0; i < nl.getLength(); i++) {
					_t = nl.item(i);
					if (_t instanceof Element) {
						_e = (Element) _t;
						if (_e.getNodeName().equals("Event")) {
							eventSM.add(_e.getTextContent());
						}
					}
				}
			}
			cashshop = Boolean.parseBoolean(getSingleNodeValue(element,
					"UseCashShop", "false"));
			mts = Boolean.parseBoolean(getSingleNodeValue(element, "UseMTS",
					"false"));
			servername = getSingleNodeValue(element, "ServerName", "");
			useAddMaxAttack = Boolean.parseBoolean(getSingleNodeValue(element,
					"UseAddMaxAttack", "false"));
			_t = getSingleNode(document.getDocumentElement(), "LoginGateWay");
			if (_t instanceof Element) {
				gateway = Boolean.parseBoolean(((Element) _t)
						.getAttribute("enable"));
			}
			_t = getSingleNode(element, "ServerWeb");
			if (_t instanceof Element) {
				WebServer.getWebServer()
						.setUse(Boolean.parseBoolean(((Element) _t)
								.getAttribute("use")));
				WebServer.getWebServer().setPort(
						Integer.parseInt(((Element) _t).getAttribute("port")));
				WebServer.getWebServer().setServername(servername);
			}
			_t = getSingleNode(element, "Pvp");
			if (_t instanceof Element) {
				MaplePvp.PVP_CHANNEL = Integer.parseInt(((Element) _t)
						.getAttribute("channel"));
				MaplePvp.PVP_MAP = Integer.parseInt(((Element) _t)
						.getAttribute("map"));
			}
			ip = getSingleNodeValue(element, "InterfaceIpAddrees", "");
		}
		worldHost = getSingleNodeValue(element, "WorldHost", "");
		worlds = Integer
				.parseInt(getSingleNodeValue(element, "WorldCount", "1"));
		channels = Integer.parseInt(getSingleNodeValue(element, "ChannelCount",
				"1"));
	}

	public String getWorldHost() {
		return worldHost;
	}

	public int getWorlds() {
		return worlds;
	}

	public int getChannels() {
		return channels;
	}

	public String getId() {
		return id;
	}

	public String getKey() {
		return key;
	}

	public String getServerMessage() {
		return serverMessage;
	}

	public String getServername() {
		return servername;
	}

	public String getIp() {
		return ip;
	}

	public int getExpRate() {
		return expRate;
	}

	public int getMesoRate() {
		return mesoRate;
	}

	public int getDropRate() {
		return dropRate;
	}

	public int getBossdropRate() {
		return bossdropRate;
	}

	public int getPetExpRate() {
		return petExpRate;
	}

	public boolean isDropUndroppables() {
		return dropUndroppables;
	}

	public boolean isMoreThanOne() {
		return moreThanOne;
	}

	public boolean isCashshop() {
		return cashshop;
	}

	public boolean isMts() {
		return mts;
	}

	public boolean isUseAddMaxAttack() {
		return useAddMaxAttack;
	}

	public boolean isGateway() {
		return gateway;
	}

	public List<String> getEventSM() {
		return eventSM;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setServerMessage(String serverMessage) {
		this.serverMessage = serverMessage;
	}

	public void setExpRate(int expRate) {
		this.expRate = expRate;
	}

	public void setMesoRate(int mesoRate) {
		this.mesoRate = mesoRate;
	}

	public void setDropRate(int dropRate) {
		this.dropRate = dropRate;
	}

	public void setPetExpRate(int petExpRate) {
		this.petExpRate = petExpRate;
	}

	public void setBossdropRate(int bossdropRate) {
		this.bossdropRate = bossdropRate;
	}
}
