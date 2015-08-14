/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.login.gateway;

import net.sf.odinms.tools.data.input.ByteArrayByteStream;
import net.sf.odinms.tools.data.input.GenericSeekableLittleEndianAccessor;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;

/**
 *
 * @author hxms
 */
public class LoginGatewayHandler extends IoHandlerAdapter {

    private LoginGateway gateway;

    public LoginGatewayHandler(LoginGateway gateway) {
        this.gateway = gateway;
        LoginGatewayPacket.setLoginGatewayPacket(gateway);
    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        LoginGatewayClient client = (LoginGatewayClient) session.removeAttribute(LoginGatewayClient.CLIENTKEY);
        if (client != null) {
            client.disconnect();
        }
        super.sessionClosed(session); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        //     super.exceptionCaught(session, cause); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        IoBuffer buff = (IoBuffer) message;
        byte[] data = new byte[buff.remaining()];
        buff.get(data);
        if (data.length > 2) {
            SeekableLittleEndianAccessor slea = new GenericSeekableLittleEndianAccessor(new ByteArrayByteStream(data));
            LoginGatewayPacketProcess.HandlerPacket(slea, session);
        }
        //    System.out.println(HexTool.toString(data));
        super.messageReceived(session, message); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
   //     session.write(LoginGatewayPacket.getPing());
        super.sessionIdle(session, status); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void sessionOpened(IoSession session) throws Exception {
        session.setAttribute(LoginGatewayClient.CLIENTKEY, new LoginGatewayClient(session));
        session.write(LoginGatewayPacket.getHello());
        super.sessionOpened(session); //To change body of generated methods, choose Tools | Templates.
    }
}
