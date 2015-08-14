/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.login.gateway;

import java.rmi.RemoteException;
import net.sf.odinms.net.login.LoginServer;
import org.apache.mina.core.session.IoSession;

/**
 *
 * @author hxms
 */
public class LoginGatewayClient {

    public static final String CLIENTKEY = "LoginGatewayClient";
    private IoSession session;
    private long ClientId;
    private String ip;
    private String port;

    public LoginGatewayClient(IoSession session) {
        this.session = session;
        this.ip = session.getRemoteAddress().toString();
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public long getClientId() {
        return ClientId;
    }

    public void setClientId(long ClientId) {
        this.ClientId = ClientId;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public void disconnect() throws RemoteException {
        LoginServer.getInstance().getWorldInterface().disconnectClient(port, ClientId);
    }
}
