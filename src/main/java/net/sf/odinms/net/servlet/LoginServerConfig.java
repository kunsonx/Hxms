/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.servlet;

import net.sf.odinms.net.login.gateway.LoginGateway;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 *
 * @author hxms架构
 */
public class LoginServerConfig extends GeneralServerConfig {

    private String worldHost, key, serverName, eventMessage;
    private int userLimit, flag, maxCharacters, initMapId, port, rankingInterval;

    public LoginServerConfig() {
        super(GeneralServerType.LOGIN);
    }

    @Override
    public void initConfig(String fileName, GeneralServer server) {
        analyzingFile(fileName, server, "LoginServer");
    }

    @Override
    public void loadConfigFromXmlNode(Node node) {
        if (node instanceof Element) {
            worldHost = getSingleNodeValue((Element) node, "WorldHost", "");
            key = getSingleNodeValue((Element) node, "Key", "");
            userLimit = Integer.parseInt(getSingleNodeValue((Element) node, "UserLimit", "500"));
            serverName = getSingleNodeValue((Element) node, "ServerName", "");
            eventMessage = getSingleNodeValue((Element) node, "EventMessage", "");
            flag = Integer.parseInt(getSingleNodeValue((Element) node, "Flag", "3"));
            maxCharacters = Integer.parseInt(getSingleNodeValue((Element) node, "MaxCharacters", "3"));
            initMapId = Integer.parseInt(getSingleNodeValue((Element) node, "InitMap", "3"));
            port = Integer.parseInt(getSingleNodeValue((Element) node, "Port", ""));
            rankingInterval = Integer.parseInt(getSingleNodeValue((Element) node, "RankingInterval", "3"));
        }
        Node _tmp = getSingleNode((Element) node, "LoginGateWay");
        if (_tmp != null) {
            LoginGateway.getInstance().Init(_tmp, this);
        }
    }

    public String getWorldHost() {
        return worldHost;
    }

    public String getKey() {
        return key;
    }

    public String getServerName() {
        return serverName;
    }

    public int getUserLimit() {
        return userLimit;
    }

    public int getFlag() {
        return flag;
    }

    public int getMaxCharacters() {
        return maxCharacters;
    }

    public int getInitMapId() {
        return initMapId;
    }

    public int getPort() {
        return port;
    }

    public int getRankingInterval() {
        return rankingInterval;
    }

    public String getEventMessage() {
        return eventMessage;
    }

    public void setEventMessage(String eventMessage) {
        this.eventMessage = eventMessage;
    }

    public void setUserLimit(int userLimit) {
        this.userLimit = userLimit;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }
}
