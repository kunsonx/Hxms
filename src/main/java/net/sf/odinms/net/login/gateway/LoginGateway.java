/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.login.gateway;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.mina.core.service.SimpleIoProcessorPool;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.transport.socket.nio.NioProcessor;
import org.apache.mina.transport.socket.nio.NioSession;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import net.sf.odinms.net.login.LoginServer;
import net.sf.odinms.net.servlet.LoginServerConfig;
import net.sf.odinms.tools.Pair;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author hxms
 */
public class LoginGateway {

	private static Logger log = Logger.getLogger(LoginGateway.class);
	private static LoginGateway instance = new LoginGateway();
	private String news;
	private String url;
	private String czurl;
	private String updateUrl;
	private List<String> ban_window = new ArrayList<String>();
	private double version;
	private int port = 8483;
	private int LoginServerPort;
	private Map<String, String> serverPoint = new HashMap<String, String>();
	// io class
	private NioSocketAcceptor acceptor;
	private List<Pair<String, String>> vcode = new ArrayList<Pair<String, String>>();
	private boolean start = false;

	public static LoginGateway getInstance() {
		return instance;
	}

	public List<String> getBan_window() {
		return ban_window;
	}

	public void setBan_window(String ban_window) {
		this.ban_window.addAll(Arrays.asList(ban_window.split(":")));
	}

	public void setNews(String news) {
		this.news = news;
	}

	public int getLoginServerPort() {
		return LoginServerPort;
	}

	public void setLoginServerPort(int LoginServerPort) {
		this.LoginServerPort = LoginServerPort;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getNews() {
		return news;
	}

	public String getUrl() {
		return url;
	}

	public int getPort() {
		return port;
	}

	public String getUpdateUrl() {
		return updateUrl;
	}

	public void setUpdateUrl(String updateUrl) {
		this.updateUrl = updateUrl;
	}

	public double getVersion() {
		return version;
	}

	public void setVersion(double version) {
		this.version = version;
	}

	public List<Pair<String, String>> getVcode() {
		return vcode;
	}

	public void Init(Node _tmp, LoginServerConfig config) {
		Node _t = _tmp.getAttributes().getNamedItem("enable");
		Element element = (Element) _tmp;
		if (_t != null) {
			start = Boolean.parseBoolean(_t.getNodeValue());
		}
		news = config.getSingleNodeValue(element, "News", "");
		url = config.getSingleNodeValue(element, "Url", "");
		czurl = config.getSingleNodeValue(element, "CzUrl", "");
		version = Double.parseDouble(config.getSingleNodeValue(element,
				"VerSion", ""));
		updateUrl = config.getSingleNodeValue(element, "UpdateUrl", "");
		setBan_window(config.getSingleNodeValue(element, "BanWindows", ""));
		Map<String, String> serverpoint = new HashMap<String, String>();
		_t = config.getSingleNode(element, "Lines");
		if (_t instanceof Element) {
			Element _tt = (Element) _t;
			NodeList list = _tt.getChildNodes();
			for (int i = 0; i < list.getLength(); i++) {
				if (list.item(i) instanceof Element) {
					Element _te = (Element) list.item(i);
					if (_te.getNodeName().equals("Ip")) {
						serverpoint.put(_te.getAttribute("name"),
								_te.getTextContent());
					}
				}
			}
		}
		setServerPoint(serverpoint);
		_t = config.getSingleNode(element, "WzCheck");
		if (_t instanceof Element) {
			Element _tt = (Element) _t;
			NodeList list = _tt.getChildNodes();
			for (int i = 0; i < list.getLength(); i++) {
				if (list.item(i) instanceof Element) {
					Element _te = (Element) list.item(i);
					if (_te.getNodeName().equals("Checkd")) {
						vcode.add(new Pair<String, String>(_te
								.getAttribute("name"), _te.getTextContent()));
					}
				}
			}
		}
	}

	public Map<String, String> getServerPoint() {
		return serverPoint;
	}

	public void setServerPoint(Map<String, String> serverPoint) {
		this.serverPoint = serverPoint;
	}

	public String getCzurl() {
		return czurl;
	}

	public void setCzurl(String czurl) {
		this.czurl = czurl;
	}

	public void run() throws Exception {
		if (start) {
			SimpleIoProcessorPool<NioSession> simpleIoProcessorPool = new SimpleIoProcessorPool(
					NioProcessor.class, LoginServer.ExecutorService);
			acceptor = new NioSocketAcceptor(LoginServer.ExecutorService,
					simpleIoProcessorPool);
			acceptor.getSessionConfig().setTcpNoDelay(true);
			acceptor.getFilterChain().addLast("ThreadPool",
					new ExecutorFilter(LoginServer.ExecutorService));
			acceptor.getFilterChain().addLast("executor",
					new ExecutorFilter(LoginServer.ExecutorService));
			acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 15);
			acceptor.setHandler(new LoginGatewayHandler(this));
			acceptor.bind(new InetSocketAddress(getPort()));
			log.info("网关端口监听于 " + getPort());
		}
	}
}
