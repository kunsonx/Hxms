/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.login.gateway;

import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;
import net.sf.odinms.database.DatabaseConnection;
import java.sql.*;
import net.sf.odinms.net.login.LoginServer;
import org.apache.mina.core.session.IoSession;

/**
 *
 * @author hxms
 */
public class LoginGatewayPacketProcess {

    public static void HandlerPacket(SeekableLittleEndianAccessor slea, IoSession session) {
        LoginGatewayClient client = (LoginGatewayClient) session.getAttribute(LoginGatewayClient.CLIENTKEY);
        try {
            switch (slea.readShort()) {
                case 1:
                    String user = slea.readMapleAsciiString(),
                     pass = slea.readMapleAsciiString(),
                     emali = slea.readMapleAsciiString(),
                     result = "";
                    int safecode = slea.readInt();
                    Connection con = DatabaseConnection.getConnection();
                    PreparedStatement ps = con.prepareStatement("select COUNT(*) = 0  from accounts WHERE `name` = ?");
                    ps.setString(1, user);
                    ResultSet rs = ps.executeQuery();
                    rs.next();
                    boolean create = rs.getBoolean(1);
                    rs.close();
                    ps.close();

                    if (create) {
                        ps = con.prepareStatement("insert into accounts (`name`,`password`,safecode,email) VALUES (?,sha1(?),?,?)");
                        ps.setString(1, user);
                        ps.setString(2, pass);
                        ps.setInt(3, safecode);
                        ps.setString(4, emali);
                        result = ps.executeUpdate() > 0 ? "账号注册成功!" : "账号注册失败!";
                        ps.close();
                    } else {
                        result = "账号已存在!";
                    }
                    con.close();
                    session.write(LoginGatewayPacket.getRegistryResult(result));
                    break;
                case 2:
                    String port = slea.readMapleAsciiString();
                    byte[] key = slea.read(8);
                    client.setPort(port);
                    client.setClientId(LoginServer.getInstance().getWorldInterface().checkClientIvKey(key, port));
                    session.write(LoginGatewayPacket.getSessionIDResult(client.getClientId()));
                    break;
            }
        } catch (Exception e) {
            //       e.printStackTrace();
        }
    }
}
